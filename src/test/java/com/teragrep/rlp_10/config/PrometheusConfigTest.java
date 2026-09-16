package com.teragrep.rlp_10.config;

import com.teragrep.rlp_10.exception.ConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PrometheusConfigTest {

    @Test
    void testValidPort() {
        final int expectedPort = 8080;
        final PrometheusConfig prometheusConfig = new PrometheusConfig(expectedPort);
        final int port = Assertions.assertDoesNotThrow(()->prometheusConfig.port());
        Assertions.assertEquals(expectedPort, port);
    }

    @Test
    void testInvalidPort() {
        final int invalidPort = 65536;
        final PrometheusConfig prometheusConfig = new PrometheusConfig(invalidPort);
        Assertions.assertThrows(ConfigurationException.class,()->prometheusConfig.port());
    }
}