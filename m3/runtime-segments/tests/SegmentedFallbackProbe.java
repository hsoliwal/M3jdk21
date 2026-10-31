/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.Pattern;
public class SegmentedFallbackProbe {
    static int checks;
    static void check(boolean v) { checks++; if(!v)throw new AssertionError("check="+checks); }
    static String fresh(String a,String b) { return a.concat(b); }
    public static void main(String[] args) throws Exception {
        Field value=String.class.getDeclaredField("value");value.setAccessible(true);
        Field cache=String.class.getDeclaredField("m3Flat");cache.setAccessible(true);
        Field parts=String.class.getDeclaredField("m3Parts");parts.setAccessible(true);
        for(String a: List.of("ab","日本語","\ud83d","\0","\udc00x")){
            for(String b: List.of("cd","abc","\ude00","\u0100","\ud800")){
                char[] raw=new char[a.length()+b.length()];a.getChars(0,a.length(),raw,0);b.getChars(0,b.length(),raw,a.length());
                String expected=new String(raw); String s=fresh(a,b);
                check(value.get(s)==null&&cache.get(s)==null);
                for(int start=0;start<=raw.length;start++)for(int end=start;end<=raw.length;end++) {
                    check(s.substring(start,end).equals(new String(Arrays.copyOfRange(raw,start,end))));
                }
                check(s.compareTo(expected)==0);check(s.contentEquals(new StringBuilder(expected)));
                check(s.equalsIgnoreCase(expected));check(Arrays.equals(s.toCharArray(),raw));
                check(s.indexOf(b)==expected.indexOf(b));check(s.lastIndexOf(a)==expected.lastIndexOf(a));
                check(s.startsWith(a)&&s.endsWith(b));check(s.matches(Pattern.quote(expected)));
                check(s.repeat(2).equals(expected.repeat(2)));check(s.replace("a","Q").equals(expected.replace("a","Q")));
                check(s.strip().equals(expected.strip()));check(s.toUpperCase(Locale.ROOT).equals(expected.toUpperCase(Locale.ROOT)));
                check(Arrays.equals(s.codePoints().toArray(),expected.codePoints().toArray()));
                for(Charset charset:List.of(StandardCharsets.UTF_8,StandardCharsets.UTF_16,StandardCharsets.ISO_8859_1,Charset.forName("ISO-2022-JP")))
                    check(Arrays.equals(s.getBytes(charset),expected.getBytes(charset)));
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(s);}
                try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){check(in.readObject().equals(expected));}
                check(value.get(s)==null&&parts.get(s)!=null&&cache.get(s)!=null);
            }
        }
        String joined=new String(new char[]{'x'});
        for(int i=1;i<256;i++) joined=joined.concat(new String(new char[]{'x'}));
        check(value.get(joined)==null&&cache.get(joined)==null);
        check(((String[])parts.get(joined)).length==256);
        String bounded=joined.concat(new String(new char[]{'x'}));
        check(value.get(bounded)!=null&&parts.get(bounded)==null);check(bounded.equals("x".repeat(257)));
        String after=bounded.concat("z");check(value.get(after)==null);check(((String[])parts.get(after)).length==2);
        // Arbitrary object concat conversion and null semantics remain ordinary Java.
        Object a=null,b=new Object(){public String toString(){return "done";}};
        check((""+a+b).equals("nulldone"));
        java.nio.file.Path directory=java.nio.file.Files.createTempDirectory("m3-segments-");
        java.nio.file.Path file=null;
        try {
            String path=directory.toString().concat("/").concat("payload.txt");
            check(value.get(path)==null&&cache.get(path)==null);
            String canonical=new File(path).getCanonicalPath();
            check(canonical.endsWith("/payload.txt"));check(cache.get(path)!=null);
            file=java.nio.file.Path.of(path);
            String payload="native-".concat("boundary");
            java.nio.file.Files.writeString(file,payload);
            check(java.nio.file.Files.readString(file).equals("native-boundary"));
        } finally {
            if(file!=null)java.nio.file.Files.deleteIfExists(file);
            java.nio.file.Files.delete(directory);
        }
        System.out.println("SEGMENTED_FALLBACK_PASS checks="+checks);
    }
}
