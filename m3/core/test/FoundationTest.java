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
    static int javaHash(String value){int hash=0;for(int i=0;i<value.length();i++)hash=31*hash+value.charAt(i);return hash;}
    static void writeV2(Path path,List<String> words)throws Exception{
        long units=0;for(String word:words)units=Math.addExact(units,word.length());
        int payload=64+12*words.size();ByteBuffer bytes=ByteBuffer.allocate(Math.toIntExact(payload+2*units)).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0,0x4d334c4558303031L).putInt(8,2).putInt(12,words.size()).putLong(16,payload).putLong(24,units);
        int offset=0;for(int row=0;row<words.size();row++){String word=words.get(row);int entry=64+12*row;
            bytes.putInt(entry,offset).putInt(entry+4,word.length()).putInt(entry+8,javaHash(word));
            for(int at=0;at<word.length();at++){char unit=word.charAt(at);int destination=payload+2*(offset+at);bytes.put(destination,(byte)unit).put(destination+1,(byte)(unit>>>8));}offset+=word.length();}
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");digest.update(bytes.array(),0,32);digest.update(bytes.array(),64,bytes.capacity()-64);System.arraycopy(digest.digest(),0,bytes.array(),32,32);Files.write(path,bytes.array(),StandardOpenOption.CREATE_NEW);
    }
    static void images(Path directory) throws Exception {
        Path image=directory.resolve("unicode.m3lex");List<String> words=List.of("", "a", "\u0100", "\ud800", "\ud83d\ude00", "\udc00");
        SharedLexiconImage.create(image,words);SharedLexiconImage one=SharedLexiconImage.open(image),two=SharedLexiconImage.open(image);one.warm();
        check(one.imageIdentity().equals(two.imageIdentity()));check(one.size()==words.size());
        LocalM3Arena arena=new LocalM3Arena();
        for(int i=0;i<words.size();i++)check(one.copyRecord(i,arena).flatten().equals(words.get(i)));
        Path versionTwo=directory.resolve("unicode-v2.m3lex");writeV2(versionTwo,words);SharedLexiconImage twoVersion=SharedLexiconImage.open(versionTwo);check(twoVersion.version()==2);
        for(int i=0;i<words.size();i++)check(twoVersion.copyRecord(i,arena).flatten().equals(words.get(i)));
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
    static String sha256(Path path) throws Exception {
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream input=Files.newInputStream(path)){byte[] buffer=new byte[4096];for(int read;(read=input.read(buffer))>=0;)if(read!=0)digest.update(buffer,0,read);}
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    static void catalog(Path directory) throws Exception {
        Path one=directory.resolve("one.m3lex"), two=directory.resolve("two.m3lex");
        writeV2(one,List.of("a","\ud800"));writeV2(two,List.of("\ud801","\ud802"));
        String header="shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n";
        String rows="0\tone.m3lex\ta\t\\uD800\t2\t2\t"+sha256(one)+"\n"
                +"1\ttwo.m3lex\t\\uD801\t\\uD802\t2\t2\t"+sha256(two)+"\n";
        Files.writeString(directory.resolve("synexia.shards.tsv"),header+rows,java.nio.charset.StandardCharsets.UTF_8);
        String mappingHeader="source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\n";
        String mappings=mappingHeader
                +"source-a\tpath-a\tkind-a\ten\tid-a\ta\t0\t0\tmap-a\tA\tprofile-a\tprofile-a\n"
                +"source-a2\tpath-a2\tkind-a2\ten\tid-a2\ta\t0\t0\tmap-a2\tA2\tprofile-a2\tprofile-a2\n"
                +"source-high\tpath-high\tkind-high\ten\tid-high\t\\uD800\t0\t1\tmap-high\tHIGH\tprofile-high\tprofile-high\n"
                +"source-high2\tpath-high2\tkind-high2\ten\tid-high2\t\\uD801\t1\t0\tmap-high2\tHIGH2\tprofile-high2\tprofile-high2\n"
                +"source-high3\tpath-high3\tkind-high3\ten\tid-high3\t\\uD802\t1\t1\tmap-high3\tHIGH3\tprofile-high3\tprofile-high3\n";
        Files.writeString(directory.resolve("synexia.records.tsv"),mappings,java.nio.charset.StandardCharsets.UTF_8);
        String facts="shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                +"0\t0\t1\t97\t1\t0\t0\tTrue\tTrue\tFalse\tprofile-a + profile-a2\n"
                +"0\t1\t1\t55296\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high\n"
                +"1\t0\t1\t55297\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high2\n"
                +"1\t1\t1\t55298\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high3\n";
        Files.writeString(directory.resolve("synexia.precompute.tsv"),facts,java.nio.charset.StandardCharsets.UTF_8);
        SharedLexiconCatalog catalog=SharedLexiconCatalog.open(directory);catalog.warm();
        check(catalog.shardCount()==2);check(catalog.recordCount()==4);check(catalog.shardFiles().equals(List.of("one.m3lex","two.m3lex")));
        check(catalog.mappingsAt(new SharedLexiconCatalog.Coordinate(0,0)).size()==2);
        check(catalog.mappingsAt(new SharedLexiconCatalog.Coordinate(0,0)).get(1).mappingName().equals("A2"));
        check(catalog.find("\ud801").equals(Optional.of(new SharedLexiconCatalog.Coordinate(1,0))));
        check(catalog.precomputeAt(new SharedLexiconCatalog.Coordinate(1,0)).javaHash()==55297L);
        check(catalog.find("missing").isEmpty());check(catalog.textAt(new SharedLexiconCatalog.Coordinate(1,1)).equals("\ud802"));
        Files.writeString(directory.resolve("synexia.shards.tsv"),header+"0\t../one.m3lex\ta\tb\t2\t2\t"+sha256(one)+"\n",java.nio.charset.StandardCharsets.UTF_8);
        expect(IOException.class,()->SharedLexiconCatalog.open(directory));
        Files.writeString(directory.resolve("synexia.shards.tsv"),header+rows,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.precompute.tsv"),facts.replace("55297", "1"),java.nio.charset.StandardCharsets.UTF_8);
        expect(IOException.class,()->SharedLexiconCatalog.open(directory));
        Files.writeString(directory.resolve("synexia.precompute.tsv"),facts.replace("profile-high3", "dropped-owner"),java.nio.charset.StandardCharsets.UTF_8);
        expect(IOException.class,()->SharedLexiconCatalog.open(directory));
    }
    static void exportedCatalog(Path directory) throws Exception {
        SharedLexiconCatalog catalog=SharedLexiconCatalog.open(directory);
        check(catalog.recordCount()==10004);check(catalog.shardCount()==1);
        SharedLexiconCatalog.Coordinate number=catalog.find("10000").orElseThrow();
        check(catalog.textAt(number).equals("10000"));
        check(catalog.mappingsAt(number).stream().anyMatch(mapping->mapping.mappingName().equals("NUMBER_10000")));
        check(catalog.precomputeAt(number).precomputeProfile().contains("NumberPrecompute"));
        SharedLexiconCatalog.Coordinate london=catalog.find("London").orElseThrow();
        check(catalog.mappingsAt(london).stream().anyMatch(mapping->mapping.translationProfile().equals("en->hi")));
    }
    public static void main(String[] args)throws Exception{pieces();Path dir=Files.createTempDirectory("m3-foundation-");try{images(dir);catalog(dir);if(args.length==1)exportedCatalog(Path.of(args[0]));}finally{try(var paths=Files.list(dir)){for(Path path:paths.toList())Files.delete(path);}Files.delete(dir);}System.out.println("FOUNDATION_PASS checks="+checks);}
}
