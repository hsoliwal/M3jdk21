// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Bounded CPU text atoms. Contracts use Unicode scalar values and explicit line framing. */
public final class TextTools {
    private TextTools() { }

    /** Stable UTF-16 lexical order, independent of the host locale. */
    public static TextFrame sort(TextFrame input, boolean reverse, Limits limits) {
        limits.check(input);
        List<TextLine> lines = new ArrayList<>(input.lines());
        Comparator<TextLine> order = Comparator.comparing(TextLine::content);
        lines.sort(reverse ? order.reversed() : order);
        return reorder(lines, limits);
    }

    /** Adjacent duplicate content only; retains the first occurrence and its ending. */
    public static TextFrame uniq(TextFrame input, Limits limits) {
        limits.check(input);
        FrameCollector output = new FrameCollector(limits);
        String previous = null;
        for (TextLine line : input.lines()) {
            if (!line.content().equals(previous)) output.accept(line);
            previous = line.content();
        }
        return output.frame();
    }

    public record Comparison(TextFrame leftOnly, TextFrame common, TextFrame rightOnly) { }

    /** Sorted multiset merge: duplicate occurrences are paired one-for-one. */
    public static Comparison comm(TextFrame left, TextFrame right, Limits limits) {
        sorted(left, limits); sorted(right, limits);
        FrameCollector a = new FrameCollector(limits), b = new FrameCollector(limits),
                c = new FrameCollector(limits);
        int i = 0, j = 0;
        while (i < left.lines().size() && j < right.lines().size()) {
            TextLine x = left.lines().get(i), y = right.lines().get(j);
            int order = x.content().compareTo(y.content());
            if (order < 0) { a.accept(x); i++; }
            else if (order > 0) { c.accept(y); j++; }
            else { b.accept(x); i++; j++; }
        }
        while (i < left.lines().size()) a.accept(left.lines().get(i++));
        while (j < right.lines().size()) c.accept(right.lines().get(j++));
        return new Comparison(a.frame(), b.frame(), c.frame());
    }

    /** Inner equijoin on first tab-separated field; stable left/right occurrence order. */
    public static TextFrame join(TextFrame left, TextFrame right, Limits limits) {
        limits.check(left); limits.check(right);
        Map<String, List<String>> build = new HashMap<>();
        for (TextLine line : right.lines()) {
            String[] fields = fields(line.content());
            build.computeIfAbsent(fields[0], ignored -> new ArrayList<>()).add(fields[1]);
        }
        FrameCollector out = new FrameCollector(limits);
        for (TextLine line : left.lines()) {
            String[] fields = fields(line.content());
            for (String value : build.getOrDefault(fields[0], List.of()))
                out.accept(new TextLine(fields[0] + "\t" + fields[1] + "\t" + value, "\n"));
        }
        return out.frame();
    }

