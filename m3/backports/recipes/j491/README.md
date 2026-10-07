# J491 — Virtual-thread monitor unpinning inventory

Status: multi-architecture VM candidate; not materialized.

JEP 491 removes most virtual-thread pinning caused by Java monitors. The integration commit changes
246 files spanning all supported CPU backends, interpreter/C1/C2, continuations, ObjectMonitor,
JavaThread, Java libraries, JNI/JVMTI, SA, JFR and a broad virtual-thread test matrix.

This is not a FILE or MODULE backport. Minimum authority is MULTI_MODULE with per-CPU proof.

## Upstream anchor

- JDK-8338383 / 78b80150e009745b8f28d36c3836f18ad0ca921f — implement JEP 491.

## Post-integration reconciliation set

The following commits touch directly relevant monitor/vthread behavior after the JEP and must be
classified before a backport is frozen:

- JDK-8344247 — move objectWaiter field to VirtualThread.
- JDK-8346120 — correct VirtualThreadPinned event behavior around Object.wait.
- JDK-8345543 — StopThread JVMTI behavior correction.
- JDK-8346792 — JVMTI GetThreadState Object.wait failure correction.
- JDK-8349689 — virtual-thread tests missing /native metadata.

They are not automatically declared mandatory here; each must be diffed against the intended J491
backport and either absorbed, proven irrelevant, or typed as a later independent change.

## M3 admission

Before materialization:

1. inventory all 246 initial paths and every deletion/rename;
2. split source-sealed FILE atoms by architecture and shared runtime owner;
3. deterministic DAG fan-in with explicit architecture/platform edges;
4. preserve Java21 virtual-thread/JNI/JVMTI public contracts;
5. actual fastdebug build on every supported architecture lane;
6. virtual-thread monitor/JVMTI/JFR/SA jtreg and stress;
7. compare pinned behavior for true native/foreign critical sections that must remain pinned;
8. benchmark monitor-heavy virtual-thread workloads and carrier utilization.

The current Linux execution environment lacks required native development packages, so no product
source is materialized and no pinning-performance claim is made.
