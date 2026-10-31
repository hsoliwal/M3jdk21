// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Explicit build-time compiler of bounded CPU tool DAG TSVs into sealed M3 binary plans. */
public final class M3CpuToolImageCompiler {
    public static final String HEADER = "M3_CPU_TOOL_DAG_V1\toutput\tstep\toperation\tinputs\tconstantBase64Url";
    private static final int MAX_TSV_BYTES = 1024 * 1024;

    private M3CpuToolImageCompiler() {}

    /** No shell invocation, no directory walk, no overwrites; input and output are explicit. */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: M3CpuToolImageCompiler <plan.tsv> <new-image-file>");
        }
        Path source = Path.of(args[0]);
        Path output = Path.of(args[1]);
        byte[] plan = Files.readAllBytes(source);
        if (plan.length > MAX_TSV_BYTES) throw new IllegalArgumentException("plan TSV size");
        var compiled = compileTsv(decodeStrict(plan));
        byte[] image = M3CpuToolImage.encode(compiled);
        Files.write(output, image, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println(compiled.sha256() + "\t" + compiled.stages());
    }

    public static M3CpuToolPlan.Compiled compileTsv(String source) {
        String[] lines = source.split("\\n", -1);
        if (lines.length < 2 || !lines[0].equals(HEADER)) {
            throw new IllegalArgumentException("plan header");
        }
        String output = null;
        List<M3CpuToolPlan.Step> nodes = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (index == lines.length - 1 && line.isEmpty()) break;
            String[] columns = line.split("\\t", -1);
            if (columns.length != 6 || !columns[0].equals("M3_CPU_TOOL_DAG_V1")) {
                throw new IllegalArgumentException("invalid tool row " + index);
            }
            if (output == null) output = columns[1];
            if (!output.equals(columns[1])) throw new IllegalArgumentException("mixed output ids");
            String[] deps = columns[4].isEmpty() ? new String[0] : columns[4].split(",", -1);
            String constant = decodeConstant(columns[5]);
            nodes.add(new M3CpuToolPlan.Step(columns[2], columns[3], List.of(deps), constant));
        }
        if (output == null) throw new IllegalArgumentException("empty plan TSV");
        return M3CpuToolPlan.compile(nodes, output);
    }

    private static String decodeConstant(String encoded) {
        if (encoded.length() > 8192) throw new IllegalArgumentException("encoded constant bound");
        try { return decodeStrict(Base64.getUrlDecoder().decode(encoded)); }
        catch (IllegalArgumentException bad) {
            throw new IllegalArgumentException("invalid constant base64url", bad);
        }
    }

    private static String decodeStrict(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException bad) {
            throw new IllegalArgumentException("invalid TSV UTF-8", bad);
        }
    }
}
