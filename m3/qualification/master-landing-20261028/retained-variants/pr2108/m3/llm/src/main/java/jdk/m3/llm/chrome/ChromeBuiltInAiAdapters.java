// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Thin Java adapters matching Chrome's built-in AI task vocabulary while sharing one bounded
 * {@link ChromeMlTaskService} execution path.
 *
 * <p>Every result remains candidate-only. These adapters do not claim browser Prompt API object
 * identity, model ownership, GPU execution, account authority, or semantic correctness.</p>
 */
public final class ChromeBuiltInAiAdapters {
    private static final int DEFAULT_CALLS = 64;
    private static final int DEFAULT_OUTPUT_TOKENS = 1024;
    private static final Duration DEFAULT_DEADLINE = Duration.ofMinutes(5);

    private final ChromeMlTaskService tasks;

    public ChromeBuiltInAiAdapters(ChromeMlRunner runner) {
        this(new ChromeMlTaskService(Objects.requireNonNull(runner, "runner")));
    }

    public ChromeBuiltInAiAdapters(ChromeMlTaskService tasks) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
    }

    public Prompt prompt() {
        return new Prompt(tasks);
    }

    public Writer writer() {
        return new Writer(tasks);
    }

    public Rewriter rewriter() {
        return new Rewriter(tasks);
    }

    public Proofreader proofreader() {
        return new Proofreader(tasks);
    }

    public Translator translator() {
        return new Translator(tasks);
    }

    public LanguageDetector languageDetector() {
        return new LanguageDetector(tasks);
    }

    public Summarizer summarizer() {
        return new Summarizer(tasks);
    }

    public record Prompt(ChromeMlTaskService tasks) {
        public Prompt {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result run(String instruction) throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.PROMPT, instruction, "");
        }
    }

    public record Writer(ChromeMlTaskService tasks) {
        public Writer {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result write(String brief) throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.WRITE, brief, "");
        }
    }

    public record Rewriter(ChromeMlTaskService tasks) {
        public Rewriter {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result rewrite(String source, String style) throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.REWRITE, source, style);
        }
    }

    public record Proofreader(ChromeMlTaskService tasks) {
        public Proofreader {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result proofread(String source) throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.PROOFREAD, source, "");
        }
    }

    public record Translator(ChromeMlTaskService tasks) {
        public Translator {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result translate(String source, String targetLanguage)
                throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.TRANSLATE, source, targetLanguage);
        }
    }

    public record LanguageDetector(ChromeMlTaskService tasks) {
        public LanguageDetector {
            Objects.requireNonNull(tasks, "tasks");
        }

        /**
         * Detects only within an explicitly supplied language set so model output cannot invent a
         * new authority-bearing language identifier.
         */
        public ChromeMlTaskService.Result detect(String source, List<String> allowedLanguages)
                throws Exception {
            Objects.requireNonNull(allowedLanguages, "allowedLanguages");
            if (allowedLanguages.isEmpty() || allowedLanguages.size() > 32) {
                throw new IllegalArgumentException("allowedLanguages");
            }
            return execute(
                    tasks,
                    ChromeMlTaskService.Kind.LANGUAGE_DETECT,
                    source,
                    String.join(",", allowedLanguages));
        }
    }

    public record Summarizer(ChromeMlTaskService tasks) {
        public Summarizer {
            Objects.requireNonNull(tasks, "tasks");
        }

        public ChromeMlTaskService.Result summarize(String source) throws Exception {
            return execute(tasks, ChromeMlTaskService.Kind.SUMMARY, source, "");
        }
    }

    private static ChromeMlTaskService.Result execute(
            ChromeMlTaskService tasks,
            ChromeMlTaskService.Kind kind,
            String source,
            String option) throws Exception {
        return tasks.execute(new ChromeMlTaskService.Request(
                kind,
                Objects.requireNonNull(source, "source"),
                Objects.requireNonNullElse(option, ""),
                DEFAULT_CALLS,
                DEFAULT_OUTPUT_TOKENS,
                DEFAULT_DEADLINE));
    }
}
