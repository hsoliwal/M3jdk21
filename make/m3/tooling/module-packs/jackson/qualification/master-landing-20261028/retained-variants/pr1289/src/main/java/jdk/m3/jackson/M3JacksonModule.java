// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.AccessPattern;
import java.io.IOException;
import java.io.Serial;
import java.util.Objects;

/** Opt-in real M3AST datatype binding for the original Jackson ObjectMapper. */
public final class M3JacksonModule extends SimpleModule {
    @Serial private static final long serialVersionUID = 1L;

    /** Provider constructor for Jackson Module service discovery. */
    public M3JacksonModule() { this(M3JsonLimits.DEFAULT); }

    public M3JacksonModule(M3JsonLimits limits) {
        super("M3Jackson", new Version(1, 0, 0, null, "jdk.m3", "jackson"));
        Objects.requireNonNull(limits, "limits");
        addSerializer(M3AST.class, new AstSerializer(limits));
        addDeserializer(M3AST.class, new AstDeserializer(limits));
    }

    // The companion constructor uses exactly the two factory updates performed by
    // this module's SimpleModule.setupModule, without exposing its constructing owner.
    SerializerFactory withSerializers(SerializerFactory factory) {
        return factory.withAdditionalSerializers(_serializers);
    }

    DefaultDeserializationContext withDeserializers(DefaultDeserializationContext context) {
        return context.with(context.getFactory().withAdditionalDeserializers(_deserializers));
    }

    private static final class AstSerializer extends StdSerializer<M3AST> {
        @Serial private static final long serialVersionUID = 1L;
        private final M3JsonLimits limits;
        AstSerializer(M3JsonLimits limits) { super(M3AST.class); this.limits = limits; }
        @Override public void serialize(M3AST value, JsonGenerator generator,
                                        SerializerProvider provider) throws IOException {
            M3JsonCodec.write(value, generator, limits);
        }
    }

    private static final class AstDeserializer extends StdDeserializer<M3AST> {
        @Serial private static final long serialVersionUID = 1L;
        private final M3JsonLimits limits;
        AstDeserializer(M3JsonLimits limits) { super(M3AST.class); this.limits = limits; }
        @Override public M3AST deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            return M3JsonCodec.read(parser, limits);
        }
        @Override public AccessPattern getNullAccessPattern() { return AccessPattern.DYNAMIC; }
        @Override public M3AST getAbsentValue(DeserializationContext context) { return null; }
        /** A root or nested JSON null remains an AST null atom, not a missing AST reference. */
        @Override public M3AST getNullValue(DeserializationContext context) throws JsonMappingException {
            try {
                return M3JsonCodec.nullValue(limits);
            } catch (IllegalArgumentException failure) {
                throw JsonMappingException.from(context == null ? null : context.getParser(),
                        "M3AST null admission refused: " + failure.getMessage(), failure);
            }
        }
    }
}
