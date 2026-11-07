/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Reads reviewed file-atom identity overrides from the exact committed census tree. */
public final class M3FileAtomIdentities {
    /** Repository-relative path of the reviewed identity override table. */
    public static final String PATH = "m3/tooling/atom-census/contract/FILE_IDENTITY_OVERRIDES.tsv";
    /** Exact override-table header. */
    public static final String HEADER = "source_path\tatom_id";

    private static final Pattern ATOM_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/@+\\-]{0,255}");

    private M3FileAtomIdentities() {
    }

    /**
     * Reads the optional exact-tree override table.
     *
     * <p>Paths absent from the table use the deterministic path-derived identity. A reviewed move preserves identity
     * by moving one existing atom ID to the new path in this table. Unknown paths, duplicate IDs, unsorted rows and
     * malformed text fail closed.</p>
     *
     * @param snapshot committed tree snapshot
     * @return immutable source-path to atom-ID overrides
     * @throws IOException when the committed table cannot be read
     */
    public static Map<String, String> read(M3GitTreeSnapshot snapshot) throws IOException {
        M3GitTreeEntry table = snapshot.entries().stream().filter(entry -> entry.path().equals(PATH)).findFirst()
                .orElse(null);
        if (table == null) {
            return Map.of();
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(snapshot.readBlob(table)))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw invalid("file is not strict UTF-8");
        }
        if (text.startsWith("\uFEFF")) {
            throw invalid("UTF-8 byte-order marks are not permitted");
        }
        if (text.indexOf('\r') >= 0) {
            throw invalid("carriage returns are not permitted; use LF");
        }
        if (!text.endsWith("\n") || text.endsWith("\n\n")) {
            throw invalid("file must end with exactly one LF");
        }
        List<String> lines = List.of(text.substring(0, text.length() - 1).split("\n", -1));
        if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
            throw invalid("expected exact header: " + HEADER);
        }
        Set<String> trackedPaths = new HashSet<>();
        snapshot.entries().forEach(entry -> trackedPaths.add(entry.path()));
        Map<String, String> overrides = new TreeMap<>();
        Set<String> atomIds = new HashSet<>();
        String previousPath = null;
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1);
            if (line.isEmpty()) {
                throw invalid(lineNumber, "blank lines are not permitted");
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 2) {
                throw invalid(lineNumber, "expected two cells");
            }
            String sourcePath = cells[0];
            String atomId = cells[1];
            if (previousPath != null && previousPath.compareTo(sourcePath) >= 0) {
                throw invalid(lineNumber, "rows must be strictly sorted by source_path");
            }
            previousPath = sourcePath;
            if (!trackedPaths.contains(sourcePath)) {
                throw invalid(lineNumber, "override references an untracked path: " + sourcePath);
            }
            if (!ATOM_ID.matcher(atomId).matches()) {
                throw invalid(lineNumber, "invalid atom_id: " + atomId);
            }
            if (!atomIds.add(atomId)) {
                throw invalid(lineNumber, "duplicate override atom_id: " + atomId);
            }
            overrides.put(sourcePath, atomId);
        }
        return Collections.unmodifiableMap(overrides);
    }

    private static IllegalArgumentException invalid(String message) {
        return invalid(1, message);
    }

    private static IllegalArgumentException invalid(int line, String message) {
        return new IllegalArgumentException(PATH + ":" + line + ": " + message);
    }
}
