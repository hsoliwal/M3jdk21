/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Strict JSON Lines pointer extraction with RFC 6901 tokens and exact source-value bytes. */
public final class JsonLinesTools {
    private JsonLinesTools() { }

    /**
     * Extract an RFC 6901 JSON Pointer from every nonempty JSON Lines record.
     * Missing paths, duplicate object names, malformed JSON or empty lines refuse atomically.
     * The selected JSON value is returned exactly as encoded in the input, plus one LF.
     */
    public static TextFrame select(TextFrame input, String pointer, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        if (pointer.length() > 4096) throw new IllegalArgumentException("pointer budget");
        List<String> parts = pointer(pointer);
        StringBuilder out = new StringBuilder();
        for (TextLine line : input.lines()) {
            Parser parser = new Parser(line.content(), Math.max(256, limits.maxLines()));
            Node node = parser.root();
            for (String part : parts) {
                if (node.object != null) {
                    node = node.object.get(part);
                } else if (node.array != null) {
                    if (!part.matches("0|[1-9][0-9]*") || part.length() > 10) {
                        throw new IllegalArgumentException("invalid JSON array index");
                    }
                    long index = Long.parseLong(part);
                    node = index >= node.array.size() ? null : node.array.get((int) index);
                } else {
                    node = null;
                }
                if (node == null) throw new IllegalArgumentException("missing JSON pointer: " + pointer);
            }
            out.append(line.content(), node.start, node.end).append('\n');
            if (out.length() > limits.maxBytes()) throw new IllegalArgumentException("JSON output budget");
        }
        TextFrame result = TextFrame.fromString(out.toString());
        limits.check(result);
        return result;
    }

