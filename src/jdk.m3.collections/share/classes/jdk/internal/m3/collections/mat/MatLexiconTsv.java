/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Local, provenance-pinned TSV compiler for finite lexicons.
 *
 * <p>Format: token TAB semantic-id [TAB flags]. No network access is performed.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatLexiconTsv.java}
 * blob {@code 1a9a043160a432e05cdf56a1ea209838635654bf} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatLexiconTsv {
    private MatLexiconTsv() { }

    public static MatStaticLexicon compile(
            String namespace,
            Path source,
            MatLexiconSource provenance) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(provenance, "provenance");
        String actualHash = sha256(source);
        if (!actualHash.equals(provenance.sha256Hex())) {
            throw new IllegalArgumentException(
                    "Source SHA-256 mismatch: expected "
                            + provenance.sha256Hex()
                            + " actual "
                            + actualHash);
        }

        List<MatStaticLexicon.Entry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                if (fields.length < 2 || fields.length > 3 || fields[0].isEmpty()) {
                    throw new IllegalArgumentException(
                            "Invalid lexicon TSV at line " + lineNumber);
                }
                long semanticId = parseLong(fields[1], lineNumber);
                long flags = fields.length == 3 && !fields[2].isBlank()
                        ? parseLong(fields[2], lineNumber)
                        : 0L;
                entries.add(new MatStaticLexicon.Entry(fields[0], semanticId, flags));
            }
        }
        return MatStaticLexicon.compile(namespace, entries);
    }

    private static long parseLong(String value, int lineNumber) {
        String text = value.strip();
        try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return Long.parseUnsignedLong(text.substring(2), 16);
            }
            return Long.parseLong(text);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "Invalid integer at lexicon TSV line " + lineNumber + ": " + value,
                    failure);
        }
    }

    private static String sha256(Path source) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream input = Files.newInputStream(source)) {
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (count != 0) {
                        digest.update(buffer, 0, count);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
