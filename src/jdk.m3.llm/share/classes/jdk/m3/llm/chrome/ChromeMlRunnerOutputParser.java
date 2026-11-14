// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for the stable textual records emitted by Chrome2api's bare runner. */
public final class ChromeMlRunnerOutputParser {
    private static final Pattern ANCHORED = Pattern.compile(
            "generate_wait=\\d+\\s+complete_status=(-?\\d+)\\s+full_text=([\\s\\S]*?)\\r?\\ndestroyed_session=1");
    private static final Pattern TOKEN = Pattern.compile(
            "^generate_output status=(-?\\d+) text=([\\s\\S]*?) tool_calls=\\d+$");

    public record Parsed(Integer status, String text) {
        public Parsed {
            text = Objects.requireNonNull(text, "text");
        }
    }

    private ChromeMlRunnerOutputParser() {}

    public static Parsed parseFullText(String stdout) {
        String text = Objects.requireNonNull(stdout, "stdout");
        Matcher anchored = ANCHORED.matcher(text);
        if (anchored.find()) {
            return new Parsed(
                    Integer.valueOf(anchored.group(1)),
                    stripTransportLineEndings(anchored.group(2)));
        }

        String marker = "full_text=";
        int index = text.lastIndexOf(marker);
        if (index < 0) {
            throw new IllegalArgumentException("RUNNER_FULL_TEXT_MISSING");
        }
        String rest = text.substring(index + marker.length());
        int newline = rest.indexOf('\n');
        String line = newline >= 0 ? rest.substring(0, newline) : rest;
        return new Parsed(null, stripTransportLineEndings(line));
    }

    /**
     * Strict native-completion gate. The legacy extraction method remains available for old logs,
     * but neither a loose full_text marker nor process exit zero proves completed inference.
     * Reject ambiguous record-looking model text rather than treating it as control evidence.
     */
    public static Parsed parseCompleted(String stdout) {
        String value = Objects.requireNonNull(stdout, "stdout");
        Matcher header = Pattern.compile("(?m)^generate_wait=(\\d+) complete_status=(-?\\d+) full_text=").matcher(value);
        if (!header.find()) { throw new IllegalArgumentException("RUNNER_COMPLETION_MISSING"); }
        String wait = header.group(1);
        String status = header.group(2);
        int start = header.end();
        if (header.find()) { throw new IllegalArgumentException("RUNNER_COMPLETION_AMBIGUOUS"); }
        Matcher tail = Pattern.compile("\\r?\\ndestroyed_session=1\\r?\\ndestroyed_model=1\\r?\\n?\\z").matcher(value);
        if (!tail.find() || tail.start() < start) {
            throw new IllegalArgumentException("RUNNER_CLEANUP_RECORDS_MISSING");
        }
        if (!"0".equals(wait) || !"1".equals(status)) {
            throw new IllegalArgumentException("RUNNER_INFERENCE_NOT_COMPLETED");
        }
        String body = value.substring(start, tail.start());
        if (Pattern.compile("(?m)^(?:destroyed_session|destroyed_model)=").matcher(body).find()) {
            throw new IllegalArgumentException("RUNNER_CLEANUP_RECORDS_AMBIGUOUS");
        }
        return new Parsed(1, body);
    }

    public static Optional<String> streamingToken(String line) {
        Matcher matcher = TOKEN.matcher(Objects.requireNonNull(line, "line"));
        if (!matcher.matches() || Integer.parseInt(matcher.group(1)) != 0) {
            return Optional.empty();
        }
        return matcher.group(2).isEmpty() ? Optional.empty() : Optional.of(matcher.group(2));
    }

    private static String stripTransportLineEndings(String value) {
        return value.replaceFirst("[\\r\\n]+$", "");
    }
    /** Compatibility alias; completion status 1 is terminal, status 0 is still in progress. */
    public static Parsed parseSuccessful(String stdout) { return parseCompleted(stdout); }
}
