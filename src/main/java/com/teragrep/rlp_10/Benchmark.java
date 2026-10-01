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
import com.teragrep.rlp_10.config.*;
import com.teragrep.rlp_10.report.MetricsReport;
import com.teragrep.rlp_10.report.PrometheusMetricsReport;
import com.teragrep.rlp_10.report.Slf4JMetricsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Benchmark runs a set of MetricsReports and starts an instance of BenchmarkExecution
 */
public final class Benchmark implements Callable<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);
    private final ExecutorService executorService;
    private final PrometheusConfig prometheusConfig;
    private final ReportConfig reportConfig;
    private final List<MetricsReport> reports;
    private final List<Future<Long>> executorTasks;
    private final BenchmarkExecution benchmarkExecution;
    private final Metrics metrics;

    public Benchmark(
            final InitiatorConfig initiatorConfig,
            final MetricsConfig metricsConfig,
            final PrometheusConfig prometheusConfig,
            final TimeoutConfig timeoutConfig,
            final TransportConfig transportConfig,
            final RecordStreamConfig recordStreamConfig,
            final ReportConfig reportConfig,
            final SocketAddressConfig socketAddressConfig,
            final DelayConfig delayConfig,
            final SyslogConfig syslogConfig
    ) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.prometheusConfig = prometheusConfig;
        this.reportConfig = reportConfig;
        this.reports = new ArrayList<>();
        this.executorTasks = new ArrayList<>();
        this.metrics = new Metrics(metricsConfig);
        this.benchmarkExecution = new BenchmarkExecution(
                executorService,
                metrics,
                initiatorConfig,
                socketAddressConfig,
                syslogConfig,
                recordStreamConfig,
                delayConfig,
                timeoutConfig,
                transportConfig
        );
    }

    public Long call() throws ConfigurationException, InterruptedException, ExecutionException, IOException {

        // reports
        
            final int prometheusPort = prometheusConfig.port();
            final PrometheusMetricsReport prometheusMetricsReport = new PrometheusMetricsReport(
                    metrics.registry(),
                    prometheusPort
            );
            reports.add(prometheusMetricsReport);
        }
        catch (final ConfigurationException configurationException) {
            LOGGER.error("Failed to start PrometheusServer!", configurationException);
        }
        final Slf4JMetricsReport slf4JMetricsReport = new Slf4JMetricsReport(metrics.registry(), reportConfig);
        reports.add(slf4JMetricsReport);

        for (final MetricsReport report : reports) {
            report.start();
        }

        // start eventloops
        benchmarkExecution.start();

        // shutdown hook in case JVM is terminated
        final Thread shutdownHook = new Thread(this::stopBenchmark);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // wait until execution is finished, then stop benchmark
        final long totalRecords = benchmarkExecution.awaitTermination();
        stopBenchmark();
        return totalRecords;
    }

    private void stopBenchmark() {
        benchmarkExecution.stop();
        for (final MetricsReport report : reports) {
            report.stop();
        }
        executorService.shutdown();
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
            final Benchmark benchmark = (Benchmark) o;
            equals = Objects.equals(executorService, benchmark.executorService) && Objects
                    .equals(prometheusConfig, benchmark.prometheusConfig)
                    && Objects.equals(reportConfig, benchmark.reportConfig) && Objects.equals(reports, benchmark.reports) && Objects.equals(executorTasks, benchmark.executorTasks) && Objects.equals(benchmarkExecution, benchmark.benchmarkExecution) && Objects.equals(metrics, benchmark.metrics);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        executorService, prometheusConfig, reportConfig, reports, executorTasks, benchmarkExecution,
                        metrics
                );
    }
}
