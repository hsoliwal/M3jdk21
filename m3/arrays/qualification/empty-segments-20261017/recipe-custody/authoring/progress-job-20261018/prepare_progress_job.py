#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare a separate additive ProgressJob input set for the canonical Maven recipe."""

import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess


DATASET = "m3jdk21-progress-job"
TASK = "m3-collections-contract-refinement-20261017"
TARGET_PIN = "86528a90079b89a50b51ef13da08e7e70d468cb4"
CANONICAL_PIN = "8f8200375d8d403b58e855ee5db02f2e8fac33a8"
PREFIX = "m3/runtime-integration/progress-job-20261018"
OWNER_PATH = "src/java.base/share/classes/java/util/concurrent/ProgressJob.java"
DEPENDENCIES = (
    "src/java.base/share/classes/java/lang/Runnable.java",
    "src/java.base/share/classes/java/util/concurrent/Callable.java",
    "src/java.base/share/classes/java/util/concurrent/MonitoredCallable.java",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitor.java",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java",
    "src/java.base/share/classes/java/util/concurrent/Executor.java",
    "src/java.base/share/classes/java/util/concurrent/ExecutorService.java",
)
TEMPLATES = {
    "ProgressJob.java.txt": OWNER_PATH,
    "M3ProgressJob.java.txt": "test/jdk/java/util/concurrent/ProgressJob/M3ProgressJob.java",
    "OldCallableClient.java.txt": PREFIX + "/src/test/java/OldCallableClient.java",
    "EclipseStatusIdentity.java.txt": PREFIX + "/src/test/java/EclipseStatusIdentity.java",
    "ProgressJobRunnableConflict.java.txt": PREFIX + "/src/test/resources/ProgressJobRunnableConflict.java.txt",
}
HEADER = ("<!--\nSPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project\n"
          "SPDX-License-Identifier: Apache-2.0\n-->\n")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_bytes(root, relative):
    return subprocess.check_output(["git", "-C", str(root), "show", TARGET_PIN + ":" + relative])


