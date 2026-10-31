/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.collections.fill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template-first fill of the node-based scaffold families (recipe ARRAY_MAP_FILL): one template per family,
 * {@code //#if FLAG}, {@code //#else} and {@code //#end} line markers select sections per member, {@code ${NAME}}
 * placeholders take the member's columns from MEMBERS.tsv. The output replaces the scaffold source of each member
 * module; a second run over its own output is byte-identical (fixed point).
 *
 * <p>CLI: {@code java jdk.m3.collections.fill.M3ArrayMapFill <template> <MEMBERS.tsv> <implementations-dir>}
 */
public final class M3ArrayMapFill {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z_]+)}");
    private static final String SOURCE = "src/main/java/jdk/m3/collections/apache/";

    private M3ArrayMapFill() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <template> <MEMBERS.tsv> <implementations-dir>");
        }
        String template = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        for (Map<String, String> member : members(Path.of(args[1]))) {
            Path target = Path.of(args[2], member.get("module"), SOURCE + member.get("CLASS") + ".java");
            String text = render(template, member);
            boolean same = Files.isRegularFile(target) && Files.readString(target, StandardCharsets.UTF_8).equals(text);
            Files.writeString(target, text, StandardCharsets.UTF_8);
            System.out.println((same ? "UNCHANGED " : "FILLED ") + target);
        }
    }

    /** The rows of the member table as column -> value maps. */
    static List<Map<String, String>> members(Path tsv) throws IOException {
        List<String> lines = Files.readAllLines(tsv, StandardCharsets.UTF_8);
        String[] header = lines.get(0).split("\t", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] field = line.split("\t", -1);
            Map<String, String> row = new java.util.LinkedHashMap<>();
            for (int i = 0; i < header.length; i++) {
                row.put(header[i], i < field.length ? field[i] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    /** The template with the member's sections kept and its placeholders filled. */
    static String render(String template, Map<String, String> member) {
        Set<String> flags = Set.of(member.getOrDefault("flags", "-").split(","));
        StringBuilder out = new StringBuilder(template.length());
        Deque<boolean[]> stack = new ArrayDeque<>(); // {this section on, every enclosing section on}
        boolean on = true;
        for (String line : template.split("\n", -1)) {
            String marker = line.strip();
            if (marker.startsWith("//#if ")) {
                boolean cond = flags.contains(marker.substring(6).strip());
                stack.push(new boolean[] {cond, on});
                on = on && cond;
            } else if (marker.equals("//#else")) {
                boolean[] top = stack.peek();
                top[0] = !top[0];
                on = top[1] && top[0];
            } else if (marker.equals("//#end")) {
                on = stack.pop()[1];
            } else if (on) {
                out.append(fill(line, member)).append('\n');
            }
        }
        if (!stack.isEmpty()) {
            throw new IllegalStateException("unclosed //#if in template");
        }
        return out.substring(0, out.length() - 1);
    }

    private static String fill(String line, Map<String, String> member) {
        Matcher m = PLACEHOLDER.matcher(line);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = member.get(m.group(1));
            if (value == null) {
                throw new IllegalStateException("no column " + m.group(1));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        return m.appendTail(out).toString();
    }
}
