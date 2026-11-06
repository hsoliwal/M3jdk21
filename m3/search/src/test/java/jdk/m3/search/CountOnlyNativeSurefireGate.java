// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/CountOnlyNativeSurefireGate.java (blob f202422cd0b8) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Fail-closed, test-only receipt check for the original Java/JNI Surefire gate.
 * Requires independently executed, non-skipped cases in each named native suite.
 */
public final class CountOnlyNativeSurefireGate {
    private static final String PACKAGE = "jdk.m3.search.";
    private static final long MAX_XML_BYTES = 8L * 1024L * 1024L;

    private static final Map<String, List<String>> REQUIRED = Map.of(
            "CountOnlyNativeSurefireGateTest", List.of(
                    "allRequiredRealSuiteCasesAreAcceptedAndCounted",
                    "missingAndReplacedOrSkippedNativeMethodsFailClosed",
                    "missingSuiteCountsAndSpoofedCaseFailureAreRejected",
                    "maliciousDoctypeAndLinkedReportAreRejected",
                    "flakyAndRerunFailuresCannotQualifyNativeSearch"),
            "CountOnlyNativeBinarySealTest", List.of(
                    "canonicalBinaryAndExactShaAreRequired",
                    "alteredOrMissingLibraryFailsClosedBeforeJniLoading",
                    "linkedLibraryIsNotTrustedAsAnExactNativeArtifact"),
            "CountOnlyCrossBackendJniTest", List.of(
                    "seededIndependentByteOracleMatchesAllThreeCompiledBackends",
                    "periodicDenseKmpFallbackAndBinaryOffsetsMatchNativeCount",
                    "cancellationAndConsumerFailureNeverInvalidateBorrowedJniPattern"),
            "JniSearchEngineNativeTest", List.of(
                    "nativeProviderIsExplicitAndDifferentiallyCorrect",
                    "nativeDirectBufferSearchUsesRemainingRangeAndPreservesBufferState",
                    "closedNativePatternCannotBeUsed",
                    "cancellationAfterNativeCompilationDoesNotPublishTheHandle",
                    "nativeCompiledPatternCanEnumerateOverlappingOffsetsWithoutTransferringOwnership",
                    "bulkNativeCountMatchesIndependentOverlappingOracle",
                    "boundedNativePositionsMatchIndependentOracleAndKeepTheHandleBorrowed",
                    "monitoredBulkMatchesIndependentOraclesWithoutChangingProgressUnits",
                    "nativeBulkScansPollBeforeReturningOrDeliveringPartialPositions",
                    "nativePrefixFailureChainCanBeCanceledWithoutPublishingAPartialTable",
                    "nativeScanFailureChainDoesNotHideCancellation",
                    "nativeMonitorLinkageFailuresPropagateByIdentityWithoutFallback",
                    "nativeBorrowDefersReentrantCloseUntilUnwindAndInvalidatesTheHandleImmediately",
                    "nativeBorrowRejectsNestedBulkAtThePrivateEntryBeforeCallingItsMonitor"));

    private CountOnlyNativeSurefireGate() { }

    /** Execute after a *clean* Maven invocation, not against retained prior XML reports. */
    static int verify(Path reports) throws IOException {
        if (reports == null) throw new IllegalArgumentException("Surefire report root is null");
        Path root = reports.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Surefire report directory is missing or linked");
        }
        int total = 0;
        for (var entry : REQUIRED.entrySet()) {
            String className = PACKAGE + entry.getKey();
            Path report = root.resolve("TEST-" + className + ".xml");
            if (!Files.isRegularFile(report, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Missing/linked Surefire report: " + report);
            }
            long size = Files.size(report);
            if (size == 0L || size > MAX_XML_BYTES) {
                throw new IllegalStateException("Empty/oversized Surefire report: " + report);
            }
            total += verifyXml(report, className, entry.getValue());
        }
        return total;
    }

    private static int verifyXml(Path report, String className, List<String> required) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void fatalError(SAXParseException failure) throws SAXException {
                    throw failure;
                }
            });
            var document = builder.parse(report.toFile());
            Element suite = document.getDocumentElement();
            if (suite == null || !"testsuite".equals(suite.getTagName())
                    || !className.equals(suite.getAttribute("name"))) {
                throw new IllegalStateException("Wrong Surefire suite identity: " + report);
            }
            if (number(suite, "failures") != 0 || number(suite, "errors") != 0
                    || number(suite, "skipped") != 0
                    || suite.getElementsByTagName("failure").getLength() != 0
                    || suite.getElementsByTagName("error").getLength() != 0
                    || suite.getElementsByTagName("skipped").getLength() != 0
                    || suite.getElementsByTagName("flakyFailure").getLength() != 0
                    || suite.getElementsByTagName("flakyError").getLength() != 0
                    || suite.getElementsByTagName("rerunFailure").getLength() != 0
                    || suite.getElementsByTagName("rerunError").getLength() != 0) {
                throw new IllegalStateException("Failed/skipped native JNI test suite: " + report);
            }
            NodeList cases = suite.getElementsByTagName("testcase");
            if (number(suite, "tests") != cases.getLength() || cases.getLength() == 0) {
                throw new IllegalStateException("Empty/inconsistent native JNI test cases: " + report);
            }
            Set<String> observed = new HashSet<>();
            for (int i = 0; i < cases.getLength(); i++) {
                Node node = cases.item(i);
                if (!(node instanceof Element test) || node.getParentNode() != suite
                        || !className.equals(test.getAttribute("classname"))
                        || test.getAttribute("name").isBlank()
                        || !observed.add(test.getAttribute("name"))) {
                    throw new IllegalStateException("Duplicate/foreign native JNI case: " + report);
                }
                if (test.getElementsByTagName("skipped").getLength() != 0
                        || test.getElementsByTagName("failure").getLength() != 0
                        || test.getElementsByTagName("error").getLength() != 0) {
                    throw new IllegalStateException("Hidden failure/skip in native JNI case: " + report);
                }
            }
            if (!observed.containsAll(required)) {
                throw new IllegalStateException("Required JNI test methods absent: " + report);
            }
            return cases.getLength();
        } catch (IOException | ParserConfigurationException | SAXException failure) {
            throw new IllegalStateException("Malformed/untrusted Surefire XML: " + report, failure);
        }
    }

    private static int number(Element suite, String name) {
        String value = suite.getAttribute(name);
        if (!value.matches("[0-9]+")) {
            throw new IllegalStateException("Missing/invalid Surefire " + name);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IllegalStateException("Overflowing Surefire " + name, failure);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: CountOnlyNativeSurefireGate <module-surefire-reports>");
        }
        int cases = verify(Path.of(args[0]));
        System.out.println("PASS M3_COUNT_ONLY_REAL_JNI_SUREFIRE_CASES=" + cases
                + " SUITES=" + REQUIRED.size() + " failures=0 errors=0 skipped=0");
    }
}
