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
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;
import com.teragrep.rlp_10.exception.TransmissionException;
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
    private final Metrics metrics;
    private final String hostname;
    private final int port;
    private final long openTimeout;
    private final long payloadTimeout;
    private final int retryTransmissionCount;
    private final int retryConnectCount;

    private volatile boolean run = true;

    //TODO: All initiators are currently in one eventLoop, allow for multiples.
    public Initiator(
            final RelpClientFactory relpClientFactory,
            final RecordStream recordStream,
            final Metrics metrics,
            final long openTimeout,
            final long payloadTimeout,
            final int retryTransmissionCount,
            final int retryConnectionCount
    ) {
        this(
                relpClientFactory,
                recordStream,
                "localhost",
                1601,
                metrics,
                openTimeout,
                payloadTimeout,
                retryTransmissionCount,
                retryConnectionCount
        );
    }

    public Initiator(
            final RelpClientFactory relpClientFactory,
            final RecordStream recordStream,
            final String hostName,
            final int port,
            final Metrics metrics,
            final long connectTimeout,
            final long payloadTimeout,
            final int retryTransmissionCount,
            final int retryConnectCount
    ) {
        this.relpClientFactory = relpClientFactory;
        this.recordStream = recordStream;
        this.hostname = hostName;
        this.port = port;
        this.metrics = metrics;
        this.openTimeout = connectTimeout;
        this.payloadTimeout = payloadTimeout;
        this.retryTransmissionCount = retryTransmissionCount;
        this.retryConnectCount = retryConnectCount;
    }

    @Override
    public void run() {
        // producer threads

        try (
                final RelpClient relpClient = relpClientFactory.open(new InetSocketAddress(hostname, port)).get(openTimeout, TimeUnit.SECONDS);
        ) {
            // try to connect, retrying until connection is established or a configured retry limit is reached
            try (final Timer.Context timerContext = metrics.connectLatency().time()) {
                if (!connect(relpClient, retryConnectCount)) {
                    LOGGER.error("Failed to connect to server! Stopping...");
                    stop();
                }
                metrics.connects().inc();
            }

            // send syslog messageCount number of times
            while (run) {
                if (!send(relpClient, retryTransmissionCount)) {
                    LOGGER.error("Failed to send syslog message! Stopping...");
                    stop();
                }
            }
            // send close
            close(relpClient);
        }
        catch (TimeoutException timeoutException) {
            throw new RuntimeException("RelpClient was not initialized within "+openTimeout+" seconds!",timeoutException);
        }
        catch (ExecutionException | InterruptedException exception) {
            throw new RuntimeException("An unrecoverable error occurred while running Initiator",exception);
        }
    }

    private boolean connect(final RelpClient relpClient, final int retryCount)
            throws InterruptedException, ExecutionException {
        int retries = 0;
        boolean connected = connect(relpClient);
        while (!connected && retries < retryCount) {
            retries++;
            metrics.retriedConnects().inc();
            connected = connect(relpClient);
        }
        return connected;
    }

    private boolean connect(final RelpClient relpClient) throws InterruptedException, ExecutionException {
        final boolean connected;
        final CompletableFuture<RelpFrame> open = relpClient
                .transmit(relpFrameFactory.create("open", "a hallo yo client"));
        try {
            open.get(openTimeout, TimeUnit.SECONDS);
            connected = true;
        }
        catch (final TimeoutException timeoutException) {
            open.cancel(false);
            LOGGER.warn("Connection attempt timeout after {} seconds!", openTimeout);
            return false;
        }
        return connected;
    }

    private boolean send(final RelpClient relpClient, final int retryCount) throws InterruptedException, ExecutionException {
        int retries = 0;
        boolean sent = send(relpClient);
        while (!sent && retries < retryCount) {
            retries++;
            metrics.resends().inc();
            sent = send(relpClient);
        }
        return sent;
    }

    private boolean send(final RelpClient relpClient) throws InterruptedException, ExecutionException{
        try {
            // start transaction and transmit timers
            final Timer.Context transactionTimer = metrics.transactionLatency().time();
            final Timer.Context transmitTimer = metrics.transmitLatency().time();
            final AtomicReference<Timer.Context> receiveTimer = new AtomicReference<Timer.Context>(); // AtomicReference to deal with variables needing to be final in lambdas
            // stop transmit timer as soon as relpClient.transmit() finishes and start receiveTimer.
            final String payload = new String(recordStream.get(), StandardCharsets.UTF_8); // todo recordStream should return a stubable SyslogMessage
            if (payload.isEmpty()) {
                stop();
                return true;
            }
            else {
                final CompletableFuture<RelpFrame> syslog = relpClient
                        .transmit(relpFrameFactory.create("syslog", payload))
                        .handleAsync((relpFrame, exception) -> {
                            transmitTimer.close();
                            if (exception != null) {
                                // transmission failed, close transaction timer and throw error.
                                transactionTimer.close();
                                throw new TransmissionException(exception);
                            }
                            receiveTimer.set(metrics.receiveLatency().time());
                            return relpFrame;
                        });
                syslog.get(payloadTimeout, TimeUnit.SECONDS);
                // stop receiveTimer and transaction timer once the syslog future resolves.
                receiveTimer.get().close();
                transactionTimer.close();
                metrics.records().inc();
                return true;
            }
        }
        catch (final TimeoutException timeoutException) {
            LOGGER.warn("Send syslog attempt timeout after {} seconds!", payloadTimeout);
            return false;
        }
    }

    private void close(final RelpClient relpClient) throws InterruptedException, ExecutionException {
        final CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
        metrics.disconnects().inc();
        close.get();
    }

    public void stop() {
        run = false;
    }
}
