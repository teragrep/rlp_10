/*
 * Teragrep RELP Flooder Client RLP_10
 * Copyright (C) 2024  Suomen Kanuuna Oy
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
 * along with this program.  If not, see <https://github.com/teragrep/teragrep/blob/main/LICENSE>.
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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.teragrep.rlp_09.RelpFlooder;
import com.teragrep.rlp_09.RelpFlooderConfig;
import com.teragrep.rlp_09.RelpFlooderIteratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

class Flooder {
    private final RelpFlooder relpFlooder;
    private final ConsoleReporter consoleReporter;
    private Instant startTime;
    private final int reportInterval;
    private static final Logger LOGGER = LoggerFactory.getLogger(Flooder.class);
    public Flooder(RelpFlooderConfig relpFlooderConfig, RelpFlooderIteratorFactory relpFlooderIteratorFactory, int reportInterval) {
        this.relpFlooder = new RelpFlooder(relpFlooderConfig, relpFlooderIteratorFactory);
        MetricRegistry metricRegistry = new MetricRegistry();
        // Records sent total
        metricRegistry.register(name( "records","sent", "total"), (Gauge<Long>) relpFlooder::getTotalRecordsSent);
        metricRegistry.register(name("records", "sent", "total", "perThread"), (Gauge<HashMap<Integer, Long>>) relpFlooder::getRecordsSentPerThread);
        // Records sent per second
        metricRegistry.register(name("records", "sent", "perSecond"), (Gauge<Float>) this::reportRecordsPerSecond);
        metricRegistry.register(name("records", "sent", "perSecond", "perThread"), (Gauge<HashMap<Integer, Float>>) this::reportRecordsPerSecondPerThread);
        // Bytes sent total
        metricRegistry.register(name("bytes", "sent", "total"), (Gauge<Long>) relpFlooder::getTotalBytesSent);
        metricRegistry.register(name("bytes", "sent", "total", "MB"), (Gauge<Float>) this::reportTotalMegaBytesSent);
        metricRegistry.register(name("bytes", "sent", "total", "perThread"), (Gauge<HashMap<Integer, Long>>) relpFlooder::getTotalBytesSentPerThread);
        // Bytes second record
        metricRegistry.register(name("bytes", "sent", "perSecond"), (Gauge<Float>) this::reportBytesPerSecond);
        metricRegistry.register(name("bytes", "sent", "perSecond", "MB"), (Gauge<Float>) this::reportMegaBytesSentPerSecond);
        metricRegistry.register(name("bytes", "sent", "perSecond", "perThread"), (Gauge<HashMap<Integer, Float>>) this::reportBytesPerSecondPerThread);
        // Elapsed
        metricRegistry.register(name("time", "elapsed"), (Gauge<String>) this::reportElapsed);
        metricRegistry.register(name("time", "elapsed", "seconds"), (Gauge<Float>) this::reportElapsedSeconds);
        this.consoleReporter = ConsoleReporter
                .forRegistry(metricRegistry)
                .build();
        this.reportInterval = reportInterval;
    }

    private String reportElapsed() {
        LOGGER.trace("Reporting Elapsed");
        float elapsed = reportElapsedSeconds();
        return String.format("%d:%02d", (int) Math.floor(elapsed/60), (int) elapsed%60);
    }
    private float reportElapsedSeconds() {
        LOGGER.trace("Reporting ElapsedSeconds");
        return (Instant.now().toEpochMilli()-startTime.toEpochMilli())/1000f;
    }

    private float reportTotalMegaBytesSent() {
        LOGGER.trace("Reporting TotalMegabytesSent");
        return relpFlooder.getTotalBytesSent()/1024f/1024f;
    }


    private float reportMegaBytesSentPerSecond() {
        LOGGER.trace("Reporting TotalMegabytesSentPerSecond");
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalBytesSent()/1024f/1024f/elapsed;
    }

    private Float reportRecordsPerSecond() {
        LOGGER.trace("Reporting RecordsPerSecond");
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalRecordsSent()/elapsed;
    }

    private HashMap<Integer, Float> reportRecordsPerSecondPerThread() {
        LOGGER.trace("Reporting RecordsPerSecondPerThread");
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli())/1000f;
        HashMap<Integer, Float> recordsPerThread = new HashMap<>();
        for(Map.Entry<Integer, Long> entry : relpFlooder.getRecordsSentPerThread().entrySet()) {
            int key = entry.getKey();
            float value = entry.getValue()/elapsed;
            LOGGER.debug("Adding key <{}>, value <{}> to RecordsPerSecondPerThread", key, value);
            recordsPerThread.put(key, value);
        }
        return recordsPerThread;
    }

    private Float reportBytesPerSecond() {
        LOGGER.trace("Reporting BytesPerSecond");
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalBytesSent()/elapsed;
    }

    private HashMap<Integer, Float> reportBytesPerSecondPerThread() {
        LOGGER.trace("Reporting BytesPerSecondPerThread");
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli())/1000f;
        HashMap<Integer, Float> bytesPerThread = new HashMap<>();
        for(Map.Entry<Integer, Long> entry : relpFlooder.getTotalBytesSentPerThread().entrySet()) {
            int key = entry.getKey();
            float value = entry.getValue()/elapsed;
            LOGGER.debug("Adding key <{}>, value <{}> to BytesPerSecondPerThread", key, value);
            bytesPerThread.put(key, value);
        }
        return bytesPerThread;
    }

    public void flood() {
        LOGGER.trace("Entering flood()");
        startTime = Instant.now();
        consoleReporter.start(reportInterval, TimeUnit.SECONDS);
        LOGGER.trace("Running relpFlooder.start()");
        relpFlooder.start();
        LOGGER.trace("Exiting flood()");
    }

    void stop() {
        LOGGER.trace("Entering stop()");
        LOGGER.trace("Stopping RelpFlooder");
        relpFlooder.stop();
        LOGGER.trace("Stopping ConsoleReporter");
        consoleReporter.stop();
        LOGGER.trace("Exiting stop()");
    }
}