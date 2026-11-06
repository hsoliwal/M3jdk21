// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Dependency-free finite unit oracle, executable against real JNI or injected test stubs. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/corpus-test/java/com/synexia/m3/pmem/PmemCorpusAtomUnitTest.java blob a40b1e04c918; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class M3PmemCorpusAtomUnitTest {
    private M3PmemCorpusAtomUnitTest() { }
    static int passed;
    private static void yes(boolean condition) {
        if (!condition) throw new AssertionError("atom oracle failed #" + passed);
        passed++;
    }
    private static String digest(byte[] raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
    }
    private static void refused(Throwing candidate) throws Exception {
        try { candidate.run(); throw new AssertionError("expected refusal"); }
        catch (IllegalArgumentException | IOException expected) { passed++; }
    }
    interface Throwing { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("pmem-atom-test-");
        try {
            byte[] raw = new byte[150_123];
            for (int i = 0; i < raw.length; i++) raw[i] = (byte) (i * 17 + 13);
            Path file = root.resolve("source.java");
            Files.write(file, raw);
            String hash = digest(raw);
            for (int[] range : new int[][] {{0,0},{0,1},{0,raw.length},
                    {65535,8},{65533,65545},{raw.length-1,1},{raw.length,0}}) {
                var atom = M3PmemCorpusAtom.read(root,"source.java",raw.length,hash,range[0],range[1]);
                yes(Arrays.equals(atom.bytes(), Arrays.copyOfRange(raw,range[0],range[0]+range[1])));
                yes(atom.end() == range[0] + range[1]);
                yes(atom.atomSha256().equals(digest(atom.bytes())));
            }
            refused(() -> M3PmemCorpusAtom.read(root,"../source.java",raw.length,hash,0,1));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java/../source.java",raw.length,hash,0,1));
            refused(() -> M3PmemCorpusAtom.read(root,"/source.java",raw.length,hash,0,1));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length,hash,Long.MAX_VALUE,1));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length,hash,0,1_048_577));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length+1,hash,0,1));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length,"0".repeat(64),0,1));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length,"BAD",0,1));
            Files.write(file, "changed".getBytes(StandardCharsets.UTF_8));
            refused(() -> M3PmemCorpusAtom.read(root,"source.java",raw.length,hash,0,1));
            Path empty = root.resolve("empty.java"); Files.write(empty,new byte[0]);
            var zero = M3PmemCorpusAtom.read(root,"empty.java",0,digest(new byte[0]),0,0);
            yes(zero.bytes().length==0);
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                Path alias=root.resolve("alias.java"); Files.createSymbolicLink(alias,file);
                refused(() -> M3PmemCorpusAtom.read(root,"alias.java",7,digest("changed".getBytes(StandardCharsets.UTF_8)),0,1));
                Files.delete(alias);
            }
            System.out.println("PASS PMEM_CORPUS_ATOM_UNIT checks=" + passed);
        } finally {
            try (var paths=Files.list(root)) {
                for (Path path:paths.toList()) Files.delete(path);
            }
            Files.delete(root);
        }
    }
}
