/* Copyright 2026 Synexia Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary M3LEX image row coordinates survive a second JVM; mapped bytes still require publication checks
 * @requires (os.family == "linux")
 * @modules java.base/jdk.m3.text
 * @run main/othervm M3PortableAtomValueTest
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.SharedLexiconImage;

/** Portable value identity only: no shared Java object or immutable-file guarantee. */
public final class M3PortableAtomValueTest {
    private static int checks;
    private static void ok(boolean value, String why) {
        if (!value) throw new AssertionError(why);
        checks++;
    }
    private static void reject(Runnable callback) {
        boolean refused = false;
        try { callback.run(); }
        catch (IllegalArgumentException | IndexOutOfBoundsException expected) { refused = true; }
        ok(refused, "invalid image coordinate refused");
    }
    private static byte[] encodeV2(List<String> words) throws Exception {
        int units = 0;
        for (String word : words) units = Math.addExact(units, word.length());
        int payload = 64 + 12 * words.size();
        ByteBuffer data = ByteBuffer.allocate(payload + 2 * units).order(ByteOrder.BIG_ENDIAN);
        data.putLong(0, 0x4d334c4558303031L);
        data.putInt(8, 2); data.putInt(12, words.size());
        data.putLong(16, payload); data.putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            int directory = 64 + 12 * row;
            data.putInt(directory, offset);
            data.putInt(directory + 4, word.length());
            data.putInt(directory + 8, word.hashCode());
            for (int i = 0; i < word.length(); i++) {
                int byteOffset = payload + 2 * (offset + i);
                char c = word.charAt(i);
                data.put(byteOffset, (byte)c);
                data.put(byteOffset + 1, (byte)(c >>> 8));
            }
            offset += word.length();
        }
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(data.asReadOnlyBuffer().slice(0, 32));
        hash.update(data.asReadOnlyBuffer().slice(64, data.limit() - 64));
        data.put(32, hash.digest());
        return data.array();
    }
    private static void inChild(Path file, String wire) throws Exception {
        SharedLexiconImage image = SharedLexiconImage.open(file);
        var coordinate = SharedLexiconImage.AtomCoordinate.parse(wire);
        ok(coordinate.equals(image.atomCoordinate(1)), "same value in second JVM");
        ok(coordinate.matchesSnapshotMetadata(image), "same opened image metadata");
        ok("beta".equals(image.copyRecord(1, new LocalM3Arena()).toString()),
                "local copy from shared image");
        System.out.println("M3LEX_CHILD_ATOM_ID_PASS|checks=" + checks);
    }
    private static void child(Path image, String wire) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        String patch = System.getProperty("m3lex.test.patch", "");
        if (!patch.isEmpty()) { command.add("--patch-module"); command.add("java.base=" + patch); }
        command.add("--add-exports"); command.add("java.base/jdk.m3.text=ALL-UNNAMED");
        command.add("-cp"); command.add(System.getProperty("java.class.path"));
        command.add(M3PortableAtomValueTest.class.getName());
        command.add("--child"); command.add(image.toString()); command.add(wire);
        Process process = new ProcessBuilder(command).inheritIO().start();
        ok(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0,
                "second JVM verified image key");
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && "--child".equals(args[0])) {
            inChild(Path.of(args[1]), args[2]); return;
        }
        if (args.length != 0) throw new IllegalArgumentException("unexpected arguments");
        Path dir = Files.createTempDirectory("m3lex-portable-id-");
        try {
            List<String> words = List.of("alpha", "beta", "omega", "zulu");
            Path first = dir.resolve("words-v1.m3lex");
            SharedLexiconImage.create(first, words);
            var v1 = SharedLexiconImage.open(first);
            var id = v1.atomCoordinate(1);
            String wire = id.externalForm();
            ok(wire.equals(SharedLexiconImage.AtomCoordinate.parse(wire).externalForm()),
                    "canonical parse/serialize round trip");
            ok(id.imageDigest().equals(v1.imageIdentity()), "256-bit content digest");
            reject(() -> SharedLexiconImage.AtomCoordinate.parse(wire + "x"));
            reject(() -> SharedLexiconImage.AtomCoordinate.parse(wire.replace(":1", ":01")));
            reject(() -> v1.atomCoordinate(v1.size()));
            child(first, wire);
            Path duplicate = dir.resolve("byte-identical-copy.m3lex");
            Files.copy(first, duplicate);
            ok(id.equals(SharedLexiconImage.open(duplicate).atomCoordinate(1)),
                    "same image bytes, different path, same value");
            Path second = dir.resolve("words-v2.m3lex");
            Files.write(second, encodeV2(words));
            var v2 = SharedLexiconImage.open(second);
            var v2Id = v2.atomCoordinate(1);
            ok(v2Id.imageVersion() == 2 && !v2Id.equals(id), "version-qualified key");
            child(second, v2Id.externalForm());

            var arena = new LocalM3Arena();
            var published = v1.copyRecord(1, arena);
            int payload = 64 + 8 * words.size();
            try (FileChannel writer = FileChannel.open(first, StandardOpenOption.WRITE)) {
                writer.write(ByteBuffer.wrap(new byte[]{(byte)'c'}), payload + 2 * words.get(0).length());
                writer.force(true);
            }
            ok(v1.recordEquals(1, "ceta"), "read-only mapping is not a file seal");
            boolean refused = false;
            try { v1.copyRecord(1, arena); }
            catch (IOException expected) { refused = true; }
            ok(refused, "corrupted bytes refused at publication");
            ok("beta".equals(published.toString()), "previous local copy unchanged");
            boolean badChecksum = false;
            try { SharedLexiconImage.open(first); }
            catch (IOException expected) { badChecksum = true; }
            ok(badChecksum, "new image opening rejects modified source");
            System.out.println("M3LEX_PORTABLE_ATOM_VALUE_PASS|checks=" + checks);
        } finally {
            try (var stream = Files.list(dir)) {
                for (Path path : stream.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(dir);
        }
    }
}