    /** Zero-based half-open Unicode code-point range, clipped at each line's end. */
    public static TextFrame cut(TextFrame input, int from, int to, Limits limits) {
        if (from < 0 || to < from) throw new IllegalArgumentException("invalid range");
        limits.check(input);
        FrameCollector out = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            String value = line.content();
            int count = value.codePointCount(0, value.length());
            int start = value.offsetByCodePoints(0, Math.min(from, count));
            int end = value.offsetByCodePoints(start, Math.min(to, count) - Math.min(from, count));
            out.accept(new TextLine(value.substring(start, end), line.ending()));
        }
        return out.frame();
    }

    /** Row-wise paste; absent fields are empty, output uses LF. */
    public static TextFrame paste(List<TextFrame> inputs, String separator, Limits limits) {
        Objects.requireNonNull(inputs, "inputs"); Canonical.validUnicode(separator);
        if (separator.indexOf('\n') >= 0 || separator.indexOf('\r') >= 0)
            throw new IllegalArgumentException("separator contains a line ending");
        if (separator.length() > limits.maxBytes()) throw new IllegalArgumentException("separator budget");
        if (inputs.size() > limits.maxNodes()) throw new IllegalArgumentException("input count budget");
        int rows = 0;
        for (TextFrame frame : inputs) { limits.check(frame); rows = Math.max(rows, frame.lines().size()); }
        FrameCollector out = new FrameCollector(limits);
        for (int row = 0; row < rows; row++) {
            StringBuilder text = new StringBuilder();
            for (int column = 0; column < inputs.size(); column++) {
                if (column != 0) text.append(separator);
                var lines = inputs.get(column).lines();
                if (row < lines.size()) text.append(lines.get(row).content());
                if (text.length() > limits.maxBytes()) throw new IllegalArgumentException("row budget");
            }
            out.accept(new TextLine(text.toString(), "\n"));
        }
        return out.frame();
    }

    /** Literal scalar translation; empty target deletes, short target repeats its final scalar. */
    public static TextFrame tr(TextFrame input, String from, String to, Limits limits) {
        limits.check(input); Canonical.validUnicode(from); Canonical.validUnicode(to);
        if ((long) from.length() + to.length() > limits.maxBytes())
            throw new IllegalArgumentException("translation alphabet budget");
        if (from.indexOf('\n') >= 0 || from.indexOf('\r') >= 0 || to.indexOf('\n') >= 0 || to.indexOf('\r') >= 0)
            throw new IllegalArgumentException("translation is within line content");
        int[] source = from.codePoints().toArray(), target = to.codePoints().toArray();
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < source.length; i++)
            map.put(source[i], target.length == 0 ? -1 : target[Math.min(i, target.length - 1)]);
        FrameCollector out = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            StringBuilder text = new StringBuilder();
            line.content().codePoints().forEach(value -> {
                int replacement = map.getOrDefault(value, value);
                if (replacement >= 0) text.appendCodePoint(replacement);
            });
            out.accept(new TextLine(text.toString(), line.ending()));
        }
        return out.frame();
    }

    /** Breaks by Unicode scalar count, not display width; preserves each final original ending. */
    public static TextFrame fold(TextFrame input, int width, Limits limits) {
        if (width < 1) throw new IllegalArgumentException("width must be positive");
        limits.check(input);
        FrameCollector out = new FrameCollector(limits);
        for (TextLine line : input.lines()) {
            String text = line.content(); int start = 0, remaining = text.codePointCount(0, text.length());
            while (remaining > width) {
                int end = text.offsetByCodePoints(start, width);
                out.accept(new TextLine(text.substring(start, end), "\n"));
                start = end; remaining -= width;
            }
            out.accept(new TextLine(text.substring(start), line.ending()));
        }
        return out.frame();
    }

    public static TextFrame nl(TextFrame input, long first, long step, Limits limits) {
        limits.check(input); FrameCollector out = new FrameCollector(limits); long number = first;
        for (int i = 0; i < input.lines().size(); i++) {
            TextLine line = input.lines().get(i);
            out.accept(new TextLine(number + "\t" + line.content(), line.ending()));
            if (i + 1 < input.lines().size()) number = Math.addExact(number, step);
        }
        return out.frame();
    }

    public static TextFrame shuf(TextFrame input, long seed, Limits limits) {
        limits.check(input); List<TextLine> lines = new ArrayList<>(input.lines()); Random random = new Random(seed);
        for (int i = lines.size(); i > 1; i--) {
            int j = random.nextInt(i); TextLine value = lines.get(i - 1);
            lines.set(i - 1, lines.get(j)); lines.set(j, value);
        }
        return reorder(lines, limits);
    }

    /** In-memory line partitions, with no implicit filesystem writes. */
    public static List<TextFrame> split(TextFrame input, int linesPerPart, Limits limits) {
        if (linesPerPart < 1) throw new IllegalArgumentException("part size must be positive");
        limits.check(input); List<TextFrame> parts = new ArrayList<>();
        for (int start = 0; start < input.lines().size();) {
            int end = start + Math.min(linesPerPart, input.lines().size() - start);
            parts.add(new TextFrame(input.lines().subList(start, end))); start = end;
        }
        return List.copyOf(parts);
    }

    /** Split before a matching line, dropping empty partitions; Java regex semantics. */
    public static List<TextFrame> csplit(TextFrame input, Pattern boundary, Limits limits) {
        limits.check(input); Objects.requireNonNull(boundary, "boundary");
        List<TextFrame> parts = new ArrayList<>(); int start = 0;
        for (int i = 0; i < input.lines().size(); i++) {
            if (boundary.matcher(input.lines().get(i).content()).find() && i > start) {
                parts.add(new TextFrame(input.lines().subList(start, i))); start = i;
            }
        }
        if (start < input.lines().size()) parts.add(new TextFrame(input.lines().subList(start, input.lines().size())));
        return List.copyOf(parts);
    }

    /** One input line is one literal argument. The host supplies execution; no implicit shell. */
    public static void xargs(TextFrame input, List<String> prefix, int batchSize,
                             Consumer<List<String>> execute, Limits limits) {
        limits.check(input); Objects.requireNonNull(execute, "execute");
        List<String> fixed = List.copyOf(prefix);
        if (batchSize < 1 || fixed.size() > limits.maxNodes()) throw new IllegalArgumentException("argument budget");
        long bytes = input.byteCount();
        for (String argument : fixed) {
            Canonical.validUnicode(argument);
            if (argument.length() > limits.maxBytes() - bytes) throw new IllegalArgumentException("argument byte budget");
            long size = argument.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (size > limits.maxBytes() - bytes) throw new IllegalArgumentException("argument byte budget");
            bytes += size;
        }
        for (int start = 0; start < input.lines().size();) {
            int end = start + Math.min(batchSize, input.lines().size() - start);
            List<String> args = new ArrayList<>(fixed);
            for (int i = start; i < end; i++) args.add(input.lines().get(i).content());
            execute.accept(List.copyOf(args)); start = end;
        }
    }

    /** Deterministic lexical topological order; each nonblank line contains exactly two tokens. */
    public static TextFrame tsort(TextFrame input, Limits limits) {
        limits.check(input); Map<String, java.util.Set<String>> edges = new java.util.TreeMap<>();
        Map<String, Integer> incoming = new HashMap<>(); int edgeCount = 0;
        for (TextLine line : input.lines()) {
            if (line.content().isBlank()) continue;
            String[] pair = line.content().strip().split("\\s+");
            if (pair.length != 2) throw new IllegalArgumentException("expected a pair");
            for (String node : pair) {
                if (!edges.containsKey(node)) {
                    if (edges.size() == limits.maxNodes()) throw new IllegalArgumentException("node budget");
                    edges.put(node, new HashSet<>()); incoming.put(node, 0);
                }
            }
            if (!pair[0].equals(pair[1]) && !edges.get(pair[0]).contains(pair[1])) {
                if (edgeCount == limits.maxEdges()) throw new IllegalArgumentException("edge budget");
                edges.get(pair[0]).add(pair[1]); edgeCount++;
                incoming.merge(pair[1], 1, Math::addExact);
            }
        }
        PriorityQueue<String> ready = new PriorityQueue<>();
        incoming.forEach((node, count) -> { if (count == 0) ready.add(node); });
        List<TextLine> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.remove(); order.add(new TextLine(node, "\n"));
            for (String next : edges.get(node)) if (incoming.merge(next, -1, Integer::sum) == 0) ready.add(next);
        }
        if (order.size() != incoming.size()) throw new IllegalArgumentException("cycle");
        return collect(order, limits);
    }

    /** Exact full-file unified diff for LF text; no change is an empty frame. */
    public static TextFrame diff(TextFrame before, TextFrame after, Limits limits) {
        lf(before, limits); lf(after, limits);
        if (before.equals(after)) return new TextFrame(List.of());
        FrameCollector out = new FrameCollector(limits);
        out.accept(new TextLine("--- before", "\n")); out.accept(new TextLine("+++ after", "\n"));
        out.accept(new TextLine("@@ -" + range(before) + " +" + range(after) + " @@", "\n"));
        diffLines(before, '-', out); diffLines(after, '+', out);
        return out.frame();
    }

    /** Applies only this exact full-file diff form; source drift, fuzz and malformed hunks refuse. */
    public static TextFrame patch(TextFrame input, TextFrame patch, Limits limits) {
        lf(input, limits); lf(patch, limits);
        if (patch.lines().isEmpty()) return input;
        var lines = patch.lines();
        if (lines.size() < 3 || !lines.get(0).content().equals("--- before")
                || !lines.get(1).content().equals("+++ after")) throw new IllegalArgumentException("patch header");
        List<TextLine> before = new ArrayList<>(), after = new ArrayList<>(); boolean added = false;
        for (int i = 3; i < lines.size(); i++) {
            String line = lines.get(i).content();
            if (line.isEmpty() || (line.charAt(0) != '-' && line.charAt(0) != '+'))
                throw new IllegalArgumentException("patch line");
            boolean plus = line.charAt(0) == '+';
            if (!plus && added) throw new IllegalArgumentException("hunk order");
            added |= plus;
            String ending = "\n";
            if (i + 1 < lines.size() && lines.get(i + 1).content().equals("\\ No newline at end of file")) {
                ending = ""; i++;
            }
            (plus ? after : before).add(new TextLine(line.substring(1), ending));
        }
        TextFrame source = collect(before, limits), result = collect(after, limits);
        lf(source, limits); lf(result, limits);
        if (!lines.get(2).content().equals("@@ -" + range(source) + " +" + range(result) + " @@"))
            throw new IllegalArgumentException("hunk counts");
        if (!source.equals(input)) throw new IllegalArgumentException("patch source drift");
        return result;
    }

    private static TextFrame reorder(List<TextLine> lines, Limits limits) {
        // An original unterminated final line can move into the middle.
        // Insert only the separator needed to keep that record distinct.
        for (int i = 0; i + 1 < lines.size(); i++) {
            TextLine line = lines.get(i);
            if (line.ending().isEmpty()) lines.set(i, new TextLine(line.content(), "\n"));
        }
        return collect(lines, limits);
    }
    private static TextFrame collect(List<TextLine> lines, Limits limits) {
        FrameCollector out = new FrameCollector(limits); lines.forEach(out); return out.frame();
    }
    private static String[] fields(String value) {
        int tab = value.indexOf('\t');
        return tab < 0 ? new String[] {value, ""} : new String[] {value.substring(0, tab), value.substring(tab + 1)};
    }
    private static void sorted(TextFrame frame, Limits limits) {
        limits.check(frame);
        for (int i = 1; i < frame.lines().size(); i++)
            if (frame.lines().get(i - 1).content().compareTo(frame.lines().get(i).content()) > 0)
                throw new IllegalArgumentException("comm requires sorted inputs");
    }
    private static void lf(TextFrame frame, Limits limits) {
        limits.check(frame);
        for (int i = 0; i < frame.lines().size(); i++) {
            String ending = frame.lines().get(i).ending();
            String content = frame.lines().get(i).content();
            if (content.indexOf('\n') >= 0 || content.indexOf('\r') >= 0)
                throw new IllegalArgumentException("embedded line ending");
            if (!(ending.equals("\n") || (ending.isEmpty() && i == frame.lines().size() - 1)))
                throw new IllegalArgumentException("diff/patch requires LF text");
        }
    }
    private static String range(TextFrame frame) {
        int size = frame.lines().size(); return (size == 0 ? 0 : 1) + "," + size;
    }
    private static void diffLines(TextFrame frame, char prefix, FrameCollector out) {
        for (TextLine line : frame.lines()) {
            out.accept(new TextLine(prefix + line.content(), "\n"));
            if (line.ending().isEmpty()) out.accept(new TextLine("\\ No newline at end of file", "\n"));
        }
    }
}
