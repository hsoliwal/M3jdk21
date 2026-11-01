/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * One JIT test per method atom of every owned decorator. A child JVM runs the warmed perf harness
 * over every target ({@code M3DecoratorDifferential --jit}) with {@code -XX:+PrintCompilation};
 * each public method of each owned class then passes when (1) the JIT compiled it (tier 1 to 3 is
 * C1, tier 4 is C2; HotSpot drops to tier 2 while the C2 queue is long, so the tier reached is
 * recorded, not asserted) and (2) its bytecode fits the HotSpot hot-method inlining limit
 * ({@code FreqInlineSize}, 325 bytes), read from the class file. Overloads are told apart by
 * bytecode size, as PrintCompilation names a method by name and size.
 *
 * <p>Properties: {@code m3.decorator.dir}, {@code m3.jit.out} (one TSV row per atom: target,
 * atom, bytes, highest tier).
 */
final class M3DecoratorJitTest {
    static final int TIER_C1 = 1;
    static final int FREQ_INLINE_SIZE = 325;
    static final int NOT_COMPILED = 0;
    static final int EXIT_OK = 0;
    static final String PROP_OUT = "m3.jit.out";
    static final String PROP_CLASS_PATH = "java.class.path";
    static final String PROP_JAVA_HOME = "java.home";
    static final String HARNESS = "M3DecoratorDifferential";
    static final String NOT_ENTRANT = "made not entrant";
    static final String SEP = "::";
    static final String LOG = "jit-compilation.log";
    static final String ROW = "%s\t%s\t%d\t%d";
    static final String NOT_COMPILED_MESSAGE = "not JIT-compiled (highest tier %d)";
    static final String TOO_LARGE = "%d bytecode bytes > FreqInlineSize %d";
    /** A PrintCompilation line: tier, then optional flags, then class::method (N bytes). */
    static final Pattern COMPILED = Pattern.compile(
            "\\s([0-4])\\s+(?:[%sbn!]+\\s+)?([\\w.$]+)::(\\w+) \\((\\d+) bytes\\)");
    static final int GROUP_TIER = 1;
    static final int GROUP_CLASS = 2;
    static final int GROUP_METHOD = 3;
    static final int GROUP_BYTES = 4;
    /** class::method:bytes -> highest tier reached (entries made not entrant are ignored). */
    static final Map<String, Integer> TIERS = new HashMap<>();

