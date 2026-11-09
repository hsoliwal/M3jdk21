/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.cds;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.SourceVersion;

/** Exact JDK, application and command-argument identity for a JDK21 CDS run. */
public final class CdsIdentity {
    /** Canonical receipt contract identifier. */
    public static final String SCHEMA = "M3JDK21-CDS-TRAIN-REPLAY/1";

    private CdsIdentity() { }

    /**
     * Creates an immutable, ordered plan for an existing JDK21 and application JAR.
     * Each input is hashed before any child JVM can execute.
     *
     * @param java the installed JDK21 launcher (not an application wrapper)
     * @param jar one stable application JAR; class directories are rejected
     * @param mainClass the application main class name
     * @param arguments exact noninteractive application argument vector
     * @return input identity and replayable process arguments
     * @throws IOException if a required source image file cannot be read
     */
    public static Plan inspect(Path java, Path jar, String mainClass, List<String> arguments)
            throws IOException {
        if (java == null || jar == null || mainClass == null || arguments == null) {
            throw new IllegalArgumentException("null CDS input");
        }
        if (!SourceVersion.isName(mainClass) || mainClass.startsWith("-")) {
            throw new IllegalArgumentException("main class must be a Java qualified type name");
        }
        Path javaReal = java.toRealPath();
        Path jarReal = jar.toRealPath();
        if (!Files.isRegularFile(javaReal) || !Files.isExecutable(javaReal)) {
            throw new IllegalArgumentException("not an executable JDK launcher");
        }
        if (!Files.isRegularFile(jarReal) || !jarReal.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("CDS classpath must be one regular .jar file");
        }
        Path javaHome = javaReal.getParent().getParent();
        Path release = javaHome.resolve("release");
        Path modules = javaHome.resolve("lib/modules");
        Path vm = locateVm(javaHome);
        if (!Files.isRegularFile(release) || !Files.isRegularFile(modules) || !Files.isRegularFile(vm)) {
            throw new IllegalArgumentException("JDK release/modules/HotSpot runtime not found");
        }
        String releaseText = Files.readString(release, StandardCharsets.UTF_8);
        if (!releaseText.matches("(?s).*?(?:^|\\n)JAVA_VERSION=\"21(?:[.\\+\\-][^\\\"]*)?\"(?:\\r?\\n|$).*")) {
            throw new IllegalArgumentException("this proof requires an exact JDK21 runtime");
        }
        String nativeEncoding = System.getProperty("native.encoding", "");
        if (nativeEncoding.isBlank()) {
            throw new IllegalArgumentException("unknown native argument encoding");
        }
        Charset nativeCharset = Charset.forName(nativeEncoding);
        List<String> safeArgs = new ArrayList<>(arguments.size());
        for (String argument : arguments) {
            if (argument == null || argument.indexOf('\u0000') >= 0) {
                throw new IllegalArgumentException("null/NUL in application arguments");
            }
            if (!nativeCharset.newEncoder().canEncode(argument)) {
                throw new IllegalArgumentException("argument not representable in native JDK encoding");
            }
            safeArgs.add(argument);
        }
        TreeMap<String, String> values = new TreeMap<>();
        values.put("schema", SCHEMA);
        values.put("native_encoding", nativeEncoding);
        values.put("java", safeValue(javaReal.toString()));
        values.put("java_sha256", sha(javaReal));
        values.put("release_sha256", sha(release));
        values.put("modules_sha256", sha(modules));
        values.put("vm_sha256", sha(vm));
        values.put("jar", safeValue(jarReal.toString()));
        values.put("jar_sha256", sha(jarReal));
        values.put("jar_size", Long.toString(Files.size(jarReal)));
        values.put("jar_modified_millis", Long.toString(Files.getLastModifiedTime(jarReal).toMillis()));
        values.put("main", mainClass);
        values.put("arg_count", Integer.toString(safeArgs.size()));
        for (int index = 0; index < safeArgs.size(); index++) {
            values.put("arg_" + String.format(Locale.ROOT, "%06d", index),
                    encodeArgument(safeArgs.get(index)));
        }
        values.put("plan_sha256", shaText(table(values)));
        return new Plan(javaReal, jarReal, mainClass, List.copyOf(safeArgs), Map.copyOf(values));
    }

