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

import com.teragrep.rlp_03.frame.RelpFrame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RetryingMeteredRelpClient implements MeteredRelpClient {

    private final MeteredRelpClient origin;
    private final Metrics metrics;
    private final int openRetryCount;
    private final int transmitRetryCount;

    public RetryingMeteredRelpClient(
            MeteredRelpClient origin,
            Metrics metrics,
            int connectRetryCount,
            int transmitRetryCount
    ) {
        this.origin = origin;
        this.metrics = metrics;
        this.openRetryCount = connectRetryCount;
        this.transmitRetryCount = transmitRetryCount;
    }

    @Override
    public void connect() throws ExecutionException, InterruptedException {
        origin.connect();
    }

    @Override
    public CompletableFuture<RelpFrame> transmitOpen() {
        return origin.transmitOpen();
    }

    @Override
    public CompletableFuture<RelpFrame> completeOpen(final CompletableFuture<RelpFrame> openFuture)
            throws ExecutionException, InterruptedException {
        // try to resolve transmitted "open" future
        CompletableFuture<RelpFrame> futureResult = origin.completeOpen(openFuture);
        int retries = 0;
        while (futureResult.isCompletedExceptionally() && retries < openRetryCount) {
            // if transmitted "open" future was completed exceptionally (for example by timing out), retransmit an "open" message configured number of times.
            retries++;
            metrics.retriedConnects().inc();
            futureResult = origin.completeOpen(transmitOpen());
        }
        // after trying to reconnect configured number of times, if "open" was not resolved successfully, throw an exception.
        if (futureResult.isCompletedExceptionally()) {
            throw new RuntimeException("Failed to open connection in " + openRetryCount + " tries!");
        }
        return futureResult;
    }

    @Override
    public CompletableFuture<RelpFrame> transmitSyslog() {
        return origin.transmitSyslog();
    }

    @Override
    public CompletableFuture<RelpFrame> completeSyslog(final CompletableFuture<RelpFrame> syslogFuture)
            throws ExecutionException, InterruptedException {
        // try to resolve transmitted "open" future
        CompletableFuture<RelpFrame> futureResult = origin.completeSyslog(syslogFuture);
        int retries = 0;
        while (futureResult.isCompletedExceptionally() && retries < transmitRetryCount) {
            // if transmitted "open" future was completed exceptionally (for example by timing out), retransmit an "open" message configured number of times.
            retries++;
            metrics.retriedConnects().inc();
            futureResult = origin.completeSyslog(transmitSyslog());
        }
        // after trying to reconnect configured number of times, if "open" was not resolved successfully, throw an exception.
        if (futureResult.isCompletedExceptionally()) {
            throw new RuntimeException("Failed to open connection in " + transmitRetryCount + " tries!");
        }
        return futureResult;
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        origin.close();
    }
}
