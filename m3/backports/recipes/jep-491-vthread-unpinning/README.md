# JEP 491 — Synchronize Virtual Threads without Pinning

Status: high-risk dependency-closure packet. **No product source is materialized by this packet.**

## Upstream authority

- JEP: 491
- JEP issue: JDK-8337395
- implementation issue: JDK-8338383
- OpenJDK review/integration PR: 21565
- reviewed PR head: `0fe604654a6976ec28a46b2c26cff50b091b6490`
- released postimage donor: `jdk-24+36`
- exact changed-path denominator: 246

The PR was reviewed across HotSpot, core-libs, serviceability and NIO and integrates monitor ownership,
continuation freeze/thaw, VM preemption on monitor enter/Object.wait, JVMTI/JFR/SA updates, CPU
port changes, libraries and tests.

## Java 21 compatibility split

Java 21 already contains final virtual threads, LockStack, ThreadIdentifier and continuation
freeze/thaw. It also contains lightweight locking as `-XX:LockingMode=2`, but its default is
`LM_LEGACY`.

JEP 491's unpinning mechanism is effective for lightweight/monitor locking, not legacy stack
locking. Therefore M3JDK21 does **not** silently change the Java 21 default.

The compatible lane is initially:

```text
default Java21:
  LockingMode=LM_LEGACY
  -> preserved

explicit candidate:
  -XX:LockingMode=2
  + adapted JEP491 monitor/continuation mechanics
  -> synchronized virtual threads may unmount
```

A future default change requires a separate explicit compatibility decision.

## Owner-layout reconciliation

Released JDK24 has separate
`src/hotspot/share/runtime/lightweightSynchronizer.{cpp,hpp}` owners. Java21 does not; its
lightweight locking implementation is still inside `synchronizer.cpp`.

This is a mechanical ownership/layout mismatch, not evidence of semantic incompatibility. The packet
must map later owners back into the Java21 owner before recipe generation. Blind 246-file copy is
forbidden.

## Preserved Java21 behavior

The packet explicitly preserves:

- Java21 default `LM_LEGACY`;
- `jdk.tracePinnedThreads` diagnostic compatibility;
- existing Java21 java.io / ReferenceQueue virtual-thread lock-avoidance implementations unless a
  dependency proves an upstream reversion is required;
- public Java API and source/class-file semantics.

## Required dependency closure

Before source recipe generation:

1. reconcile `lightweightSynchronizer.*` into Java21 synchronization owners;
2. adapt ObjectMonitor owner identity from carrier JavaThread semantics to stable lock/thread IDs;
3. extend ThreadIdentifier only as required by the feature;
4. prove LockStack <-> stack-chunk transfer under all affected GC barriers;
5. reconcile continuation VM preemption/native-wrapper handling;
6. reconcile JVMTI mount/unmount and monitor ownership;
7. reconcile JFR pinned-event production and preserve the Java21 tracing property;
8. reconcile SA ObjectMonitor layout;
9. review x86_64, aarch64, riscv64 and ppc64 continuation paths and the non-continuation consistency
   edits for arm/s390/zero;
10. classify selector preemption as a supporting atom, not hidden core behavior.

Only then may exact JDK21 -> released-JDK24 postimage recipes be generated.

## Proof gate

A candidate is not promotable until:

- all 246 upstream paths are accounted for as adapted atom, equivalent path or typed exclusion;
- no unreviewed prerequisite remains;
- server HotSpot builds;
- affected port builds/tests are represented according to supported CI;
- virtual-thread monitor enter/wait/notify, JNI, JVMTI, JFR, SA and continuation regressions pass;
- no-flag runtime keeps the Java21 locking default;
- `-XX:LockingMode=2` demonstrates the intended unpinning behavior;
- complete recipe DAG second pass is unchanged.

This packet grants no mutation or promotion authority.
