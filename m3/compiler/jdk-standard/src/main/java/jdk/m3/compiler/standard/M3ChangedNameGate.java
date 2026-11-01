// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.compiler.standard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Refuses naming findings introduced on added Java source lines. */
public final class M3ChangedNameGate {
    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern HUNK = Pattern.compile(
            "^@@ -[0-9]+(?:,[0-9]+)? \\+([0-9]+)(?:,([0-9]+))? @@.*$");

    private M3ChangedNameGate() {
    }

    /**
     * Checks added M3-owned Java declarations between two exact commits.
     *
     * @param args command-line arguments
     * @throws Exception if inventory, parsing, or Git inspection fails
     */
    public static void main(String[] args) throws Exception {
        Configuration configuration = parseArguments(args);
        String diff = gitDiff(configuration.root(), configuration.base(), configuration.head());
        Map<Path, List<LineRange>> added = addedLines(diff);
        List<Path> sources = added.keySet().stream()
                .filter(M3JdkStandardCensus::isM3OwnedSource)
                .sorted()
                .toList();
        if (sources.isEmpty()) {
            writeReport(configuration.report(), List.of());
            System.out.println("M3_CHANGED_NAME_GATE_PASS files=0 findings=0");
            return;
        }

        Path allReport = configuration.report().resolveSibling(
                configuration.report().getFileName() + ".all.tsv");
        M3JdkStandardCensus.Result result = M3JdkStandardCensus.run(
                configuration.root(), sources, allReport, configuration.m3Scheme());
        List<String> introduced = filterAddedFindings(result.findings(), added);
        writeReport(configuration.report(), introduced);
        if (!introduced.isEmpty()) {
            System.err.println("M3_CHANGED_NAME_GATE_REFUSED findings=" + introduced.size());
            introduced.forEach(System.err::println);
            System.exit(1);
        }
        System.out.println("M3_CHANGED_NAME_GATE_PASS files=" + sources.size()
                + " findings=0 parsedFindings=" + result.findingCount());
    }

    static Map<Path, List<LineRange>> addedLines(String diff) {
        Map<Path, List<LineRange>> result = new LinkedHashMap<>();
        Path current = null;
        for (String line : diff.split("\\R", -1)) {
            if (line.startsWith("+++ ")) {
                String value = line.substring(4);
                if (value.equals("/dev/null")) {
                    current = null;
                } else {
                    current = diffPath(value);
                    result.computeIfAbsent(current, ignored -> new ArrayList<>());
                }
                continue;
            }
            Matcher matcher = HUNK.matcher(line);
            if (current != null && matcher.matches()) {
                long start = Long.parseLong(matcher.group(1));
                long count = matcher.group(2) == null ? 1L : Long.parseLong(matcher.group(2));
                if (count > 0) {
                    result.get(current).add(new LineRange(start, start + count - 1L));
                }
            }
        }
        result.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return result;
    }

    static List<String> filterAddedFindings(List<String> findings,
            Map<Path, List<LineRange>> added) {
        List<String> result = new ArrayList<>();
        for (String row : findings) {
            String[] fields = row.split("\\t", 5);
            if (fields.length != 5) {
                throw new IllegalArgumentException("invalid finding row: " + row);
            }
            Path source = Path.of(fields[0]);
            long line = Long.parseLong(fields[1]);
            List<LineRange> ranges = added.getOrDefault(source, List.of());
            if (ranges.stream().anyMatch(range -> range.contains(line))) {
                result.add(row);
            }
        }
        result.sort(M3JdkStandardCensus::compareFindingRows);
        return List.copyOf(result);
    }

    private static Path diffPath(String value) {
        String path = value.startsWith("b/") ? value.substring(2) : value;
        Path result = Path.of(path).normalize();
        if (result.isAbsolute() || result.startsWith("..")) {
            throw new IllegalArgumentException("diff path escapes repository: " + value);
        }
        return result;
    }

    private static String gitDiff(Path root, String base, String head)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "git", "diff", "--unified=0", "--no-ext-diff", "--no-color",
                "--find-renames=100%", base + "..." + head, "--", "*.java")
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("git diff failed with exit " + exit + ":\n" + output);
        }
        return output;
    }

    private static void writeReport(Path report, List<String> rows) throws IOException {
        Path absolute = report.toAbsolutePath().normalize();
        if (absolute.getParent() != null) {
            Files.createDirectories(absolute.getParent());
        }
        StringBuilder body = new StringBuilder("file\tline\tcolumn\trule\tmessage\n");
        rows.forEach(row -> body.append(row).append('\n'));
        Files.writeString(absolute, body, StandardCharsets.UTF_8);
    }

    private static Configuration parseArguments(String[] args) {
        Path root = null;
        Path report = null;
        String base = null;
        String head = null;
        boolean m3Scheme = false;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--root" -> root = Path.of(requireValue(args, ++index, argument));
                case "--base" -> base = requireValue(args, ++index, argument);
                case "--head" -> head = requireValue(args, ++index, argument);
                case "--report" -> report = Path.of(requireValue(args, ++index, argument));
                case "--scheme=m3" -> m3Scheme = true;
                default -> throw new IllegalArgumentException("unknown argument: " + argument);
            }
        }
        if (root == null || report == null || !isCommit(base) || !isCommit(head)) {
            throw new IllegalArgumentException(
                    "usage: M3ChangedNameGate --root <repo> --base <sha> --head <sha> "
                            + "--report <tsv> [--scheme=m3]");
        }
        return new Configuration(root.toAbsolutePath().normalize(), base, head,
                report.toAbsolutePath().normalize(), m3Scheme);
    }

    private static boolean isCommit(String value) {
        return value != null && COMMIT.matcher(value).matches();
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing value for " + option);
        }
        return args[index];
    }

    record LineRange(long first, long last) {
        LineRange {
            if (first < 1 || last < first) {
                throw new IllegalArgumentException("invalid line range " + first + ".." + last);
            }
        }

        boolean contains(long line) {
            return line >= first && line <= last;
        }
    }

    private record Configuration(Path root, String base, String head, Path report,
            boolean m3Scheme) {
    }
}
