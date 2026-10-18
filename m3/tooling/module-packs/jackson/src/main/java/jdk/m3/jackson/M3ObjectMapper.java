// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serial;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;

/** Original Jackson databinding plus opt-in immutable M3 file/text/AST entry points. */
public class M3ObjectMapper extends ObjectMapper {
    @Serial private static final long serialVersionUID = 1L;
    private final M3JsonLimits m3Limits;

    public M3ObjectMapper() { this(null, M3JsonLimits.DEFAULT); }
    public M3ObjectMapper(JsonFactory factory) { this(factory, M3JsonLimits.DEFAULT); }
    public M3ObjectMapper(M3JsonLimits limits) { this(null, limits); }
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

    protected M3ObjectMapper(M3ObjectMapper source, JsonFactory factory) {
        super(source, factory);
        m3Limits = source.m3Limits;
    }

    public M3JsonLimits m3Limits() { return m3Limits; }

    @Override public M3ObjectMapper copy() {
        _checkInvalidCopy(M3ObjectMapper.class);
        return new M3ObjectMapper(this, null);
    }

    @Override public M3ObjectMapper copyWith(JsonFactory factory) {
        _checkInvalidCopy(M3ObjectMapper.class);
        return new M3ObjectMapper(this, factory);
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
        return readAST(M3File.read(path, charset, maxBytes));
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

    private static String stable(CharSequence source) {
        return Objects.requireNonNull(source, "source").toString();
    }
}
