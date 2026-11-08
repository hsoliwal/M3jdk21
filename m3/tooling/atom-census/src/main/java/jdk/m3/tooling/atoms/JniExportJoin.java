/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Joins existing Java native-declaration and C/C++ JNI export candidate inventories.
 * This tool deliberately does not parse either language or establish native ABI authority.
 * Immutable javac -h headers, Java UTF-16 slices, and native source hashes provide
 * strictly bounded source-backed candidate evidence only.
 */
public final class JniExportJoin {
    /** The result schema is stable for deterministic source-identity joins. */
    public static final String HEADER = "native_atom_id\trepo\tjava_path\tjava_slice_sha256"
            + "\tjava_signature\tjvm_descriptor\texpected_symbol\tnative_path"
            + "\tnative_sha256\tnative_line\theader_path\theader_sha256\tstatus";
    private static final String DECL_HEADER = "native_atom_id\trepo\tpath\towner\tmethod\tjava_signature"
            + "\tstart_utf16\tend_utf16\tsource_sha256\tshort_symbol\tresolution";
    private static final String EXPORT_HEADER = "source,sourceSHA256,line,kind,symbol,status";
    private static final String UNVERIFIED = "UNVERIFIED_NATIVE_JNI_CONTRACT";
    private static final Pattern HEX = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern ATOM_ID = Pattern.compile("A[0-9a-f]{64}");
    private static final Pattern SYMBOL = Pattern.compile("Java_[A-Za-z0-9_]+|JNI_OnLoad|JNI_OnUnload");
    private static final Pattern HEADER_ENTRY = Pattern.compile(
            "/\\*\\s*\\*\\s*Class:\\s*([^\\r\\n]+)\\R\\s*\\*\\s*Method:\\s*([^\\r\\n]+)"
            + "\\R\\s*\\*\\s*Signature:\\s*([^\\r\\n]+)\\R\\s*\\*/"
            + "\\s*JNIEXPORT\\s+[^\\r\\n]*?\\bJNICALL\\s+"
            + "(Java_[A-Za-z0-9_]+)\\s*\\(");
    private static final int MAX_INPUT_BYTES = 64 * 1024 * 1024;
    private static final int MAX_SOURCE_BYTES = 64 * 1024 * 1024;
    private static final int MAX_HEADERS = 4096;
    private static final int MAX_ROWS = 1_000_000;

    private JniExportJoin() {
    }

    /** Source-verified Java declaration row, preserving native atom identity. */
    private record Decl(String id, String repo, String path, String owner, String method,
            String signature, int start, int end, String sha, String shortName, String resolution) {
    }

    /** Unverified lexical export from the existing Synexia native scanner. */
    private record NativeEntry(String path, String sha, int line, String kind, String symbol) {
        String identity() { return path + ':' + line + ':' + symbol; }
    }

    /** A compiler-generated header observation (not a native ABI certificate). */
    private record HeaderEntry(String path, String sha, String symbol, String descriptor) {
    }

