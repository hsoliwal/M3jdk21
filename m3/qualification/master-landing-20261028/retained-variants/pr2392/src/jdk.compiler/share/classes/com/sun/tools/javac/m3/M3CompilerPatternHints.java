/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.sun.tools.javac.m3;

import com.sun.source.util.M3AST;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * M3 compiler-origin machine-oriented-programming hints for every analyzed source type.
 * Consumes the original M3AST.methodFacts() projection: no parser, AST scan, model
 * re-attribution, external dependency, runtime instrumentation or JNI call is introduced.
 * Reports are source-linked review candidates; GoF intent and equivalence are NOT proved.
 */
public final class M3CompilerPatternHints implements M3AST.Consumer {
    public static final String SCHEMA = "M3_COMPILER_PATTERN_HINTS_V1";
    private static final int MAX_TYPES = 65_536;
    private static final int MAX_UTF8 = 32 * 1024 * 1024;
    private static final int MAX_NOTES_PER_TYPE = 7;
    private static final String[] NAMES = {
        "FIELD_METHOD_RELATION", "INTERFACE_INVOCATION", "ITERATIVE_INVOCATION",
        "ALLOCATION_FACTORY_REVIEW", "CONDITIONAL_CONTROL", "SYNCHRONIZATION",
        "LOOP_BRANCH_REVIEW"
    };
    // Derived only from the canonical MethodFacts column IDs; no per-method allocation.
    private static final long[] REQUIRED_MASKS = {
        (1L << M3AST.MethodFacts.FIELD_READ_COUNT)
                | (1L << M3AST.MethodFacts.FIELD_WRITE_COUNT)
                | (1L << M3AST.MethodFacts.INVOCATION_COUNT),
        1L << M3AST.MethodFacts.INVOCATION_COUNT,
        (1L << M3AST.MethodFacts.LOOP_COUNT)
                | (1L << M3AST.MethodFacts.INVOCATION_COUNT),
        (1L << M3AST.MethodFacts.NEW_CLASS_COUNT)
                | (1L << M3AST.MethodFacts.NEW_ARRAY_COUNT),
        (1L << M3AST.MethodFacts.IF_COUNT) | (1L << M3AST.MethodFacts.SWITCH_COUNT)
                | (1L << M3AST.MethodFacts.CONDITIONAL_COUNT),
        1L << M3AST.MethodFacts.SYNCHRONIZED_COUNT,
        (1L << M3AST.MethodFacts.LOOP_COUNT) | (1L << M3AST.MethodFacts.IF_COUNT)
                | (1L << M3AST.MethodFacts.SWITCH_COUNT)
                | (1L << M3AST.MethodFacts.CONDITIONAL_COUNT)
    };
    private final Mode mode;
    private final Path receipt;
    private final ArrayList<String> rows = new ArrayList<>();
    private final Set<String> uniqueTypes = new HashSet<>();
    private int typeCount;
    private boolean closed;

    public enum Mode {
        NOTES, RECEIPTS, BOTH;
        public static Mode parse(String name) {
            return switch (Objects.requireNonNull(name, "hints")) {
                case "notes" -> NOTES;
                case "receipts" -> RECEIPTS;
                case "both" -> BOTH;
                default -> throw new IllegalArgumentException("M3AST hints must be notes, receipts or both");
            };
        }
        public boolean notes() { return this != RECEIPTS; }
        public boolean receipts() { return this != NOTES; }
    }

