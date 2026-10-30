// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * One typed, content-addressed MOP atom: a TYPE header, METHOD or FIELD declaration.
 *
 * <p>Address = path + char range + line range + {@code exactSha256} of the exact source span.
 * {@code inputs}, {@code output}, {@code effects} and {@code failure} are the typed view;
 * {@code limits} lists M3 hard-limit violations (METHOD_15, LOOP, CONST_HOIST);
 * {@code shapeKey} is the javac tree-kind projection used for pattern identity and
 * {@code contract} the exact declaration contract used by the refactor gate. Effects are
 * syntax evidence from the javac tree, never a purity proof.</p>
 */
//FLLDCIM: MOP — typed atom: inputs, outputs, effects, failure; range + sha256 address
public record M3Atom(String path, String owner, String kind, String name, boolean exposed,
        int startLine, int endLine, int startChar, int endChar, String inputs, String output,
        String effects, String failure, String limits, int anchors, String exactSha256,
        String shapeKey, String contract) {
    public static final String HEADER = "atomId\tpath\towner\tkind\tname\texposed\tstartLine"
            + "\tendLine\tstartChar\tendChar\tinputs\toutput\teffects\tfailure\tlimits\tanchors"
            + "\texactSha256\tshapeKey\tcontractSha256";
    public static final String NONE = "NONE";
    public static final String METHOD_15 = "METHOD_15";
    public static final String LOOP = "LOOP";
    public static final String CONST_HOIST = "CONST_HOIST";
    public static final String ANCHOR = "//FLLDCIM:";
    private static final String ID_DOMAIN = "M3_MOP_ATOM_ID_V1";

    public M3Atom {
        Stream.of(path, owner, kind, name, inputs, output, effects, failure, limits,
                exactSha256, shapeKey, contract).forEach(Objects::requireNonNull);
        if (startLine < 1 || endLine < startLine || startChar < 0 || endChar <= startChar
                || anchors < 0) {
            throw new IllegalArgumentException("invalid atom range: " + path + "#" + name);
        }
    }

    /** Stable atom id: path, owner, kind, name and declaration contract. */
    public String id() {
        return M3MopReceipt.canonical(ID_DOMAIN, path, owner, kind, name, contract).substring(0, 16);
    }

    /** API identity compared by the refactor gate. */
    public String signature() {
        return owner + '#' + kind + '#' + name + '@' + contract;
    }

    /** Number of hard-limit violations of one kind (0 or 1 per atom). */
    public int limit(String kind) {
        return Stream.of(limits.split("\\|")).anyMatch(kind::equals) ? 1 : 0;
    }

    /** One ATOMS.tsv row without the trailing newline. */
    public String row() {
        return String.join("\t", id(), path, owner, kind, name, Boolean.toString(exposed),
                Integer.toString(startLine), Integer.toString(endLine),
                Integer.toString(startChar), Integer.toString(endChar), inputs, output, effects,
                failure, limits, Integer.toString(anchors), exactSha256, shapeKey,
                M3MopReceipt.sha256(contract));
    }

    /** Collapses whitespace and removes TSV control characters from a source fragment. */
    static String clean(CharSequence text) {
        String value = text.toString().replaceAll("\\s+", " ").strip();
        return value.isEmpty() ? NONE : value;
    }
}
