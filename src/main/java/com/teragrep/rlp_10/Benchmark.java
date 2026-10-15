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
import com.teragrep.rlp_10.report.MetricsReport;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Benchmark runs a set of MetricsReports and starts an instance of BenchmarkExecution within a given ExecutorService
 */
public final class Benchmark {

    private final ExecutorService executorService;
    private final List<MetricsReport> reports;
    private final BenchmarkExecution benchmarkExecution;

    public Benchmark(
            final ExecutorService executorService,
            final BenchmarkExecution benchmarkExecution,
            final List<MetricsReport> reports
    ) {
        this.executorService = executorService;
        this.reports = List.copyOf(reports);
        this.benchmarkExecution = benchmarkExecution;
    }

    public Future<Long> start() throws ConfigurationException, InterruptedException, ExecutionException, IOException {
        // start reports
        for (final MetricsReport report : reports) {
            report.start();
        }
        // start benchmark execution
        Future<Long> execution = executorService.submit(benchmarkExecution);
        return execution;
    }

    public void stop() {
        benchmarkExecution.stop();
        // stop reports
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
                    .equals(reports, benchmark.reports)
                    && Objects.equals(benchmarkExecution, benchmark.benchmarkExecution);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executorService, reports, benchmarkExecution);
    }
}
