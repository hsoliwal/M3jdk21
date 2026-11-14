// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChromeBuiltInAiAdaptersTest {

    @Test
    void exposesAllRequestedAdaptersThroughOneBoundedTaskService() throws Exception {
        ChromeBuiltInAiAdapters adapters = new ChromeBuiltInAiAdapters(new FixtureRunner());

        assertEquals(ChromeMlTaskService.Kind.PROMPT, adapters.prompt().run("Say OK").kind());
        assertEquals(ChromeMlTaskService.Kind.WRITE, adapters.writer().write("Draft a note").kind());
        assertEquals(
                ChromeMlTaskService.Kind.REWRITE,
                adapters.rewriter().rewrite("hello", "formal").kind());
        assertEquals(
                ChromeMlTaskService.Kind.PROOFREAD,
                adapters.proofreader().proofread("hello world").kind());
        assertEquals(
                ChromeMlTaskService.Kind.TRANSLATE,
                adapters.translator().translate("hello", "French").kind());
        assertEquals(
                ChromeMlTaskService.Kind.SUMMARY,
                adapters.summarizer().summarize("one two three").kind());

        ChromeMlTaskService.Result detected =
                adapters.languageDetector().detect("hello", List.of("English", "French"));
        assertEquals(ChromeMlTaskService.Kind.LANGUAGE_DETECT, detected.kind());
        assertTrue(detected.schemaValidated());
        assertEquals("{\"label\":\"English\"}", detected.text());
        assertFalse(detected.executionAuthority());
    }

    @Test
    void languageDetectorFailsClosedWithoutExplicitVocabulary() {
        ChromeBuiltInAiAdapters adapters = new ChromeBuiltInAiAdapters(new FixtureRunner());
        assertThrows(
                IllegalArgumentException.class,
                () -> adapters.languageDetector().detect("hello", List.of()));
    }

    private static final class FixtureRunner implements ChromeMlRunner {
        @Override
        public ChromeMlRunnerResult generate(
                ChromeMlRunnerRequest request,
                TokenSink sink) throws Exception {
            String text = request.prompt().contains("one key label")
                    ? "{\"label\":\"English\"}"
                    : "candidate";
            sink.accept(text);
            return new ChromeMlRunnerResult(text, 1, "", "");
        }
    }
}
