package com.m3.affix;
import java.util.*;

public class AffixIndexProof {
    static long checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static int[] brute(String[] words, String affix, boolean reverse) {
        int[] out = new int[words.length]; int n = 0;
        for (int i=0; i<words.length; i++) {
            if ((reverse?words[i].endsWith(affix):words[i].startsWith(affix))) out[n++]=i;
        }
        return Arrays.copyOf(out,n);
    }
    static void verify(String[] words) {
        M3String[] sources=Arrays.stream(words).map(M3String::new).toArray(M3String[]::new);
        M3StringAffixPrecompute idx=M3StringAffixPrecompute.tryPrepare(sources);
        check(idx!=null,"budget refused reasonable fixture");
        check(idx.size()==words.length,"size");
        check(idx.primitiveBytes()==(long)words.length*40,"retained primitive geometry");
        for (int i=0;i<words.length;i++) {
            int pr=idx.prefixRank(i), sr=idx.suffixRank(i);
            check(pr>=0&&pr<words.length&&sr>=0&&sr<words.length,"rank");
            check(idx.prefixEnd(i)>pr && idx.suffixEnd(i)>sr,"descendant ranges nonempty");
            int pp=idx.prefixParent(i), sp=idx.suffixParent(i);
            check(pp<0|| (pp!=i && words[i].startsWith(words[pp])&&words[pp].length()<words[i].length()),"prefix parent");
            check(sp<0|| (sp!=i && words[i].endsWith(words[sp])&&words[sp].length()<words[i].length()),"suffix parent");
            for (int j=0;j<words.length;j++){
                boolean prefix=words[j].startsWith(words[i]);
                boolean suffix=words[j].endsWith(words[i]);
                check(prefix == (idx.prefixRank(j)>=idx.prefixBegin(i) && idx.prefixRank(j)<idx.prefixEnd(i)),"prefix subtree "+i+"/"+j);
                check(suffix == (idx.suffixRank(j)>=idx.suffixBegin(i) && idx.suffixRank(j)<idx.suffixEnd(i)),"suffix subtree "+i+"/"+j);
            }
        }
        String[] common={"", "a", "ab", "b", "XYZ", "hello", "123", "smile", "_"};
        for(String q:common) search(idx,words,q);
        Random r=new Random(20261017L);
        for(int i=0;i<500;i++) {
            String origin=words[r.nextInt(words.length)];
            int len=r.nextInt(origin.length()+1);
            String q=r.nextBoolean()?origin.substring(0,len):origin.substring(origin.length()-len);
            search(idx,words,q);
            search(idx,words,randomWord(r,6));
        }
    }
    static void search(M3StringAffixPrecompute index,String[] words,String query){
        M3StringAffixPrecompute.Range pre=index.prefixRange(query);
        M3StringAffixPrecompute.Range suf=index.suffixRange(query);
        check(Arrays.equals(index.rowsInSourceOrder(pre,false),brute(words,query,false)),"prefix query "+query);
        check(Arrays.equals(index.rowsInSourceOrder(suf,true),brute(words,query,true)),"suffix query "+query);
    }
    static String randomWord(Random r,int max) {
        char[] alphabet={'a','b','c','A','_','0','\u0000','\uD83D','\uDE00','\uFFFF'};
        StringBuilder b=new StringBuilder();
        int n=r.nextInt(max+1);
        for(int i=0;i<n;i++) b.append(alphabet[r.nextInt(alphabet.length)]);
        return b.toString();
    }
    public static void main(String[] args) {
        verify(new String[]{"", "a", "ab", "abc", "a", "ab", "abb", "b", "ba", "a\n", "🙂", "a🙂", "🙂a", "", "\u0000", "a\u0000", "\uFFFF", "ab\uFFFF"});
        Random r=new Random(100);
        for (int trial=0;trial<12;trial++){
            String[] words=new String[64+trial*7];
            for(int j=0;j<words.length;j++)words[j]=randomWord(r,9);
            verify(words);
        }
        System.out.println("M3_AFFIX_PRECOMPUTE_PROOF_PASS checks="+checks);
    }
}
