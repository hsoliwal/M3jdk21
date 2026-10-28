/*******************************************************************************
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
package com.synexia.m3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/** Maven-bound, source-sealed receiver for the M3 direct long-address atom. */
public final class M3LongDirectAddressKernelRecipe {
    private record Additive(String resource, String relativePath) { }

    private static final String PROJECTION =
            "m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java";
    private static final List<Additive> ADDITIVE = List.of(
            new Additive("M3LongAddressKernel.java",
                    "m3/collections/src/main/java/com/m3/collections/M3LongAddressKernel.java"),
            new Additive("M3LongAddressKernelSelfTest.java",
                    "m3/collections/src/test/java/com/m3/collections/M3LongAddressKernelSelfTest.java"),
            new Additive("M3LongAddressKernelTest.java",
                    "m3/collections/src/test/java/com/m3/collections/M3LongAddressKernelTest.java"),
            new Additive("m3_long_address_kernel.c",
                    "m3/collections/src/main/native/collections/m3_long_address_kernel.c"),
            new Additive("m3-long-direct-address-kernel.yml",
                    ".github/workflows/m3-long-direct-address-kernel.yml"));

    private M3LongDirectAddressKernelRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    static void run(Path targetRoot, boolean apply) throws IOException {
        Path root = targetRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21_RECEIVER");
        }

        byte[] before = resource("before-M3LongIndexProjection.java");
        byte[] after = transform(before);
        Path projection = checked(root, PROJECTION);
        byte[] current = Files.readAllBytes(projection);
        if (!Arrays.equals(current, before) && !Arrays.equals(current, after)) {
            throw new IOException("REFUSE_DRIFT:" + PROJECTION);
        }

        for (Additive item : ADDITIVE) {
            Path target = checked(root, item.relativePath());
            byte[] expected = resource(item.resource());
            if (Files.exists(target) && !Arrays.equals(Files.readAllBytes(target), expected)) {
                throw new IOException("REFUSE_DRIFT:" + item.relativePath());
            }
            if (!apply && !Files.exists(target)) {
                throw new IOException("REFUSE_MISSING:" + item.relativePath());
            }
        }

        if (apply) {
            if (Arrays.equals(current, before)) writeAtomic(projection, after);
            for (Additive item : ADDITIVE) {
                Path target = checked(root, item.relativePath());
                if (!Files.exists(target)) writeAtomic(target, resource(item.resource()));
            }
        }

        if (!Arrays.equals(Files.readAllBytes(projection), after)) {
            throw new IOException("POSTIMAGE_MISMATCH:" + PROJECTION);
        }
        for (Additive item : ADDITIVE) {
            Path target = checked(root, item.relativePath());
            if (!Files.exists(target)
                    || !Arrays.equals(Files.readAllBytes(target), resource(item.resource()))) {
                throw new IOException("POSTIMAGE_MISMATCH:" + item.relativePath());
            }
        }
        System.out.println("PASS M3LongDirectAddressKernelRecipe mode="
                + (apply ? "apply" : "check") + " files=" + (ADDITIVE.size() + 1));
    }

    private static byte[] transform(byte[] sourceBytes) throws IOException {
        String source = new String(sourceBytes, java.nio.charset.StandardCharsets.UTF_8);
        source = replaceOnce(source,
                "    public int physicalIndex(int ordinal) {\n"
                        + "        return indices[from + Objects.checkIndex(ordinal, size())];\n"
                        + "    }\n\n"
                        + "    public long get(int ordinal) { return values[physicalIndex(ordinal)]; }",
                "    public int physicalIndex(int ordinal) {\n"
                        + "        int checked = Objects.checkIndex(ordinal, size());\n"
                        + "        return M3LongAddressKernel.addressUnchecked(indices, from, checked);\n"
                        + "    }\n\n"
                        + "    public long get(int ordinal) {\n"
                        + "        int checked = Objects.checkIndex(ordinal, size());\n"
                        + "        return M3LongAddressKernel.loadUnchecked(values, indices, from, checked);\n"
                        + "    }");
        source = replaceOnce(source,
                "        long[] result = new long[size()];\n"
                        + "        for (int i = 0; i < result.length; i++) result[i] = values[indices[from + i]];\n"
                        + "        return result;",
                "        long[] result = new long[size()];\n"
                        + "        M3LongAddressKernel.gatherJavaUnchecked(\n"
                        + "                values, indices, from, result, 0, result.length);\n"
                        + "        return result;");
        source = replaceOnce(source,
                "                selected[written++] = indices[from + i];",
                "                selected[written++] = M3LongAddressKernel.addressUnchecked(indices, from, i);");
        source = replaceOnce(source,
                "                    int cmp = Long.compare(values[current[a]], values[current[b]]);",
                "                    int cmp = Long.compare(\n"
                        + "                            M3LongAddressKernel.loadPhysicalUnchecked(values, current[a]),\n"
                        + "                            M3LongAddressKernel.loadPhysicalUnchecked(values, current[b]));");
        return source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String replaceOnce(String source, String before, String after) throws IOException {
        int first = source.indexOf(before);
        if (first < 0 || source.indexOf(before, first + 1) >= 0) {
            throw new IOException("RECIPE_PATTERN_COUNT:" + before.substring(0, Math.min(32, before.length())));
        }
        return source.substring(0, first) + after + source.substring(first + before.length());
    }

    private static Path checked(Path root, String relative) throws IOException {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IOException("REFUSE_PATH_TRAVERSAL");
        for (Path scan = target; scan != null && !scan.equals(root); scan = scan.getParent()) {
            if (Files.isSymbolicLink(scan)) throw new IOException("REFUSE_SYMLINK:" + scan);
        }
        return target;
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream input = M3LongDirectAddressKernelRecipe.class.getResourceAsStream(
                "/templates/" + name)) {
            if (input == null) throw new IOException("MISSING_TEMPLATE:" + name);
            return input.readAllBytes();
        }
    }

    private static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".m3-address-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void selfTest() throws IOException {
        Path root = Files.createTempDirectory("m3-direct-address-recipe-");
        try {
            Path projection = root.resolve(PROJECTION);
            Files.createDirectories(projection.getParent());
            Files.write(projection, resource("before-M3LongIndexProjection.java"));
            try {
                run(root, false);
                throw new AssertionError("missing additive outputs must refuse check");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING")) throw expected;
            }
            run(root, true);
            run(root, true);
            run(root, false);

            Path drift = root.resolve(ADDITIVE.get(0).relativePath());
            Files.writeString(drift, "drift");
            try {
                run(root, true);
                throw new AssertionError("drift must refuse");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT")) throw expected;
            }
            System.out.println("PASS M3LongDirectAddressKernelRecipeSelfTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }
}
