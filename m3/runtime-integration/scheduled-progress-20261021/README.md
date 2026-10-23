<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Borrowed progress in scheduled execution

This bounded M3JDK21 change adds four requested default methods to the existing
`java.util.concurrent.ScheduledExecutorService` interface:

- `scheduleMonitored(Runnable,long,TimeUnit,ProgressMonitor)`
- `scheduleMonitored(Callable<V>,long,TimeUnit,ProgressMonitor)`
- `scheduleAtFixedRateMonitored(Runnable,long,long,TimeUnit,ProgressMonitor)`
- `scheduleWithFixedDelayMonitored(Runnable,long,long,TimeUnit,ProgressMonitor)`

These are **additive M3JDK21 APIs**. The four original abstract scheduling methods
retain their exact public declarations and JVM descriptors. The added defaults
compose the existing `ProgressMonitors.run/call` operation at execution time and
delegate to the existing scheduler. They return its actual `ScheduledFuture`.
The scheduler retains delay, fixed-rate/fixed-delay, rejection, task decoration,
worker, interrupt, failure and future-state authority. No scheduler, queue, future
implementation, status type or JNI entry point is added.

## Ownership and failure contract

The supplied monitor is borrowed. Submission and execution do not invoke its
`begin`, `done` or `cancel` methods. Each execution binds the supplied monitor,
checks cancellation before and after the body, and restores the executing thread's
previous scope. Submitting through the legacy methods does not acquire new
automatic context capture behavior. Monitor implementations and callers remain
responsible for concurrent use.

The future's `cancel` method retains the actual scheduler's contract; it does not
cancel or finish the borrowed monitor. Conversely, a cancellation exception
observed while the scoped task runs becomes exceptional task completion under the
actual future contract. It does not implicitly set `Future.isCancelled()`.
Exceptions and successful result objects retain their identity. A `ProgressJob`
may therefore return an application status, including an Eclipse `IStatus`,
without introducing a status conversion policy in `java.base`.

Periodic execution reuses the supplied monitor with a fresh scope for every
invocation. It does not reset or complete the monitor between invocations.
Exceptions, including cooperative cancellation, suppress subsequent executions
according to the original periodic scheduler contract. Callers create and complete
weighted children through the existing `ProgressMonitor.child` owner. The core
SPI's local child cancellation remains distinct from the separately owned Apache
M3/Eclipse progress SPI's parent-propagating cancellation semantics.

## Current-source recovery and qualification

Receiving source is `2779aba3f98f31c324be43f731be7914e1a05935`. The scheduling and
progress owners match the earlier `d6d76b563050fc13c4fa79bc494edf29f1614b37`
preimages. The old unpublished task's raw postimages and receipts were not
recovered. This packet records a fresh bounded extension and fresh qualification;
it does not relabel remembered results as current evidence.

`SOURCE_PIN.json` identifies the original owner, candidate owner, eight unchanged
source dependencies and three exact test sources. The canonical Synexia recipe
must materialize these exact postimages before actual-target acceptance. Recipe
ownership, refusal/fixed-point evidence and publication pins are carried by the
enclosing canonical packet. The target-local verifier is a receiver proof tool.

The isolated authoring candidate passed strict Java 21 compilation and public
descriptor comparison. Sixteen runtime launches exercised the following in mixed
and interpreted modes:

- The original `M3ProgressJob` contract: 81 checks per execution, unchanged.
- The new scheduled contract: 153 checks per execution, with matching baseline
  explicit-composition and candidate-default observations.
- A two-class scheduler implementation compiled once against stock Java 21:
  unchanged original calls on stock/baseline/candidate runtimes, plus reflective
  dispatch of all four added defaults through that unchanged old implementer.

The scheduled contract covers returned-future identity, parameter/overload
delegation, nested and reused-worker scopes, checked exception and Error identity,
cooperative cancellation before/after execution, pending and running future
cancellation with both interrupt policies, weighted children, periodic
non-overlap, periodic suppression, invalid inputs and rejection without monitor
lifecycle effects. A negative compiler witness records that the baseline lacks
the requested four methods. New contract code is compiled against the candidate
API; its baseline mode exercises the existing explicit composition. The frozen
stock client is a separate binary-compatibility witness.

The first authoring run exposed a reflection-oracle mistake: private synthetic
lambda bodies were counted as public interface obligations. The correction limits
that census to public declared methods; it preserves every descriptor and behavior
assertion. The original failing log, source pins and test source remain in recipe
correction evidence. Production candidate bytes did not change for that correction.

## Reproduce against emitted sources

Use the exact baseline sources identified in `SOURCE_PIN.json` and an actual
Java 21 toolchain. The output directory must be new:

```sh
python3 m3/runtime-integration/scheduled-progress-20261021/verify.py \
  --root /absolute/path/M3jdk21 \
  --baseline-root /absolute/path/frozen-baseline \
  --jdk-home /absolute/path/jdk-21 \
  --out /absolute/path/new-scheduled-proof
```

The verifier refuses source/test drift, compiles isolated baseline/candidate
`java.base` source closures, preserves the stock client class hashes and writes
raw logs plus `RESULT.json`, including failures. Omitting `--candidate` selects
the actual emitted target. An authoring trial may explicitly pass a separate
candidate tree; its receipt is labeled `ISOLATED_AUTHORING_CANDIDATE`.

## Acceptance boundary

This gate executes source-compiled patches and the existing jtreg-style test mains
directly. It is not a complete source-built M3JDK21 image or a jtreg harness run.
Full image/jtreg, broad ABI/JCK/platform/concurrency acceptance and CPU/heap/native
memory measurements remain pending. No speedup or complete scheduler-family claim
is made. The global receiving phase remains STRING.

Eclipse JobManager rules, job families, listener lifecycle and SWT UI affinity
remain with their original owners. The separate PR #1576 donor uses real Eclipse
Jobs and another existing progress SPI; it requires its own real-dependency
qualification and is not silently substituted for these core defaults.

## Rights

OpenJDK's original file header and JSR-166 public-domain notice remain intact.
The new target-owned scheduling methods retain the target GPLv2/Classpath terms.
New test source retains its stated GPLv2 terms. This documentation, catalogue and
receiver verification tool are Apache-2.0 first-party work with the recorded
attribution. No Eclipse or challenge-site implementation is copied into
`java.base`. A recipe's license does not relicense its generated output.
