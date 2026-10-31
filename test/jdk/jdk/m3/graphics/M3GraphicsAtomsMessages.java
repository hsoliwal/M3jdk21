/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Throwables of the jdk.m3.graphics atom tests compare by class (through the rename) and by message, along the cause
 * chain up to {@link #MAX_CAUSES} levels. Before the comparison both messages are normalized the same way: a donor
 * class name ({@code io.github.humbleui.skija.X}, dotted or slashed) becomes its M3 name by the leaf-1 rename
 * (M3GraphicsAtomsOracle.m3Name, which the generator checks against TYPE_RENAME_L1.tsv), the rename's {@code M3}
 * prefix is dropped, class-loader descriptions ({@code (X is in unnamed module of loader 'app'; ...)}) and identity
 * hashes ({@code @1b6d3586}) are removed, and the local-variable name in a helpful NullPointerException message
 * ({@code because "p" is null}, {@code "<local1>"}, {@code "<parameter1>"}) becomes {@code "<local>"}, since it
 * depends on whether the class file carries a LocalVariableTable. Messages that needed the normalization are counted.
 */
final class M3GraphicsAtomsMessages {
    private static final int MAX_CAUSES = 4;
    private static final Pattern DONOR = Pattern.compile(
            "io([./])github\\1humbleui\\1(?:skija|types)(?:\\1[a-z_][a-z0-9_]*)*\\1[A-Z][\\w$]*");
    private static final Pattern RENAMED = Pattern.compile("(?<![\\w$])M3(?=[A-Z])");
    private static final Pattern LOADER = Pattern.compile(" ?\\([^()]* of loader [^()]*\\)");
    private static final Pattern IDENTITY = Pattern.compile("@[0-9a-f]{1,8}\\b");
    private static final Pattern LOCAL = Pattern.compile(
            "because \"(?:<(?:local|parameter)\\d+>|[A-Za-z_$][\\w$]*)(?=[\\[\".])");
    private static final String LOCAL_NAME = "because \"<local>";
    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final String EMPTY = "";
    private static final String AT = "@";
    private static final String CLASS = ".class";
    private static final String MESSAGE = ".message";
    private static final String CAUSE = ".cause";
    private static final String COLON = ": M3 ";
    private static final String VERSUS = " / oracle ";
    private static final AtomicInteger NORMALIZED = new AtomicInteger();

    private M3GraphicsAtomsMessages() {}

    /** The first difference between two throwables (class, normalized message, causes), empty when they agree. */
    static Optional<String> compare(String path, Throwable m3, Throwable oracle) {
        return compare(path, m3, oracle, 0);
    }

    /** The number of messages that matched only after normalization since the last call. */
    static int normalizedAndReset() {
        return NORMALIZED.getAndSet(0);
    }

    /** A message after the normalization described in the class comment ("null" for no message). */
    static String normalized(String message) {
        String s = DONOR.matcher(String.valueOf(message)).replaceAll(r -> Matcher.quoteReplacement(m3Name(r.group())));
        s = RENAMED.matcher(LOADER.matcher(s).replaceAll(EMPTY)).replaceAll(EMPTY);
        return LOCAL.matcher(IDENTITY.matcher(s).replaceAll(AT)).replaceAll(Matcher.quoteReplacement(LOCAL_NAME));
    }

    private static Optional<String> compare(String path, Throwable m, Throwable o, int depth) {
        if (m == null || o == null) {
            return m == o ? Optional.empty() : differ(path, m, o);
        }
        if (!M3GraphicsAtomsCompare.sameType(m.getClass(), o.getClass())) {
            return differ(path + CLASS, m, o);
        }
        String mm = normalized(m.getMessage());
        String om = normalized(o.getMessage());
        if (!mm.equals(om)) {
            return differ(path + MESSAGE, mm, om);
        }
        if (!Objects.equals(m.getMessage(), o.getMessage())) {
            NORMALIZED.incrementAndGet();
        }
        return depth + 1 < MAX_CAUSES ? compare(path + CAUSE, m.getCause(), o.getCause(), depth + 1)
                : Optional.empty();
    }

    private static String m3Name(String donor) {
        boolean slashed = donor.contains(SLASH);
        String name = M3GraphicsAtomsOracle.m3Name(donor.replace(SLASH, DOT));
        return slashed ? name.replace(DOT, SLASH) : name;
    }

    private static Optional<String> differ(String path, Object m, Object o) {
        return Optional.of(path + COLON + M3GraphicsAtomsCompare.show(m) + VERSUS + M3GraphicsAtomsCompare.show(o));
    }
}
