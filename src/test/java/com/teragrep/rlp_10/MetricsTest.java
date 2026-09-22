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

import com.codahale.metrics.Timer;
import com.teragrep.rlp_10.config.MetricsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class MetricsTest {

    final MetricsConfig config = new MetricsConfig();
    final Metrics metrics = new Metrics(config);
    final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    // counters should be incremented properly even when called asynchronously
    @Test
    void testAsynchronousCounters() {
        final int numThreads = 500;
        Assertions.assertEquals(0, metrics.connects().getCount());
        final List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(executorService.submit(() -> {
                metrics.connects().inc();
                metrics.retriedConnects().inc();
                metrics.disconnects().inc();
                metrics.resends().inc();
                metrics.records().inc();
            }));
        }
        Assertions.assertEquals(numThreads, tasks.size());

        // wait until each task is compelete
        for (final Future<?> task : tasks) {
            Assertions.assertDoesNotThrow(() -> task.get(50, TimeUnit.MILLISECONDS));
        }
        Assertions.assertEquals(numThreads, metrics.connects().getCount());
        Assertions.assertEquals(numThreads, metrics.retriedConnects().getCount());
        Assertions.assertEquals(numThreads, metrics.disconnects().getCount());
        Assertions.assertEquals(numThreads, metrics.resends().getCount());
        Assertions.assertEquals(numThreads, metrics.records().getCount());
    }

    // timers should provide proper number of metrics even when called asynchronously
    @Test
    void testAsynchronousTimers() {
        final int numThreads = 500;
        Assertions.assertEquals(0, metrics.connects().getCount());
        final List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            tasks.add(executorService.submit(() -> {
                final Timer.Context transmitTimer = metrics.transmitLatency().time();
                final Timer.Context transactionTimer = metrics.transactionLatency().time();
                transmitTimer.close();
                final Timer.Context receiveTimer = metrics.receiveLatency().time();
                receiveTimer.close();
                transactionTimer.close();
            }));
        }
        Assertions.assertEquals(numThreads, tasks.size());
        // wait until each task is complete
        for (final Future<?> task : tasks) {
            Assertions.assertDoesNotThrow(() -> task.get(50, TimeUnit.MILLISECONDS));
        }

        Assertions.assertEquals(numThreads, metrics.transmitLatency().getCount());
        Assertions.assertEquals(numThreads, metrics.transactionLatency().getCount());
        Assertions.assertEquals(numThreads, metrics.receiveLatency().getCount());

        // timers should contain some value (non-deterministic so we assert not equal to 0)
        Assertions.assertNotEquals(0, metrics.transmitLatency().getMeanRate());
        Assertions.assertNotEquals(0, metrics.transactionLatency().getMeanRate());
        Assertions.assertNotEquals(0, metrics.receiveLatency().getMeanRate());
    }
}
