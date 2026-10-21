// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.parser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Detached index over an ALREADY PARSED ANTLR4 grammar tree.
 *
 * <p>This is an optional donor upper-layer consumer, not an ANTLR parser or a
 * competing AST. The original ParserRuleContext, ParseTree, rule dictionary,
 * error-recovery nodes, and CharStream remain with the caller. Only the
 * original {@link M3File}, integer positions, flags and bounded scalar details
 * survive the call. No ANTLR classes become mandatory JMOD dependencies.
 *
 * <p>The caller must provide the exact original CharStream. Its complete text
 * is verified against the M3File generation before any row is published.
 * Modern ANTLR CodePointCharStream offsets count Unicode code points; legacy
 * ANTLRInputStream offsets count UTF-16 units. Both map to M3's exact UTF-16
 * half-open offsets, including strings containing supplementary characters.
 * Unsupported streaming getText or unverified input identity fails closed.
 */
public final class M3Antlr4Ingress {
    private M3Antlr4Ingress() { }

    public static M3ASTIndex index(M3File source, Object parseTree,
            Object originalParser, Object originalCharStream, M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(parseTree, "original ANTLR4 ParseTree");
        Objects.requireNonNull(originalParser, "original ANTLR4 Parser");
        Objects.requireNonNull(originalCharStream, "original ANTLR4 CharStream");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("ANTLR input exceeds source budget");
        }
        try {
            Reflection api = new Reflection(parseTree.getClass().getClassLoader());
            api.validate(parseTree, originalParser, originalCharStream);
            Geometry positions = new Geometry(source, originalCharStream, api);
            String[] ruleNames = (String[]) api.invoke(api.parserRuleNames, originalParser);
            if (ruleNames == null) throw new IllegalArgumentException("ANTLR rule dictionary is unavailable");
            M3ASTIndex.Builder output = M3ASTIndex.builder(
                    source, M3ASTIndex.Domain.ANTLR4, limits.maxNodes());
            Object[] pending = new Object[32];
            int[] parents = new int[32], depths = new int[32];
            int count = 1;
            pending[0] = parseTree;
            parents[0] = -1;
            depths[0] = 1;
            Lines lines = new Lines(source);
            while (count != 0) {
                Object node = pending[--count];
                int parent = parents[count], depth = depths[count];
                pending[count] = null;
                if (depth > limits.maxDepth()) {
                    throw new IllegalArgumentException("ANTLR parse-tree depth budget exceeded");
                }
                final String kind;
                final int start, end;
                boolean recovered = false;
                if (api.rule.isInstance(node)) {
                    int id = (Integer) api.invoke(api.ruleIndex, node);
                    if (id < 0 || id >= ruleNames.length || ruleNames[id] == null
                            || ruleNames[id].isBlank()) {
                        throw new IllegalArgumentException("ANTLR rule name is missing");
                    }
                    kind = "ANTLR_RULE:" + ruleNames[id];
                    Object begin = api.invoke(api.ruleStart, node);
                    Object finish = api.invoke(api.ruleStop, node);
                    long range = positions.range(begin, finish);
                    start = (int) (range >>> 32); end = (int) range;
                    recovered = api.exception(node) != null;
                } else if (api.terminal.isInstance(node)) {
                    Object token = api.invoke(api.terminalSymbol, node);
                    if (token == null) throw new IllegalArgumentException("ANTLR terminal without original token");
                    int type = (Integer) api.invoke(api.tokenType, token);
                    kind = "ANTLR_TOKEN:" + type;
                    long range = positions.range(token, token);
                    start = (int) (range >>> 32); end = (int) range;
                    recovered = api.errorNode.isInstance(node) || type == -1;
                } else {
                    throw new IllegalArgumentException("foreign ANTLR4 tree occurrence");
                }
                int flags = M3ASTIndex.SYNTAX_ONLY
                        | (start < 0 ? 0 : M3ASTIndex.EXACT_RANGE)
                        | (recovered ? M3ASTIndex.RECOVERED : 0);
                int row = output.add(parent, kind, start, end, flags);
                if (start >= 0) {
                    output.details(row, lines.line(start), lines.line(Math.max(start, end - 1)),
                            null, M3ASTIndex.LiteralKind.NONE, null, 0L, -1, -1);
                }
                int children = (Integer) api.invoke(api.childCount, node);
                if (children < 0 || children > limits.maxNodes() - row - 1 - count) {
                    throw new IllegalArgumentException("ANTLR tree exceeds node budget");
                }
                for (int child = children - 1; child >= 0; child--) {
                    if (count == pending.length) {
                        int next = (int) Math.min(limits.maxNodes(),
                                pending.length + (long) (pending.length >>> 1) + 1L);
                        if (next <= pending.length) {
                            throw new IllegalArgumentException("ANTLR pending occurrence budget");
                        }
                        pending = Arrays.copyOf(pending, next);
                        parents = Arrays.copyOf(parents, next);
                        depths = Arrays.copyOf(depths, next);
                    }
                    Object element = api.invoke(api.child, node, child);
                    if (element == null || !api.tree.isInstance(element)) {
                        throw new IllegalArgumentException("ANTLR rule contains foreign/null child");
                    }
                    pending[count] = element;
                    parents[count] = row;
                    depths[count++] = depth + 1;
                }
            }
            return output.seal();
        } catch (ReflectiveOperationException unavailable) {
            throw new IllegalArgumentException("ANTLR4 original parser API is unavailable/incompatible", unavailable);
        }
    }

    private static final class Geometry {
        private final M3File source;
        private final Object charStream;
        private final Reflection api;
        private final int symbols;
        private final boolean codePoints;
        private final int[] utf16AtPoint;

        Geometry(M3File source, Object charStream, Reflection api) {
            this.source = source;
            this.charStream = charStream;
            this.api = api;
            symbols = (Integer) api.invoke(api.streamSize, charStream);
            if (symbols < 0) throw new IllegalArgumentException("negative ANTLR stream size");
            // A real ANTLR CharStream must yield the complete input. Prove the
            // source snapshot before interpreting any upstream token coordinate.
            Object interval = api.invoke(api.intervalOf, null, 0, symbols - 1);
            String original = (String) api.invoke(api.streamGetText, charStream, interval);
            if (!source.text().equals(original)) {
                throw new IllegalArgumentException("ANTLR source generation differs from M3File");
            }
            codePoints = api.codePointStream.isInstance(charStream);
            int pointCount = source.text().codePointCount(0, source.length());
            if (symbols != (codePoints ? pointCount : source.length())) {
                throw new IllegalArgumentException("ANTLR stream coordinate domain differs from source");
            }
            if (codePoints && symbols != source.length()) {
                utf16AtPoint = new int[symbols + 1];
                for (int p = 0, offset = 0; p < symbols; p++) {
                    utf16AtPoint[p] = offset;
                    offset += Character.charCount(Character.codePointAt(source.text(), offset));
                }
                utf16AtPoint[symbols] = source.length();
            } else {
                utf16AtPoint = null;
            }
        }

        long range(Object firstToken, Object lastToken) {
            if (firstToken == null || lastToken == null) return pack(-1, -1);
            if (!api.token.isInstance(firstToken) || !api.token.isInstance(lastToken)) {
                throw new IllegalArgumentException("foreign ANTLR token boundary");
            }
            Object initial = api.invoke(api.tokenInput, firstToken);
            Object terminal = api.invoke(api.tokenInput, lastToken);
            // Inserted tokens may have no input stream; they cannot claim
            // verified geometry. A different stream is a generation mismatch.
            if (initial == null || terminal == null) return pack(-1, -1);
            if (initial != charStream || terminal != charStream) {
                throw new IllegalArgumentException("ANTLR token belongs to another source generation");
            }
            int start = (Integer) api.invoke(api.tokenStart, firstToken);
            int stop = (Integer) api.invoke(api.tokenStop, lastToken);
            if (start == -1 || stop == -1 || stop < start) return pack(-1, -1);
            if (start < 0 || stop >= symbols) {
                throw new IllegalArgumentException("ANTLR token range exceeds source symbols");
            }
            return pack(utf16(start), utf16(stop + 1));
        }

        private static long pack(int start, int end) {
            return ((long) start << 32) | (end & 0xffffffffL);
        }

        int utf16(int offset) {
            if (offset < 0 || offset > symbols) throw new IllegalArgumentException("ANTLR offset out of range");
            return utf16AtPoint == null ? offset : utf16AtPoint[offset];
        }
    }

    private static final class Lines {
        private int[] starts = new int[32];
        private int count = 1;
        Lines(M3File source) {
            starts[0] = 0;
            for (int i = 0; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '\n' || (c == '\r' && (i + 1 == source.length() || source.charAt(i + 1) != '\n'))) {
                    if (count == starts.length) starts = Arrays.copyOf(starts, count + (count >>> 1) + 1);
                    starts[count++] = i + 1;
                }
            }
        }
        int line(int utf16Offset) {
            int index = Arrays.binarySearch(starts, 0, count, utf16Offset);
            return index >= 0 ? index + 1 : -index - 1;
        }
    }

    private static final class Reflection {
        final Class<?> tree, rule, terminal, errorNode, parser, stream, token, codePointStream;
        final Method childCount, child, ruleIndex, ruleStart, ruleStop;
        final Field ruleException;
        final Method terminalSymbol, tokenType, tokenStart, tokenStop, tokenInput;
        final Method parserRuleNames, streamSize, intervalOf, streamGetText;

        Reflection(ClassLoader loader) throws ReflectiveOperationException {
            tree = Class.forName("org.antlr.v4.runtime.tree.ParseTree", false, loader);
            rule = Class.forName("org.antlr.v4.runtime.ParserRuleContext", false, loader);
            terminal = Class.forName("org.antlr.v4.runtime.tree.TerminalNode", false, loader);
            errorNode = Class.forName("org.antlr.v4.runtime.tree.ErrorNode", false, loader);
            parser = Class.forName("org.antlr.v4.runtime.Parser", false, loader);
            stream = Class.forName("org.antlr.v4.runtime.CharStream", false, loader);
            token = Class.forName("org.antlr.v4.runtime.Token", false, loader);
            codePointStream = Class.forName("org.antlr.v4.runtime.CodePointCharStream", false, loader);
            Class<?> interval = Class.forName("org.antlr.v4.runtime.misc.Interval", false, loader);
            childCount = tree.getMethod("getChildCount");
            child = tree.getMethod("getChild", int.class);
            ruleIndex = rule.getMethod("getRuleIndex");
            ruleStart = rule.getMethod("getStart");
            ruleStop = rule.getMethod("getStop");
            ruleException = rule.getField("exception");
            terminalSymbol = terminal.getMethod("getSymbol");
            tokenType = token.getMethod("getType");
            tokenStart = token.getMethod("getStartIndex");
            tokenStop = token.getMethod("getStopIndex");
            tokenInput = token.getMethod("getInputStream");
            parserRuleNames = parser.getMethod("getRuleNames");
            streamSize = stream.getMethod("size");
            intervalOf = interval.getMethod("of", int.class, int.class);
            streamGetText = stream.getMethod("getText", interval);
        }

        Object exception(Object node) {
            try { return ruleException.get(node); }
            catch (IllegalAccessException error) {
                throw new IllegalArgumentException("ANTLR rule recovery state unavailable", error);
            }
        }

        void validate(Object root, Object parserOwner, Object source) {
            if (!rule.isInstance(root) || !parser.isInstance(parserOwner)
                    || !stream.isInstance(source)) {
                throw new IllegalArgumentException("requires original ANTLR4 rule/parser/CharStream");
            }
        }

        Object invoke(Method method, Object owner, Object... arguments) {
            try {
                return method.invoke(owner, arguments);
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                if (cause instanceof Error fatal) throw fatal;
                throw new IllegalArgumentException("ANTLR upstream parser accessor failed", cause);
            } catch (ReflectiveOperationException invalid) {
                throw new IllegalArgumentException("ANTLR upstream parser accessor incompatible", invalid);
            }
        }
    }
}
