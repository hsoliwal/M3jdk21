/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only JNI short-name candidates from the already parsed Java source tree.
 * A candidate never proves that a native symbol exists or that RegisterNatives
 * has not been used. Overloads require independent javac -h ABI inspection.
 */
final class JniDeclarationProjection {
    static final String HEADER = "native_atom_id\trepo\tpath\towner\tmethod\tjava_signature"
            + "\tstart_utf16\tend_utf16\tsource_sha256\tshort_symbol\tresolution";

    /** The atom ID and source digest are those of the original Java semantic row. */
    record Candidate(String nativeAtomId, String repo, String path, String owner,
            String method, String signature, long start, long end, String sourceSha256,
            String shortSymbol, String resolution) {
        String row() {
            return String.join("\t", nativeAtomId, repo, path, owner, method, signature,
                    Long.toString(start), Long.toString(end), sourceSha256, shortSymbol, resolution);
        }
    }

    private JniDeclarationProjection() {
    }

    /** JNI naming is UTF-16 code-unit based. Slash is only a class-name separator. */
    static String encode(String name, boolean className) {
        StringBuilder output = new StringBuilder(name.length() * 2);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
                    || ch >= '0' && ch <= '9') {
                output.append(ch);
            } else if (ch == '/' && className) {
                output.append('_');
            } else if (ch == '_') {
                output.append("_1");
            } else if (ch == ';') {
                output.append("_2");
            } else if (ch == '[') {
                output.append("_3");
            } else {
                output.append("_0").append(String.format(Locale.ROOT, "%04x", (int) ch));
            }
        }
        return output.toString();
    }

    /** Short-name *candidate*: overloaded native methods require long-form proof. */
    static String shortName(String binaryClassName, String method) {
        if (binaryClassName.isEmpty() || method.isEmpty()) {
            throw new IllegalArgumentException("empty JNI class/method identity");
        }
        return "Java_" + encode(binaryClassName, true) + '_' + encode(method, false);
    }

    /** Stable deterministic projection with no source-ID duplication. */
    static List<String> lines(List<Candidate> candidates) {
        List<Candidate> ordered = candidates.stream().sorted(Comparator.comparing(Candidate::path)
                .thenComparingLong(Candidate::start).thenComparingLong(Candidate::end)
                .thenComparing(Candidate::nativeAtomId)).toList();
        List<String> lines = new ArrayList<>(ordered.size() + 1);
        lines.add(HEADER);
        Set<String> nativeIds = new HashSet<>();
        for (Candidate entry : ordered) {
            if (!nativeIds.add(entry.nativeAtomId())) {
                throw new IllegalArgumentException("duplicate JNI declaration source ID " + entry.nativeAtomId());
            }
            lines.add(entry.row());
        }
        return lines;
    }
}