def write(root, relative, data):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def encoded(value):
    return (json.dumps(value, indent=2) + "\n").encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline-root", "recipe-owner", "donor-root", "canonical-source", "work-dir"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    owner = args.recipe_owner.resolve()
    crate = owner / "recipe-crates" / TASK
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    baseline = work / "baseline"
    candidates = work / "candidate-input"
    baseline.mkdir()
    candidates.mkdir()
    resolved = subprocess.check_output(["git", "-C", str(args.baseline_root), "rev-parse", TARGET_PIN], text=True).strip()
    if resolved != TARGET_PIN:
        raise ValueError("target pin is unavailable")
    dependencies = []
    for path in DEPENDENCIES:
        data = git_bytes(args.baseline_root, path)
        write(baseline, path, data)
        dependencies.append({"path": path, "sha256": sha(data), "bytes": len(data),
                             "git_blob": hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest(),
                             "repository": "hsoliwal/M3jdk21", "revision": TARGET_PIN,
                             "disposition": "EXISTING_OWNER_REUSED_UNCHANGED"})
    templates = Path(__file__).resolve().parent / "templates"
    for template, path in TEMPLATES.items():
        write(candidates, path, (templates / template).read_bytes())
    verifier = Path(__file__).resolve().parent / "verify_progress_job.py"
    write(candidates, PREFIX + "/verify_progress_job.py", verifier.read_bytes())
    donor_receipt = json.loads((args.donor_root / "receipt.json").read_text())
    for entry in donor_receipt["files"]:
        if sha((args.donor_root / entry["file"]).read_bytes()) != entry["sha256"]:
            raise ValueError("Eclipse input drift: " + entry["file"])
        for source in entry.get("reviewed_sources", []):
            if sha((args.donor_root / source["file"]).read_bytes()) != source["sha256"]:
                raise ValueError("Eclipse reviewed source drift")
    source_catalogue = args.canonical_source / "progress-refinement/canonical/synexia-openrewrite-recipes/recipe-crates" / TASK / "catalogue/context-owner-inventory.json"
    selected = []
    for entry in json.loads(source_catalogue.read_text())["files"]:
        if entry["path"] in ("synexia-job/src/main/java/com/synexia/job/IJobResult.java",
                             "synexia-job/src/main/java/com/synexia/job/AbstractJob.java",
                             "synexia-cop/src/main/java/com/synexia/context/core/IAdaptable.java"):
            if sha((args.canonical_source / "source" / entry["path"]).read_bytes()) != entry["sha256"]:
                raise ValueError("Synexia donor source drift")
            selected.append(entry)
    if len(selected) != 3:
        raise ValueError("expected three inspected Synexia source owners")
    review = {
        "schema": "M3_PROGRESS_JOB_OWNER_REVIEW_V1", "target_pin": TARGET_PIN,
        "canonical_pin": CANONICAL_PIN, "product_owner": OWNER_PATH,
        "product_source_sha256": sha((candidates / OWNER_PATH).read_bytes()),
        "current_owner_dependencies": dependencies, "synexia_sources": selected,
        "eclipse_release_inputs": donor_receipt,
        "selection": {
            "implementation_owner": "Existing java.util.concurrent.MonitoredCallable and ProgressMonitors; no duplicate cancellation or scope implementation.",
            "synexia": "IJobResult is a sealed Success/Failure/Cancelled value with metrics/duration/adaptation, not Eclipse IStatus. AbstractJob adds listeners/context/state and wraps non-IOPException failures; those semantics are not imported.",
            "eclipse": "Job.run(IProgressMonitor) returns non-null IStatus and supports scheduler state/ASYNC_FINISH. ProgressJob borrows the protected result hook pattern only; the generic result and existing Callable cancellation/exception contracts remain distinct.",
            "future_adapter": "new FutureTask<S>(() -> job.call(monitor)); executor.execute(future); bind inside the callable so pre-cancellation is recorded by FutureTask.",
            "alternative": "Duplicating the template or adding status conversion would add a competing owner and change checked exception/cancellation identity; rejected.",
            "runnable_conflict": "A subclass of this new Callable-oriented base cannot also implement Runnable because Runnable.run(ProgressMonitor) returns void. An explicit negative compiler witness is retained.",
            "thread_safety": "Base has no fields. Lexical bindings use the existing owner. Subclass and monitor concurrency contracts remain their own.",
            "complexity": "Each new concrete method performs one direct dispatch; O(1) wrapper work and no explicit task/monitor allocation. No measured performance improvement claimed.",
            "challenge_catalogues": {"LeetCode": "NOT_APPLICABLE: template/result identity is not an array/search algorithm.",
                                     "HackerRank": "NOT_APPLICABLE: no challenge algorithm substitutes for these public contracts.",
                                     "GeeksforGeeks": "Reference pattern discovery is secondary; selection uses inspected current owners and Eclipse primary source, not unvalidated snippets."},
        },
        "license": {"new_java_base_and_jtreg": "First-party target-owned contribution under GPL-2.0-only WITH Classpath-exception-2.0; no copied Eclipse or Apache donor method body.",
                    "recipe_and_m3_proof": "Apache-2.0, exact Synexia first-party notice retained.",
                    "eclipse": "EPL-2.0 source review and unmodified external test dependency only; original notices retained in source inputs, no Eclipse source incorporated into java.base.",
                    "synexia": "Apache-2.0 source review; source identities retained, no Synexia runtime dependency."},
        "scope": "SEPARATE_ADDITIVE_API_CANDIDATE", "target_phase": "STRING", "phase_advanced": False,
        "admission": "PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE",
        "Object_universal_no_break": "HOLD_UNCHANGED",
    }
    write(candidates, PREFIX + "/DONOR-REVIEW.json", encoded(review))
    mapping = {
        "schema": "M3_PROGRESS_JOB_ADDITIVE_RECEIVING_MAP_V1", "target_base": TARGET_PIN,
        "source_category": "task execution / protected result hook / existing monitored Callable template",
        "target": "java.util.concurrent.ProgressJob<S>", "target_module": "java.base",
        "status": "PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE", "target_phase": "STRING", "phase_advanced": False,
        "public_api_change": "New opt-in abstract class; not an unchanged-interface internal refinement.",
        "interfaces_preserved": ["Runnable.run():void", "Callable.call():V throws Exception", "MonitoredCallable.callMonitored(ProgressMonitor):V throws Exception"],
        "canonical_recipe": "com.synexia.m3jdk.CollectionsContractRefinement20261017", "dataset": DATASET,
        "recipe_owner": "hsoliwal/com.synexia:synexia-openrewrite-recipes",
        "source_sha256": review["product_source_sha256"],
        "status_result": "S is returned unchanged; no IStatus/IJobResult dependency or conversion in java.base.",
        "executor": "Existing FutureTask and ExecutorService.submitMonitored compose with this Callable; no new scheduler or capture rule.",
        "limitations": ["ProgressJob subclasses cannot also implement Runnable because of the progress overload's return type.",
                        "MonitoredCallable cancellation exceptions are preserved; a canceled monitor does not yield an Eclipse CANCEL status.",
                        "Existing universal Object.toAdapter no-break failure remains on HOLD.",
                        "No claim of full Eclipse Job scheduler/listener/resource or Synexia context parity."],
    }
    write(candidates, PREFIX + "/RECEIVING-MAP.json", encoded(mapping))
    catalogue = [
        ["id", "problem_category", "existing_owner", "selection", "compatibility", "complexity", "gate", "disposition"],
        ["progress-job-template", "execution/protected-result-hook", "MonitoredCallable.call", "Final call delegates to current owned template", "Existing pre/post cancellation and checked exceptions retained", "O(1) dispatch; no benchmark claim", "Strict compiler; identity and scope tests; recipe refusal", "ADDITIVE_API_PENDING"],
        ["progress-job-status", "result/identity-and-exception", "Eclipse IStatus and Synexia IJobResult distinct owners", "Generic S passed unchanged; no translation", "Null remains a valid Callable result; thrown exceptions remain exceptions", "O(1) reference return", "Real Eclipse Status/MultiStatus identity and exception-cause witness", "ADDITIVE_API_PENDING"],
        ["progress-job-future", "execution/future-completion-under-cancellation", "FutureTask", "Bind inside FutureTask Callable", "Future.get wraps body exceptions in ExecutionException; isCancelled remains false unless Future.cancel wins", "Existing FutureTask cost; no new scheduler", "Pre-cancel done-future and outer-wrapper counterexample", "EXISTING_ADAPTER_REUSED"],
        ["progress-job-scope", "context/lexical-borrowed-monitor", "ProgressMonitors and ExecutorService.submitMonitored", "Reuse exact lexical scope owner", "Borrowed done is caller-owned; ordinary submit does not capture caller scope", "Existing scope cost; no new state owner", "Nested restoration; checked failure; reused worker; two concurrent calls", "EXISTING_OWNER_UNCHANGED"],
        ["progress-job-runnable", "compatibility/new-base-return-collision", "Runnable.run(ProgressMonitor):void", "Keep ProgressJob Callable-oriented", "Do not implement Runnable on this new base; ordinary legacy dual clients remain separate", "Not an algorithm", "Negative new-subclass compiler witness plus frozen stock-client binaries", "EXPLICIT_LIMITATION"],
        ["progress-job-donors", "discovery/template-and-framework-results", "Pinned Synexia and Eclipse source", "Reuse current JDK template; use external sources for contract comparison", "No Apache/EPL source-body copy into GPL java.base", "No performance ranking without measurement", "DONOR-REVIEW.json exact sources and artifact hashes", "SOURCE_REVIEW_AND_REAL_DEPENDENCY_TEST"],
    ]
    write(candidates, PREFIX + "/CATALOGUE.tsv", ("\n".join("\t".join(row) for row in catalogue) + "\n").encode())
    readme = HEADER + """# ProgressJob: a separate Callable-oriented API candidate

`java.util.concurrent.ProgressJob<S>` gives JDK-owned subclasses a protected
`run(ProgressMonitor)` hook that returns the caller's result type. Its two final
entry points delegate directly to the existing `MonitoredCallable` template and
the protected body. It introduces no task state, scheduler, status conversion,
native code, or dependency from `java.base` to a framework.

This is a new public API candidate. `RECEIVING-MAP.json` records
`PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE`; the active STRING phase and the
existing universal `Object.toAdapter` compatibility HOLD remain unchanged. A
passing bounded test does not advance either gate or establish full Eclipse Job
or Synexia context parity. This packet is separate from the frozen runtime repairs.

## Result and cancellation contracts

The caller can choose `S` as the real Eclipse `IStatus`, the distinct Synexia
`IJobResult`, or another result type. Successful values, including null, are
returned unchanged. The existing template propagates a body exception unchanged
and throws `CancellationException` if its monitor checks observe cancellation.
It can therefore reject a returned `Status.CANCEL_STATUS` when the monitor was
canceled. Eclipse Job's scheduler, `ASYNC_FINISH`, non-null IStatus contract,
listeners and result conversion are not supplied by this class.

`call()` uses the executing thread's current monitor. Inherited `call(monitor)`
binds an explicit borrowed monitor and restores the prior scope. Direct
`callMonitored(monitor)` is the body entry point: it passes its argument unchanged
and does not bind a scope or check cancellation. Neither path completes a
borrowed monitor. The class has no fields; subclasses and monitors still own the
thread-safety contract of their state.

## Using the existing executor owners

For an ordinary Executor, bind inside a FutureTask's Callable:

```java
FutureTask<IStatus> future = new FutureTask<>(() -> job.call(monitor));
executor.execute(future);
IStatus result = future.get();
```

The actual Eclipse type belongs to the application class path. A pre-cancellation
exception is recorded as exceptional completion by FutureTask; `get()` throws an
`ExecutionException` whose cause is that exception. This is not `Future.cancel`:
`isDone()` is true and `isCancelled()` is false. A checked body exception is the
unchanged cause of `ExecutionException`. The existing
`executorService.submitMonitored(job, monitor)` also binds inside its submitted
Callable. Ordinary `submit(job)` continues to use the worker's current monitor
and does not capture the submitting thread's scope.

An outer `executor.execute(future, monitor)` can reject a canceled monitor before
`FutureTask.run()` executes and leave that future pending. The regression witness
retains this counterexample and uses the inner binding above for the supported
adapter. No new scheduler is needed.

This new base is Callable-oriented. Its value-returning `run(ProgressMonitor)`
conflicts with Runnable's already published void progress overload, so a subclass
cannot also implement Runnable. The negative source witness records this
limitation. Separate old classes that already implement Runnable and Callable
are compiled with an unmodified stock JDK and exercised unchanged as finite
compatibility witnesses; they are not evidence of universal source compatibility.

## Reproduction and evidence boundaries

The reusable Synexia Maven/OpenRewrite host qualifies the sealed dataset
`m3jdk21-progress-job` with its original five SDK, eight sealer and nine receiver
cases. Its receiver emits the source, tests, catalogue and mapping. The host and
recipe engines are unchanged. Target emission must use that qualified receiver;
never patch generated output to repair a failed transformation.

After receiving, run `verify_progress_job.py --baseline-root <repair-source-tree>
--candidate <received-tree> --stock-jdk <stock-java21> --m3-jdk <built-m3-java21>
--eclipse-dir <pinned-eclipse-inputs> --output <new-proof-directory>`. The exact
existing dependency hashes are in `DONOR-REVIEW.json`. The verifier performs the
original JDK protected/package doclint and warning-as-error compile, strict
consumer compiles, frozen stock-client binary runs, core and real Eclipse status
witnesses on a stock patched JDK and a separately built M3 JDK patched only with
the new class, and an expected negative Runnable-subclass compile. It records
commands, source/class identities and outputs. Patching an existing image for
these tests does not qualify a full new image containing this additive API.

The jtreg core test is
`test/jdk/java/util/concurrent/ProgressJob/M3ProgressJob.java`. The external Eclipse
fixture remains under this proof directory and does not make jtreg or java.base
depend on Eclipse. Real Synexia IJobResult source contracts were reviewed;
executing its full dependency closure is a separate, unclaimed gate.

`DONOR-REVIEW.json` identifies the exact current JDK, Synexia and Eclipse owners,
licenses, selection rationale and algorithm-discovery applicability. The new
java.base class and jtreg test are first-party target-licensed GPL+Classpath
contributions; recipe/proof tooling is Apache-2.0. Eclipse is an unmodified
external test dependency and source reference, with no Eclipse implementation
copied into java.base. No JNI operation changes in this packet.
"""
    write(candidates, PREFIX + "/README.md", readme.encode())
    for path in sorted(p.relative_to(candidates).as_posix() for p in candidates.rglob("*") if p.is_file()):
        existing = subprocess.run(["git", "-C", str(args.baseline_root), "cat-file", "-e", TARGET_PIN + ":" + path],
                                  capture_output=True, check=False)
        if existing.returncode == 0:
            raise ValueError("additive target unexpectedly exists in pinned baseline: " + path)
    spec = importlib.util.spec_from_file_location("canonical_prepare", crate / "prepare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    metadata = module.seal(crate, DATASET, "hsoliwal/M3jdk21", baseline, [candidates], TARGET_PIN, owner)
    receipt = {"schema": "M3_PROGRESS_JOB_PREPARATION_V1", "dataset": metadata,
               "baseline_source_root": str(baseline), "candidate_input_root": str(candidates),
               "product_sources_written": False, "target_pin": TARGET_PIN,
               "existing_source_dependencies": dependencies,
               "authored_files": [{"path": p.relative_to(candidates).as_posix(), "sha256": sha(p.read_bytes()), "bytes": p.stat().st_size}
                                  for p in sorted(candidates.rglob("*")) if p.is_file()]}
    (work / "preparation-receipt.json").write_bytes(encoded(receipt))
    print(json.dumps(metadata, sort_keys=True))


if __name__ == "__main__":
    main()
