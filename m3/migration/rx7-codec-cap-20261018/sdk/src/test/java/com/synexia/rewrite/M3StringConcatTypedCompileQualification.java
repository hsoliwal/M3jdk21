// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.tools.ToolProvider;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Compile-only continuation of the qualified Result exporter; emitted classes are never loaded. */
final class M3StringConcatTypedCompileQualification {
    private static final String FACTORY =
            "src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java";
    private static final String TEST =
            "test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java";
    private static final String MANIFEST = "m3/runtime-integration/recipe/manifest.json";
    private static final String FACTORY_UNIT = "java/lang/invoke/StringConcatFactory.java";
    private static final String JLA_UNIT = "jdk/internal/access/JavaLangAccess.java";
    private static final String TEST_UNIT = "M3StringConcatReferenceDagTest.java";
    private static final String JLA_RESOURCE =
            "/com/synexia/rewrite/typed-compile-20261018/JavaLangAccess.java.txt";
    private static final String JLA_SHA =
            "b096377681dbc135d90f26abc972769557ae7c271d48ad59d241e149658438e3";
    private static final long RESERVE = 8L * 1024 * 1024 * 1024;
    private static final int WORK_CAP = 2 * 1024 * 1024;
    private static final int RECEIPT_CAP = 5800;
    private static final int CARRIER_CAP = 8192;
    private static final Map<String, Pin> PINS = Map.of(
            FACTORY, new Pin(62348,
                    "0bdcf30e2e090545d41e80566ca2b60d50d3a1c3e80e3dc849f1a22ecc3e11b8",
                    62359, "732c410330a1137aace06d3b20e0e8a86899b4fe98f15c738d588102383c4052"),
            TEST, new Pin(4536,
                    "e4624f56a93d68cd04337e39ed2cd3896867149908f287d5839ddff9b965ae95",
                    6913, "053c69bd70909731e6509d06f6b29d31a24e2fb669a57f82bf22587e44e839c0"),
            MANIFEST, new Pin(18689,
                    "12a749485fcd401d8fba311e5b8911f30e10988a63a14ebcbd11edac3fbee643",
                    18795, "260cf8202542543db466b0fba64ae7613e10810ecaac76dac4d6fea20e096779"));

    private M3StringConcatTypedCompileQualification() {}

    static void qualify(Map<String, String> before, List<Result> actual) throws Exception {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(actual, "actual");
        before = Map.copyOf(before);
        actual = List.copyOf(actual);
        require(before.keySet().equals(PINS.keySet()) && actual.size() == 3,
                "exact three-Result source allowlist required");
        Map<String, String> after = new TreeMap<>();
        for (String path : PINS.keySet()) {
            Pin pin = PINS.get(path);
            pinned(before.get(path), pin.beforeBytes(), pin.beforeSha(), "original " + path);
        }
        for (Result result : actual) {
            require(result != null && result.getBefore() != null && result.getAfter() != null,
                    "complete existing-file Result required");
            SourceFile original = result.getBefore();
            SourceFile generated = result.getAfter();
            String path = portable(generated.getSourcePath());
            require(PINS.containsKey(path) && portable(original.getSourcePath()).equals(path),
                    "foreign or renamed compile Result");
            require(original.getSourcePath().equals(generated.getSourcePath())
                            && original.getId().equals(generated.getId()),
                    "Result source identity changed");
            require(original.printAll().equals(before.get(path)), "Result preimage mismatch");
            if (path.equals(TEST)) {
                require(original instanceof J.CompilationUnit && generated instanceof J.CompilationUnit,
                        "jtreg Result must remain Java AST");
            } else {
                require(original instanceof PlainText && generated instanceof PlainText,
                        "existing Factory/JSON text plane required");
            }
            Pin pin = PINS.get(path);
            String text = generated.printAll();
            pinned(text, pin.afterBytes(), pin.afterSha(), "actual Result " + path);
            require(after.put(path, text) == null, "duplicate compile Result");
        }
        require(after.keySet().equals(PINS.keySet()), "missing compile Result");
        byte[] jlaBytes;
        try (InputStream stream =
                M3StringConcatTypedCompileQualification.class.getResourceAsStream(JLA_RESOURCE)) {
            require(stream != null, "authentic full JavaLangAccess resource required");
            jlaBytes = stream.readNBytes(65537);
        }
        require(jlaBytes.length == 19390 && digest(jlaBytes).equals(JLA_SHA),
                "authentic JavaLangAccess resource drift");
        String jla = decode(jlaBytes);
        require(java.util.Arrays.equals(jlaBytes, jla.getBytes(StandardCharsets.UTF_8)),
                "JavaLangAccess UTF-8 round trip");
        require(Runtime.version().feature() == 21, "native Java21 compiler required");
        var compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "full native Java21 compiler required");

