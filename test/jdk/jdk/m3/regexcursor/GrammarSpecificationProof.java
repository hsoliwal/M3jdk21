// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/** Independent Java21 executable property/contract proof for the prepared grammar candidate. */
public final class GrammarSpecificationProof {
    private static int checks;

    private GrammarSpecificationProof() { }

    public static void main(String[] args) {
        fixedRegexAndGeometry();
        grammarFirstFollowAndEpsilon();
        rejectUnsupportedOrAmbiguous();
        inputGenerationAndIdentity();
        ebnfPreparation();
        binaryImageRoundTrip();
        randomizedRegexDifferential();
        System.out.println("GrammarSpecificationProof PASS checks=" + checks);
    }

    private static GrammarSpecification compile(List<StaticGrammarLexicon.Literal> fixed,
            List<GrammarSpecification.RegexRule> regex, int start,
            List<GrammarSpecification.Production> syntax) {
        return GrammarSpecification.compile("java", "21", fixed, regex, start, syntax);
    }

    private static GrammarSpecification.Production p(int lhs, int... rhs) {
        return new GrammarSpecification.Production(lhs, rhs);
    }

    private static void fixedRegexAndGeometry() {
        GrammarSpecification g = compile(List.of(
                new StaticGrammarLexicon.Literal("=",2,false),
                new StaticGrammarLexicon.Literal("if",3,true),
                new StaticGrammarLexicon.Literal("😀",4,false)),
                List.of(new GrammarSpecification.RegexRule(1,"[\\p{L}_][\\p{L}0-9_]*",0,10),
                        new GrammarSpecification.RegexRule(5,"[0-9]+",0,15)),
                0,List.of());
        GrammarSpecification.Session scan=g.bind(" xx if = 555 😀 z");
        eq(3,scan.classify(4,6));
        eq(2,scan.classify(7,8));
        eq(5,scan.classify(9,12));
        eq(4,scan.classify(13,15));
        eq(1,scan.classify(16,17));
        eq(0,scan.classify(0,1));
        eq(-1,scan.classify(0,1)==0?-1:1);
        yes(g.contextual(3));
        no(g.contextual(1));
        int[] lane=
                GrammarTokenProjection.projectPrepared(g, " xx if = 555 😀 z", new int[]{4,7,9},
                        new int[]{6,8,12},new byte[]{1,1,1},true);
        yes(Arrays.equals(new int[]{3,2,5},lane));
        int[] sparse=
                GrammarTokenProjection.projectPrepared(g, " xx if = 555 😀 z", new int[]{0,4},
                        new int[]{1,6},new byte[]{0,1},false);
        yes(Arrays.equals(new int[]{0,3},sparse));
        reject(IllegalArgumentException.class,()->
                GrammarTokenProjection.projectPrepared(g, " xx if = 555 😀 z", new int[]{0,4},
                        new int[]{1,6},new byte[]{0,1},true));
        reject(IllegalArgumentException.class,()->
                GrammarTokenProjection.projectPrepared(g, " xx if = 555 😀 z", new int[]{1,0},
                        new int[]{2,1},new byte[]{1,1},false));
        reject(IllegalArgumentException.class,()->scan.classify(0,0));
        reject(IndexOutOfBoundsException.class,()->scan.classify(-1,1));
        reject(IllegalStateException.class,()->g.accepts(new int[0]));
        scan.rebind("abc 42");
        eq(1,scan.classify(0,3));
        eq(5,scan.classify(4,6));
        eq(0,scan.classify(0,6));
    }

