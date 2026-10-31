// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/**
 * Runs {@link M3VendoredPinRecipe} over a target list of poms read from {@code git -C <repo> show <base>:<pom>} and
 * writes the would-be poms to an out dir (never the repository), the rows TSV in the oracle's exact schema, and a
 * receipt TSV (pins, per-pom before/after SHA-256, fixed point). Exit codes: 0 done, 3 drift refusal (an out-dir pom
 * holds other bytes than this run computes; nothing is written), 4 not a fixed point. Refused inputs (modules table
 * off its pin, non-canonical target path, a pom that is not XML) throw.
 * <p>usage: M3VendoredPinApply --repo R --base B --modules TSV --modules-sha256 H --suffix S [--targets TSV]
 * [--default-props k=v,...] [--native a,b] --out-dir D --rows ROWS.tsv --receipt RECEIPT.tsv
 */
public final class M3VendoredPinApply {

    /** {@code switch_consumers.TARGETS}: the oracle's target list when no targets file is given. */
    static final List<String> DEFAULT_TARGETS = List.of("synexia-bom/pom.xml", "pom.xml", "synexia-indexstring/pom.xml",
            "synexia-mindex/codec-toml/pom.xml", "synexia-mindex/codec-yaml/pom.xml", "synexia-mindex/runtime/pom.xml",
            "synexia-mindex/integration/fixed-univocity/pom.xml", "synexia-mindex/pom.xml",
            "synexia-openrewrite-recipes/pom.xml", "synexia-common/pom.xml");
    static final String POM_NOT_FOUND = "POM_NOT_FOUND";

    private M3VendoredPinApply() {
    }

    /** Parsed command line. */
    public record Options(Path repo, String base, Path modules, String modulesSha256, String suffix, Optional<Path> targets,
                          String defaultProps, List<String> nativeInBinary, Path outDir, Path rows, Path receipt) {

        public static Options parse(String... args) {
            Map<String, String> m = new HashMap<>();
            IntStream.iterate(0, i -> i + 1 < args.length, i -> i + 2).forEach(i -> m.put(args[i], args[i + 1]));
            List<String> natives = Arrays.stream(m.getOrDefault("--native", "").split(",")).filter(s -> !s.isEmpty()).toList();
            return new Options(Path.of(need(m, "--repo")), need(m, "--base"), Path.of(need(m, "--modules")), need(m, "--modules-sha256"),
                    need(m, "--suffix"), Optional.ofNullable(m.get("--targets")).map(Path::of), m.getOrDefault("--default-props", ""),
                    natives, Path.of(need(m, "--out-dir")), Path.of(need(m, "--rows")), Path.of(need(m, "--receipt")));
        }

        private static String need(Map<String, String> m, String key) {
            return Optional.ofNullable(m.get(key)).orElseThrow(() -> new IllegalArgumentException("missing " + key));
        }
    }

    /** One recipe pass over one pom. */
    record Pass(String after, List<M3VendoredPinRows.Row> rows, int changes) {
    }

    /** First-run outcome per target (in target order; a repeated target is processed again, like the oracle). */
    record Outcome(String target, Optional<byte[]> base, Pass pass) {
        boolean found() {
            return base.isPresent();
        }
    }

    public static void main(String[] args) {
        System.exit(apply(Options.parse(args)));
    }

    public static int apply(Options o) {
        List<String> targets = targets(o.targets());
        M3VendoredPinRecipe recipe = new M3VendoredPinRecipe(o.modules().toString(), o.modulesSha256(), o.suffix(), o.nativeInBinary(), o.defaultProps());
        List<Outcome> first = targets.stream().map(t -> outcome(recipe, t, Git.show(o.repo(), o.base(), t))).toList();
        String pins = pins(o, targets);
        List<String> drifted = first.stream().filter(x -> drifted(o.outDir(), x)).map(Outcome::target).toList();
        if (!drifted.isEmpty()) {
            write(o.receipt(), pins + drifted.stream().map(t -> "REFUSED\t" + t + "\t\t\tDRIFT_IN_OUT_DIR\n").collect(joining()));
            return 3;
        }
        first.stream().filter(Outcome::found).forEach(x -> write(o.outDir().resolve(x.target()), x.pass().after()));
        write(o.rows(), Stream.concat(Stream.of(M3VendoredPinRows.HEADER), first.stream().flatMap(x -> x.pass().rows().stream())
                .map(M3VendoredPinRows.Row::tsv)).collect(joining("\n")) + "\n");
        int second = first.stream().filter(Outcome::found).mapToInt(x -> pass(recipe, x.target(), x.pass().after().getBytes(UTF_8)).changes()).sum();
        write(o.receipt(), pins + first.stream().map(M3VendoredPinApply::receiptRow).collect(joining()) + fixedPoint(first, second));
        return second == 0 ? 0 : 4;
    }

