// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight structural guard for GitHub Actions YAML.
 *
 * <p>This is intentionally not a general YAML parser. It validates the failure modes that make
 * Actions reject a workflow before creating jobs: duplicate mapping keys in one scope, empty job
 * mappings, step entries outside a steps list, and named steps without run/uses.</p>
 */
public final class WorkflowStructureCheck {
    private static final Pattern MAPPING =
            Pattern.compile("^([A-Za-z0-9_.-]+):(?:\\s*(.*))?$");
    private static final Pattern NAMED_STEP =
            Pattern.compile("^(\\s*)-\\s+name:\\s*.*$");
    private static final Pattern ACTION_STEP =
            Pattern.compile("^(\\s*)-\\s+uses:\\s*.*$");

    private WorkflowStructureCheck() {}

    record Problem(Path file, int line, String code, String detail) {
        @Override
        public String toString() {
            return file + ":" + line + "\t" + code + "\t" + detail;
        }
    }

    private record Frame(int indent, String label) {}

    public static void main(final String[] args) throws Exception {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length == 0) {
            throw new IllegalArgumentException("workflow path required");
        }

        final List<Problem> all = new ArrayList<>();
        for (String argument : args) {
            final Path file = Path.of(argument).toAbsolutePath().normalize();
            all.addAll(validate(file));
        }
        if (!all.isEmpty()) {
            all.forEach(problem -> System.err.println(problem));
            throw new IllegalStateException("WORKFLOW_STRUCTURE_INVALID:" + all.size());
        }
        System.out.println("WORKFLOW_STRUCTURE_OK\tfiles=" + args.length);
    }

    static List<Problem> validate(final Path file) throws IOException {
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        final List<Problem> problems = new ArrayList<>();
        final Deque<Frame> context = new ArrayDeque<>();
        final Map<String, Set<String>> keysByScope = new HashMap<>();
        int blockScalarIndent = -1;

        for (int index = 0; index < lines.size(); index++) {
            final String raw = lines.get(index);
            if (raw.isBlank() || raw.stripLeading().startsWith("#")) {
                continue;
            }
            final int indent = indentation(raw);
            if (indent < 0) {
                problems.add(new Problem(file, index + 1, "TAB_INDENT",
                        "tabs are not allowed in YAML indentation"));
                continue;
            }
            if (blockScalarIndent >= 0) {
                if (indent > blockScalarIndent) {
                    continue;
                }
                blockScalarIndent = -1;
            }

            final String trimmed = raw.substring(indent);
            while (!context.isEmpty() && context.peekLast().indent() >= indent) {
                context.removeLast();
            }

            if (trimmed.startsWith("- ")) {
                final boolean underSteps =
                        context.stream().anyMatch(frame -> "steps".equals(frame.label()));
                if ((NAMED_STEP.matcher(raw).matches() || ACTION_STEP.matcher(raw).matches())
                        && !underSteps) {
                    problems.add(new Problem(file, index + 1, "STEP_OUTSIDE_STEPS",
                            trimmed));
                }
                context.addLast(new Frame(indent, "[]@" + (index + 1)));
                continue;
            }

            final Matcher matcher = MAPPING.matcher(trimmed);
            if (!matcher.matches()) {
                continue;
            }
            final String key = matcher.group(1);
            final String value = matcher.group(2) == null ? "" : matcher.group(2).trim();
            final String scope = scope(context, indent);
            final String slot = scope + "@" + indent;
            if (!keysByScope.computeIfAbsent(slot, ignored -> new HashSet<>()).add(key)) {
                problems.add(new Problem(file, index + 1, "DUPLICATE_MAPPING_KEY",
                        (scope.isEmpty() ? "<root>" : scope) + "." + key));
            }

            if ("jobs".equals(scope) && value.isEmpty()) {
                final int next = nextSignificant(lines, index + 1);
                if (next < 0 || indentation(lines.get(next)) <= indent) {
                    problems.add(new Problem(file, index + 1, "EMPTY_JOB", key));
                }
            }

            if (blockScalar(value)) {
                blockScalarIndent = indent;
            } else if (value.isEmpty()) {
                context.addLast(new Frame(indent, key));
            }
        }

        for (int index = 0; index < lines.size(); index++) {
            final Matcher step = NAMED_STEP.matcher(lines.get(index));
            if (!step.matches()) {
                continue;
            }
            final int indent = step.group(1).length();
            boolean executor = false;
            for (int cursor = index + 1; cursor < lines.size(); cursor++) {
                final String raw = lines.get(cursor);
                if (raw.isBlank() || raw.stripLeading().startsWith("#")) {
                    continue;
                }
                final int childIndent = indentation(raw);
                if (childIndent < 0) {
                    break;
                }
                final String trimmed = raw.substring(childIndent);
                if (childIndent <= indent) {
                    break;
                }
                if (trimmed.startsWith("run:") || trimmed.startsWith("uses:")) {
                    executor = true;
                    break;
                }
            }
            if (!executor) {
                problems.add(new Problem(file, index + 1, "STEP_WITHOUT_EXECUTOR",
                        lines.get(index).trim()));
            }
        }
        return List.copyOf(problems);
    }

    private static int indentation(final String value) {
        int count = 0;
        while (count < value.length()) {
            final char c = value.charAt(count);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                return -1;
            } else {
                break;
            }
        }
        return count;
    }

    private static String scope(final Deque<Frame> context, final int indent) {
        final StringBuilder value = new StringBuilder();
        for (Frame frame : context) {
            if (frame.indent() < indent) {
                if (value.length() > 0) {
                    value.append('/');
                }
                value.append(frame.label());
            }
        }
        return value.toString();
    }

    private static boolean blockScalar(final String value) {
        return value.equals("|") || value.equals(">")
                || value.equals("|-") || value.equals(">-")
                || value.equals("|+") || value.equals(">+");
    }

    private static int nextSignificant(final List<String> lines, final int start) {
        for (int index = start; index < lines.size(); index++) {
            final String line = lines.get(index);
            if (!line.isBlank() && !line.stripLeading().startsWith("#")) {
                return index;
            }
        }
        return -1;
    }

    private static void selfTest() throws Exception {
        final Path root = Files.createTempDirectory("workflow-structure-selftest-");
        try {
            final Path good = root.resolve("good.yml");
            Files.writeString(good, """
                    name: Good
                    on:
                      pull_request:
                    jobs:
                      verify:
                        runs-on: ubuntu-latest
                        steps:
                          - name: Run
                            run: echo ok
                          - uses: actions/checkout@v4
                    """);
            if (!validate(good).isEmpty()) {
                throw new AssertionError("valid workflow rejected: " + validate(good));
            }

            final Path bad = root.resolve("bad.yml");
            Files.writeString(bad, """
                    name: First
                    name: Second
                    on:
                      push:
                        branches: [develop]
                        branches: [main]
                    jobs:
                      empty:
                      broken:
                        runs-on: ubuntu-latest
                        - name: Outside
                          run: echo no
                      orphan:
                        runs-on: ubuntu-latest
                        steps:
                          - name: Missing executor
                          - name: Next
                            run: echo next
                    """);
            final Set<String> codes = new HashSet<>();
            validate(bad).forEach(problem -> codes.add(problem.code()));
            final Set<String> expected = Set.of(
                    "DUPLICATE_MAPPING_KEY",
                    "EMPTY_JOB",
                    "STEP_OUTSIDE_STEPS",
                    "STEP_WITHOUT_EXECUTOR");
            if (!codes.containsAll(expected)) {
                throw new AssertionError("missing expected codes: " + codes);
            }
            System.out.println("WORKFLOW_STRUCTURE_SELF_TEST_OK");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
