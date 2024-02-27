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

import com.teragrep.rlp_09.RelpFlooder;
import com.teragrep.rlp_09.RelpFlooderConfig;

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public class Flooder {
    private final long startTime = Instant.now().toEpochMilli();
    private long lastReportEventsSent = 0;
    private long lastReportTime = Instant.now().toEpochMilli();
    private final RelpFlooderConfig relpFlooderConfig;
    private final RelpFlooder relpFlooder;
    private final TimerTask timerTask;
    public Flooder(RelpFlooderConfig relpFlooderConfig) {
        this.relpFlooderConfig = relpFlooderConfig;
        this.relpFlooder = new RelpFlooder(relpFlooderConfig);
        this.timerTask = new TimerTask() {
            @Override
            public void run() {
                printEps();
            }
        };
    }

    public void flood() {
        Timer statsReporter = new Timer();
        statsReporter.scheduleAtFixedRate(timerTask, 10000, 10000);
        relpFlooder.start();
    }

    void stop() {
        timerTask.cancel();
        relpFlooder.stop();
        printEps();
    }

    private void printEps() {
        long totalSent = relpFlooder.getMessagesSent();
        long deltaSent = totalSent - lastReportEventsSent;
        float totalBytes = (float) totalSent * relpFlooderConfig.getMessageLength();
        float deltaBytes = (float) deltaSent * relpFlooderConfig.getMessageLength();
        long currentTime = Instant.now().toEpochMilli();
        float totalElapsed = (float) (currentTime - startTime) / 1000;
        float deltaElapsed = (float) (currentTime - lastReportTime) / 1000;
        System.out.format(
                "Sent %,d messages / %,.2f MB in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s), total sent %,1d messages / %,.1f MB in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s)%n",
                deltaSent,
                deltaBytes / 1024 / 1024,
                deltaElapsed,
                deltaSent / deltaElapsed,
                (deltaBytes / deltaElapsed) / 1024 / 1024,
                totalSent,
                totalBytes / 1024 / 1024,
                totalElapsed,
                totalSent / totalElapsed,
                (totalBytes / totalElapsed) / 1024 / 1024
        );
        lastReportEventsSent = totalSent;
        lastReportTime = currentTime;
    }
}