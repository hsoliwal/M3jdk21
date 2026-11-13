/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads and writes normalized profile data. */
public final class M3ProfileIO {
    private static final String HEADER = "kind\tsymbol\tcount\tfirst_ordinal";

    private M3ProfileIO() {
    }

    /** Writes profile.tsv deterministically. */
    public static void write(Path file, M3Profile profile) throws IOException {
        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        for (M3Profile.Observation row : profile.observations()) {
            builder.append(row.kind()).append('\t')
                    .append(escape(row.symbol())).append('\t')
                    .append(row.count()).append('\t')
                    .append(row.firstOrdinal()).append('\n');
        }
        M3Files.writeUtf8(file, builder.toString());
    }

    /** Reads profile.tsv. */
    public static M3Profile read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return M3Profile.empty();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new IOException("Invalid profile header in " + file);
        }
        Map<String, Long> ranks = new LinkedHashMap<>();
        Map<String, Long> classSamples = new LinkedHashMap<>();
        Map<String, Long> methodSamples = new LinkedHashMap<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 4) {
                throw new IOException("Invalid profile row " + (index + 1) + " in " + file);
            }
            String symbol = unescape(fields[1]);
            long count = parseLong(fields[2], file, index + 1);
            long first = parseLong(fields[3], file, index + 1);
            if (count < 1 || first < 0) {
                throw new IOException("Invalid profile counters at row " + (index + 1) + " in " + file);
            }
            switch (fields[0]) {
                case "CLASS_LOAD" -> ranks.merge(symbol, first, Math::min);
                case "CLASS_SAMPLE" -> classSamples.merge(symbol, count, Math::addExact);
                case "METHOD_SAMPLE" -> methodSamples.merge(symbol, count, Math::addExact);
                default -> throw new IOException("Unknown profile kind at row " + (index + 1) + ": " + fields[0]);
            }
        }
        return new M3Profile(ranks, classSamples, methodSamples);
    }

    /** Writes loaded classes in startup order. */
    public static void writeLoadedClasses(Path file, M3Profile profile) throws IOException {
        StringBuilder builder = new StringBuilder();
        profile.loadedClassRanks().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> builder.append(entry.getKey()).append('\n'));
        M3Files.writeUtf8(file, builder.toString());
    }

    /** Reads class names from a HotSpot DumpLoadedClassList file. */
    public static M3Profile readHotSpotClassList(Path classList) throws IOException {
        if (!Files.isRegularFile(classList)) {
            return M3Profile.empty();
        }
        Map<String, Long> ranks = new LinkedHashMap<>();
        long ordinal = 0;
        for (String line : Files.readAllLines(classList, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                continue;
            }
            String token = trimmed.split("\\s+", 2)[0];
            if (token.startsWith("[")) {
                continue;
            }
            String binary = token.replace('/', '.');
            if (M3Names.isBinaryName(binary)) {
                ranks.putIfAbsent(binary, ordinal++);
            }
        }
        return new M3Profile(ranks, Map.of(), Map.of());
    }

    private static long parseLong(String value, Path file, int row) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid number at row " + row + " in " + file, exception);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                builder.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IOException("Trailing escape in profile symbol");
            }
            char escaped = value.charAt(index);
            builder.append(switch (escaped) {
                case '\\' -> '\\';
                case 't' -> '\t';
                case 'n' -> '\n';
                case 'r' -> '\r';
                default -> throw new IOException("Unknown profile escape: \\" + escaped);
            });
        }
        return builder.toString();
    }
}
