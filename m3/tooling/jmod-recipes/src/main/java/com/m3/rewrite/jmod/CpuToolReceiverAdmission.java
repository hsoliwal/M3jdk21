// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded, exact CPU-tool receiver catalogue admission. Does not install or load code. */
public final class CpuToolReceiverAdmission {
    private CpuToolReceiverAdmission() { }
    public record Result(int donorCount, int mappedCount, int deferredCount, List<String> deferredModules) { }

    public static Result verify(String donorTsv, String mappingTsv, String deferralTsv) {
        Map<String, String[]> donor = rows(donorTsv, 5, "module\tmain_class\tfamily\tstatus\tlicense");
        Map<String, String[]> mapped = rows(mappingTsv, 5,
                "source_module\tcandidate_target_module\tintake_mode\tphase\tstatus");
        Map<String, String[]> deferred = rows(deferralTsv, 4,
                "source_module\tdisposition\tproblem_category\trationale");
        Set<String> targetModules = new HashSet<>();
        for (var entry : mapped.entrySet()) {
            String source = entry.getKey();
            String[] values = entry.getValue();
            if (!donor.containsKey(source)) throw new IllegalArgumentException("unknown donor " + source);
            String target = source.replace("com.synexia.m3.", "com.m3.tools.");
            if (!target.equals(values[1]) || !targetModules.add(values[1])) {
                throw new IllegalArgumentException("target collision or package drift " + source);
            }
            if (!values[2].equals("INDEPENDENT_APACHE_MODULE")
                    || !values[3].equals("OPTIONAL_TOOLING")
                    || !values[4].equals("QUALIFICATION_PENDING")) {
                throw new IllegalArgumentException("unreviewed receiver promotion " + source);
            }
            if (deferred.containsKey(source)) throw new IllegalArgumentException("mapped and deferred " + source);
        }
        List<String> missing = new ArrayList<>();
        for (var entry : donor.entrySet()) {
            String source = entry.getKey();
            String[] fields = entry.getValue();
            if (!fields[4].equals("Apache-2.0") || !fields[1].startsWith(source + ".")
                    || !(fields[3].equals("IMPLEMENTED") || fields[3].equals("ADAPTER_PENDING"))) {
                throw new IllegalArgumentException("donor contract/rights drift " + source);
            }
            if (mapped.containsKey(source)) continue;
            String[] disposition = deferred.get(source);
            if (disposition == null || !disposition[1].equals("SEPARATE_QUALIFICATION_PENDING")) {
                throw new IllegalArgumentException("silently missing donor " + source);
            }
            if (source.equals("com.synexia.m3.compute.webgpu")) {
                if (!disposition[2].equals("OPTIONAL_GPU_NATIVE"))
                    throw new IllegalArgumentException("GPU provider must stay optional");
            } else if (source.equals("com.synexia.m3.model.distill")) {
                if (!disposition[2].equals("CPU_MODEL_MATH"))
                    throw new IllegalArgumentException("CPU model math cannot be silently dropped");
            } else throw new IllegalArgumentException("unreviewed deferral " + source);
            missing.add(source);
        }
        if (missing.size() != deferred.size() || mapped.size() + deferred.size() != donor.size()) {
            throw new IllegalArgumentException("incomplete donor catalogue closure");
        }
        missing.sort(String::compareTo);
        return new Result(donor.size(), mapped.size(), missing.size(), List.copyOf(missing));
    }

    public static Result verify(Path source, Path receiver, Path deferred) throws IOException {
        return verify(strict(source), strict(receiver), strict(deferred));
    }

    private static String strict(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path) || Files.size(path) > 65_536) {
            throw new IOException("manifest path/budget refused " + path);
        }
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
    }

    private static Map<String, String[]> rows(String raw, int width, String header) {
        if (raw == null || raw.length() > 65_536 || !raw.endsWith("\n")) {
            throw new IllegalArgumentException("manifest size/newline");
        }
        Map<String, String[]> result = new HashMap<>();
        boolean headerSeen = false;
        for (String line : raw.split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (!headerSeen) {
                if (!line.equals(header)) throw new IllegalArgumentException("manifest header");
                headerSeen = true;
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != width) throw new IllegalArgumentException("manifest field count");
            for (String cell : cells) {
                if (cell.isBlank() || cell.length() > 4096
                        || cell.indexOf('\r') >= 0 || cell.indexOf('\0') >= 0) {
                    throw new IllegalArgumentException("invalid manifest cell");
                }
            }
            if (!cells[0].matches("com\\.synexia\\.m3(?:\\.[a-z][a-z0-9]*){2,}"))
                throw new IllegalArgumentException("module syntax");
            if (result.putIfAbsent(cells[0], cells) != null)
                throw new IllegalArgumentException("duplicate module");
            if (result.size() > 256) throw new IllegalArgumentException("module count");
        }
        if (!headerSeen) throw new IllegalArgumentException("missing header");
        return result;
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 3) throw new IllegalArgumentException("donor.tsv receiver.tsv disposition.tsv");
        var receipt = verify(Path.of(arguments[0]), Path.of(arguments[1]), Path.of(arguments[2]));
        System.out.println("CPU_TOOL_RECEIVER_ADMISSION_PASS donor=" + receipt.donorCount()
                + " mapped=" + receipt.mappedCount() + " deferred=" + receipt.deferredCount());
        receipt.deferredModules().forEach(name -> System.out.println("DEFERRED\t" + name));
    }
}
