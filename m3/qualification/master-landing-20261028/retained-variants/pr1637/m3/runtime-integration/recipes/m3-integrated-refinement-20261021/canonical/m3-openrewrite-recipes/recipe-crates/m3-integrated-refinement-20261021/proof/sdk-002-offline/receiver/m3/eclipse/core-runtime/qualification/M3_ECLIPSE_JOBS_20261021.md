<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse monitored Jobs: recovered donor and real dependency qualification

## Source and ownership

This is an additive recovery of the existing M3JDK21 PR [1576](https://github.com/hsoliwal/M3jdk21/pull/1576)
at commit `6b75e71559c09c9212ef051f2d3927202eaa5819`, reviewed against receiving commit
`2779aba3f98f31c324be43f731be7914e1a05935`. Its existing canonical recipe lineage is
[Synexia PR 12575](https://github.com/hsoliwal/com.synexia/pull/12575). The current
recovery uses the shared, source-sealed Synexia Maven/OpenRewrite task; exact
application and refusal receipts belong to that task's receiving evidence.

The recovered public types are `M3EclipseJobs` and `M3EclipseJobScheduler`, including
the former's nested `Task` interface. They are additions to this receiving tree.
Their public/protected descriptors remain identical to the pinned donor. Existing
M3, Eclipse and JDK public types are not renamed or replaced.

The donor's Apache-2.0 notices remain in every recovered Java file. The new regression
and qualification material are first-party Apache-2.0. Eclipse and Java APIs are
called through their existing owners; no Eclipse or OpenJDK implementation bodies
are copied into these adapters. No JNI entry point or native callback dispatch is
introduced.

## Execution contracts

`M3EclipseJobs.submit` uses the caller's `ExecutorService` and a Java `FutureTask`.
The caller exclusively assigns an existing `M3ProgressMonitor` to the operation.
Rejection starts no task lifecycle. Execution owns `beginTask` and `done`; the
borrowed `M3EclipseProgressMonitor` does not restart or complete that owner.
An accepted Future cancellation uses `FutureTask` cancellation and marks its M3
owner canceled. Completed Futures cannot be retroactively canceled.

`submitChild` uses the existing `M3ProgressMonitor.split` owner. Its generation,
progress-credit and parent-cancellation semantics remain those of `M3ChildProgressMonitor`.
The parent must not start another generation while its children remain active.
This Apache module's child contract is distinct from `java.util.concurrent.ProgressMonitor`;
the two existing owner families are not interchangeable.

`M3EclipseJobScheduler.schedule` returns an actual Eclipse `Job` scheduled through
the original JobManager. Its `ISchedulingRule` controls exclusion. The operation
receives both the original Eclipse monitor and an existing borrowed M3 facade.
The implementation does not introduce a competing job queue. A job canceled while
waiting can be removed by JobManager before any M3 lifecycle callback occurs.

Normal completion preserves the original `IStatus`. Interrupted tasks preserve
the thread interrupt and return CANCEL. Java `CancellationException` and Eclipse
`OperationCanceledException` return CANCEL and mark the owner canceled. Other
Exceptions become ERROR with their original cause; JVM Errors propagate after
M3 progress completion. Native Eclipse JobManager retains its own handling of
Errors that escape a Job callback.

The returned Job retains native join/cancel/listener APIs. This helper supplies no
custom `belongsTo` implementation, family membership policy, job priority argument,
or UI-thread dispatch. Applications requiring those policies continue to use their
existing Job/UIJob subclasses. This source addition does not claim workbench,
PDE, m2e, Tycho, or OSGi application integration.

## Retained donor defect and correction

The unchanged donor passed its twelve tests and the five current progress-facade
tests with real Eclipse dependencies. A new independent comparison then ran the
same cancellation signal through a real Eclipse Job and the donor adapter:
the native Job returned CANCEL (severity 8), while the donor returned ERROR
(severity 4). Its synchronous `runNow` had the same defect. Both failed tests and
their exact original source/log/report hashes are retained as counterexamples.

The correction adds `OperationCanceledException` to the existing cancellation
catch. The original seventeen tests and the four new native-oracle tests remain
unchanged between the failed donor trial and the corrected trial. The correction
preserves donor public/protected API descriptors and does not alter the native
JobManager, monitor owners, scheduling-rule routing, or Future implementation.

## Executed preparation evidence

These are candidate preparation gates, before canonical target materialization:

| Source and dependency case | Result | Scope |
| --- | --- | --- |
| Exact donor, Eclipse runtime 3.35.0 and jobs 3.15.700 | 17 tests passed | Twelve original donor tests plus five current progress tests |
| Exact donor plus independent native-oracle regression | 21 tests; two cancellation failures | Frozen counterexample; rule exclusion and waiting-job cancellation passed |
| Corrected donor, original jobs 3.15.700 | 21 tests passed | Same frozen test corpus |
| Corrected donor, current runtime-transitive jobs 3.15.900 | 21 tests passed | No dependency downgrade |
| Donor versus corrected public/protected API | Identical declarations and descriptors | `javap -protected -s`, two public classes and nested Task |

The focused component compiled eight actual production source files and four test
files on Java 21 with `-Xlint:all -Werror`. It compiled current M3 progress sources
directly and used real Maven Eclipse/JUnit artifacts; it contained no replacement
Eclipse or JUnit interfaces. Exact source, POM, report, runtime JAR and log SHA-256
identities are retained in the task evidence. The first donor trial had three test
source files, before the native-oracle regression was added.

The current owning module POM remains unchanged. Its existing runtime 3.35.0
dependency provides jobs 3.15.900 transitively. The older donor's explicit
jobs 3.15.700 POM edit is excluded from recovery.

## Receiving gates

After the shared canonical recipe emits these sources, verify their exact postimage
hashes, then execute the unchanged owning module POM with a source-qualified
`m3-collections` artifact:

```sh
mvn -B -f m3/eclipse/core-runtime/pom.xml verify
```

The focused preparation results do not substitute for that actual-target command,
canonical parser/refusal/fixed-point proof, a full M3JDK image, source-built Eclipse,
or application-level OSGi/PDE/UI qualification. Their completion is recorded by
separate task receipts. No performance improvement, universal scheduler parity,
or advance of the repository's ordered global qualification phases is claimed.
