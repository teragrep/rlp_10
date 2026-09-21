package com.teragrep.rlp_10.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class ConfigFactoryTest {
    Path baseTlsDirectory = Path.of("src/test/resources/tls");
    @Test
    void testDefaultConfig() {
        Map<String,String> configMap = new HashMap<>();
        ConfigFactory configFactory = new ConfigFactory(baseTlsDirectory,configMap);

        // you should be able to create every kind of configuration object.
        InitiatorConfig initiatorConfig = Assertions.assertDoesNotThrow(()->configFactory.initiatorConfig());
        RecordStreamConfig recordStreamConfig = Assertions.assertDoesNotThrow(()->configFactory.recordStreamConfig());
        DelayConfig delayConfig = Assertions.assertDoesNotThrow(()->configFactory.delayConfig());
        MetricsConfig metricsConfig = Assertions.assertDoesNotThrow(()->configFactory.metricsConfig());
        PrometheusConfig prometheusConfig = Assertions.assertDoesNotThrow(()->configFactory.prometheusConfig());
        ReportConfig reportConfig = Assertions.assertDoesNotThrow(()->configFactory.reportConfig());
        SocketAddressConfig socketAddressConfig = Assertions.assertDoesNotThrow(()->configFactory.socketAddressConfig());
        SyslogConfig syslogConfig = Assertions.assertDoesNotThrow(()->configFactory.syslogConfig());
        TimeoutConfig timeoutConfig = Assertions.assertDoesNotThrow(()->configFactory.timeoutConfig());
        TransportConfig transportConfig = Assertions.assertDoesNotThrow(()->configFactory.transportConfig());
        PayloadConfig payloadConfig = Assertions.assertDoesNotThrow(()->configFactory.payloadConfig());

        // every configuration object should contain valid values (eg. PrometheusConfig should not have a 'port' value that is out of range when using default configs, which would throw an Exception)
        Assertions.assertDoesNotThrow(()->initiatorConfig.initiatorCount());
        Assertions.assertDoesNotThrow(()->initiatorConfig.eventLoopCount());
        Assertions.assertDoesNotThrow(()->initiatorConfig.retryConnectionCount());
        Assertions.assertDoesNotThrow(()->initiatorConfig.retryTransmissionCount());
        Assertions.assertDoesNotThrow(()->recordStreamConfig.records());
        Assertions.assertDoesNotThrow(()->delayConfig.delay());
        Assertions.assertDoesNotThrow(()->metricsConfig.window());
        Assertions.assertDoesNotThrow(()->prometheusConfig.port());
        Assertions.assertDoesNotThrow(()->reportConfig.interval());
        Assertions.assertDoesNotThrow(()->reportConfig.durationTimeUnit());
        Assertions.assertDoesNotThrow(()->reportConfig.rateTimeUnit());
        Assertions.assertDoesNotThrow(()->socketAddressConfig.port());
        Assertions.assertDoesNotThrow(()->socketAddressConfig.hostname());
        Assertions.assertDoesNotThrow(()->syslogConfig.hostname());
        Assertions.assertDoesNotThrow(()->syslogConfig.appName());
        Assertions.assertDoesNotThrow(()->timeoutConfig.openTimeout());
        Assertions.assertDoesNotThrow(()->timeoutConfig.payloadTimeout());
        Assertions.assertDoesNotThrow(()->transportConfig.tls());
        Assertions.assertDoesNotThrow(()->transportConfig.keyStoreFile());
        Assertions.assertDoesNotThrow(()->transportConfig.keyStorePassword());
        Assertions.assertDoesNotThrow(()->transportConfig.trustStoreFile());
        Assertions.assertDoesNotThrow(()->transportConfig.trustStorePassword());
        Assertions.assertDoesNotThrow(()->transportConfig.protocol());
    }

    @Test
    void testCustomConfig() {
        Map<String,String> configMap = new HashMap<>();
        configMap.put("initiator.count","10");
        configMap.put("recordstream.records","100000");
        configMap.put("delay.duration","PT1S");
        configMap.put("metrics.window","2000");
        configMap.put("prometheus.port","8000");
        configMap.put("report.interval","60000");
        configMap.put("socket.hostname","127.0.0.1");
        configMap.put("syslog.appname","testApp");
        configMap.put("timeout.open","1");
        configMap.put("transport.tls","true");
        ConfigFactory configFactory = new ConfigFactory(baseTlsDirectory,configMap);

        // you should be able to create every kind of configuration object.
        InitiatorConfig initiatorConfig = Assertions.assertDoesNotThrow(()->configFactory.initiatorConfig());
        RecordStreamConfig recordStreamConfig = Assertions.assertDoesNotThrow(()->configFactory.recordStreamConfig());
        DelayConfig delayConfig = Assertions.assertDoesNotThrow(()->configFactory.delayConfig());
        MetricsConfig metricsConfig = Assertions.assertDoesNotThrow(()->configFactory.metricsConfig());
        PrometheusConfig prometheusConfig = Assertions.assertDoesNotThrow(()->configFactory.prometheusConfig());
        ReportConfig reportConfig = Assertions.assertDoesNotThrow(()->configFactory.reportConfig());
        SocketAddressConfig socketAddressConfig = Assertions.assertDoesNotThrow(()->configFactory.socketAddressConfig());
        SyslogConfig syslogConfig = Assertions.assertDoesNotThrow(()->configFactory.syslogConfig());
        TimeoutConfig timeoutConfig = Assertions.assertDoesNotThrow(()->configFactory.timeoutConfig());
        TransportConfig transportConfig = Assertions.assertDoesNotThrow(()->configFactory.transportConfig());
        PayloadConfig payloadConfig = Assertions.assertDoesNotThrow(()->configFactory.payloadConfig());

        // every configuration object should contain valid values (eg. PrometheusConfig should not have a 'port' value that is out of range when using default configs, which would throw an Exception)
        Assertions.assertEquals(10,initiatorConfig.initiatorCount());
        Assertions.assertEquals(1,initiatorConfig.eventLoopCount());
        Assertions.assertEquals(3,initiatorConfig.retryConnectionCount());
        Assertions.assertEquals(3,initiatorConfig.retryTransmissionCount());
        Assertions.assertEquals(100000,recordStreamConfig.records());
        Assertions.assertEquals(1000000000,delayConfig.delay());
        Assertions.assertEquals(2000,metricsConfig.window());
        Assertions.assertEquals(8000,prometheusConfig.port());
        Assertions.assertEquals(60000,reportConfig.interval());
        Assertions.assertEquals(TimeUnit.MILLISECONDS,reportConfig.durationTimeUnit());
        Assertions.assertEquals(TimeUnit.SECONDS,reportConfig.rateTimeUnit());
        Assertions.assertEquals("127.0.0.1",socketAddressConfig.hostname());
        Assertions.assertEquals(1601,socketAddressConfig.port());
        Assertions.assertEquals("testApp",syslogConfig.appName());
        Assertions.assertEquals("localhost",syslogConfig.hostname());
        Assertions.assertEquals(1,timeoutConfig.openTimeout());
        Assertions.assertEquals(5,timeoutConfig.payloadTimeout());
        Assertions.assertEquals(true,transportConfig.tls());
        Assertions.assertEquals("changeit",transportConfig.keyStorePassword());
        Assertions.assertEquals("changeit",transportConfig.trustStorePassword());
        Assertions.assertEquals(Paths.get("src/test/resources/tls/keystore.jks").toAbsolutePath().toFile(),transportConfig.keyStoreFile());
        Assertions.assertEquals(Paths.get("src/test/resources/tls/truststore.jks").toAbsolutePath().toFile(),transportConfig.trustStoreFile());
        Assertions.assertEquals("TLSv1.3",transportConfig.protocol());
    }

}