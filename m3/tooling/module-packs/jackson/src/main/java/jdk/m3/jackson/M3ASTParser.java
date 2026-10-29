// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.jackson;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * Pull cursor over the original sealed AST lanes. No source reader, generator,
 * JSON grammar parser, token buffer, node wrapper or second atom store is created.
 * Jackson's ParserBase owns numeric coercion, duplicate detection and read context.
 *
 * <p>Traversal uses two primitive arrays and Jackson's reused contexts per open
 * container. Text access returns the retained String; an explicit character-array
 * or numeric-coercion request may use Jackson's current-token text buffer. No
 * document-wide token or scalar copy is retained. Optional strict duplicate
 * detection keeps Jackson's ordinary per-object name set.
 */
@SuppressWarnings("deprecation")
final class M3ASTParser extends ParserBase {
    private static final int[] NO_STACK = new int[0];

    private M3AST ast;
    private ObjectCodec codec;
    private final M3JsonLimits limits;
    private int[] containers = NO_STACK;
    private int[] nextChild = NO_STACK;
    private int depth;
    private int pendingValue;
    private String text;
    private String bufferedText;
    private boolean numberBuffered;
    private long tokens;
    private long textUnits;

    M3ASTParser(M3AST ast, JsonFactory factory, ObjectCodec codec, M3JsonLimits limits)
            throws IOException {
        super(context(Objects.requireNonNull(factory, "factory")), factory.getParserFeatures());
        this.ast = Objects.requireNonNull(ast, "ast");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.limits = Objects.requireNonNull(limits, "limits");
        pendingValue = ast.rootAtomIndex();
        if ("FIELD".equals(ast.kindAt(pendingValue))) {
            throw refused("a field occurrence is not a complete JSON value");
        }
        if (ast.atomCount() > limits.maxAtoms()) throw refused("M3 AST atom budget");
        if (ast.childReferenceCount() > limits.maxChildReferences()) {
            throw refused("M3 AST child-reference budget");
        }
        if (ast.retainedTextUnits() > limits.maxTextUnits()) {
            throw refused("M3 AST retained text budget");
        }
        _parsingContext.reset(JsonReadContext.TYPE_ROOT, -1, -1);
    }

    /** Each token kind has its own constraint; a long number is not a long string. */
    private static IOContext context(JsonFactory factory) {
        return new IOContext(factory.streamReadConstraints(), factory.streamWriteConstraints(),
                ErrorReportConfiguration.defaults(), null, ContentReference.unknown(), false) {
            @Override public TextBuffer constructReadConstrainedTextBuffer() {
                return new TextBuffer(null);
            }
        }.markBufferRecyclerReleased();
    }

    @Override public ObjectCodec getCodec() { return codec; }
    @Override public void setCodec(ObjectCodec codec) { this.codec = codec; }

    @Override public JsonToken nextToken() throws IOException {
        if (_closed) return _updateTokenToNull();
        text = null;
        bufferedText = null;
        numberBuffered = false;
        _numTypesValid = NR_UNKNOWN;
        _numberString = null;
        _numberBigInt = null;
        _numberBigDecimal = null;
        _binaryValue = null;
        _textBuffer.resetWithEmpty();

        if (pendingValue >= 0) {
            int atom = pendingValue;
            pendingValue = -1;
            if (depth == 0) _parsingContext.expectComma();
            return value(atom);
        }
        if (depth == 0) {
            close();
            return _updateTokenToNull();
        }

        int top = depth - 1;
        int container = containers[top];
        boolean object = "OBJECT".equals(ast.kindAt(container));
        if (nextChild[top] == ast.childCountAt(container)) {
            depth--;
            _parsingContext = _parsingContext.clearAndGetParent();
            return token(object ? JsonToken.END_OBJECT : JsonToken.END_ARRAY);
        }
        int child = ast.childAtomIndexAt(container, nextChild[top]++);
        _parsingContext.expectComma();
        if (!object) return value(child);

        if (!"FIELD".equals(ast.kindAt(child)) || ast.childCountAt(child) != 1) {
            throw refused("object child requires a field with one value");
        }
        text = ast.nameAt(child);
        if (text == null) throw refused("field requires a name");
        _streamReadConstraints.validateNameLength(text.length());
        countText(text);
        _parsingContext.setCurrentName(text);
        pendingValue = ast.childAtomIndexAt(child, 0);
        return token(JsonToken.FIELD_NAME);
    }

