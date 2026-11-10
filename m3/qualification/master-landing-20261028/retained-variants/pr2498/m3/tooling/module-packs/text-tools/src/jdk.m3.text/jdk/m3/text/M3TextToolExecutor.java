// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Thin deterministic dispatcher over the canonical text owners.
 *
 * <p>This is not another scheduler. It validates one request, invokes exactly one bounded
 * atom, and emits a content-addressed result. Effectful filesystem/process tools are
 * deliberately refused here and remain explicit host bindings.</p>
 */
public final class M3TextToolExecutor {
    private M3TextToolExecutor() { }

    public static M3TextToolResult execute(M3TextToolRequest request, Limits limits) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(limits, "limits");
        M3TextToolCatalog.Spec spec = M3TextToolCatalog.require(request.tool());
        admit(request, spec, limits);
        if (spec.effect() != M3TextToolCatalog.Effect.PURE) {
            throw new IllegalArgumentException(
                    "effectful text tool requires explicit host binding: " + spec.id());
        }

        List<TextFrame> frames;
        List<List<String>> batches = List.of();
        List<TextFrame> input = request.inputs();
        List<String> args = request.arguments();

        switch (spec.id()) {
            case "cat" -> {
                noArgs(args);
                frames = List.of(M3TextOperators.cat(input, limits));
            }
            case "sort" -> {
                boolean reverse = args.isEmpty() ? false
                        : args.size() == 1 && args.getFirst().equals("reverse");
                if (!args.isEmpty() && !reverse) throw usage(spec.id());
                frames = List.of(TextTools.sort(input.getFirst(), reverse, limits));
            }
            case "uniq" -> {
                noArgs(args);
                frames = List.of(TextTools.uniq(input.getFirst(), limits));
            }
            case "uniq-count" -> {
                noArgs(args);
                frames = List.of(M3TextOperators.uniqCounted(input.getFirst(), limits));
            }
            case "head" -> {
                exactArgs(args, 1);
                frames = List.of(M3TextOperators.head(
                        input.getFirst(), nonnegativeInt(args.getFirst()), limits));
            }
            case "tail" -> {
                exactArgs(args, 1);
                frames = List.of(M3TextOperators.tail(
                        input.getFirst(), nonnegativeInt(args.getFirst()), limits));
            }
            case "grep-literal" -> {
                MatchArguments match = matchArguments(args);
                frames = List.of(M3TextOperators.grepLiteral(
                        input.getFirst(), match.pattern(), match.invert(), limits));
            }
            case "grep-regex" -> {
                MatchArguments match = matchArguments(args);
                frames = List.of(M3TextOperators.grepRegex(
                        input.getFirst(), Pattern.compile(match.pattern()), match.invert(), limits));
            }
            case "wc" -> {
                M3TextOperators.CountMode mode = countMode(args);
                frames = List.of(M3TextOperators.wc(input.getFirst(), mode, limits));
            }
            case "cut" -> {
                exactArgs(args, 2);
                frames = List.of(TextTools.cut(input.getFirst(),
                        nonnegativeInt(args.get(0)), nonnegativeInt(args.get(1)), limits));
            }
            case "tr" -> {
                exactArgs(args, 2);
                frames = List.of(TextTools.tr(input.getFirst(), args.get(0), args.get(1), limits));
            }
            case "fold" -> {
                exactArgs(args, 1);
                frames = List.of(TextTools.fold(input.getFirst(), positiveInt(args.getFirst()), limits));
            }
            case "nl" -> {
                if (!args.isEmpty() && args.size() != 2) throw usage(spec.id());
                long first = args.isEmpty() ? 1 : longValue(args.get(0));
                long step = args.isEmpty() ? 1 : longValue(args.get(1));
                frames = List.of(TextTools.nl(input.getFirst(), first, step, limits));
            }
            case "shuf" -> {
                exactArgs(args, 1);
                frames = List.of(TextTools.shuf(input.getFirst(), longValue(args.getFirst()), limits));
            }
            case "tsort" -> {
                noArgs(args);
                frames = List.of(TextTools.tsort(input.getFirst(), limits));
            }
            case "comm" -> {
                noArgs(args);
                TextTools.Comparison comparison = TextTools.comm(input.get(0), input.get(1), limits);
                frames = List.of(comparison.leftOnly(), comparison.common(), comparison.rightOnly());
            }
            case "join" -> {
                noArgs(args);
                frames = List.of(TextTools.join(input.get(0), input.get(1), limits));
            }
            case "paste" -> {
                exactArgs(args, 1);
                frames = List.of(TextTools.paste(input, args.getFirst(), limits));
            }
            case "split" -> {
                exactArgs(args, 1);
                frames = TextTools.split(input.getFirst(), positiveInt(args.getFirst()), limits);
            }
            case "csplit" -> {
                exactArgs(args, 1);
                frames = TextTools.csplit(input.getFirst(), Pattern.compile(args.getFirst()), limits);
            }
            case "xargs-plan" -> {
                if (args.isEmpty()) throw usage(spec.id());
                batches = M3TextOperators.xargsBatches(input.getFirst(),
                        args.subList(1, args.size()), positiveInt(args.getFirst()), limits);
                frames = List.of();
            }
            case "diff" -> {
                noArgs(args);
                frames = List.of(TextTools.diff(input.get(0), input.get(1), limits));
            }
            case "patch" -> {
                noArgs(args);
                frames = List.of(TextTools.patch(input.get(0), input.get(1), limits));
            }
            case "csv-cut", "tsv-cut" -> {
                if (args.isEmpty()) throw usage(spec.id());
                int[] columns = new int[args.size()];
                for (int index = 0; index < args.size(); index++) {
                    columns[index] = nonnegativeInt(args.get(index));
                }
                frames = List.of(StructuredTextTools.selectDelimited(input.getFirst(),
                        spec.id().equals("csv-cut") ? ',' : '\t', columns, limits));
            }
            case "jsonl-pointer" -> {
                exactArgs(args, 1);
                frames = List.of(JsonLinesTools.select(input.getFirst(), args.getFirst(), limits));
            }
            case "ini-get" -> {
                exactArgs(args, 2);
                Map<String, String> section = StructuredTextTools.ini(input.getFirst(), limits)
                        .get(args.get(0));
                if (section == null || !section.containsKey(args.get(1))) {
                    throw new IllegalArgumentException("missing INI key");
                }
                frames = List.of(singleValue(section.get(args.get(1)), limits));
            }
            case "env-get" -> {
                exactArgs(args, 1);
                Map<String, String> values = StructuredTextTools.dotenv(input.getFirst(), limits);
                if (!values.containsKey(args.getFirst())) {
                    throw new IllegalArgumentException("missing .env key");
                }
                frames = List.of(singleValue(values.get(args.getFirst()), limits));
            }
            default -> throw new IllegalStateException("unhandled pure text tool: " + spec.id());
        }