    private static Outcome outcome(M3VendoredPinRecipe recipe, String target, Optional<byte[]> base) {
        Pass pass = base.map(bytes -> pass(recipe, target, bytes))
                .orElseGet(() -> new Pass("", List.of(new M3VendoredPinRows.Row(target, "", "", "", "", POM_NOT_FOUND, "")), 0));
        return new Outcome(target, base, pass);
    }

    /** Parse (UTF-8, as the oracle decodes), run the recipe once, print. A UTF-8 BOM is kept as the oracle keeps it. */
    static Pass pass(M3VendoredPinRecipe recipe, String target, byte[] bytes) {
        boolean bom = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
        byte[] body = bom ? Arrays.copyOfRange(bytes, 3, bytes.length) : bytes;
        ExecutionContext ctx = context();
        List<SourceFile> parsed = new XmlParser().parseInputs(List.of(new Parser.Input(Path.of(target), () -> new ByteArrayInputStream(body))), null, ctx).toList();
        if (parsed.size() != 1 || !(parsed.get(0) instanceof Xml.Document)) {
            throw new IllegalStateException("not parseable as XML: " + target);
        }
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(parsed), ctx);
        List<Result> results = run.getChangeset().getAllResults();
        String after = results.isEmpty() ? new String(bytes, UTF_8) : (bom ? "﻿" : "") + results.get(0).getAfter().printAll();
        return new Pass(after, List.copyOf(run.<M3VendoredPinRows.Row>getDataTableRows(M3VendoredPinRows.class.getName())), results.size());
    }

    private static ExecutionContext context() {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new IllegalStateException(t);
        });
        ParsingExecutionContextView.view(ctx).setCharset(UTF_8);
        return ctx;
    }

    /** The oracle's {@code --targets} reader: universal newlines, blank lines skipped, at least two TAB columns. */
    static List<String> targets(Optional<Path> file) {
        List<String> targets = file.map(f -> Arrays.stream(new String(PyText.readBytes(f), UTF_8).split("\r\n|\n|\r", -1))
                .filter(l -> !PyText.strip(l).isEmpty()).map(l -> l.split("\t", -1))
                .map(c -> c.length >= 2 ? c[0] : fail("targets line needs <pom path><TAB><mode>: " + String.join("\t", c)))
                .toList()).orElse(DEFAULT_TARGETS);
        targets.stream().filter(t -> !PyText.slash(Path.of(t)).equals(t)).findFirst()
                .ifPresent(t -> fail("non-canonical target path (row pom column would differ): " + t));
        return targets;
    }

    private static String fail(String message) {
        throw new IllegalArgumentException(message);
    }

    private static boolean drifted(Path outDir, Outcome x) {
        Path out = outDir.resolve(x.target());
        return x.found() && Files.exists(out) && !Arrays.equals(PyText.readBytes(out), x.pass().after().getBytes(UTF_8));
    }

    private static String pins(Options o, List<String> targets) {
        return "kind\tkey\tbefore_sha256\tafter_sha256\tnote\n"
                + "PIN\tbase\t" + o.base() + "\t" + Git.commit(o.repo(), o.base()) + "\tbase commit\n"
                + "PIN\tmodules\t" + PyText.slash(o.modules()) + "\t" + o.modulesSha256() + "\tsha256 pin (verified)\n"
                + "PIN\tsuffix\t" + o.suffix() + "\t\t\n"
                + "PIN\tdefault_props\t" + o.defaultProps() + "\t\t\n"
                + "PIN\tnative_in_binary\t" + String.join(",", o.nativeInBinary()) + "\t\t\n"
                + "PIN\ttargets\t" + o.targets().map(PyText::slash).orElse("DEFAULT_TARGETS") + "\t" + PyText.sha256(String.join("\n", targets)) + "\ttarget list sha256\n";
    }

    private static String receiptRow(Outcome x) {
        String before = x.base().map(PyText::sha256).orElse("ABSENT");
        String after = x.found() ? PyText.sha256(x.pass().after()) : "";
        String change = !x.found() ? POM_NOT_FOUND : x.pass().changes() > 0 ? "CHANGED" : "UNCHANGED";
        return "POM\t" + x.target() + "\t" + before + "\t" + after + "\t" + change + " rows=" + x.pass().rows().size() + "\n";
    }

    private static String fixedPoint(List<Outcome> first, int second) {
        int changes = first.stream().mapToInt(x -> x.pass().changes()).sum();
        return "FIXED_POINT\t" + (second == 0) + "\tfirst_run_changes=" + changes + "\tsecond_run_changes=" + second + "\t\n";
    }

    static void write(Path path, String text) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.write(path, text.getBytes(UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
