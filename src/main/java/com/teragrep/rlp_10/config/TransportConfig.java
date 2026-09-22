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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TransportConfig {

    private final boolean tls;
    private final Path keystorePath;
    private final Path truststorePath;
    private final String keystorePassword;
    private final String truststorePassword;
    private final String protocol;

    public TransportConfig() {
        this(false, Paths.get("tls/keystore.jks"), Paths.get("tls/truststore.jks"), "password", "password", "TLSv1.3");
    }

    public TransportConfig(
            final boolean tls,
            final Path keystorePath,
            final Path truststorePath,
            final String keystorePassword,
            final String truststorePassword,
            final String protocol
    ) {
        this.tls = tls;
        this.keystorePath = keystorePath;
        this.truststorePath = truststorePath;
        this.keystorePassword = keystorePassword;
        this.truststorePassword = truststorePassword;
        this.protocol = protocol;
    }

    public boolean tls() {
        return tls;
    }

    public File keyStoreFile() {
        return keystorePath.toFile();
    }

    public File trustStoreFile() {
        return truststorePath.toFile();
    }

    public String keyStorePassword() {
        return keystorePassword;
    }

    public String trustStorePassword() {
        return truststorePassword;
    }

    public String protocol() {
        return protocol;
    }

}
