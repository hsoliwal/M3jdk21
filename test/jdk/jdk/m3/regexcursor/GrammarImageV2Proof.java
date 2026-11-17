// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Version-2 image, version-1 readback and packed-production adversarial proof. */
public final class GrammarImageV2Proof {
    private static int assertions;
    private GrammarImageV2Proof() { }

    public static void main(String[] args) throws Exception {
        oldVersionReadback();
        oversizedRegexRoundtrip();
        surrogateLiteralRoundtrip();
        packedPredictionStress();
        badImageRefusals();
        System.out.println("GrammarImageV2Proof PASS checks=" + assertions);
    }

    private static void oldVersionReadback() throws IOException {
        // Golden fixture was emitted with IMAGE_VERSION=1. It must remain readable
        // by the version-2 importer despite a changed deterministic writer format.
        byte[] legacy = Files.readAllBytes(Path.of(
                System.getProperty("m3.regexcursor.examples", "examples"),
                "legacy-format.m3image"));
        ok(legacy.length == 272);
        ok(legacy[7] == 1);
        GrammarSpecification old = GrammarSpecification.fromImage(legacy,
                "3e0c457bbdaae2b940501dcd2ac6ebf83aabd674a48813f7b2ff7b6bdebec21e");
        byte[] upgraded = old.image();
        ok(upgraded[7] == 2);
        GrammarSpecification loaded = GrammarSpecification.fromImage(upgraded,
                GrammarSpecification.imageSha256(upgraded));
        eq(old.grammarSha256(), loaded.grammarSha256());
        ok(Arrays.equals(upgraded, loaded.image()));
        Random random = new Random(8210);
        for (int k = 0; k < 5_000; k++) {
            int[] tokens = new int[random.nextInt(30)];
            for (int i = 0; i < tokens.length; i++) tokens[i] = random.nextInt(8) + 1;
            ok(old.accepts(tokens) == loaded.accepts(tokens));
        }
    }

    private static void oversizedRegexRoundtrip() {
        String expression = "[" + "x".repeat(66_000) + "]+";
        GrammarSpecification grammar = GrammarSpecification.compile("many", "2",
                List.of(), List.of(new GrammarSpecification.RegexRule(43, expression, 0, 0)),
                0, List.of());
        byte[] image = grammar.image();
        ok(image.length > 132_000);
        GrammarSpecification loaded = GrammarSpecification.fromImage(image,
                GrammarSpecification.imageSha256(image));
        eq(grammar.grammarSha256(), loaded.grammarSha256());
        ok(loaded.bind("xxxxx").classify(0, 5) == 43);
        ok(loaded.bind("xxxxx").classify(0, 5) == 43);
        ok(loaded.bind("y").classify(0, 1) == 0);
        ok(Arrays.equals(image, loaded.image()));
    }

    private static void surrogateLiteralRoundtrip() {
        // Images represent Java UTF-16 specifications, not Unicode-scalar text:
        // round-trip unpaired surrogate code units without replacement characters.
        String unit = "prefix\ud800";
        GrammarSpecification grammar = GrammarSpecification.compile("utf16", "1",
                List.of(new StaticGrammarLexicon.Literal(unit, 19, true)), List.of(), 0, List.of());
        byte[] bytes = grammar.image();
        GrammarSpecification decoded = GrammarSpecification.fromImage(bytes,
                GrammarSpecification.imageSha256(bytes));
        ok(decoded.bind(unit).classify(0, unit.length()) == 19);
        ok(decoded.contextual(19));
        ok(!decoded.grammarSha256().isEmpty());
        ok(Arrays.equals(bytes, decoded.image()));
    }

    private static void packedPredictionStress() throws Exception {
        // Reflection is test-only: production runtime storage must not be one
        // primitive-array object per rule. This is a physical-layout invariant.
        for (Field field : GrammarSpecification.class.getDeclaredFields()) {
            ok(field.getType() != int[][].class);
        }
        var rows = EbnfGrammar.normalize(
                "S = ID, { '+', ID };", "S", Map.of("ID", 1, "+", 2));
        GrammarSpecification grammar = GrammarSpecification.compile("expr", "1",
                List.of(new StaticGrammarLexicon.Literal("+", 2, false)),
                List.of(new GrammarSpecification.RegexRule(1, "[A-Za-z]+", 0, 0)),
                rows.startSymbol(), rows.productions());
        ok(grammar.rhsSymbolCount() > 0);
        byte[] image = grammar.image();
        GrammarSpecification roundtrip = GrammarSpecification.fromImage(image,
                GrammarSpecification.imageSha256(image));
        Random random = new Random(9922);
        for (int trial = 0; trial < 10_000; trial++) {
            int[] stream = new int[random.nextInt(100)];
            for (int i = 0; i < stream.length; i++) stream[i] = random.nextInt(2) + 1;
            ok(grammar.accepts(stream) == roundtrip.accepts(stream));
        }
        ok(roundtrip.rhsSymbolCount() == grammar.rhsSymbolCount());
    }

    private static void badImageRefusals() {
        GrammarSpecification grammar = GrammarSpecification.compile("x", "1",
                List.of(new StaticGrammarLexicon.Literal("x", 1, false)), List.of(), 0, List.of());
        byte[] image = grammar.image();
        byte[] changed = image.clone();
        changed[8] = (byte) 0xff;
        reject(() ->
                GrammarSpecification.fromImage(changed, GrammarSpecification.imageSha256(image)));
        byte[] wrongVersion = image.clone();
        wrongVersion[7] = (byte) 123;
        reject(() ->
                GrammarSpecification.fromImage(wrongVersion,
                        GrammarSpecification.imageSha256(image)));
        reject(() -> GrammarSpecification.fromImage(image, "f".repeat(64)));
        reject(() -> GrammarSpecification.fromImage(new byte[17_000_000], "garbage"));
    }

    private static void ok(boolean condition) {
        assertions++;
        if (!condition) throw new AssertionError("failure at check " + assertions);
    }

    private static void eq(String expected, String actual) {
        ok(expected.equals(actual));
    }

    private static void reject(Runnable action) {
        assertions++;
        try { action.run(); throw new AssertionError("expected refusal at " + assertions); }
        catch (IllegalArgumentException expected) { /* pass */ }
    }
}
