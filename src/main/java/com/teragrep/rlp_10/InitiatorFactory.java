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

import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_10.config.InitiatorConfig;
import com.teragrep.rlp_10.config.SocketAddressConfig;
import com.teragrep.rlp_10.config.TimeoutConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * InitiatorFactory creates a configured number of Initiators for a single EventLoop
 */

public final class InitiatorFactory {

    private final RecordStream recordStream;
    private final SocketAddressConfig socketAddressConfig;
    private final Metrics metrics;
    private final TimeoutConfig timeoutConfig;
    private final InitiatorConfig initiatorConfig;

    public InitiatorFactory(
            final RecordStream recordStream,
            final Metrics metrics,
            final SocketAddressConfig socketAddressConfig,
            final TimeoutConfig timeoutConfig,
            final InitiatorConfig initiatorConfig
    ) {
        this.recordStream = recordStream;
        this.metrics = metrics;
        this.socketAddressConfig = socketAddressConfig;
        this.timeoutConfig = timeoutConfig;
        this.initiatorConfig = initiatorConfig;
    }

    public List<Initiator> createInitiators(final long numberOfInitiators, final RelpClientFactory relpClientFactory) {
        final List<Initiator> initiators = new ArrayList<>();
        for (int initiatorCount = 0; initiatorCount < numberOfInitiators; initiatorCount++) {
            final Initiator initiator = new Initiator(
                    relpClientFactory,
                    recordStream,
                    socketAddressConfig.hostname(),
                    socketAddressConfig.port(),
                    metrics,
                    timeoutConfig.openTimeout(),
                    timeoutConfig.payloadTimeout(),
                    initiatorConfig.retryTransmissionCount(),
                    initiatorConfig.retryConnectionCount()
            );
            initiators.add(initiator);
        }
        return initiators;
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
            final InitiatorFactory that = (InitiatorFactory) o;
            equals = Objects.equals(recordStream, that.recordStream)
                    && Objects.equals(socketAddressConfig, that.socketAddressConfig) && Objects.equals(metrics, that.metrics) && Objects.equals(timeoutConfig, that.timeoutConfig) && Objects.equals(initiatorConfig, that.initiatorConfig);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordStream, socketAddressConfig, metrics, timeoutConfig, initiatorConfig);
    }
}
