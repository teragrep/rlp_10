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

class FlooderConfig {
    public final String hostname;
    public final String appname;
    public final String target;
    public final int port;
    public final int threads;
    public final boolean useTls;
    public final int payloadSize;
    public final int reportInterval;
    public final long maxMessagesSent;
    public final boolean usePerThreadIterator;
    public FlooderConfig() {
        this.hostname = System.getProperty("hostname", "localhost");
        this.appname = System.getProperty("appname", "rlp_10");
        this.target = System.getProperty("target", "127.0.0.1");
        this.port = Integer.parseInt(System.getProperty("port", "1601"));
        this.threads = Integer.parseInt(System.getProperty("threads", "4"));
        this.useTls = Boolean.parseBoolean(System.getProperty("useTls", "false"));
        this.payloadSize = Integer.parseInt(System.getProperty("payloadSize", "10"));
        this.reportInterval = Integer.parseInt(System.getProperty("reportInterval", "10"));
        this.maxMessagesSent = Long.parseLong(System.getProperty("maxMessagesSent", "-1"));
        this.usePerThreadIterator = Boolean.parseBoolean(System.getProperty("usePerThreadIterator", "true"));
    }
}