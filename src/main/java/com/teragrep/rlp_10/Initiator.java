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
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class Initiator implements Callable<Long> {

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
    private final AtomicLong recordsSent;

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
        this.recordsSent = new AtomicLong(); // todo move
    }

    @Override
    public Long call() {
        try {
            MeteredRelpClient meteredRelpClient = new RetryingMeteredRelpClient(
                    new TimeoutMeteredRelpClient(
                            new MeteredRelpClientImpl(
                                    relpClientFactory,
                                    relpFrameFactory,
                                    recordStream,
                                    hostname,
                                    port,
                                    metrics
                            ),
                            openTimeout,
                            payloadTimeout
                    ),
                    metrics,
                    retryConnectCount,
                    retryTransmissionCount
            );
            meteredRelpClient.connect();
            Timer.Context connectTimer = metrics.connectLatency().time();
            CompletableFuture<RelpFrame> openFrame = meteredRelpClient.transmitOpen();
            meteredRelpClient.completeOpen(openFrame);
            connectTimer.close();
            while (run) {
                final String payload = new String(recordStream.get(), StandardCharsets.UTF_8); // todo recordStream should return a stubable SyslogMessage, currently stubness is represented by empty bytearray
                if (payload.isEmpty()) {
                    stop();
                    break;
                }
                Timer.Context transactionTimer = metrics.transactionLatency().time();
                CompletableFuture<RelpFrame> syslogFrame = meteredRelpClient.transmitSyslog(payload);
                meteredRelpClient.completeSyslog(syslogFrame, payload);
                transactionTimer.close();
                recordsSent.incrementAndGet();
            }
            meteredRelpClient.close();
        }
        catch (ExecutionException | InterruptedException e) {
            LOGGER.error("unrecoverable error");
        }
        return recordsSent.get();
    }

    public void stop() {
        run = false;
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
            final Initiator initiator = (Initiator) o;
            equals = port == initiator.port && openTimeout == initiator.openTimeout
                    && payloadTimeout == initiator.payloadTimeout
                    && retryTransmissionCount == initiator.retryTransmissionCount
                    && retryConnectCount == initiator.retryConnectCount && run == initiator.run && Objects
                            .equals(relpClientFactory, initiator.relpClientFactory)
                    && Objects.equals(recordStream, initiator.recordStream) && Objects.equals(metrics, initiator.metrics) && Objects.equals(hostname, initiator.hostname) && Objects.equals(recordsSent, initiator.recordsSent);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        relpClientFactory, recordStream, metrics, hostname, port, openTimeout, payloadTimeout,
                        retryTransmissionCount, retryConnectCount, run, recordsSent
                );
    }
}
