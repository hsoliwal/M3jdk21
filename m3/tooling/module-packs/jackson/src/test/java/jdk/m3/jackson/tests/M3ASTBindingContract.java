// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.jackson.tests;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/** Real Jackson binding and token-contract proof; no substitute parser or AST fixtures. */
public final class M3ASTBindingContract {
    private static final M3JsonLimits ROOM =
            new M3JsonLimits(2_000_000, 2_000_000, 4_000_000, 67_108_864, 4096);
    private static int checks;

    private M3ASTBindingContract() { }

    public static void main(String[] arguments) throws Exception { verify(); }

    public static void verify() throws Exception {
        checks = 0;
        shapesAndOverloads();
        sourceAndSerializationAreNotInvoked();
        originalSerializerContract();
        originalNumericOracle();
        nonFiniteFactoryPolicy();
        originalDeserializerConfiguration();
        tokenContextsAndLifetime();
        currentValues();
        closeReleasesCursor();
        binaryAndTextAccess();
        sharedOccurrencesAndLimits();
        streamConstraints();
        isolatedSubtree();
        deepCursor();
        System.out.println("M3_AST_BINDING_PASS checks=" + checks
                + " direct_source_parsers=0 direct_generators=0 direct_serializers=0");
    }

    private static void shapesAndOverloads() throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        ObjectMapper oracle = new ObjectMapper();
        String[] inputs = {
            "null", "true", "false", "0", "-2.5e+2", "\"\"", "[]", "{}",
            "{\"a\":1,\"a\":2,\"empty\":[],\"object\":{}}",
            "[null,true,false,\"quote\\\"slash\\\\line\\n\",[1,2],{\"n\":3}]",
            "\"\\u0000\\u001f\\uD800\\uDC00\\uD800x\\uDC00\"",
            "{\"\\\"\\\\\\n\":\"\\b\\f\\r\\t\\/\\u2028\\u2029\"}"
        };
        for (String input : inputs) {
            M3AST ast = mapper.readAST(input);
            Object expected = oracle.readValue(input, Object.class);
            equal(expected, mapper.readTreeValue(ast, Object.class), "class binding " + input);
            equal(expected, mapper.readTreeValue(ast, new TypeReference<Object>() { }),
                    "type-reference binding " + input);
            equal(expected, mapper.readTreeValue(ast, mapper.constructType(Object.class)),
                    "JavaType binding " + input);
        }
        M3AST records = mapper.readAST("[{\"name\":\"a\",\"count\":2},{\"name\":\"b\",\"count\":3}]");
        TypeReference<List<Item>> type = new TypeReference<>() { };
        equal(List.of(new Item("a", 2), new Item("b", 3)), mapper.readTreeValue(records, type),
                "parameterized records");
        expect(NullPointerException.class, () -> mapper.readTreeValue(null, Object.class), "null AST");
        M3AST ast = mapper.readAST("0");
        expect(NullPointerException.class, () -> mapper.readTreeValue(ast, (Class<?>) null), "null class");
        expect(NullPointerException.class, () -> mapper.readTreeValue(ast, (TypeReference<?>) null), "null reference");
        expect(NullPointerException.class, () -> mapper.readTreeValue(ast, (JavaType) null), "null JavaType");
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        expect(JsonMappingException.class, () -> mapper.readTreeValue(mapper.readAST("null"), int.class),
                "primitive null policy");
    }

    private static void sourceAndSerializationAreNotInvoked() throws Exception {
        GuardedFactory factory = new GuardedFactory();
        M3ObjectMapper mapper = new M3ObjectMapper(factory, ROOM);
        M3AST ast = mapper.readAST("{\"name\":\"retained\",\"count\":7}");
        equal(1, factory.parsers, "one authoritative ingress parser");
        long hash = ast.structuralHash64();
        int atoms = ast.atomCount();
        int references = ast.childReferenceCount();
        int[] serializers = {0};
        SimpleModule module = new SimpleModule();
        module.addSerializer(M3AST.class, new StdSerializer<M3AST>(M3AST.class) {
            @Serial private static final long serialVersionUID = 1L;
            @Override public void serialize(M3AST value, JsonGenerator generator, SerializerProvider provider) {
                serializers[0]++;
                throw new AssertionError("direct tree binding invoked a serializer");
            }
        });
        mapper.registerModule(module);
        factory.blockSource = true;
        for (int i = 0; i < 4; i++) {
            equal(new Item("retained", 7), mapper.readTreeValue(ast, Item.class), "reuse class binding");
            equal(new Item("retained", 7), mapper.readTreeValue(ast, new TypeReference<Item>() { }),
                    "reuse generic binding");
            equal(new Item("retained", 7), mapper.readTreeValue(ast, mapper.constructType(Item.class)),
                    "reuse JavaType binding");
        }
        equal(1, factory.parsers, "zero additional source parsers");
        equal(0, factory.generators, "zero JSON generators");
        equal(0, serializers[0], "zero serializers");
        equal(hash, ast.structuralHash64(), "binding preserves original tree");
        equal(atoms, ast.atomCount(), "no added retained atoms");
        equal(references, ast.childReferenceCount(), "no added retained references");
        check(ast.sameStructure(ast.root().asAST()), "original root identity retained");
    }

    private static void originalSerializerContract() throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        M3AST ast = mapper.readAST("{\"name\":\"tree\",\"count\":7}");
        int[] calls = {0};
        SimpleModule module = new SimpleModule();
        module.addSerializer(M3AST.class, new StdSerializer<M3AST>(M3AST.class) {
            @Serial private static final long serialVersionUID = 1L;
            @Override public void serialize(M3AST value, JsonGenerator generator, SerializerProvider provider)
                    throws IOException {
                calls[0]++;
                generator.writeStartObject();
                generator.writeStringField("name", "serialized");
                generator.writeNumberField("count", 99);
                generator.writeEndObject();
            }
        });
        mapper.registerModule(module);
        equal(new Item("tree", 7), mapper.readTreeValue(ast, Item.class), "explicit raw-tree policy");
        equal(0, calls[0], "raw-tree binding bypasses serializer by contract");
        Item serialized = new Item("serialized", 99);
        equal(serialized, mapper.readValue(ast, Item.class), "legacy class serializer contract");
        equal(serialized, mapper.readValue(ast, new TypeReference<Item>() { }), "legacy generic serializer contract");
        equal(serialized, mapper.readValue(ast, mapper.constructType(Item.class)), "legacy JavaType serializer contract");
        equal(3, calls[0], "all legacy overloads still call the registered serializer");
        equal(new Item("tree", 7), mapper.copy().readTreeValue(ast, Item.class), "copy retains direct binding");
    }

    private static void originalNumericOracle() throws Exception {
        String[] numbers = {
            "0", "-0", "1", "-1", "2147483647", "-2147483648", "2147483648", "-2147483649",
            "9223372036854775807", "-9223372036854775808", "9223372036854775808",
            "-9223372036854775809", "12345678901234567890123456789012345678901234567890",
            "0.0", "-0.0", "1.2300", "-2.5e+2", "1E-400", "1e400", "1.0000000596046448",
            "3.4028234663852886e38", "1.7976931348623157e308", "1e1000000"
        };
        List<ProbeRead> reads = List.of(
            p -> new Probe(p.getText() + "|" + p.currentToken() + "|" + p.getNumberValue().getClass().getName()
                    + "|" + p.getNumberValue()),
            p -> new Probe(p.getNumberValueExact().getClass().getName() + "|" + p.getNumberValueExact()),
            p -> new Probe(Long.toHexString(Double.doubleToRawLongBits(p.getDoubleValue()))),
            p -> new Probe(Integer.toHexString(Float.floatToRawIntBits(p.getFloatValue()))),
            p -> new Probe(Integer.toString(p.getIntValue())),
            p -> new Probe(Long.toString(p.getLongValue())),
            p -> new Probe(p.getBigIntegerValue().toString()),
            p -> new Probe(p.getDecimalValue().toString()),
            p -> {
                String text = new String(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
                return new Probe(text + "|" + p.getNumberValue() + "|" + p.getDecimalValue());
            }
        );
        M3ObjectMapper ingress = M3ObjectMapper.withLimits(ROOM);
        for (String number : numbers) {
            M3AST ast = ingress.readAST(number);
            for (ProbeRead read : reads) {
                M3ObjectMapper mapper = probeMapper(read);
                equal(observe(() -> mapper.readValue(number, Probe.class)),
                        observe(() -> mapper.readTreeValue(ast, Probe.class)), "original numeric oracle " + number);
            }
        }
        M3ObjectMapper big = M3ObjectMapper.withLimits(ROOM);
        big.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        String source = "[9223372036854775808,1.2300,-0.0]";
        equal(big.readValue(source, Object.class), big.readTreeValue(ingress.readAST(source), Object.class),
                "configured number type coercion");
    }

    private static void nonFiniteFactoryPolicy() throws Exception {
        JsonFactory factory = JsonFactory.builder().enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS).build();
        M3ObjectMapper mapper = new M3ObjectMapper(factory, ROOM);
        for (String source : new String[] {"NaN", "Infinity", "-Infinity"}) {
            Double original = mapper.readValue(source, Double.class);
            check(source.equals("NaN") ? original.isNaN()
                    : original.isInfinite() && (original < 0) == source.startsWith("-"),
                    "ordinary inherited Jackson retains configured non-finite support " + source);
            JsonMappingException refusal = expect(JsonMappingException.class,
                    () -> mapper.readAST(source), "existing finite M3AST admission " + source);
            check(refusal.getOriginalMessage().contains("finite JSON numbers"),
                    "non-finite refusal belongs to existing authoritative AST ingress");
        }
        M3ObjectMapper ingress = M3ObjectMapper.withLimits(ROOM);
        for (String source : new String[] {"1e400", "-1e400", "1e-400", "-0.0"}) {
            equal(mapper.readValue(source, Double.class),
                    mapper.readTreeValue(ingress.readAST(source), Double.class),
                    "finite lexeme conversion with permissive factory " + source);
        }
    }

    private static void originalDeserializerConfiguration() throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        equal(new Named("Ada"), mapper.readTreeValue(mapper.readAST("{\"first_name\":\"Ada\",\"extra\":9}"),
                Named.class), "naming and unknown-property policy");
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        equal(List.of(7), mapper.readTreeValue(mapper.readAST("7"), new TypeReference<List<Integer>>() { }),
                "single-value collection coercion");
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        equal(Map.of("x", 1), mapper.readTreeValue(mapper.readAST("{\"x\":1}"),
                new TypeReference<Map<String, Integer>>() { }), "single complete tree under trailing-token policy");

        M3ObjectMapper codecMapper = probeMapper(p -> {
            check(p.getCodec() instanceof M3ObjectMapper, "original mapper is the cursor codec");
            Item value = p.readValueAs(Item.class);
            return new Probe(value.name() + ":" + value.count());
        });
        equal(new Probe("codec:8"), codecMapper.readTreeValue(
                mapper.readAST("{\"name\":\"codec\",\"count\":8}"), Probe.class),
                "custom deserializer calls original codec");

        M3ObjectMapper ids = M3ObjectMapper.withLimits(ROOM);
        List<Identity> values = ids.readTreeValue(
                ids.readAST("[{\"id\":1,\"next\":2},{\"id\":2,\"next\":1}]"),
                new TypeReference<List<Identity>>() { });
        check(values.get(0).next == values.get(1) && values.get(1).next == values.get(0),
                "original Jackson forward object identity resolution");
        expect(JsonMappingException.class,
                () -> ids.readTreeValue(ids.readAST("[{\"id\":1,\"next\":2}]"),
                        new TypeReference<List<Identity>>() { }), "unresolved object IDs checked when binding closes");
        expect(JsonMappingException.class, () -> ids.readTreeValue(ids.readAST("\"wrong\""), int.class),
                "original incompatible-scalar failure");
    }

    private static void tokenContextsAndLifetime() throws Exception {
        JsonParser[] seen = {null};
        ProbeRead scan = p -> {
            seen[0] = p;
            StringBuilder observation = new StringBuilder();
            do {
                observation.append(p.currentToken()).append('|').append(p.currentName()).append('|')
                        .append(p.getParsingContext().typeDesc()).append('|')
                        .append(p.getParsingContext().getCurrentIndex()).append('|').append(p.getText()).append('\n');
            } while (p.nextToken() != null);
            return new Probe(observation.toString());
        };
        String source = "{\"a\":[1,{\"b\":true}],\"a\":null,\"empty\":{}}";
        M3ObjectMapper mapper = probeMapper(scan);
        Probe expected = mapper.readValue(source, Probe.class);
        M3AST ast = M3ObjectMapper.withLimits(ROOM).readAST(source);
        equal(expected, mapper.readTreeValue(ast, Probe.class), "native token order, names and context indexes");
        check(seen[0].isClosed(), "cursor closes after successful binding");
        equal(null, seen[0].nextToken(), "closed cursor has no further token");
        equal(JsonLocation.NA, seen[0].currentLocation(), "no fabricated current source offset");
        equal(JsonLocation.NA, seen[0].currentTokenLocation(), "no fabricated token source offset");

        mapper = probeMapper(p -> {
            seen[0] = p;
            p.nextToken();
            p.overrideCurrentName("renamed");
            equal("renamed", p.currentName(), "current field name override");
            equal("renamed", p.getText(), "text follows current field name override");
            p.skipChildren();
            equal(JsonToken.FIELD_NAME, p.currentToken(), "skipChildren on a field is a no-op");
            p.nextToken();
            p.skipChildren();
            equal(JsonToken.END_ARRAY, p.currentToken(), "skipChildren lands at original end event");
            p.nextToken();
            throw JsonMappingException.from(p, "intentional deserializer failure");
        });
        M3ObjectMapper failing = mapper;
        JsonMappingException failure = expect(JsonMappingException.class,
                () -> failing.readTreeValue(ast, Probe.class), "custom deserializer failure retained");
        check(failure.getOriginalMessage().contains("intentional deserializer failure"), "original error is retained");
        equal(JsonLocation.NA, failure.getLocation(), "error location is explicitly unavailable");
        check(seen[0].isClosed(), "cursor closes after failed binding");
    }

    private static void currentValues() throws Exception {
        ProbeRead read = p -> {
            Object objectValue = new Object();
            Object arrayValue = new Object();
            p.assignCurrentValue(objectValue);
            check(p.currentValue() == objectValue, "object owns assigned value");
            p.nextToken();
            check(p.currentValue() == objectValue, "field retains object value");
            p.nextToken();
            equal(null, p.currentValue(), "new child context starts without an assigned value");
            p.assignCurrentValue(arrayValue);
            p.nextToken();
            check(p.currentValue() == arrayValue, "array element retains array value");
            p.nextToken();
            check(p.currentValue() == objectValue, "array end restores parent object value");
            p.nextToken();
            check(p.currentValue() == objectValue, "next field retains parent value");
            p.nextToken();
            p.nextToken();
            equal(null, p.currentValue(), "object end restores root value");
            return new Probe("context values");
        };
        M3ObjectMapper mapper = probeMapper(read);
        String source = "{\"a\":[0],\"b\":true}";
        Probe expected = mapper.readValue(source, Probe.class);
        equal(expected, mapper.readTreeValue(M3ObjectMapper.withLimits(ROOM).readAST(source), Probe.class),
                "native JsonReadContext value lifecycle");
    }

    private static void closeReleasesCursor() throws Exception {
        String[] inputs = {
            "\"retained-normal-scalar\"", "\"retained-eof-scalar\"", "\"retained-failure-scalar\"",
            "123456789012345678901234567890.001", "123456789012345678901234567890"
        };
        M3ObjectMapper ingress = M3ObjectMapper.withLimits(ROOM);
        for (int scenario = 0; scenario < inputs.length; scenario++) {
            final int mode = scenario;
            JsonParser[] seen = {null};
            TextBuffer[] buffer = {null};
            JsonStreamContext[] context = {null};
            M3ObjectMapper mapper = probeMapper(p -> {
                seen[0] = p;
                context[0] = p.getParsingContext();
                p.assignCurrentValue(new Object());
                String value = p.getText();
                p.getTextCharacters();
                if (mode == 3) p.getDecimalValue();
                if (mode == 4) p.getBigIntegerValue();
                buffer[0] = (TextBuffer) field(ParserBase.class, p, "_textBuffer");
                check(buffer[0].size() > 0, "fixture retains a current scalar before close");
                if (mode == 1) equal(null, p.nextToken(), "explicit EOF closes scalar cursor");
                if (mode == 2) throw JsonMappingException.from(p, "retained-scalar failure");
                return new Probe(value);
            });
            M3AST ast = ingress.readAST(inputs[scenario]);
            if (scenario == 2) {
                expect(JsonMappingException.class, () -> mapper.readTreeValue(ast, Probe.class),
                        "deserializer failure closes the cursor");
            } else {
                mapper.readTreeValue(ast, Probe.class);
            }
            JsonParser parser = seen[0];
            check(parser.isClosed(), "binding cursor closed in scenario " + scenario);
            equal(0, buffer[0].size(), "closed token buffer empty in scenario " + scenario);
            equal("", buffer[0].contentsAsString(), "closed cursor releases last scalar String");
            for (String name : new String[] {"ast", "codec", "text", "bufferedText"}) {
                equal(null, field(parser.getClass(), parser, name), "closed cursor releases " + name);
            }
            for (String name : new String[] {"_numberString", "_numberBigInt", "_numberBigDecimal",
                    "_binaryValue", "_byteArrayBuilder"}) {
                equal(null, field(ParserBase.class, parser, name), "closed cursor releases " + name);
            }
            equal(0, ((int[]) field(parser.getClass(), parser, "containers")).length,
                    "closed cursor releases container lane");
            equal(0, ((int[]) field(parser.getClass(), parser, "nextChild")).length,
                    "closed cursor releases child-ordinal lane");
            check(parser.getParsingContext() != context[0], "closed cursor drops the binding context");
            check(parser.getParsingContext().inRoot(), "closed cursor keeps only an empty root context");
            equal(null, parser.getParsingContext().getParent(), "closed root has no enclosing context");
            equal(null, parser.currentValue(), "closed context releases deserialized current value");
            equal(null, parser.currentName(), "closed context releases field name");
        }
    }

    /** Test-only module opens admit inspection; product packages gain no open directive. */
    private static Object field(Class<?> owner, Object value, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(value);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("closed-cursor lifetime inspection", failure);
        }
    }

    private static void binaryAndTextAccess() throws Exception {
        M3ObjectMapper ingress = M3ObjectMapper.withLimits(ROOM);
        M3AST scalar = ingress.readAST("\"same retained value\"");
        String retained = scalar.valueAt(scalar.rootAtomIndex());
        M3ObjectMapper mapper = probeMapper(p -> {
            check(p.getText() == retained, "getText returns the retained scalar reference");
            char[] first = p.getTextCharacters();
            check(first == p.getTextCharacters(), "current scalar character array reused");
            equal(retained, new String(first, p.getTextOffset(), p.getTextLength()), "text array contract");
            return new Probe(p.getValueAsString());
        });
        equal(new Probe(retained), mapper.readTreeValue(scalar, Probe.class), "retained text binding");
        mapper = probeMapper(p -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int count = p.readBinaryValue(bytes);
            equal(4, count, "binary byte count");
            equal(List.of(0, 1, 2, 255), unsigned(bytes.toByteArray()), "original Base64 decoding");
            equal(unsigned(bytes.toByteArray()), unsigned(p.getBinaryValue()), "binary value reuse");
            return new Probe("binary");
        });
        equal(new Probe("binary"), mapper.readTreeValue(ingress.readAST("\"AAEC/w==\""), Probe.class),
                "binary binding from existing scalar");
        expect(JsonMappingException.class,
                () -> ingress.readTreeValue(ingress.readAST("\"!!!!\""), byte[].class), "invalid Base64 error");
    }

    private static void sharedOccurrencesAndLimits() throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        M3AST shared = mapper.readAST("[{\"x\":[1,1]},{\"x\":[1,1]}]");
        equal(shared.arrayElementAt(shared.rootAtomIndex(), 0), shared.arrayElementAt(shared.rootAtomIndex(), 1),
                "fixture actually shares original subtree atoms");
        equal(mapper.readValue("[{\"x\":[1,1]},{\"x\":[1,1]}]", Object.class),
                mapper.readTreeValue(shared, Object.class), "every shared subtree occurrence is emitted");
        M3ObjectMapper fewTokens = M3ObjectMapper.withLimits(new M3JsonLimits(3, 1000, 1000, 10000, 100));
        expect(JsonMappingException.class, () -> fewTokens.readTreeValue(shared, Object.class),
                "expanded shared occurrences count toward token budget");
        String repeated = "[\"" + "0123456789".repeat(4) + "\",\"" + "0123456789".repeat(4) + "\",\""
                + "0123456789".repeat(4) + "\",\"" + "0123456789".repeat(4) + "\"]";
        M3AST repeatedAst = mapper.readAST(repeated);
        M3ObjectMapper fewText = M3ObjectMapper.withLimits(new M3JsonLimits(100, 100, 100, 100, 100));
        expect(JsonMappingException.class, () -> fewText.readTreeValue(repeatedAst, Object.class),
                "expanded text occurrences count toward text budget");
        expect(JsonMappingException.class,
                () -> M3ObjectMapper.withLimits(new M3JsonLimits(100, 1, 100, 10000, 100))
                        .readTreeValue(shared, Object.class), "retained atom admission");
        expect(JsonMappingException.class,
                () -> M3ObjectMapper.withLimits(new M3JsonLimits(100, 100, 1, 10000, 100))
                        .readTreeValue(shared, Object.class), "retained child-reference admission");
        expect(JsonMappingException.class,
                () -> M3ObjectMapper.withLimits(new M3JsonLimits(100, 100, 100, 1, 100))
                        .readTreeValue(shared, Object.class), "retained text admission");
        expect(JsonMappingException.class,
                () -> M3ObjectMapper.withLimits(new M3JsonLimits(100, 100, 100, 10000, 1))
                        .readTreeValue(shared, Object.class), "M3 container depth admission");
    }

    private static void streamConstraints() throws Exception {
        M3ObjectMapper ingress = M3ObjectMapper.withLimits(ROOM);
        constrainedFailure(ingress.readAST("[[]]"), StreamReadConstraints.builder().maxNestingDepth(1).build(),
                "Jackson nesting constraint");
        constrainedFailure(ingress.readAST("\"12345\""), StreamReadConstraints.builder().maxStringLength(4).build(),
                "Jackson string constraint");
        constrainedFailure(ingress.readAST("{\"name\":0}"), StreamReadConstraints.builder().maxNameLength(3).build(),
                "Jackson name constraint");
        constrainedFailure(ingress.readAST("123456"), StreamReadConstraints.builder().maxNumberLength(5).build(),
                "Jackson number constraint");
        constrainedFailure(ingress.readAST("[1,2]"), StreamReadConstraints.builder().maxTokenCount(2).build(),
                "Jackson token count constraint");
        JsonFactory factory = JsonFactory.builder().streamReadConstraints(
                StreamReadConstraints.builder().maxStringLength(1).maxNumberLength(100).maxNameLength(100).build()).build();
        M3ObjectMapper independent = new M3ObjectMapper(factory, ROOM);
        equal(Map.of("long_name", new BigInteger("123456789012345678901234567890")),
                independent.readTreeValue(ingress.readAST("{\"long_name\":123456789012345678901234567890}"),
                        Object.class), "name and numeric limits remain independent of string-value limits");
        JsonFactory duplicates = JsonFactory.builder().build();
        duplicates.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        expect(IOException.class, () -> new M3ObjectMapper(duplicates, ROOM)
                .readTreeValue(ingress.readAST("{\"x\":1,\"x\":2}"), Object.class), "ordinary strict duplicate detection");
        M3ObjectMapper mapperFeature = M3ObjectMapper.withLimits(ROOM);
        mapperFeature.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        expect(IOException.class, () -> mapperFeature.readTreeValue(
                ingress.readAST("{\"x\":1,\"x\":2}"), Object.class), "mapper parser feature initialization");
    }

    private static void isolatedSubtree() throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        M3AST ast = mapper.readAST("{\"left\":{\"name\":\"kept\",\"count\":4},\"right\":[0]}");
        int left = ast.objectFieldValueAt(ast.rootAtomIndex(), "left", 0);
        M3AST subtree = ast.atom(left).asAST();
        equal(new Item("kept", 4), mapper.readTreeValue(subtree, Item.class),
                "bind an existing root view without reading unrelated siblings");
        int field = ast.childAtomIndexAt(ast.rootAtomIndex(), 0);
        expect(JsonMappingException.class, () -> mapper.readTreeValue(ast.atom(field).asAST(), Object.class),
                "field view cannot pretend to be a complete JSON value");
    }

    private static void deepCursor() throws Exception {
        int depth = 3000;
        JsonFactory factory = JsonFactory.builder().streamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(depth + 1).build()).build();
        M3ObjectMapper ingress = new M3ObjectMapper(factory, ROOM);
        M3AST ast = ingress.readAST("[".repeat(depth) + "0" + "]".repeat(depth));
        M3ObjectMapper mapper = new M3ObjectMapper(factory.copy(), ROOM);
        mapper.registerModule(probeModule(p -> {
            p.skipChildren();
            equal(JsonToken.END_ARRAY, p.currentToken(), "deep skip uses iterative cursor");
            return new Probe("deep");
        }));
        equal(new Probe("deep"), mapper.readTreeValue(ast, Probe.class), "3000-container cursor is iterative");
    }

    private static void constrainedFailure(M3AST ast, StreamReadConstraints constraints, String label)
            throws Exception {
        JsonFactory factory = JsonFactory.builder().streamReadConstraints(constraints).build();
        expect(IOException.class, () -> new M3ObjectMapper(factory, ROOM).readTreeValue(ast, Object.class), label);
    }

    private static M3ObjectMapper probeMapper(ProbeRead read) {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        mapper.registerModule(probeModule(read));
        return mapper;
    }

    private static SimpleModule probeModule(ProbeRead read) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Probe.class, new ProbeDeserializer(read));
        return module;
    }

    private static List<Integer> unsigned(byte[] bytes) {
        List<Integer> values = new ArrayList<>();
        for (byte value : bytes) values.add(value & 255);
        return values;
    }

    private static String observe(Checked action) throws Exception {
        try { return "VALUE:" + action.run(); }
        catch (IOException failure) { return "ERROR:" + failure.getClass().getName(); }
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static <T extends Throwable> T expect(Class<T> type, Checked action, String label) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError(label + ": wrong failure", failure);
        }
        throw new AssertionError(label + ": no failure");
    }

    @FunctionalInterface private interface Checked { Object run() throws Exception; }
    @FunctionalInterface private interface ProbeRead { Probe read(JsonParser parser) throws IOException; }
    public record Item(String name, int count) { }
    public record Named(String firstName) { }
    public record Probe(String value) { }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    public static final class Identity {
        public int id;
        public Identity next;
    }

    private static final class ProbeDeserializer extends StdDeserializer<Probe> {
        @Serial private static final long serialVersionUID = 1L;
        private final transient ProbeRead read;
        ProbeDeserializer(ProbeRead read) { super(Probe.class); this.read = read; }
        @Override public Probe deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return read.read(parser);
        }
    }

    /** Every byte/character grammar-parser and generator construction hook is guarded. */
    private static final class GuardedFactory extends JsonFactory {
        @Serial private static final long serialVersionUID = 1L;
        int parsers;
        int generators;
        boolean blockSource;
        private void parseEntry() {
            parsers++;
            if (blockSource) throw new AssertionError("tree binding constructed a source parser");
        }
        private void writeEntry() {
            generators++;
            if (blockSource) throw new AssertionError("tree binding constructed a generator");
        }
        @Override protected JsonParser _createParser(InputStream input, IOContext context) throws IOException {
            parseEntry(); return super._createParser(input, context);
        }
        @Override protected JsonParser _createParser(Reader input, IOContext context) throws IOException {
            parseEntry(); return super._createParser(input, context);
        }
        @Override protected JsonParser _createParser(char[] input, int offset, int length, IOContext context,
                                                     boolean recyclable) throws IOException {
            parseEntry(); return super._createParser(input, offset, length, context, recyclable);
        }
        @Override protected JsonParser _createParser(byte[] input, int offset, int length, IOContext context)
                throws IOException {
            parseEntry(); return super._createParser(input, offset, length, context);
        }
        @Override protected JsonParser _createParser(DataInput input, IOContext context) throws IOException {
            parseEntry(); return super._createParser(input, context);
        }
        @Override protected JsonGenerator _createGenerator(Writer output, IOContext context) throws IOException {
            writeEntry(); return super._createGenerator(output, context);
        }
        @Override protected JsonGenerator _createUTF8Generator(OutputStream output, IOContext context)
                throws IOException {
            writeEntry(); return super._createUTF8Generator(output, context);
        }
    }
}
