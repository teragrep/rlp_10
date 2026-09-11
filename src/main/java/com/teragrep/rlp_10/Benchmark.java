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

import com.teragrep.net_01.channel.context.ConnectContextFactory;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.channel.socket.TLSFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_10.config.*;
import com.teragrep.rlp_10.report.MetricsReport;
import com.teragrep.rlp_10.report.PrometheusMetricsReport;
import com.teragrep.rlp_10.report.Slf4JMetricsReport;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public class Benchmark {

    private final ExecutorService executorService;
    private final InitiatorConfig initiatorConfig;
    private final MetricsConfiguration metricsConfiguration;
    private final PrometheusConfiguration prometheusConfiguration;
    private final TimeoutConfiguration timeoutConfiguration;
    private final TransportConfig transportConfiguration;
    private final RecordStreamConfig recordStreamConfiguration;
    private final ReportConfig reportConfiguration;
    private final List<Initiator> initiators;
    private final List<MetricsReport> reports;
    private final List<Future> executorTasks;

    public Benchmark() {
        this(
                new InitiatorConfig(),
                new MetricsConfiguration(),
                new PrometheusConfiguration(),
                new TimeoutConfiguration(),
                new TransportConfig(),
                new RecordStreamConfig(),
                new ReportConfig()
        );
    }

    public Benchmark(
            final InitiatorConfig initiatorConfig,
            final MetricsConfiguration metricsConfiguration,
            final PrometheusConfiguration prometheusConfiguration,
            final TimeoutConfiguration timeoutConfiguration,
            final TransportConfig transportConfiguration,
            final RecordStreamConfig recordStreamConfig,
            final ReportConfig reportConfiguration
    ) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.initiatorConfig = initiatorConfig;
        this.metricsConfiguration = metricsConfiguration;
        this.prometheusConfiguration = prometheusConfiguration;
        this.timeoutConfiguration = timeoutConfiguration;
        this.transportConfiguration = transportConfiguration;
        this.recordStreamConfiguration = recordStreamConfig;
        this.reportConfiguration = reportConfiguration;
        this.initiators = new ArrayList<>(initiatorConfig.count());
        this.reports = new ArrayList<>();
        this.executorTasks = new ArrayList<>();
    }

    public void startBenchmark() {
        // todo configs

        final Metrics metrics = new Metrics(metricsConfiguration);
        final SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

        // reports
        final PrometheusMetricsReport prometheusMetricsReport = new PrometheusMetricsReport(
                metrics.registry(),
                prometheusConfiguration
        );
        final Slf4JMetricsReport slf4JMetricsReport = new Slf4JMetricsReport(metrics.registry(), reportConfiguration);
        reports.add(prometheusMetricsReport);
        reports.add(slf4JMetricsReport);

        for (final MetricsReport report : reports) {
            report.start();
        }

        // eventloop threads
        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        try {
            final EventLoop eventLoop = eventLoopFactory.create();
            executorService.submit(eventLoop);

            final SocketFactory socketFactory;

            if (transportConfiguration.tls()) {
                try {
                    final SSLContext sslContext = SSLContext.getInstance(transportConfiguration.protocol());
                    final KeyStore ks = KeyStore.getInstance("JKS");
                    final KeyStore ts = KeyStore.getInstance("JKS");

                    final File ksFile = new File(transportConfiguration.keyStorePath().toUri());
                    final File tsFile = new File(transportConfiguration.trustStorePath().toUri());

                    final FileInputStream ksFileIS = new FileInputStream(ksFile);
                    final FileInputStream tsFileIS = new FileInputStream(tsFile);
                    ts.load(tsFileIS, transportConfiguration.trustStorePassword().toCharArray());
                    final TrustManagerFactory tmf = TrustManagerFactory
                            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ts);

                    ks.load(ksFileIS, transportConfiguration.keyStorePassword().toCharArray());
                    final KeyManagerFactory kmf = KeyManagerFactory
                            .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(ks, transportConfiguration.keyStorePassword().toCharArray());
                    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                    tsFileIS.close();
                    ksFileIS.close();

                    final Function<SSLContext, SSLEngine> sslEngineFunction = context -> {
                        final SSLEngine engine = context.createSSLEngine();
                        engine.setUseClientMode(true);
                        return engine;
                    };
                    socketFactory = new TLSFactory(sslContext, sslEngineFunction);
                }
                catch (
                    final KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException
                            | UnrecoverableKeyException | KeyManagementException e
                ) {
                    // unrecoverable error
                    throw new RuntimeException("Error while initializing TLS connection, check your configuration!", e);
                }
            }
            else {
                socketFactory = new PlainFactory();
            }

            final ConnectContextFactory connectContextFactory = new ConnectContextFactory(
                    executorService,
                    socketFactory
            );

            final RelpClientFactory relpClientFactory = new RelpClientFactory(connectContextFactory, eventLoop);

            final SyslogConfig syslogConfig = new SyslogConfig();

            // todo use Hostname class from aer_02 or create new component for it
            final RecordStream recordStream = new RecordStreamImpl(
                    "someOrigin",
                    syslogConfig.hostname(),
                    syslogConfig.appName(),
                    recordStreamConfiguration.records()
            );

            final DelayConfig delayConfig = new DelayConfig();
            final RecordStream delayedStream;
            if (delayConfig.delay() > 0) {
                delayedStream = new RecordStreamDelay(delayConfig.delay(), recordStream);
            }
            else {
                delayedStream = recordStream;
            }

            for (int initiatorCount = 0; initiatorCount < initiatorConfig.count(); initiatorCount++) {
                final Initiator initiator = new Initiator(
                        relpClientFactory,
                        delayedStream,
                        socketAddressConfig.hostname(),
                        socketAddressConfig.port(),
                        metrics,
                        timeoutConfiguration.openTimeout(),
                        timeoutConfiguration.payloadTimeout(),
                        initiatorConfig.retryTransmissionCount(),
                        initiatorConfig.retryConnectionCount()
                );
                executorTasks.add(executorService.submit(initiator));
                initiators.add(initiator);
            }

            // shutdown hook in case JVM is terminated
            final Thread shutdownHook = new Thread(this::stopBenchmark);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            // block until each task is complete
            for (final Future task : executorTasks) {
                task.get();
            }
            stopBenchmark();
        }
        catch (final InterruptedException | ExecutionException | IOException e) {
            // unrecoverable exceptions
            throw new RuntimeException(e);
        }

    }

    public void stopBenchmark() {
        for (final Initiator initiator : initiators) {
            initiator.stop();
        }
        for (final MetricsReport report : reports) {
            report.stop();
        }
    }
}
