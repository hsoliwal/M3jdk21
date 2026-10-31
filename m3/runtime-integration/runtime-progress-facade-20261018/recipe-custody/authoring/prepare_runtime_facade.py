#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare an additive Runtime facade for the original Synexia Maven recipe.

The candidate is a separate input tree. The canonical recipe owns transformation,
qualification and receiving; this helper never edits a receiving checkout.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil


RUNTIME = "src/java.base/share/classes/java/lang/Runtime.java"
BEFORE = "daacc198e43203c8bfabaf925569da40964f2758b9f1513dfce88b77e8da8c87"
BASELINE = "4b24824c3aa09d802121447f8970d442a855f064"
QUALIFICATION = "m3/runtime-integration/runtime-progress-facade-20261018"
IMPORTS = "import java.util.concurrent.ProgressMonitor;\nimport java.util.concurrent.ProgressMonitors;\n"
COPYRIGHT = " * Copyright (c) 2026 Hitesh Soliwal and contributors.\n"
FACADE = """    /**
     * Returns the borrowed progress monitor selected for the calling thread.
     * A monitor bound by {@link ProgressMonitors#bind(ProgressMonitor)} takes
     * precedence over the process fallback. Without such a binding, the
     * current fallback provider is evaluated on each call; a {@code null}
     * provider result is represented by {@link ProgressMonitor#NONE}.
     *
     * <p>This method delegates to {@link ProgressMonitors#current()}.
     * Ownership remains with the caller that supplied or bound the monitor.
     * A task reports work to a borrowed monitor and leaves its completion to
     * that owner. Thread bindings are local to the binding thread; monitored
     * executor methods provide explicit propagation to asynchronous tasks.
     *
     * <p>The fallback provider may be invoked concurrently, and its unchecked
     * failures propagate to the caller. The selected monitor determines
     * whether concurrent updates are supported.
     *
     * @apiNote This is an opt-in M3JDK progress facade over the existing
     * {@code java.util.concurrent} progress owners.
     * @return the current lexical or process fallback monitor, never {@code null}
     * @see java.util.concurrent.Executor#executeWithProgress(Runnable)
     * @since 21
     */
    public ProgressMonitor progressMonitor() {
        return ProgressMonitors.current();
    }

    /**
     * Allocates a child progress budget from the monitor selected for this thread.
     * The parent is resolved once, then passed to
     * {@link ProgressMonitors#child(ProgressMonitor, String, long, long)}.
     * Subsequent scope or fallback-provider changes do not rebind that child.
     *
     * <p>The caller completes the returned child independently. Completion
     * credits its remaining parent budget without completing the borrowed
     * parent. Parent cancellation is visible to the child; cancelling the
     * child leaves the parent's cancellation state unchanged. When the parent
     * is {@link ProgressMonitor#NONE}, this method returns that monitor.
     *
     * <p>Child accounting uses the existing atomic implementation. The parent
     * must support concurrent callbacks when the child is shared across
     * threads. A nonempty task description is delivered through the parent's
     * {@link ProgressMonitor#message(String) message} method during creation.
     * Unchecked provider or callback failures propagate unchanged. The parent
     * is selected before the child factory validates the work range.
     *
     * @apiNote This is an opt-in M3JDK progress facade; it uses the existing
     * child factory and current-monitor selection rules.
     * @param task the child description, or {@code null} for no description
     * @param parentTicks the nonnegative budget credited to the parent
     * @param totalWork the positive child work estimate, or
     *                  {@link ProgressMonitor#UNKNOWN_WORK} for completion-only credit
     * @return the child monitor, or {@link ProgressMonitor#NONE} for a no-op parent
     * @throws IllegalArgumentException if {@code parentTicks} is negative or
     *         {@code totalWork} is neither positive nor {@code UNKNOWN_WORK}
     * @since 21
     */
    public ProgressMonitor subProgressMonitor(String task, long parentTicks, long totalWork) {
        return ProgressMonitors.child(ProgressMonitors.current(), task, parentTicks, totalWork);
    }

"""


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def once(text, before, after):
    if text.count(before) != 1:
        raise ValueError("reviewed Runtime insertion anchor drift: " + before[:100])
    return text.replace(before, after, 1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--templates", type=Path, required=True)
    parser.add_argument("--source-receipt", type=Path, required=True)
    parser.add_argument("--decisions", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        raise ValueError("a fresh candidate directory is required")
    source = args.baseline / RUNTIME
    if sha(source) != BEFORE:
        raise ValueError("Runtime source preimage drift")
    closure = json.loads(args.source_receipt.read_text())
    for row in closure["files"]:
        if sha(args.baseline / row["path"]) != row["sha256"]:
            raise ValueError("existing progress dependency drift: " + row["path"])
    original = source.read_text()
    candidate = once(original,
            " * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.\n",
            " * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.\n" + COPYRIGHT)
    candidate = once(candidate, "import java.util.StringTokenizer;\n",
            "import java.util.StringTokenizer;\n" + IMPORTS)
    anchor = "    /** Don't let anyone else instantiate this class */\n    private Runtime() {}\n"
    candidate = once(candidate, anchor, FACADE + anchor)
    # Every old source token and comment remains in place after removing only
    # the three explicit additive insertions. No legacy method is rewritten.
    if candidate.replace(COPYRIGHT, "", 1).replace(IMPORTS, "", 1).replace(FACADE, "", 1) != original:
        raise ValueError("legacy Runtime source changed outside the admitted additions")
    target = output / RUNTIME
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(candidate)
    for name in ("M3RuntimeProgressFacade.java", "M3RuntimeLegacyClient.java"):
        target = output / "test/jdk/java/lang/Runtime" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(args.templates / name, target)
    qualification = output / QUALIFICATION
    qualification.mkdir(parents=True)
    shutil.copyfile(args.decisions, qualification / "CATALOGUE.tsv")
    mapping = {
        "schema": "M3_RUNTIME_PROGRESS_FACADE_PENDING_MAP_V1",
        "target_owner": "java.lang.Runtime", "target_source": RUNTIME,
        "baseline_ref": BASELINE, "before_sha256": BEFORE,
        "receiving_integration_baseline_ref": "86528a90079b89a50b51ef13da08e7e70d468cb4",
        "after_sha256": sha(output / RUNTIME),
        "status": "PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE",
        "active_receiving_phase": "STRING", "phase_promotion": False,
        "policy_disposition": "explicit deferred receiving map for this additive draft; the global naming/migration map and its existing phase remain unchanged",
        "existing_runtime_owners": closure["files"],
        "new_public_methods": [
            {"signature": "public ProgressMonitor Runtime.progressMonitor()", "delegate": "ProgressMonitors.current()", "ownership": "borrowed lexical or process fallback monitor"},
            {"signature": "public ProgressMonitor Runtime.subProgressMonitor(String,long,long)", "delegate": "ProgressMonitors.child(ProgressMonitors.current(),task,parentTicks,totalWork)", "ownership": "existing child factory; parent selected exactly once and borrowed"},
        ],
        "compatibility": {
            "public_api": "explicit superset: two new instance methods, every old declaration retained",
            "Runtime_constructor": "existing private constructor retained; source subclassing remains refused",
            "fields_added": 0, "native_changes": 0, "provider_or_scheduler_owners_added": 0,
            "existing_method_source": "byte-for-byte retained after removing the three declared additive source insertions",
            "reflection": "the new public methods are intentionally discoverable; an identical reflective method inventory is not claimed",
            "universal_binary_compatibility": "no universal claim for arbitrary generated bytecode, Unsafe-created subtypes or unsupported VM assumptions",
        },
        "license": {"mode": "TARGET_OWNED_PORT", "target": "GPL-2.0-only WITH Classpath-exception-2.0", "recipe": "Apache-2.0", "donor_copy": "no external implementation body copied; delegate to existing target-owned GPL APIs"},
        "recipe": {"canonical_repository": "hsoliwal/com.synexia", "canonical_engine_revision": "8f8200375d8d403b58e855ee5db02f2e8fac33a8", "crate": "synexia-openrewrite-recipes/recipe-crates/m3-runtime-progress-facade-20261018", "dataset": "m3jdk21-runtime-facade", "parser": "com.synexia.sourcebuilt.openrewrite:rewrite-java-21:8.17.1-m3-javadoc-1", "parser_scope": "separately qualified upstream Javadoc repair; original engines and tests retained"},
        "required_gates": ["original Maven SDK five tests, eight sealing tests and nine receiver cases", "exact original JDK compiler lint/doclint gate", "old API/field inventory and frozen stock-Java21 Runtime client on baseline and candidate patches", "lexical/global/provider/child/executor/concurrency/error witnesses", "matched full M3JDK image and applicable jtreg/VM/native/platform evidence before runtime promotion"],
        "cost": {"new_facade_state": "zero fields", "current_query": "one existing owner call plus provider-defined cost", "child_creation": "one current selection and existing child factory; existing allocation and callback costs", "performance_claim": "none"},
    }
    (qualification / "RECEIVING-MAP.json").write_text(json.dumps(mapping, indent=2) + "\n")
    (qualification / "README.md").write_text("""<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Runtime progress facade candidate

The existing `Runtime.getRuntime()` singleton exposes two opt-in convenience
methods backed by the existing `java.util.concurrent` progress owners:

```java
ProgressMonitor borrowed = Runtime.getRuntime().progressMonitor();
ProgressMonitor child = Runtime.getRuntime().subProgressMonitor("index", 12L, 3L);
```

`progressMonitor()` selects the calling thread's lexical binding, otherwise it
evaluates the process fallback provider. Ownership stays with whoever supplied
or bound that monitor. `subProgressMonitor(...)` selects that parent once and
uses `ProgressMonitors.child`; the caller completes the child independently.
Scope or provider changes after creation do not rebind an existing child.

Existing `ProgressMonitors.bind`, `Executor.executeWithProgress`, and
`ExecutorService.submitMonitored` supply scope and executor integration.
Ordinary executor submission keeps its existing behavior. A provider may be
invoked concurrently; a shared child's parent must support concurrent callbacks.
Provider and callback failures keep the existing exception behavior. Child work
range validation follows current-monitor resolution, including for a no-op parent.

The facade adds exactly two public instance methods and zero fields. Every old
Runtime declaration and method body remains intact, including its private
constructor and native methods. The new methods are visible to reflection. No
claim covers arbitrary generated bytecode or unsupported VM subtype assumptions.

`CATALOGUE.tsv` records the selected owners and rejected duplicate-global design.
`RECEIVING-MAP.json` is the explicit deferred naming/receiving disposition for
this draft. String remains the active receiving stage. A patched Java 21 proof,
recipe qualification, or a merged source PR does not promote this capability to
full M3JDK runtime acceptance. A matched image and applicable target tests remain
separate gates.

The original canonical Maven recipe dataset is `m3jdk21-runtime-facade` in
`m3-collections-contract-refinement-20261017`. It retains the original four
engines and five/eight/nine SDK/sealing/receiving gates. The two test sources in
`test/jdk/java/lang/Runtime` provide the progress and legacy-client witnesses.
The external qualification driver first compiles the legacy client with an
unmodified Java 21 compiler and freezes those class bytes before candidate
execution. Ordinary jtreg compilation alone does not establish that frozen
binary comparison.
""")
    print(json.dumps({"kind": "RECIPE_INPUT_ONLY", "runtime_before_sha256": BEFORE,
        "runtime_after_sha256": sha(output / RUNTIME), "new_methods": 2,
        "new_fields": 0, "phase_promotion": False,
        "targets": sorted(p.relative_to(output).as_posix() for p in output.rglob("*") if p.is_file())}))


if __name__ == "__main__":
    main()
