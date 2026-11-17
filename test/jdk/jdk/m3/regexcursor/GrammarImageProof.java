// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Deterministic binary image roundtrip, corruption, and parse-equivalence proof. */
public final class GrammarImageProof {
    private static int checks;
    private GrammarImageProof() { }

    public static void main(String[] args) {
        List<StaticGrammarLexicon.Literal> fixed = List.of(
                new StaticGrammarLexicon.Literal("+", 3, false),
                new StaticGrammarLexicon.Literal("(", 4, false),
                new StaticGrammarLexicon.Literal(")", 5, false));
        List<GrammarSpecification.RegexRule> regex = List.of(
                new GrammarSpecification.RegexRule(1, "[A-Za-z]+", 0, 0),
                new GrammarSpecification.RegexRule(2, "[0-9]+", 0, 1));
        var spec =
                EbnfGrammar.normalize("E = Atom, { '+', Atom }; Atom = WORD | NUMBER | '(', E, "
                        + "')';",
                "E", Map.of("WORD", 1, "NUMBER", 2, "+", 3, "(", 4, ")", 5));
        GrammarSpecification compiled = GrammarSpecification.compile("java-demo", "21",
                fixed, regex, spec.startSymbol(), spec.productions());
        byte[] image = compiled.image();
        eq(true, Arrays.equals(image, compiled.image()));
        GrammarSpecification loaded = GrammarSpecification.fromImage(image,
                GrammarSpecification.imageSha256(image));
        eq(compiled.grammarSha256(), loaded.grammarSha256());
        eq(true, Arrays.equals(image, loaded.image()));
        eq(true, loaded.accepts(new int[]{1,3,2,3,4,1,5}));
        eq(false, loaded.accepts(new int[]{1,3}));
        eq(1, loaded.bind("abc").classify(0,3));
        eq(2, loaded.bind("123").classify(0,3));
        eq(3, loaded.bind("+").classify(0,1));
        Random random = new Random(20261117L);
        for(int i=0;i<20_000;i++) {
            int[] tokens = new int[random.nextInt(45)];
            for(int k=0;k<tokens.length;k++) tokens[k]=random.nextInt(5)+1;
            eq(compiled.accepts(tokens), loaded.accepts(tokens));
        }
        byte[] changed = image.clone();
        changed[changed.length/2] ^= 0x02;
        refuse(() ->
                GrammarSpecification.fromImage(changed, GrammarSpecification.imageSha256(image)));
        refuse(() -> GrammarSpecification.fromImage(image, "bad"));
        refuse(() -> GrammarSpecification.fromImage(new byte[0], "bad"));
        refuseNull(() -> GrammarSpecification.fromImage(image, null));
        var lexical = GrammarSpecification.compile("x", "1", fixed, regex, 0, List.of());
        byte[] lexicalImage = lexical.image();
        var copied = GrammarSpecification.fromImage(lexicalImage,
                GrammarSpecification.imageSha256(lexicalImage));
        eq(false, copied.hasSyntax());
        eq(1, copied.bind("foo").classify(0,3));
        eq(true, Arrays.equals(lexicalImage, copied.image()));
        System.out.println("GrammarImageProof PASS checks="+checks+" bytes="+image.length);
    }
    private static void eq(boolean expected, boolean actual){checks++;if(expected!=actual)
        throw new AssertionError("boolean #"+checks);}
    private static void eq(int expected, int actual){checks++;if(expected!=actual)
        throw new AssertionError("int #"+checks+" expected="+expected+" actual="+actual);}
    private static void eq(String expected, String actual){checks++;if(!expected.equals(actual))
        throw new AssertionError("text #"+checks);}
    private static void refuseNull(Runnable task){checks++;try{task.run();
        throw new AssertionError("expected null refusal #"+checks);
            }catch(NullPointerException expected){}}
    private static void refuse(Runnable task){checks++;try{task.run();
        throw new AssertionError("expected refusal #"+checks);
            }catch(IllegalArgumentException expected){}}
}
