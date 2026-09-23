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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ReportConfig {

    private final long interval;
    private final TimeUnit rateTimeUnit;
    private final TimeUnit durationTimeUnit;

    public ReportConfig() {
        this(1000, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    }

    public ReportConfig(final long intervalMs, final TimeUnit rateTimeUnit, final TimeUnit durationTimeUnit) {
        this.interval = intervalMs;
        this.rateTimeUnit = rateTimeUnit;
        this.durationTimeUnit = durationTimeUnit;
    }

    public long interval() {
        return interval;
    }

    public TimeUnit rateTimeUnit() {
        return rateTimeUnit;
    }

    public TimeUnit durationTimeUnit() {
        return durationTimeUnit;
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
            final ReportConfig that = (ReportConfig) o;
            equals = interval == that.interval && rateTimeUnit == that.rateTimeUnit
                    && durationTimeUnit == that.durationTimeUnit;
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, rateTimeUnit, durationTimeUnit);
    }
}
