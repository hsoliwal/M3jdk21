// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Consume the *original caller-owned* Jackson JsonParser once into the existing
 * immutable primitive M3ASTIndex. Never constructs a Jackson JsonNode, M3ASTAtom,
 * duplicate String payload, Map.Entry or a second grammar parser.
 *
 * <p>Each JSON object field is an occurrence node whose value is its one child;
 * duplicate field names and repeated children preserve original input order.
 * Parser grammar, token decoding and stream lifetime remain Jackson-owned.
 * Source ranges are unknown because the parser's byte/character coordinate
 * authority was not provided as a verified M3File source seal.
 */
public final class M3ASTJacksonEvents {
    private M3ASTJacksonEvents() { }

    public static M3ASTIndex fromJackson(M3File source, Object originalParser,
            M3ASTIngress.Limits limits) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(originalParser, "original Jackson JsonParser");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("Jackson source exceeds M3 AST source budget");
        }
        Class<?> type;
        Method current;
        Method next;
        try {
            type = Class.forName("com.fasterxml.jackson.core.JsonParser", false,
                    originalParser.getClass().getClassLoader());
            if (!type.isInstance(originalParser)) {
                throw new IllegalArgumentException("not the original Jackson JsonParser");
            }
            current = type.getMethod("currentToken");
            next = type.getMethod("nextToken");
        } catch (ClassNotFoundException | NoSuchMethodException failure) {
            throw new IllegalArgumentException("Jackson 2.19 JsonParser contract unavailable", failure);
        }
        M3ASTIndex.Builder out = M3ASTIndex.builder(
                source, M3ASTIndex.Domain.JACKSON_JSON, limits.maxNodes());
        int[] containers = new int[16];
        int[] fields = new int[16];
        boolean[] objects = new boolean[16];
        int depth = 0;
        boolean complete = false;
        long events = 0;
        long maxEvents = (long) limits.maxNodes() * 4L + 4L;
        Object token = read(current, originalParser);
        if (token == null) token = read(next, originalParser);
        if (token == null) throw new IOException("empty Jackson JSON input");
        while (token != null) {
            if (++events > maxEvents) throw new IllegalArgumentException("Jackson token event budget");
            if (complete) throw new IOException("Jackson input contains trailing root");
            if (!(token instanceof Enum<?> symbol)) {
                throw new IllegalArgumentException("Jackson returned a non-enum JSON token");
            }
            String kind = symbol.name();
            switch (kind) {
                case "START_OBJECT", "START_ARRAY" -> {
                    int parent = parent(containers, fields, objects, depth);
                    if (depth >= limits.maxDepth()) {
                        throw new IllegalArgumentException("Jackson AST depth budget");
                    }
                    int row = out.add(parent,
                            kind.equals("START_OBJECT") ? "JSON_OBJECT" : "JSON_ARRAY",
                            -1, -1, M3ASTIndex.SOURCE_UNVERIFIED);
                    if (depth > 0 && objects[depth - 1]) fields[depth - 1] = -1;
                    if (depth == containers.length) {
                        int expanded = Math.min(limits.maxDepth(), containers.length * 2);
                        containers = Arrays.copyOf(containers, expanded);
                        fields = Arrays.copyOf(fields, expanded);
                        objects = Arrays.copyOf(objects, expanded);
                    }
                    containers[depth] = row;
                    objects[depth] = kind.equals("START_OBJECT");
                    fields[depth] = -1;
                    depth++;
                }
                case "FIELD_NAME" -> {
                    if (depth == 0 || !objects[depth - 1] || fields[depth - 1] >= 0) {
                        throw new IOException("Jackson field outside object or missing value");
                    }
                    fields[depth - 1] = out.add(containers[depth - 1], "JSON_FIELD",
                            -1, -1, M3ASTIndex.SOURCE_UNVERIFIED);
                }
                case "END_OBJECT", "END_ARRAY" -> {
                    if (depth == 0 || objects[depth - 1] != kind.equals("END_OBJECT")
                            || fields[depth - 1] >= 0) {
                        throw new IOException("mismatched Jackson container closing token");
                    }
                    depth--;
                    if (depth == 0) complete = true;
                }
                case "VALUE_STRING", "VALUE_NUMBER_INT", "VALUE_NUMBER_FLOAT",
                        "VALUE_TRUE", "VALUE_FALSE", "VALUE_NULL" -> {
                    int parent = parent(containers, fields, objects, depth);
                    out.add(parent, "JSON_" + kind.substring("VALUE_".length()),
                            -1, -1, M3ASTIndex.SOURCE_UNVERIFIED);
                    if (depth > 0 && objects[depth - 1]) fields[depth - 1] = -1;
                    if (depth == 0) complete = true;
                }
                default -> throw new IOException("non-JSON Jackson token refused: " + kind);
            }
            token = read(next, originalParser);
        }
        if (!complete || depth != 0) throw new IOException("incomplete Jackson JSON document");
        return out.seal();
    }

    private static int parent(int[] containers, int[] fields, boolean[] objects, int depth)
            throws IOException {
        if (depth == 0) return -1;
        int p = depth - 1;
        if (objects[p]) {
            if (fields[p] == -1) throw new IOException("Jackson value without a field name");
            return fields[p];
        }
        return containers[p];
    }

    private static Object read(Method method, Object parser) throws IOException {
        try {
            return method.invoke(parser);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof IOException io) throw io;
            if (error.getCause() instanceof RuntimeException re) throw re;
            throw new IOException("Jackson parser failed", error.getCause());
        } catch (IllegalAccessException problem) {
            throw new IllegalArgumentException("Jackson JsonParser method not accessible", problem);
        }
    }
}
