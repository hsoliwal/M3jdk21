// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Arrays;

/** Deterministic closed-form and differential LL(1) BNF/EBNF grammar proof. */
public final class GrammarEbnfProof {
    private static int checks;
    private GrammarEbnfProof() { }

    public static void main(String[] args) {
        List<StaticGrammarLexicon.Literal> fixed = List.of(
                new StaticGrammarLexicon.Literal("+", 3, false),
                new StaticGrammarLexicon.Literal("(", 4, false),
                new StaticGrammarLexicon.Literal(")", 5, false));
        List<GrammarSpecification.RegexRule> regex = List.of(
                new GrammarSpecification.RegexRule(1, "[A-Za-z]+", 0, 0),
                new GrammarSpecification.RegexRule(2, "[0-9]+", 0, 0));
        String ebnf = "expr = atom, { \"+\", atom }; atom = IDENT | NUMBER | \"(\", expr, \")\";";
        GrammarSpecification grammar = compileEbnf("java", "21", fixed, regex,
                Map.of("IDENT",1,"NUMBER",2), ebnf, "expr");
        yes(grammar.accepts(new int[]{1}));
        yes(grammar.accepts(new int[]{2,3,1,3,2}));
        yes(grammar.accepts(new int[]{4,2,3,1,5,3,1}));
        no(grammar.accepts(new int[]{1,3}));
        no(grammar.accepts(new int[]{3,2}));
        no(grammar.accepts(new int[]{4,1}));
        no(grammar.accepts(new int[]{}));
        GrammarSpecification reordered=compileEbnf("java", "21", fixed, regex,
                Map.of("IDENT",1,"NUMBER",2),
                "atom = IDENT | NUMBER | \"(\", expr, \")\"; expr = atom, {\"+\", atom};", "expr");
        eq(grammar.grammarSha256(), reordered.grammarSha256());
        GrammarSpecification bnf=compileEbnf("x","1",fixed,regex,Map.of("IDENT",1,"NUMBER",2),
                "S ::= IDENT, T ; T ::= \"+\", NUMBER | ;", "S");
        yes(bnf.accepts(new int[]{1}));
        yes(bnf.accepts(new int[]{1,3,2}));
        no(bnf.accepts(new int[]{1,3,1}));
        GrammarSpecification plus=compileEbnf("x","1",fixed,regex,Map.of("IDENT",1,"NUMBER",2),
                "S = IDENT, (\"+\", IDENT)+ ;", "S");
        yes(plus.accepts(new int[]{1,3,1}));
        yes(plus.accepts(new int[]{1,3,1,3,1}));
        no(plus.accepts(new int[]{1}));
        GrammarSpecification optional=compileEbnf("x","1",fixed,regex,Map.of("IDENT",1,"NUMBER",2),
                "S = IDENT, [\"+\",NUMBER] ;", "S");
        yes(optional.accepts(new int[]{1}));
        yes(optional.accepts(new int[]{1,3,2}));
        no(optional.accepts(new int[]{1,3}));
        Random random=new Random(0x11ab_C001L);
        for(int iteration=0; iteration<20_000;iteration++) {
            int count=random.nextInt(30)+1;
            int[] valid = new int[count*2-1];
            for(int i=0;i<count;i++) {valid[2*i]=1+random.nextInt(2);if(i!=count-1)valid[2*i+1]=3;}
            yes(grammar.accepts(valid));
            if(valid.length>1) {
                int[] invalid=valid.clone();
                invalid[random.nextInt(count)*2]=5;
                no(grammar.accepts(invalid));
            }
        }
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S = UNKNOWN;","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of(),"S = \"missing\";","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of(),"S = S;","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of(),"S = IDENT; S = NUMBER;","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S = IDENT|IDENT;","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S = IDENT,;","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S = { [IDENT] };","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S ::= IDENT","S"));
        refuse(() -> compileEbnf("x","1",fixed,regex,Map.of("IDENT",1),"S = \"unterminated;","S"));
        System.out.println("GrammarEbnfProof PASS checks="+checks);
    }
    private static GrammarSpecification compileEbnf(String language, String revision,
            List<StaticGrammarLexicon.Literal> fixed,
            List<GrammarSpecification.RegexRule> regex, Map<String,Integer> names,
            String text, String start) {
        java.util.HashMap<String,Integer> symbols = new java.util.HashMap<>(names);
        for (StaticGrammarLexicon.Literal literal : fixed) {
            symbols.put(literal.spelling(), literal.tokenId());
        }
        EbnfGrammar.Normalized normalized = EbnfGrammar.normalize(text, start, symbols);
        return GrammarSpecification.compile(language, revision, fixed, regex,
                normalized.startSymbol(), normalized.productions());
    }

    private static void yes(boolean p){checks++;if(!p)throw new AssertionError("true #"+checks);}
    private static void no(boolean p){yes(!p);}
    private static void eq(String a,String b){checks++;if(!a.equals(b))
        throw new AssertionError("hash mismatch #"+checks);}
    private static void refuse(Runnable r){checks++;try{r.run();
        throw new AssertionError("expected refusal #"+checks);
            }catch(IllegalArgumentException pass){}}
}
