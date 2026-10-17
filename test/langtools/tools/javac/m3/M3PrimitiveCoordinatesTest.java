/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only.
 */
/*
 * @test
 * @summary Production primitive coordinate export: typed constants and prepublication refusals
 * @requires (os.family == "linux")
 * @modules jdk.compiler/com.sun.tools.javac.m3
 * @run main/othervm M3PrimitiveCoordinatesTest
 */

import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import javax.tools.ToolProvider;

/** Runs actual compiler output, not a hand-built replacement coordinate class. */
public final class M3PrimitiveCoordinatesTest {
    private static int checks;
    private static Path work, image, slots, source, output;
    private record Request(String name, String kind, long bits, int tag) {
        String argument() { return name + ":" + kind + ":" + HexFormat.of().toHexDigits(bits); }
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static List<String> arguments(Path javaOut, String javaClass, List<String> selections) {
        var args = new ArrayList<>(List.of("--out", image.toString(), "--slots", slots.toString(),
                "--fold-expressions", source.toString()));
        if (javaOut != null) Collections.addAll(args, "--java-out", javaOut.toString());
        if (javaClass != null) Collections.addAll(args, "--java-class", javaClass);
        for (String selection : selections) Collections.addAll(args, "--constant", selection);
        return args;
    }
    private static void emit(List<String> args) throws Exception {
        M3PrimitiveImageCompiler.main(args.toArray(String[]::new));
    }
    private static void refuse(List<String> args) throws Exception {
        byte[] a = Files.readAllBytes(image), b = Files.readAllBytes(slots);
        byte[] c = Files.readAllBytes(output), d = Files.readAllBytes(source);
        boolean rejected = false;
        try { emit(args); }
        catch (IllegalArgumentException | IOException expected) { rejected = true; }
        check(rejected, "bad export request refused: " + args);
        check(Arrays.equals(a, Files.readAllBytes(image)), "image preserved");
        check(Arrays.equals(b, Files.readAllBytes(slots)), "sidecar preserved");
        check(Arrays.equals(c, Files.readAllBytes(output)), "Java export preserved");
        check(Arrays.equals(d, Files.readAllBytes(source)), "source preserved");
    }
    private static long number(Class<?> type, String field) throws Exception {
        return ((Number)type.getField(field).get(null)).longValue();
    }
    private static void compileAndInspect(Path javaOut, String name, List<Request> requests) throws Exception {
        Path classes = Files.createTempDirectory(work, "export-classes-");
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d", classes.toString(), javaOut.toString());
        check(result == 0, "generated Java compiles strictly");
        byte[] raw = Files.readAllBytes(image);
        ByteBuffer imageBytes = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, null)) {
            Class<?> type = Class.forName(name, true, loader);
            check(type.getFields().length == requests.size() * 5 + 4, "bounded scalar fields only");
            check(type.getField("IMAGE_NAMESPACE").get(null).equals(HexFormat.of().formatHex(raw, 24, 56)), "compiled namespace");
            check(number(type, "IMAGE_LENGTH") == raw.length, "compiled image size");
            check(number(type, "IMAGE_ROWS") == imageBytes.getInt(12), "compiled row count");
            check(number(type, "IMAGE_VERSION") == 1, "compiled format version");
            for (Request request : requests) {
                long slot = number(type, request.name + "_SLOT");
                long row = number(type, request.name + "_ROW_OFFSET");
                long bitsOffset = number(type, request.name + "_BITS_OFFSET");
                check(slot >= 0 && slot < imageBytes.getInt(12), "bounded slot");
                check(row == 64 + 16 * slot && bitsOffset == row + 8, "row vs payload offset");
                check(number(type, request.name + "_KIND") == request.tag, "requested kind");
                check(Byte.toUnsignedInt(imageBytes.get((int)row)) == request.tag, "image kind");
                check(number(type, request.name + "_RAW_BITS") == request.bits, "compiled raw bits");
                check(imageBytes.getLong((int)bitsOffset) == request.bits, "image raw bits");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        work = args.length == 0 ? Files.createTempDirectory("m3-coordinates-") : Path.of(args[0]);
        Files.createDirectories(work);
        source = work.resolve("Input.java"); image = work.resolve("values.m3prim");
        slots = work.resolve("values.tsv"); output = work.resolve("Coordinates.java");
        Files.writeString(source, """
                class Input {
                  static final boolean B = true;
                  static final byte Y = (byte)-1;
                  static final short S = (short)-1;
                  static final char C = (char)65535;
                  static final int I = -1;
                  static final long L = -1L;
                  static final float F = -0.0f;
                  static final double D = -0.0d;
                  static final long LOW = -32L, HIGH = 32L;
                  static { throwIfExecuted(); }
                  static void throwIfExecuted() { throw new AssertionError("application executed"); }
                }
                """);
        List<Request> requests = List.of(new Request("BOOL", "boolean", 1, 1),
                new Request("BYTE", "byte", 255, 2), new Request("SHORT", "short", 65535, 3),
                new Request("CHAR", "char", 65535, 4), new Request("INT", "int", 0xffffffffL, 5),
                new Request("LONG", "long", -1, 6), new Request("FLOAT", "float", 0x80000000L, 7),
                new Request("DOUBLE", "double", Long.MIN_VALUE, 8), new Request("LOW", "long", -32, 6),
                new Request("HIGH", "long", 32, 6), new Request("LOW_ALIAS", "long", -32, 6));
        List<String> selections = requests.stream().map(Request::argument).toList();
        emit(arguments(null, null, List.of()));
        byte[] baselineImage = Files.readAllBytes(image), baselineSlots = Files.readAllBytes(slots);
        emit(arguments(output, "exported.Coordinates", selections));
        byte[] java = Files.readAllBytes(output);
        check(Arrays.equals(baselineImage, Files.readAllBytes(image)), "export does not change image");
        check(Arrays.equals(baselineSlots, Files.readAllBytes(slots)), "export does not change sidecar");
        check(!Files.exists(work.resolve("Input.class")), "no application class output");
        compileAndInspect(output, "exported.Coordinates", requests);
        var reversed = new ArrayList<>(selections); Collections.reverse(reversed);
        emit(arguments(output, "exported.Coordinates", reversed));
        check(Arrays.equals(java, Files.readAllBytes(output)), "deterministic request ordering");
        // Also prove name shadowing and no-package output remain compilable.
        Path shadow = work.resolve("String.java");
        emit(arguments(shadow, "shadow.String", selections));
        compileAndInspect(shadow, "shadow.String", requests);
        emit(arguments(output, "Coordinates", selections));
        compileAndInspect(output, "Coordinates", requests);
        emit(arguments(output, "exported.Coordinates", selections));

        for (String bad : List.of("MISSING:int:12345678", "BAD:boolean:2", "BAD:byte:100",
                "BAD:short:10000", "BAD:char:10000", "BAD:int:100000000", "BAD:float:100000000",
                "BAD:string:1", "BAD:long:-1", "BAD:long:0x1", "BAD:long:10000000000000000",
                "BAD:long:", "a:int:1", "A;evil:int:1", "A:int:1:2", "_:int:1")) {
            refuse(arguments(output, "exported.Coordinates", List.of(bad)));
        }
        for (String bad : List.of("a.class", "a.record", "a..Coordinates", ".Coordinates",
                "A;evil", "java", "a.java", "java.lang.Coordinates", "a._", "a.var", "a/Coordinates",
                "A".repeat(65), "a.".repeat(100) + "Coordinates")) {
            refuse(arguments(output, bad, selections));
        }
        refuse(arguments(output, "Other", selections));
        refuse(arguments(output, "exported.Coordinates", List.of("X:long:ffffffffffffffff", "X:long:ffffffffffffffff")));
        refuse(arguments(null, "exported.Coordinates", selections));
        refuse(arguments(output, null, selections));
        refuse(arguments(output, "exported.Coordinates", List.of()));
        refuse(arguments(null, null, selections));
        refuse(arguments(image, "Coordinates", selections));
        refuse(arguments(source, "Input", selections));
        refuse(arguments(work.resolve("values.m3prim/Coordinates.java"), "Coordinates", selections));
        List<String> noValue = arguments(output, "exported.Coordinates", selections);
        noValue.add("--constant"); refuse(noValue);
        List<String> duplicate = arguments(output, "exported.Coordinates", selections);
        Collections.addAll(duplicate, "--java-class", "exported.Coordinates"); refuse(duplicate);
        duplicate = arguments(output, "exported.Coordinates", selections);
        Collections.addAll(duplicate, "--java-out", output.toString()); refuse(duplicate);
        // Both sides of the bounded export policy are executed, not merely counted.
        var maximum = new ArrayList<Request>();
        for (int i = 0; i < 128; i++) maximum.add(new Request("V" + i, "long", -1L, 6));
        emit(arguments(output, "exported.Coordinates", maximum.stream().map(Request::argument).toList()));
        compileAndInspect(output, "exported.Coordinates", maximum);
        maximum.add(new Request("V128", "long", -1, 6));
        refuse(arguments(output, "exported.Coordinates", maximum.stream().map(Request::argument).toList()));
        emit(arguments(output, "exported.Coordinates", selections));
        Path alias = work.resolve("Alias.java"); Files.createLink(alias, source);
        refuse(arguments(alias, "Alias", selections));
        Files.delete(alias); Files.createSymbolicLink(alias, output.toAbsolutePath());
        refuse(arguments(alias, "Alias", selections));
        Files.delete(alias);
        Files.createSymbolicLink(work.resolve("linked"), work.toAbsolutePath());
        refuse(arguments(work.resolve("linked/Coordinates.java"), "Coordinates", selections));
        // Missing selection must not create any output, including its missing directory.
        List<String> fresh = arguments(work.resolve("fresh/Coordinates.java"), "Coordinates", List.of("MISSING:long:12345678"));
        fresh.set(1, work.resolve("fresh/values.m3prim").toString());
        fresh.set(3, work.resolve("fresh/values.tsv").toString());
        refuse(fresh);
        check(!Files.exists(work.resolve("fresh")), "prepublication refusal creates no files/directories");
        check(Arrays.equals(java, Files.readAllBytes(output)), "final export intact");
        System.out.println("M3_COORDINATES_PASS checks=" + checks + " selected=" + requests.size()
                + " maxSelections=128 output=" + output);
    }
}
