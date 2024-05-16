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

import com.teragrep.rlp_09.RelpFlooderConfig;
import com.teragrep.rlp_09.RelpFlooderIteratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        FlooderConfig flooderConfig = new FlooderConfig();
        RelpFlooderConfig relpFlooderConfig = new RelpFlooderConfig(flooderConfig.target, flooderConfig.port, flooderConfig.threads, flooderConfig.connectTimeout, flooderConfig.waitForAcks);
        LOGGER.info("Using hostname <[{}]>", flooderConfig.hostname);
        LOGGER.info("Using appname <[{}]>", flooderConfig.appname);
        LOGGER.info("Adding <[{}]> characters to payload size", flooderConfig.payloadSize);
        LOGGER.info("Sending records to: <[{}]:[{}]>", flooderConfig.target, flooderConfig.port);
        LOGGER.info("Using <[{}]> mode with <[{}]> threads", flooderConfig.mode, flooderConfig.threads);
        RelpFlooderIteratorFactory relpFlooderIteratorFactory;
        switch(flooderConfig.mode) {
            case "simple":
                LOGGER.info(
                        "Sending <[{}]> static records per thread, total of <[{}]> records",
                        flooderConfig.maxRecordsSent < 0 ? "infinite" : flooderConfig.maxRecordsSent,
                        flooderConfig.maxRecordsSent < 0 ? "infinite" : flooderConfig.maxRecordsSent * flooderConfig.threads
                );
                relpFlooderIteratorFactory = new SimpleRecordIteratorFactory(flooderConfig);
                break;
            case "dynamic":
                LOGGER.info(
                        "Sending <[{}]> dynamic records per thread, total of <[{}]> records",
                        flooderConfig.maxRecordsSent < 0 ? "infinite" : flooderConfig.maxRecordsSent,
                        flooderConfig.maxRecordsSent < 0 ? "infinite" : flooderConfig.maxRecordsSent * flooderConfig.threads
                );
                relpFlooderIteratorFactory = new PerThreadRecordIteratorFactory(flooderConfig);
                break;
            case "dynamic-shared":
                LOGGER.info(
                        "Sending total of <[{}]> dynamic records across all threads",
                        flooderConfig.maxRecordsSent < 0 ? "infinite" : flooderConfig.maxRecordsSent * flooderConfig.threads
                );
                relpFlooderIteratorFactory = new SharedTotalRecordIteratorFactory(flooderConfig);
                break;
            default:
                LOGGER.error("Invalid mode <[{}]> selected", flooderConfig.mode);
                throw new IllegalStateException("Unexpected mode: " + flooderConfig.mode);
        }
        Flooder flooder = new Flooder(relpFlooderConfig, relpFlooderIteratorFactory, flooderConfig.reportInterval);
        LOGGER.info("Waiting for acks: <[{}]>", flooderConfig.waitForAcks);
        LOGGER.info("TLS enabled (FIXME: Implement): <[{}]>", flooderConfig.useTls);
        LOGGER.info("Reporting stats every <[{}]> seconds", flooderConfig.reportInterval);

        Thread shutdownHook = new Thread(() -> {
            LOGGER.info("Shutting down...");
            try {
                flooder.stop();
            } catch (InterruptedException | RuntimeException e) {
                LOGGER.error("Failed to stop properly: <{}>", e.getMessage());
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            flooder.flood();
        }
        catch (Exception e){
            LOGGER.error("Caught an error while flooding: <{}>", e.getMessage());
        }
        System.exit(0);
    }
}
