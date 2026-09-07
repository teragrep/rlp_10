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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

class Initiator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Initiator.class);
    private static final RelpFrameFactory relpFrameFactory = new RelpFrameFactory();
    private final RelpClientFactory relpClientFactory;
    private final RecordStream recordStream;
    private final MetricRegistry metricRegistry;
    private final String hostname;
    private final int port;
    private final int messageCount;
    private final long openTimeout;
    private final long payloadTimeout;

    private final Counter records;
    private final Timer transactionLatency;
    private final Timer transmitLatency;
    private final Timer receiveLatency;
    private final Counter connects;
    private final Counter disconnects;
    private final Counter retriedConnects;
    private final Counter resends;
    private final Timer connectLatency;


    private volatile boolean run = true;

    //TODO: All initiators are currently in one eventLoop, allow for multiples.
    public Initiator(final RelpClientFactory relpClientFactory, final RecordStream recordStream, final MetricRegistry metricRegistry, int messageCount, int openTimeout, long payloadTimeout) {
        this(relpClientFactory, recordStream, "localhost", 1601, metricRegistry, messageCount, openTimeout, payloadTimeout);
    }

    public Initiator(
            final RelpClientFactory relpClientFactory,
            final RecordStream recordStream,
            final String hostName,
            final int port,
            final MetricRegistry metricRegistry,
            final int messageCount,
            final long connectTimeout,
            final long payloadTimeout
    ) {
        this.relpClientFactory = relpClientFactory;
        this.recordStream = recordStream;
        this.hostname = hostName;
        this.port = port;
        this.metricRegistry = metricRegistry;
        this.messageCount = messageCount;
        this.openTimeout = connectTimeout;
        this.payloadTimeout = payloadTimeout;
        this.records = metricRegistry.counter("records");
        this.transactionLatency = metricRegistry.timer("transactionLatency");
        this.transmitLatency = metricRegistry.timer("transmitLatency");
        this.receiveLatency = metricRegistry.timer("receiveLatency");
        this.connects = metricRegistry.counter("connects");
        this.disconnects = metricRegistry.counter("disconnects");
        this.retriedConnects = metricRegistry.counter("retriedConnects");
        this.resends = metricRegistry.counter("resends");
        this.connectLatency = metricRegistry.timer("connectLatency");

    }

    @Override
    public void run() {
        // producer threads

        try (
                RelpClient relpClient = relpClientFactory.open(new InetSocketAddress(hostname, port)).get(openTimeout, TimeUnit.SECONDS);
        ) {
            // try to connect, retrying until connection is established.
            // TODO: will this Timer skew connection statistics if a connection fails?
            try(final Timer.Context timerContext = connectLatency.time()) {
                connects.inc();
                boolean connected = false;
                while(!connected){
                    connected = connect(relpClient);
                    if(!connected){
                        retriedConnects.inc();
                    }
                }
            }

            // send syslog messageCount number of times
            int sentMessages = 0;
            while (run && ++sentMessages <= messageCount) {
                send(relpClient);
            }

            // send close
            close(relpClient);
            disconnects.inc();

        }
        catch (final Exception e) {
            // todo log
            System.err.println(e.getMessage());
            run = false;
        }
    }

    private boolean connect(RelpClient relpClient) throws InterruptedException, ExecutionException{
        final boolean connected;
        final CompletableFuture<RelpFrame> open = relpClient
                .transmit(relpFrameFactory.create("open", "a hallo yo client"));
        try{
            open.get(openTimeout, TimeUnit.SECONDS);
            connected = true;
        } catch (TimeoutException timeoutException){
            return false;
        }
        return connected;
    }

    private boolean send(RelpClient relpClient){
        try {
            // start transaction and transmit timers
            Timer.Context transactionTimer = transactionLatency.time();
            Timer.Context transmitTimer = transmitLatency.time();
            final AtomicReference<Timer.Context> receiveTimer = new AtomicReference<Timer.Context>();
            // stop transmit timer as soon as relpClient.transmit() finishes and start receiveTimer.
            CompletableFuture<RelpFrame> syslog = relpClient.transmit(relpFrameFactory.create("syslog", new String(recordStream.get(), StandardCharsets.UTF_8)))
                    .handleAsync((relpFrame, exception) ->{
                        transmitTimer.close();
                        receiveTimer.set(receiveLatency.time());
                        return relpFrame;
                    });
            syslog.get(payloadTimeout, TimeUnit.SECONDS);
            // stop receiveTimer and transaction timer once the syslog future resolves.
            receiveTimer.get().close();
            transactionTimer.close();
            records.inc();
            return true;
        }
        catch (TimeoutException timeoutException) {
            return false;
        }
        catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void close(RelpClient relpClient) throws ExecutionException, InterruptedException {
        final CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
        close.get();
    }

    public void stop() {
        run = false;
    }
}
