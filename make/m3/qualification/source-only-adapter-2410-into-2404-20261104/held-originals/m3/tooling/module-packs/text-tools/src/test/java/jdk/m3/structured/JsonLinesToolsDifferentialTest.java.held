/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry points for the dependency-free protected contract corpus. */
public final class JsonLinesToolsDifferentialTest {
    @Test void exactLexemesAndPointers() { JsonLinesToolsContract.exactLexemesAndPointers(); }
    @Test void grammarAndFailureOrder() { JsonLinesToolsContract.grammarAndFailureOrder(); }
    @Test void boundariesAndBudgets() { JsonLinesToolsContract.boundaryAndBudgetContracts(); }
    @Test void collisionsAndArrayIndexes() { JsonLinesToolsContract.collidingObjectKeysAndArrayIndexes(); }
    @Test void generatedLexemesAndMutations() { JsonLinesToolsContract.generatedLexemesAndMutations(); }
    @Test void seededMalformedDocuments() { JsonLinesToolsContract.seededMalformedDocuments(); }
    @Test void parallelReplay() throws Exception { JsonLinesToolsContract.parallelAndRepeatedCalls(); }
    @Test void publicApiUnchanged() { JsonLinesToolsContract.publicApiUnchanged(); }
}
