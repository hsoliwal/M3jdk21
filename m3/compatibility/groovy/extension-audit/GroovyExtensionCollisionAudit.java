// SPDX-FileCopyrightText: 2026 Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Test-only fail-closed Groovy extension/JDK-member collision census.
 *
 * A Groovy extension usually has the form public static result name(Receiver, args...).
 * The first argument is the receiver, so an added JDK instance member with the same
 * selector and explicit argument count is a conservative extension-shadow hazard.
 *
 * No Groovy internals or third-party bytecode are embedded, and this scanner does
 * not invoke Groovy extension methods or evaluate scripts. Method-name/arity
 * overlap is a warning, not proof of Groovy's dynamic overload selection. The
 * existing GroovyInteropGate behavioral oracle remains required for admission.
 */
public final class GroovyExtensionCollisionAudit {
    private static final String PREFIX = "M3X\t";
    /** The only independently pinned Groovy releases admitted by this gate. */
    private static final Set<String> GROOVY_RELEASES = Set.of("4.0.33", "5.1.3", "6.0.0");
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final long MAX_CHILD_BYTES = 64L * 1024 * 1024;
    private static final Pattern TYPE = Pattern.compile("(?:[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*|boolean|byte|short|char|int|long|float|double)(?:\\[\\])*");
    private static final Pattern JAR_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]*\\.jar");
    private static final Pattern SHA256 = Pattern.compile("[a-f0-9]{64}");
    private static final List<String> DEFAULT_OWNERS = List.of(
            "boolean[]", "byte[]", "char[]", "double[]", "float[]", "int[]",
            "java.io.File", "java.io.InputStream", "java.io.Reader", "java.lang.CharSequence",
            "java.lang.Integer", "java.lang.Iterable", "java.lang.Number", "java.lang.Object",
            "java.lang.Object[]", "java.lang.String", "java.lang.StringBuilder",
            "java.lang.String[]", "java.net.URI", "java.net.URL",
            "java.nio.file.Path", "java.time.LocalDate", "java.util.ArrayList",
            "java.util.Collection", "java.util.Iterator", "java.util.LinkedHashMap",
            "java.util.List", "java.util.Map", "java.util.Optional", "java.util.Set",
            "java.util.concurrent.CompletableFuture", "java.util.regex.Matcher",
            "java.util.stream.Stream", "long[]", "short[]");
    private static final List<String> DEFAULT_PROVIDERS = List.of(
            "org.codehaus.groovy.runtime.ArrayGroovyMethods",
            "org.codehaus.groovy.runtime.DefaultGroovyMethods",
            "org.codehaus.groovy.runtime.IOGroovyMethods",
            "org.codehaus.groovy.runtime.ResourceGroovyMethods",
            "org.codehaus.groovy.runtime.StringGroovyMethods");

    private GroovyExtensionCollisionAudit() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("selftest")) {
            selfTest();
        } else if (args.length == 2 && args[0].equals("defaults")) {
            defaults(Path.of(args[1]));
        } else if (args.length == 3 && (args[0].equals("probe")
                || args[0].equals("probe-m3") || args[0].equals("probe-synthetic"))) {
            probe(Path.of(args[1]), Path.of(args[2]), args[0].equals("probe-synthetic"),
                    args[0].equals("probe-m3"));
        } else if (args.length == 9 && args[0].equals("compare-locked")) {
            compareLocked(Path.of(args[1]), Path.of(args[2]), args[3],
                    Path.of(args[4]), Path.of(args[5]), Path.of(args[6]), Path.of(args[7]),
                    false, args[8]);
        } else if (args.length == 8 && args[0].equals("compare-synthetic-locked")) {
            compareLocked(Path.of(args[1]), Path.of(args[2]), args[3],
                    Path.of(args[4]), Path.of(args[5]), Path.of(args[6]), Path.of(args[7]),
                    true, "SYNTHETIC_ONLY");
        } else {
            throw new IllegalArgumentException("Usage: selftest | defaults <directory> | probe <owners.txt> <providers.txt> | "
                    + "compare-locked <stock-java> <m3-java> <groovy-jar-classpath> "
                    + " <owners.txt> <providers.txt> <verified-jar-lock.tsv> "
                    + "<fresh-output-directory> <exact-groovy-version>");
        }
    }

    /** Emit reviewed, deterministic default inventories; refuse edits on replay. */
    private static void defaults(Path destination) throws IOException {
        if (Files.isSymbolicLink(destination) || Files.exists(destination) && !Files.isDirectory(destination)) {
            throw new IllegalArgumentException("Invalid manifest destination: " + destination);
        }
        Files.createDirectories(destination);
        writeExact(destination.resolve("owners.txt"), String.join("\n", DEFAULT_OWNERS) + "\n");
        writeExact(destination.resolve("providers.txt"), String.join("\n", DEFAULT_PROVIDERS) + "\n");
        System.out.println("M3_GROOVY_EXTENSION_DEFAULTS_PASS owners=" + DEFAULT_OWNERS.size()
                + " providers=" + DEFAULT_PROVIDERS.size());
    }

    private static void writeExact(Path destination, String expected) throws IOException {
        if (Files.isSymbolicLink(destination) || Files.exists(destination)
                && (!Files.isRegularFile(destination)
                || !Files.readString(destination, StandardCharsets.UTF_8).equals(expected))) {
            throw new IllegalArgumentException("Refusing manifest overwrite: " + destination);
        }
        if (!Files.exists(destination)) Files.writeString(destination, expected, StandardCharsets.UTF_8);
    }

    /** Inspect only explicitly named owner and extension-provider types. */
    private static void probe(Path ownerFile, Path providerFile,
            boolean synthetic, boolean m3Candidate) throws Exception {
        if (m3Candidate && ModuleLayer.boot().findModule("jdk.m3.collections").isEmpty()) {
            throw new IllegalStateException("Candidate JVM did not resolve jdk.m3.collections");
        }
        List<String> owners = types(ownerFile);
        List<String> providers = types(providerFile);
        String groovyVersion = "SYNTHETIC_ONLY";
        if (!synthetic) {
            if (!providers.contains("org.codehaus.groovy.runtime.DefaultGroovyMethods")
                    || !providers.contains("org.codehaus.groovy.runtime.StringGroovyMethods")) {
                throw new IllegalArgumentException("Real Groovy audit requires core GDK providers");
            }
            Class<?> groovy = Class.forName("groovy.lang.GroovySystem", false,
                    GroovyExtensionCollisionAudit.class.getClassLoader());
            groovyVersion = String.valueOf(groovy.getMethod("getVersion").invoke(null));
            if (!GROOVY_RELEASES.contains(groovyVersion)) {
                throw new IllegalStateException("Unrecognised Groovy runtime: " + groovyVersion);
            }
        }
        List<Class<?>> loaded = new ArrayList<>(providers.size());
        for (String provider : providers) {
            loaded.add(Class.forName(provider, false, GroovyExtensionCollisionAudit.class.getClassLoader()));
        }
        Snapshot observed = snapshot(owners, loaded);
        System.out.println(PREFIX + "V\t" + groovyVersion);
        for (var entry : observed.members.entrySet()) {
            for (String descriptor : entry.getValue()) {
                System.out.println(PREFIX + "M\t" + entry.getKey() + "\t" + descriptor);
            }
        }
        for (var entry : observed.extensions.entrySet()) {
            for (String descriptor : entry.getValue()) {
                System.out.println(PREFIX + "E\t" + entry.getKey() + "\t" + descriptor);
            }
        }
    }

    /** No change is made to either Java home or third-party jars. */
    private static void compareLocked(Path stockJava, Path m3Java, String groovyClasspath,
            Path ownerFile, Path providerFile, Path jarLock, Path outputDir,
            boolean synthetic, String expectedGroovyVersion) throws Exception {
        requireRegular(stockJava, "stock java launcher");
        requireRegular(m3Java, "candidate java launcher");
        if (Files.isSameFile(stockJava, m3Java) && !synthetic) {
            throw new IllegalArgumentException("Stock and candidate Java executables are identical");
        }
        if (synthetic) {
            if (!"SYNTHETIC_ONLY".equals(expectedGroovyVersion)) {
                throw new IllegalArgumentException("Synthetic mode must use synthetic attestation");
            }
        } else if (!GROOVY_RELEASES.contains(expectedGroovyVersion)) {
            throw new IllegalArgumentException("Unapproved Groovy release: " + expectedGroovyVersion);
        }
        Map<String, String> locked = lock(jarLock);
        Map<String, String> observed = classpathSeals(groovyClasspath);
        if (!locked.equals(observed)) throw new IllegalArgumentException("Groovy classpath does not match verified lock");
        List<String> owners = types(ownerFile), providers = types(providerFile);
        // These exact inputs are re-sealed between launches to detect source drift.
        Map<String, String> beforeSeals = inputSeals(ownerFile, providerFile, jarLock, groovyClasspath);
        if (Files.exists(outputDir) || Files.isSymbolicLink(outputDir)) {
            throw new IllegalArgumentException("Audit output directory must not exist: " + outputDir);
        }
        Files.createDirectories(outputDir);
        String stock = launch(stockJava, groovyClasspath, ownerFile, providerFile, synthetic, false);
        requireStable(beforeSeals, ownerFile, providerFile, jarLock, groovyClasspath);
        String candidate = launch(m3Java, groovyClasspath, ownerFile, providerFile, synthetic, !synthetic);
        requireStable(beforeSeals, ownerFile, providerFile, jarLock, groovyClasspath);
        Snapshot prior = parse(stock), next = parse(candidate);
        if (prior.members.isEmpty() || next.members.isEmpty()
                || prior.extensions.isEmpty() || next.extensions.isEmpty()) {
            throw new IllegalStateException("Missing Java-member or Groovy extension evidence");
        }
        List<String> findings = diff(prior, next);
        if (prior.version.isBlank() || next.version.isBlank()
                || !prior.version.equals(next.version)) {
            findings.add("BLOCK\tGROOVY_VERSION_DRIFT\t" + prior.version + " -> " + next.version);
        }
        if (!prior.version.equals(expectedGroovyVersion)
                || !next.version.equals(expectedGroovyVersion)) {
            findings.add("BLOCK\tUNEXPECTED_GROOVY_VERSION\texpected " + expectedGroovyVersion
                    + ", observed " + prior.version + " -> " + next.version);
        }
        if (!synthetic && (prior.version.equals("SYNTHETIC_ONLY")
                || next.version.equals("SYNTHETIC_ONLY"))) {
            findings.add("BLOCK\tNOT_REAL_GROOVY\tno runtime attestation");
        }
        boolean blocked = findings.stream().anyMatch(s -> s.startsWith("BLOCK\t"));
        String status = blocked ? "BLOCK" : synthetic ? "MOCK_PASS" : "PASS";
        var report = new StringBuilder();
        report.append("status\t").append(status).append("\n")
                .append("groovy_version\t").append(next.version).append("\n")
                .append("expected_groovy_version\t").append(expectedGroovyVersion).append("\n")
                .append("evidence\t").append(synthetic ? "SYNTHETIC_ONLY" : "REAL_GROOVY_PROVIDER_SCAN").append("\n")
                .append("owners\t").append(owners.size()).append("\n")
                .append("providers\t").append(providers.size()).append("\n")
                .append("baseline_member_signatures\t").append(signatureCount(prior.members)).append("\n")
                .append("candidate_member_signatures\t").append(signatureCount(next.members)).append("\n")
                .append("candidate_extension_signatures\t").append(signatureCount(next.extensions)).append("\n");
        for (String row : findings) report.append(row).append("\n");
        // Publish complete proof, including failing runs, for reproducible review.
        Files.writeString(outputDir.resolve("baseline.tsv"), stock, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("candidate.tsv"), candidate, StandardCharsets.UTF_8);
        var pins = new StringBuilder();
        beforeSeals.forEach((name, digest) -> pins.append(digest).append("  ").append(name).append("\n"));
        Files.writeString(outputDir.resolve("input-sha256.txt"), pins, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("report.tsv"), report, StandardCharsets.UTF_8);
        System.out.print("M3_GROOVY_EXTENSION_AUDIT_" + status + "\n" + report);
        if (status.equals("BLOCK")) System.exit(1);
    }

    private static int signatureCount(Map<String, Set<String>> map) {
        return map.values().stream().mapToInt(Set::size).sum();
    }

    private static void requireStable(Map<String, String> expected, Path ownerFile,
            Path providerFile, Path jarLock, String classpath) throws IOException {
        if (!expected.equals(inputSeals(ownerFile, providerFile, jarLock, classpath))) {
            throw new IllegalStateException("Groovy audit source changed during comparison");
        }
    }

    private static Map<String, String> inputSeals(Path ownerFile, Path providerFile,
            Path jarLock, String classpath) throws IOException {
        var result = new TreeMap<String, String>();
        result.put("owners.txt", sha(ownerFile));
        result.put("providers.txt", sha(providerFile));
        result.put("jar-lock.tsv", sha(jarLock));
        classpathSeals(classpath).forEach((name, digest) -> result.put("jar:" + name, digest));
        return result;
    }

    private static String launch(Path javaExe, String jars, Path owners, Path providers,
            boolean synthetic, boolean requireM3) throws Exception {
        var location = GroovyExtensionCollisionAudit.class.getProtectionDomain().getCodeSource();
        if (location == null || location.getLocation() == null) {
            throw new IllegalStateException("Audit's own code source is unavailable");
        }
        Path own = Path.of(location.getLocation().toURI());
        String classpath = own + System.getProperty("path.separator") + jars;
        List<String> command = new ArrayList<>();
        command.add(javaExe.toAbsolutePath().toString());
        if (requireM3) {
            // Fail before the oracle can claim a result from an ordinary stock JDK copy.
            command.add("--add-modules");
            command.add("jdk.m3.collections");
        }
        command.addAll(List.of("-Duser.language=en", "-Duser.country=US", "-Duser.timezone=UTC",
                "-Dfile.encoding=UTF-8", "-cp", classpath,
                GroovyExtensionCollisionAudit.class.getName(),
                synthetic ? "probe-synthetic" : requireM3 ? "probe-m3" : "probe",
                owners.toAbsolutePath().toString(), providers.toAbsolutePath().toString()));
        Path captured = Files.createTempFile("m3-groovy-extension-audit-", ".log");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(captured.toFile()).start();
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IllegalStateException("Timed out probing " + javaExe);
            }
            if (Files.size(captured) > MAX_CHILD_BYTES) {
                throw new IllegalStateException("Groovy audit child exceeded output budget");
            }
            String stdout = Files.readString(captured, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Audit child failed: " + javaExe + " exit="
                        + process.exitValue() + "\n" + stdout.substring(0, Math.min(stdout.length(), 2048)));
            }
            return stdout;
        } finally {
            Files.deleteIfExists(captured);
        }
    }

    private static Snapshot snapshot(List<String> owners, List<Class<?>> providers)
            throws ClassNotFoundException {
        Snapshot result = new Snapshot();
        // Inspect donor metadata once, then fan out to all receiver classes.
        List<Method> donorMethods = new ArrayList<>();
        for (Class<?> provider : providers) {
            for (Method method : provider.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())
                        && Modifier.isStatic(method.getModifiers())
                        && !method.isBridge() && !method.isSynthetic()
                        && method.getParameterCount() > 0) donorMethods.add(method);
            }
        }
        for (String name : owners) {
            Class<?> receiver = loadReceiver(name);
            for (Method method : receiver.getMethods()) {
                if (Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && !method.isBridge() && !method.isSynthetic()) {
                    result.put(result.members, key(name, method.getName(), method.getParameterCount()),
                            methodSignature(method));
                }
            }
            for (Method method : donorMethods) {
                Class<?> extensionReceiver = method.getParameterTypes()[0];
                if (!receiverCompatible(extensionReceiver, receiver)) continue;
                result.put(result.extensions, key(name, method.getName(), method.getParameterCount() - 1),
                        methodSignature(method));
            }
        }
        return result;
    }

    private static Class<?> loadReceiver(String name) throws ClassNotFoundException {
        if (name.endsWith("[]")) {
            String element = name.substring(0, name.length() - 2);
            Class<?> component = switch (element) {
                case "boolean" -> boolean.class;
                case "byte" -> byte.class;
                case "short" -> short.class;
                case "char" -> char.class;
                case "int" -> int.class;
                case "long" -> long.class;
                case "float" -> float.class;
                case "double" -> double.class;
                default -> loadReceiver(element);
            };
            return Array.newInstance(component, 0).getClass();
        }
        return Class.forName(name, false, GroovyExtensionCollisionAudit.class.getClassLoader());
    }

    private static boolean receiverCompatible(Class<?> required, Class<?> owner) {
        if (required.isAssignableFrom(owner)) return true;
        if (!required.isPrimitive()) return false;
        Class<?> boxed = switch (required.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "char" -> Character.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> required;
        };
        return boxed.isAssignableFrom(owner);
    }

    private static String methodSignature(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + "("
                + String.join(",", Arrays.stream(method.getParameterTypes())
                        .map(Class::getTypeName).toList()) + "):" + method.getReturnType().getTypeName()
                + (method.isVarArgs() ? " [varargs]" : "");
    }

    private static String key(String owner, String method, int arity) {
        return owner + "\t" + method + "\t" + arity;
    }

    private static Snapshot parse(String output) {
        Snapshot result = new Snapshot();
        for (String raw : output.split("\\R", -1)) {
            if (!raw.startsWith(PREFIX)) continue;
            if (raw.startsWith(PREFIX + "V\t")) {
                String[] version = raw.split("\t", -1);
                if (version.length != 3 || version[2].isBlank() || !result.version.isEmpty()) {
                    throw new IllegalArgumentException("Duplicate or missing runtime version: " + raw);
                }
                result.version = version[2];
                continue;
            }
            String[] cells = raw.split("\t", 6);
            if (cells.length != 6 || !cells[0].equals("M3X") || cells[2].isBlank()
                    || cells[3].isBlank() || !cells[4].matches("[0-9]+") || cells[5].isBlank()) {
                throw new IllegalArgumentException("Invalid audit output row: " + raw);
            }
            Map<String, Set<String>> table = switch (cells[1]) {
                case "M" -> result.members;
                case "E" -> result.extensions;
                default -> throw new IllegalArgumentException("Unknown audit row kind: " + raw);
            };
            String rowKey = key(cells[2], cells[3], Integer.parseInt(cells[4]));
            if (!result.put(table, rowKey, cells[5])) {
                throw new IllegalArgumentException("Duplicate audit row: " + raw);
            }
        }
        return result;
    }

    private static List<String> diff(Snapshot baseline, Snapshot candidate) {
        List<String> findings = new ArrayList<>();
        if (!baseline.extensions.equals(candidate.extensions)) {
            findings.add("BLOCK\tEXTENSION_DRIFT\tGroovy provider surface differs between JVMs");
        }
        var all = new TreeSet<String>();
        all.addAll(baseline.members.keySet());
        all.addAll(candidate.members.keySet());
        for (String key : all) {
            Set<String> oldMethods = baseline.members.getOrDefault(key, Set.of());
            Set<String> newMethods = candidate.members.getOrDefault(key, Set.of());
            for (String oldMethod : oldMethods) {
                if (!newMethods.contains(oldMethod)) {
                    findings.add("BLOCK\tREMOVED_OR_CHANGED_MEMBER\t" + key + "\t" + oldMethod);
                }
            }
            for (String newMethod : newMethods) {
                if (oldMethods.contains(newMethod)) continue;
                Set<String> overlaps = extensionCandidates(candidate.extensions, key);
                if (!overlaps.isEmpty()) {
                    findings.add("BLOCK\tNEW_EXTENSION_SHADOW\t" + key + "\t" + newMethod
                            + "\t" + String.join(";", overlaps));
                } else {
                    findings.add("INFO\tNEW_NONCOLLIDING_MEMBER\t" + key + "\t" + newMethod);
                }
            }
        }
        return findings;
    }

    /** Varargs extensions may accept any argument count >= their fixed-argument count. */
    private static Set<String> extensionCandidates(Map<String, Set<String>> extensions, String memberKey) {
        Set<String> matching = new TreeSet<>(extensions.getOrDefault(memberKey, Set.of()));
        String[] member = memberKey.split("\t", -1);
        int callArity = Integer.parseInt(member[2]);
        String prefix = member[0] + "\t" + member[1] + "\t";
        for (var entry : extensions.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            int nominalArity = Integer.parseInt(entry.getKey().substring(prefix.length()));
            if (callArity < nominalArity - 1) continue;
            for (String descriptor : entry.getValue()) {
                if (descriptor.endsWith(" [varargs]")) matching.add(descriptor);
            }
        }
        return matching;
    }

    private static List<String> types(Path path) throws IOException {
        requireRegular(path, "type manifest");
        List<String> classes = new ArrayList<>();
        String previous = null;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            if (!TYPE.matcher(line).matches() || previous != null && previous.compareTo(line) >= 0) {
                throw new IllegalArgumentException("Type manifest must be sorted unique binary names: " + line);
            }
            classes.add(line);
            previous = line;
        }
        if (classes.isEmpty()) throw new IllegalArgumentException("Empty type manifest: " + path);
        return List.copyOf(classes);
    }

    private static Map<String, String> classpathSeals(String classpath) throws IOException {
        if (classpath.isBlank()) throw new IllegalArgumentException("Empty Groovy classpath");
        Map<String, String> result = new TreeMap<>();
        for (String raw : classpath.split(Pattern.quote(System.getProperty("path.separator")), -1)) {
            if (raw.isBlank()) throw new IllegalArgumentException("Empty classpath component");
            Path path = Path.of(raw);
            String name = path.getFileName().toString();
            if (!JAR_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Expected explicit jar file: " + name);
            }
            if (result.putIfAbsent(name, sha(path)) != null) {
                throw new IllegalArgumentException("Duplicate jar filename: " + name);
            }
        }
        return result;
    }

    private static Map<String, String> lock(Path file) throws IOException {
        requireRegular(file, "verified artifact lock");
        var result = new TreeMap<String, String>();
        String previous = null;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 2 || !JAR_NAME.matcher(cells[0]).matches()
                    || !SHA256.matcher(cells[1]).matches()
                    || previous != null && previous.compareTo(cells[0]) >= 0) {
                throw new IllegalArgumentException("Invalid/duplicate/unsorted jar-lock row: " + line);
            }
            result.put(cells[0], cells[1]);
            previous = cells[0];
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty jar-lock manifest");
        return result;
    }

    private static String sha(Path path) throws IOException {
        requireRegular(path, "sealed input");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var source = Files.newInputStream(path)) {
                byte[] block = new byte[65536];
                for (int n; (n = source.read(block)) != -1;) digest.update(block, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 missing", unavailable);
        }
    }

    private static void requireRegular(Path path, String description) {
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Missing or symlinked " + description + ": " + path);
        }
    }

    /** Synthetic tests are NOT Groovy oracle evidence. */
    private static void selfTest() throws Exception {
        int checks = 0;
        Snapshot actual = snapshot(List.of(SyntheticReceiver.class.getName()), List.of(SyntheticExtension.class));
        String capitalizeKey = key(SyntheticReceiver.class.getName(), "capitalize", 0);
        assertTrue(actual.members.containsKey(capitalizeKey), "synthetic member detected"); checks++;
        assertTrue(actual.extensions.containsKey(capitalizeKey), "synthetic extension detected"); checks++;
        assertTrue(actual.extensions.get(capitalizeKey).size() == 1, "extension overload classified"); checks++;
        Snapshot array = snapshot(List.of("int[]"), List.of(SyntheticExtension.class));
        assertTrue(array.extensions.containsKey(key("int[]", "first", 0)),
                "primitive array extension visible"); checks++;
        assertTrue(receiverCompatible(int.class, Integer.class), "primitive/boxed receiver match"); checks++;
        assertTrue(DEFAULT_OWNERS.size() == 35 && DEFAULT_PROVIDERS.size() == 5,
                "default inventory remains bounded and explicit"); checks++;
        assertTrue(GROOVY_RELEASES.equals(Set.of("4.0.33", "5.1.3", "6.0.0")),
                "pinned release set is exact"); checks++;
        assertTrue(new TreeSet<>(DEFAULT_OWNERS).size() == DEFAULT_OWNERS.size(),
                "unique owner inventory"); checks++;
        assertTrue(new TreeSet<>(DEFAULT_PROVIDERS).size() == DEFAULT_PROVIDERS.size(),
                "unique provider inventory"); checks++;
        assertTrue(loadReceiver("int[]") == int[].class, "primitive array receiver resolved"); checks++;
        assertTrue(loadReceiver("java.lang.String[][]") == String[][].class,
                "nested object array receiver resolved"); checks++;
        String varargsKey = key(SyntheticReceiver.class.getName(), "all", 0);
        assertTrue(!extensionCandidates(actual.extensions, varargsKey).isEmpty(),
                "varargs extension can shadow zero-argument Java member"); checks++;
        assertTrue(!extensionCandidates(actual.extensions,
                key(SyntheticReceiver.class.getName(), "all", 7)).isEmpty(),
                "varargs extension can shadow higher-arity Java member"); checks++;
        String sourceKey = key("java.lang.String", "capitalize", 0);
        Snapshot before = new Snapshot(), after = new Snapshot();
        before.put(before.extensions, sourceKey, "groovy.gdk#capitalize(java.lang.String):java.lang.String");
        after.put(after.extensions, sourceKey, "groovy.gdk#capitalize(java.lang.String):java.lang.String");
        after.put(after.members, sourceKey, "java.lang.String#capitalize():java.lang.String");
        assertTrue(diff(before, after).stream().anyMatch(s -> s.contains("NEW_EXTENSION_SHADOW")),
                "new Java member is a blocking extension shadow"); checks++;
        before.put(before.members, sourceKey, "java.lang.String#capitalize():java.lang.String");
        assertTrue(diff(before, after).isEmpty(), "known identical existing collision"); checks++;
        after.members.clear();
        assertTrue(diff(before, after).stream().anyMatch(s -> s.contains("REMOVED_OR_CHANGED_MEMBER")),
                "lost Java method is blocking"); checks++;
        before.members.clear();
        after.put(after.members, key("java.lang.String", "unrelated", 0),
                "java.lang.String#unrelated():java.lang.String");
        assertTrue(diff(before, after).stream().anyMatch(s -> s.contains("INFO\tNEW_NONCOLLIDING_MEMBER")),
                "unrelated addition is informational"); checks++;
        after.extensions.clear();
        assertTrue(diff(before, after).stream().anyMatch(s -> s.contains("EXTENSION_DRIFT")),
                "different Groovy provider is blocking"); checks++;
        assertThrows(() -> parse("M3X\tM\tjava.lang.String\tcap\t0\tfn\nM3X\tM\tjava.lang.String\tcap\t0\tfn\n"),
                "duplicate row rejected"); checks++;
        assertThrows(() -> parse("M3X\tM\tjava.lang.String\tcap\tx\tfn\n"),
                "malformed arity rejected"); checks++;
        assertThrows(() -> parse("M3X\tV\t4.0.33\nM3X\tV\t6.0.0\n"),
                "duplicate Groovy attestation rejected"); checks++;
        Path types = Files.createTempFile("groovy-audit-types", ".txt");
        try {
            Files.writeString(types, "java.lang.String\njava.util.List\n", StandardCharsets.UTF_8);
            assertTrue(types(types).size() == 2, "sorted manifest parsed"); checks++;
            Files.writeString(types, "java.util.List\njava.lang.String\n", StandardCharsets.UTF_8);
            assertThrows(() -> types(types), "unsorted manifest rejected"); checks++;
            Files.writeString(types, "java.lang.String\njava.lang.String\n", StandardCharsets.UTF_8);
            assertThrows(() -> types(types), "duplicate manifest rejected"); checks++;
        } finally {
            Files.deleteIfExists(types);
        }
        if (ModuleLayer.boot().findModule("jdk.m3.collections").isEmpty()) {
            try {
                probe(Path.of("missing-owners-for-negative-test"),
                        Path.of("missing-providers-for-negative-test"), false, true);
                throw new AssertionError("a stock JVM accepted M3 module proof");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("jdk.m3.collections"),
                        "stock JVM must refuse M3 module attestation"); checks++;
            }
        } else {
            assertTrue(ModuleLayer.boot().findModule("jdk.m3.collections").get()
                            .getName().equals("jdk.m3.collections"),
                    "resolved M3 boot module has expected identity"); checks++;
        }
        Path jar = Files.createTempFile("groovy-audit-selftest", ".jar");
        Path jarLock = Files.createTempFile("groovy-audit-lock", ".tsv");
        try {
            Files.writeString(jar, "mock-not-groovy", StandardCharsets.UTF_8);
            String name = jar.getFileName().toString();
            Files.writeString(jarLock, name + "\t" + sha(jar) + "\n", StandardCharsets.UTF_8);
            assertTrue(lock(jarLock).equals(classpathSeals(jar.toString())), "jar lock matches bytes"); checks++;
            Files.writeString(jarLock, name + "\t" + "0".repeat(64) + "\n", StandardCharsets.UTF_8);
            assertTrue(!lock(jarLock).equals(classpathSeals(jar.toString())), "tampered lock refuses admission"); checks++;
        } finally {
            Files.deleteIfExists(jar);
            Files.deleteIfExists(jarLock);
        }
        System.out.println("M3_GROOVY_EXTENSION_SELFTEST_PASS checks=" + checks);
    }

    private static void assertThrows(ThrowingAction action, String name) throws Exception {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(name);
    }
    private static void assertTrue(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
    public static final class SyntheticReceiver {
        public String capitalize() { return "member"; }
    }
    public static final class SyntheticExtension {
        public static String capitalize(SyntheticReceiver receiver) { return "extension"; }
        public static String other(SyntheticReceiver receiver, int number) { return "other"; }
        public static String all(SyntheticReceiver receiver, String... args) { return "all"; }
        public static int first(int[] values) { return values[0]; }
        public void notExtension(SyntheticReceiver receiver) { }
    }
    private static final class Snapshot {
        String version = "";
        final Map<String, Set<String>> members = new TreeMap<>();
        final Map<String, Set<String>> extensions = new TreeMap<>();
        boolean put(Map<String, Set<String>> target, String key, String signature) {
            return target.computeIfAbsent(key, ignored -> new TreeSet<>()).add(signature);
        }
    }
}
