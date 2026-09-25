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
package com.teragrep.rlp_10.relpClient;

import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_10.Metrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RetryingMeteredRelpClient implements MeteredRelpClient {

    private final MeteredRelpClient origin;
    private final Metrics metrics;
    private final int openRetryCount;
    private final int transmitRetryCount;

    /**
     * Decorator for MeteredRelpClient which retries sending of open and syslog messages a set number of times.
     * Increments reconnection and resend counts of a Metrics Object.
     * 
     * @param origin             MeteredRelpClient to decorate
     * @param metrics            Metrics object to increment resend and reconnect counts
     * @param connectRetryCount  Number of times to retry opening
     * @param transmitRetryCount Number of times to retry sending
     */
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
        int retries = 0;
        CompletableFuture<RelpFrame> openFrame = openFuture;
        while (retries < openRetryCount) {
            try {
                origin.completeOpen(openFrame);
                return openFrame;
            }
            catch (ExecutionException e) {
                // if transmission fails, retransmit until retryCount is reached.
                retries++;
                metrics.retriedConnects().inc();
                openFrame = origin.transmitOpen();
            }
            catch (InterruptedException e) {
                throw new RuntimeException("An unrecoverable error occurred while opening a connection!", e);
            }
        }
        return origin.completeOpen(openFrame);
    }

    @Override
    public CompletableFuture<RelpFrame> transmitSyslog(String payload) {
        return origin.transmitSyslog(payload);
    }

    @Override
    public CompletableFuture<RelpFrame> completeSyslog(final CompletableFuture<RelpFrame> syslogFuture, String payload)
            throws ExecutionException, InterruptedException {
        int retries = 0;
        CompletableFuture<RelpFrame> syslogFrame = syslogFuture;
        while (retries < transmitRetryCount) {
            try {
                origin.completeSyslog(syslogFrame, payload);
                return syslogFrame;
            }
            catch (ExecutionException e) {
                // if transmission fails, retransmit until retryCount is reached.
                retries++;
                metrics.resends().inc();
                syslogFrame = origin.transmitSyslog(payload);
            }
            catch (InterruptedException e) {
                throw new RuntimeException("An unrecoverable error occurred while transmitting a syslog record!", e);
            }
        }
        return origin.completeSyslog(syslogFrame, payload);
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        origin.close();
    }
}
