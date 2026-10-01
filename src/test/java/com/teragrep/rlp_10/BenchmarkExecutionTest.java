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
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_10.config.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchmarkExecutionTest {

    private EventLoop eventLoop;
    private ExecutorService executorService;
    final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

    // start a server
    public void init(final Supplier<FrameDelegate> frameDelegateSupplier) {

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

    @AfterAll
    public void clearMessageList() {
        eventLoop.stop();
        executorService.shutdown();
    }

    @Test
    void testExecution() {

        final long expectedRecords = 5000;

        init(() -> new DefaultFrameDelegate((frame) -> {
        }));
        final Metrics metrics = new Metrics(new MetricsConfig());
        final InitiatorConfig initiatorConfig = new InitiatorConfig();
        final SyslogConfig syslogConfig = new SyslogConfig();
        final RecordStreamConfig recordStreamConfig = new RecordStreamConfig(expectedRecords);
        final DelayConfig delayConfig = new DelayConfig();
        final TimeoutConfig timeoutConfig = new TimeoutConfig();
        final TransportConfig transportConfig = new TransportConfig();
        final BenchmarkExecution benchmarkExecution = new BenchmarkExecution(
                executorService,
                metrics,
                initiatorConfig,
                socketAddressConfig,
                syslogConfig,
                recordStreamConfig,
                delayConfig,
                timeoutConfig,
                transportConfig
        );

        // should start normally
        Assertions.assertDoesNotThrow(() -> benchmarkExecution.start());

        // should return number of sent records
        Assertions
                .assertEquals(expectedRecords, Assertions.assertDoesNotThrow(() -> benchmarkExecution.awaitTermination()));
    }

    @Test
    public void testContract() {
        EqualsVerifier.forClass(BenchmarkExecution.class).verify();
    }
}
