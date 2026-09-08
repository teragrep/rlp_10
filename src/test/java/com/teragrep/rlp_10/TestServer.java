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
package com.teragrep.rlp_10;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.TLSFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_10.config.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * These are a copy from rlp_03 test suite
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServer.class);

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private ExecutorService executorService;

    private final List<byte[]> messageList = new LinkedList<>();

    private void init() {
        final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertDoesNotThrow(() -> eventLoop = eventLoopFactory.create());

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        executorService = Executors.newSingleThreadExecutor();
        final ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(() -> new DefaultFrameDelegate((frame) -> messageList.add(frame.relpFrame().payload().toBytes())))
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(socketAddressConfig.port()));
    }

    private void initWithTLS() {
        final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertDoesNotThrow(() -> eventLoop = eventLoopFactory.create());

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();
        executorService = Executors.newSingleThreadExecutor();

        try{
            final TransportConfig transportConfiguration = new TransportConfig(true, Path.of("src/test/resources/tls/keystore-server.jks"),Path.of("src/test/resources/tls/truststore.jks"),"changeit","changeit","TLSv1.3");

            SSLContext sslContext = SSLContext.getInstance(transportConfiguration.protocol());
            KeyStore ks = KeyStore.getInstance("JKS");

            File file = new File(transportConfiguration.keyStorePath().toUri());
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                ks.load(fileInputStream, transportConfiguration.keyStorePassword().toCharArray());
                TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                KeyManagerFactory kmf =
                        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, transportConfiguration.keyStorePassword().toCharArray());
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            }

            Function<SSLContext, SSLEngine> sslEngineFunction = context -> {
                SSLEngine engine = context.createSSLEngine();
                engine.setUseClientMode(false);
                return engine;
            };

            final ServerFactory serverFactory = new ServerFactory(
                    eventLoop,
                    executorService,
                    new TLSFactory(sslContext,sslEngineFunction),
                    new FrameDelegationClockFactory(() -> new DefaultFrameDelegate((frame) -> messageList.add(frame.relpFrame().payload().toBytes())))
            );
            Assertions.assertDoesNotThrow(() -> serverFactory.create(socketAddressConfig.port()));
        }
        catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e){
            //TODO handle error
        } catch (UnrecoverableKeyException | KeyManagementException e) {
            //TODO handle error
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void cleanup() {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
    }

    @AfterEach
    public void clearMessageList() {
        // clear received list
        messageList.clear();
    }

    @Test
    public void testTLSServer() {
        Assertions.assertDoesNotThrow(()-> initWithTLS());
        final int clients = 1;
        final int messageCount = 150;
        final int retryTransmissionCount = 0;
        final int retryConnectionCount = 0;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(clients, messageCount, retryTransmissionCount, retryConnectionCount);
        final MetricsConfiguration metricsConfiguration = new MetricsConfiguration(10000, 1);
        final PrometheusConfiguration prometheusConfiguration = new PrometheusConfiguration(8080);
        final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration();
        final TransportConfig transportConfiguration = new TransportConfig(true, Path.of("src/test/resources/tls/keystore-client.jks"),Path.of("src/test/resources/tls/truststore.jks"),"changeit","changeit","TLSv1.3");
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration
        );
        benchmark.startBenchmark();
        Assertions.assertDoesNotThrow(() -> Thread.sleep(12000));
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:"+prometheusConfiguration.port()+"/metrics"))
                .GET()
                .build();

        // send a GET request to prometheus URL. Expect to receive a response containing each ot the metrics.
        final HttpResponse<String> response = Assertions.assertDoesNotThrow(()->client.send(request, HttpResponse.BodyHandlers.ofString()));
        Assertions.assertEquals(200,response.statusCode());
        benchmark.stopBenchmark();

        final int expectedRecords = clients * messageCount;
        final int expectedResends = 0;
        final int expectedReconnects = 0;

        Assertions.assertTrue(response.body().contains("connects "+clients));
        Assertions.assertTrue(response.body().contains("records "+expectedRecords));
        Assertions.assertTrue(response.body().contains("connectLatency_count "+clients));
        Assertions.assertTrue(response.body().contains("transactionLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("transmitLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("receiveLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("retriedConnects "+expectedReconnects));
        Assertions.assertTrue(response.body().contains("transmitLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("resends "+expectedResends));
    }

    // TODO: get rid of Thread.sleep somehow
    @Test
    public void testBenchmark() {
        Assertions.assertDoesNotThrow(()->init());
        final int clients = 50;
        final int messageCount = 250;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(clients, messageCount, retryTransmissionCount, retryConnectionCount);
        final MetricsConfiguration metricsConfiguration = new MetricsConfiguration(10000, 1);
        final PrometheusConfiguration prometheusConfiguration = new PrometheusConfiguration(8080);
        final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration();
        final TransportConfig transportConfiguration = new TransportConfig();
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration
        );
        benchmark.startBenchmark();
        Assertions.assertDoesNotThrow(() -> Thread.sleep(10000));
        benchmark.stopBenchmark();
        Assertions.assertDoesNotThrow(() -> Thread.sleep(200));
        Assertions.assertTrue(!messageList.isEmpty());
        Assertions.assertTrue(messageList.size() <= clients * messageCount);
    }

    @Test
    public void testPrometheusServer() {
        Assertions.assertDoesNotThrow(()->init());
        final int clients = 50;
        final int messageCount = 250;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(clients, messageCount, retryTransmissionCount, retryConnectionCount);
        final MetricsConfiguration metricsConfiguration = new MetricsConfiguration(10000, 1);
        final PrometheusConfiguration prometheusConfiguration = new PrometheusConfiguration(8080);
        final TimeoutConfiguration timeoutConfiguration = new TimeoutConfiguration();
        final TransportConfig transportConfiguration = new TransportConfig();
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration
        );
        benchmark.startBenchmark();
        Assertions.assertDoesNotThrow(() -> Thread.sleep(12000));
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:"+prometheusConfiguration.port()+"/metrics"))
                .GET()
                .build();

        // send a GET request to prometheus URL. Expect to receive a response containing each ot the metrics.
        final HttpResponse<String> response = Assertions.assertDoesNotThrow(()->client.send(request, HttpResponse.BodyHandlers.ofString()));
        Assertions.assertEquals(200,response.statusCode());
        benchmark.stopBenchmark();

        final int expectedRecords = clients * messageCount;
        final int expectedResends = 0;
        final int expectedReconnects = 0;

        Assertions.assertTrue(response.body().contains("connects "+clients));
        Assertions.assertTrue(response.body().contains("records "+expectedRecords));
        Assertions.assertTrue(response.body().contains("connectLatency_count "+clients));
        Assertions.assertTrue(response.body().contains("transactionLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("transmitLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("receiveLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("retriedConnects "+expectedReconnects));
        Assertions.assertTrue(response.body().contains("transmitLatency_count "+expectedRecords));
        Assertions.assertTrue(response.body().contains("resends "+expectedResends));
    }

}