        Path build = owned("m3.build.root");
        Path recipe = owned("m3.recipe.root");
        Path output = owned("m3.output.root");
        require(disjoint(build, recipe) && disjoint(build, output) && disjoint(recipe, output),
                "external build/recipe/output roots must be disjoint");
        Path temporary = owned("java.io.tmpdir");
        require(temporary.startsWith(build), "temporary root must stay in external build");
        reserve(build);
        Path work = Files.createTempDirectory(build, "string-concat-typed-compile-");
        require(work.getParent().equals(build) && !Files.isSymbolicLink(work),
                "fresh build-owned work required");
        try (var children = Files.list(work)) {
            require(children.findAny().isEmpty(), "fresh work must be empty");
        }

        List<String> rows = new ArrayList<>();
        Map<String, FilePin> files = new TreeMap<>();
        Set<String> factoryClasses = Set.of("java/lang/invoke/StringConcatFactory.class",
                "jdk/internal/access/JavaLangAccess.class");
        Set<String> testClasses = Set.of("M3StringConcatReferenceDagTest.class",
                "M3StringConcatReferenceDagTest$Probe.class");
        addCase(work, "factory-before", Map.of(FACTORY_UNIT, before.get(FACTORY), JLA_UNIT, jla),
                true, true, factoryClasses, rows, files);
        addCase(work, "factory-actual", Map.of(FACTORY_UNIT, after.get(FACTORY), JLA_UNIT, jla),
                true, true, factoryClasses, rows, files);
        addCase(work, "factory-without-jla", Map.of(FACTORY_UNIT, after.get(FACTORY)),
                true, false, Set.of(), rows, files);
        addCase(work, "jtreg-before", Map.of(TEST_UNIT, before.get(TEST)),
                false, true, testClasses, rows, files);
        addCase(work, "jtreg-actual", Map.of(TEST_UNIT, after.get(TEST)),
                false, true, testClasses, rows, files);
        List<String> distinct = List.copyOf(new LinkedHashSet<>(rows));
        require(distinct.stream().filter(row -> row.startsWith("case\t")).count() == 5
                        && distinct.stream().filter(row -> row.startsWith("class\t")).count() == 8
                        && distinct.stream().filter(row -> row.startsWith("diagnostic\t")).count() == 1
                        && distinct.stream().filter(row -> row.startsWith("options_b64\t")).count() == 2,
                "five compile cases/eight class outputs/two option profiles required");
        verifyWork(work, files);
        reserve(build);
        StringBuilder receipt = new StringBuilder(
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "# schema=M3_STRING_CONCAT_TYPED_COMPILE_V1\n"
                + "# recipe=com.synexia.m3.StringConcatNullString20261018\n"
                + "# sourceOrigin=PINNED_BEFORE_AND_ACTUAL_NAMED_FORWARD_RESULT_AFTER\n"
                + "# caseRootPattern=" + portable(work) + "/{id}\n"
                + "# options_b64=Base64(UTF8(newline-joined options)); no trailing newline; "
                + "${CASE}=absolute caseRootPattern with {id} replaced by row case id\n"
                + "# java.version=" + property("java.version") + "\n"
                + "# java.runtime.version=" + property("java.runtime.version") + "\n"
                + "# java.vendor=" + property("java.vendor") + "\n"
                + "# java.home=" + property("java.home") + "\n"
                + "# nativeRuntime=" + Runtime.version() + "\n"
                + "# compiler=" + compiler.getClass().getName() + "\n"
                + "# compilerModule=" + compiler.getClass().getModule().getName() + "\n"
                + "# javaReleaseSha256=" + releaseHash() + "\n"
                + "# cases=5 positive=4 missingJlaRefusals=1 emittedClasses=8\n"
                + "# originalModel=NOT_RUN\n# actualEveryModule=NOT_RUN\n"
                + "# fullJdkCompile=NOT_RUN\n# matchedImages=NOT_RUN\n# JNI=NOT_RUN\n"
                + "# runtime=NOT_RUN\n# jtregAssertionsExecuted=0\n"
                + "# performanceAndRetention=NOT_RUN\n# productApplied=false\n"
                + "# canonicalPromotion=false\n# oldDiffExporterFlags=UNCHANGED_STAGE_LOCAL\n"
                + "# cooperativeWorkCapBytes=2097152; not an OS quota\n"
                + "# rows=case(id,mode,outcome);source(id,path,bytes,sha256);"
                + "class(id,path,bytes,sha256,major,minor);"
                + "diagnostic(id,code,line,messageSha256);options_b64(mode,payload)\n");
        for (String row : distinct) receipt.append(row).append('\n');
        byte[] bytes = receipt.toString().getBytes(StandardCharsets.UTF_8);
        require(bytes.length <= RECEIPT_CAP, "typed compile receipt exceeds 5800-byte cap");
        Path receiptPath = work.resolve("receipt.tsv");
        Files.write(receiptPath, bytes, StandardOpenOption.CREATE_NEW);
        files.put("receipt.tsv", new FilePin(bytes.length, digest(bytes)));
        verifyWork(work, files);
        reserve(build);
        byte[] observed = bounded(receiptPath, RECEIPT_CAP);
        require(java.util.Arrays.equals(bytes, observed), "typed receipt readback drift");
        String carrier = "M3_STRING_CONCAT_TYPED_COMPILE_B64_V1\treceipt.tsv\t"
                + digest(observed) + "\t" + observed.length + "\t"
                + Base64.getEncoder().encodeToString(observed) + "\n";
        require(carrier.getBytes(StandardCharsets.UTF_8).length <= CARRIER_CAP,
                "typed compile carrier exceeds 8KiB cap");
        System.out.print(carrier);
        System.out.println("M3_STRING_CONCAT_TYPED_COMPILE_PASS cases=5 positive=4"
                + " missingJlaRefusals=1 emittedClasses=8 runtime=NOT_RUN productionApplied=false"
                + " originalModel=NOT_RUN actualEveryModule=NOT_RUN fullJdkCompile=NOT_RUN"
                + " matchedImages=NOT_RUN JNI=NOT_RUN jtregAssertionsExecuted=0");
    }

    private static void addCase(Path work, String id, Map<String, String> units,
            boolean platform, boolean success, Set<String> expectedClasses,
            List<String> rows, Map<String, FilePin> files) throws Exception {
        List<String> observed = M3StringConcatTypedCompiler.compileCase(
                work, id, units, platform, success, expectedClasses);
        require(observed != null && observed.size() <= 16, "bounded compiler rows required");
        String mode = platform ? "PLATFORM" : "UNNAMED";
        Set<String> sources = new TreeSet<>();
        Set<String> classes = new TreeSet<>();
        int cases = 0;
        int diagnostics = 0;
        int options = 0;
        for (String row : observed) {
            require(row != null && !row.contains("\n") && !row.contains("\r")
                            && row.getBytes(StandardCharsets.UTF_8).length <= 2048,
                    "bounded single-line compiler row required");
            String[] fields = row.split("\t", -1);
            switch (fields[0]) {
                case "case" -> {
                    require(fields.length == 4 && fields[1].equals(id)
                                    && fields[2].equals(mode)
                                    && fields[3].equals(success ? "SUCCESS" : "EXPECTED_MISSING_JLA"),
                            "compiler case result drift");
                    cases++;
                }
                case "source" -> {
                    require(fields.length == 5 && fields[1].equals(id)
                                    && units.containsKey(fields[2]) && sources.add(fields[2]),
                            "compiler source inventory drift");
                    byte[] expected = units.get(fields[2]).getBytes(StandardCharsets.UTF_8);
                    require(fields[3].equals(Integer.toString(expected.length))
                                    && fields[4].equals(digest(expected)),
                            "compiler source byte/hash drift");
                    String relative = id + "/sources/" + fields[2];
                    FilePin pin = new FilePin(expected.length, digest(expected));
                    require(files.put(relative, pin) == null, "duplicate source receipt path");
                    require(java.util.Arrays.equals(expected,
                                    bounded(work.resolve(relative), 65536)),
                            "compiler source changed after admission");
                }
                case "class" -> {
                    require(fields.length == 7 && fields[1].equals(id)
                                    && expectedClasses.contains(fields[2]) && classes.add(fields[2])
                                    && fields[5].equals("65") && fields[6].equals("0"),
                            "compiler class inventory/Java21 version drift");
                    String relative = id + "/classes/" + fields[2];
                    byte[] data = bounded(work.resolve(relative), 262144);
                    require(data.length >= 8 && data[0] == (byte) 0xca
                                    && data[1] == (byte) 0xfe && data[2] == (byte) 0xba
                                    && data[3] == (byte) 0xbe && data[4] == 0 && data[5] == 0
                                    && data[6] == 0 && data[7] == 65
                                    && fields[3].equals(Integer.toString(data.length))
                                    && fields[4].equals(digest(data)),
                            "compiler class readback drift");
                    require(files.put(relative, new FilePin(data.length, digest(data))) == null,
                            "duplicate class receipt path");
                }
                case "diagnostic" -> {
                    require(!success && fields.length == 5 && fields[1].equals(id)
                                    && fields[2].startsWith("compiler.err.cant.resolve")
                                    && fields[3].equals("492")
                                    && fields[4].matches("[0-9a-f]{64}"),
                            "expected exact missing-JLA diagnostic required");
                    diagnostics++;
                }
                case "options_b64" -> {
                    require(fields.length == 3 && fields[1].equals(mode),
                            "compiler option profile drift");
                    byte[] encoded = Base64.getDecoder().decode(fields[2]);
                    require(encoded.length <= 2048
                                    && Base64.getEncoder().encodeToString(encoded).equals(fields[2]),
                            "canonical bounded option encoding required");
                    String decoded = decode(encoded);
                    require(decoded.contains("${CASE}") && !decoded.endsWith("\n")
                                    && !decoded.contains("\r")
                                    && !decoded.contains(work.toString())
                                    && !decoded.contains(portable(work)),
                            "compiler options must use documented case placeholder");
                    options++;
                }
                default -> throw new AssertionError("unknown compiler receipt row");
            }
            rows.add(row);
        }
        require(cases == 1 && options == 1 && diagnostics == (success ? 0 : 1)
                        && sources.equals(units.keySet()) && classes.equals(expectedClasses),
                "incomplete or duplicated compiler receipt");
        verifyWork(work, files);
        reserve(work);
    }

    private static void verifyWork(Path work, Map<String, FilePin> expected) throws Exception {
        Set<String> directories = new TreeSet<>();
        directories.add("");
        for (String id : List.of("factory-before", "factory-actual", "factory-without-jla",
                "jtreg-before", "jtreg-actual")) {
            if (Files.exists(work.resolve(id), LinkOption.NOFOLLOW_LINKS)) {
                directories.add(id);
                directories.add(id + "/sources");
                directories.add(id + "/classes");
                directories.add(id + "/empty");
            }
        }
        for (String file : expected.keySet()) {
            Path parent = Path.of(file).getParent();
            while (parent != null) {
                directories.add(portable(parent));
                parent = parent.getParent();
            }
        }
        Set<String> found = new TreeSet<>();
        long total = 0;
        try (var stream = Files.walk(work)) {
            List<Path> paths = stream.limit(129).toList();
            require(paths.size() <= 128, "bounded closed compile tree required");
            for (Path path : paths) {
                require(!Files.isSymbolicLink(path), "compile work contains a symlink");
                String relative = portable(work.relativize(path));
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    require(directories.contains(relative), "foreign compile directory");
                } else {
                    require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                                    && expected.containsKey(relative) && found.add(relative),
                            "foreign compile output");
                    FilePin pin = expected.get(relative);
                    byte[] data = bounded(path, Math.toIntExact(pin.bytes()));
                    require(data.length == pin.bytes() && digest(data).equals(pin.sha()),
                            "closed compile inventory drift");
                    total += data.length;
                }
            }
        }
        require(found.equals(expected.keySet()) && found.size() <= 64 && total <= WORK_CAP,
                "closed compile inventory/cap mismatch");
    }

    private static Path owned(String property) throws Exception {
        String value = property(property);
        Path path = Path.of(value);
        require(path.isAbsolute() && path.equals(path.normalize()), "absolute normalized root required");
        for (Path current = path; current != null; current = current.getParent()) {
            require(!Files.isSymbolicLink(current)
                            && Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS),
                    "nonlink existing root ancestry required");
        }
        return path;
    }

    private static boolean disjoint(Path first, Path second) {
        return !first.startsWith(second) && !second.startsWith(first);
    }

    private static void reserve(Path path) throws Exception {
        require(Files.getFileStore(path).getUsableSpace() >= RESERVE + WORK_CAP,
                "8GiB reserve plus planned two-MiB work required");
    }

    private static String releaseHash() throws Exception {
        Path release = Path.of(property("java.home")).resolve("release");
        if (!Files.isRegularFile(release, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(release)) return "UNAVAILABLE";
        return digest(bounded(release, 65536));
    }

    private static String property(String name) {
        String value = System.getProperty(name);
        require(value != null && !value.isBlank() && value.length() <= 1024
                        && value.chars().noneMatch(Character::isISOControl),
                "bounded native property required: " + name);
        return value;
    }

    private static void pinned(String text, int bytes, String sha, String label) throws Exception {
        require(text != null, "missing " + label);
        byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
        require(encoded.length == bytes && digest(encoded).equals(sha), "source pin drift: " + label);
    }

    private static byte[] bounded(Path path, int maximum) throws Exception {
        require(maximum >= 0 && maximum <= WORK_CAP
                        && !Files.isSymbolicLink(path)
                        && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && Files.size(path) <= maximum,
                "bounded regular nonlink file required");
        byte[] data;
        try (InputStream stream = Files.newInputStream(path)) {
            data = stream.readNBytes(maximum + 1);
        }
        require(data.length <= maximum && !Files.isSymbolicLink(path)
                        && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS),
                "bounded read changed during custody");
        return data;
    }

    private static String decode(byte[] bytes) throws Exception {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static String digest(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String portable(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record Pin(int beforeBytes, String beforeSha, int afterBytes, String afterSha) {}
    private record FilePin(long bytes, String sha) {}
}