    public M3CompilerPatternHints(Mode mode, Path receipt) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (mode.receipts() != (receipt != null)) {
            throw new IllegalArgumentException("M3AST hintfile must accompany receipt hints only");
        }
        this.receipt = receipt == null ? null : receipt.toAbsolutePath().normalize();
        if (this.receipt != null && this.receipt.getFileName() == null) {
            throw new IllegalArgumentException("M3AST hintfile must be a regular output file");
        }
    }

    @Override public void analyzed(M3AST.Unit unit) throws Exception {
        if (closed) throw new IllegalStateException("M3 pattern hint consumer already closed");
        TypeElement type = unit.analyzedType();
        if (type == null || type.getEnclosingElement().getKind() != ElementKind.PACKAGE) return;
        // Existing single-event, attributed, source-sealed projection. Its rows include nested
        // declarations, so a single top-level ANALYZE event covers its original subtree.
        M3AST.MethodFacts facts = M3AST.methodFacts(unit);
        String uri = facts.sourceUri().toASCIIString();
        String name = facts.analyzedBinaryName();
        String identity = uri + "\u0000" + name;
        if (!uniqueTypes.add(identity)) return; // duplicate completion event, not another source
        if (++typeCount > MAX_TYPES) throw new IllegalStateException("M3 hint type budget exceeded");
        boolean interfaces = !type.getInterfaces().isEmpty();
        String prefix = "T\t" + encoded(uri) + '\t' + facts.sourceUtf16Sha256()
                + '\t' + encoded(name);
        rows.add(prefix + "\t" + facts.rowCount() + "\n");
        int[] nominations = new int[NAMES.length];
        String[] firstMethod = new String[NAMES.length];
        for (int row = 0; row < facts.rowCount(); row++) {
            int known = 0;
            int candidates = 0;
            for (int hint = 0; hint < NAMES.length; hint++) {
                int state = state(facts, row, hint, interfaces);
                if (state != 0) known |= 1 << hint;
                if (state == 2) {
                    candidates |= 1 << hint;
                    nominations[hint]++;
                    if (firstMethod[hint] == null) {
                        firstMethod[hint] = method(facts, row);
                    }
                }
            }
            if ((candidates & ~known) != 0) throw new AssertionError("M3 hint masks inconsistent");
            int start = value(facts, row, M3AST.MethodFacts.SOURCE_START);
            int end = value(facts, row, M3AST.MethodFacts.SOURCE_END);
            rows.add("M\t" + encoded(uri) + '\t' + facts.sourceUtf16Sha256()
                    + '\t' + encoded(name) + '\t' + encoded(method(facts, row))
                    + '\t' + start + '\t' + end + '\t'
                    + Integer.toHexString(known) + '\t'
                    + Integer.toHexString(candidates) + "\n");
            if (rows.size() > MAX_TYPES * 16) {
                throw new IllegalStateException("M3 pattern hint row budget exceeded");
            }
        }
        if (mode.notes()) {
            TreePath path = unit.trees().getPath(type);
            if (path != null) {
                int notes = 0;
                for (int hint = 0; hint < NAMES.length && notes < MAX_NOTES_PER_TYPE; hint++) {
                    if (nominations[hint] == 0) continue;
                    unit.trees().printMessage(Diagnostic.Kind.NOTE,
                            "[M3-MOP:CANDIDATE] " + NAMES[hint] + " in " + name
                                    + " (" + nominations[hint] + " source method(s), e.g. "
                                    + firstMethod[hint] + "); review in M3AST/Maven; no rewrite authority",
                            path.getLeaf(), unit.tree());
                    notes++;
                }
            }
        }
    }

    /** 0=UNKNOWN, 1=NOT_OBSERVED, 2=CANDIDATE; absent projection columns are never zero. */
    static int state(M3AST.MethodFacts facts, int row, int hint, boolean interfaces) {
        long required = REQUIRED_MASKS[Objects.checkIndex(hint, NAMES.length)];
        while (required != 0L) {
            int column = Long.numberOfTrailingZeros(required);
            if (!facts.hasValue(row, column)) return 0;
            required &= required - 1;
        }
        if (hint <= 2 && facts.hasValue(row, M3AST.MethodFacts.UNRESOLVED)
                && facts.intValue(row, M3AST.MethodFacts.UNRESOLVED) != 0) return 0;
        boolean branch = hint == 4 || hint == 6 ? branching(facts, row) : false;
        boolean candidate = switch (hint) {
            case 0 -> (facts.intValue(row, M3AST.MethodFacts.FIELD_READ_COUNT)
                    + (long) facts.intValue(row, M3AST.MethodFacts.FIELD_WRITE_COUNT)) > 0
                    && positive(facts, row, M3AST.MethodFacts.INVOCATION_COUNT);
            case 1 -> interfaces && positive(facts, row, M3AST.MethodFacts.INVOCATION_COUNT);
            case 2 -> positive(facts, row, M3AST.MethodFacts.LOOP_COUNT)
                    && positive(facts, row, M3AST.MethodFacts.INVOCATION_COUNT);
            case 3 -> positive(facts, row, M3AST.MethodFacts.NEW_CLASS_COUNT)
                    || positive(facts, row, M3AST.MethodFacts.NEW_ARRAY_COUNT);
            case 4 -> branch;
            case 5 -> positive(facts, row, M3AST.MethodFacts.SYNCHRONIZED_COUNT)
                    || (facts.longValue(row, M3AST.MethodFacts.DECLARATION_FLAGS)
                    & M3AST.MethodFacts.FLAG_SYNCHRONIZED) != 0;
            case 6 -> positive(facts, row, M3AST.MethodFacts.LOOP_COUNT) && branch;
            default -> throw new AssertionError("unknown M3 hint");
        };
        return candidate ? 2 : 1;
    }

    private static boolean branching(M3AST.MethodFacts facts, int row) {
        return positive(facts, row, M3AST.MethodFacts.IF_COUNT)
                || positive(facts, row, M3AST.MethodFacts.SWITCH_COUNT)
                || positive(facts, row, M3AST.MethodFacts.CONDITIONAL_COUNT);
    }
    private static boolean positive(M3AST.MethodFacts facts, int row, int column) {
        return facts.intValue(row, column) > 0;
    }
    private static int value(M3AST.MethodFacts facts, int row, int column) {
        return facts.hasValue(row, column) ? facts.intValue(row, column) : -1;
    }
    private static String method(M3AST.MethodFacts facts, int row) {
        return facts.hasValue(row, M3AST.MethodFacts.METHOD_ID)
                ? facts.identity(facts.intValue(row, M3AST.MethodFacts.METHOD_ID)) : "UNRESOLVED";
    }
    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    @Override public void finished(M3AST.Summary summary) throws IOException {
        if (closed) return;
        closed = true;
        if (!mode.receipts() || !summary.completed() || summary.compilerErrors() != 0
                || summary.consumerFailed()) return;
        Collections.sort(rows);
        StringBuilder data = new StringBuilder(SCHEMA + "\tSOURCE_TIME_ONLY\tCANDIDATE_ONLY\n");
        data.append("N\t").append(typeCount).append('\t').append(rows.size()).append('\n');
        for (String row : rows) data.append(row);
        String body = data.toString();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_UTF8) throw new IOException("M3 hint receipt byte budget exceeded");
        String complete = body + "SHA256\t" + sha256(bytes) + '\n';
        Path parent = receipt.getParent();
        if (parent == null || Files.isSymbolicLink(receipt))
            throw new IOException("M3 hint output path refused");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".m3-pattern-hints-", ".tmp");
        try {
            Files.writeString(temporary, complete, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, receipt, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, receipt, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
