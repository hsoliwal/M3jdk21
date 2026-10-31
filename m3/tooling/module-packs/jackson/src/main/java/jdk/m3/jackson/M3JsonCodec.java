// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

/** Iterative Jackson-token projection; Jackson remains the JSON grammar and escaping owner. */
final class M3JsonCodec {
    private M3JsonCodec() { }

    static M3AST nullValue(M3JsonLimits limits) {
        M3ASTBuilder builder = new M3ASTBuilder(limits);
        return builder.build(builder.leaf("NULL", null, null));
    }

    static M3AST read(JsonParser parser, M3JsonLimits limits) throws IOException {
        try {
            return readValue(parser, limits);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException failure) {
            throw JsonMappingException.from(parser, "M3AST admission refused: " + failure.getMessage(), failure);
        }
    }

    private static M3AST readValue(JsonParser parser, M3JsonLimits limits) throws IOException {
        M3ASTBuilder builder = new M3ASTBuilder(limits);
        ArrayDeque<InputFrame> stack = new ArrayDeque<>();
        Work work = new Work(limits);
        JsonToken token = parser.currentToken();
        if (token == null) token = parser.nextToken();
        if (token == null) throw JsonMappingException.from(parser, "empty JSON input");
        while (true) {
            work.token();
            int atom;
            switch (token) {
                case START_OBJECT, START_ARRAY -> {
                    if (stack.size() >= limits.maxDepth()) throw new IllegalArgumentException("JSON depth budget");
                    stack.push(new InputFrame(token == JsonToken.START_OBJECT ? "OBJECT" : "ARRAY"));
                    token = requiredNext(parser);
                    continue;
                }
                case FIELD_NAME -> {
                    if (stack.isEmpty() || !stack.peek().kind.equals("OBJECT") || stack.peek().name != null) {
                        throw JsonMappingException.from(parser, "field outside object or without a preceding value");
                    }
                    String name = parser.currentName();
                    work.text(name);
                    stack.peek().name = name;
                    token = requiredNext(parser);
                    continue;
                }
                case END_OBJECT, END_ARRAY -> {
                    if (stack.isEmpty()) throw JsonMappingException.from(parser, "unmatched container end");
                    InputFrame frame = stack.pop();
                    String expected = token == JsonToken.END_OBJECT ? "OBJECT" : "ARRAY";
                    if (!frame.kind.equals(expected) || frame.name != null) {
                        throw JsonMappingException.from(parser, "mismatched container or field without a value");
                    }
                    atom = builder.atom(frame.kind, null, null, Arrays.copyOf(frame.children, frame.size));
                }
                case VALUE_STRING -> {
                    String value = parser.getText();
                    work.text(value);
                    atom = builder.leaf("STRING", null, value);
                }
                case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
                    if (parser.isNaN()) throw JsonMappingException.from(parser, "M3AST requires finite JSON numbers");
                    String value = parser.getText();
                    work.text(value);
                    // RFC 8259 numeric grammar; no parser allocation per numeric token.
                    if (!isStrictJsonNumber(value)) {
                        throw JsonMappingException.from(parser, "M3AST requires a strict JSON numeric lexeme");
                    }
                    atom = builder.leaf(token == JsonToken.VALUE_NUMBER_INT ? "NUMBER_INT" : "NUMBER_FLOAT",
                            null, value);
                }
                case VALUE_TRUE -> atom = builder.leaf("TRUE", null, null);
                case VALUE_FALSE -> atom = builder.leaf("FALSE", null, null);
                case VALUE_NULL -> atom = builder.leaf("NULL", null, null);
                default -> throw JsonMappingException.from(parser, "unsupported finite JSON token: " + token);
            }
            if (stack.isEmpty()) return builder.build(atom);
            InputFrame parent = stack.peek();
            if (parent.kind.equals("OBJECT")) {
                if (parent.name == null) throw JsonMappingException.from(parser, "object value without a field name");
                atom = builder.field(parent.name, atom);
                parent.name = null;
            }
            parent.add(atom, limits.maxChildReferences());
            token = requiredNext(parser);
        }
    }

    /** Validates the entire JSON number lexeme without creating another parser or Node. */
    private static boolean isStrictJsonNumber(String text) {
        int size = text.length();
        if (size == 0) return false;
        int cursor = text.charAt(0) == '-' ? 1 : 0;
        if (cursor == size) return false;
        char first = text.charAt(cursor);
        if (first == '0') {
            cursor++;
        } else if (first >= '1' && first <= '9') {
            cursor++;
            while (cursor < size && text.charAt(cursor) >= '0' && text.charAt(cursor) <= '9') cursor++;
        } else {
            return false;
        }
        if (cursor < size && text.charAt(cursor) == '.') {
            cursor++;
            int start = cursor;
            while (cursor < size && text.charAt(cursor) >= '0' && text.charAt(cursor) <= '9') cursor++;
            if (cursor == start) return false;
        }
        if (cursor < size && (text.charAt(cursor) == 'e' || text.charAt(cursor) == 'E')) {
            cursor++;
            if (cursor < size && (text.charAt(cursor) == '+' || text.charAt(cursor) == '-')) cursor++;
            int start = cursor;
            while (cursor < size && text.charAt(cursor) >= '0' && text.charAt(cursor) <= '9') cursor++;
            if (cursor == start) return false;
        }
        return cursor == size;
    }

    private static JsonToken requiredNext(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token == null) throw JsonMappingException.from(parser, "unexpected end of JSON input");
        return token;
    }

    static void write(M3AST ast, JsonGenerator generator, M3JsonLimits limits) throws IOException {
        try {
            writeValue(ast, generator, limits);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException failure) {
            throw JsonMappingException.from(generator, "M3AST projection refused: " + failure.getMessage(), failure);
        }
    }

    private static void writeValue(M3AST ast, JsonGenerator generator, M3JsonLimits limits) throws IOException {
        if ("FIELD".equals(ast.kindAt(ast.rootAtomIndex()))) {
            throw new IllegalArgumentException("a field occurrence is not a complete JSON value");
        }
        if (ast.atomCount() > limits.maxAtoms()) throw new IllegalArgumentException("M3 AST atom budget");
        if (ast.childReferenceCount() > limits.maxChildReferences()) {
            throw new IllegalArgumentException("M3 AST child-reference budget");
        }
        if (ast.retainedTextUnits() > limits.maxTextUnits()) {
            throw new IllegalArgumentException("M3 AST retained text budget");
        }

        // This stack is two primitive arrays, not one OutputFrame object per JSON token.
        int[] atomStack = new int[32];
        int[] nextStack = new int[32];
        atomStack[0] = ast.rootAtomIndex();
        nextStack[0] = -1;
        int top = 0;
        int containers = 0;
        Work work = new Work(limits);
        while (top >= 0) {
            int index = atomStack[top];
            String kind = ast.kindAt(index);
            if (nextStack[top] == -1) {
                work.token();
                switch (kind) {
                    case "OBJECT", "ARRAY" -> {
                        if (++containers > limits.maxDepth()) {
                            throw new IllegalArgumentException("JSON depth budget");
                        }
                        if (kind.equals("OBJECT")) generator.writeStartObject();
                        else generator.writeStartArray();
                        nextStack[top] = 0;
                    }
                    case "FIELD" -> {
                        if (ast.childCountAt(index) != 1) {
                            throw new IllegalStateException("field requires one value");
                        }
                        String name = ast.nameAt(index);
                        work.text(name);
                        generator.writeFieldName(name);
                        nextStack[top] = 0;
                    }
                    case "STRING" -> {
                        String value = ast.valueAt(index);
                        work.text(value);
                        generator.writeString(value);
                        top--;
                        continue;
                    }
                    case "NUMBER_INT", "NUMBER_FLOAT" -> {
                        String value = ast.valueAt(index);
                        work.text(value);
                        if (generator instanceof TokenBuffer buffer) {
                            buffer.writeNumber(value, kind.equals("NUMBER_INT"));
                        } else {
                            generator.writeNumber(value);
                        }
                        top--;
                        continue;
                    }
                    case "TRUE", "FALSE" -> {
                        generator.writeBoolean(kind.equals("TRUE"));
                        top--;
                        continue;
                    }
                    case "NULL" -> {
                        generator.writeNull();
                        top--;
                        continue;
                    }
                    default -> throw new IllegalStateException("unrecognized JSON atom kind: " + kind);
                }
            }
            if (nextStack[top] < ast.childCountAt(index)) {
                int child = ast.childAtomIndexAt(index, nextStack[top]++);
                if (top + 1 == atomStack.length) {
                    int capacity = Math.multiplyExact(atomStack.length, 2);
                    atomStack = Arrays.copyOf(atomStack, capacity);
                    nextStack = Arrays.copyOf(nextStack, capacity);
                }
                atomStack[++top] = child;
                nextStack[top] = -1;
            } else {
                if (kind.equals("OBJECT") || kind.equals("ARRAY")) {
                    work.token();
                    if (kind.equals("OBJECT")) generator.writeEndObject();
                    else generator.writeEndArray();
                    containers--;
                }
                top--;
            }
        }
    }

    /** Only one frame per open container; child references remain a primitive lane. */
    private static final class InputFrame {
        final String kind;
        String name;
        int[] children = new int[0];
        int size;
        InputFrame(String kind) { this.kind = kind; }
        void add(int atom, long limit) {
            if (size >= limit) throw new IllegalArgumentException("JSON child-reference budget");
            if (size == children.length) {
                long next = Math.min(limit, Math.max(4L, (long) size + (size >>> 1) + 1L));
                children = Arrays.copyOf(children, Math.toIntExact(next));
            }
            children[size++] = atom;
        }
    }

    /** Count occurrences as well as retained atoms: DAG sharing cannot evade output work bounds. */
    private static final class Work {
        final M3JsonLimits limits;
        long tokens;
        long text;
        Work(M3JsonLimits limits) { this.limits = limits; }
        void token() {
            if (tokens == limits.maxTokens()) throw new IllegalArgumentException("JSON token budget");
            tokens++;
        }
        void text(String value) {
            text = Math.addExact(text, value.length());
            if (text > limits.maxTextUnits()) throw new IllegalArgumentException("JSON text budget");
        }
    }
}
