package com.teragrep.rlp_10;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_10.config.MetricsConfiguration;

public class Metrics {
    private final MetricRegistry metricRegistry;
    private final MetricsConfiguration metricsConfiguration;
    private final Counter records;
    private final Counter resends;
    private final Counter connects;
    private final Counter disconnects;
    private final Counter retriedConnects;
    private final Timer transactionLatency;
    private final Timer transmitLatency;
    private final Timer receiveLatency;
    private final Timer connectLatency;

    public Metrics(MetricsConfiguration metricsConfiguration){
        this.metricsConfiguration = metricsConfiguration;
        metricRegistry = new MetricRegistry();
        records = metricRegistry.counter("records");
        resends = metricRegistry.counter("resends");
        connects = metricRegistry.counter("connects");
        disconnects = metricRegistry.counter("disconnects");
        retriedConnects = metricRegistry.counter("retriedConnects");
        transactionLatency = metricRegistry.timer("transactionLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        transmitLatency = metricRegistry.timer("transmitLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        receiveLatency = metricRegistry.timer("receiveLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        connectLatency = metricRegistry.timer("connectLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
    }

    public MetricRegistry registry(){
        return metricRegistry;
    }

    public MetricsConfiguration configuration(){
        return metricsConfiguration;
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
