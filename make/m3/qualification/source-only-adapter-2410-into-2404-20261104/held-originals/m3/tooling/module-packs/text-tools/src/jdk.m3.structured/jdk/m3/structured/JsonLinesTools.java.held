/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            int node = parser.root();
            for (String part : parts) {
                if (parser.kind[node] == Parser.OBJECT) {
                    node = parser.member(node, part);
                } else if (parser.kind[node] == Parser.ARRAY) {
                    if (!part.matches("0|[1-9][0-9]*") || part.length() > 10) {
                        throw new IllegalArgumentException("invalid JSON array index");
                    }
                    long index = Long.parseLong(part);
                    node = parser.size == null || index >= parser.size[node] ? -1
                            : parser.children[parser.first[node] + (int) index];
                } else {
                    node = -1;
                }
                if (node < 0) throw new IllegalArgumentException("missing JSON pointer: " + pointer);
            }
            out.append(line.content(), parser.start[node], parser.end[node]).append('\n');
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

    /**
     * One invocation owns primitive value lanes, exact source spans and member indexes.
     * Zero denotes a missing tree link; a retained link is its value index plus one.
     * The original recursive grammar and refusal order remain the validation authority.
     */
    private static final class Parser {
        static final byte OBJECT = 1, ARRAY = 2;
        final String text;
        final int maxNodes;
        int at, count;
        int[] start = new int[2], end = new int[2];
        byte[] kind = new byte[2];
        int[] first, size, arrayParent;
        int[] keyStart, keyEnd, keyHash, left, right;
        byte[] height;
        int[] children;

        Parser(String text, int maxNodes) { this.text = text; this.maxNodes = maxNodes; }
        int root() {
            int node = value(0);
            spaces();
            if (at != text.length()) bad("trailing JSON data");
            indexArrays();
            return node;
        }
        int value(int depth) {
            if (depth > 64 || ++count > maxNodes) bad("JSON depth/node budget");
            int node = count - 1;
            reserve(count);
            spaces();
            start[node] = at;
            if (at == text.length()) bad("expected JSON value");
            char ch = text.charAt(at);
            if (ch == '{') {
                at++; spaces();
                kind[node] = OBJECT;
                if (!eat('}')) {
                    do {
                        spaces();
                        if (at == text.length() || text.charAt(at) != '"') bad("expected object key");
                        int from = at + 1;
                        int hash = string();
                        int to = at - 1;
                        spaces(); require(':');
                        int child = value(depth + 1);
                        objectLanes();
                        keyStart[child] = from;
                        keyEnd[child] = to;
                        keyHash[child] = hash;
                        first[node] = insert(first[node], child);
                        spaces();
                        if (eat('}')) break;
                        require(',');
                    } while (true);
                }
            } else if (ch == '[') {
                at++; spaces();
                kind[node] = ARRAY;
                if (!eat(']')) {
                    do {
                        int child = value(depth + 1);
                        arrayLanes();
                        arrayParent[child] = node + 1;
                        size[node]++;
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
            end[node] = at;
            return node;
        }

        /** Unused container kinds retain no member or direct-child metadata. */
        void objectLanes() {
            if (keyStart != null) return;
            int capacity = start.length;
            if (first == null) first = new int[capacity];
            keyStart = new int[capacity];
            keyEnd = new int[capacity];
            keyHash = new int[capacity];
            left = new int[capacity];
            right = new int[capacity];
            height = new byte[capacity];
        }
        void arrayLanes() {
            if (size != null) return;
            int capacity = start.length;
            if (first == null) first = new int[capacity];
            size = new int[capacity];
            arrayParent = new int[capacity];
        }

        /** Keep capacity growth geometric and within the unchanged value-node budget. */
        void reserve(int needed) {
            if (needed <= start.length) return;
            int capacity = (int) Math.min(maxNodes, Math.max((long) needed, 2L * start.length));
            start = Arrays.copyOf(start, capacity);
            end = Arrays.copyOf(end, capacity);
            kind = Arrays.copyOf(kind, capacity);
            if (first != null) first = Arrays.copyOf(first, capacity);
            if (size != null) {
                size = Arrays.copyOf(size, capacity);
                arrayParent = Arrays.copyOf(arrayParent, capacity);
            }
            if (keyStart != null) {
                keyStart = Arrays.copyOf(keyStart, capacity);
                keyEnd = Arrays.copyOf(keyEnd, capacity);
                keyHash = Arrays.copyOf(keyHash, capacity);
                left = Arrays.copyOf(left, capacity);
                right = Arrays.copyOf(right, capacity);
                height = Arrays.copyOf(height, capacity);
            }
        }

        /** Prefix endpoints plus reverse preorder produce stable direct-child ranges. */
        void indexArrays() {
            if (size == null) return;
            int length = 0;
            for (int node = 0; node < count; node++) {
                if (kind[node] == ARRAY) {
                    length += size[node];
                    first[node] = length;
                }
            }
            children = new int[length];
            for (int node = count - 1; node > 0; node--) {
                int parent = arrayParent[node] - 1;
                if (parent >= 0) children[--first[parent]] = node;
            }
        }

        /** Hashes only order candidates; decoded UTF-16 comparison establishes equality. */
        int insert(int link, int child) {
            if (link == 0) {
                height[child] = 1;
                return child + 1;
            }
            int node = link - 1;
            int comparison = Integer.compare(keyHash[child], keyHash[node]);
            if (comparison == 0) comparison = compareKeys(child, node);
            if (comparison == 0) bad("duplicate JSON key");
            if (comparison < 0) left[node] = insert(left[node], child);
            else right[node] = insert(right[node], child);
            updateHeight(node);
            int balance = height(left[node]) - height(right[node]);
            if (balance > 1) {
                int lower = left[node] - 1;
                if (height(left[lower]) < height(right[lower])) left[node] = rotateLeft(left[node]);
                return rotateRight(link);
            }
            if (balance < -1) {
                int lower = right[node] - 1;
                if (height(right[lower]) < height(left[lower])) right[node] = rotateRight(right[node]);
                return rotateLeft(link);
            }
            return link;
        }
        int height(int link) { return link == 0 ? 0 : height[link - 1]; }
        void updateHeight(int node) {
            height[node] = (byte) (1 + Math.max(height(left[node]), height(right[node])));
        }
        int rotateLeft(int link) {
            int node = link - 1;
            int upper = right[node] - 1;
            right[node] = left[upper];
            left[upper] = link;
            updateHeight(node);
            updateHeight(upper);
            return upper + 1;
        }
        int rotateRight(int link) {
            int node = link - 1;
            int upper = left[node] - 1;
            left[node] = right[upper];
            right[upper] = link;
            updateHeight(node);
            updateHeight(upper);
            return upper + 1;
        }
        int member(int object, String key) {
            int hash = key.hashCode();
            int link = first == null ? 0 : first[object];
            while (link != 0) {
                int node = link - 1;
                int comparison = Integer.compare(hash, keyHash[node]);
                if (comparison == 0) comparison = compareKey(key, node);
                if (comparison == 0) return node;
                link = comparison < 0 ? left[node] : right[node];
            }
            return -1;
        }
        int compareKeys(int one, int two) {
            int a = keyStart[one], b = keyStart[two];
            while (a < keyEnd[one] && b < keyEnd[two]) {
                long av = decodedUnit(a), bv = decodedUnit(b);
                int comparison = Character.compare((char) av, (char) bv);
                if (comparison != 0) return comparison;
                a = (int) (av >>> 32);
                b = (int) (bv >>> 32);
            }
            return a < keyEnd[one] ? 1 : b < keyEnd[two] ? -1 : 0;
        }
        int compareKey(String key, int node) {
            int i = 0, cursor = keyStart[node];
            while (i < key.length() && cursor < keyEnd[node]) {
                long unit = decodedUnit(cursor);
                int comparison = Character.compare(key.charAt(i++), (char) unit);
                if (comparison != 0) return comparison;
                cursor = (int) (unit >>> 32);
            }
            return i < key.length() ? 1 : cursor < keyEnd[node] ? -1 : 0;
        }

        /** Read one already-validated decoded unit, carrying its next source offset. */
        long decodedUnit(int cursor) {
            char unit = text.charAt(cursor++);
            if (unit == '\\') {
                char escape = text.charAt(cursor++);
                unit = switch (escape) {
                    case '"', '\\', '/' -> escape;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> {
                        int code = 0;
                        for (int i = 0; i < 4; i++) {
                            char h = text.charAt(cursor++);
                            int digit = h >= '0' && h <= '9' ? h - '0'
                                    : h >= 'a' && h <= 'f' ? h - 'a' + 10 : h - 'A' + 10;
                            code = (code << 4) | digit;
                        }
                        yield (char) code;
                    }
                    default -> throw new AssertionError("validated JSON escape");
                };
            }
            return ((long) cursor << 32) | unit;
        }

        int string() {
            require('"');
            int hash = 0;
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') return hash;
                if (c < 0x20) bad("control character in JSON string");
                if (c == '\\') {
                    if (at == text.length()) bad("truncated JSON escape");
                    c = text.charAt(at++);
                    switch (c) {
                        case '"', '\\', '/' -> hash = 31 * hash + c;
                        case 'b' -> hash = 31 * hash + '\b';
                        case 'f' -> hash = 31 * hash + '\f';
                        case 'n' -> hash = 31 * hash + '\n';
                        case 'r' -> hash = 31 * hash + '\r';
                        case 't' -> hash = 31 * hash + '\t';
                        case 'u' -> {
                            char unit = hex4();
                            if (Character.isHighSurrogate(unit)) {
                                if (at + 1 >= text.length() || text.charAt(at++) != '\\' || text.charAt(at++) != 'u')
                                    bad("unpaired JSON high surrogate");
                                char low = hex4();
                                if (!Character.isLowSurrogate(low)) bad("unpaired JSON low surrogate");
                                hash = 31 * (31 * hash + unit) + low;
                            } else if (Character.isLowSurrogate(unit)) {
                                bad("unpaired JSON low surrogate");
                            } else {
                                hash = 31 * hash + unit;
                            }
                        }
                        default -> bad("invalid JSON escape");
                    }
                } else {
                    if (Character.isSurrogate(c)) {
                        if (!Character.isHighSurrogate(c) || at == text.length()
                                || !Character.isLowSurrogate(text.charAt(at))) bad("unpaired raw JSON surrogate");
                        hash = 31 * (31 * hash + c) + text.charAt(at++);
                    } else hash = 31 * hash + c;
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

