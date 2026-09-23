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
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_10.config.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.*;

import java.util.concurrent.*;

/**
 * These are a copy from rlp_03 test suite
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class BenchmarkTest {

    private EventLoop eventLoop;
    private Thread eventLoopThread;
    private ExecutorService executorService;

    // default configs to reduce clutter
    private final MetricsConfig metricsConfiguration = new MetricsConfig();
    private final PrometheusConfig prometheusConfiguration = new PrometheusConfig();
    private final TimeoutConfig timeoutConfiguration = new TimeoutConfig();
    private final TransportConfig transportConfiguration = new TransportConfig();
    private final ReportConfig reportConfig = new ReportConfig();
    private final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();
    private final DelayConfig delayConfig = new DelayConfig();
    private final SyslogConfig syslogConfig = new SyslogConfig();

    private final ConcurrentLinkedDeque<byte[]> messageDeque = new ConcurrentLinkedDeque<>();

    @BeforeAll
    public void init() {
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
                new FrameDelegationClockFactory(() -> new DefaultFrameDelegate((frame) -> messageDeque.add(frame.relpFrame().payload().toBytes())))
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(socketAddressConfig.port()));
    }

    @AfterAll
    public void cleanup() {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
    }

    @AfterEach
    public void clearMessageList() {
        // clear received list
        messageDeque.clear();
    }

    /**
     * Should receive configured number of messages regardless of number of clients or EventLoops configured
     */
    @Test
    public void testMessageCount() {
        final int clients = 123;
        final long messageCount = 100000;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final int eventLoopCount = 19;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(
                clients,
                retryTransmissionCount,
                retryConnectionCount,
                eventLoopCount
        );
        final RecordStreamConfig recordStreamConfig = new RecordStreamConfig(messageCount);
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration,
                recordStreamConfig,
                reportConfig,
                socketAddressConfig,
                delayConfig,
                syslogConfig
        );
        Assertions.assertEquals(messageCount, benchmark.call());
        Assertions.assertFalse(messageDeque.isEmpty());
        Assertions.assertEquals(messageCount, messageDeque.size());
    }

    /**
     * Should be able to have fewer clients than EventLoops
     */
    @Test
    public void testFewerClientsThanEventLoops() {
        final int clients = 5;
        final long messageCount = 5000;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final int eventLoopCount = 19;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(
                clients,
                retryTransmissionCount,
                retryConnectionCount,
                eventLoopCount
        );
        final RecordStreamConfig recordStreamConfig = new RecordStreamConfig(messageCount);
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration,
                recordStreamConfig,
                reportConfig,
                socketAddressConfig,
                delayConfig,
                syslogConfig
        );
        Assertions.assertEquals(messageCount, benchmark.call());
        Assertions.assertFalse(messageDeque.isEmpty());
        Assertions.assertEquals(messageCount, messageDeque.size());
    }

    /**
     * Should be able to configure no clients
     */
    @Test
    public void testNoClients() {
        final int clients = 0;
        final long messageCount = 5000;
        final int retryTransmissionCount = 3;
        final int retryConnectionCount = 3;
        final int eventLoopCount = 19;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(
                clients,
                retryTransmissionCount,
                retryConnectionCount,
                eventLoopCount
        );
        final RecordStreamConfig recordStreamConfig = new RecordStreamConfig(messageCount);
        final Benchmark benchmark = new Benchmark(
                initiatorConfig,
                metricsConfiguration,
                prometheusConfiguration,
                timeoutConfiguration,
                transportConfiguration,
                recordStreamConfig,
                reportConfig,
                socketAddressConfig,
                delayConfig,
                syslogConfig
        );
        benchmark.call();
        Assertions.assertTrue(messageDeque.isEmpty());
    }

    @Test
    public void testContract() {
        EqualsVerifier.forClass(Benchmark.class).verify();
    }
}
