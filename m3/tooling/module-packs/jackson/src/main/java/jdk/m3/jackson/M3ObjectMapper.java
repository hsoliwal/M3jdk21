/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serial;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;


/**
 * Jackson ObjectMapper with indexed M3 file and NIO input boundaries.
 *
 * <p>The upstream mapper remains the authority for binding, annotations, modules,
 * coercion, duplicate fields and errors. Existing String APIs remain unchanged;
 * the M3JDK runtime owns their internal M3String representation. This class adds
 * no string pool, JSON parser, reflection engine or retained text payload.
 * Configure a mapper before concurrent use, as required by ObjectMapper.
 *
 * <p>Methods accepting M3File open a forward reader over its canonical atom
 * references. Methods accepting Path open a byte stream so Jackson retains its
 * encoding detection. These methods own and close only the streams they open,
 * including when AUTO_CLOSE_SOURCE or AUTO_CLOSE_TARGET is disabled.
 */
public class M3ObjectMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;

    private final M3JsonLimits m3Limits;

    public M3ObjectMapper() { this(null, M3JsonLimits.DEFAULT); }
    public M3ObjectMapper(JsonFactory factory) { this(factory, M3JsonLimits.DEFAULT); }
    /** Named limits factory preserves source compatibility of new M3ObjectMapper(null). */
    public static M3ObjectMapper withLimits(M3JsonLimits limits) {
        return new M3ObjectMapper(null, limits);
    }
    public M3ObjectMapper(JsonFactory factory, M3JsonLimits limits) {
        super(factory);
        m3Limits = Objects.requireNonNull(limits, "limits");
        // Match the final binding's Jackson setup operations without publishing this
        // through SetupContext or calling subclass registration/configuration hooks.
        M3JacksonModule binding = new M3JacksonModule(m3Limits);
        boolean install = true;
        if (_serializationConfig.isEnabled(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)) {
            Object typeId = binding.getTypeId();
            if (typeId != null) {
                if (_registeredModuleTypes == null) _registeredModuleTypes = new LinkedHashSet<>();
                install = _registeredModuleTypes.add(typeId);
            }
        }
        if (install) {
            _serializerFactory = binding.withSerializers(_serializerFactory);
            _deserializationContext = binding.withDeserializers(_deserializationContext);
        }
    }

    protected M3ObjectMapper(M3ObjectMapper source) {
        super(source);
        m3Limits = source.m3Limits;
    }

    protected M3ObjectMapper(M3ObjectMapper source, JsonFactory factory) {
        super(source, factory);
        m3Limits = source.m3Limits;
    }

    public M3JsonLimits m3Limits() { return m3Limits; }

    /** Retains Jackson's isolated-copy semantics and this mapper's added APIs. */
    @Override public M3ObjectMapper copy() {
        _checkInvalidCopy(M3ObjectMapper.class);
        return new M3ObjectMapper(this);
    }

    @Override public M3ObjectMapper copyWith(JsonFactory factory) {
        _checkInvalidCopy(M3ObjectMapper.class);
        return new M3ObjectMapper(this, factory);
    }

    public <T> T readValue(M3File source, Class<T> type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var reader = Objects.requireNonNull(source, "source").reader()) {
            return super.readValue(reader, type);
        }
    }

    public <T> T readValue(M3File source, TypeReference<T> type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var reader = Objects.requireNonNull(source, "source").reader()) {
            return super.readValue(reader, type);
        }
    }

    public <T> T readValue(M3File source, JavaType type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var reader = Objects.requireNonNull(source, "source").reader()) {
            return super.readValue(reader, type);
        }
    }

    public JsonNode readTree(M3File source) throws IOException {
        try (var reader = Objects.requireNonNull(source, "source").reader()) {
            return super.readTree(reader);
        }
    }

    public <T> T readValue(Path source, Class<T> type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var stream = Files.newInputStream(Objects.requireNonNull(source, "source"))) {
            return super.readValue(stream, type);
        }
    }

    public <T> T readValue(Path source, TypeReference<T> type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var stream = Files.newInputStream(Objects.requireNonNull(source, "source"))) {
            return super.readValue(stream, type);
        }
    }

    public <T> T readValue(Path source, JavaType type) throws IOException {
        Objects.requireNonNull(type, "type");
        try (var stream = Files.newInputStream(Objects.requireNonNull(source, "source"))) {
            return super.readValue(stream, type);
        }
    }

    public JsonNode readTree(Path source) throws IOException {
        try (var stream = Files.newInputStream(Objects.requireNonNull(source, "source"))) {
            return super.readTree(stream);
        }
    }

    /** Reads an owned indexed sequence; close the iterator when stopping early. */
    public <T> MappingIterator<T> readValues(M3File source, Class<T> type) throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    public <T> MappingIterator<T> readValues(M3File source, TypeReference<T> type)
            throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    public <T> MappingIterator<T> readValues(M3File source, JavaType type) throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    /** Reads an owned byte-stream sequence with Jackson's normal encoding detection. */
    public <T> MappingIterator<T> readValues(Path source, Class<T> type) throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    public <T> MappingIterator<T> readValues(Path source, TypeReference<T> type)
            throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    public <T> MappingIterator<T> readValues(Path source, JavaType type) throws IOException {
        return M3Binding.readSequence(readerFor(Objects.requireNonNull(type, "type")), source);
    }

    /** Serializes to indexed String chunks, without flattening the document. */
    public M3File writeValueAsM3File(Object value) throws IOException {
        return writeValueAsM3File(value, Integer.MAX_VALUE);
    }

    /** Serializes to indexed output with an explicit UTF-16 length limit. */
    public M3File writeValueAsM3File(Object value, int maxUtf16Length) throws IOException {
        try (M3File.Writer output = M3File.newWriter(maxUtf16Length)) {
            super.writeValue(output, value);
            return output.finish();
        }
    }

    /** Writes through the Path provider, with normal create/truncate semantics. */
    public void writeValue(Path destination, Object value) throws IOException {
        try (var stream = Files.newOutputStream(Objects.requireNonNull(destination, "destination"))) {
            super.writeValue(stream, value);
        }
    }

    /** Prepare Jackson's existing immutable typed reader and writer once. */
    public <T> M3Binding<T> prepare(Class<T> type) {
        return prepare(getTypeFactory().constructType(Objects.requireNonNull(type, "type")));
    }

    public <T> M3Binding<T> prepare(TypeReference<T> type) {
        return prepare(getTypeFactory().constructType(Objects.requireNonNull(type, "type")));
    }

    public <T> M3Binding<T> prepare(JavaType type) {
        Objects.requireNonNull(type, "type");
        return new M3Binding<>(type, readerFor(type), writerFor(type));
    }

    public <T> T readValue(CharSequence source, Class<T> valueType) throws IOException {
        try (var reader = new StringReader(stable(source))) { return super.readValue(reader, valueType); }
    }

    public <T> T readValue(CharSequence source, TypeReference<T> valueType) throws IOException {
        try (var reader = new StringReader(stable(source))) { return super.readValue(reader, valueType); }
    }

    public <T> T readValue(CharSequence source, JavaType valueType) throws IOException {
        try (var reader = new StringReader(stable(source))) { return super.readValue(reader, valueType); }
    }

    public JsonNode readTree(CharSequence source) throws IOException {
        try (var reader = new StringReader(stable(source))) { return super.readTree(reader); }
    }

    /** Consume exactly one complete finite JSON root with the configured Jackson factory. */
    public M3AST readAST(CharSequence source) throws IOException {
        try (JsonParser parser = getFactory().createParser(stable(source))) {
            M3AST result = M3JsonCodec.read(parser, m3Limits);
            if (parser.nextToken() != null) throw JsonMappingException.from(parser, "trailing JSON content");
            return result;
        }
    }

    /** Explicit strict-decoder source snapshot before JSON projection. */
    public M3AST readAST(Path path, Charset charset, int maxBytes) throws IOException {
        return readAST(M3File.readBytes(path, charset, maxBytes));
    }

    /**
     * Parses a bounded file as a stream, without creating a whole-file byte array,
     * String snapshot or per-atom Java object. Malformed character sequences fail closed.
     *
     * <p>The resulting AST retains only its indexed atoms and scalar text; unlike
     * {@link M3File}, this route does not retain a source snapshot or byte digest.
     */
    public M3AST readASTStreaming(Path path, Charset charset, long maxBytes) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        if (maxBytes < 0) throw new IllegalArgumentException("negative source byte limit");
        try (InputStream bytes = new BoundedInput(Files.newInputStream(path), maxBytes);
             Reader characters = new InputStreamReader(bytes, charset.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT)
                     .onUnmappableCharacter(CodingErrorAction.REPORT));
             JsonParser parser = getFactory().createParser(characters)) {
            M3AST result = M3JsonCodec.read(parser, m3Limits);
            if (parser.nextToken() != null) {
                throw JsonMappingException.from(parser, "trailing JSON content");
            }
            return result;
        }
    }

    /** Byte-budget enforcement must also cover streams that grow while being read. */
    private static final class BoundedInput extends FilterInputStream {
        private long remaining;

        BoundedInput(InputStream input, long maxBytes) {
            super(input);
            remaining = maxBytes;
        }

        @Override public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                if (remaining == 0) throw new IOException("source exceeds byte budget");
                remaining--;
            }
            return value;
        }

        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            if (length == 0) return 0;
            if (remaining == 0) {
                if (super.read() != -1) throw new IOException("source exceeds byte budget");
                return -1;
            }
            int result = super.read(bytes, offset, (int) Math.min(remaining, length));
            if (result > 0) remaining -= result;
            return result;
        }

        @Override public long skip(long count) { return 0; }
    }

    /** JSON projection is an explicit materialization boundary; ordinary Jackson binds the result. */
    public <T> T readValue(M3AST ast, Class<T> valueType) throws IOException {
        return super.readValue(writeValueAsString(Objects.requireNonNull(ast, "ast")), valueType);
    }

    public <T> T readValue(M3AST ast, TypeReference<T> valueType) throws IOException {
        return super.readValue(writeValueAsString(Objects.requireNonNull(ast, "ast")), valueType);
    }

    public <T> T readValue(M3AST ast, JavaType valueType) throws IOException {
        return super.readValue(writeValueAsString(Objects.requireNonNull(ast, "ast")), valueType);
    }

    /**
     * Bind the existing authoritative tree through its original indexed tokens.
     * No JSON text is written, read or parsed. The AST remains immutable and owned
     * by its caller; only this binding cursor is closed.
     *
     * <p>Ordinary Jackson deserializers, naming/coercion settings, object identities,
     * parser features and token-kind read constraints apply. M3 retained-state and
     * expanded-traversal budgets apply as well. The AST has no source locations:
     * mapping failures report an unavailable location rather than invented offsets.
     *
     * <p>This explicit tree operation does not invoke an M3AST serializer, a
     * generator, input/output decorators or format-specific source processing.
     * Source-length limits have no byte/character stream to measure here. The
     * existing {@code readValue(M3AST, ...)} overloads keep their serializer-based
     * JSON materialization contract.
     */
    public <T> T readTreeValue(M3AST ast, Class<T> valueType) throws IOException {
        return readTreeValue(ast, getTypeFactory().constructType(
                Objects.requireNonNull(valueType, "valueType")));
    }

    /** Bind original AST tokens to a parameterized type without a JSON round-trip. */
    public <T> T readTreeValue(M3AST ast, TypeReference<T> valueType) throws IOException {
        return readTreeValue(ast, getTypeFactory().constructType(
                Objects.requireNonNull(valueType, "valueType")));
    }

    /** Bind original AST tokens with this mapper's configured Jackson deserialization. */
    @SuppressWarnings("unchecked")
    public <T> T readTreeValue(M3AST ast, JavaType valueType) throws IOException {
        Objects.requireNonNull(ast, "ast");
        Objects.requireNonNull(valueType, "valueType");
        return (T) _readMapAndClose(new M3ASTParser(ast, getFactory(), this, m3Limits), valueType);
    }

    private static String stable(CharSequence source) {
        return Objects.requireNonNull(source, "source").toString();
    }
}
