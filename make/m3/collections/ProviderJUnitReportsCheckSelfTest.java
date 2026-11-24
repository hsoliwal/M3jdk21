// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Synthetic valid Jupiter XML plus 12 deliberate no-test/partial/incorrect-report refusals. */
public final class ProviderJUnitReportsCheckSelfTest {
    private static final int CLASSES = 15;
    private static int refused;

    private ProviderJUnitReportsCheckSelfTest() { }

    private static String source(int number) {
        return "jdk.m3.collections.eclipse.impl.Sample" + number + "Test";
    }

    private static String suite(int cases) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\"?>\n");
        xml.append("<testsuite name=\"JUnit Jupiter\" tests=\"").append(cases)
                .append("\" skipped=\"0\" failures=\"0\" errors=\"0\">\n");
        for (int i = 0; i < cases; i++) {
            xml.append("<testcase name=\"smoke()\" classname=\"")
                    .append(source(i)).append("\"/>\n");
        }
        return xml.append("</testsuite>\n").toString();
    }

    private static void mustRefuse(Path selections, Path xml, String label) throws Exception {
        try {
            ProviderJUnitReportsCheck.main(new String[] {
                    selections.toString(), xml.toString() });
            throw new AssertionError("accepted invalid Jupiter report: " + label);
        } catch (IllegalStateException | org.xml.sax.SAXException expected) {
            refused++;
        }
    }

    private static void badXml(Path selected, Path report, String contents, String why)
            throws Exception {
        Files.writeString(report, contents, StandardCharsets.UTF_8);
        mustRefuse(selected, report, why);
    }

    private static void rm(Path directory) throws IOException {
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no parameters");
        Path tmp = Files.createTempDirectory("m3-provider-junit-report-");
        try {
            Path selected = tmp.resolve("selected-classes.txt");
            Path report = tmp.resolve("TEST-junit-jupiter.xml");
            List<String> requested = new ArrayList<>();
            for (int i = 0; i < CLASSES; i++) requested.add(source(i));
            Files.write(selected, requested, StandardCharsets.UTF_8);
            String clean = suite(CLASSES);
            Files.writeString(report, clean, StandardCharsets.UTF_8);
            ProviderJUnitReportsCheck.main(new String[] {
                    selected.toString(), report.toString() });
            badXml(selected, report, suite(0), "zero discovered tests");
            badXml(selected, report, suite(CLASSES - 1), "one selected class missing");
            badXml(selected, report, clean.replace("tests=\"15\"", "tests=\"14\""),
                    "JUnit testcase count mismatch");
            badXml(selected, report, clean.replace("classname=\"" + source(0),
                    "classname=\"jdk.m3.collections.other.Unknown"), "unknown testcase");
            badXml(selected, report, clean.replace("skipped=\"0\"", "skipped=\"1\""),
                    "disabled Jupiter tests");
            badXml(selected, report, clean.replace("failures=\"0\"", "failures=\"1\""),
                    "failing JUnit assertion");
            badXml(selected, report, clean.replace("errors=\"0\"", "errors=\"1\""),
                    "test execution error");
            badXml(selected, report, clean.replace("<testcase name=\"smoke()\" classname=\""
                    + source(0) + "\"/>", "<testcase name=\"smoke()\" classname=\""
                    + source(0) + "\"><skipped/></testcase>"), "hidden skipped testcase");
            badXml(selected, report, clean.replace("<?xml version=\"1.0\"?>",
                    "<?xml version=\"1.0\"?><!DOCTYPE testsuite [<!ENTITY ext SYSTEM 'file:///etc/passwd'>]>"),
                    "external entity/DTD");
            badXml(selected, report, clean.replace("name=\"JUnit Jupiter\"",
                    "name=\"Unknown Engine\""), "wrong engine");
            Files.writeString(report, clean, StandardCharsets.UTF_8);
            List<String> duplicated = new ArrayList<>(requested);
            duplicated.set(1, requested.get(0));
            Files.write(selected, duplicated, StandardCharsets.UTF_8);
            mustRefuse(selected, report, "duplicate selected classes");
            Files.write(selected, requested, StandardCharsets.UTF_8);
            Files.delete(report);
            mustRefuse(selected, report, "missing generated report");
            if (refused != 12) throw new AssertionError("expected 12 refusals: " + refused);
            System.out.println("M3_PROVIDER_JUNIT_REPORT_SELFTEST_PASS negatives=12");
        } finally {
            rm(tmp);
        }
    }
}
