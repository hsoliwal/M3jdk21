// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.xml.Assertions.xml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class M3VendoredPinRecipeTest implements RewriteTest {

    static final String SUFFIX = "-m3-source-1";
    static final String MODULES = "donor\tkind\tpath\tgroup\tartifact\tversion\tstatus\r\n"
            + "lib-1.0\tMODULE\tp/lib\torg.demo\tlib\t1.1-m3-source-1\tPASS\r\n"
            + "lib-1.1\tMODULE\tp/lib\torg.demo\tlib\t1.0-m3-source-1\tPASS\r\n"
            + "native-2.0\tMODULE\tp/native\torg.demo\tnative\t2.0-m3-source-1\tPASS\r\n"
            + "image-3.0\tIMAGE\tp/image\torg.demo\timage\t3.0-m3-source-1\tPASS\r\n"
            + "rewrite-core-8.17.1\tMODULE\tp/rc\torg.openrewrite\trewrite-core\t8.17.1-m3-source-1\tPASS\r\n";

    @TempDir
    static Path dir;
    static Path modules;

    @BeforeAll
    static void modulesTable() throws Exception {
        modules = Files.writeString(dir.resolve("THIRD_PARTY_MODULES.tsv"), MODULES, StandardCharsets.UTF_8);
    }

    static M3VendoredPinRecipe recipe() {
        return new M3VendoredPinRecipe(modules.toString(), PyText.sha256(MODULES), SUFFIX, List.of("native"), "rewrite.version=8.17.1");
    }

    static String pom(String dependencies) {
        return "<project>\n  <dependencies>\n" + dependencies + "  </dependencies>\n</project>\n";
    }

    static String dep(String group, String artifact, String version) {
        return "    <dependency><groupId>" + group + "</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version></dependency>\n";
    }

    static M3VendoredPinRows.Row row(String g, String a, String declared, String shown, String action, String resolved) {
        return new M3VendoredPinRows.Row("pom.xml", g, a, declared, shown, action, resolved);
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(recipe());
    }

    @Test
    void switchesAnInlinedLiteralVersion() {
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(
                        List.of(row("org.demo", "lib", "1.0", "1.0" + SUFFIX, "SWITCHED", "1.0")), rows)),
                xml(pom(dep("org.demo", "lib", "1.0")), pom(dep("org.demo", "lib", "1.0" + SUFFIX)), s -> s.path("pom.xml")));
    }

    @Test
    void keepsAnAlreadySourceVersion() {
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(
                        List.of(row("org.other", "x", "5" + SUFFIX, "5" + SUFFIX, "ALREADY_SOURCE", "5" + SUFFIX)), rows)),
                xml(pom(dep("org.other", "x", "5" + SUFFIX)), s -> s.path("pom.xml")));
    }

    @Test
    void reportsAVersionThatWasNotInlined() {
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(
                        List.of(row("org.demo", "lib", "9.9", "9.9", "VERSION_NOT_INLINED (inlined: 1.0,1.1)", "9.9")), rows)),
                xml(pom(dep("org.demo", "lib", "9.9") + dep("org.unknown", "y", "1.0")), s -> s.path("pom.xml")));
    }

    @Test
    void keepsNativeInBinaryArtifacts() {
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(
                        List.of(row("org.demo", "native", "2.0", "2.0", "KEPT_BINARY_NATIVE_LIBRARIES", "2.0")), rows)),
                xml(pom(dep("org.demo", "native", "2.0") + dep("org.demo", "image", "3.0")), s -> s.path("pom.xml")));
    }

    @Test
    void leavesPluginDependenciesUntouched() {
        String plugin = "<project>\n  <build><plugins><plugin><artifactId>p</artifactId><dependencies>\n" + dep("org.demo", "lib", "1.0")
                + "  </dependencies></plugin></plugins></build>\n</project>\n";
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(
                        List.of(row("", "lib", "", "", PinPolicy.PLUGIN_UNTOUCHED, "")), rows)),
                xml(plugin, s -> s.path("pom.xml")));
    }

    @Test
    void resolvesPropertiesFromThePomOverTheDefaults() {
        String before = "<project>\n  <properties><lib.version>1.1</lib.version></properties>\n  <dependencies>\n"
                + dep("org.openrewrite", "rewrite-core", "${rewrite.version}") + dep("org.demo", "lib", "${lib.version}") + "  </dependencies>\n</project>\n";
        rewriteRun(spec -> spec.dataTable(M3VendoredPinRows.Row.class, rows -> assertEquals(List.of(
                        row("org.openrewrite", "rewrite-core", "${rewrite.version}", "${rewrite.version}" + SUFFIX, "SWITCHED", "8.17.1"),
                        row("org.demo", "lib", "${lib.version}", "${lib.version}" + SUFFIX, "SWITCHED", "1.1")), rows)),
                xml(before, before.replace("${rewrite.version}<", "${rewrite.version}" + SUFFIX + "<").replace("${lib.version}<", "${lib.version}" + SUFFIX + "<"),
                        s -> s.path("pom.xml")));
    }

    @Test
    void stripsTheDeclaredVersionLikeTheOracle() {
        rewriteRun(xml(pom(dep("org.demo", "lib", " 1.0\r\n ")), pom(dep("org.demo", "lib", "1.0" + SUFFIX)), s -> s.path("pom.xml")));
    }

    @Test
    void secondRunIsAFixedPoint() {
        String once = M3VendoredPinApply.pass(recipe(), "pom.xml", pom(dep("org.demo", "lib", "1.1")).getBytes(StandardCharsets.UTF_8)).after();
        assertEquals(pom(dep("org.demo", "lib", "1.1" + SUFFIX)), once);
        M3VendoredPinApply.Pass twice = M3VendoredPinApply.pass(recipe(), "pom.xml", once.getBytes(StandardCharsets.UTF_8));
        assertEquals(0, twice.changes());
        assertEquals(once, twice.after());
    }

    @Test
    void refusesAModulesTableOffItsPin() {
        M3VendoredPinRecipe drifted = new M3VendoredPinRecipe(modules.toString(), PyText.sha256("other"), SUFFIX, List.of(), "");
        assertThrows(IllegalStateException.class, drifted::getVisitor);
    }

    @Test
    void applyWritesOutDirRowsAndReceiptThenRefusesDrift(@TempDir Path work) throws Exception {
        Path repo = Files.createDirectories(work.resolve("repo"));
        Files.writeString(repo.resolve("pom.xml"), pom(dep("org.demo", "lib", "1.0")), StandardCharsets.UTF_8);
        git(repo, "init", "-q");
        git(repo, "add", "pom.xml");
        git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "base");
        Path targets = Files.writeString(work.resolve("targets.tsv"), "pom.xml\texplicit\r\nmissing/pom.xml\texplicit\r\n", StandardCharsets.UTF_8);
        String[] args = {"--repo", repo.toString(), "--base", "HEAD", "--modules", modules.toString(), "--modules-sha256", PyText.sha256(MODULES),
            "--suffix", SUFFIX, "--targets", targets.toString(), "--out-dir", work.resolve("out").toString(),
            "--rows", work.resolve("ROWS.tsv").toString(), "--receipt", work.resolve("RECEIPT.tsv").toString()};
        assertEquals(0, M3VendoredPinApply.apply(M3VendoredPinApply.Options.parse(args)));
        assertEquals(pom(dep("org.demo", "lib", "1.0" + SUFFIX)), Files.readString(work.resolve("out/pom.xml"), StandardCharsets.UTF_8));
        assertEquals(M3VendoredPinRows.HEADER + "\npom.xml\torg.demo\tlib\t1.0\t1.0" + SUFFIX + "\tSWITCHED\t1.0\nmissing/pom.xml\t\t\t\t\tPOM_NOT_FOUND\t\n",
                Files.readString(work.resolve("ROWS.tsv"), StandardCharsets.UTF_8));
        String receipt = Files.readString(work.resolve("RECEIPT.tsv"), StandardCharsets.UTF_8);
        assertTrue(receipt.contains("FIXED_POINT\ttrue\tfirst_run_changes=1\tsecond_run_changes=0"), receipt);
        assertEquals(0, M3VendoredPinApply.apply(M3VendoredPinApply.Options.parse(args)), "same inputs, same bytes: no drift");
        Files.writeString(work.resolve("out/pom.xml"), "<project/>\n", StandardCharsets.UTF_8);
        assertEquals(3, M3VendoredPinApply.apply(M3VendoredPinApply.Options.parse(args)));
        assertEquals("<project/>\n", Files.readString(work.resolve("out/pom.xml"), StandardCharsets.UTF_8));
    }

    static void git(Path repo, String... args) throws Exception {
        List<String> command = new java.util.ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), out);
    }
}
