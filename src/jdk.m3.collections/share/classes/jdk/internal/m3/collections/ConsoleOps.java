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

import java.io.PrintStream;
/**
 * Console/terminal output utilities.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>ANSI color output</li>
 * <li>Progress indicators</li>
 * <li>Formatted output</li>
 * <li>Table formatting</li>
 * </ul>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ConsoleOps.java}
 * blob {@code b70fbec306b2de553234fdafa3391e09cd35d074} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConsoleOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConsoleOps.class);


    private ConsoleOps() {}

    private static final long PROGRESS_STEP_DELAY_MS = 100;

    // ANSI color codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Background colors
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";

    // Styles
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";

    // CNPCDA[FLLDCIM:R12:P2:FM-48] private static without final — genuinely
    // reassigned via setters
    private static boolean colorsEnabled = true;
    private static PrintStream out = System.out;

    // Configuration
    public static void setColorsEnabled(boolean enabled) {
        colorsEnabled = enabled;
    }

    public static void setOutput(PrintStream stream) {
        out = stream;
    }

    // Colored output
    public static String color(String text, String color) {
        return colorsEnabled ? color + text + RESET : text;
    }

    public static String red(String text) {
        return color(text, RED);
    }

    public static String green(String text) {
        return color(text, GREEN);
    }

    public static String yellow(String text) {
        return color(text, YELLOW);
    }

    public static String blue(String text) {
        return color(text, BLUE);
    }

    public static String cyan(String text) {
        return color(text, CYAN);
    }

    public static String purple(String text) {
        return color(text, PURPLE);
    }

    public static String bold(String text) {
        return color(text, BOLD);
    }

    public static String dim(String text) {
        return color(text, DIM);
    }

    // Output methods
    public static void print(String text) {
        out.print(text);
    }

    public static void println(String text) {
        out.println(text);
    }

    public static void println() {
        out.println();
    }

    public static void printf(String format, Object... args) {
        out.printf(java.util.Locale.ROOT, format, args);
    }

    // Semantic output
    public static void info(String msg) {
        println(cyan("[INFO] ") + msg);
    }

    public static void success(String msg) {
        println(green("[SUCCESS] ") + msg);
    }

    public static void warn(String msg) {
        println(yellow("[WARN] ") + msg);
    }

    public static void error(String msg) {
        println(red("[ERROR] ") + msg);
    }

    public static void debug(String msg) {
        println(dim("[DEBUG] " + msg));
    }

    // Progress
    public static void progress(int current, int total) {
        int percent = total > 0 ? (current * 100) / total : 0;
        int bars = percent / 5;
        String bar = "[" + "=".repeat(bars) + " ".repeat(20 - bars) + "]";
        out.printf(java.util.Locale.ROOT, "\r%s %3d%% (%d/%d)", bar, percent, current, total);
        if (current >= total) println();
    }

    public static void spinner(int frame) {
        char[] frames = {'|', '/', '-', '\\'};
        out.printf(java.util.Locale.ROOT, "\r%c", frames[frame % 4]);
    }

    // Separators
    public static void line() {
        line('-', 60);
    }

    public static void line(char c, int width) {
        println(String.valueOf(c).repeat(width));
    }

    public static void doubleLine() {
        line('=', 60);
    }

    // Headers
    public static void header(String title) {
        doubleLine();
        println(bold(center(title, 60)));
        doubleLine();
    }

    public static void section(String title) {
        println();
        println(bold(title));
        line();
    }

    // Table formatting (simple)
    public static void row(String... cols) {
        println(java.util.Arrays.stream(cols)
                .map(col -> String.format(java.util.Locale.ROOT, "%-20s", col))
                .collect(java.util.stream.Collectors.joining()));
    }

    public static void tableHeader(String... cols) {
        row(cols);
        line('-', cols.length * 20);
    }

    // Utility
    private static String center(String s, int width) {
        if (s.length() >= width) return s;
        int pad = width - s.length();
        return " ".repeat(pad / 2) + s + " ".repeat(pad - pad / 2);
    }

    public static void clear() {
        print("\033[H\033[2J");
        out.flush();
    }

    public static void cursorUp(int n) {
        out.printf(java.util.Locale.ROOT, "\033[%dA", n);
    }

    public static void cursorDown(int n) {
        out.printf(java.util.Locale.ROOT, "\033[%dB", n);
    }

    public static void cursorRight(int n) {
        out.printf(java.util.Locale.ROOT, "\033[%dC", n);
    }

    public static void cursorLeft(int n) {
        out.printf(java.util.Locale.ROOT, "\033[%dD", n);
    }

    /**
     * Command-line interface for ConsoleOps demonstrations.
     *
     * <p>
     * Usage:
     *
     * <pre>
     * java jdk.internal.m3.collections.ConsoleOps &lt;command&gt; [args]
     *
     * Commands:
     *   list                  - List all available operations
     *   colors                - Demonstrate color output
     *   progress              - Demonstrate progress bar
     *   table                 - Demonstrate table formatting
     *
     * Examples:
     *   java jdk.internal.m3.collections.ConsoleOps colors
     *   java jdk.internal.m3.collections.ConsoleOps table
     * </pre>
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
                case "colors" -> {
                    println(red("Red text"));
                    println(green("Green text"));
                    println(yellow("Yellow text"));
                    println(blue("Blue text"));
                    println(cyan("Cyan text"));
                    println(purple("Purple text"));
                    println(bold("Bold text"));
                    println(dim("Dim text"));
                    info("Info message");
                    success("Success message");
                    warn("Warning message");
                    error("Error message");
                }
                case "progress" -> {
                    for (int i = 0; i <= 100; i += 5) { // NFL — Thread.sleep with InterruptedException handling + break on interrupt
                        progress(i, 100);
                        try {
                            Thread.sleep(PROGRESS_STEP_DELAY_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                case "table" -> {
                    header("Sample Table");
                    tableHeader("Name", "Age", "City");
                    row("John", "30", "New York");
                    row("Jane", "25", "Boston");
                    row("Bob", "35", "Chicago");
                    line();
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
        log.info("""
				Usage: java jdk.internal.m3.collections.ConsoleOps <command> [args]

				Commands:
				  list                  - List all available operations
				  colors                - Demonstrate color output
				  progress              - Demonstrate progress bar
				  table                 - Demonstrate table formatting

				Examples:
				  java jdk.internal.m3.collections.ConsoleOps colors
				  java jdk.internal.m3.collections.ConsoleOps table
				""");
    }

    private static void printOperations() {
        log.info("""
				ConsoleOps - Console Output Utilities

				Colors:
				  red, green, yellow, blue, cyan, purple
				  bold, dim, color

				Semantic Output:
				  info, success, warn, error, debug

				Progress:
				  progress              - Progress bar (current/total)
				  spinner               - Spinning indicator

				Separators:
				  line, doubleLine      - Horizontal lines
				  header, section       - Section headers

				Table Formatting:
				  row                   - Print table row
				  tableHeader           - Print table header

				Utilities:
				  clear                 - Clear screen
				  cursorUp/Down/Left/Right - Cursor movement
				""");
    }
}
