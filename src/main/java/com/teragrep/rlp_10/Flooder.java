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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class Flooder {
    private final RelpFlooder relpFlooder;
    private final ConsoleReporter consoleReporter;
    private Instant startTime;
    private final int reportInterval;
    public Flooder(RelpFlooderConfig relpFlooderConfig, int reportInterval) {
        this.relpFlooder = new RelpFlooder(relpFlooderConfig);
        MetricRegistry metricRegistry = new MetricRegistry();
        // Records sent total
        metricRegistry.register(name( "records","sent", "total"), (Gauge<Integer>) relpFlooder::getTotalRecordsSent);
        metricRegistry.register(name("records", "sent", "total", "perThread"), (Gauge<HashMap<Integer, Integer>>) relpFlooder::getRecordsSentPerThread);
        // Records sent per second
        metricRegistry.register(name("records", "sent", "perSecond"), (Gauge<Float>) this::reportRecordsPerSecond);
        metricRegistry.register(name("records", "sent", "perSecond", "perThread"), (Gauge<HashMap<Integer, Float>>) this::reportRecordsPerSecondPerThread);
        // Bytes sent total
        metricRegistry.register(name("bytes", "sent", "total"), (Gauge<Integer>) relpFlooder::getTotalBytesSent);
        metricRegistry.register(name("bytes", "sent", "total", "MB"), (Gauge<Float>) this::reportTotalMegaBytesSent);
        metricRegistry.register(name("bytes", "sent", "total", "perThread"), (Gauge<HashMap<Integer, Integer>>) relpFlooder::getTotalBytesSentPerThread);
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
        float elapsed = reportElapsedSeconds();
        return String.format("%d:%02d", (int) Math.floor(elapsed/60), (int) elapsed%60);
    }
    private float reportElapsedSeconds() {
        return (Instant.now().toEpochMilli()-startTime.toEpochMilli())/1000f;
    }

    private float reportTotalMegaBytesSent() {
        return relpFlooder.getTotalBytesSent()/1024f/1024f;
    }


    private float reportMegaBytesSentPerSecond() {
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalBytesSent()/1024f/1024f/elapsed;
    }

    private Float reportRecordsPerSecond() {
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalRecordsSent()/elapsed;
    }

    private HashMap<Integer, Float> reportRecordsPerSecondPerThread() {
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli())/1000f;
        HashMap<Integer, Float> recordsPerThread = new HashMap<>();
        for(Map.Entry<Integer, Integer> entry : relpFlooder.getRecordsSentPerThread().entrySet()) {
            recordsPerThread.put(entry.getKey(), entry.getValue()/elapsed);
        }
        return recordsPerThread;
    }

    private Float reportBytesPerSecond() {
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli()) / 1000f;
        return relpFlooder.getTotalBytesSent()/elapsed;
    }

    private HashMap<Integer, Float> reportBytesPerSecondPerThread() {
        Instant now = Instant.now();
        float elapsed = (now.toEpochMilli() - startTime.toEpochMilli())/1000f;
        HashMap<Integer, Float> bytesPerThread = new HashMap<>();
        for(Map.Entry<Integer, Integer> entry : relpFlooder.getTotalBytesSentPerThread().entrySet()) {
            bytesPerThread.put(entry.getKey(), entry.getValue()/elapsed);
        }
        return bytesPerThread;
    }

    public void flood() {
        startTime = Instant.now();
        consoleReporter.start(reportInterval, TimeUnit.SECONDS);
        relpFlooder.start();
    }

    void stop() throws InterruptedException {
        relpFlooder.stop();
        consoleReporter.stop();
    }
}