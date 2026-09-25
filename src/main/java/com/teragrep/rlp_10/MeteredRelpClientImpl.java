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
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.client.RelpClientStub;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MeteredRelpClientImpl implements MeteredRelpClient {

    private final RelpClientFactory relpClientFactory;
    private final RelpFrameFactory relpFrameFactory;
    private final RecordStream recordStream;
    private final String hostname;
    private final int port;
    private RelpClient relpClient;
    private final Metrics metrics;

    public MeteredRelpClientImpl(
            RelpClientFactory relpClientFactory,
            RelpFrameFactory relpFrameFactory,
            RecordStream recordStream,
            String hostname,
            int port,
            Metrics metrics
    ) {
        this.relpClientFactory = relpClientFactory;
        this.relpFrameFactory = relpFrameFactory;
        this.recordStream = recordStream;
        this.hostname = hostname;
        this.port = port;
        this.relpClient = new RelpClientStub();
        this.metrics = metrics;
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        final CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
        close.get();
        metrics.disconnects().inc();
        relpClient.close();
    }

    @Override
    public CompletableFuture<RelpFrame> transmitSyslog() {
        final String payload = new String(recordStream.get(), StandardCharsets.UTF_8); // todo recordStream should return a stubable SyslogMessage, currently stubness is represented by empty bytearray
        if (payload.isEmpty()) {
            CompletableFuture<RelpFrame> rv = new CompletableFuture<>();
            rv.completeExceptionally(new RuntimeException("RecordStream is exhausted!"));
            return rv;
        }
        try (Timer.Context transmitTimer = metrics.transmitLatency().time()) {
            final CompletableFuture<RelpFrame> syslog = relpClient.transmit(relpFrameFactory.create("syslog", payload));
            return syslog;
        }
    }

    @Override
    public CompletableFuture<RelpFrame> completeSyslog(CompletableFuture<RelpFrame> syslogFuture)
            throws ExecutionException, InterruptedException {
        if (syslogFuture.isCompletedExceptionally()) {
            return syslogFuture;
        }
        try (Timer.Context receiveTimer = metrics.receiveLatency().time()) {
            syslogFuture.get();
            metrics.records().inc();
            return syslogFuture;
        }
    }

    @Override
    public void connect() throws ExecutionException, InterruptedException {
        relpClient = relpClientFactory.open(new InetSocketAddress(hostname, port)).get();
    }

    @Override
    public CompletableFuture<RelpFrame> transmitOpen() {
        final RelpFrame openFrame = relpFrameFactory.create("open", "a hallo yo client");
        final CompletableFuture<RelpFrame> open = relpClient.transmit(openFrame);
        return open;
    }

    @Override
    public CompletableFuture<RelpFrame> completeOpen(CompletableFuture<RelpFrame> openFuture)
            throws ExecutionException, InterruptedException {
        openFuture.get();
        metrics.connects().inc();
        return openFuture;
    }

}
