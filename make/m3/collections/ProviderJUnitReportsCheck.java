// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Fail-closed validation of actual JUnit Jupiter XML for the 15 original
 * source-preserved provider test classes (not merely JUnit selector discovery).
 */
public final class ProviderJUnitReportsCheck {
    private static final int REQUIRED_CLASSES = 15;

    private ProviderJUnitReportsCheck() { }

    private static int count(Element suite, String attribute) {
        String value = suite.getAttribute(attribute);
        if (!value.matches("[0-9]+")) {
            throw new IllegalStateException("missing/invalid JUnit " + attribute);
        }
        return Integer.parseInt(value);
    }

    /** Original JUnit class inventory and JUnit Jupiter report. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: ProviderJUnitReportsCheck selected-classes.txt TEST-junit-jupiter.xml");
        }
        Path selected = Path.of(args[0]);
        Path report = Path.of(args[1]);
        if (!Files.isRegularFile(selected, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(report, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("missing/symlinked JUnit inputs");
        }
        List<String> requested = Files.readAllLines(selected, StandardCharsets.UTF_8);
        if (requested.size() != REQUIRED_CLASSES
                || new HashSet<>(requested).size() != REQUIRED_CLASSES
                || requested.stream().anyMatch(name ->
                        !name.matches("jdk[.]m3[.]collections[.][A-Za-z0-9_.]+Test"))) {
            throw new IllegalStateException("incorrect provider JUnit class inventory");
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element suite = factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
        if (!"testsuite".equals(suite.getTagName())
                || !"JUnit Jupiter".equals(suite.getAttribute("name"))
                || count(suite, "failures") != 0 || count(suite, "errors") != 0
                || count(suite, "skipped") != 0) {
            throw new IllegalStateException("JUnit Jupiter test suite not fully successful");
        }
        NodeList results = suite.getElementsByTagName("testcase");
        int testCases = count(suite, "tests");
        if (testCases < REQUIRED_CLASSES || results.getLength() != testCases) {
            throw new IllegalStateException("missing or inconsistent JUnit testcases");
        }
        Map<String, Integer> executed = new HashMap<>();
        for (int i = 0; i < results.getLength(); i++) {
            Element test = (Element) results.item(i);
            String owner = test.getAttribute("classname");
            String match = null;
            for (String expected : requested) {
                if (owner.equals(expected) || owner.startsWith(expected + "$")) {
                    if (match != null) {
                        throw new IllegalStateException("ambiguous JUnit testcase: " + owner);
                    }
                    match = expected;
                }
            }
            if (match == null || test.getElementsByTagName("failure").getLength() != 0
                    || test.getElementsByTagName("error").getLength() != 0
                    || test.getElementsByTagName("skipped").getLength() != 0) {
                throw new IllegalStateException("unexpected/failed JUnit testcase: " + owner);
            }
            executed.merge(match, 1, Integer::sum);
        }
        Set<String> missing = new HashSet<>(requested);
        missing.removeAll(executed.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("unexecuted JUnit provider classes: " + missing);
        }
        System.out.println("M3_PROVIDER_JUNIT_REPORT_PASS classes=" + REQUIRED_CLASSES
                + " tests=" + testCases);
    }
}
