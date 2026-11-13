<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# ProgressJob: a separate Callable-oriented API candidate

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
