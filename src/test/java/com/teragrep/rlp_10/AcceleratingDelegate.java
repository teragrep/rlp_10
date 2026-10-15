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

import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;

import java.util.concurrent.locks.LockSupport;

/**
 * Initially delays open and syslog transmissions by a configured delay, with delay rapidly decreasing for each received
 * message
 */
public class AcceleratingDelegate implements FrameDelegate {

    private final FrameDelegate origin;
    private final long initialOpenDelay;
    private final long initialSyslogDelay;
    private int openCount = 1;
    private int syslogCount = 1;

    public AcceleratingDelegate(final FrameDelegate origin, final long openDelayNanos, final long syslogDelayNanos) {
        this.origin = origin;
        this.initialOpenDelay = openDelayNanos;
        this.initialSyslogDelay = syslogDelayNanos;
    }

    @Override
    public boolean accept(final FrameContext frameContext) {
        if (frameContext.relpFrame().command().toString().equals("open")) {
            LockSupport.parkNanos(initialOpenDelay / openCount);
            openCount++;
        }
        else if (frameContext.relpFrame().command().toString().equals("syslog")) {
            LockSupport.parkNanos(initialSyslogDelay / syslogCount);
            syslogCount++;
        }
        return origin.accept(frameContext);
    }

    @Override
    public void close() throws Exception {
        origin.close();
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