    @BeforeAll
    static void runWarmedHarnessUnderPrintCompilation() throws IOException, InterruptedException {
        Path log = Files.createTempDirectory(HARNESS).resolve(LOG);
        Process p = new ProcessBuilder(Path.of(System.getProperty(PROP_JAVA_HOME), "bin", "java")
                .toString(), "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintCompilation", "-cp",
                System.getProperty(PROP_CLASS_PATH), HARNESS, "--jit",
                M3DecoratorAtomTest.DIR.resolve(M3DecoratorAtomTest.TARGETS).toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertEquals(EXIT_OK, p.waitFor());
        try (Stream<String> lines = Files.lines(log, StandardCharsets.ISO_8859_1)) {
            lines.filter(l -> !l.contains(NOT_ENTRANT)).map(COMPILED::matcher)
                    .filter(Matcher::find).forEach(m -> TIERS.merge(m.group(GROUP_CLASS) + SEP
                    + m.group(GROUP_METHOD) + SEP + m.group(GROUP_BYTES),
                    Integer.parseInt(m.group(GROUP_TIER)), Math::max));
        }
    }

    @TestFactory
    Stream<DynamicNode> everyMethodAtomIsJitCompiledAndInlinable() throws IOException {
        return M3DecoratorAtomTest.targets(M3DecoratorAtomTest.DIR).stream()
                .map(M3DecoratorJitTest::target);
    }

    private static DynamicNode target(String[] t) {
        String label = M3DecoratorAtomTest.label(t);
        Class<?> owned;
        Map<String, Integer> sizes;
        try {
            owned = Class.forName(t[M3DecoratorAtomTest.COL_OWNED]);
            sizes = codeSizes(owned);
        } catch (ReflectiveOperationException | IOException e) {
            return DynamicTest.dynamicTest(label, () -> {
                throw e;
            });
        }
        List<Method> atoms = Arrays.stream(owned.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()).toList();
        return DynamicContainer.dynamicContainer(label, atoms.stream().map(m -> {
            int bytes = sizes.get(m.getName() + descriptor(m));
            int tier = TIERS.getOrDefault(owned.getName() + SEP + m.getName() + SEP + bytes,
                    NOT_COMPILED);
            record(label, M3DecoratorDifferential.key(m), bytes, tier);
            return DynamicTest.dynamicTest(M3DecoratorDifferential.key(m), () -> {
                assertTrue(bytes <= FREQ_INLINE_SIZE, String.format(TOO_LARGE, bytes,
                        FREQ_INLINE_SIZE));
                assertTrue(tier >= TIER_C1, String.format(NOT_COMPILED_MESSAGE,
                        tier));
            });
        }));
    }

    private static String descriptor(Method m) {
        return MethodType.methodType(m.getReturnType(), m.getParameterTypes())
                .toMethodDescriptorString();
    }

    /** name + descriptor -> Code attribute length, read from the class file. */
    static Map<String, Integer> codeSizes(Class<?> c) throws IOException {
        try (InputStream in = c.getResourceAsStream(c.getSimpleName() + ".class")) {
            return ClassFile.codeSizes(new DataInputStream(in));
        }
    }

    private static void record(String label, String atom, int bytes, int tier) {
        String out = System.getProperty(PROP_OUT, "");
        if (!out.isEmpty()) {
            try {
                Files.writeString(Path.of(out), String.format(ROW, label, atom, bytes, tier)
                        + System.lineSeparator(), StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
    }

    /** The minimal class-file reader the JIT test needs (JVMS 4.1, 4.4, 4.6, 4.7.3). */
    static final class ClassFile {
        static final int TAG_UTF8 = 1;
        static final int TAG_INTEGER = 3;
        static final int TAG_FLOAT = 4;
        static final int TAG_LONG = 5;
        static final int TAG_DOUBLE = 6;
        static final int TAG_CLASS = 7;
        static final int TAG_STRING = 8;
        static final int TAG_METHOD_HANDLE = 15;
        static final int TAG_METHOD_TYPE = 16;
        static final int TAG_MODULE = 19;
        static final int TAG_PACKAGE = 20;
        static final int HEADER_BYTES = 8;
        static final int CLASS_INFO_BYTES = 6;
        static final int MEMBER_FIXED_BYTES = 6;
        static final int TWO_BYTE_ENTRY = 2;
        static final int FOUR_BYTE_ENTRY = 4;
        static final int HANDLE_ENTRY = 3;
        static final int CODE_HEADER_BYTES = 4;
        static final String CODE = "Code";

        private ClassFile() {
        }

        static Map<String, Integer> codeSizes(DataInputStream in) throws IOException {
            in.skipNBytes(HEADER_BYTES);
            String[] utf8 = constants(in);
            in.skipNBytes(CLASS_INFO_BYTES);
            in.skipNBytes((long) TWO_BYTE_ENTRY * in.readUnsignedShort());
            int fields = in.readUnsignedShort();
            for (int i = 0; i < fields; i++) {
                in.skipNBytes(MEMBER_FIXED_BYTES);
                skipAttributes(in);
            }
            Map<String, Integer> sizes = new HashMap<>();
            int methods = in.readUnsignedShort();
            for (int i = 0; i < methods; i++) {
                in.readUnsignedShort();
                String name = utf8[in.readUnsignedShort()];
                String descriptor = utf8[in.readUnsignedShort()];
                int attributes = in.readUnsignedShort();
                for (int a = 0; a < attributes; a++) {
                    String attribute = utf8[in.readUnsignedShort()];
                    int length = in.readInt();
                    if (attribute.equals(CODE)) {
                        in.skipNBytes(CODE_HEADER_BYTES);
                        int code = in.readInt();
                        sizes.put(name + descriptor, code);
                        in.skipNBytes((long) length - CODE_HEADER_BYTES - Integer.BYTES);
                    } else {
                        in.skipNBytes(length);
                    }
                }
            }
            return sizes;
        }

        /** Reads the constant pool; returns the UTF-8 entries by index (others are null). */
        private static String[] constants(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            String[] utf8 = new String[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case TAG_UTF8 -> utf8[i] = in.readUTF();
                    case TAG_CLASS, TAG_STRING, TAG_METHOD_TYPE, TAG_MODULE, TAG_PACKAGE ->
                            in.skipNBytes(TWO_BYTE_ENTRY);
                    case TAG_METHOD_HANDLE -> in.skipNBytes(HANDLE_ENTRY);
                    case TAG_LONG, TAG_DOUBLE -> {
                        in.skipNBytes(Long.BYTES);
                        i++;
                    }
                    case TAG_INTEGER, TAG_FLOAT -> in.skipNBytes(FOUR_BYTE_ENTRY);
                    default -> in.skipNBytes(FOUR_BYTE_ENTRY);
                }
            }
            return utf8;
        }

        private static void skipAttributes(DataInputStream in) throws IOException {
            int attributes = in.readUnsignedShort();
            for (int a = 0; a < attributes; a++) {
                in.readUnsignedShort();
                in.skipNBytes(Integer.toUnsignedLong(in.readInt()));
            }
        }
    }
}
