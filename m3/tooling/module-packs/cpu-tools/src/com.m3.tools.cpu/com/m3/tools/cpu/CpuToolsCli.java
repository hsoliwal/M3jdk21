// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jdk.m3.io.M3File;
import jdk.m3.text.TextFrame;

/** Read-only command-line entry point for the linked CPU-tools image. */
public final class CpuToolsCli {
    private CpuToolsCli() { }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != 0) System.exit(code);
    }

    /** Returns 0 on success, 1 on refused execution and 2 on command syntax errors. */
    public static int run(String[] args, PrintStream output, PrintStream diagnostics) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(diagnostics, "diagnostics");
        if (args.length == 0) return usage(diagnostics, 2);
        try {
            switch (args[0]) {
                case "--help" -> { return usage(output, args.length == 1 ? 0 : 2); }
                case "--list" -> {
                    if (args.length != 1) return usage(diagnostics, 2);
                    for (var binding : CpuTools.session().registry().bindings()) {
                        output.println(binding.id() + "\t" + CpuTools.escape(binding.definition()));
                    }
                }
                case "--self-test" -> {
                    if (args.length != 1) return usage(diagnostics, 2);
                    selfTest(output);
                }
                case "--run" -> {
                    if (args.length != 3) return usage(diagnostics, 2);
                    List<String> operations = Arrays.asList(args[1].split(",", -1));
                    var session = CpuTools.session();
                    // Validate every binding before the source is opened.
                    session.pipeline(operations);
                    M3File file = M3File.load(Path.of(args[2]));
                    var result = session.execute(operations, file);
                    output.print(result.output().text());
                    diagnostics.println("source\t" + file.uri().toASCIIString()
                            + "\trawSha256=" + file.rawSha256() + "\tutf16Sha256=" + file.utf16Sha256());
                    diagnostics.println("receipt\tdag=" + result.receipt().sha256()
                            + "\tfile=" + CpuTools.sourceReceipt(file, result));
                }
                case "--find" -> {
                    if (args.length < 3 || args.length > 4) return usage(diagnostics, 2);
                    int maxHits = args.length == 4 ? Integer.parseInt(args[3]) : 1000;
                    if (maxHits < 0 || maxHits > 1_000_000) throw new IllegalArgumentException("match budget");
                    M3File file = M3File.load(Path.of(args[2]));
                    var result = CpuTools.literal(file, args[1], maxHits);
                    for (int position : result.positions()) output.println(position);
                    diagnostics.println("receipt\tliteral=" + result.sha256()
                            + "\thits=" + result.size() + "\tcomplete=" + result.complete()
                            + "\tcoordinates=UTF-16");
                }
                case "--ast" -> {
                    if (args.length < 2 || args.length - 1 > M3AstTools.MAX_FILES) return usage(diagnostics, 2);
                    var files = new ArrayList<M3File>(args.length - 1);
                    long totalUtf16 = 0;
                    for (int i = 1; i < args.length; i++) {
                        M3File file = M3File.load(Path.of(args[i]));
                        totalUtf16 += file.length();
                        if (totalUtf16 > M3AstTools.MAX_TOTAL_UTF16) {
                            throw new IllegalArgumentException("aggregate source UTF-16 budget");
                        }
                        files.add(file);
                    }
                    for (var snapshot : M3AstTools.parse(files)) printAst(snapshot, output, diagnostics);
                }
                default -> { return usage(diagnostics, 2); }
            }
            if (output.checkError() || diagnostics.checkError()) return 1;
            return 0;
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            diagnostics.println("CPU_TOOLS_REFUSED\t" + CpuTools.escape(
                    Objects.toString(failure.getMessage(), failure.getClass().getSimpleName())));
            return 1;
        }
    }

    private static void printAst(M3AstTools.Snapshot snapshot, PrintStream output, PrintStream diagnostics) {
        var identity = snapshot.source();
        output.println("source\t" + identity.uri().toASCIIString() + "\t" + identity.utf16Length()
                + "\t" + identity.utf16Sha256() + "\t" + identity.rawSha256().orElse("-"));
        for (int i = 0; i < snapshot.structureCount(); i++) {
            var node = snapshot.structure(i);
            output.println("node\t" + i + "\t" + node.kind() + "\t" + node.span().start()
                    + "\t" + node.span().end() + "\t" + node.parent() + "\t" + node.depth());
        }
        for (var symbol : snapshot.symbols()) {
            output.println("symbol\t" + symbol.kind() + "\t" + CpuTools.escape(symbol.name())
                    + "\t" + symbol.span().start() + "\t" + symbol.span().end());
        }
        for (var imported : snapshot.imports()) {
            output.println("import\t" + imported.staticImport() + "\t" + CpuTools.escape(imported.qualifiedName())
                    + "\t" + imported.span().start() + "\t" + imported.span().end());
        }
        for (var method : snapshot.methods()) {
            output.println("method\t" + CpuTools.escape(method.name()) + "\t" + method.parameterCount()
                    + "\t" + method.constructor() + "\t" + method.span().start() + "\t" + method.span().end()
                    + "\t" + method.body().map(M3AstTools.Span::start).orElse(-1)
                    + "\t" + method.body().map(M3AstTools.Span::end).orElse(-1));
        }
        diagnostics.println("receipt\tast=" + snapshot.sha256() + "\tphase=" + snapshot.phase()
                + "\tvisited=" + snapshot.visitedNodes() + "\tunpositioned=" + snapshot.unpositionedNodes());
    }

    private static void selfTest(PrintStream output) {
        var session = CpuTools.session();
        var plan = session.pipeline(List.of("text.normalize.nfc", "text.words", "text.frequency"));
        var input = TextFrame.fromString("cafe\u0301 beta caf\u00e9\n");
        var result = session.execute(plan, input);
        if (!result.output().text().equals("beta\t1\ncaf\u00e9\t2\n")) {
            throw new IllegalStateException("text composition self-test failed");
        }
        if (!session.replay(plan, input, result).equals(result)) {
            throw new IllegalStateException("receipt replay self-test failed");
        }
        var graph = session.execute(session.pipeline(List.of("graph.topology")),
                TextFrame.fromString("4\n0\t2\n1\t2\n2\t3\n"));
        if (!graph.output().text().equals("0\n1\n2\n3\n")) {
            throw new IllegalStateException("graph self-test failed");
        }
        output.println("M3_CPU_TOOLS_PASS modules=20 bindings=" + session.registry().bindings().size());
        output.println("implementationSha256=" + CpuTools.implementationSha256());
        output.println("receiptSha256=" + result.receipt().sha256());
    }

    private static int usage(PrintStream output, int code) {
        output.println("Usage: java -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli COMMAND");
        output.println("  --list                            List exact registry operations and contracts");
        output.println("  --run OP[,OP...] FILE              Execute a text DAG over one UTF-8 M3File snapshot");
        output.println("  --find NEEDLE FILE [MAX_HITS]       Exact UTF-16 offsets; explicit completeness receipt");
        output.println("  --ast FILE.java [FILE.java...]     Detached syntax facts from the existing M3AST frontend");
        output.println("  --self-test                       Deterministic composition and replay smoke test");
        output.println("  --help                            Show this help");
        return code;
    }
}
