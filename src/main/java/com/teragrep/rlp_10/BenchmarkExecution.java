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

/**
 * BenchmarkExecution starts a configured number of Eventloops within a given ExecutorService and keeps track of every
 * Task within each of the EventLoops
 */
public final class BenchmarkExecution {

    private final Map<EventLoop, List<Initiator>> eventLoops;
    private final EventLoopFactory eventLoopFactory;
    private final List<Future<Long>> executorTasks;
    private final ExecutorService executorService;
    private final SocketFactory socketFactory;
    private final InitiatorFactory initiatorFactory;
    private final int initiatorCount;
    private final int eventLoopCount;

    public BenchmarkExecution(
            final ExecutorService executorService,
            final SocketFactory socketFactory,
            final InitiatorFactory initiatorFactory,
            final int initiatorCount,
            final int eventLoopCount
    ) {
        this.executorService = executorService;
        this.eventLoopFactory = new EventLoopFactory();
        this.eventLoops = new HashMap<>();
        this.executorTasks = new ArrayList<>();
        this.socketFactory = socketFactory;
        this.initiatorFactory = initiatorFactory;
        this.initiatorCount = initiatorCount;
        this.eventLoopCount = eventLoopCount;
    }

    public void start() throws IOException {
        final int baseInitiators = initiatorCount / eventLoopCount;
        final int remainder = initiatorCount % eventLoopCount;

        // create and start configured number of EventLoops
        for (int i = 0; i < eventLoopCount; i++) {
            // use remainder to determine if this EventLoop should get an additional Initiator or not in order to fit all Initiators within configured number of EventLoops
            final int initiatorsForEventLoop = baseInitiators + (i < remainder ? 1 : 0);
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
            for (final Initiator initiator : initiators) {
                executorTasks.add(executorService.submit(initiator));
            }
            this.eventLoops.put(eventLoop, initiators);
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
        executorService.shutdown();
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
            equals = initiatorCount == that.initiatorCount && eventLoopCount == that.eventLoopCount && Objects
                    .equals(eventLoops, that.eventLoops) && Objects
                            .equals(eventLoopFactory, that.eventLoopFactory)
                    && Objects.equals(executorTasks, that.executorTasks) && Objects.equals(executorService, that.executorService) && Objects.equals(socketFactory, that.socketFactory) && Objects.equals(initiatorFactory, that.initiatorFactory);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        eventLoops, eventLoopFactory, executorTasks, executorService, socketFactory, initiatorFactory,
                        initiatorCount, eventLoopCount
                );
    }
}
