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

import com.codahale.metrics.Timer;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_10.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MeteredRelpClient implements RelpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(MeteredRelpClient.class);
    public final Metrics metrics;
    public final RelpClient origin;

    public MeteredRelpClient(RelpClient origin, Metrics metrics) {
        this.origin = origin;
        this.metrics = metrics;
    }

    /**
     * Transmits a RelpFrame to decorated RelpClient while measuring transaction time. Syslog frames block until
     * resolved to measure transmit and receive timers.
     * 
     * @param relpFrame
     * @return
     */
    @Override
    public CompletableFuture<RelpFrame> transmit(final RelpFrame relpFrame) {
        CompletableFuture<RelpFrame> rv = new CompletableFuture<>();
        try {
            if (relpFrame.command().toString().equals("open")) {
                try (Timer.Context connectTimer = metrics.connectLatency().time()) {
                    rv = origin.transmit(relpFrame);
                    rv.get();
                    metrics.connects().inc();
                }
            }
            else if (relpFrame.command().toString().equals("syslog")) {
                try (Timer.Context transactionTimer = metrics.transactionLatency().time()) {
                    Timer.Context transmitTimer = metrics.transmitLatency().time();
                    rv = origin.transmit(relpFrame);
                    transmitTimer.close();
                    Timer.Context receiveTimer = metrics.receiveLatency().time();
                    rv.get();
                    metrics.records().inc();
                    receiveTimer.close();
                }
            }
            else if (relpFrame.command().toString().equals("close")) {
                rv = origin.transmit(relpFrame);
                rv.get();
                metrics.disconnects().inc();
            }
            else {
                rv = origin.transmit(relpFrame);
            }
            return rv;
        }
        catch (ExecutionException | InterruptedException exception) {
            LOGGER.error("Failed to transmit {} frame!", relpFrame.command().toString(), exception);
            // we return an exceptionally completed Future here, since RelpClient's transmit() signature does not declare any Exceptions.
            // Calling get() on the return value of this method will allow access to the underlying Exception.
            rv.completeExceptionally(exception);
            return rv;
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
