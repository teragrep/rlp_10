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
package com.teragrep.rlp_10.config;

import com.teragrep.rlp_10.exception.ConfigurationException;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConfigFactory {

    private final Map<String, String> configValues;
    private final Path baseDirectory;

    public ConfigFactory(Path baseDirectory, final Map<String, String> configValues) {
        this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
        this.configValues = Collections.unmodifiableMap(configValues);
    }

    public DelayConfig delayConfig() {
        String configuredDuration = configValues.getOrDefault("delay.duration", "PT0S");
        try {
            Duration delayDuration = Duration.parse(configuredDuration);
            return new DelayConfig(delayDuration);
        }
        catch (DateTimeParseException dateTimeParseException) {
            throw new ConfigurationException(
                    "DelayConfig contains invalid configuration value!",
                    dateTimeParseException
            );
        }
    }

    public InitiatorConfig initiatorConfig() {
        String configuredInitiatorCount = configValues.getOrDefault("initiator.count", "1");
        String configuredEventloopCount = configValues.getOrDefault("initiator.eventloopcount", "1");
        String configuredRetryTransmissionCount = configValues.getOrDefault("initiator.retrytransmissioncount", "3");
        String configuredRetryConnectionCount = configValues.getOrDefault("initiator.retryconnectioncount", "3");
        try {
            int initiatorCount = Integer.parseInt(configuredInitiatorCount);
            int eventloopCount = Integer.parseInt(configuredEventloopCount);
            int retryTransmissionCount = Integer.parseInt(configuredRetryTransmissionCount);
            int retryConnectionCount = Integer.parseInt(configuredRetryConnectionCount);
            return new InitiatorConfig(initiatorCount, retryTransmissionCount, retryConnectionCount, eventloopCount);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "InititatorConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public MetricsConfig metricsConfig() {
        try {
            String configuredWindow = configValues.getOrDefault("metrics.window", "10000");
            int window = Integer.parseInt(configuredWindow);
            return new MetricsConfig(window);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "MetricsConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public PayloadConfig payloadConfig() { // todo: remove is no usages
        String configuredpayloadLength = configValues.getOrDefault("payload.length", "0");
        try {
            int payloadLength = Integer.parseInt(configuredpayloadLength);
            return new PayloadConfig(payloadLength);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "PayloadConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public PrometheusConfig prometheusConfig() {
        String configuredPort = configValues.getOrDefault("prometheus.port", "8080");
        try {
            int port = Integer.parseInt(configuredPort);
            return new PrometheusConfig(port);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "PrometheusConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public RecordStreamConfig recordStreamConfig() {
        String configuredRecords = configValues.getOrDefault("recordstream.records", String.valueOf(Long.MAX_VALUE));
        try {
            long records = Long.parseLong(configuredRecords);
            return new RecordStreamConfig(records);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "RecordStreamConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public ReportConfig reportConfig() {
        String configuredInterval = configValues.getOrDefault("report.interval", "1000");
        String configuredRateTimeUnit = configValues.getOrDefault("report.ratetimeunit", "SECONDS");
        String configuredDurationTimeUnit = configValues.getOrDefault("report.durationtimeunit", "MILLISECONDS");
        try {
            long interval = Long.parseLong(configuredInterval);
            TimeUnit rateTimeUnit = TimeUnit.valueOf(configuredRateTimeUnit);
            TimeUnit durationTimeUnit = TimeUnit.valueOf(configuredDurationTimeUnit);
            return new ReportConfig(interval, rateTimeUnit, durationTimeUnit);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                    "ReportConfig contains invalid configuration value!",
                    illegalArgumentException
            );
        }
    }

    public SocketAddressConfig socketAddressConfig() {
        String configuredHostname = configValues.getOrDefault("socket.hostname", "localhost");
        String configuredPort = configValues.getOrDefault("socket.port", "8080");
        try {
            int port = Integer.parseInt(configuredPort);
            return new SocketAddressConfig(configuredHostname, port);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "SocketAddressConfig contains invalid configuration value!",
                    numberFormatException
            );

        }
    }

    public SyslogConfig syslogConfig() {
        String configuredHostname = configValues.getOrDefault("syslog.hostname", "localhost");
        String configuredAppName = configValues.getOrDefault("syslog.appname", "appName");
        return new SyslogConfig(configuredHostname, configuredAppName);
    }

    public TimeoutConfig timeoutConfig() {
        String configuredOpenTimeout = configValues.getOrDefault("timeout.open", "10");
        String configuredPayloadTimeout = configValues.getOrDefault("timeout.payload", "5");
        try {
            long openTimeout = Long.parseLong(configuredOpenTimeout);
            long payloadTimeout = Long.parseLong(configuredPayloadTimeout);
            return new TimeoutConfig(openTimeout, payloadTimeout);
        }
        catch (NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "TimeoutConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public TransportConfig transportConfig() {
        String configuredTls = configValues.getOrDefault("transport.tls", "false");
        String configuredKeystorePath = configValues.getOrDefault("transport.keystorepath", "tls/keystore.jks");
        String configuredTruststorePath = configValues.getOrDefault("transport.truststorepath", "tls/truststore.jks");
        Path keystorePath = baseDirectory.resolve(configuredKeystorePath).normalize();
        Path truststorePath = baseDirectory.resolve(configuredTruststorePath).normalize();
        String configuredKeystorePassword = configValues.getOrDefault("transport.keystorepassword", "changeit");
        String configuredTruststorePassword = configValues.getOrDefault("transport.truststorepassword", "changeit");
        String protocol = configValues.getOrDefault("transport.protocol", "TLSv1.3");
        // Detect path traversal
        if (!keystorePath.startsWith(baseDirectory)) {
            throw new ConfigurationException("Invalid keystore path!");
        }
        if (!truststorePath.startsWith(baseDirectory)) {
            throw new ConfigurationException("Invalid truststore path!");
        }
        boolean tls = Boolean.getBoolean(configuredTls);
        return new TransportConfig(
                tls,
                keystorePath,
                truststorePath,
                configuredKeystorePassword,
                configuredTruststorePassword,
                protocol
        );
    }
}
