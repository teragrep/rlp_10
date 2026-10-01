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

import com.teragrep.aer_02.Hostname;
import com.teragrep.rlp_10.RecordStream;
import com.teragrep.rlp_10.RecordStreamDelay;
import com.teragrep.rlp_10.RecordStreamImpl;

import java.util.Objects;

public final class RecordStreamConfig {

    private static final long maxRecords = Long.MAX_VALUE;

    private final long records;

    public RecordStreamConfig() {
        this(maxRecords);
    }

    public RecordStreamConfig(final long records) {
        this.records = records;
    }

    public long records() {
        return records;
    }

    public RecordStream recordStream(final String hostname, final String appName, final long delay) {
        final RecordStream recordStream = new RecordStreamImpl(
                new Hostname("defaultOrigin").toString(),
                hostname,
                appName,
                records
        );
        // apply delay to recordStream if configured
        final RecordStream delayedStream;
        if (delay > 0) {
            delayedStream = new RecordStreamDelay(delay, recordStream);
        }
        else {
            delayedStream = recordStream;
        }
        return delayedStream;
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
            final RecordStreamConfig that = (RecordStreamConfig) o;
            equals = records == that.records;
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(records);
    }
}