    /**
     * CLI: {@code <java-tsv> <native-csv> <java-root> <native-root> <javac-h-dir> <output-tsv>
     * [--require-candidate-match]}.
     * @param args exactly six paths and optionally one strict candidate-matching flag
     * @throws IOException on malformed input, source drift, or unsafe publication
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 6 && args.length != 7) {
            throw new IllegalArgumentException("usage: <java.tsv> <native.csv> <java-root>"
                    + " <native-root> <javac-h-dir> <output.tsv> [--require-candidate-match]");
        }
        if (args.length == 7 && !"--require-candidate-match".equals(args[6])) {
            throw new IllegalArgumentException("unknown candidate matching option");
        }
        Path declarations = Path.of(args[0]);
        Path exports = Path.of(args[1]);
        Path javaRoot = realDirectory(Path.of(args[2]));
        Path nativeRoot = realDirectory(Path.of(args[3]));
        Path headerRoot = realDirectory(Path.of(args[4]));
        Path output = Path.of(args[5]);
        List<String> joined = join(declarations, exports, javaRoot, nativeRoot, headerRoot);
        if (args.length == 7 && joined.stream().skip(1)
                .anyMatch(row -> !row.endsWith("\tEXACT_EXPORT_CANDIDATE")
                        && !row.endsWith("\tJNI_LIFECYCLE_UNSCOPED"))) {
            throw new IllegalStateException("incomplete candidate join; no ABI admission");
        }
        publish(output, declarations, exports, javaRoot, nativeRoot, headerRoot, joined);
        System.out.println("M3_JNI_JOIN\trows=" + (joined.size() - 1)
                + "\tmatched=" + joined.stream().filter(row -> row.endsWith(
                        "\tEXACT_EXPORT_CANDIDATE")).count() + "\tstatus=CANDIDATE_ONLY");
    }

    /**
     * Produces a deterministic, immutable TSV report after source checks.
     * @param declarations the unchanged M3JDK Java native-declaration TSV
     * @param exports the unchanged Synexia native-export candidate CSV
     * @param javaRoot exact Java source root for UTF-16 slice rehashing
     * @param nativeRoot exact native source root for whole-file rehashing
     * @param headerRoot compiler-generated javac -h header directory
     * @return ordered header and candidate rows; no output is published here
     * @throws IOException on an unsafe or malformed evidence source
     */
    public static List<String> join(Path declarations, Path exports, Path javaRoot,
            Path nativeRoot, Path headerRoot) throws IOException {
        List<Decl> java = declarations(declarations, javaRoot);
        List<NativeEntry> nativeEntries = exports(exports, nativeRoot);
        Map<String, List<NativeEntry>> byNativeSymbol = new HashMap<>();
        for (NativeEntry nativeEntry : nativeEntries) {
            byNativeSymbol.computeIfAbsent(nativeEntry.symbol(), ignored -> new ArrayList<>())
                    .add(nativeEntry);
        }
        Map<String, List<HeaderEntry>> byHeaderSymbol = headers(headerRoot);
        // Two Java source atoms must never receive one supposedly unique export.
        // This can occur with malformed or partially scoped source ledgers.
        Map<String, Integer> javaSymbolCounts = new HashMap<>();
        for (Decl decl : java) {
            String descriptor = descriptor(decl.signature());
            if (descriptor != null && (decl.resolution().equals("SHORT_SYMBOL_CANDIDATE")
                    || decl.resolution().equals("OVERLOAD_REQUIRES_JAVAC_H"))) {
                String name = JniDeclarationProjection.shortName(
                        decl.owner().replace('.', '/'), decl.method());
                String expected = decl.resolution().equals("OVERLOAD_REQUIRES_JAVAC_H")
                        ? name + "__" + JniDeclarationProjection.encode(
                                descriptor.substring(1, descriptor.indexOf(')')), false)
                        : name;
                javaSymbolCounts.merge(expected, 1, Integer::sum);
            }
        }
        List<String> records = new ArrayList<>();
        Set<String> consumed = new HashSet<>();
        for (Decl decl : java) {
            String descriptor = descriptor(decl.signature());
            String expectedSymbol = "-";
            String nativePath = "-", nativeSha = "-", nativeLine = "-";
            String headerPath = "-", headerSha = "-";
            String status;
            if (!decl.resolution().equals("SHORT_SYMBOL_CANDIDATE")
                    && !decl.resolution().equals("OVERLOAD_REQUIRES_JAVAC_H")) {
                status = "JAVA_DECLARATION_UNRESOLVED";
            } else if (descriptor == null) {
                status = "JAVA_DESCRIPTOR_UNRESOLVED";
            } else {
                String derived = JniDeclarationProjection.shortName(
                        decl.owner().replace('.', '/'), decl.method());
                if (!derived.equals(decl.shortName())) {
                    throw new IOException("forged JNI short symbol for " + decl.id());
                }
                int endOfArgs = descriptor.indexOf(')');
                expectedSymbol = decl.resolution().equals("OVERLOAD_REQUIRES_JAVAC_H")
                        ? derived + "__" + JniDeclarationProjection.encode(
                                descriptor.substring(1, endOfArgs), false)
                        : derived;
                List<HeaderEntry> observedHeaders = byHeaderSymbol.getOrDefault(expectedSymbol, List.of());
                List<HeaderEntry> matchingHeaders = observedHeaders.stream()
                        .filter(entry -> entry.descriptor().equals(descriptor)).toList();
                if (javaSymbolCounts.get(expectedSymbol) != 1) {
                    status = "AMBIGUOUS_JAVA_SYMBOL";
                } else if (matchingHeaders.size() != 1 || observedHeaders.size() != 1) {
                    status = "HEADER_UNCONFIRMED";
                } else {
                    HeaderEntry header = matchingHeaders.getFirst();
                    headerPath = header.path();
                    headerSha = header.sha();
                    List<NativeEntry> nativeMatches = byNativeSymbol.getOrDefault(expectedSymbol, List.of());
                    if (nativeMatches.isEmpty()) {
                        status = "NATIVE_EXPORT_NOT_OBSERVED";
                    } else if (nativeMatches.size() != 1 || !nativeMatches.getFirst().kind().equals("JNI_ENTRY")) {
                        status = "DUPLICATE_NATIVE_EXPORT";
                        for (NativeEntry match : nativeMatches) consumed.add(match.identity());
                    } else {
                        NativeEntry match = nativeMatches.getFirst();
                        consumed.add(match.identity());
                        nativePath = match.path();
                        nativeSha = match.sha();
                        nativeLine = Integer.toString(match.line());
                        status = "EXACT_EXPORT_CANDIDATE";
                    }
                }
            }
            records.add(String.join("\t", decl.id(), decl.repo(), decl.path(), decl.sha(),
                    decl.signature(), descriptor == null ? "-" : descriptor,
                    expectedSymbol, nativePath, nativeSha, nativeLine,
                    headerPath, headerSha, status));
        }
        for (NativeEntry nativeEntry : nativeEntries) {
            if (!consumed.contains(nativeEntry.identity())) {
                String status = nativeEntry.kind().equals("JNI_LIFECYCLE")
                        ? "JNI_LIFECYCLE_UNSCOPED" : "ORPHAN_NATIVE_EXPORT";
                records.add(String.join("\t", "-", "-", "-", "-", "-", "-",
                        nativeEntry.symbol(), nativeEntry.path(), nativeEntry.sha(),
                        Integer.toString(nativeEntry.line()), "-", "-", status));
            }
        }
        records.sort(Comparator.naturalOrder());
        List<String> result = new ArrayList<>(records.size() + 1);
        result.add(HEADER);
        result.addAll(records);
        return List.copyOf(result);
    }

