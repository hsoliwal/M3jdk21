/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3JDK21 StringReader transfers immutable String ranges to trusted encoders without a char[] intermediary
 * @run main M3StringReaderTransferTest
 */

import java.io.*;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class M3StringReaderTransferTest {
    private static int checks;
    private static void check(boolean b, String s) { checks++; if (!b) throw new AssertionError(s); }
    private static final class RecordingWriter extends Writer {
        private final StringBuilder sink = new StringBuilder();
        int arrayCalls, stringCalls;
        @Override public void write(char[] a, int o, int l) { arrayCalls++; sink.append(a,o,l); }
        @Override public void write(String s, int o, int l) { stringCalls++; sink.append(s,o,o+l); }
        @Override public void flush() {} @Override public void close() {}
    }
    private static final class ObservedReader extends StringReader {
        int readCalls;
        ObservedReader(String s) {super(s);}
        @Override public int read(char[] a,int o,int l)throws IOException {readCalls++;return super.read(a,o,l);}
    }
    private static void testText(String s, Charset cs) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try (StringReader src = new StringReader(s);
             OutputStreamWriter w = new OutputStreamWriter(bytes,cs)) {
            check(src.transferTo(w)==s.length(), "count");
            check(src.transferTo(w)==0L, "eof repeat");
        }
        check(Arrays.equals(bytes.toByteArray(),s.getBytes(cs)), "encoded bytes " + cs);
        RecordingWriter probe=new RecordingWriter();
        try(StringReader src=new StringReader(s)) {
            check(src.transferTo(probe)==s.length(), "generic count");
        }
        check(probe.sink.toString().equals(s), "generic text");
        check(probe.stringCalls==0 && (s.isEmpty() ? probe.arrayCalls==0 : probe.arrayCalls>0),"custom writer uses inherited char[] path");
        ByteArrayOutputStream subclassBytes = new ByteArrayOutputStream();
        ObservedReader sub = new ObservedReader(s);
        try(OutputStreamWriter w = new OutputStreamWriter(subclassBytes,cs)) {
            check(sub.transferTo(w)==s.length(),"subclass count");
        }
        check(sub.readCalls>0,"StringReader subclass override observed");
        check(Arrays.equals(subclassBytes.toByteArray(),s.getBytes(cs)),"subclass bytes");
    }
    private static final class TraceCharset extends Charset {
        TraceCharset() { super("X-M3-READER-TRACE", null); }
        public boolean contains(Charset cs) { return cs==this; }
        public CharsetDecoder newDecoder() { throw new UnsupportedOperationException(); }
        public CharsetEncoder newEncoder() { return new TraceEncoder(this); }
    }
    private static final class TraceEncoder extends CharsetEncoder {
        boolean sawArray, sawReadOnly;
        TraceEncoder(Charset cs) { super(cs,1,1); }
        @Override public boolean isLegalReplacement(byte[] replacement) { return true; }
        @Override protected CoderResult encodeLoop(CharBuffer src,ByteBuffer dst) {
            if(src.hasRemaining()) { sawArray |= src.hasArray(); sawReadOnly |= src.isReadOnly(); }
            while(src.hasRemaining()) {
                if(!dst.hasRemaining()) return CoderResult.OVERFLOW;
                dst.put((byte)src.get());
            }
            return CoderResult.UNDERFLOW;
        }
    }
    private static void traceDirect(boolean expectDirect) throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        TraceEncoder encoder = (TraceEncoder)new TraceCharset().newEncoder();
        try (OutputStreamWriter out = new OutputStreamWriter(b,encoder);
             StringReader r = new StringReader("abcdef")) {
             check(r.transferTo(out)==6L,"trace count");
        }
        check(Arrays.equals(b.toByteArray(),"abcdef".getBytes(StandardCharsets.US_ASCII)),"trace bytes");
        if(expectDirect) check(encoder.sawReadOnly && !encoder.sawArray,"native String range to encoder");
        else check(encoder.sawArray,"stock char[] baseline");
    }
    private static void fileWriter() throws Exception {
        Path p = Files.createTempFile("m3-string-reader-", ".txt");
        try {
            String text="\u0000a\ud83d\ude00b\r\n".repeat(3000);
            try(StringReader r = new StringReader(text); FileWriter w = new FileWriter(p.toFile(),StandardCharsets.UTF_8)) {
                check(r.transferTo(w)==text.length(),"FileWriter count");
            }
            check(Files.readString(p,StandardCharsets.UTF_8).equals(text),"FileWriter content");
        } finally {Files.deleteIfExists(p);}
    }
    public static void main(String[] a)throws Exception {
        traceDirect(a.length==0 || !a[0].equals("baseline"));
        fileWriter();
        String[] cases={"", "a", "abc", "abc\u0000\u00ff\u0100", "\ud800", "\udc00", "a\ud83d\ude00b", "\r\n\u2028", "xy\ud800\udc00\ud800z"};
        for (String s:cases)for(Charset cs:new Charset[]{StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.ISO_8859_1})testText(s,cs);
        Random rng=new Random(312092L);
        for (int i=0;i<500;i++) {
            char[] s=new char[rng.nextInt(19000)];
            for(int j=0;j<s.length;j++)s[j]=(char)rng.nextInt(65536);
            testText(new String(s), StandardCharsets.UTF_8);
        }
        String markText="0123456789\ud83d\ude00".repeat(2500);
        StringReader sr=new StringReader(markText);
        check(sr.skip(7)==7,"skip");
        sr.mark(0);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(OutputStreamWriter w=new OutputStreamWriter(bytes,StandardCharsets.UTF_8)){
            check(sr.transferTo(w)==markText.length()-7,"marked transfer");
        }
        check(Arrays.equals(bytes.toByteArray(),markText.substring(7).getBytes(StandardCharsets.UTF_8)),"marked bytes");
        sr.reset(); check(sr.read() == markText.charAt(7),"reset preserved");
        sr.close();
        try {sr.transferTo(new OutputStreamWriter(new ByteArrayOutputStream())); throw new AssertionError("closed not rejected");}
        catch(IOException ex){checks++;}
        try {sr.transferTo(null); throw new AssertionError("null not rejected");}
        catch(NullPointerException ex){checks++;}
        System.out.println("M3_STRING_READER_TRANSFER_PASS checks="+checks);
    }
}
