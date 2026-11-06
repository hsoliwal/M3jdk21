// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/CountOnlyNativeSurefireGateTest.java (blob 3b25b6aa7c46) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Independent negative controls for true, unskipped native Surefire execution receipts. */
final class CountOnlyNativeSurefireGateTest {
    private static final String PREFIX = "jdk.m3.search.";
    private static final Map<String, List<String>> METHODS = Map.of(
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

    private static String report(String simple, String wrongMethod, String suiteAttrs, String suffix) {
        String name = PREFIX + simple;
        StringBuilder body = new StringBuilder();
        for (String method : METHODS.get(simple)) {
            String key = method.equals(wrongMethod) ? "decoy" : method;
            body.append("<testcase classname=\"").append(name).append("\" name=\"")
                    .append(key).append("\"/>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\""
                + name + "\" tests=\"" + METHODS.get(simple).size()
                + "\" failures=\"0\" errors=\"0\" skipped=\"0\""
                + suiteAttrs + ">\n" + body + suffix + "</testsuite>";
    }

    private static Path populate() throws IOException {
        Path dir = Files.createTempDirectory("m3-jni-surefire-");
        for (String name : METHODS.keySet()) {
            Files.writeString(dir.resolve("TEST-" + PREFIX + name + ".xml"),
                    report(name, null, "", ""));
        }
        return dir;
    }

    private static void clear(Path dir) throws IOException {
        try (var paths = Files.list(dir)) {
            for (Path entry : paths.toList()) Files.deleteIfExists(entry);
        }
        Files.deleteIfExists(dir);
    }

    @Test void allRequiredRealSuiteCasesAreAcceptedAndCounted() throws IOException {
        Path dir = populate();
        try { assertEquals(25, CountOnlyNativeSurefireGate.verify(dir)); }
        finally { clear(dir); }
    }

    @Test void missingAndReplacedOrSkippedNativeMethodsFailClosed() throws IOException {
        Path dir = populate();
        try {
            Path required = dir.resolve("TEST-" + PREFIX + "CountOnlyCrossBackendJniTest.xml");
            Files.delete(required);
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            Files.writeString(required, report("CountOnlyCrossBackendJniTest", "periodicDenseKmpFallbackAndBinaryOffsetsMatchNativeCount", "", ""));
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            String skipped = report("CountOnlyCrossBackendJniTest", null, "", "");
            skipped = skipped.replace(
                    "name=\"seededIndependentByteOracleMatchesAllThreeCompiledBackends\"/>",
                    "name=\"seededIndependentByteOracleMatchesAllThreeCompiledBackends\"><skipped/></testcase>");
            Files.writeString(required, skipped);
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
        } finally { clear(dir); }
    }

    @Test void missingSuiteCountsAndSpoofedCaseFailureAreRejected() throws IOException {
        Path dir = populate();
        try {
            Path nativeSuite = dir.resolve("TEST-" + PREFIX + "JniSearchEngineNativeTest.xml");
            Files.writeString(nativeSuite, report("JniSearchEngineNativeTest", null, "", "<failure/>").replace(
                    "tests=\"14\"", "tests=\"0\""));
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            Files.writeString(nativeSuite, report("JniSearchEngineNativeTest", null, "", "")
                    .replace("classname=\"" + PREFIX + "JniSearchEngineNativeTest\"",
                            "classname=\"foreign\""));
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
        } finally { clear(dir); }
    }

    @Test void flakyAndRerunFailuresCannotQualifyNativeSearch() throws IOException {
        Path dir = populate();
        try {
            Path suite = dir.resolve("TEST-" + PREFIX + "CountOnlyNativeSurefireGateTest.xml");
            String original = report("CountOnlyNativeSurefireGateTest", null, "", "");
            String first = "name=\"allRequiredRealSuiteCasesAreAcceptedAndCounted\"/>";
            assertTrue(original.contains(first));
            for (String event : List.of("flakyFailure", "flakyError",
                    "rerunFailure", "rerunError")) {
                String injection = "name=\"allRequiredRealSuiteCasesAreAcceptedAndCounted\">"
                        + "<" + event + " message=\"first attempt failed\"/>" + "</testcase>";
                Files.writeString(suite, original.replace(first, injection));
                assertThrows(IllegalStateException.class,
                        () -> CountOnlyNativeSurefireGate.verify(dir));
            }
            // A failure marker appearing as ordinary stdout text is not a failure event.
            Files.writeString(suite, original.replace(first,
                    "name=\"allRequiredRealSuiteCasesAreAcceptedAndCounted\">"
                            + "<system-out>flakyFailure is a search term</system-out></testcase>"));
            assertEquals(25, CountOnlyNativeSurefireGate.verify(dir));
        } finally { clear(dir); }
    }

    @Test void maliciousDoctypeAndLinkedReportAreRejected() throws IOException {
        Path dir = populate();
        try {
            Path report = dir.resolve("TEST-" + PREFIX + "CountOnlyNativeBinarySealTest.xml");
            String legitimate = Files.readString(report);
            Files.writeString(report, legitimate.replace("?>\n", "?>\n<!DOCTYPE testsuite [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>\n"));
            assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            Files.delete(report);
            try {
                Files.createSymbolicLink(report,
                        Path.of("TEST-" + PREFIX + "JniSearchEngineNativeTest.xml"));
                assertTrue(Files.isSymbolicLink(report));
                assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            } catch (UnsupportedOperationException | SecurityException | IOException unsupported) {
                assertThrows(IllegalStateException.class, () -> CountOnlyNativeSurefireGate.verify(dir));
            }
        } finally { clear(dir); }
    }
}
