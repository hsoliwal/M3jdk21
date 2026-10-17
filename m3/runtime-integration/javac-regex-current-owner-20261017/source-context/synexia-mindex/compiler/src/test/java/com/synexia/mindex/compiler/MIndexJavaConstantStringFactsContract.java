// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler;

import com.synexia.indexstring.MIndexJdkConstantRegexPlan;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Executable JDK21 contract for compiler facts; no Maven/JUnit dependency. */
public final class MIndexJavaConstantStringFactsContract {
    private static long checks;

    private MIndexJavaConstantStringFactsContract() {}

    private static void check(boolean ok, String detail) {
        checks++;
        if (!ok) throw new AssertionError(detail);
    }

    private static String source() {
        return """
                import java.util.regex.Pattern;
                class CompileTimeFixture {
                  static final String TOKEN = "ab";
                  static final String REGEX = TOKEN + "[cd]+";
                  static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
                  static class Mimic { boolean matches(String value) { return false; } }
                  void probe(String input, String dynamic) {
                    input.matches(REGEX);
                    input.matches("ab" + "[cd]+");
                    input.contains(TOKEN);
                    input.startsWith("ab");
                    input.endsWith("cd");
                    input.indexOf(TOKEN);
                    input.split("[,;]+", -1);
                    input.replaceAll(REGEX, "$0");
                    input.replaceFirst(REGEX, "x");
                    Pattern.compile(REGEX);
                    Pattern.compile(REGEX, FLAGS);
                    Pattern.matches(REGEX, input);
                    Pattern.compile("[");
                    Pattern.compile("a", -1);
                    Pattern.compile(dynamic);
                    new Mimic().matches(REGEX);
                  }
                }
                """;
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-javac-regex");
        Path fixture = directory.resolve("CompileTimeFixture.java");
        try {
            Files.writeString(fixture, source(), StandardCharsets.UTF_8);
            var report = MIndexJavaConstantStringFacts.analyze(
                    fixture, List.of("--release", "21", "-Xlint:all"));
            check(report.sites().size() == 14, "14 symbol-resolved constant callsites");
            check(report.dynamicSites() == 1, "dynamic Pattern.compile refused");
            check(report.eligibleSites() == 12L, "two invalid patterns ineligible");
            check(report.tsv().startsWith("line\tcolumn\toperation"),
                    "deterministic TSV heading");
            check(report.sites().stream().anyMatch(x -> x.operation()
                    == MIndexJavaConstantStringFacts.Operation.STRING_CONTAINS),
                    "literal contains evidence");
            check(report.sites().stream().anyMatch(x -> x.operation()
                    == MIndexJavaConstantStringFacts.Operation.STRING_INDEX_OF),
                    "overload-resolved String.indexOf evidence");
            check(report.sites().stream().noneMatch(x -> x.constant().equals("dynamic")),
                    "never invent a runtime variable's regex value");
            List<MIndexJavaConstantStringFacts.Site> shared = report.sites().stream()
                    .filter(x -> x.constant().equals("ab[cd]+") && x.flags() == 0
                            && x.operation().regex()).toList();
            check(shared.size() >= 4, "multiple operations share the same exact regex");
            String root = shared.get(0).planSha256();
            check(shared.stream().allMatch(x -> x.planSha256().equals(root)),
                    "cross-operation regex plans share one stable fingerprint");
            var prepared = report.preparedRegexPlans();
            check(prepared.containsKey(root), "compile-once plan admitted across operations");
            check(prepared.get(root).matches("ABcc") == false,
                    "default flags preserve case sensitivity");
            check(prepared.get(root).matches("abcc"),
                    "shared plan exactly matches the constant regex");
            check(prepared.size() < report.sites().stream()
                    .filter(x -> x.operation().regex() && x.eligible()).count(),
                    "one immutable prepared Pattern replaces repeated site compilations");
            boolean immutable = false;
            try { prepared.clear(); }
            catch (UnsupportedOperationException expected) { immutable = true; }
            check(immutable, "prepared plan map is immutable");
            var configured = report.sites().stream()
                    .filter(x -> x.flags() == (Pattern.CASE_INSENSITIVE | Pattern.MULTILINE))
                    .findFirst().orElseThrow();
            check(configured.eligible(), "static integer flags resolved with javac");
            check(!configured.planSha256().equals(root), "flags are semantic dependencies");

            for (var site : report.sites()) {
                check(site.sourceSha256().equals(report.sourceSha256()), "bound source digest");
                check(site.line() > 0 && site.column() > 0, "original file coordinates");
                if (site.operation().regex() && site.eligible()) {
                    MIndexJdkConstantRegexPlan plan = site.prepareRegex();
                    check(plan.expression().equals(site.constant()), "JDK Pattern retains input");
                    check(plan.flags() == site.flags(), "JDK Pattern retains flags");
                } else {
                    boolean refused = false;
                    try { site.prepareRegex(); }
                    catch (IllegalStateException expected) { refused = true; }
                    check(refused, "only eligible regex sites prepare");
                }
            }
            check(report.sites().stream().filter(x -> !x.eligible()).count() == 2,
                    "invalid regex/flags remain diagnostic-only");
            check(report.sites().stream().anyMatch(x -> x.rejection().equals("invalid-regex")),
                    "record failed PatternSyntaxException without compile-time movement");
            check(report.sites().stream().anyMatch(x -> x.rejection().equals("invalid-flags")),
                    "record failed JDK flag validation");

            // External edits invalidate source provenance but not an unchanged
            // literal's canonical semantic regex plan.
            Files.writeString(fixture, source() + "\n// harmless source edit\n",
                    StandardCharsets.UTF_8);
            var second = MIndexJavaConstantStringFacts.analyze(
                    fixture, List.of("--release", "21"));
            check(!second.sourceSha256().equals(report.sourceSha256()),
                    "exact source modification invalidates source hash");
            check(second.sites().get(0).planSha256().equals(report.sites().get(0).planSha256()),
                    "same constant expression preserves semantic plan fingerprint");

            String loneHigh = new String(new char[] {(char) 0xD800});
            var lowLevel = new MIndexJavaConstantStringFacts.Site(
                    fixture, 1, 1,
                    MIndexJavaConstantStringFacts.Operation.STRING_CONTAINS,
                    loneHigh, 0, true, "", report.sourceSha256(), root);
            check(lowLevel.constantUtf16Hex().equals("d800"),
                    "UTF-16 identity preserves unpaired surrogate code units");
            check(!Objects.equals(Arrays.toString(lowLevel.constant().getBytes(StandardCharsets.UTF_8)),
                    lowLevel.constantUtf16Hex()), "UTF-8 replacement not the plan key");

            // A compiler error cannot produce partially trusted evidence.
            Files.writeString(fixture, "class CompileTimeFixture { MissingType x; }",
                    StandardCharsets.UTF_8);
            boolean compileRefused = false;
            try {
                MIndexJavaConstantStringFacts.analyze(fixture, List.of("--release", "21"));
            } catch (IllegalArgumentException expected) {
                compileRefused = expected.getMessage().contains("javac refused");
            }
            check(compileRefused, "unresolved Java compiler binding refuses entire report");
            System.out.println("MIndexJavaConstantStringFactsContract checks=" + checks + " PASS");
        } finally {
            Files.deleteIfExists(fixture);
            Files.deleteIfExists(directory);
        }
    }
}
