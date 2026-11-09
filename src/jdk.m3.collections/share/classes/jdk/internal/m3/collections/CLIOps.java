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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * CLI argument parsing utilities.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Argument parsing (flags, options, positional)</li>
 * <li>Help text generation</li>
 * <li>Environment variable fallback</li>
 * </ul>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/CLIOps.java}
 * blob {@code ed350693fea945dc2a4213c7ce3040f559be30d3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CLIOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(CLIOps.class);


    private CLIOps() {}

    /** Parsed CLI arguments. */
    public record ParsedArgs(Map<String, String> options, Set<String> flags, List<String> positional) {

        public String opt(String name) {
            return options.get(name);
        }

        public String opt(String name, String defaultValue) {
            return options.getOrDefault(name, defaultValue);
        }

        public boolean flag(String name) {
            return flags.contains(name);
        }

        public String arg(int index) {
            return index < positional.size() ? positional.get(index) : null;
        }

        public String arg(int index, String defaultValue) {
            String v = arg(index);
            return v != null ? v : defaultValue;
        }

        public int argInt(int index, int defaultValue) {
            String v = arg(index);
            if (v == null) return defaultValue;
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public boolean has(String name) {
            return options.containsKey(name) || flags.contains(name);
        }

        public int argCount() {
            return positional.size();
        }
    }

    /** Parse command line arguments. */
    public static ParsedArgs parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        Set<String> flags = new LinkedHashSet<>();
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) { // NFL — index-dependent parsing with i++ skip inside body
            String arg = args[i];
            if (arg.startsWith("--")) {
                String name = arg.substring(2);
                int eq = name.indexOf('=');
                if (eq > 0) {
                    options.put(name.substring(0, eq), name.substring(eq + 1));
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    options.put(name, args[++i]);
                } else {
                    flags.add(name);
                }
            } else if (arg.startsWith("-") && arg.length() > 1) {
                String name = arg.substring(1);
                if (name.length() == 1 && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    options.put(name, args[++i]);
                } else {
                    name.chars().forEach(c -> flags.add(String.valueOf((char) c)));
                }
            } else {
                positional.add(arg);
            }
        }
        return new ParsedArgs(options, flags, positional);
    }

    /** Get option with env var fallback. */
    public static String optOrEnv(ParsedArgs args, String name, String envVar, String defaultValue) {
        String v = args.opt(name);
        if (v != null) return v;
        v = System.getenv(envVar);
        return v != null ? v : defaultValue;
    }

    /** Get option with env var fallback (int). */
    public static int optOrEnvInt(ParsedArgs args, String name, String envVar, int defaultValue) {
        String v = optOrEnv(args, name, envVar, null);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Require option or throw if missing. */
    public static String require(ParsedArgs args, String name, String message) {
        String v = args.opt(name);
        if (v == null) {
            // FM-40: replaced System.exit with exception — library code must not terminate
            // JVM
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    /** Require positional arg or throw if missing. */
    public static String requireArg(ParsedArgs args, int index, String message) {
        String v = args.arg(index);
        if (v == null) {
            // FM-40: replaced System.exit with exception — library code must not terminate
            // JVM
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    // Help text builder
    public static class HelpBuilder {

        private final String name;
        private String description = "";
        private final List<String[]> options = new ArrayList<>();
        private final List<String[]> commands = new ArrayList<>();
        private String usage = "";

        public HelpBuilder(String name) {
            this.name = name;
        }

        public HelpBuilder description(String desc) {
            this.description = desc;
            return this;
        }

        public HelpBuilder usage(String u) {
            this.usage = u;
            return this;
        }

        public HelpBuilder option(String opt, String desc) {
            options.add(new String[] {opt, desc});
            return this;
        }

        public HelpBuilder command(String cmd, String desc) {
            commands.add(new String[] {cmd, desc});
            return this;
        }

        public String build() {
            var sb = new StringBuilder();
            sb.append(name);
            if (!description.isEmpty()) sb.append(" - ").append(description);
            sb.append("\n\n");
            if (!usage.isEmpty()) sb.append("Usage: ").append(usage).append("\n\n");
            if (!commands.isEmpty()) {
                sb.append("Commands:\n");
                commands.forEach(c -> sb.append(String.format(java.util.Locale.ROOT, "  %-20s %s\n", c[0], c[1])));
                sb.append("\n");
            }
            if (!options.isEmpty()) {
                sb.append("Options:\n");
                options.forEach(o -> sb.append(String.format(java.util.Locale.ROOT, "  %-20s %s\n", o[0], o[1])));
            }
            return sb.toString();
        }

        public void print() {
            log.info(build());
        }
    }

    public static HelpBuilder help(String name) {
        return new HelpBuilder(name);
    }

    /**
     * Parse arguments with automatic type detection (for Map-based args).
     *
     * <p>
     * Parses key-value pairs with automatic type conversion:
     * <ul>
     * <li>Numbers (long/double) - parsed from numeric strings</li>
     * <li>Booleans - "true"/"false" (case-insensitive)</li>
     * <li>Strings - everything else</li>
     * </ul>
     *
     * @param args
     *            arguments array
     * @param startIndex
     *            index to start parsing from
     * @return map of typed values
     */
    public static Map<String, Object> parseDirectArgsTyped(String[] args, int startIndex) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = startIndex; i < args.length; i += 2) { // NFL — step-2 index iteration with lookahead
            if (i + 1 < args.length) {
                String key = args[i].startsWith("--") ? args[i].substring(2) : args[i];
                String value = args[i + 1];

                // Try to parse as number
                try {
                    if (value.contains(".")) {
                        map.put(key, Double.parseDouble(value));
                    } else {
                        map.put(key, Long.parseLong(value));
                    }
                } catch (NumberFormatException e) {
                    // Not a number - check boolean
                    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        map.put(key, Boolean.parseBoolean(value));
                    } else {
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    /**
     * Command-line interface for CLIOps demonstrations.
     *
     * <p>
     * Usage:
     *
     * <pre>
     * java jdk.internal.m3.collections.CLIOps &lt;command&gt; [args]
     *
     * Commands:
     *   list                  - List all available operations
     *   parse [args...]       - Parse command-line arguments
     *   help name desc        - Generate help text
     *
     * Examples:
     *   java jdk.internal.m3.collections.CLIOps parse --name John --age 30 -v file.txt
     *   java jdk.internal.m3.collections.CLIOps help MyApp "Sample application"
     * </pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "parse" -> {
                    if (args.length < 2) {
                        log.error("parse requires: [args...]");
                        System.exit(1);
                    }
                    String[] parseArgs = Arrays.copyOfRange(args, 1, args.length);
                    ParsedArgs parsed = parse(parseArgs);
                    log.info("Parsed Arguments:");
                    log.info("  Options: {}", parsed.options());
                    log.info("  Flags: {}", parsed.flags());
                    log.info("  Positional: {}", parsed.positional());
                }
                case "help" -> {
                    if (args.length < 3) {
                        log.error("help requires: <name> <description>");
                        System.exit(1);
                    }
                    HelpBuilder builder = help(args[1])
                            .description(args[2])
                            .usage(args[1] + " [options] <file>")
                            .option("--name <n>", "Specify name")
                            .option("--age <n>", "Specify age")
                            .option("-v, --verbose", "Enable verbose mode")
                            .command("run", "Run the application")
                            .command("test", "Run tests");
                    log.info(builder.build());
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
				Usage: java jdk.internal.m3.collections.CLIOps <command> [args]

				Commands:
				  list                  - List all available operations
				  parse [args...]       - Parse command-line arguments
				  help name desc        - Generate help text

				Examples:
				  java jdk.internal.m3.collections.CLIOps parse --name John --age 30 -v file.txt
				  java jdk.internal.m3.collections.CLIOps help MyApp "Sample application"
				""");
    }

    private static void printOperations() {
        log.info("""
				CLIOps - CLI Argument Parsing

				Parsing:
				  parse                 - Parse command-line arguments
				  parseDirectArgsTyped  - Parse with automatic type detection

				Extraction:
				  opt                   - Get option value
				  flag                  - Check if flag is set
				  arg                   - Get positional argument
				  argInt                - Get positional argument as int

				Utilities:
				  optOrEnv              - Get option with env var fallback
				  require               - Require option or exit
				  requireArg            - Require positional arg or exit

				Help Generation:
				  help                  - Create help builder
				  HelpBuilder           - Fluent help text builder
				""");
    }
}
