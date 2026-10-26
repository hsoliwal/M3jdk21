// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3FileEdits;

/** Fuzzed source-generation and conservative AST invalidation contract. */
public final class M3FileEditsContract {
    private static int checks;
    private static void check(boolean value) { checks++; if(!value) throw new AssertionError("edit contract"); }
    private static void reject(Runnable op) {
        checks++; try {op.run();} catch(IllegalArgumentException expected){return;}
        throw new AssertionError("expected rejection");
    }
    public static void main(String[] args) {
        M3File old=M3File.of(URI.create("memory:/e"), "a\ud83d\ude00bcdef");
        var delta=M3FileEdits.apply(old, List.of(new M3FileEdits.Edit(3,4,"xyz")),1,100);
        check(delta.after().text().equals("a\ud83d\ude00xyzcdef"));
        check(old.text().equals("a\ud83d\ude00bcdef") && delta.after().byteSha256().isEmpty());
        check(delta.retainedPrimitiveBytes()==16 && delta.editCount()==1);
        check(delta.translateUntouched(4)==-1 && delta.translateUntouched(5)==7);
        check(delta.touches(4,6) && !delta.touches(5,6));
        reject(()->M3FileEdits.apply(old,List.of(new M3FileEdits.Edit(2,2,"x")),1,30));
        reject(()->M3FileEdits.apply(old,List.of(new M3FileEdits.Edit(3,4,"a"),
                new M3FileEdits.Edit(2,3,"b")),2,30));
        reject(()->M3FileEdits.apply(old,List.of(new M3FileEdits.Edit(0,0,"a"),
                new M3FileEdits.Edit(0,0,"b")),2,30));
        reject(()->M3FileEdits.apply(old,List.of(new M3FileEdits.Edit(3,4,"0123456789")),1,8));
        var b=M3ASTIndex.builder(old,M3ASTIndex.Domain.JAVAC,5);
        int root=b.add(-1,"ROOT",0,old.length(),M3ASTIndex.EXACT_RANGE);
        int left=b.add(root,"LEFT",0,2,M3ASTIndex.EXACT_RANGE);
        int middle=b.add(root,"MIDDLE",3,4,M3ASTIndex.EXACT_RANGE);
        int far=b.add(root,"RIGHT",5,8,M3ASTIndex.EXACT_RANGE);
        int unknown=b.add(root,"UNKNOWN",-1,-1,0);
        BitSet dirty=delta.touchedNodes(b.seal(),5);
        check(dirty.get(root) && dirty.get(middle) && dirty.get(unknown));
        check(!dirty.get(left) && !dirty.get(far));
        check(M3FileEdits.apply(old,List.of(),0,old.length()).after()==old);
        Random r=new Random(2135888);
        String[] pieces={"a","é","\ud83d\ude00","\r\n","\ud800","z"};
        String[] replacements={"","*", "\ud83d\ude00","xyz"};
        for(int trial=0;trial<1400;trial++){
            StringBuilder text=new StringBuilder();
            for(int i=r.nextInt(30);i>0;i--)text.append(pieces[r.nextInt(pieces.length)]);
            String original=text.toString();
            List<Integer> boundaries=new ArrayList<>();
            for(int i=0;i<=original.length();i++)
                if(!(i>0 && i<original.length()
                        && Character.isHighSurrogate(original.charAt(i-1))
                        && Character.isLowSurrogate(original.charAt(i)))) boundaries.add(i);
            int at=boundaries.get(r.nextInt(boundaries.size()));
            int end=at;
            for(int i=0;i<boundaries.size();i++) if(boundaries.get(i)>=at && r.nextInt(5)==0) {
                end=boundaries.get(i); break;
            }
            String replacement=replacements[r.nextInt(replacements.length)];
            M3File source=M3File.of(URI.create("memory:/random"),original);
            var result=M3FileEdits.apply(source,List.of(new M3FileEdits.Edit(at,end,replacement)),1,512);
            String expected=original.substring(0,at)+replacement+original.substring(end);
            check(result.after().text().equals(expected) && source.text().equals(original));
            check(result.oldStart(0)==at && result.oldEnd(0)==end);
            for(int pos=0;pos<=original.length();pos++){
                int mapped=result.translateUntouched(pos);
                if(mapped<0)continue;
                check(mapped<=expected.length());
                if(pos<original.length() && mapped<expected.length() && !result.touches(pos,pos))
                    check(original.charAt(pos)==expected.charAt(mapped));
            }
        }
        System.out.println("M3_FILE_EDITS_PASS checks="+checks);
    }
}