    private static List<String> pointer(String value) {
        Objects.requireNonNull(value, "pointer");
        if (value.isEmpty()) return List.of();
        if (!value.startsWith("/")) throw new IllegalArgumentException("pointer must start with /");
        List<String> tokens = new ArrayList<>();
        for (String token : value.substring(1).split("/", -1)) {
            StringBuilder decoded = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c == '~') {
                    if (++i == token.length()) throw new IllegalArgumentException("invalid ~ escape");
                    c = switch (token.charAt(i)) {
                        case '0' -> '~';
                        case '1' -> '/';
                        default -> throw new IllegalArgumentException("invalid ~ escape");
                    };
                }
                decoded.append(c);
            }
            tokens.add(decoded.toString());
        }
        return List.copyOf(tokens);
    }

    private static final class Node {
        final int start, end;
        final Map<String, Node> object;
        final List<Node> array;
        Node(int start, int end, Map<String, Node> object, List<Node> array) {
            this.start = start; this.end = end; this.object = object; this.array = array;
        }
    }

    private static final class Parser {
        final String text;
        final int maxNodes;
        int at, count;
        Parser(String text, int maxNodes) { this.text = text; this.maxNodes = maxNodes; }
        Node root() {
            Node node = value(0);
            spaces();
            if (at != text.length()) bad("trailing JSON data");
            return node;
        }
        Node value(int depth) {
            if (depth > 64 || ++count > maxNodes) bad("JSON depth/node budget");
            spaces();
            int start = at;
            if (at == text.length()) bad("expected JSON value");
            char ch = text.charAt(at);
            Map<String, Node> object = null;
            List<Node> array = null;
            if (ch == '{') {
                at++; spaces();
                object = new HashMap<>();
                if (!eat('}')) {
                    do {
                        spaces();
                        if (at == text.length() || text.charAt(at) != '"') bad("expected object key");
                        String key = string();
                        spaces(); require(':');
                        Node child = value(depth + 1);
                        if (object.putIfAbsent(key, child) != null) bad("duplicate JSON key");
                        spaces();
                        if (eat('}')) break;
                        require(',');
                    } while (true);
                }
            } else if (ch == '[') {
                at++; spaces();
                array = new ArrayList<>();
                if (!eat(']')) {
                    do {
                        array.add(value(depth + 1));
                        spaces();
                        if (eat(']')) break;
                        require(',');
                    } while (true);
                }
            } else if (ch == '"') {
                string();
            } else if (ch == 't') {
                literal("true");
            } else if (ch == 'f') {
                literal("false");
            } else if (ch == 'n') {
                literal("null");
            } else {
                number();
            }
            return new Node(start, at, object, array);
        }
        String string() {
            require('"');
            StringBuilder decoded = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') return decoded.toString();
                if (c < 0x20) bad("control character in JSON string");
                if (c == '\\') {
                    if (at == text.length()) bad("truncated JSON escape");
                    c = text.charAt(at++);
                    switch (c) {
                        case '"', '\\', '/' -> decoded.append(c);
                        case 'b' -> decoded.append('\b');
                        case 'f' -> decoded.append('\f');
                        case 'n' -> decoded.append('\n');
                        case 'r' -> decoded.append('\r');
                        case 't' -> decoded.append('\t');
                        case 'u' -> {
                            char unit = hex4();
                            if (Character.isHighSurrogate(unit)) {
                                if (at + 1 >= text.length() || text.charAt(at++) != '\\' || text.charAt(at++) != 'u')
                                    bad("unpaired JSON high surrogate");
                                char low = hex4();
                                if (!Character.isLowSurrogate(low)) bad("unpaired JSON low surrogate");
                                decoded.append(unit).append(low);
                            } else if (Character.isLowSurrogate(unit)) {
                                bad("unpaired JSON low surrogate");
                            } else {
                                decoded.append(unit);
                            }
                        }
                        default -> bad("invalid JSON escape");
                    }
                } else {
                    if (Character.isSurrogate(c)) {
                        if (!Character.isHighSurrogate(c) || at == text.length()
                                || !Character.isLowSurrogate(text.charAt(at))) bad("unpaired raw JSON surrogate");
                        decoded.append(c).append(text.charAt(at++));
                    } else decoded.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }
        char hex4() {
            if (at + 4 > text.length()) { bad("truncated JSON unicode escape"); }
            int code = 0;
            for (int i = 0; i < 4; i++) {
                char h = text.charAt(at++);
                int digit = h >= '0' && h <= '9' ? h - '0'
                        : h >= 'a' && h <= 'f' ? h - 'a' + 10
                        : h >= 'A' && h <= 'F' ? h - 'A' + 10 : -1;
                if (digit < 0) bad("invalid JSON unicode escape");
                code = (code << 4) | digit;
            }
            return (char) code;
        }
        void number() {
            if (eat('-') && at == text.length()) bad("incomplete JSON number");
            if (!eat('0')) {
                if (at == text.length() || text.charAt(at) < '1' || text.charAt(at) > '9')
                    bad("invalid JSON number");
                do { at++; } while (at < text.length() && digit(text.charAt(at)));
            }
            if (eat('.')) {
                if (at == text.length() || !digit(text.charAt(at))) bad("invalid JSON fraction");
                do { at++; } while (at < text.length() && digit(text.charAt(at)));
            }
            if (eat('e') || eat('E')) {
                if (!eat('+')) eat('-');
                if (at == text.length() || !digit(text.charAt(at))) bad("invalid JSON exponent");
                do { at++; } while (at < text.length() && digit(text.charAt(at)));
            }
        }
        boolean digit(char c) { return c >= '0' && c <= '9'; }
        void literal(String value) {
            if (!text.startsWith(value, at)) bad("invalid JSON literal");
            at += value.length();
        }
        void spaces() {
            while (at < text.length()) {
                char c = text.charAt(at);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
                at++;
            }
        }
        boolean eat(char ch) {
            if (at < text.length() && text.charAt(at) == ch) { at++; return true; }
            return false;
        }
        void require(char ch) { if (!eat(ch)) bad("expected " + ch); }
        void bad(String reason) { throw new IllegalArgumentException(reason + " at offset " + at); }
    }
}