    private JsonToken value(int atom) throws IOException {
        String kind = ast.kindAt(atom);
        return switch (kind) {
            case "OBJECT", "ARRAY" -> {
                if (depth == limits.maxDepth()) throw refused("JSON depth budget");
                if (depth == containers.length) {
                    int capacity = (int) Math.min(limits.maxDepth(),
                            Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, 2L * depth)));
                    if (capacity <= depth) throw refused("JSON traversal array budget");
                    containers = Arrays.copyOf(containers, capacity);
                    nextChild = Arrays.copyOf(nextChild, capacity);
                }
                containers[depth] = atom;
                nextChild[depth++] = 0;
                if (kind.equals("OBJECT")) {
                    createChildObjectContext(-1, -1);
                    yield token(JsonToken.START_OBJECT);
                }
                createChildArrayContext(-1, -1);
                yield token(JsonToken.START_ARRAY);
            }
            case "STRING" -> {
                text = ast.valueAt(atom);
                _streamReadConstraints.validateStringLength(text.length());
                countText(text);
                yield token(JsonToken.VALUE_STRING);
            }
            case "NUMBER_INT", "NUMBER_FLOAT" -> {
                text = ast.valueAt(atom);
                countText(text);
                // The authoritative Jackson ingress already validated this lexeme.
                // Identify component lengths for ParserBase; do not parse JSON again.
                int sign = text.charAt(0) == '-' ? 1 : 0;
                if (kind.equals("NUMBER_INT")) {
                    yield token(resetInt(sign != 0, text.length() - sign));
                }
                int exponent = text.indexOf('e');
                if (exponent < 0) exponent = text.indexOf('E');
                int mantissaEnd = exponent < 0 ? text.length() : exponent;
                int point = text.indexOf('.');
                int integerLength = (point < 0 ? mantissaEnd : point) - sign;
                int fractionLength = point < 0 ? 0 : mantissaEnd - point - 1;
                int exponentLength = 0;
                if (exponent >= 0) {
                    int first = exponent + 1;
                    if (text.charAt(first) == '+' || text.charAt(first) == '-') first++;
                    exponentLength = text.length() - first;
                }
                yield token(resetFloat(sign != 0, integerLength, fractionLength, exponentLength));
            }
            case "TRUE" -> token(JsonToken.VALUE_TRUE);
            case "FALSE" -> token(JsonToken.VALUE_FALSE);
            case "NULL" -> token(JsonToken.VALUE_NULL);
            default -> throw refused("unsupported JSON value kind: " + kind);
        };
    }

    private JsonToken token(JsonToken token) throws IOException {
        if (tokens == limits.maxTokens()) throw refused("JSON token budget");
        tokens++;
        return _updateToken(token);
    }

    private void countText(String value) throws IOException {
        if (value.length() > limits.maxTextUnits() - textUnits) {
            throw refused("JSON text budget");
        }
        textUnits += value.length();
    }

    private JsonMappingException refused(String reason) {
        return JsonMappingException.from(this, "M3AST tree binding refused: " + reason);
    }

    @Override public String getText() throws IOException {
        if (_currToken == null) return null;
        if (_currToken == JsonToken.FIELD_NAME) return _parsingContext.getCurrentName();
        return text == null ? _currToken.asString() : text;
    }

    @Override public char[] getTextCharacters() throws IOException {
        String value = getText();
        if (value == null) return null;
        if (bufferedText != value) {
            _textBuffer.resetWithString(value);
            bufferedText = value;
            numberBuffered = false;
        }
        return _textBuffer.getTextBuffer();
    }

    @Override public int getTextLength() throws IOException {
        String value = getText();
        return value == null ? 0 : value.length();
    }

    @Override public int getTextOffset() { return 0; }

    @Override public boolean hasTextCharacters() {
        return _currToken != null && _textBuffer.size() > 0 && _textBuffer.hasTextAsCharacters();
    }

    /** Reuse Jackson's numeric coercion and overflow oracle, only on numeric access. */
    private void bufferNumber() throws IOException {
        if (!numberBuffered && _currToken != null && _currToken.isNumeric()) {
            _textBuffer.resetWithCopy(text, 0, text.length());
            bufferedText = text;
            numberBuffered = true;
        }
    }

    @Override protected void _parseNumericValue(int expectedType) throws IOException {
        bufferNumber();
        super._parseNumericValue(expectedType);
    }

    @Override protected int _parseIntValue() throws IOException {
        bufferNumber();
        return super._parseIntValue();
    }

    @Override public int readBinaryValue(Base64Variant variant, OutputStream output) throws IOException {
        byte[] value = getBinaryValue(variant);
        output.write(value);
        return value.length;
    }

    @Override public JsonLocation getTokenLocation() { return JsonLocation.NA; }
    @Override public JsonLocation getCurrentLocation() { return JsonLocation.NA; }
    @Override protected JsonLocation _currentLocationMinusOne() { return JsonLocation.NA; }
    @Override public long getTokenCharacterOffset() { return -1; }
    @Override public int getTokenLineNr() { return -1; }
    @Override public int getTokenColumnNr() { return -1; }

    @Override protected void _closeInput() {
        ast = null;
        codec = null;
        text = null;
        bufferedText = null;
        containers = NO_STACK;
        nextChild = NO_STACK;
        depth = 0;
        pendingValue = -1;
        _numberString = null;
        _numberBigInt = null;
        _numberBigDecimal = null;
        _binaryValue = null;
        _byteArrayBuilder = null;
        _textBuffer.resetWithEmpty();
        _parsingContext = JsonReadContext.createRootContext(-1, -1, null);
    }
}
