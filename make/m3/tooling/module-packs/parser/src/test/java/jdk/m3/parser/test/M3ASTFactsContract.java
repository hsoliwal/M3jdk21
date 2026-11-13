// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ASTFacts;

public final class M3ASTFactsContract {
    private static int checks;
    private static void check(boolean yes) {checks++;if(!yes)throw new AssertionError("typed facts contract");}
    private static void reject(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try {action.run();}catch(Throwable failure){if(kind.isInstance(failure))return;throw new AssertionError("wrong failure",failure);}
        throw new AssertionError("expected rejection "+kind.getName());
    }
    public static void main(String[] args) {
        M3File src=M3File.of(URI.create("memory:/fact"),"declaration");
        var make=M3ASTIndex.builder(src,M3ASTIndex.Domain.JAVAC,8);
        int a=make.add(-1,"UNIT",0,src.length(),M3ASTIndex.EXACT_RANGE);
        int b=make.add(a,"TYPE",0,8,M3ASTIndex.EXACT_RANGE);
        int c=make.add(a,"DECL",8,src.length(),M3ASTIndex.EXACT_RANGE);
        M3ASTIndex index=make.seal();
        long specialNaN=0x7ff8000000000011L;
        M3ASTFacts.Builder build=M3ASTFacts.builder(index,12,12,1024);
        build.signedLong(a,"syntax.kind.ordinal",-9)
                .text(a,"rewrite.marker","root")
                .ieeeDouble(a,"literal.double",Double.longBitsToDouble(specialNaN))
                .booleanValue(b,"symbol.resolved",true)
                .nodeRef(b,"symbol.owner",a)
                .text(b,"symbol.name","T")
                .text(b,"symbol.name","T")
                .signedLong(c,"synthetic.priority",Long.MIN_VALUE);
        M3ASTFacts facts=build.seal();
        check(facts.index()==index && facts.factCount()==8 && M3ASTFacts.SCHEMA.equals("M3_AST_TYPED_FACTS_V1"));
        check(facts.firstFact(a)==0 && facts.endFact(a)==3);
        check(facts.firstFact(b)==3 && facts.endFact(b)==7);
        check(facts.firstFact(c)==7 && facts.endFact(c)==8);
        check(facts.signedLong(0)==-9 && facts.kind(0)==M3ASTFacts.Kind.SIGNED_LONG);
        check(facts.key(1).equals("rewrite.marker") && facts.text(1).equals("root"));
        check(facts.ieeeDoubleRawBits(2)==specialNaN && Double.isNaN(facts.ieeeDouble(2)));
        check(facts.booleanValue(3) && facts.nodeRef(4)==a && facts.owner(5)==b);
        check(facts.text(5).equals("T") && facts.text(6).equals("T"));
        check(facts.signedLong(7)==Long.MIN_VALUE);
        List<Integer> matches=new ArrayList<>();facts.each(b,"symbol.name",matches::add);
        check(matches.equals(List.of(5,6)));
        check(facts.retainedPrimitiveBytes()>0);
        reject(IllegalStateException.class,()->facts.booleanValue(1));
        reject(IllegalStateException.class,()->build.text(c,"late","value"));
        reject(IndexOutOfBoundsException.class,()->facts.firstFact(999));
        reject(IndexOutOfBoundsException.class,()->facts.key(12));
        M3ASTFacts.Builder limited=M3ASTFacts.builder(index,2,2,10);
        reject(IllegalArgumentException.class,()->limited.text(a,"first","huge text value"));
        limited.signedLong(a,"a",1).signedLong(a,"a",2);
        reject(IllegalArgumentException.class,()->limited.signedLong(a,"a",3));
        check(limited.seal().factCount()==2);
        var sorted=M3ASTFacts.builder(index,3,3,100);
        sorted.signedLong(b,"a",1);
        reject(IllegalArgumentException.class,()->sorted.signedLong(a,"b",2));
        reject(IndexOutOfBoundsException.class,()->M3ASTFacts.builder(index,3,3,100).nodeRef(a,"ref",22));
        reject(IllegalArgumentException.class,()->M3ASTFacts.builder(index,3,3,100).text(a," ","x"));
        var zero=M3ASTFacts.builder(index,0,0,0).seal();
        check(zero.firstFact(a)==0 && zero.factCount()==0);
        System.out.println("M3_AST_TYPED_FACTS_PASS checks="+checks);
    }
}
