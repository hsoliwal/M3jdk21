/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @bug 8369181
 * @summary Default-owned InflaterOutputStream rejects writes after finish with IOException;
 *          custom-owned Inflater and decompression parity are preserved.
 * @run main/othervm Jbs8369181Oracle
 * @run main/othervm -Xint Jbs8369181Oracle
 * @run main/othervm -Xcomp Jbs8369181Oracle
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

public final class Jbs8369181Oracle {
    static int checks;
    static void ok(boolean c, String message) { if(!c) throw new AssertionError(message); checks++; }
    static byte[] compress(byte[] plain) {
        Deflater d = new Deflater();
        try {
            d.setInput(plain); d.finish();
            byte[] compressed = new byte[plain.length * 2 + 100];
            int n = d.deflate(compressed);
            ok(d.finished(), "deflation finished");
            return Arrays.copyOf(compressed,n);
        } finally { d.end(); }
    }
    static void expectIOException(InflaterOutputStream out) throws Exception {
        try { out.write(new byte[]{0},0,1); throw new AssertionError("write accepted after finish"); }
        catch(IOException e) { checks++; }
    }
    public static void main(String... args) throws Exception {
        byte[] plain = "M3JDK21 backport proof for retained JNI/API/ABI".repeat(12).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] compressed = compress(plain);
        for (int n : new int[]{1, 2, 7, compressed.length}) {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            InflaterOutputStream out = new InflaterOutputStream(sink);
            for (int offset=0;offset<compressed.length;) {
                int span = Math.min(n,compressed.length-offset);
                out.write(compressed,offset,span);
                offset += span;
            }
            out.finish();
            ok(Arrays.equals(plain,sink.toByteArray()),"exact decompressed bytes");
            expectIOException(out);
            try { out.write(0); throw new AssertionError("int write accepted after finish"); }
            catch (IOException expected) { checks++; }
            try { out.write(new byte[0], 0, 0); throw new AssertionError("zero length write accepted after finish"); }
            catch (IOException expected) { checks++; }
            out.finish();
            out.close();
            out.close();
            try { out.write(compressed); throw new AssertionError("write accepted after close"); }
            catch (IOException expected) { ok("Stream closed".equals(expected.getMessage()), "closed exception contract"); }
            ok(Arrays.equals(plain,sink.toByteArray()),"close preserved bytes");
        }
        Inflater infl = new Inflater();
        try {
            var sink=new ByteArrayOutputStream();
            var out=new InflaterOutputStream(sink,infl);
            out.write(compressed);
            out.finish();
            ok(Arrays.equals(sink.toByteArray(),plain),"caller-owned inflater roundtrip");
            ok(infl.finished(),"caller-owned inflater left intact");
            out.close();
        } finally { infl.end(); }
        System.out.println("JBS8369181_PASS|checks="+checks);
    }
}