    private static void grammarFirstFollowAndEpsilon() {
        GrammarSpecification e=compile(List.of(new StaticGrammarLexicon.Literal("+",3,false)),
                List.of(new GrammarSpecification.RegexRule(1,"[A-Za-z]+",0,0),
                        new GrammarSpecification.RegexRule(4,"[0-9]+",0,0)),
                -1,List.of(p(-1,-2,-3),p(-3,3,-2,-3),p(-3),p(-2,1),p(-2,4)));
        yes(e.hasSyntax());
        yes(e.accepts(new int[]{1}));
        yes(e.accepts(new int[]{4,3,1}));
        yes(e.accepts(new int[]{1,3,4,3,1}));
        no(e.accepts(new int[]{}));
        no(e.accepts(new int[]{1,3}));
        no(e.accepts(new int[]{3,1}));
        no(e.accepts(new int[]{1,4}));
        no(e.accepts(new int[]{1,0}));
        eq(5,e.productionCount());
        eq(3,e.terminalCount());
        GrammarSpecification nullable=compile(List.of(
                new StaticGrammarLexicon.Literal("a",1,false),
                new StaticGrammarLexicon.Literal("b",2,false)),List.of(),
                -1,List.of(p(-1,-2,2),p(-2,1),p(-2)));
        yes(nullable.accepts(new int[]{2}));
        yes(nullable.accepts(new int[]{1,2}));
        no(nullable.accepts(new int[]{1}));
        no(nullable.accepts(new int[]{1,1,2}));
        GrammarSpecification recursive=
                compile(List.of(new StaticGrammarLexicon.Literal("x",1,false)),
                List.of(),-1,List.of(p(-1,1,-1),p(-1)));
        int[] many=new int[4_000];Arrays.fill(many,1);
        yes(recursive.accepts(many));
        many[37]=2;
        no(recursive.accepts(many));
    }

