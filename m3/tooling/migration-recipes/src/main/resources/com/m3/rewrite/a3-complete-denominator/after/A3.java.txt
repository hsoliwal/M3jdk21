// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Compact CLI for A3 = Atomize -> Patternize -> Absorb. */
public final class A3 {

    private A3() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            throw new IllegalArgumentException("missing A3 command");
        }

        Args options = Args.parse(args);
        switch (args[0]) {
            case "inv" ->
                    A3Inv.write(
                            options.root(),
                            options.out("m3/build/a3/inventory.tsv"));
            case "plan" ->
                    A3Plan.write(
                            options.root(),
                            options.upstreamInventory(),
                            options.out("m3/build/a3/plan.tsv"));
            case "apply" ->
                    A3Apply.run(
                            options.root(),
                            options.out("m3/build/a3/apply"),
                            options.sources());
            default -> {
                usage();
                throw new IllegalArgumentException(
                        "unknown A3 command: " + args[0]);
            }
        }
    }

    private static void usage() {
        System.err.println(
                "A3: inv|plan|apply [--root PATH] [--out PATH] "
                        + "[--upstream-inventory PATH] "
                        + "[--file PATH ...] [--list PATH]");
    }

    private record Args(
            Path root,
            Path out,
            Path upstreamInventory,
            List<String> files,
            Path list) {

        static Args parse(String[] args) {
            Path root = Path.of(".");
            Path out = null;
            Path upstreamInventory = null;
            Path list = null;
            ArrayList<String> files = new ArrayList<>();

            for (int index = 1; index < args.length; index++) {
                String key = args[index];
                if ("--file".equals(key)) {
                    files.add(requireValue(args, ++index, key));
                } else if ("--root".equals(key)) {
                    root = Path.of(requireValue(args, ++index, key));
                } else if ("--out".equals(key)) {
                    out = Path.of(requireValue(args, ++index, key));
                } else if ("--upstream-inventory".equals(key)) {
                    upstreamInventory =
                            Path.of(requireValue(args, ++index, key));
                } else if ("--list".equals(key)) {
                    list = Path.of(requireValue(args, ++index, key));
                } else {
                    throw new IllegalArgumentException(
                            "unknown A3 option: " + key);
                }
            }
            return new Args(
                    root,
                    out,
                    upstreamInventory,
                    List.copyOf(files),
                    list);
        }

        Path out(String fallback) {
            return out == null ? Path.of(fallback) : out;
        }

        Path upstreamInventory() {
            if (upstreamInventory == null) {
                return null;
            }
            return A3Fs.source(A3Fs.root(root), upstreamInventory);
        }

        List<String> sources() throws Exception {
            ArrayList<String> result = new ArrayList<>(files);
            if (list != null) {
                Path rootPath = A3Fs.root(root);
                Path listPath = A3Fs.source(rootPath, list);
                for (String line :
                        Files.readAllLines(
                                listPath,
                                StandardCharsets.UTF_8)) {
                    String value = line.strip();
                    if (!value.isEmpty() && !value.startsWith("#")) {
                        result.add(value);
                    }
                }
            }
            return result.stream().distinct().sorted().toList();
        }

        private static String requireValue(
                String[] args,
                int index,
                String key) {
            if (index >= args.length) {
                throw new IllegalArgumentException(
                        "missing value for " + key);
            }
            return args[index];
        }
    }
}
