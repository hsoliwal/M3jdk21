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
        Files.write(path,prefixImageBytes(words),StandardOpenOption.CREATE_NEW);
    }
    static byte[] prefixImageBytes(List<String> words) throws Exception {
        long units=0;for(String word:words)units=Math.addExact(units,word.length());
        int payload=64+12*words.size();ByteBuffer bytes=ByteBuffer.allocate(Math.toIntExact(payload+2*units)).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0,0x4d334c4558303031L).putInt(8,2).putInt(12,words.size()).putLong(16,payload).putLong(24,units);
        int offset=0;for(int row=0;row<words.size();row++){String word=words.get(row);int entry=64+12*row;
            bytes.putInt(entry,offset).putInt(entry+4,word.length()).putInt(entry+8,javaHash(word));
            for(int at=0;at<word.length();at++){char unit=word.charAt(at);int destination=payload+2*(offset+at);bytes.put(destination,(byte)unit).put(destination+1,(byte)(unit>>>8));}offset+=word.length();}
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");digest.update(bytes.array(),0,32);digest.update(bytes.array(),64,bytes.capacity()-64);System.arraycopy(digest.digest(),0,bytes.array(),32,32);return bytes.array();
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
    static String profileHash(String profile,long sourceRecords,long imageRecords)throws Exception{
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        digest.update((profile+"\t"+sourceRecords+"\t"+imageRecords+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    static void catalog(Path directory) throws Exception {
        Path one=directory.resolve("one.m3lex"), two=directory.resolve("two.m3lex");
        writeV2(one,List.of("a","\ud800"));writeV2(two,List.of("\ud801","\ud802"));
        String header="shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n";
        String rows="0\tone.m3lex\ta\t\\uD800\t2\t2\t"+sha256(one)+"\n"
                +"1\ttwo.m3lex\t\\uD801\t\\uD802\t2\t2\t"+sha256(two)+"\n";
        Files.writeString(directory.resolve("synexia.shards.tsv"),header+rows,java.nio.charset.StandardCharsets.UTF_8);
        String mappingHeader="source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n";
        String mappings=mappingHeader
                +"source-a\tpath-a\tkind-a\ten\tid-a\ta\t0\t0\tmap-a\tA\tprofile-a\tprofile-a\t{}\n"
                +"source-a2\tpath-a2\tkind-a2\ten\tid-a2\ta\t0\t0\tmap-a2\tA2\tprofile-a2\tprofile-a2\t{\"frequency_rank\":1}\n"
                +"source-high\tpath-high\tkind-high\ten\tid-high\t\\uD800\t0\t1\tmap-high\tHIGH\tprofile-high\tprofile-high\t{}\n"
                +"source-high2\tpath-high2\tkind-high2\ten\tid-high2\t\\uD801\t1\t0\tmap-high2\tHIGH2\tprofile-high2\tprofile-high2\t{}\n"
                +"source-high3\tpath-high3\tkind-high3\ten\tid-high3\t\\uD802\t1\t1\tmap-high3\tHIGH3\tprofile-high3\tprofile-high3\t{}\n";
        Files.writeString(directory.resolve("synexia.records.tsv"),mappings,java.nio.charset.StandardCharsets.UTF_8);
        String facts="shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n"
                +"0\t0\t1\t97\t1\t0\t0\tTrue\tTrue\tFalse\tprofile-a + profile-a2\n"
                +"0\t1\t1\t55296\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high\n"
                +"1\t0\t1\t55297\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high2\n"
                +"1\t1\t1\t55298\t1\t1\t0\tFalse\tFalse\tFalse\tprofile-high3\n";
        Files.writeString(directory.resolve("synexia.precompute.tsv"),facts,java.nio.charset.StandardCharsets.UTF_8);
        String profileHeader="precompute_profile\tsource_records\timage_records\tsha256\n";
        String profiles=profileHeader
                +"profile-a\t1\t1\t"+profileHash("profile-a",1,1)+"\n"
                +"profile-a2\t1\t1\t"+profileHash("profile-a2",1,1)+"\n"
                +"profile-high\t1\t1\t"+profileHash("profile-high",1,1)+"\n"
                +"profile-high2\t1\t1\t"+profileHash("profile-high2",1,1)+"\n"
                +"profile-high3\t1\t1\t"+profileHash("profile-high3",1,1)+"\n";
        Files.writeString(directory.resolve("synexia.precompute-index.tsv"),profiles,java.nio.charset.StandardCharsets.UTF_8);
        SharedLexiconCatalog catalog=SharedLexiconCatalog.open(directory);catalog.warm();
        check(catalog.shardCount()==2);check(catalog.recordCount()==4);check(catalog.shardFiles().equals(List.of("one.m3lex","two.m3lex")));
        check(catalog.precomputeProfiles().size()==5);
        check(catalog.precomputeProfiles().get(0).profile().equals("profile-a"));
        check(catalog.mappingsAt(new SharedLexiconCatalog.Coordinate(0,0)).size()==2);
        check(catalog.mappingsAt(new SharedLexiconCatalog.Coordinate(0,0)).get(1).mappingName().equals("A2"));
        check(catalog.mappingsAt(new SharedLexiconCatalog.Coordinate(0,0)).get(1).precomputePayload().equals("{\"frequency_rank\":1}"));
        check(catalog.findMapping("source-a2","id-a2").orElseThrow().mappingName().equals("A2"));
        check(catalog.findMappings("a").size()==2);check(catalog.findMappings("missing").isEmpty());
        check(catalog.prefix("a",10).equals(List.of(new SharedLexiconCatalog.Coordinate(0,0))));
        expect(IllegalArgumentException.class,()->catalog.prefix("",1));
        check(catalog.find("\ud801").equals(Optional.of(new SharedLexiconCatalog.Coordinate(1,0))));
        check(catalog.precomputeAt(new SharedLexiconCatalog.Coordinate(1,0)).javaHash()==55297L);
        check(catalog.find("missing").isEmpty());check(catalog.textAt(new SharedLexiconCatalog.Coordinate(1,1)).equals("\ud802"));
        Path lazyDirectory=Files.createTempDirectory("m3-lazy-");
        for(String file:List.of("one.m3lex","two.m3lex","synexia.shards.tsv",
                "synexia.records.tsv","synexia.precompute.tsv","synexia.precompute-index.tsv"))
            Files.copy(directory.resolve(file),lazyDirectory.resolve(file));
        SharedLexiconCatalog lazy=SharedLexiconCatalog.openLazy(lazyDirectory);
        check(lazy.find("a").equals(Optional.of(new SharedLexiconCatalog.Coordinate(0,0))));
        Path lazyTwo=lazyDirectory.resolve("two.m3lex"), repaired=lazyDirectory.resolve("two.repaired");
        Files.copy(lazyTwo,repaired);
        byte[] damaged=Files.readAllBytes(lazyTwo);damaged[0]^=1;Files.write(lazyTwo,damaged);
        SharedLexiconCatalog retry=SharedLexiconCatalog.openLazy(lazyDirectory);
        check(retry.find("a").equals(Optional.of(new SharedLexiconCatalog.Coordinate(0,0))));
        check(retry.prefix("a",1).equals(List.of(new SharedLexiconCatalog.Coordinate(0,0))));
        check(retry.prefix("a",2).equals(List.of(new SharedLexiconCatalog.Coordinate(0,0))));
        check(retry.prefix("b",2).isEmpty());
        expect(java.io.UncheckedIOException.class,()->retry.prefix("\ud801",1));
        expect(java.io.UncheckedIOException.class,()->retry.find("\ud801"));
        Files.move(repaired,lazyTwo,StandardCopyOption.REPLACE_EXISTING);
        check(retry.find("\ud801").equals(Optional.of(new SharedLexiconCatalog.Coordinate(1,0))));
        check(retry.prefix("\ud801",1).equals(List.of(new SharedLexiconCatalog.Coordinate(1,0))));
        retry.close();expect(IllegalStateException.class,()->retry.find("a"));
        expect(IllegalStateException.class,()->retry.prefix("a",1));
        expect(IllegalStateException.class,()->retry.precomputeAt(new SharedLexiconCatalog.Coordinate(0,0)));
        lazy.close();expect(IllegalStateException.class,()->lazy.textAt(new SharedLexiconCatalog.Coordinate(0,0)));
        try(var paths=Files.list(lazyDirectory)){for(Path path:paths.toList())Files.delete(path);}Files.delete(lazyDirectory);
        String overlapMappings=mappings.replace("profile-high2\tprofile-high2\t{}\n",
                "profile-high\tprofile-high\t{}\n");
        String overlapProfiles=profiles
                .replace("profile-high\t1\t1\t"+profileHash("profile-high",1,1)+"\n",
                        "profile-high\t2\t2\t"+profileHash("profile-high",2,2)+"\n")
                .replace("profile-high2\t1\t1\t"+profileHash("profile-high2",1,1)+"\n", "");
        Files.writeString(directory.resolve("synexia.records.tsv"),overlapMappings,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.precompute-index.tsv"),overlapProfiles,java.nio.charset.StandardCharsets.UTF_8);
        expect(IOException.class,()->SharedLexiconCatalog.open(directory));
        Files.writeString(directory.resolve("synexia.records.tsv"),mappings,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("synexia.precompute-index.tsv"),profiles,java.nio.charset.StandardCharsets.UTF_8);
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
        check(catalog.findMapping("numbers","10000").orElseThrow().mappingName().equals("NUMBER_10000"));
        check(catalog.mappingsAt(number).stream().anyMatch(mapping->mapping.precomputePayload().contains("\"frequency_rank\":10000")));
        check(catalog.precomputeAt(number).precomputeProfile().contains("NumberPrecompute"));
        SharedLexiconCatalog.Coordinate london=catalog.find("London").orElseThrow();
        check(catalog.mappingsAt(london).stream().anyMatch(mapping->mapping.translationProfile().equals("en->hi")));
        check(catalog.mappingsAt(london).stream().anyMatch(mapping->mapping.precomputePayload().contains("\"phonetic_id\":13")));
        check(catalog.mappingsAt(london).stream().anyMatch(mapping->mapping.precomputePayload().contains("\"sim_hash64\":19")));
        List<SharedLexiconCatalog.Coordinate> hundreds=catalog.prefix("100",3);
        check(hundreds.size()==3);check(catalog.textAt(hundreds.get(0)).equals("100"));
        check(catalog.textAt(hundreds.get(1)).equals("1000"));check(catalog.textAt(hundreds.get(2)).equals("10000"));
    }
    static String prefixEscaped(String value) {
        StringBuilder encoded=new StringBuilder();
        for(int i=0;i<value.length();i++){
            encoded.append("\\u");
            String hex=Integer.toHexString(value.charAt(i));
            encoded.append("0".repeat(4-hex.length())).append(hex);
        }
        return encoded.toString();
    }
    static void writePrefixFixture(Path root,List<List<String>> shards,boolean onlyLast) throws Exception {
        StringBuilder manifest=new StringBuilder("shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256\n");
        StringBuilder mappings=new StringBuilder("source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\tprecompute_profile\tprecompute_payload\n");
        StringBuilder facts=new StringBuilder("shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\tunpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile\n");
        long records=0;
        for(int shard=0;shard<shards.size();shard++){
            List<String> words=shards.get(shard);String file="part-"+shard+".m3lex";
            boolean physical=!onlyLast||shard==shards.size()-1;
            if(physical)writeV2(root.resolve(file),words);
            long units=words.stream().mapToLong(String::length).sum();
            manifest.append(shard).append('\t').append(file).append('\t')
                    .append(prefixEscaped(words.getFirst())).append('\t').append(prefixEscaped(words.getLast()))
                    .append('\t').append(words.size()).append('\t').append(units).append('\t')
                    .append(physical?sha256(root.resolve(file)):java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(prefixImageBytes(words)))).append('\n');
            for(int row=0;row<words.size();row++){
                String word=words.get(row),id=shard+"-"+row;
                mappings.append("prefix-fixture\tfixture\tfixture\tund\t").append(id).append('\t')
                        .append(prefixEscaped(word)).append('\t').append(shard).append('\t').append(row)
                        .append("\tmap-").append(id).append("\tROW_").append(id).append("\t-\tprefix-proof\t{}\n");
                int nonBmp=(int)word.codePoints().filter(cp->cp>0xffff).count();
                int surrogateUnits=(int)word.chars().filter(c->Character.isSurrogate((char)c)).count();
                facts.append(shard).append('\t').append(row).append('\t').append(word.length()).append('\t')
                        .append(Integer.toUnsignedString(javaHash(word))).append('\t')
                        .append(word.codePointCount(0,word.length())).append('\t')
                        .append(surrogateUnits-2*nonBmp).append('\t').append(nonBmp).append('\t')
                        .append(word.chars().allMatch(c->c<128)?"True":"False").append('\t')
                        .append(word.chars().allMatch(c->c<256)?"True":"False").append('\t')
                        .append(word.codePoints().anyMatch(Character::isWhitespace)?"True":"False")
                        .append("\tprefix-proof\n");
                records++;
            }
        }
        Files.writeString(root.resolve("synexia.shards.tsv"),manifest,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(root.resolve("synexia.records.tsv"),mappings,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(root.resolve("synexia.precompute.tsv"),facts,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(root.resolve("synexia.precompute-index.tsv"),
                "precompute_profile\tsource_records\timage_records\tsha256\n"
                        +"prefix-proof\t"+records+"\t"+records+"\t"+profileHash("prefix-proof",records,records)+"\n",
                java.nio.charset.StandardCharsets.UTF_8);
    }
    static List<SharedLexiconCatalog.Coordinate> prefixOracle(List<List<String>> shards,String query,int limit) {
        List<SharedLexiconCatalog.Coordinate> result=new ArrayList<>();
        for(int s=0;s<shards.size();s++)for(int r=0;r<shards.get(s).size()&&result.size()<limit;r++)
            if(shards.get(s).get(r).startsWith(query))result.add(new SharedLexiconCatalog.Coordinate(s,r));
        return List.copyOf(result);
    }
    static void deletePrefixFixture(Path root) throws IOException {
        try(var files=Files.list(root)){for(Path path:files.toList())Files.delete(path);}Files.delete(root);
    }
    static void assertPrefixFixture(List<List<String>> shards,List<String> queries) throws Exception {
        Path root=Files.createTempDirectory("m3-prefix-matrix-");
        try{
            writePrefixFixture(root,shards,false);
            for(boolean lazy:List.of(false,true)){
                SharedLexiconCatalog catalog=lazy?SharedLexiconCatalog.openLazy(root):SharedLexiconCatalog.open(root);
                try{
                    for(String query:queries)for(int limit:new int[]{1,2,3,5,6,100000})
                        check(catalog.prefix(query,limit).equals(prefixOracle(shards,query,limit)));
                    expect(IllegalArgumentException.class,()->catalog.prefix(null,1));
                    expect(IllegalArgumentException.class,()->catalog.prefix("",1));
                    for(int limit:new int[]{-1,0,100001})
                        expect(IllegalArgumentException.class,()->catalog.prefix(queries.getFirst(),limit));
                    expect(UnsupportedOperationException.class,()->catalog.prefix(queries.getFirst(),1)
                            .add(new SharedLexiconCatalog.Coordinate(0,0)));
                }finally{catalog.close();}
            }
        }finally{deletePrefixFixture(root);}
    }
    static void prefixRouting() throws Exception {
        int before=checks;
        assertPrefixFixture(List.of(List.of("a","ab"),List.of("aba","abb"),List.of("abc","abz"),List.of("b","ba")),
                List.of("ab","aba","abx","ac","0","z","b","ba","a"));
        String high=String.valueOf((char)0xd800),low=String.valueOf((char)0xdc00);
        String ff=String.valueOf((char)0xffff),nul=String.valueOf((char)0);
        assertPrefixFixture(List.of(List.of(high,high+"a"),List.of(high+low,high+ff),List.of(low),
                        List.of(ff,ff+nul),List.of(ff+ff,ff+ff+"x")),
                List.of(high,high+low,low,String.valueOf((char)0xd801),ff,ff+nul,ff+ff,ff+ff+ff));
        assertPrefixFixture(List.of(List.of("ab","ac"),List.of("b")),List.of("a","ab"));
        for(int count:new int[]{16,256,1024}){
            List<List<String>> shards=new ArrayList<>();
            for(int s=0;s<count;s++){
                String key="k"+String.format(java.util.Locale.ROOT,"%04d",s);shards.add(List.of(key,key+"x"));
            }
            Path root=Files.createTempDirectory("m3-prefix-many-");
            try{
                // Missing nonselected images make any accidental first touch observable.
                writePrefixFixture(root,shards,true);
                check(!Files.exists(root.resolve("part-0.m3lex")));
                SharedLexiconCatalog catalog=SharedLexiconCatalog.openLazy(root);
                try{
                    check(catalog.recordCount()==2L*count);
                    String last=shards.getLast().getFirst();
                    check(catalog.prefix(last,1).equals(List.of(new SharedLexiconCatalog.Coordinate(count-1,0))));
                    check(catalog.prefix("z",1).isEmpty());
                    check(catalog.prefix("0",1).isEmpty());
                    expect(java.io.UncheckedIOException.class,()->catalog.prefix("k0000",1));
                }finally{catalog.close();}
            }finally{deletePrefixFixture(root);}
        }
        if(checks-before!=282)throw new AssertionError("prefix coverage count");
        System.out.println("M3_PREFIX_SHARD_MATRIX_PASS checks="+(checks-before)
                +" healthyFixtures=3 prefixCases=228 manyShardFixtures=3");
    }
    public static void main(String[] args)throws Exception{pieces();Path dir=Files.createTempDirectory("m3-foundation-");try{images(dir);catalog(dir);prefixRouting();if(args.length==1)exportedCatalog(Path.of(args[0]));}finally{try(var paths=Files.list(dir)){for(Path path:paths.toList())Files.delete(path);}Files.delete(dir);}System.out.println("FOUNDATION_PASS checks="+checks);}
}
