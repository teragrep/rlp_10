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
import com.teragrep.rlp_09.*;

public class Main {
    public static void main(String[] args) {
        RelpFlooderConfig relpFlooderConfig = new RelpFlooderConfig();
        relpFlooderConfig.setHostname(System.getProperty("hostname", "localhost"));
        relpFlooderConfig.setAppname(System.getProperty("appname", "rlp_10"));
        relpFlooderConfig.setTarget(System.getProperty("target", "127.0.0.1"));
        relpFlooderConfig.setPort(Integer.parseInt(System.getProperty("port", "1601")));
        relpFlooderConfig.setThreads(Integer.parseInt(System.getProperty("threads", "4")));
        relpFlooderConfig.setUseTls(Boolean.parseBoolean(System.getProperty("useTls", "false")));
        relpFlooderConfig.setPayloadSize(Integer.parseInt(System.getProperty("payloadSize", "10")));
        relpFlooderConfig.setBatchSize(Integer.parseInt(System.getProperty("batchSize", "1")));
        System.out.printf("Using hostname <[%s]>%n", relpFlooderConfig.getHostname());
        System.out.printf("Using appname <[%s]>%n", relpFlooderConfig.getAppname());
        System.out.printf("Adding <[%s]> characters to payload size making total event size <%s>%n", relpFlooderConfig.getPayloadSize(), relpFlooderConfig.getMessageLength());
        System.out.printf("Sending <[%s]> messages per batch%n", relpFlooderConfig.getBatchSize());
        System.out.printf("Sending messages to: <[%s]:[%s]>%n", relpFlooderConfig.getTarget(), relpFlooderConfig.getPort());
        System.out.printf("TLS enabled (FIXME: Implement): <[%s]>%n", relpFlooderConfig.isUseTls());

        Flooder flooder = new Flooder(relpFlooderConfig);
        Thread shutdownHook = new Thread(() -> {
            System.out.println("Shutting down...");
            flooder.stop();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            flooder.flood();
        }
        catch (Exception e){
            System.out.printf("Caught an error while flooding: <%s>%n", e.getMessage());
        }
        System.exit(0);
    }
}