    /**
     * Reconstructs and independently reseals the original command, rejecting file drift.
     *
     * @param receipt original exact hash-bound training receipt
     * @return recomputed current plan when all original input identities match
     * @throws IOException if the source archive/JDK inputs cannot be read
     */
    public static Plan recheck(Map<String, String> receipt) throws IOException {
        require(receipt, "schema");
        if (!SCHEMA.equals(receipt.get("schema"))) {
            throw new IllegalArgumentException("unsupported plan contract");
        }
        int count;
        try {
            count = Integer.parseInt(require(receipt, "arg_count"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid arg_count", exception);
        }
        if (count < 0 || count > 10000) {
            throw new IllegalArgumentException("invalid argument count");
        }
        List<String> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String input = require(receipt, "arg_" + String.format(Locale.ROOT, "%06d", i));
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(input.equals("~") ? "" : input);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("invalid argument encoding", exception);
            }
            String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(
                    java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(
                    java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            if (!encodeArgument(value).equals(input)) {
                throw new IllegalArgumentException("noncanonical argument encoding");
            }
            args.add(value);
        }
        Plan now = inspect(Path.of(require(receipt, "java")), Path.of(require(receipt, "jar")),
                require(receipt, "main"), args);
        for (Map.Entry<String, String> entry : now.fields().entrySet()) {
            if (!entry.getValue().equals(receipt.get(entry.getKey()))) {
                throw new IllegalArgumentException("CDS source or JDK drift: " + entry.getKey());
            }
        }
        return now;
    }

    /**
     * Streams SHA-256 without materializing whole JDK images in memory.
     *
     * @param file existing source or archive file to digest
     * @return lowercase hexadecimal SHA-256
     * @throws IOException when a file cannot be read
     */
    public static String sha(Path file) throws IOException {
        MessageDigest digest = sha256Digest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[1 << 16];
            for (int n; (n = in.read(buffer)) != -1;) {
                digest.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Hash of exact UTF-8 text.
     *
     * @param value exact source text
     * @return lowercase hexadecimal SHA-256
     */
    public static String shaText(String value) {
        return HexFormat.of().formatHex(sha256Digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Strict canonical UTF-8 TSV without controls or empty values.
     *
     * @param values sorted or unsorted valid unique fields
     * @return LF-terminated key-sorted TSV text
     */
    public static String table(Map<String, String> values) {
        StringBuilder result = new StringBuilder("key\tvalue\n");
        for (var entry : new TreeMap<>(values).entrySet()) {
            result.append(safeValue(entry.getKey())).append('\t')
                    .append(safeValue(entry.getValue())).append('\n');
        }
        return result.toString();
    }

    /**
     * Decodes exact TSV and rejects duplicate, unsorted or noncanonical rows.
     *
     * @param file exact receipt path
     * @return immutable canonical receipt fields
     * @throws IOException when the receipt cannot be read
     */
    public static Map<String, String> parse(Path file) throws IOException {
        String contents = Files.readString(file, StandardCharsets.UTF_8);
        if (!contents.startsWith("key\tvalue\n") || !contents.endsWith("\n")
                || contents.indexOf('\r') >= 0 || contents.startsWith("\uFEFF")) {
            throw new IllegalArgumentException("noncanonical CDS receipt");
        }
        Map<String, String> values = new TreeMap<>();
        for (String line : contents.substring("key\tvalue\n".length()).split("\n")) {
            if (line.isEmpty()) continue;
            String[] pair = line.split("\t", -1);
            if (pair.length != 2 || values.put(safeValue(pair[0]), safeValue(pair[1])) != null) {
                throw new IllegalArgumentException("invalid or duplicated CDS receipt field");
            }
        }
        if (!table(values).equals(contents)) {
            throw new IllegalArgumentException("CDS receipt is not strictly canonical");
        }
        return Map.copyOf(values);
    }

    private static String encodeArgument(String arg) {
        if (arg.isEmpty()) return "~";
        return Base64.getEncoder().encodeToString(arg.getBytes(StandardCharsets.UTF_8));
    }

    private static String require(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null) throw new IllegalArgumentException("missing field " + key);
        return value;
    }

    private static String safeValue(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("blank receipt value");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\t' || c == '\r' || c == '\n' || c == '\0' || Character.isISOControl(c)) {
                throw new IllegalArgumentException("control byte in receipt value");
            }
        }
        return value;
    }

    private static Path locateVm(Path home) {
        Path[] candidates = {home.resolve("lib/server/libjvm.so"),
                home.resolve("lib/server/libjvm.dylib"), home.resolve("bin/server/jvm.dll")};
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) return candidate;
        }
        throw new IllegalArgumentException("HotSpot JVM binary not found");
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is mandatory", exception);
        }
    }

    /**
     * Immutable exact runtime/application command plus hash-bound TSV fields.
     *
     * @param java canonical JDK21 executable path
     * @param jar canonical application JAR path
     * @param mainClass Java main class
     * @param arguments ordered application argument vector
     * @param fields exact source and runtime image seals
     */
    public record Plan(Path java, Path jar, String mainClass, List<String> arguments,
            Map<String, String> fields) { }
}
