# Exact String successor and workflow SDK receiving copies

Source PR11973 owns both reusable Maven recipes. Main module is the exact54-file tree `edb997124a975bc4a0a4ff8f5b67b74526e5a33c` at source commit `4349d8d20622176ed08cfae2fbbf98404916a80d`. Workflow module is the exact13-file tree `e5fa92b5734c593091f55636d4637472f4d62ed2` at source commit `3b61df6a000bf162b727c7e725ed35b52ce78cc3`. Canonical application metadata lives outside the modules so a workflow fixture never depends on its own tree hash.

The existing receiving Java21/Maven job runs both modules and retains exact XML, emitted source/workflow outputs and logs. Expected: six original/serial String SDK tests and two workflow SDK tests, 162 and40input-closure refusals, complete26successor and12custody Python tests, and an isolated nonzero full-build hold probe. Counts are requirements pending execution.

Initial job117311661103 passed162closure refusals, then strict compilation stopped on missing org.intellij.lang.annotations.Language. Both Maven fixtures now explicitly use JetBrains annotations24.1.0 provided, as documented upstream. Warnings remain errors; providers and test bodies remain unchanged. The inherited UTF8 proof passed5tests and26refusals in that same job.

The checker and runtime workflow repair remain unapplied. After exact qualifications pass, apply SDK-emitted V3 thenV4 as separate serial commits, pin their actual immediate preimages, and apply the emitted workflow hold before full-JDK execution can become reachable. Preserve V1/V2/V3, historical chain prefix, UTF8 HOLD and public contracts. Full matched-JDK output is NOT_ADMITTED; phase/runtime/retention/performance gates remain open.

Generic Apache-2.0 glue is directly required for this migration; no unrelated private GPU resources or new executor is introduced. Public Java fixtures retain GPL/ClassPath-exception notices. See canonical-source.json, workflow-canonical-source.json, DISCLOSURE_PREFLIGHT.json and THIRD_PARTY_NOTICES.md. Both PRs remain draft.