    private static void rejectUnsupportedOrAmbiguous() {
        List<StaticGrammarLexicon.Literal> a=List.of(new StaticGrammarLexicon.Literal("a",1,false),
                new StaticGrammarLexicon.Literal("b",2,false));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(),-1,List.of(p(-1,1),p(-1,1,2))));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(),-1,List.of(p(-1,-1,1),p(-1,2))));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(),-1,List.of(p(-1,-2),p(-2,-1),p(-2))));
        reject(IllegalArgumentException.class,()->compile(a,List.of(),-1,List.of(p(-1,3))));
        reject(IllegalArgumentException.class,()->compile(a,List.of(),-1,List.of(p(-1,0))));
        reject(IllegalArgumentException.class,()->compile(a,List.of(),-9,List.of(p(-1,1))));
        reject(IllegalArgumentException.class,()->compile(a,List.of(),-1,List.of()));
        reject(IllegalArgumentException.class,()->compile(a,List.of(),0,List.of(p(-1,1))));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(new GrammarSpecification.RegexRule(1,"abc",0,0)),0,List.of()));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(new GrammarSpecification.RegexRule(3,"a*",0,0)),0,List.of()));
        reject(IllegalArgumentException.class,()->
                compile(a,List.of(new GrammarSpecification.RegexRule(0,"x",0,0)),0,List.of()));
        reject(java.util.regex.PatternSyntaxException.class,()->
                compile(a,List.of(new GrammarSpecification.RegexRule(3,"[",0,0)),0,List.of()));
        int[] source={1};
        GrammarSpecification.Production production=p(-1,source);
        source[0]=2;
        yes(Arrays.equals(new int[]{1},production.rhs()));
        production.rhs()[0]=2;
        yes(Arrays.equals(new int[]{1},production.rhs()));
    }

    private static void inputGenerationAndIdentity() {
        List<StaticGrammarLexicon.Literal> a=List.of(new StaticGrammarLexicon.Literal("if",10,true),
                new StaticGrammarLexicon.Literal("=",11,false));
        List<StaticGrammarLexicon.Literal> reversed=new ArrayList<>(a);
        java.util.Collections.reverse(reversed);
        GrammarSpecification g1=
                compile(a,List.of(new GrammarSpecification.RegexRule(20,"[a-z]+",0,1)),0,List.of());
        GrammarSpecification g2=
                compile(reversed,List.of(new GrammarSpecification.RegexRule(20,"[a-z]+",0,1)),0,
                        List.of());
        eq(g1.grammarSha256(),g2.grammarSha256());
        GrammarSpecification g3=GrammarSpecification.compile("java","22",a,
                List.of(new GrammarSpecification.RegexRule(20,"[a-z]+",0,1)),0,List.of());
        no(g1.grammarSha256().equals(g3.grammarSha256()));
        GrammarSpecification.Session s1=g1.bind("if");
        GrammarSpecification.Session s2=g1.bind("abc");
        eq(10,s1.classify(0,2));
        eq(20,s2.classify(0,3));
        eq(10,s1.classify(0,2));
        eq(20,s1.rebind("abc").classify(0,3));
        // No eager string flatten; compilation is CharSequence-backed.
        CharSequence seq=new StringBuilder("hello");
        eq(20,g1.bind(seq).classify(0,5));
        // Regex flags survive precomputation.
        GrammarSpecification insensitive=compile(List.of(),List.of(
                new GrammarSpecification.RegexRule(7,"abc",Pattern.CASE_INSENSITIVE,0)),0,
                        List.of());
        eq(7,insensitive.bind("ABC").classify(0,3));
        // JDK Pattern region anchoring, rather than substring allocation or a new regex engine.
        GrammarSpecification anchored=compile(List.of(),List.of(
                new GrammarSpecification.RegexRule(9,"^abc$",0,0)),0,List.of());
        eq(9,anchored.bind("xxabczz").classify(2,5));
    }

    private static void ebnfPreparation() {
        String schema = """
            (* normalized Java-like expression grammar *)
            expression ::= atom, { '+', atom } ;
            atom = IDENT | NUMBER ;
            """;
        EbnfGrammar.Normalized bnf = EbnfGrammar.normalize(schema, "expression",
                java.util.Map.of("+", 3, "IDENT", 1, "NUMBER", 2));
        GrammarSpecification prepared = compile(List.of(
                        new StaticGrammarLexicon.Literal("+", 3, false)),
                List.of(new GrammarSpecification.RegexRule(1,"[a-z]+",0,0),
                        new GrammarSpecification.RegexRule(2,"[0-9]+",0,0)),
                bnf.startSymbol(), bnf.productions());
        yes(prepared.accepts(new int[]{1}));
        yes(prepared.accepts(new int[]{2,3,1}));
        yes(prepared.accepts(new int[]{1,3,2,3,1}));
        no(prepared.accepts(new int[]{3,1}));
        no(prepared.accepts(new int[]{1,3}));
        EbnfGrammar.Normalized optional = EbnfGrammar.normalize(
                "head = [ 'if' ], WORD ;", "head",java.util.Map.of("if",1,"WORD",2));
        GrammarSpecification opt = compile(List.of(new StaticGrammarLexicon.Literal("if",1,false)),
                List.of(new GrammarSpecification.RegexRule(2,"[a-z]+",0,0)),
                optional.startSymbol(),optional.productions());
        yes(opt.accepts(new int[]{2}));
        yes(opt.accepts(new int[]{1,2}));
        no(opt.accepts(new int[]{1}));
        no(opt.accepts(new int[]{1,1,2}));
        // Grouped alternatives do not require an exponential grammar expansion.
        EbnfGrammar.Normalized groups = EbnfGrammar.normalize(
                "root = 'x', ('y' | 'z'), { 'x' } ;", "root",
                java.util.Map.of("x",1,"y",2,"z",3));
        GrammarSpecification grouped = compile(List.of(
                new StaticGrammarLexicon.Literal("x",1,false),
                new StaticGrammarLexicon.Literal("y",2,false),
                new StaticGrammarLexicon.Literal("z",3,false)),
                List.of(),groups.startSymbol(),groups.productions());
        yes(grouped.accepts(new int[]{1,2}));
        yes(grouped.accepts(new int[]{1,3,1,1}));
        no(grouped.accepts(new int[]{1,1,3}));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = UNKNOWN ;", "a", java.util.Map.of("b", 1)));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = 'x' ; a = 'x' ;", "a", java.util.Map.of("x", 1)));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = 'x', ;", "a", java.util.Map.of("x", 1)));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = { 'x' ;", "a", java.util.Map.of("x", 1)));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = (* unterminated", "a", java.util.Map.of("x", 1)));
        reject(IllegalArgumentException.class, () -> EbnfGrammar.normalize(
                "a = 'x' ;", "missing", java.util.Map.of("x", 1)));
        EbnfGrammar.Normalized recursive = EbnfGrammar.normalize("a = a | 'x' ;",
                "a",java.util.Map.of("x",1));
        reject(IllegalArgumentException.class, () -> compile(
                List.of(new StaticGrammarLexicon.Literal("x",1,false)), List.of(),
                recursive.startSymbol(), recursive.productions()));
        // Parenthesized alternation with all-empty branch must stay rejectable.
        EbnfGrammar.Normalized nullable = EbnfGrammar.normalize("a = { [ 'x' ] } ;",
                "a",java.util.Map.of("x",1));
        reject(IllegalArgumentException.class, () -> compile(
                List.of(new StaticGrammarLexicon.Literal("x",1,false)), List.of(),
                nullable.startSymbol(), nullable.productions()));
    }

    private static void binaryImageRoundTrip() {
        var rows=List.of(new StaticGrammarLexicon.Literal("x",1,false),
                new StaticGrammarLexicon.Literal("+",2,false));
        var rules=List.of(new GrammarSpecification.RegexRule(3,"[0-9]+",0,0));
        var productions=List.of(p(-1,-2,-3),p(-2,1),p(-2,3),
                p(-3,2,-2,-3),p(-3));
        GrammarSpecification compiled=compile(rows,rules,-1,productions);
        byte[] image=compiled.syntaxImage();
        GrammarSpecification reused=GrammarSpecification.compileWithSyntaxImage("java","21",
                rows,rules,-1,productions,image);
        eq(compiled.grammarSha256(), reused.grammarSha256());
        yes(Arrays.equals(image,reused.syntaxImage()));
        for(int n=0;n<100;n++) {
            int[] tokens=new int[n*2+1];
            for(int k=0;k<tokens.length;k++) tokens[k]=k%2==0?1:2;
            eq(compiled.accepts(tokens)?1:0,reused.accepts(tokens)?1:0);
            tokens[n]=3;
            eq(compiled.accepts(tokens)?1:0,reused.accepts(tokens)?1:0);
        }
        byte[] corrupt=image.clone();
        corrupt[corrupt.length-33]^=1;
        reject(IllegalArgumentException.class,()->GrammarSpecification.compileWithSyntaxImage(
                "java","21",rows,rules,-1,productions,corrupt));
        reject(IllegalArgumentException.class,()->GrammarSpecification.compileWithSyntaxImage(
                "java","22",rows,rules,-1,productions,image));
        reject(IllegalArgumentException.class,()->GrammarSpecification.compileWithSyntaxImage(
                "java","21",rows,rules,-1,List.of(p(-1,1)),image));
        reject(IllegalArgumentException.class,()->GrammarSpecification.compileWithSyntaxImage(
                "java","21",rows,rules,0,List.of(),image));
        reject(IllegalStateException.class,()->compile(rows,rules,0,List.of()).syntaxImage());
        byte[] shortImage=Arrays.copyOf(image,10);
        reject(IllegalArgumentException.class,()->GrammarSpecification.compileWithSyntaxImage(
                "java","21",rows,rules,-1,productions,shortImage));
    }

    private static void randomizedRegexDifferential() {
        String[] expressions={"[a-z]+","a+","a|ab","(ab)+","[\\p{L}]+","[0-9]{1,3}",
                "(?:foo|bar)","(a)\\1","(?i:a+)","^b+$"};
        List<GrammarSpecification.RegexRule> rules=new ArrayList<>();
        for(int i=0;i<expressions.length;i++)
            rules.add(new GrammarSpecification.RegexRule(i+1,expressions[i],0,i));
        GrammarSpecification prepared=compile(List.of(),rules,0,List.of());
        Pattern[] oracle=Arrays.stream(expressions).map(Pattern::compile).toArray(Pattern[]::new);
        Random random=new Random(0x5f57_2026L);
        String alphabet="abfo012あé";
        for(int n=0;n<20_000;n++) {
            int len=random.nextInt(10)+1;
            StringBuilder chars=new StringBuilder(len);
            for(int k=0;k<len;k++) chars.append(alphabet.charAt(random.nextInt(alphabet.length())));
            String input="<"+chars+">";
            int expected=0;
            for(int i=0;i<oracle.length;i++) {
                if(oracle[i].matcher(input).region(1,len+1).matches()) {expected=i+1;break;}
            }
            eq(expected,prepared.bind(input).classify(1,len+1));
        }
    }

    private static void yes(boolean value){checks++;if(!value)
        throw new AssertionError("expected true #"+checks);}
    private static void no(boolean value){yes(!value);}
    private static void eq(int expected,int actual){checks++;if(expected!=actual)
        throw new AssertionError("expected="+expected+" actual="+actual+" #"+checks);}
    private static void eq(String expected,String actual){checks++;if(!expected.equals(actual))
        throw new AssertionError("digest differs #"+checks);}
    private static void reject(Class<? extends Throwable> cls,Runnable action){checks++;
        try{action.run();throw new AssertionError("expected "+cls.getSimpleName());
            }catch(Throwable error){if(!cls.isInstance(error))
                throw new AssertionError("unexpected "+error,error);}}
}
