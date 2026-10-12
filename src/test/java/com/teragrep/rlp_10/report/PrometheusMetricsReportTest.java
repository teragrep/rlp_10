/*
 * Teragrep performance test application for RELP (rlp_10)
 * Copyright (C) 2026 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.rlp_10.report;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_10.*;
import com.teragrep.rlp_10.config.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class PrometheusMetricsReportTest {

    private EventLoop eventLoop;
    private ExecutorService executorService;

    private final MetricsConfig metricsConfiguration = new MetricsConfig();
    private final TimeoutConfig timeoutConfiguration = new TimeoutConfig();
    private final TransportConfig transportConfiguration = new TransportConfig();
    private final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();
    private final DelayConfig delayConfig = new DelayConfig();
    private final SyslogConfig syslogConfig = new SyslogConfig();

    private final ConcurrentLinkedDeque<byte[]> messageDeque = new ConcurrentLinkedDeque<>();

    private void init(final Supplier<FrameDelegate> frameDelegateSupplier) {

        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertDoesNotThrow(() -> eventLoop = eventLoopFactory.create());
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        executorService.submit(eventLoop);

        final ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(() -> frameDelegateSupplier.get())
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(socketAddressConfig.port()));
    }

    @AfterEach
    public void clearMessageList() {
        eventLoop.stop();
        executorService.shutdown();
        // clear received list
        messageDeque.clear();
    }

    @Test
    public void testPrometheusServer() {
        final int prometheusPort = 8080;
        init(() -> new DefaultFrameDelegate((frame) -> messageDeque.add(frame.relpFrame().payload().toBytes())));
        final int initiatorCount = 50;
        final long messageCount = 10000;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final int eventLoopCount = 2;

        final SocketFactory socketFactory = Assertions.assertDoesNotThrow(transportConfiguration::socketFactory);
        final RecordStream recordStream = new RecordStreamConfig(messageCount)
                .recordStream(syslogConfig.hostname(), syslogConfig.appName(), delayConfig.delay());
        final Metrics metrics = metricsConfiguration.metrics();
        final InitiatorFactory initiatorFactory = new InitiatorFactory(
                recordStream,
                metrics,
                socketAddressConfig.hostname(),
                socketAddressConfig.port(),
                timeoutConfiguration.openTimeout(),
                timeoutConfiguration.payloadTimeout(),
                retryTransmissionCount,
                retryConnectionCount
        );
        final BenchmarkExecution benchmarkExecution = new BenchmarkExecution(
                executorService,
                socketFactory,
                initiatorFactory,
                initiatorCount,
                eventLoopCount
        );

        final List<MetricsReport> reports = new ArrayList<>();
        final PrometheusMetricsReport report = new PrometheusMetricsReport(metrics.registry(), prometheusPort);
        reports.add(report);

        final Benchmark benchmark = new Benchmark(benchmarkExecution, reports);

        final ExecutorService forkJoinPool = ForkJoinPool.commonPool();
        final Future<Long> clientRecords = forkJoinPool.submit(benchmark);
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:" + prometheusPort + "/metrics"))
                .GET()
                .build();

        // send a GET request to prometheus URL. Expect to receive a response containing each ot the metrics.
        final HttpResponse<String> response = Assertions
                .assertDoesNotThrow(() -> client.send(request, HttpResponse.BodyHandlers.ofString()));

        final AtomicLong recordsSent = new AtomicLong();
        Assertions.assertDoesNotThrow(() -> {
            final Long records = clientRecords.get();
            recordsSent.set(records);
        });
        Assertions.assertEquals(messageCount, recordsSent.get());

        client.close();
        forkJoinPool.shutdown();
        // assert that HTTP response contains information about each metric in prometheus format
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP connects Generated from Dropwizard metric import (metric=connects, type=com.codahale.metrics.Counter)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP disconnects Generated from Dropwizard metric import (metric=disconnects, type=com.codahale.metrics.Counter)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP records Generated from Dropwizard metric import (metric=records, type=com.codahale.metrics.Counter)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP connectLatency Generated from Dropwizard metric import (metric=connectLatency, type=com.codahale.metrics.Timer)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP transactionLatency Generated from Dropwizard metric import (metric=transactionLatency, type=com.codahale.metrics.Timer)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP transmitLatency Generated from Dropwizard metric import (metric=transmitLatency, type=com.codahale.metrics.Timer)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP receiveLatency Generated from Dropwizard metric import (metric=receiveLatency, type=com.codahale.metrics.Timer)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP retriedConnects Generated from Dropwizard metric import (metric=retriedConnects, type=com.codahale.metrics.Counter)"
                                )
                );
        Assertions
                .assertTrue(
                        response
                                .body()
                                .contains(
                                        "# HELP resends Generated from Dropwizard metric import (metric=resends, type=com.codahale.metrics.Counter)"
                                )
                );

        // each metric should have proper type
        Assertions.assertTrue(response.body().contains("# TYPE connects gauge"));
        Assertions.assertTrue(response.body().contains("# TYPE disconnects gauge"));
        Assertions.assertTrue(response.body().contains("# TYPE records gauge"));
        Assertions.assertTrue(response.body().contains("# TYPE connectLatency summary"));
        Assertions.assertTrue(response.body().contains("# TYPE transactionLatency summary"));
        Assertions.assertTrue(response.body().contains("# TYPE transmitLatency summary"));
        Assertions.assertTrue(response.body().contains("# TYPE receiveLatency summary"));
        Assertions.assertTrue(response.body().contains("# TYPE retriedConnects gauge"));
        Assertions.assertTrue(response.body().contains("# TYPE resends gauge"));
    }

    @Test
    public void testContract() {
        final Server redServer = new Server(8080);
        final Server blueServer = new Server(8081);
        EqualsVerifier
                .forClass(PrometheusMetricsReport.class)
                .withPrefabValues(Server.class, redServer, blueServer)
                .verify();
    }
}
