// SPDX-License-Identifier: Apache-2.0
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/** Compiled separately against exact original and candidate product sources. */
public final class DelimitedLegacyContract {
    private static final MessageDigest DIGEST;
    private static long cases;
    static { try { DIGEST = MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException e) { throw new ExceptionInInitializerError(e); } }
    private DelimitedLegacyContract() { }
    private static void text(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (int shift=24;shift>=0;shift-=8) DIGEST.update((byte)(bytes.length>>>shift));
        DIGEST.update(bytes);
    }
    private static void test(String source, char separator, Limits limits) {
        cases++;
        TextFrame frame=TextFrame.fromString(source);
        try {
            List<List<String>> records=StructuredTextTools.delimited(frame,separator,limits);
            text("OK:"+records.size());
            for (var row:records) {text("ROW:"+row.size());for(String field:row) text(field);}
            for(int[] columns:new int[][]{{0},{1,0},{0,0},{2},{-1}, {}}) {
                try { text(StructuredTextTools.selectDelimited(frame,separator,columns,limits).text()); }
                catch(RuntimeException e){text(e.getClass().getName()+":"+e.getMessage());}
            }
        } catch (RuntimeException e) { text(e.getClass().getName()+":"+e.getMessage()); }
    }
    public static void main(String[] args) {
        char[] alphabet={'a',',','"','\r','\n'};
        int count=1;
        for(int length=0;length<=7;length++) {
            for(int code=0;code<count;code++) {
                int n=code;char[] s=new char[length];
                for(int i=0;i<length;i++){s[i]=alphabet[n%alphabet.length];n/=alphabet.length;}
                String value=new String(s);
                for(Limits limits:new Limits[]{new Limits(10,0,100,1024),new Limits(1,0,100,1024),
                        new Limits(10,0,1,1024),new Limits(10,0,100,4)}) test(value,',',limits);
            }
            count*=alphabet.length;
        }
        for(String value:List.of("", "\"漢\",\"😀\"\r\n", "a\t\"b\tc\"\n", "a\0b,c", "x;\"y;z\"", "\ufeffa,b")) {
            for(char delimiter:new char[]{',','\t',';','漢','\0','"','\r','\n','\ud800'})
                test(value,delimiter,Limits.standard());
        }
        // Unrelated existing format owners are not modified.
        text(StructuredTextTools.ini(TextFrame.fromString("a=b\n[s]\nx=y\n"),Limits.standard()).toString());
        text(StructuredTextTools.dotenv(TextFrame.fromString("export A='x'\nB=y # c\n"),Limits.standard()).toString());
        System.out.println("DELIMITED_LEGACY_PARITY cases="+cases+" digest="+HexFormat.of().formatHex(DIGEST.digest()));
    }
}
