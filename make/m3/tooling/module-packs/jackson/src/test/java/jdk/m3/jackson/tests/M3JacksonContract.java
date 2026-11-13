// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson.tests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTAtom;
import jdk.m3.jackson.M3File;
import jdk.m3.jackson.M3JacksonModule;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/** Standalone behavioral contracts, compiled once and run on both host and linked images. */
public final class M3JacksonContract {
    private static final M3JsonLimits ROOM = new M3JsonLimits(100_000, 10_000, 100_000, 100_000, 512);
    private static final StringBuilder RESULTS = new StringBuilder("case\tchecks\n");
    private static int checks;
    private static int cases;

    private M3JacksonContract() { }

    /** All fixture writes belong to the supplied, previously absent output directory. */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) throw new IllegalArgumentException("one fresh output directory required");
        Path output = Path.of(arguments[0]).toAbsolutePath().normalize();
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("contract output already exists: " + output);
        }
        Files.createDirectory(output);
        run("original-objectmapper", M3JacksonContract::originalObjectMapper);
        run("character-and-ast-overloads", M3JacksonContract::characterAndAstOverloads);
        run("direct-ast-token-binding", M3ASTBindingContract::verify);
        run("registered-datatype-and-null", M3JacksonContract::registeredDatatypeAndNull);
        run("enclosing-parser-cursor", M3JacksonContract::enclosingParserCursor);
        run("ordered-duplicate-and-shared-subtrees", M3JacksonContract::orderedSharedSubtrees);
        run("document-identity-and-bounds", M3JacksonContract::documentIdentityAndBounds);
        run("unicode-and-surrogate-escaping", M3JacksonContract::unicodeEscaping);
        run("numeric-lexemes-and-parser-features", M3JacksonContract::numericLexemes);
        run("token-buffer-and-tree-number-binding", M3JacksonContract::tokenBufferBinding);
        run("trailing-and-malformed-input", M3JacksonContract::trailingAndMalformed);
        run("character-snapshot-and-source-goldens", M3JacksonContract::characterSnapshots);
        run("file-bounds-decoding-and-byte-goldens", () -> fileSnapshots(output));
        run("primitive-index-and-streamed-json", () -> primitiveIndexAndStreaming(output));
        run("invalid-limit-configuration", M3JacksonContract::invalidLimits);
        run("input-token-atom-reference-text-depth-limits", M3JacksonContract::inputLimits);
        run("retained-output-limits", M3JacksonContract::retainedOutputLimits);
        run("expanded-shared-dag-output-limits", M3JacksonContract::expandedOutputLimits);
        run("deep-iterative-traversal", M3JacksonContract::deepTraversal);
        run("copy-and-factory-contracts", M3JacksonContract::copyContracts);
        run("module-service-discovery", M3JacksonContract::serviceDiscovery);
        Files.writeString(output.resolve("CONTRACT_RESULTS.tsv"), RESULTS,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("M3_JACKSON_CONTRACT_PASS cases=" + cases + " checks=" + checks);
    }

    private static void primitiveIndexAndStreaming(Path output) throws Exception {
        String json = "{\"dup\":\"one\",\"dup\":\"two\",\"number\":-2.5e+2,"
                + "\"nested\":[null,true,false,\"🚀\"]}";
        Path path = output.resolve("streaming.json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);

        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        M3AST indexed = mapper.readASTStreaming(path, StandardCharsets.UTF_8, bytes.length);
        int root = indexed.rootAtomIndex();
        equal("OBJECT", indexed.kindAt(root), "primitive root kind");
        equal(4, indexed.childCountAt(root), "preserve duplicate ordered fields");
        int firstField = indexed.childAtomIndexAt(root, 0);
        int secondField = indexed.childAtomIndexAt(root, 1);
        equal("FIELD", indexed.kindAt(firstField), "primitive field kind");
        equal("dup", indexed.nameAt(firstField), "first duplicate field name");
        equal("dup", indexed.nameAt(secondField), "second duplicate field name");
        equal("one", indexed.valueAt(indexed.childAtomIndexAt(firstField, 0)),
                "first duplicate field value");
        equal("two", indexed.valueAt(indexed.childAtomIndexAt(secondField, 0)),
                "second duplicate field value");
        equal("one", indexed.valueAt(indexed.objectFieldValueAt(root, "dup", 0)),
                "first duplicate occurrence query");
        equal("two", indexed.valueAt(indexed.objectFieldValueAt(root, "dup", 1)),
                "second duplicate occurrence query");
        equal(-1, indexed.objectFieldValueAt(root, "dup", 2), "missing duplicate occurrence");
        int nested = indexed.objectFieldValueAt(root, "nested", 0);
        equal("ARRAY", indexed.kindAt(nested), "indexed nested array");
        equal("NULL", indexed.kindAt(indexed.arrayElementAt(nested, 0)), "array element index");
        expect(IllegalArgumentException.class, () -> indexed.arrayElementAt(root, 0),
                "array access rejects non-array kind");
        equal(indexed.structuralHash64(), indexed.structuralHash64At(root),
                "indexed hash matches root hash");
        equal(json, mapper.writeValueAsString(indexed), "streamed JSON index round-trip");
        equal(mapper.writeValueAsString(mapper.readAST(json)),
                mapper.writeValueAsString(indexed), "snapshot/streaming parity");
        expect(IndexOutOfBoundsException.class, () -> indexed.kindAt(-1),
                "invalid primitive atom coordinate");
        expect(IndexOutOfBoundsException.class, () -> indexed.childAtomIndexAt(root, 4),
                "invalid primitive child coordinate");
        expect(IOException.class, () -> mapper.readASTStreaming(path,
                StandardCharsets.UTF_8, bytes.length - 1), "stream byte cap");
        expect(IllegalArgumentException.class, () -> mapper.readASTStreaming(path,
                StandardCharsets.UTF_8, -1), "negative streaming byte cap");

        Path malformed = output.resolve("malformed-streaming.json");
        Files.write(malformed, HexFormat.of().parseHex("7b2278223a22c328227d"),
                StandardOpenOption.CREATE_NEW);
        expect(IOException.class, () -> mapper.readASTStreaming(malformed,
                StandardCharsets.UTF_8, 1024), "malformed streaming UTF-8 rejected");
    }

    private static void originalObjectMapper() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Person ada = new Person("Ada", 37);
        equal(ada, mapper.readValue("{\"person_name\":\"Ada\",\"age\":37}", Person.class),
                "record annotation binding");
        equal("Ada", mapper.readTree(mapper.writeValueAsString(ada)).get("person_name").textValue(),
                "record annotation serialization");
        Bean bean = mapper.readValue("{\"wire_name\":\"Grace\",\"hidden\":\"ignored\"}", Bean.class);
        equal("Grace", bean.name, "POJO property annotation");
        equal("private-default", bean.hidden, "ignored field input");
        check(!mapper.readTree(mapper.writeValueAsString(bean)).has("hidden"), "ignored field output");
        List<Person> people = mapper.readValue("[{\"person_name\":\"Ada\",\"age\":37}]",
                new TypeReference<List<Person>>() { });
        equal(List.of(ada), people, "generic element binding");
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        Map<String, Object> values = mapper.readValue("{\"integer\":123456789012345678901234567890,\"decimal\":1.2300}",
                new TypeReference<Map<String, Object>>() { });
        equal(new BigInteger("123456789012345678901234567890"), values.get("integer"), "configured big integer");
        equal(new BigDecimal("1.2300"), values.get("decimal"), "configured exact decimal scale");
        mapper.registerModule(ticketModule());
        equal("\"ticket:7\"", mapper.writeValueAsString(new Ticket(7)), "custom serializer remains active");
        equal(new Ticket(9), mapper.readValue("\"ticket:9\"", Ticket.class), "custom deserializer remains active");
        Shape shape = new Circle(4);
        String polymorphic = mapper.writerFor(Shape.class).writeValueAsString(shape);
        equal(shape, mapper.readValue(polymorphic, Shape.class), "ordinary POJO polymorphism remains unchanged");
    }

    private static void characterAndAstOverloads() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        CountingText source = new CountingText("{\"person_name\":\"Lin\",\"age\":28}");
        equal(new Person("Lin", 28), mapper.readValue(source, Person.class), "CharSequence class binding");
        equal(1, source.snapshots, "mutable source snapshotted once for class binding");
        CountingText collection = new CountingText("[{\"person_name\":\"Lin\",\"age\":28}]");
        TypeReference<List<Person>> reference = new TypeReference<>() { };
        equal(List.of(new Person("Lin", 28)), mapper.readValue(collection, reference), "CharSequence generic binding");
        equal(1, collection.snapshots, "generic source snapshotted once");
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Person.class);
        equal(List.of(new Person("Lin", 28)), mapper.readValue((CharSequence) collection.text, type),
                "CharSequence JavaType binding");
        CountingText tree = new CountingText("{\"a\":1}");
        equal(1, mapper.readTree(tree).get("a").intValue(), "CharSequence tree binding");
        equal(1, tree.snapshots, "tree source snapshotted once");
        M3AST ast = mapper.readAST(collection.text);
        equal(List.of(new Person("Lin", 28)), mapper.readValue(ast, reference), "AST generic binding");
        equal(List.of(new Person("Lin", 28)), mapper.readValue(ast, type), "AST JavaType binding");
        equal(new Person("Lin", 28), mapper.readValue(ast.root().child(0).asAST(), Person.class),
                "AST class binding through explicit JSON projection");
        CountingText astSource = new CountingText("[1,2]");
        equal("[1,2]", mapper.writeValueAsString(mapper.readAST(astSource)), "CharSequence AST binding");
        equal(1, astSource.snapshots, "AST source snapshotted once");
        expect(NullPointerException.class, () -> mapper.readAST((CharSequence) null), "null character source");
    }

    private static void registeredDatatypeAndNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new M3JacksonModule());
        String json = "{\"payload\":{\"x\":[1,2]},\"after\":\"done\",\"marker\":9}";
        Envelope envelope = mapper.readValue(json, Envelope.class);
        equal("OBJECT", envelope.payload().root().kind(), "nested AST value");
        equal("done", envelope.after(), "property after nested AST");
        equal(9, envelope.marker(), "second property after nested AST");
        equal(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(envelope)), "nested AST round trip");
        M3AST nullRoot = mapper.readValue("null", M3AST.class);
        check(nullRoot != null, "JSON null is an AST value");
        equal("NULL", nullRoot.root().kind(), "root null kind");
        equal("null", mapper.writeValueAsString(nullRoot), "root null output");
        Envelope nullEnvelope = mapper.readValue("{\"payload\":null,\"after\":\"kept\",\"marker\":4}", Envelope.class);
        equal("NULL", nullEnvelope.payload().root().kind(), "nested null kind");
        equal("kept", nullEnvelope.after(), "property after nested null");
        Envelope absent = mapper.readValue("{}", Envelope.class);
        equal(null, absent.payload(), "absent record parameter remains a missing Java reference");
        equal(null, mapper.readValue("{}", AstBean.class).payload, "absent mutable field remains a missing Java reference");
        equal("NULL", mapper.readValue("{\"payload\":null}", AstBean.class).payload.root().kind(),
                "explicit mutable-field null remains an AST null value");
        List<M3AST> nulls = mapper.readValue("[null,null]", new TypeReference<>() { });
        check(!nulls.get(0).sameStructure(nulls.get(1)), "separately bound null values retain separate document pools");
        List<M3AST> list = mapper.readValue("[{},null,[],1,\"x\",true,false]", new TypeReference<>() { });
        equal(List.of("OBJECT", "NULL", "ARRAY", "NUMBER_INT", "STRING", "TRUE", "FALSE"),
                list.stream().map(value -> value.root().kind()).toList(), "nested heterogeneous AST sequence");
        equal("[{},null,[],1,\"x\",true,false]", mapper.writeValueAsString(list), "nested null cursor and output");
        ObjectMapper constrained = new ObjectMapper().registerModule(new M3JacksonModule(
                new M3JsonLimits(100, 100, 100, 1, 10)));
        expect(JsonMappingException.class, () -> constrained.readValue("null", M3AST.class),
                "null datatype quota failure is a Jackson mapping exception");
    }

    private static void enclosingParserCursor() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new M3JacksonModule());
        try (JsonParser parser = mapper.getFactory().createParser("[{\"x\":1},null,{\"y\":2},99]")) {
            equal(JsonToken.START_ARRAY, parser.nextToken(), "outer array start");
            equal(JsonToken.START_OBJECT, parser.nextToken(), "first root start");
            equal("{\"x\":1}", mapper.writeValueAsString(mapper.readValue(parser, M3AST.class)), "first root binding");
            equal(JsonToken.VALUE_NULL, parser.nextToken(), "first binding leaves next value unconsumed");
            equal("NULL", mapper.readValue(parser, M3AST.class).root().kind(), "null parser binding");
            equal(JsonToken.START_OBJECT, parser.nextToken(), "null binding leaves next object unconsumed");
            equal("{\"y\":2}", mapper.writeValueAsString(mapper.readValue(parser, M3AST.class)), "second object binding");
            equal(JsonToken.VALUE_NUMBER_INT, parser.nextToken(), "following scalar remains available");
            equal(99, parser.getIntValue(), "following scalar value");
            equal(JsonToken.END_ARRAY, parser.nextToken(), "enclosing end remains available");
            equal(null, parser.nextToken(), "parser exhausted normally");
        }
    }

    private static void orderedSharedSubtrees() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        String source = "{\"b\":[1,2],\"a\":[1,2],\"b\":[1,2],\"a\":[2,1]}";
        M3AST ast = mapper.readAST(source);
        M3ASTAtom root = ast.root();
        equal("OBJECT", root.kind(), "object root kind");
        equal(4, root.childCount(), "duplicate field occurrences retained");
        equal(List.of("b", "a", "b", "a"),
                List.of(root.child(0).name(), root.child(1).name(), root.child(2).name(), root.child(3).name()),
                "exact field order");
        equal(root.childAtomIndex(0), root.childAtomIndex(2), "identical fields share one atom");
        equal(root.child(0).childAtomIndex(0), root.child(1).childAtomIndex(0), "repeated ordered arrays share");
        check(root.child(1).childAtomIndex(0) != root.child(3).childAtomIndex(0), "different array order remains distinct");
        equal(source, mapper.writeValueAsString(ast), "duplicate fields and numeric order survive output");
        equal("[1,2]", mapper.writeValueAsString(root.child(0).child(0).asAST()), "subtree output retains only its root");
        expect(JsonMappingException.class, () -> mapper.writeValueAsString(root.child(0).asAST()),
                "FIELD cannot be serialized as a complete JSON root");
        M3ObjectMapper strict = new M3ObjectMapper(JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
        expect(IOException.class, () -> strict.readAST(source), "configured duplicate detection remains effective");
    }

    private static void documentIdentityAndBounds() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        M3AST first = mapper.readAST("[[1,2],[1,2]]");
        M3AST left = first.root().child(0).asAST();
        M3AST right = first.root().child(1).asAST();
        check(left.sameStructure(right), "shared roots have exact document identity");
        equal(left, right, "shared root equality");
        equal(left.hashCode(), right.hashCode(), "equal roots have equal hash codes");
        equal(first.root().child(0), first.root().child(1), "atom handle equality uses document coordinates");
        M3AST independent = mapper.readAST("[[1,2],[1,2]]");
        check(!first.sameStructure(independent), "independent documents do not claim canonical identity");
        check(!first.equals(independent), "equal bytes do not merge document owners");
        equal(first.structuralHash64(), independent.structuralHash64(), "structural candidate hash is deterministic");
        check(!first.sameStructure(null), "null document has no identity");
        equal(first.atomCount(), left.atomCount(), "subtree retains its complete document pool");
        expect(IndexOutOfBoundsException.class, () -> first.atom(-1), "negative atom index");
        expect(IndexOutOfBoundsException.class, () -> first.atom(first.atomCount()), "atom upper boundary");
        expect(IndexOutOfBoundsException.class, () -> first.root().child(-1), "negative child index");
        expect(IndexOutOfBoundsException.class, () -> first.root().child(2), "child upper boundary");
        M3AST collisionCandidates = mapper.readAST("[\"Aa\",\"BB\"]");
        equal("Aa".hashCode(), "BB".hashCode(), "fixture has an ordinary String hash collision");
        check(collisionCandidates.root().childAtomIndex(0) != collisionCandidates.root().childAtomIndex(1),
                "String hash collision cannot merge distinct spelling");
    }

    private static void unicodeEscaping() throws Exception {
        String name = "field\"\\\n";
        String value = "quote\" slash\\ \b\f\n\r\t \u0000 \u0939\u093f\u0928\u094d\u0926\u0940 \uD83D\uDE00 lone:\uD800 end";
        ObjectMapper oracle = new ObjectMapper(JsonFactory.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build());
        String encoded = oracle.writeValueAsString(Map.of(name, value));
        M3ObjectMapper mapper = new M3ObjectMapper(JsonFactory.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build());
        M3AST ast = mapper.readAST(encoded);
        equal(name, ast.root().child(0).name(), "decoded field spelling");
        equal(value, ast.root().child(0).child(0).value(), "decoded UTF-16 including lone surrogate");
        String emitted = mapper.writeValueAsString(ast);
        check(emitted.chars().allMatch(unit -> unit < 128), "configured generator produces ASCII escapes");
        check(emitted.toLowerCase(Locale.ROOT).contains("\\ud800"), "lone surrogate emitted as an escape");
        equal(Map.of(name, value), oracle.readValue(emitted, new TypeReference<Map<String, String>>() { }),
                "property and value escaping round trip");
        String injection = "\"},\"injected\":true,\"tail\":\"";
        M3AST quoted = mapper.readAST(oracle.writeValueAsString(injection));
        equal(injection, oracle.readValue(mapper.writeValueAsString(quoted), String.class),
                "String content never becomes raw JSON syntax");
    }

    private static void numericLexemes() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        String source = "[-0,1.2300,1e+02,-2E-3,123456789012345678901234567890]";
        M3AST ast = mapper.readAST(source);
        equal(source, mapper.writeValueAsString(ast), "numeric lexemes remain exact");
        equal("NUMBER_INT", ast.root().child(0).kind(), "integer token kind");
        equal("NUMBER_FLOAT", ast.root().child(1).kind(), "decimal token kind");
        M3ObjectMapper comments = new M3ObjectMapper(JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build());
        equal("{\"a\":1}", comments.writeValueAsString(comments.readAST("/* input */ {\"a\":1}")),
                "configured Jackson parser owns comment handling");
        JsonFactory extensionFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS).build();
        M3ObjectMapper extensions = new M3ObjectMapper(extensionFactory);
        check(extensions.readValue("NaN", Double.class).isNaN(), "inherited non-finite numeric support remains available");
        equal(0.5d, extensions.readValue(".5", Double.class), "inherited leading decimal extension");
        equal(1.0d, extensions.readValue("1.", Double.class), "inherited trailing decimal extension");
        expect(IOException.class, () -> extensions.readAST("NaN"), "finite AST rejects NaN");
        expect(IOException.class, () -> extensions.readAST("Infinity"), "finite AST rejects infinity");
        expect(IOException.class, () -> extensions.readAST(".5"), "AST refuses nonstandard numeric lexeme");
        expect(IOException.class, () -> extensions.readAST("1."), "AST refuses trailing decimal point");
    }

    private static void trailingAndMalformed() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        equal(1, mapper.readValue("1 2", Integer.class), "inherited default trailing-token behavior");
        equal(1, mapper.readValue((CharSequence) new StringBuilder("1 2"), Integer.class),
                "character helper preserves configured trailing-token behavior");
        equal("1", mapper.writeValueAsString(mapper.readAST(" \r\n1\t ")), "one root with whitespace");
        for (String source : List.of("", " ", "1 2", "{}[]", "{", "[1,]", "{\"a\":}", "\"bad\nstring\"")) {
            expect(IOException.class, () -> mapper.readAST(source), "complete-root refusal: " + source.replace('\n', ' '));
        }
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        expect(IOException.class, () -> mapper.readValue((CharSequence) "1 2", Integer.class),
                "configured trailing-token rejection on character helper");
    }

    private static void tokenBufferBinding() throws Exception {
        M3ObjectMapper mapper = new M3ObjectMapper();
        BigInteger integer = new BigInteger("1234567890123456789012345678901234567890");
        M3AST integerAst = mapper.readAST(integer.toString());
        check(mapper.valueToTree(integerAst).isIntegralNumber(), "valueToTree retains integral token kind");
        equal(integer, mapper.valueToTree(integerAst).bigIntegerValue(), "valueToTree retains complete large integer");
        equal(integer, mapper.convertValue(integerAst, BigInteger.class), "convertValue retains complete large integer");
        Numeric numeric = new Numeric(integer, new BigDecimal("1.2500"));
        M3AST converted = mapper.convertValue(numeric, M3AST.class);
        equal(numeric, mapper.convertValue(converted, Numeric.class), "TokenBuffer numeric record round trip");
        M3AST decimalOne = mapper.convertValue(BigDecimal.ONE, M3AST.class);
        equal("NUMBER_FLOAT", decimalOne.root().kind(), "TokenBuffer BigDecimal keeps its original floating token kind");
        equal("1", decimalOne.root().value(), "integer-shaped BigDecimal lexeme remains valid JSON");
        equal(BigDecimal.ONE, mapper.convertValue(decimalOne, BigDecimal.class), "integer-shaped decimal binds back exactly");
        M3AST fromTree = mapper.treeToValue(mapper.readTree("[" + integer + "]"), M3AST.class);
        equal("[" + integer + "]", mapper.writeValueAsString(fromTree), "tree parser admission retains large integer");
    }

    private static void characterSnapshots() throws Exception {
        URI uri = URI.create("memory:/m3-jackson/source.json");
        String immutable = "{\"a\":1}";
        M3File sameOwner = M3File.of(uri, immutable);
        check(sameOwner.source() == immutable, "immutable String owner is reused");
        equal(uri, sameOwner.sourceUri(), "caller URI remains exact");
        equal(null, sameOwner.byteSha256(), "character input has no invented byte digest");
        StringBuilder mutable = new StringBuilder(immutable);
        M3File snapshot = M3File.of(uri, mutable);
        mutable.setLength(0);
        mutable.append("changed");
        equal(immutable, snapshot.source(), "mutable character input detached once");
        equal(immutable.length(), snapshot.length(), "snapshot length");
        equal(immutable.charAt(1), snapshot.charAt(1), "snapshot code unit");
        equal(immutable.substring(1, 4), snapshot.subSequence(1, 4).toString(), "snapshot substring");
        CountingText counting = new CountingText(immutable);
        M3File.of(uri, counting);
        equal(1, counting.snapshots, "M3File calls mutable snapshot once");
        try (Reader first = snapshot.openReader(); Reader second = snapshot.openReader()) {
            equal((int) '{', first.read(), "first independent reader");
            equal((int) '"', first.read(), "first reader advances");
            equal((int) '{', second.read(), "second reader retains independent position");
        }
        // Independently computed SHA-256 over the documented domain, length and explicit UTF-16BE bytes.
        M3File unpaired = M3File.of(uri, "A\r\n\u00e9\uD800");
        equal("de68058dbb722f2f710868feaa18b787edc68525356b69997f4943826e337aa2", unpaired.sourceSha256(),
                "golden UTF-16 identity retains CRLF and unpaired surrogate");
        expect(NullPointerException.class, () -> M3File.of(null, immutable), "null source URI");
        expect(NullPointerException.class, () -> M3File.of(uri, null), "null snapshot input");
        expect(IndexOutOfBoundsException.class, () -> snapshot.charAt(snapshot.length()), "snapshot char upper boundary");
    }

    private static void fileSnapshots(Path output) throws Exception {
        byte[] bytes = HexFormat.of().parseHex("410d0ac3a9f09f9880");
        Path path = output.resolve("source-utf8.txt");
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
        M3File file = M3File.readBytes(path, StandardCharsets.UTF_8, bytes.length);
        equal("A\r\n\u00e9\uD83D\uDE00", file.source(), "strict UTF-8 decoding at exact byte budget");
        equal(path.toUri(), file.sourceUri(), "file snapshot URI");
        equal("0d41069c39e02f18d196a6bb22c889f6d90ad20fa19313dffb00d1249d859802", file.byteSha256(),
                "independent original-byte SHA-256 golden");
        equal("b91877a571e32f5e56a6c11c31a3e362af5fa67238053b3d1a8764c0458a0117", file.sourceSha256(),
                "independent decoded-source SHA-256 golden");
        check(!file.byteSha256().equals(file.sourceSha256()), "byte and source hash domains remain distinct");
        expect(IOException.class, () -> M3File.readBytes(path, StandardCharsets.UTF_8, bytes.length - 1), "one byte over budget");
        expect(IOException.class, () -> M3File.readBytes(path, StandardCharsets.UTF_8, 0), "nonempty file with zero budget");
        expect(IllegalArgumentException.class, () -> M3File.readBytes(path, StandardCharsets.UTF_8, -1), "negative byte budget");
        Files.writeString(path, "changed", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        equal("A\r\n\u00e9\uD83D\uDE00", file.source(), "file mutation cannot alter retained source");
        Path empty = output.resolve("empty-source.txt");
        Files.write(empty, new byte[0], StandardOpenOption.CREATE_NEW);
        M3File emptyFile = M3File.readBytes(empty, StandardCharsets.UTF_8, 0);
        equal("", emptyFile.source(), "zero budget admits empty input");
        equal("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", emptyFile.byteSha256(),
                "empty raw SHA-256 golden");
        equal("682d087c89bab38303a5e9be7b30808459818b069fca4a1b4cbc061e92aee961", emptyFile.sourceSha256(),
                "empty source-domain SHA-256 golden");
        Path malformed = output.resolve("malformed-utf8.bin");
        Files.write(malformed, new byte[] {(byte) 0xc3, 0x28}, StandardOpenOption.CREATE_NEW);
        expect(CharacterCodingException.class, () -> M3File.readBytes(malformed, StandardCharsets.UTF_8, 2),
                "malformed UTF-8 is rejected rather than replaced");
        Path unmappable = output.resolve("non-ascii.bin");
        Files.write(unmappable, new byte[] {(byte) 0x80}, StandardOpenOption.CREATE_NEW);
        expect(CharacterCodingException.class, () -> M3File.readBytes(unmappable, StandardCharsets.US_ASCII, 1),
                "decoder charset errors are preserved");
        Path json = output.resolve("file-input.json");
        Files.writeString(json, "{\"a\":1}", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        M3ObjectMapper mapper = new M3ObjectMapper();
        equal("{\"a\":1}", mapper.writeValueAsString(mapper.readAST(json, StandardCharsets.UTF_8, 7)),
                "Path AST helper uses bounded source snapshot");
        expect(IOException.class, () -> mapper.readAST(json, StandardCharsets.UTF_8, 6), "Path AST helper byte refusal");
        expect(IOException.class, () -> M3File.readBytes(output.resolve("missing.txt"), StandardCharsets.UTF_8, 10),
                "missing source file remains an IO failure");
    }

    private static void invalidLimits() throws Exception {
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(0, 1, 1, 1, 1), "zero token limit");
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(1, 0, 1, 1, 1), "zero atom limit");
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(1, 1, 0, 1, 1), "zero reference limit");
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(1, 1, Integer.MAX_VALUE, 1, 1), "unrepresentable reference limit");
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(1, 1, 1, 0, 1), "zero text limit");
        expect(IllegalArgumentException.class, () -> new M3JsonLimits(1, 1, 1, 1, 0), "zero depth limit");
    }

    private static void inputLimits() throws Exception {
        roundTrip("[0]", new M3JsonLimits(3, 100, 100, 100, 10), "exact input token budget");
        refuseInput("[0]", new M3JsonLimits(2, 100, 100, 100, 10), "input token quota");
        roundTrip("[0,1]", new M3JsonLimits(100, 3, 100, 100, 10), "exact unique atom budget");
        refuseInput("[0,1]", new M3JsonLimits(100, 2, 100, 100, 10), "unique atom quota");
        roundTrip("[0,0]", new M3JsonLimits(100, 2, 100, 100, 10), "exact duplicate leaves share atom allowance");
        roundTrip("{\"x\":0}", new M3JsonLimits(100, 100, 2, 100, 10), "exact retained reference budget");
        refuseInput("{\"x\":0}", new M3JsonLimits(100, 100, 1, 100, 10), "global retained reference quota");
        refuseInput("[0,0]", new M3JsonLimits(100, 100, 1, 100, 10), "container child reference quota");
        roundTrip("[[0],[0]]", new M3JsonLimits(100, 100, 3, 100, 10), "shared subtree reference accounting");
        refuseInput("[[0],[0]]", new M3JsonLimits(100, 100, 2, 100, 10), "shared subtree arena remains bounded");
        String text = "\"" + "x".repeat(200) + "\"";
        roundTrip(text, new M3JsonLimits(100, 100, 100, 256, 10), "bounded text succeeds");
        refuseInput(text, new M3JsonLimits(100, 100, 100, 199, 10), "input text work quota");
        roundTrip("[[0]]", new M3JsonLimits(100, 100, 100, 100, 2), "exact container depth budget");
        refuseInput("[[0]]", new M3JsonLimits(100, 100, 100, 100, 1), "input container depth quota");
    }

    private static void retainedOutputLimits() throws Exception {
        M3ObjectMapper source = M3ObjectMapper.withLimits(ROOM);
        M3AST ast = source.readAST("{\"left\":[1,2],\"right\":[2,3]}");
        check(ast.atomCount() > 1 && ast.childReferenceCount() > 1, "retained-limit fixture is nontrivial");
        refuseOutput(ast, new M3JsonLimits(1000, ast.atomCount() - 1, 1000, 1000, 10), "retained output atom quota");
        refuseOutput(ast, new M3JsonLimits(1000, 1000, ast.childReferenceCount() - 1L, 1000, 10),
                "retained output reference quota");
        M3AST longText = source.readAST("\"" + "x".repeat(200) + "\"");
        refuseOutput(longText, new M3JsonLimits(1000, 1000, 1000, 100, 10), "retained output text quota");
        M3JsonLimits exact = new M3JsonLimits(1000, ast.atomCount(), ast.childReferenceCount(), 1000, 10);
        equal(source.writeValueAsString(ast), writer(exact).writeValueAsString(ast), "exact retained output bounds admit document");
    }

    private static void expandedOutputLimits() throws Exception {
        M3ObjectMapper source = M3ObjectMapper.withLimits(ROOM);
        String repeated = "[[0,0],[0,0],[0,0],[0,0]]";
        M3AST ast = source.readAST(repeated);
        for (int index = 1; index < 4; index++) {
            equal(ast.root().childAtomIndex(0), ast.root().childAtomIndex(index), "repeated output subtree identity");
        }
        equal(repeated, writer(new M3JsonLimits(18, 1000, 1000, 1000, 10)).writeValueAsString(ast),
                "expanded token quota exact boundary");
        refuseOutput(ast, new M3JsonLimits(17, 1000, 1000, 1000, 10), "shared DAG cannot bypass output token quota");
        String word = "abcdefghijklmnop";
        String manyWords = "[" + String.join(",", Collections.nCopies(12, "\"" + word + "\"")) + "]";
        M3AST sharedText = source.readAST(manyWords);
        check(sharedText.atomCount() < 12, "text occurrences share retained atoms");
        refuseOutput(sharedText, new M3JsonLimits(1000, 1000, 1000, 100, 10), "shared text cannot bypass expanded text quota");
        equal(manyWords, writer(new M3JsonLimits(1000, 1000, 1000, 192, 10)).writeValueAsString(sharedText),
                "expanded text quota exact boundary");
        M3AST nested = source.readAST("[[0]]");
        refuseOutput(nested, new M3JsonLimits(1000, 1000, 1000, 1000, 1), "output depth quota independent of original reader");
    }

    private static void deepTraversal() throws Exception {
        String source = "[".repeat(300) + "0" + "]".repeat(300);
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(ROOM);
        M3AST ast = mapper.readAST(source);
        equal(source, mapper.writeValueAsString(ast), "300 nested containers round trip iteratively");
        M3ASTAtom cursor = ast.root();
        for (int depth = 0; depth < 300; depth++) {
            equal("ARRAY", cursor.kind(), "deep container kind");
            equal(1, cursor.childCount(), "deep container arity");
            cursor = cursor.child(0);
        }
        equal("0", cursor.value(), "deep scalar leaf");
        refuseInput(source, new M3JsonLimits(1000, 1000, 1000, 1000, 256), "deep input refused at configured boundary");
        refuseOutput(ast, new M3JsonLimits(1000, 1000, 1000, 1000, 256), "deep output refused at configured boundary");
    }

    private static void copyContracts() throws Exception {
        M3JsonLimits limits = new M3JsonLimits(1000, 1000, 1000, 1000, 2);
        M3ObjectMapper original = new M3ObjectMapper(JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS).build(), limits);
        original.registerModule(ticketModule());
        original.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        original.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        M3ObjectMapper copy = original.copy();
        copy.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        check(copy != original, "copy has independent mapper identity");
        check(copy.getFactory() != original.getFactory(), "copy has independent factory");
        check(copy.getFactory().getCodec() == copy, "copied factory binds its copied mapper");
        equal(limits, copy.m3Limits(), "copy retains M3 limits");
        equal(original.getRegisteredModuleIds(), copy.getRegisteredModuleIds(), "copy retains registered module identities");
        equal(new Camel("Ada"), copy.readValue("{\"first_name\":\"Ada\",\"extra\":1}", Camel.class),
                "copy keeps naming strategy and independent unknown-property configuration");
        expect(IOException.class, () -> original.readValue("{\"first_name\":\"Ada\",\"extra\":1}", Camel.class),
                "copy configuration does not mutate original");
        equal("\"ticket:5\"", copy.writeValueAsString(new Ticket(5)), "copy retains custom serializer");
        equal(new Ticket(6), copy.readValue("\"ticket:6\"", Ticket.class), "copy retains custom deserializer");
        equal(new BigDecimal("1.250"), copy.readValue("1.250", Object.class), "copy retains numeric configuration");
        equal("{\"v\":1}", copy.writeValueAsString(copy.readAST("/* copied */ {\"v\":1}")),
                "copy retains parser feature and M3 binding");
        expect(IOException.class, () -> copy.readAST("[[[0]]]"), "copy retains depth refusal");
        M3ObjectMapper nullFactoryCopy = original.copyWith(null);
        check(nullFactoryCopy.getFactory() != original.getFactory(), "copyWith(null) copies original factory");
        equal("1", nullFactoryCopy.writeValueAsString(nullFactoryCopy.readAST("/* retained */ 1")),
                "copyWith(null) retains parser features");
        JsonFactory replacement = new JsonFactory();
        M3ObjectMapper replacementCopy = original.copyWith(replacement);
        check(replacementCopy.getFactory() == replacement, "copyWith uses supplied factory");
        check(replacement.getCodec() == replacementCopy, "supplied factory binds copied mapper");
        equal(limits, replacementCopy.m3Limits(), "copyWith retains M3 limits");
        equal(new Ticket(8), replacementCopy.readValue("\"ticket:8\"", Ticket.class), "copyWith retains custom module");
        equal("[1]", replacementCopy.writeValueAsString(replacementCopy.readAST("[1]")), "copyWith retains M3 module");
        expect(IOException.class, () -> replacementCopy.readAST("/* not enabled */ 1"),
                "copyWith honors replacement parser feature set");
        for (M3ObjectMapper defaults : List.of(new M3ObjectMapper(), M3ObjectMapper.withLimits(M3JsonLimits.DEFAULT))) {
            M3ObjectMapper defaultCopy = defaults.copy();
            equal("JSON", defaultCopy.getFactory().getFormatName(), "default factory copy remains JSON");
            try (JsonParser parser = defaultCopy.getFactory().createParser("[1]")) {
                equal("[1]", defaultCopy.writeValueAsString(parser.readValueAs(M3AST.class)),
                        "default copied factory codec supports M3 binding");
            }
        }
        DerivedMapper derived = new DerivedMapper();
        equal("[1]", derived.writeValueAsString(derived.readValue("[1]", M3AST.class)),
                "constructor binding works without entering subclass registration hooks");
        equal(new ObjectMapper().registerModule(new M3JacksonModule()).getRegisteredModuleIds(),
                derived.getRegisteredModuleIds(), "constructor retains Jackson module identities");
        derived.registerModule(ticketModule());
        equal("\"ticket:9\"", derived.writeValueAsString(new Ticket(9)),
                "ordinary module registration remains extensible after construction");
        original.registerModule(new M3JacksonModule(M3JsonLimits.DEFAULT));
        expect(IOException.class, () -> original.readValue("[[[0]]]", M3AST.class),
                "duplicate registration keeps the original bounded binding");
        ObjectMapper codec = new ObjectMapper();
        JsonFactory supplied = new JsonFactory(codec);
        M3ObjectMapper suppliedMapper = new M3ObjectMapper(supplied);
        check(suppliedMapper.getFactory() == supplied, "constructor retains supplied factory identity");
        check(supplied.getCodec() == codec, "constructor preserves an existing supplied-factory codec");
        expect(IllegalStateException.class, derived::copy, "subclass must explicitly implement its copy contract");
        expect(IllegalStateException.class, () -> derived.copyWith(null), "subclass copyWith guard remains effective");
    }

    private static void serviceDiscovery() throws Exception {
        List<com.fasterxml.jackson.databind.Module> modules = ObjectMapper.findModules();
        check(modules.stream().anyMatch(M3JacksonModule.class::isInstance), "Jackson discovers advertised module service");
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        M3AST ast = mapper.readValue("{\"discovered\":true}", M3AST.class);
        equal("{\"discovered\":true}", mapper.writeValueAsString(ast), "discovered module performs actual datatype binding");
        equal("NULL", mapper.readValue("null", M3AST.class).root().kind(), "discovered null provider is active");
    }

    private static ObjectMapper writer(M3JsonLimits limits) {
        return new ObjectMapper().registerModule(new M3JacksonModule(limits));
    }

    private static void roundTrip(String source, M3JsonLimits limits, String message) throws Exception {
        M3ObjectMapper mapper = M3ObjectMapper.withLimits(limits);
        equal(source, mapper.writeValueAsString(mapper.readAST(source)), message);
    }

    private static void refuseInput(String source, M3JsonLimits limits, String message) throws Exception {
        expect(JsonMappingException.class, () -> M3ObjectMapper.withLimits(limits).readAST(source), message);
    }

    private static void refuseOutput(M3AST ast, M3JsonLimits limits, String message) throws Exception {
        expect(JsonMappingException.class, () -> writer(limits).writeValueAsString(ast), message);
    }

    private static SimpleModule ticketModule() {
        SimpleModule module = new SimpleModule("ContractTicket");
        module.addSerializer(Ticket.class, new TicketSerializer());
        module.addDeserializer(Ticket.class, new TicketDeserializer());
        return module;
    }

    private static void run(String name, Checked operation) throws Exception {
        int before = checks;
        operation.run();
        cases++;
        RESULTS.append(name).append('\t').append(checks - before).append('\n');
        System.out.println("PASS " + name + " checks=" + (checks - before));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + "; expected=" + expected + ", actual=" + actual);
    }

    private static <T extends Exception> T expect(Class<T> type, Checked operation, String message) throws Exception {
        checks++;
        try {
            operation.run();
        } catch (Exception failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError(message + "; expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError(message + "; expected " + type.getName());
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    public record Person(@JsonProperty("person_name") String name, int age) { }
    public record Envelope(M3AST payload, String after, int marker) { }
    public record Ticket(int number) { }
    public record Camel(String firstName) { }
    public record Numeric(BigInteger integer, BigDecimal decimal) { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes(@JsonSubTypes.Type(value = Circle.class, name = "circle"))
    public interface Shape { }
    public record Circle(int radius) implements Shape { }

    public static final class Bean {
        @JsonProperty("wire_name") public String name;
        @JsonIgnore public String hidden = "private-default";
        public Bean() { }
    }

    public static final class AstBean {
        public M3AST payload;
        public AstBean() { }
    }

    private static final class CountingText implements CharSequence {
        final String text;
        int snapshots;
        CountingText(String text) { this.text = text; }
        @Override public int length() { return text.length(); }
        @Override public char charAt(int index) { return text.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { return text.subSequence(start, end); }
        @Override public String toString() { snapshots++; return text; }
    }

    private static final class TicketSerializer extends StdSerializer<Ticket> {
        @Serial private static final long serialVersionUID = 1L;
        TicketSerializer() { super(Ticket.class); }
        @Override public void serialize(Ticket value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString("ticket:" + value.number());
        }
    }

    private static final class TicketDeserializer extends StdDeserializer<Ticket> {
        @Serial private static final long serialVersionUID = 1L;
        TicketDeserializer() { super(Ticket.class); }
        @Override public Ticket deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getText();
            if (parser.currentToken() != JsonToken.VALUE_STRING || !text.startsWith("ticket:")) {
                throw JsonMappingException.from(parser, "ticket string required");
            }
            try {
                return new Ticket(Integer.parseInt(text.substring(7)));
            } catch (NumberFormatException failure) {
                throw JsonMappingException.from(parser, "ticket integer required", failure);
            }
        }
    }

    private static final class DerivedMapper extends M3ObjectMapper {
        @Serial private static final long serialVersionUID = 1L;
        private boolean ready = true;

        @Override public ObjectMapper registerModule(com.fasterxml.jackson.databind.Module module) {
            if (!ready) throw new IllegalStateException("registerModule during subclass construction");
            return super.registerModule(module);
        }

        @Override public boolean isEnabled(MapperFeature feature) {
            if (!ready) throw new IllegalStateException("isEnabled during subclass construction");
            return super.isEnabled(feature);
        }
    }
}
