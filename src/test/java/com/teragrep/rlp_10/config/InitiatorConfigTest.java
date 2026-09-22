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

import com.teragrep.cnf_01.ConfigurationException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class InitiatorConfigTest {

    @Test
    void testValidConfiguration() {
        final int expectedInitiatorCount = 50;
        final int expectedRetryTransmissionCount = 2;
        final int expectedRetryConnectionCount = 2;
        final int expectedEventLoopCount = 4;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(
                expectedInitiatorCount,
                expectedRetryTransmissionCount,
                expectedRetryConnectionCount,
                expectedEventLoopCount
        );
        Assertions
                .assertEquals(
                        expectedInitiatorCount, Assertions.assertDoesNotThrow(() -> initiatorConfig.initiatorCount())
                );
        Assertions.assertEquals(expectedRetryTransmissionCount, initiatorConfig.retryTransmissionCount());
        Assertions.assertEquals(expectedRetryConnectionCount, initiatorConfig.retryConnectionCount());
        Assertions
                .assertEquals(
                        expectedEventLoopCount, Assertions.assertDoesNotThrow(() -> initiatorConfig.eventLoopCount())
                );
    }

    @Test
    void testInvalidConfiguration() {
        final int invalidInitiatorCount = -25;
        final int invalidEventLoopCount = 0;
        final int retryTransmissionCount = 2;
        final int retryConnectionCount = 2;
        final InitiatorConfig initiatorConfig = new InitiatorConfig(
                invalidInitiatorCount,
                retryTransmissionCount,
                retryConnectionCount,
                invalidEventLoopCount
        );
        Assertions.assertEquals(retryTransmissionCount, initiatorConfig.retryTransmissionCount());
        Assertions.assertEquals(retryConnectionCount, initiatorConfig.retryConnectionCount());
        Assertions.assertThrows(ConfigurationException.class, () -> initiatorConfig.initiatorCount());
        Assertions.assertThrows(ConfigurationException.class, () -> initiatorConfig.eventLoopCount());
    }

    @Test
    public void testContract() {
        EqualsVerifier.forClass(InitiatorConfig.class).verify();
    }
}
