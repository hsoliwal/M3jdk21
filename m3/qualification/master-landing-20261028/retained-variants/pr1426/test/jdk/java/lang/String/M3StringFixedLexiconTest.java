/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Fixed M3 lexicon admission preserves shared addresses, exact identity and strict refusal
 * @requires (os.family == "linux" | os.family == "windows") & vm.bits == "64"
 * @modules java.base/java.lang:open
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3StringFixedLexiconTest
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32C;

public final class M3StringFixedLexiconTest {
    private static final long BASE = 0x600000000000L;
    private static final String[] WORDS = {"Aa", "BB", "alpha", "delta", "caf\u00e9", "\u0000q", "\u03bb", "\ud800", "\ud83d\ude00", "\udfff"};
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 0) { child(args); return; }
        Path directory = Files.createTempDirectory("m3-fixed-lexicon-\u03bb-");
        int cases = 0;
        try {
            Path v1 = directory.resolve("words-v1-\u00e9.lex");
            Path v2 = directory.resolve("words-v2-\u03bb.lex");
            Path shared = directory.resolve("shared.synarr");
            Files.write(v1, image(1)); Files.write(v2, image(2)); Files.write(shared, sharedImage());
            cases += run("valid", v1, "0x600000000000", digest(v1), false);
            cases += run("valid", v2, "0x600000000000", digest(v2), false);
            cases += run("valid", shared, "0x600000000000", digest(shared), false);
            cases += run("collision", v2, "0x600000000000", digest(v2), false);
            cases += run("refuse", v2, "0x600000000001", digest(v2), false);
            cases += run("refuse", v2, "-65536", digest(v2), false);
            cases += run("refuse", v2, "not-an-address", digest(v2), false);
            cases += run("refuse", v2, "0x600000000000", null, false);
            cases += run("refuse", v2, "0x600000000000", "z".repeat(64), false);
            cases += run("refuse", v2, "0x600000000000", "0".repeat(64), false);
            Path corrupt = directory.resolve("bad-format.lex");
            byte[] invalid = Files.readAllBytes(v2); invalid[0] ^= 1; Files.write(corrupt, invalid);
            cases += run("refuse", corrupt, "0x600000000000", digest(corrupt), false);
            cases += run("default", v2, null, null, false);
            try {
                String.class.getDeclaredField("m3");
                cases += run("ordinary", v2, "0x600000000000", digest(v2), true);
            } catch (NoSuchFieldException externalBootJdk) {
                System.out.println("INTEGRATED_ORDINARY_STRING=REQUIRES_BUILT_M3JDK21");
            }
            System.out.println("PASS M3StringFixedLexiconTest childCases=" + cases);
        } finally {
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.delete(directory);
        }
    }

    private static int run(String mode, Path file, String address, String pin, boolean ordinary) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String option : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!option.startsWith("-Dm3.string.lexicon.") && !option.startsWith("-Djdk.mindex.lexicon=")) command.add(option);
        }
        command.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        command.add("-Xcheck:jni");
        if (address != null) command.add("-Dm3.string.lexicon.baseAddress=" + address);
        if (pin != null) command.add("-Dm3.string.lexicon.sha256=" + pin);
        if (ordinary) {
            command.add("-XX:+UnlockExperimentalVMOptions"); command.add("-XX:+UseM3StringStorage");
            command.add("-Djdk.mindex.lexicon=" + file);
        }
        command.add("-cp"); command.add(System.getProperty("java.class.path"));
        command.add(M3StringFixedLexiconTest.class.getName()); command.add(mode); command.add(file.toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly(); throw new AssertionError("Child timeout: " + mode);
        }
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.print(output);
        require(process.exitValue() == 0 && output.contains("CHILD_PASS " + mode), "child " + mode + " exit=" + process.exitValue());
        require(!output.contains("WARNING in native method") && !output.contains("FATAL ERROR in native method"), "checked JNI " + mode);
        return 1;
    }

    private static void child(String[] args) throws Exception {
        String library = System.getProperty("m3.test.native.library");
        if (library != null) Class.forName("java.lang.M3NativeTestLibrary").getMethod("load", String.class).invoke(null, library);
        String mode = args[0]; Path file = Path.of(args[1]);
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> lexiconType = Class.forName("java.lang.M3StringPool$Lexicon");
        Method initialize = method(pool, "initializeLexicon", String.class);
        Field active = field(pool, "lexicon");
        if (mode.equals("refuse")) {
            try { initialize.invoke(null, file.toString()); throw new AssertionError("strict configuration silently accepted"); }
            catch (InvocationTargetException expected) { require(expected.getCause() instanceof IllegalStateException, "strict exception"); }
            require(active.get(null) == null, "no fallback published");
            if (System.getProperty("os.name").equals("Linux")) require(mappingLine(BASE) == null, "failed admission released mapping");
        } else if (mode.equals("collision")) {
            Method open = method(lexiconType, "open", Path.class);
            Object first = open.invoke(null, file);
            try { open.invoke(null, file); throw new AssertionError("occupied address replaced"); }
            catch (InvocationTargetException expected) { require(expected.getCause() instanceof IOException, "collision refused"); }
            checkLexicon(first, true);
        } else {
            initialize.invoke(null, file.toString());
            Object lexicon = active.get(null); require(lexicon != null, "lexicon published");
            checkLexicon(lexicon, !mode.equals("default"));
            if (mode.equals("ordinary")) {
                Field m3 = field(String.class, "m3");
                Field owner = field(Class.forName("java.lang.M3String"), "owner");
                Method find = method(lexiconType, "find", char[].class, int.class, int.class);
                Method atom = method(lexiconType, "atom", int.class);
                for (String word : WORDS) {
                    String value = new String(word.toCharArray());
                    Object view = m3.get(value); require(view != null, "ordinary String has M3 storage");
                    int row = (Integer)find.invoke(lexicon, word.toCharArray(), 0, word.length());
                    require(owner.get(view) == atom.invoke(lexicon, row), "ordinary String shares canonical mapped owner");
                    require(value.equals(word) && value.hashCode() == word.hashCode(), "ordinary spelling and hash");
                    require(value.intern() == new String(word.toCharArray()).intern(), "String.intern identity");
                }
            }
        }
        System.out.println("CHILD_PASS " + mode + " checks=" + checks);
    }

    private static void checkLexicon(Object lexicon, boolean fixed) throws Exception {
        Class<?> type = lexicon.getClass();
        ByteBuffer map = (ByteBuffer)field(type, "mapping").get(lexicon);
        long base = field(type, "baseAddress").getLong(lexicon);
        require(map.isDirect() && map.isReadOnly(), "direct read-only mapping");
        if (fixed) require(base == BASE, "exact base address");
        try { map.put(0, (byte)0); throw new AssertionError("writable Java view"); }
        catch (ReadOnlyBufferException expected) { checks++; }
        Method find = method(type, "find", char[].class, int.class, int.class);
        Method atom = method(type, "atom", int.class);
        Class<?> atomType = Class.forName("java.lang.M3StringAtom");
        Method charAt = method(atomType, "charAt", int.class);
        Field address = field(atomType, "address");
        for (String word : WORDS) {
            char[] chars = word.toCharArray();
            int row = (Integer)find.invoke(lexicon, chars, 0, chars.length);
            require(row >= 0, "forward lookup"); Object value = atom.invoke(lexicon, row);
            require(value == atom.invoke(lexicon, row), "reverse identity");
            long payload = address.getLong(value);
            require(payload >= base && payload + chars.length * 2L <= base + map.capacity(), "mapped payload range");
            for (int i = 0; i < chars.length; i++) require((Character)charAt.invoke(value, i) == chars[i], "reverse UTF16");
        }
        require((Integer)find.invoke(lexicon, "missing".toCharArray(), 0, 7) == -1, "lookup miss");
        if (fixed && System.getProperty("os.name").equals("Linux")) {
            String line = mappingLine(base); require(line != null && line.contains("r--s"), "read-only shared OS mapping");
            System.out.println("FIXED_MAPPING " + line);
        }
    }

    private static String mappingLine(long address) throws IOException {
        String prefix = Long.toHexString(address) + "-";
        return Files.readAllLines(Path.of("/proc/self/maps")).stream().filter(line -> line.startsWith(prefix)).findFirst().orElse(null);
    }
    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method value = type.getDeclaredMethod(name, parameters); value.setAccessible(true); return value;
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field value = type.getDeclaredField(name); value.setAccessible(true); return value;
    }
    private static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static String digest(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }

    private static byte[] image(int version) throws Exception {
        String[] words = WORDS.clone(); Arrays.sort(words);
        int units = Arrays.stream(words).mapToInt(String::length).sum();
        int width = version == 1 ? 8 : 12; int payload = 64 + width * words.length;
        ByteBuffer map = ByteBuffer.allocate(payload + units * 2).order(ByteOrder.BIG_ENDIAN);
        map.putLong(0, 0x4d334c4558303031L); map.putInt(8, version); map.putInt(12, words.length);
        map.putLong(16, payload); map.putLong(24, units); int offset = 0;
        for (int i = 0; i < words.length; i++) {
            String word = words[i]; map.putInt(64 + i * width, offset); map.putInt(68 + i * width, word.length());
            if (version == 2) map.putInt(72 + i * width, word.hashCode());
            for (int j = 0; j < word.length(); j++) {
                char c = word.charAt(j); map.put(payload + offset * 2, (byte)c); map.put(payload + offset * 2 + 1, (byte)(c >>> 8)); offset++;
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(map.asReadOnlyBuffer().slice(0, 32)); digest.update(map.asReadOnlyBuffer().slice(64, map.capacity() - 64));
        map.put(32, digest.digest()); return map.array();
    }

    private static byte[] sharedImage() {
        int length = 64 + Arrays.stream(WORDS).mapToInt(word -> 88 + word.length() * 2).sum();
        ByteBuffer map = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        map.putLong(0, 0x53594e4152523031L); map.putInt(8, 1); map.putLong(16, 17); map.putLong(24, 29);
        map.putLong(32, length); map.putInt(40, WORDS.length);
        CRC32C crc = new CRC32C(); crc.update(map.asReadOnlyBuffer().slice(0, 56)); map.putLong(56, crc.getValue());
        int at = 64;
        for (String word : WORDS) {
            int payload = word.length() * 2; int total = 88 + payload;
            map.putInt(at, 0x41525231); map.putInt(at + 4, total); map.putInt(at + 8, 2);
            map.putInt(at + 16, word.length()); map.putInt(at + 20, -1); map.putInt(at + 24, word.hashCode()); map.putInt(at + 28, payload);
            for (int i = 0; i < word.length(); i++) map.putChar(at + 80 + i * 2, word.charAt(i));
            crc.reset(); crc.update(map.asReadOnlyBuffer().slice(at, 64)); crc.update(map.asReadOnlyBuffer().slice(at + 80, payload));
            map.putLong(at + 64, crc.getValue()); map.putLong(at + total - 8, 0x434f4d4d49543031L ^ at ^ total); at += total;
        }
        return map.array();
    }
}
