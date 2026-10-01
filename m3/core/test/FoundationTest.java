/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class FoundationTest {
    static int checks;
    static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("check " + checks); }
    interface Throwing { void run() throws Exception; }
    static void expect(Class<? extends Throwable> kind, Throwing body) throws Exception {
        try { body.run(); throw new AssertionError("missing " + kind); }
        catch (Throwable failure) { check(kind.isInstance(failure)); }
    }
    static void pieces() throws Exception {
        LocalM3Arena arena = new LocalM3Arena();Random random = new Random(210035);
        for (int test = 0; test < 1000; test++) {
            char[] input = new char[random.nextInt(65)];
            for (int i = 0; i < input.length; i++) input[i] = (char)random.nextInt(65536);
            String expected = new String(input);
            LocalM3StringPiece piece = arena.copyUtf16(input);
            Arrays.fill(input, 'x');check(piece.flatten().equals(expected));
            int split = random.nextInt(expected.length() + 1);
            LocalM3StringPiece left = piece.subSequence(0, split), right = piece.subSequence(split, piece.length());
            check(left.storageIdentity().equals(right.storageIdentity()));check(right.codeUnitOffset() == split);
            M3StringPiece[] directory = {left, right};M3StringPiece joined = M3StringPiece.join(directory);
            directory[0] = arena.copyUtf16(new char[]{'!'});check(joined.flatten().equals(expected));
            for (int i = 0; i < expected.length(); i++) check(joined.charAt(i) == expected.charAt(i));
            for (int i = 0; i <= expected.length(); i++) check(joined.subSequence(i, expected.length()).flatten().equals(expected.substring(i)));
            check(!arena.copyUtf16(expected.toCharArray()).storageIdentity().equals(piece.storageIdentity()));
        }
        byte[] all = new byte[256];for(int i=0;i<all.length;i++)all[i]=(byte)i;
        LocalM3StringPiece latin = arena.copyLatin1(all);Arrays.fill(all,(byte)0);
        for(int i=0;i<256;i++)check(latin.charAt(i)==i);
        char[] malformed = {'\ud800','a','\udc00','\u0000','\uffff'};
        check(arena.copyUtf16(malformed).flatten().equals(new String(malformed)));
        M3StringPiece chain = latin;M3StringPiece empty = M3StringPiece.join();
        for(int i=0;i<10000;i++)chain=M3StringPiece.join(empty,chain,empty);
        check(chain.flatten().equals(latin.flatten()));
        expect(IndexOutOfBoundsException.class,()->latin.charAt(-1));
        expect(IndexOutOfBoundsException.class,()->latin.charAt(256));
        expect(IndexOutOfBoundsException.class,()->chainIndex());
        expect(NullPointerException.class,()->M3StringPiece.join((M3StringPiece)null));
        expect(IndexOutOfBoundsException.class,()->latin.subSequence(2,1));
        ExecutorService executor=Executors.newFixedThreadPool(8);
        List<Future<Set<StorageIdentity>>> results=new ArrayList<>();
        for(int t=0;t<8;t++)results.add(executor.submit(()->{Set<StorageIdentity> ids=new HashSet<>();for(int i=0;i<1000;i++)ids.add(arena.copyUtf16(new char[]{'x'}).storageIdentity());return ids;}));
        Set<StorageIdentity> ids=new HashSet<>();for(Future<Set<StorageIdentity>> result:results)check(ids.addAll(result.get()));executor.shutdown();check(ids.size()==8000);
    }
    static void chainIndex(){M3StringPiece.join().charAt(0);}
    static void images(Path directory) throws Exception {
        Path image=directory.resolve("unicode.m3lex");List<String> words=List.of("", "a", "\u0100", "\ud800", "\udc00", "\ud83d\ude00");
        SharedLexiconImage.create(image,words);SharedLexiconImage one=SharedLexiconImage.open(image),two=SharedLexiconImage.open(image);one.warm();
        check(one.imageIdentity().equals(two.imageIdentity()));check(one.size()==words.size());
        LocalM3Arena arena=new LocalM3Arena();
        for(int i=0;i<words.size();i++)check(one.copyRecord(i,arena).flatten().equals(words.get(i)));
        LocalM3StringPiece saved=one.copyRecord(1,arena);
        // A dimension reinterpretation must not retain the same valid image identity.
        Path changedHeader=directory.resolve("changed-header");byte[] headerBytes=Files.readAllBytes(image);
        ByteBuffer.wrap(headerBytes).putInt(12,0).putLong(16,64).putLong(24,(headerBytes.length-64)/2);
        Files.write(changedHeader,headerBytes);expect(IOException.class,()->SharedLexiconImage.open(changedHeader));
        expect(FileAlreadyExistsException.class,()->SharedLexiconImage.create(image,words));
        expect(IndexOutOfBoundsException.class,()->one.copyRecord(-1,arena));
        // An external file mutation must not alter an already-published local piece.
        try(FileChannel channel=FileChannel.open(image,StandardOpenOption.WRITE)){channel.write(ByteBuffer.wrap(new byte[]{99}),64+8L*words.size());channel.force(true);}
        check(saved.flatten().equals("a"));expect(IOException.class,()->one.copyRecord(1,arena));expect(IOException.class,()->SharedLexiconImage.open(image));
        Path tiny=directory.resolve("tiny");Files.write(tiny,new byte[3]);expect(IOException.class,()->SharedLexiconImage.open(tiny));
        expect(IOException.class,()->SharedLexiconImage.open(directory.resolve("missing-shard")));
        // Explicit local fallback after optional image failure.
        check(arena.copyUtf16("fallback".toCharArray()).flatten().equals("fallback"));
        Path largeCount=directory.resolve("bad-count");byte[] bytes=Files.readAllBytes(image);ByteBuffer.wrap(bytes).putInt(12,Integer.MAX_VALUE);Files.write(largeCount,bytes);expect(IOException.class,()->SharedLexiconImage.open(largeCount));
    }
    public static void main(String[] args)throws Exception{pieces();Path dir=Files.createTempDirectory("m3-foundation-");try{images(dir);}finally{try(var paths=Files.list(dir)){for(Path path:paths.toList())Files.delete(path);}Files.delete(dir);}System.out.println("FOUNDATION_PASS checks="+checks);}
}
