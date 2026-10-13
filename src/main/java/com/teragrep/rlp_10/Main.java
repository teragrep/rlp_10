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

import com.teragrep.cnf_01.ConfigurationException;
import com.teragrep.cnf_01.PathConfiguration;
import com.teragrep.cnf_01.PropertiesConfiguration;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.rlp_10.config.*;
import com.teragrep.rlp_10.report.MetricsReport;
import com.teragrep.rlp_10.report.PrometheusMetricsReport;
import com.teragrep.rlp_10.report.Slf4JMetricsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        final Map<String, String> configurationValues = new HashMap<>();
        final PathConfiguration pathConfiguration = new PathConfiguration(
                System.getProperty("configurationPath", "config/rlp_10.properties")
        );
        final PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();

        try {
            configurationValues.putAll(pathConfiguration.asMap());
        }
        catch (final ConfigurationException configurationException) {
            LOGGER.warn("Could not load properties from configuration path, proceeding with defaults...");
        }
        configurationValues.putAll(propertiesConfiguration.asMap());

        try {
            final ConfigFactory configFactory = new ConfigFactory(configurationValues);

            // reusable config objects
            final InitiatorConfig initiatorConfig = configFactory.initiatorConfig();
            final TimeoutConfig timeoutConfig = configFactory.timeoutConfig();
            final SocketAddressConfig socketAddressConfig = configFactory.socketAddressConfig();
            final SyslogConfig syslogConfig = configFactory.syslogConfig();
            final ReportConfig reportConfig = configFactory.reportConfig();

            // objects requiring complex initialization
            final Metrics metrics = configFactory.metricsConfig().toMetrics();
            final RecordStream recordStream = configFactory
                    .recordStreamConfig()
                    .toRecordStream(syslogConfig.hostname(), syslogConfig.appName(), configFactory.delayConfig().delay());
            final InitiatorFactory initiatorFactory = new InitiatorFactory(
                    recordStream,
                    metrics,
                    socketAddressConfig.hostname(),
                    socketAddressConfig.port(),
                    timeoutConfig.openTimeout(),
                    timeoutConfig.payloadTimeout(),
                    initiatorConfig.retryTransmissionCount(),
                    initiatorConfig.retryConnectionCount()
            );
            final SocketFactory socketFactory = configFactory.transportConfig().socketFactory();
            final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
            final BenchmarkExecution benchmarkExecution = new BenchmarkExecution(
                    executorService,
                    socketFactory,
                    initiatorFactory,
                    initiatorConfig.initiatorCount(),
                    initiatorConfig.eventLoopCount()
            );

            final List<MetricsReport> reports = new ArrayList<>();
            final Slf4JMetricsReport slf4JMetricsReport = new Slf4JMetricsReport(
                    metrics.registry(),
                    reportConfig.interval(),
                    reportConfig.rateTimeUnit(),
                    reportConfig.durationTimeUnit()
            );
            final PrometheusMetricsReport prometheusMetricsReport = new PrometheusMetricsReport(
                    metrics.registry(),
                    configFactory.prometheusConfig().port()
            );
            reports.add(slf4JMetricsReport);
            reports.add(prometheusMetricsReport);

            final Benchmark benchmark = new Benchmark(executorService, benchmarkExecution, reports);

            // shutdownhook in case JVM is terminated
            Thread shutdownThread = new Thread(benchmark::stop);
            Runtime.getRuntime().addShutdownHook(shutdownThread);

            // start benchmark and block until copmletion
            final long recordsSent = benchmark.start().get();
            System.out.println(recordsSent);
        }
        catch (final ConfigurationException configurationException) {
            LOGGER.error("Invalid configuration!", configurationException);
            System.exit(1);
        }
        catch (final InterruptedException | IOException | ExecutionException unrecoverableException) {
            LOGGER.error("An unrecoverable exception occurred!", unrecoverableException);
            System.exit(1);
        }
    }
}
