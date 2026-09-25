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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TimeoutMeteredRelpClient implements MeteredRelpClient {

    private final MeteredRelpClient origin;
    private final long connectionTimeout;
    private final long payloadTimeout;

    /**
     * Decorator for MeteredRelpClient which waits for at most the set amount of milliseconds when sending open and
     * syslog messages before failing.
     * 
     * @param origin         MeteredRelpClient to decorate.
     * @param connectTimeout Maximum time to wait for connect messages to resolve before failing in milliseconds.
     * @param payloadTimeout Maximum time to wait for syslog messages to resolve before failing in milliseconds.
     */
    public TimeoutMeteredRelpClient(MeteredRelpClient origin, long connectTimeout, long payloadTimeout) {
        this.origin = origin;
        this.connectionTimeout = connectTimeout;
        this.payloadTimeout = payloadTimeout;
    }

    @Override
    public void connect() throws ExecutionException, InterruptedException {
        origin.connect();
    }

    @Override
    public CompletableFuture<RelpFrame> transmitOpen() {
        return origin.transmitOpen().orTimeout(connectionTimeout, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<RelpFrame> completeOpen(final CompletableFuture<RelpFrame> openFuture)
            throws ExecutionException, InterruptedException {
        return origin.completeOpen(openFuture);
    }

    @Override
    public CompletableFuture<RelpFrame> transmitSyslog(String payload) {
        return origin.transmitSyslog(payload).orTimeout(payloadTimeout, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<RelpFrame> completeSyslog(final CompletableFuture<RelpFrame> syslogFuture, String payload)
            throws ExecutionException, InterruptedException {
        return origin.completeSyslog(syslogFuture, payload);
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        origin.close();
    }
}
