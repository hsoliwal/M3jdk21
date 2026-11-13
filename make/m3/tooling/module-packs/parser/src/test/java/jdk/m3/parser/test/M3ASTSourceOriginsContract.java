// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;
import java.net.URI;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTSourceOrigins;
import jdk.m3.parser.M3File;

public final class M3ASTSourceOriginsContract {
    private static int checks;
    private static void check(boolean yes){checks++;if(!yes)throw new AssertionError("origin contract");}
    private static void reject(Class<? extends Throwable> kind,Runnable code){
        checks++;try{code.run();}catch(Throwable failure){if(kind.isInstance(failure))return;
            throw new AssertionError("wrong failure",failure);}throw new AssertionError("expected refusal");
    }
    public static void main(String[] args){
        M3File main=M3File.of(URI.create("file:///unit.c"),"USE(X)");
        M3File header=M3File.of(URI.create("file:///macro.h"),"#define X 100");
        M3File sameUriNewGeneration=M3File.of(URI.create("file:///macro.h"),"#define X 101");
        var b=M3ASTIndex.builder(main,M3ASTIndex.Domain.JAVAC,5);
        int root=b.add(-1,"UNIT",0,6,M3ASTIndex.EXACT_RANGE);
        int use=b.add(root,"USE",0,6,M3ASTIndex.EXACT_RANGE);
        int named=b.add(use,"NAME",4,5,M3ASTIndex.EXACT_RANGE);
        M3ASTIndex index=b.seal();
        var builder=M3ASTSourceOrigins.builder(index,6,3,100);
        builder.add(root,M3ASTSourceOrigins.Role.EXPANSION,main,0,6)
                .add(use,M3ASTSourceOrigins.Role.SPELLING,header,10,13)
                .add(use,M3ASTSourceOrigins.Role.EXPANSION,main,4,5)
                .add(named,M3ASTSourceOrigins.Role.GENERATED,sameUriNewGeneration,10,13);
        M3ASTSourceOrigins origins=builder.seal();
        check(origins.index()==index && origins.originCount()==4 && origins.sourceCount()==3);
        check(origins.firstOrigin(root)==0 && origins.endOrigin(root)==1);
        check(origins.firstOrigin(use)==1 && origins.endOrigin(use)==3);
        check(origins.firstOrigin(named)==3 && origins.endOrigin(named)==4);
        check(origins.source(1)==header && origins.source(3)==sameUriNewGeneration);
        check(origins.sourceId(1)!=origins.sourceId(3));
        check(origins.sourceText(1).equals("100") && origins.sourceText(3).equals("101"));
        check(origins.role(1)==M3ASTSourceOrigins.Role.SPELLING && origins.owner(2)==use);
        check(origins.start(2)==4 && origins.end(2)==5);
        check(origins.retainedPrimitiveBytes()>0);
        reject(IllegalStateException.class,()->builder.add(named,M3ASTSourceOrigins.Role.SPELLING,main,0,1));
        reject(IndexOutOfBoundsException.class,()->origins.sourceAt(9));
        reject(IndexOutOfBoundsException.class,()->origins.firstOrigin(9));
        reject(IndexOutOfBoundsException.class,()->origins.owner(9));
        reject(IllegalArgumentException.class,()->M3ASTSourceOrigins.builder(index,2,1,1));
        var small=M3ASTSourceOrigins.builder(index,2,1,main.length());
        reject(IllegalArgumentException.class,()->small.add(root,M3ASTSourceOrigins.Role.SPELLING,header,0,1));
        check(small.add(root,M3ASTSourceOrigins.Role.EXPANSION,main,0,1).seal().sourceCount()==1);
        var unsorted=M3ASTSourceOrigins.builder(index,2,2,100);
        unsorted.add(named,M3ASTSourceOrigins.Role.SPELLING,header,0,1);
        reject(IllegalArgumentException.class,()->unsorted.add(root,M3ASTSourceOrigins.Role.SPELLING,main,0,1));
        reject(IndexOutOfBoundsException.class,()->M3ASTSourceOrigins.builder(index,1,2,100)
                .add(root,M3ASTSourceOrigins.Role.SPELLING,main,-1,1));
        var empty=M3ASTSourceOrigins.builder(index,0,1,main.length()).seal();
        check(empty.originCount()==0 && empty.sourceCount()==1);
        System.out.println("M3_AST_SOURCE_ORIGINS_PASS checks="+checks);
    }
}