        validateOutput(spec, frames, batches, limits);
        return M3TextToolResult.create(request, frames, batches);
    }

    /** Re-executes and requires exact request, catalog, frames, argument batches, and receipts. */
    public static M3TextToolResult replay(M3TextToolRequest request,
                                          M3TextToolResult expected,
                                          Limits limits) {
        Objects.requireNonNull(expected, "expected");
        if (!expected.requestSha256().equals(request.sha256())) {
            throw new IllegalArgumentException("replay request mismatch");
        }
        if (!expected.catalogSha256().equals(M3TextToolCatalog.sha256())) {
            throw new IllegalArgumentException("replay catalog mismatch");
        }
        M3TextToolResult actual = execute(request, limits);
        if (!actual.equals(expected)) throw new IllegalStateException("text-tool replay mismatch");
        return actual;
    }

    private static void admit(M3TextToolRequest request, M3TextToolCatalog.Spec spec,
                              Limits limits) {
        int count = request.inputs().size();
        boolean valid = switch (spec.input()) {
            case NONE -> count == 0;
            case ONE -> count == 1;
            case TWO -> count == 2;
            case ONE_OR_MORE -> count >= 1 && count <= limits.maxNodes();
        };
        if (!valid) throw new IllegalArgumentException("invalid input shape for " + spec.id());
        if (request.arguments().size() > limits.maxNodes()) {
            throw new IllegalArgumentException("argument count budget");
        }
        long argumentBytes = 0;
        for (String argument : request.arguments()) {
            argumentBytes = Math.addExact(argumentBytes, Utf8Segments.length(argument));
            if (argumentBytes > limits.maxBytes()) {
                throw new IllegalArgumentException("argument byte budget");
            }
        }
        for (TextFrame frame : request.inputs()) limits.check(frame);
    }

    private static void validateOutput(M3TextToolCatalog.Spec spec,
                                       List<TextFrame> frames,
                                       List<List<String>> batches,
                                       Limits limits) {
        frames = List.copyOf(frames);
        batches = List.copyOf(batches);
        switch (spec.output()) {
            case FRAME -> {
                if (frames.size() != 1 || !batches.isEmpty()) {
                    throw new IllegalStateException("frame output contract");
                }
            }
            case FRAMES -> {
                if (!batches.isEmpty()) throw new IllegalStateException("frames output contract");
            }
            case ARGUMENT_BATCHES -> {
                if (!frames.isEmpty()) throw new IllegalStateException("batch output contract");
            }
            case EFFECT -> throw new IllegalStateException("effect output reached pure executor");
        }
        if (frames.size() > limits.maxNodes() || batches.size() > limits.maxLines()) {
            throw new IllegalArgumentException("output collection budget");
        }
        long argumentBytes = 0;
        for (List<String> batch : batches) {
            if (batch.size() > limits.maxNodes()) {
                throw new IllegalArgumentException("output argument count budget");
            }
            for (String argument : batch) {
                argumentBytes = Math.addExact(argumentBytes, Utf8Segments.length(argument));
                if (argumentBytes > limits.maxBytes()) {
                    throw new IllegalArgumentException("output argument byte budget");
                }
            }
        }
        frames.forEach(limits::check);
    }

    private static TextFrame singleValue(String value, Limits limits) {
        TextFrame frame = TextFrame.fromString(value + "\n");
        limits.check(frame);
        return frame;
    }

    private static MatchArguments matchArguments(List<String> arguments) {
        if (arguments.size() == 1) return new MatchArguments(arguments.getFirst(), false);
        if (arguments.size() == 2 && arguments.getFirst().equals("invert")) {
            return new MatchArguments(arguments.get(1), true);
        }
        throw usage("grep");
    }

    private static M3TextOperators.CountMode countMode(List<String> arguments) {
        if (arguments.isEmpty()) return M3TextOperators.CountMode.DEFAULT;
        exactArgs(arguments, 1);
        return switch (arguments.getFirst()) {
            case "lines" -> M3TextOperators.CountMode.LINES;
            case "words" -> M3TextOperators.CountMode.WORDS;
            case "bytes" -> M3TextOperators.CountMode.BYTES;
            case "scalars" -> M3TextOperators.CountMode.SCALARS;
            default -> throw usage("wc");
        };
    }

    private static int positiveInt(String value) {
        int parsed = nonnegativeInt(value);
        if (parsed == 0) throw new IllegalArgumentException("positive decimal required");
        return parsed;
    }

    private static int nonnegativeInt(String value) {
        if (!value.matches("0|[1-9][0-9]*")) {
            throw new IllegalArgumentException("nonnegative decimal required");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException overflow) {
            throw new IllegalArgumentException("integer overflow", overflow);
        }
    }

    private static long longValue(String value) {
        if (!value.matches("-?(0|[1-9][0-9]*)")) {
            throw new IllegalArgumentException("signed decimal required");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException overflow) {
            throw new IllegalArgumentException("long overflow", overflow);
        }
    }

    private static void noArgs(List<String> arguments) {
        exactArgs(arguments, 0);
    }

    private static void exactArgs(List<String> arguments, int count) {
        if (arguments.size() != count) throw usage("arguments");
    }

    private static IllegalArgumentException usage(String tool) {
        return new IllegalArgumentException("invalid arguments for " + tool);
    }

    private record MatchArguments(String pattern, boolean invert) { }
}
