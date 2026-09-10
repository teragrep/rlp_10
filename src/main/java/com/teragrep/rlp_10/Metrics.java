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
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_10.config.MetricsConfiguration;

public class Metrics {

    private final MetricRegistry metricRegistry;
    private final Counter records;
    private final Counter resends;
    private final Counter connects;
    private final Counter disconnects;
    private final Counter retriedConnects;
    private final Timer transactionLatency;
    private final Timer transmitLatency;
    private final Timer receiveLatency;
    private final Timer connectLatency;

    public Metrics(final MetricsConfiguration metricsConfiguration) {
        // must register each metric before usage, otherwise Prometheus server would fail to report metrics that have not yet received any events
        metricRegistry = new MetricRegistry();
        records = metricRegistry.counter("records");
        resends = metricRegistry.counter("resends");
        connects = metricRegistry.counter("connects");
        disconnects = metricRegistry.counter("disconnects");
        retriedConnects = metricRegistry.counter("retriedConnects");
        transactionLatency = metricRegistry
                .timer("transactionLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        transmitLatency = metricRegistry
                .timer("transmitLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        receiveLatency = metricRegistry
                .timer("receiveLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        connectLatency = metricRegistry
                .timer("connectLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
    }

    public MetricRegistry registry() {
        return metricRegistry;
    }

    public Counter connects() {
        return connects;
    }

    public Counter disconnects() {
        return disconnects;
    }

    public Counter records() {
        return records;
    }

    public Counter resends() {
        return resends;
    }

    public Counter retriedConnects() {
        return retriedConnects;
    }

    public Timer transactionLatency() {
        return transactionLatency;
    }

    public Timer connectLatency() {
        return connectLatency;
    }

    public Timer receiveLatency() {
        return receiveLatency;
    }

    public Timer transmitLatency() {
        return transmitLatency;
    }
}