    /** Only exact primitive/array or fully qualified reference types are unambiguous. */
    static String descriptor(String signature) {
        int end = signature.indexOf(')');
        if (!signature.startsWith("(") || end < 1) return null;
        StringBuilder output = new StringBuilder("(");
        String parameterList = signature.substring(1, end);
        if (!parameterList.isBlank()) {
            for (String type : parameterList.split(",", -1)) {
                String desc = fieldDescriptor(type.strip(), false);
                if (desc == null) return null;
                output.append(desc);
            }
        }
        output.append(')');
        String returnDesc = fieldDescriptor(signature.substring(end + 1).strip(), true);
        return returnDesc == null ? null : output.append(returnDesc).toString();
    }

    private static String fieldDescriptor(String type, boolean allowVoid) {
        int dimensions = 0;
        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2).strip();
            dimensions++;
        }
        if (dimensions > 255) return null;
        String simple = switch (type) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "void" -> allowVoid && dimensions == 0 ? "V" : null;
            default -> null;
        };
        if (simple == null) {
            // Unqualified reference names can shadow java.lang and are intentionally refused.
            if (type.matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+")
                    && !type.contains("..")) {
                simple = "L" + type.replace('.', '/') + ";";
            } else {
                return null;
            }
        }
        return "[".repeat(dimensions) + simple;
    }

    private static List<Decl> declarations(Path table, Path root) throws IOException {
        List<String> lines = strictLines(table, DECL_HEADER);
        if (lines.size() - 1 > MAX_ROWS) throw new IOException("Java declaration row budget exceeded");
        List<Decl> result = new ArrayList<>(lines.size() - 1);
        Map<String, String> sourceImages = new HashMap<>();
        Set<String> atomIds = new HashSet<>();
        String previousKey = "";
        for (int i = 1; i < lines.size(); i++) {
            String[] f = lines.get(i).split("\t", -1);
            if (f.length != 11 || !ATOM_ID.matcher(f[0]).matches()
                    || !atomIds.add(f[0]) || !HEX.matcher(f[8]).matches()
                    || f[1].isBlank() || f[3].isBlank() || f[4].isBlank()
                    || f[5].isBlank() || !f[5].startsWith("(")
                    || !Set.of("SHORT_SYMBOL_CANDIDATE", "OVERLOAD_REQUIRES_JAVAC_H",
                            "UNRESOLVED_ANONYMOUS_CLASS", "UNRESOLVED_LOCAL_CLASS")
                            .contains(f[10])) {
                throw new IOException("invalid Java declaration identity at line " + (i + 1));
            }
            int first, last;
            try {
                first = Integer.parseInt(f[6]);
                last = Integer.parseInt(f[7]);
            } catch (NumberFormatException exception) {
                throw new IOException("invalid UTF-16 span at line " + (i + 1), exception);
            }
            String key = f[2] + "\t" + String.format(java.util.Locale.ROOT, "%010d", first)
                    + "\t" + f[0];
            if (key.compareTo(previousKey) <= 0) {
                throw new IOException("Java declaration rows are not in source order");
            }
            previousKey = key;
            Path file = guarded(root, f[2]);
            String source = sourceImages.get(f[2]);
            if (source == null) {
                source = readStrict(file, MAX_SOURCE_BYTES);
                sourceImages.put(f[2], source);
            }
            if (first < 0 || last <= first || last > source.length()
                    || !sha256(source.substring(first, last).getBytes(StandardCharsets.UTF_8)).equals(f[8])) {
                throw new IOException("Java source span SHA-256 drift: " + f[2] + ':' + first);
            }
            boolean unresolved = f[10].startsWith("UNRESOLVED_");
            if (unresolved != f[9].equals("-") || !unresolved && !SYMBOL.matcher(f[9]).matches()) {
                throw new IOException("invalid Java declaration symbol/status combination");
            }
            result.add(new Decl(f[0], f[1], f[2], f[3], f[4], f[5], first, last,
                    f[8], f[9], f[10]));
        }
        return List.copyOf(result);
    }

    private static List<NativeEntry> exports(Path file, Path root) throws IOException {
        List<String> lines = strictLines(file, EXPORT_HEADER);
        if (lines.size() - 1 > MAX_ROWS) throw new IOException("native export row budget exceeded");
        List<NativeEntry> result = new ArrayList<>(lines.size() - 1);
        Map<String, String> verified = new HashMap<>();
        Set<String> identities = new HashSet<>();
        String previous = "";
        for (int i = 1; i < lines.size(); i++) {
            List<String> f = csvFields(lines.get(i));
            if (f.size() != 6 || !HEX.matcher(f.get(1)).matches()
                    || !SYMBOL.matcher(f.get(4)).matches()
                    || !UNVERIFIED.equals(f.get(5))
                    || !Set.of("JNI_ENTRY", "JNI_LIFECYCLE").contains(f.get(3))) {
                throw new IOException("invalid native candidate row at " + (i + 1));
            }
            int line;
            try { line = Integer.parseInt(f.get(2)); }
            catch (NumberFormatException exception) { throw new IOException("invalid native line", exception); }
            if (line < 1 || (f.get(3).equals("JNI_ENTRY") != f.get(4).startsWith("Java_"))) {
                throw new IOException("native kind/symbol mismatch");
            }
            Path source = guarded(root, f.get(0));
            String actual = verified.get(f.get(0));
            if (actual == null) {
                actual = sha256(Files.readAllBytes(source));
                verified.put(f.get(0), actual);
            }
            if (!actual.equals(f.get(1))) throw new IOException("native source SHA-256 drift: " + f.get(0));
            NativeEntry entry = new NativeEntry(f.get(0), f.get(1), line, f.get(3), f.get(4));
            if (!identities.add(entry.identity())) throw new IOException("duplicate native candidate row");
            String key = entry.path() + "\t" + entry.symbol() + "\t"
                    + String.format(java.util.Locale.ROOT, "%010d", line);
            if (key.compareTo(previous) <= 0) throw new IOException("native candidate rows out of order");
            previous = key;
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static Map<String, List<HeaderEntry>> headers(Path directory) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(directory)) {
            files = stream.filter(p -> p.toString().endsWith(".h")).sorted().toList();
        }
        if (files.size() > MAX_HEADERS) throw new IOException("generated header budget exceeded");
        Map<String, List<HeaderEntry>> result = new HashMap<>();
        for (Path file : files) {
            Path relative = directory.relativize(file);
            Path checked = guarded(directory, relative.toString().replace('\\', '/'));
            byte[] bytes = Files.readAllBytes(checked);
            String source = decode(bytes);
            String digest = sha256(bytes);
            Matcher m = HEADER_ENTRY.matcher(source);
            int seen = 0;
            while (m.find()) {
                seen++;
                String symbol = m.group(4);
                String descriptor = m.group(3).strip();
                if (!descriptor.matches("\\([^\\s)]*\\)[^\\s]+") || !SYMBOL.matcher(symbol).matches()) {
                    throw new IOException("malformed compiler JNI header: " + relative);
                }
                HeaderEntry entry = new HeaderEntry(relative.toString().replace('\\', '/'), digest,
                        symbol, descriptor);
                result.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(entry);
            }
            if (seen != count(source, "JNIEXPORT")) {
                throw new IOException("unrecognized/partial javac -h header: " + relative);
            }
        }
        return result;
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int pos = 0; (pos = text.indexOf(needle, pos)) >= 0; pos += needle.length()) count++;
        return count;
    }

    private static List<String> csvFields(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); i++;
                    } else { quoted = false; closed = true; }
                } else { current.append(ch); }
            } else if (ch == '"' && current.isEmpty() && !closed) {
                quoted = true;
            } else if (ch == ',') {
                fields.add(current.toString());
                current.setLength(0);
                closed = false;
            } else if (ch == '"' || closed) {
                throw new IOException("malformed CSV quoting");
            } else {
                current.append(ch);
            }
        }
        if (quoted) throw new IOException("unterminated quoted CSV field");
        fields.add(current.toString());
        return fields;
    }

    private static List<String> strictLines(Path file, String header) throws IOException {
        String data = readStrict(file, MAX_INPUT_BYTES);
        if (data.indexOf('\r') >= 0 || !data.endsWith("\n") || data.endsWith("\n\n")
                || data.startsWith("\uFEFF")) throw new IOException("invalid UTF-8/LF table: " + file);
        List<String> lines = Arrays.asList(data.substring(0, data.length() - 1).split("\n", -1));
        if (lines.isEmpty() || !lines.getFirst().equals(header) || lines.stream().anyMatch(String::isEmpty)) {
            throw new IOException("unexpected inventory header or empty row: " + file);
        }
        return lines;
    }

    private static String readStrict(Path file, int maxBytes) throws IOException {
        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.size(file) > maxBytes) throw new IOException("unsafe or oversized file: " + file);
        return decode(Files.readAllBytes(file));
    }

    private static String decode(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            throw new IOException("invalid UTF-8 source image", exception);
        }
    }

    private static String sha256(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 required by Java21", impossible);
        }
    }

    private static Path realDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(dir)) {
            throw new IOException("missing or symbolic root: " + dir);
        }
        return dir.toRealPath();
    }

    private static Path guarded(Path root, String path) throws IOException {
        Objects.requireNonNull(root);
        if (path.isBlank() || path.startsWith("/") || path.indexOf('\\') >= 0
                || path.indexOf('\t') >= 0 || path.indexOf('\r') >= 0
                || path.indexOf('\n') >= 0 || path.indexOf(':') >= 0) {
            throw new IOException("noncanonical source path: " + path);
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("traversing source path: " + path);
            }
        }
        Path chosen = root.resolve(path).normalize();
        if (!chosen.startsWith(root)) throw new IOException("source escapes root: " + path);
        for (Path walk = chosen; !walk.equals(root); walk = walk.getParent()) {
            if (Files.isSymbolicLink(walk)) throw new IOException("symbolic source path: " + path);
        }
        if (!Files.isRegularFile(chosen, LinkOption.NOFOLLOW_LINKS)
                || Files.size(chosen) > MAX_SOURCE_BYTES) {
            throw new IOException("missing/oversized source: " + path);
        }
        return chosen;
    }

    private static void publish(Path output, Path declarations, Path exports,
            Path javaRoot, Path nativeRoot, Path headerRoot, List<String> data) throws IOException {
        Path destination = output.toAbsolutePath().normalize();
        if (destination.equals(declarations.toAbsolutePath().normalize())
                || destination.equals(exports.toAbsolutePath().normalize())
                || destination.startsWith(javaRoot) || destination.startsWith(nativeRoot)
                || destination.startsWith(headerRoot)) {
            throw new IOException("output aliases input/source evidence");
        }
        Path parent = destination.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(parent)) {
            throw new IOException("output parent missing/symbolic");
        }
        if (Files.isSymbolicLink(destination)
                || (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("unsafe output target");
        }
        Path temporary = Files.createTempFile(parent, ".m3-jni-export-join-", ".tmp");
        try {
            Files.write(temporary, data, StandardCharsets.UTF_8);
            if (Files.isSymbolicLink(destination)) throw new IOException("output changed to symlink");
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
