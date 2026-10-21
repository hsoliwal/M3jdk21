package com.m3.affix;

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

public class RegexRowProof {
    private static long checks, indexed;
    private static void check(boolean cond, String name) {
        checks++;
        if (!cond) throw new AssertionError(name);
    }
    static String literal(Random rnd) {
        String abc = "abcXY012_";
        StringBuilder builder = new StringBuilder();
        for (int i=0,n=1+rnd.nextInt(7);i<n;i++) builder.append(abc.charAt(rnd.nextInt(abc.length())));
        return builder.toString();
    }
    static void trial(String[] corpus, int patterns) {
        M3String[] values=Arrays.stream(corpus).map(M3String::new).toArray(M3String[]::new);
        M3StringRegexRowPrecompute row=M3StringRegexRowPrecompute.compile(values);
        Random rnd=new Random(0x4230f00d);
        String[] first={"abc.*", ".*abc", "abc", "abc.*"};
        for(String pat:first) row.prepare(pat,0);
        check(row.affixesForProof()!=null,"reused index built");
        for(int i=0;i<patterns;i++){
            String lit = i%5==0 ? "abc" : literal(rnd);
            String expr=switch(i%6){
                case 0 -> lit;
                case 1 -> lit+".*";
                case 2 -> ".*"+lit;
                case 3 -> ".*"+lit+".*";
                case 4 -> "^"+lit+"$";
                default -> "[a-z]*";
            };
            int flags=i%23==0 ? Pattern.CASE_INSENSITIVE : 0;
            Pattern expected=Pattern.compile(expr,flags);
            var prepared=row.prepare(expr,flags);
            int[] candidates=prepared.candidateRows();
            int[] actual=prepared.matchingRows();
            boolean[] admitted=new boolean[corpus.length];
            int prev=-1;
            for(int rowid:candidates){
                check(rowid>prev && rowid<corpus.length,"candidate row order");
                prev=rowid; admitted[rowid]=true;
            }
            int count=0;
            for(int rid=0;rid<corpus.length;rid++){
                if(expected.matcher(corpus[rid]).matches()){
                    check(admitted[rid],"false-negative candidate row");
                    count++;
                }
            }
            check(actual.length==count,"matched count");
            prev=-1;
            for(int rid:actual){
                check(rid>prev && expected.matcher(corpus[rid]).matches(),"matched order/truth");
                prev=rid;
            }
            if (prepared.indexedForProof()) indexed++;
        }
    }
    public static void main(String[] args) {
        Random rnd=new Random(0x56300f);
        String[] corpus=new String[512];
        char[] alphabet="abcXYZ01_\r\n".toCharArray();
        for(int i=0;i<corpus.length;i++){
            if(i%32==0) corpus[i]="abc";
            else if(i%32==1) corpus[i]="abc\r\n";
            else if(i%32==2) corpus[i]="xxabc";
            else if(i%32==3) corpus[i]="abcxx";
            else {
                StringBuilder b=new StringBuilder();
                int n=3+rnd.nextInt(19);
                for(int j=0;j<n;j++)b.append(alphabet[rnd.nextInt(alphabet.length)]);
                corpus[i]=b.toString();
            }
        }
        trial(corpus,5000);
        System.out.println("M3_REGEX_ROW_AFFIX_PROOF_PASS checks="+checks+" indexed="+indexed);
    }
}
