/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8374808
 * @summary KeyStore creation Instant API preserves legacy Date SPI providers
 * @run main CreationInstant
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

public class CreationInstant {
    private static final long CREATED_MILLIS = 1_700_000_000_123L;
    private static final Date CREATED = new Date(CREATED_MILLIS);

    public static void main(String[] args) throws Exception {
        LegacyDateSpi spi = new LegacyDateSpi();

        Instant expected = Instant.ofEpochMilli(CREATED_MILLIS);
        if (!expected.equals(spi.engineGetCreationInstant("present"))) {
            throw new RuntimeException("default SPI Date-to-Instant bridge mismatch");
        }
        if (spi.engineGetCreationInstant("missing") != null) {
            throw new RuntimeException("missing SPI alias must return null");
        }

        KeyStore uninitialized = new TestKeyStore(spi);
        try {
            uninitialized.getCreationInstant("present");
            throw new RuntimeException("uninitialized keystore must reject access");
        } catch (KeyStoreException expectedException) {
            // Existing KeyStore initialization contract is preserved.
        }

        KeyStore initialized = new TestKeyStore(spi);
        initialized.load(null, null);

        if (!expected.equals(initialized.getCreationInstant("present"))) {
            throw new RuntimeException("KeyStore Instant bridge mismatch");
        }
        if (initialized.getCreationInstant("missing") != null) {
            throw new RuntimeException("missing KeyStore alias must return null");
        }

        Date compatibility = initialized.getCreationDate("present");
        if (!CREATED.equals(compatibility)) {
            throw new RuntimeException("legacy Date API changed");
        }
    }

    private static final class TestKeyStore extends KeyStore {
        TestKeyStore(KeyStoreSpi spi) {
            super(
                    spi,
                    new Provider("M3KeyStoreTest", "1.0", "M3 KeyStore test provider") {},
                    "M3TEST");
        }
    }

    /**
     * Deliberately implements only the Java 21 creation-date SPI. The new default method must
     * adapt this provider without requiring any provider-storage rewrite.
     */
    private static final class LegacyDateSpi extends KeyStoreSpi {
        @Override
        public Key engineGetKey(String alias, char[] password)
                throws NoSuchAlgorithmException, UnrecoverableKeyException {
            return null;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            return null;
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            return "present".equals(alias) ? new Date(CREATED.getTime()) : null;
        }

        @Override
        public void engineSetKeyEntry(
                String alias, Key key, char[] password, Certificate[] chain)
                throws KeyStoreException {}

        @Override
        public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
                throws KeyStoreException {}

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert)
                throws KeyStoreException {}

        @Override
        public void engineDeleteEntry(String alias) throws KeyStoreException {}

        @Override
        public Enumeration<String> engineAliases() {
            return Collections.enumeration(Collections.singleton("present"));
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            return "present".equals(alias);
        }

        @Override
        public int engineSize() {
            return 1;
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            return false;
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            return false;
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return null;
        }

        @Override
        public void engineStore(OutputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException, CertificateException {}

        @Override
        public void engineLoad(InputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException, CertificateException {}
    }
}
