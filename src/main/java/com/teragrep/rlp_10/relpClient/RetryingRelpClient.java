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

import com.codahale.metrics.Counter;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_10.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RetryingRelpClient implements RelpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingRelpClient.class);
    private final RelpClient origin;
    private final Metrics metrics;
    private final int retryOpenCount;
    private final int retrySyslogCount;

    public RetryingRelpClient(
            final RelpClient origin,
            final Metrics metrics,
            final int retryOpenCount,
            final int retrySyslogCount
    ) {
        this.origin = origin;
        this.metrics = metrics;
        this.retryOpenCount = retryOpenCount;
        this.retrySyslogCount = retrySyslogCount;
    }

    /**
     * Transmit a RelpFrame, retrying configured number of times if unsuccessful. Note that this methdo blocks until the transmission is completed either successfully or exceptionally.
     * @param relpFrame RelpFrame to transmit
     * @return CompletableFuture received from decorated RelpClient
     */
    @Override
    public CompletableFuture<RelpFrame> transmit(final RelpFrame relpFrame) {
        // attempt to transmit normally
        CompletableFuture<RelpFrame> rv = origin.transmit(relpFrame);
        try {
            rv.get();
        }
        catch (final ExecutionException | InterruptedException exception) {
            // if any error occurs during transmission, retry
            final String command = relpFrame.command().toString();
            if ("open".equals(command)) {
                rv = retryTransmission(relpFrame, 0, retryOpenCount, metrics.retriedConnects());
            }
            else if ("syslog".equals(command)) {
                rv = retryTransmission(relpFrame, 0, retrySyslogCount, metrics.resends());
            }
        }
        return rv;
    }

    private CompletableFuture<RelpFrame> retryTransmission(
            final RelpFrame relpFrame,
            int retries,
            final int maxRetries,
            final Counter counter
    ) {
        try {
            counter.inc();
            final CompletableFuture<RelpFrame> frame = origin.transmit(relpFrame);
            frame.get();
            return frame;
        }
        catch (final ExecutionException | InterruptedException exception) {
            LOGGER.error("Failed to send <{}> frame, retrying!", relpFrame.command().toString(), exception);
            if (retries < maxRetries) {
                retries++;
                return retryTransmission(relpFrame, retries, maxRetries, counter);
            }
            else {
                throw new RuntimeException(
                        "Failed to send " + relpFrame.command().toString() + " frame after " + retries + " tries!",
                        exception
                );
            }
        }
    }

    @Override
    public void close() {
        origin.close();
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
