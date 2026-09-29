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

import com.teragrep.cnf_01.ConfigurationException;
import com.teragrep.net_01.channel.context.ConnectContextFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_10.config.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class BenchmarkExecution {

    private final Map<EventLoop, List<Initiator>> eventLoops;
    private final EventLoopFactory eventLoopFactory;
    private final List<Future<Long>> executorTasks;
    private final ExecutorService executorService;
    private final Metrics metrics;
    private final InitiatorConfig initiatorConfig;
    private final SocketAddressConfig socketAddressConfig;
    private final SyslogConfig syslogConfig;
    private final RecordStreamConfig recordStreamConfig;
    private final DelayConfig delayConfig;
    private final TimeoutConfig timeoutConfig;
    private final TransportConfig transportConfig;

    public BenchmarkExecution(
            ExecutorService executorService,
            Metrics metrics,
            InitiatorConfig initiatorConfig,
            SocketAddressConfig socketAddressConfig,
            SyslogConfig syslogConfig,
            RecordStreamConfig recordStreamConfig,
            DelayConfig delayConfig,
            TimeoutConfig timeoutConfig,
            TransportConfig transportConfig
    ) {
        this.executorService = executorService;
        this.metrics = metrics;
        this.initiatorConfig = initiatorConfig;
        this.socketAddressConfig = socketAddressConfig;
        this.syslogConfig = syslogConfig;
        this.recordStreamConfig = recordStreamConfig;
        this.delayConfig = delayConfig;
        this.timeoutConfig = timeoutConfig;
        this.transportConfig = transportConfig;
        this.eventLoopFactory = new EventLoopFactory();
        this.eventLoops = new HashMap<>();
        this.executorTasks = new ArrayList<>();
    }

    public void start() throws ConfigurationException, IOException {

        final SocketFactory socketFactory = transportConfig.socketFactory();

        // recordStream is shared across all Initiators. Initiators ask for records until the recordstream is exhausted
        RecordStream recordStream = recordStreamConfig
                .recordStream(syslogConfig.hostname(), syslogConfig.appName(), delayConfig.delay());

        InitiatorFactory initiatorFactory = new InitiatorFactory(
                recordStream,
                metrics,
                socketAddressConfig,
                timeoutConfig,
                initiatorConfig
        );
        final int baseInitiators = initiatorConfig.initiatorCount() / initiatorConfig.eventLoopCount();
        final int remainder = initiatorConfig.initiatorCount() % initiatorConfig.eventLoopCount();

        // create and start configured number of EventLoops
        for (int eventLoopCount = 0; eventLoopCount < initiatorConfig.eventLoopCount(); eventLoopCount++) {
            // use remainder to determine if this EventLoop should get an additional Initiator or not in order to fit all Initiators within configured number of EventLoops
            final int initiatorsForEventLoop = baseInitiators + (eventLoopCount < remainder ? 1 : 0);
            final ConnectContextFactory connectContextFactory = new ConnectContextFactory(
                    executorService,
                    socketFactory
            );

            final EventLoop eventLoop = eventLoopFactory.create();
            executorService.submit(eventLoop);

            final RelpClientFactory relpClientFactory = new RelpClientFactory(connectContextFactory, eventLoop);

            // create and start initiators for this eventloop
            final List<Initiator> initiators = initiatorFactory
                    .createInitiators(initiatorsForEventLoop, relpClientFactory);
            for (Initiator initiator : initiators) {
                executorTasks.add(executorService.submit(initiator));
            }
            eventLoops.put(eventLoop, initiators);
        }
    }

    public Long awaitTermination() throws ExecutionException, InterruptedException {
        // block until each task is complete
        long totalRecords = 0;
        for (final Future<Long> task : executorTasks) {
            final long taskRecords = task.get();
            totalRecords += taskRecords;
        }
        return totalRecords;
    }

    public void stop() {
        for (final Map.Entry<EventLoop, List<Initiator>> entry : eventLoops.entrySet()) {
            final List<Initiator> initiators = entry.getValue();
            final EventLoop eventLoop = entry.getKey();
            for (final Initiator initiator : initiators) {
                initiator.stop();
            }
            eventLoop.stop();
        }
    }

    @Override
    public boolean equals(final Object o) {
        final boolean equals;
        if (this == o) {
            equals = true;
        }
        else if (o == null || getClass() != o.getClass()) {
            equals = false;
        }
        else {
            final BenchmarkExecution that = (BenchmarkExecution) o;
            equals = Objects.equals(eventLoops, that.eventLoops) && Objects
                    .equals(eventLoopFactory, that.eventLoopFactory)
                    && Objects.equals(executorTasks, that.executorTasks) && Objects.equals(executorService, that.executorService) && Objects.equals(metrics, that.metrics) && Objects.equals(initiatorConfig, that.initiatorConfig) && Objects.equals(socketAddressConfig, that.socketAddressConfig) && Objects.equals(syslogConfig, that.syslogConfig) && Objects.equals(recordStreamConfig, that.recordStreamConfig) && Objects.equals(delayConfig, that.delayConfig) && Objects.equals(timeoutConfig, that.timeoutConfig) && Objects.equals(transportConfig, that.transportConfig);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        eventLoops, eventLoopFactory, executorTasks, executorService, metrics, initiatorConfig,
                        socketAddressConfig, syslogConfig, recordStreamConfig, delayConfig, timeoutConfig,
                        transportConfig
                );
    }
}
