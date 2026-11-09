/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/FormatOps.java}
 * blob {@code ad1cdfd6c1cff1359af8ef4b8532fba6df8be328} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FormatOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(FormatOps.class);


    private FormatOps() {}

    public static String format(String template, Object... args) {
        return template == null ? "" : String.format(Locale.ROOT, template, args);
    }

    public static String format(Locale locale, String template, Object... args) {
        return template == null ? "" : String.format(locale, template, args);
    }

    public static String formatInt(int n) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(n);
    }

    public static String formatInt(long n) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(n);
    }

    public static String formatInt(int n, Locale locale) {
        return NumberFormat.getIntegerInstance(locale).format(n);
    }

    public static String formatDecimal(double n) {
        return NumberFormat.getNumberInstance(Locale.ROOT).format(n);
    }

    public static String formatDecimal(double n, int decimals) {
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ROOT);
        df.applyPattern("#." + "#".repeat(decimals));
        return df.format(n);
    }

    public static String formatDecimal(double n, Locale locale) {
        return NumberFormat.getNumberInstance(locale).format(n);
    }

    public static String formatCurrency(double n) {
        return NumberFormat.getCurrencyInstance(Locale.ROOT).format(n);
    }

    public static String formatCurrency(double n, Locale locale) {
        return NumberFormat.getCurrencyInstance(locale).format(n);
    }

    public static String formatPercent(double n) {
        return NumberFormat.getPercentInstance(Locale.ROOT).format(n);
    }

    public static String formatPercent(double n, int decimals) {
        NumberFormat f = NumberFormat.getPercentInstance(Locale.ROOT);
        f.setMinimumFractionDigits(decimals);
        return f.format(n);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return formatDecimal(kb, 1) + " KB";
        double mb = kb / 1024;
        if (mb < 1024) return formatDecimal(mb, 1) + " MB";
        double gb = mb / 1024;
        if (gb < 1024) return formatDecimal(gb, 1) + " GB";
        return formatDecimal(gb / 1024, 1) + " TB";
    }

    public static String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        double secs = millis / 1000.0;
        if (secs < 60) return formatDecimal(secs, 1) + "s";
        double mins = secs / 60;
        if (mins < 60) return formatDecimal(mins, 1) + "m";
        double hours = mins / 60;
        if (hours < 24) return formatDecimal(hours, 1) + "h";
        return formatDecimal(hours / 24, 1) + "d";
    }

    public static String formatDurationDetailed(long millis) {
        long ms = millis % 1000;
        long s = (millis / 1000) % 60;
        long m = (millis / 60000) % 60;
        long h = (millis / 3600000) % 24;
        long d = millis / 86400000;
        var sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0) sb.append(s).append("s ");
        if (ms > 0 || sb.isEmpty()) sb.append(ms).append("ms");
        return sb.toString().trim();
    }

    public static String padLeft(String s, int len) {
        return padLeft(s, len, ' ');
    }

    public static String padLeft(String s, int len, char c) {
        if (s == null) s = "";
        return s.length() >= len ? s : String.valueOf(c).repeat(len - s.length()) + s;
    }

    public static String padRight(String s, int len) {
        return padRight(s, len, ' ');
    }

    public static String padRight(String s, int len, char c) {
        if (s == null) s = "";
        return s.length() >= len ? s : s + String.valueOf(c).repeat(len - s.length());
    }

    public static String center(String s, int len) {
        return center(s, len, ' ');
    }

    public static String center(String s, int len, char c) {
        if (s == null) s = "";
        if (s.length() >= len) return s;
        int pad = len - s.length();
        return String.valueOf(c).repeat(pad / 2) + s + String.valueOf(c).repeat(pad - pad / 2);
    }

    public static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    public static String truncateMiddle(String s, int max) {
        if (s == null || s.length() <= max) return s;
        int half = (max - 3) / 2;
        return s.substring(0, half) + "..." + s.substring(s.length() - half);
    }

    public static String repeat(String s, int times) {
        return s == null ? "" : s.repeat(times);
    }

    public static String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    public static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    public static String singleQuote(String s) {
        return s == null ? "null" : "'" + s + "'";
    }

    public static String join(String delim, Object... items) {
        if (items == null) return "";
        // TODO: Fix incomplete lambda: return
        // java.util.Arrays.stream(items).map(String::valueOf).collect(java.util.stream.Collectors.joining(delim));
        return java.util.Arrays.stream(items).map(String::valueOf).collect(java.util.stream.Collectors.joining(delim));
    }

    //FLLDCIM: MRL — FM-36 return null eliminated; Optional<String> preserves contract
    public static Optional<String> joinNonNull(String delim, Object... items) {
        if (items == null) return Optional.empty();
        String result = java.util.Arrays.stream(items)
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(delim));
        return Optional.of(result);
    }

    /**
     * Formats a number with commas.
     *
     * @param n
     *            the number to format
     * @return formatted number string
     */
    public static String formatNumber(long n) {
        return formatInt(n);
    }

    /**
     * Builds a progress bar string.
     *
     * @param progress
     *            the progress percentage (0-100)
     * @param width
     *            the width of the bar in characters
     * @return progress bar string like "[=====> ]"
     */
    public static String buildProgressBar(int progress, int width) {
        int filled = (int) ((progress / 100.0) * width);
        int empty = width - filled;
        var sb = new StringBuilder("[");
        sb.append("=".repeat(Math.max(0, filled - 1)));
        if (filled > 0) {
            sb.append(">");
        }
        sb.append(" ".repeat(Math.max(0, empty)));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Command-line interface for FormatOps utilities.
     *
     * <p>
     * Provides interactive access to string formatting, number formatting, and text
     * manipulation operations.
     *
     * <h4>Usage</h4>
     *
     * <pre>{@code
     * java jdk.internal.m3.collections.FormatOps list
     * java jdk.internal.m3.collections.FormatOps bytes 1234567890
     * java jdk.internal.m3.collections.FormatOps duration 12345678
     * java jdk.internal.m3.collections.FormatOps pad "hello" 20
     * java jdk.internal.m3.collections.FormatOps truncate "very long string" 10
     * }</pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1); // FM-40: acceptable in CLI main()
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "bytes" -> {
                    if (args.length < 2) {
                        log.error("bytes requires a numeric value");
                        System.exit(1);
                    }
                    long bytes = Long.parseLong(args[1]);
                    log.info("Formatting {} bytes:", bytes);
                    log.info("  {}", formatBytes(bytes));
                }
                case "duration" -> {
                    if (args.length < 2) {
                        log.error("duration requires milliseconds value");
                        System.exit(1);
                    }
                    long millis = Long.parseLong(args[1]);
                    log.info("Formatting {}ms:", millis);
                    log.info("  Compact: {}", formatDuration(millis));
                    log.info("  Detailed: {}", formatDurationDetailed(millis));
                }
                case "number" -> {
                    if (args.length < 2) {
                        log.error("number requires a numeric value");
                        System.exit(1);
                    }
                    double num = Double.parseDouble(args[1]);
                    log.info("Formatting number: {}", num);
                    log.info("  Integer: {}", formatInt((long) num));
                    log.info("  Decimal: {}", formatDecimal(num));
                    log.info("  Decimal(2): {}", formatDecimal(num, 2));
                    log.info("  Percent: {}", formatPercent(num));
                }
                case "pad" -> {
                    if (args.length < 3) {
                        log.error("pad requires text and length");
                        System.exit(1);
                    }
                    String text = args[1];
                    int len = Integer.parseInt(args[2]);
                    log.info("Padding '{}' to length {}:", text, len);
                    log.info("  Left:   [{}]", padLeft(text, len));
                    log.info("  Right:  [{}]", padRight(text, len));
                    log.info("  Center: [{}]", center(text, len));
                }
                case "truncate" -> {
                    if (args.length < 3) {
                        log.error("truncate requires text and max length");
                        System.exit(1);
                    }
                    String text = args[1];
                    int max = Integer.parseInt(args[2]);
                    log.info("Truncating '{}' to {} chars:", text, max);
                    log.info("  End:    {}", truncate(text, max));
                    log.info("  Middle: {}", truncateMiddle(text, max));
                }
                case "progressbar" -> {
                    if (args.length < 2) {
                        log.error("progressbar requires progress percentage (0-100)");
                        System.exit(1);
                    }
                    int progress = Integer.parseInt(args[1]);
                    int width = args.length > 2 ? Integer.parseInt(args[2]) : 40;
                    log.info("Progress: {}%", progress);
                    log.info("{} {}%", buildProgressBar(progress, width), progress);
                }
                case "demo" -> {
                    log.info("FormatOps Demo:");

                    log.info("Bytes formatting:");
                    log.info("  1024 bytes = {}", formatBytes(1024));
                    log.info("  1048576 bytes = {}", formatBytes(1048576));
                    log.info("  1073741824 bytes = {}", formatBytes(1073741824L));

                    log.info("Duration formatting:");
                    log.info("  500ms = {}", formatDuration(500));
                    log.info("  65000ms = {}", formatDuration(65000));
                    log.info("  3665000ms = {}", formatDurationDetailed(3665000));

                    log.info("Progress bars:");
                    java.util.stream.IntStream.iterate(0, i -> i <= 100, i -> i + 25)
                            .forEach(i -> log.info("  {} {}%", buildProgressBar(i, 30), i));
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("Usage: java jdk.internal.m3.collections.FormatOps <command> [args...]");

        log.info("Commands:");
        log.info("  list                    - List all available operations");
        log.info("  bytes <num>             - Format bytes (B/KB/MB/GB/TB)");
        log.info("  duration <millis>       - Format duration");
        log.info("  number <num>            - Format number in various ways");
        log.info("  pad <text> <len>        - Demonstrate padding");
        log.info("  truncate <text> <max>   - Demonstrate truncation");
        log.info("  progressbar <0-100> [width] - Show progress bar");
        log.info("  demo                    - Show formatting examples");
    }

    private static void printOperations() {
        log.info("FormatOps - String and number formatting utilities");

        log.info("String Formatting:");
        log.info("  format(template, args...) - String.format with ROOT locale");
        log.info("  format(locale, template, args...) - With custom locale");

        log.info("Number Formatting:");
        log.info("  formatInt(n)        - Format integer with grouping");
        log.info("  formatDecimal(n)    - Format decimal number");
        log.info("  formatDecimal(n, decimals) - With precision");
        log.info("  formatCurrency(n)   - Format as currency");
        log.info("  formatPercent(n)    - Format as percentage");
        log.info("  formatNumber(n)     - Format with commas");

        log.info("Size Formatting:");
        log.info("  formatBytes(n)      - Format bytes (B/KB/MB/GB/TB)");

        log.info("Duration Formatting:");
        log.info("  formatDuration(ms)  - Format compact (1.5h, 3.2m)");
        log.info("  formatDurationDetailed(ms) - Format detailed (1h 30m 45s)");

        log.info("Padding:");
        log.info("  padLeft(s, len)     - Pad left with spaces");
        log.info("  padLeft(s, len, c)  - Pad left with char");
        log.info("  padRight(s, len)    - Pad right with spaces");
        log.info("  padRight(s, len, c) - Pad right with char");
        log.info("  center(s, len)      - Center with spaces");
        log.info("  center(s, len, c)   - Center with char");

        log.info("Truncation:");
        log.info("  truncate(s, max)    - Truncate end with ...");
        log.info("  truncateMiddle(s, max) - Truncate middle with ...");

        log.info("Other:");
        log.info("  repeat(s, times)    - Repeat string");
        log.info("  repeat(c, times)    - Repeat character");
        log.info("  quote(s)            - Add double quotes");
        log.info("  singleQuote(s)      - Add single quotes");
        log.info("  join(delim, items...) - Join with delimiter");
        log.info("  buildProgressBar(progress, width) - Create progress bar");
    }
}
