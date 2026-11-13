# M3JDK21 monitored template-method receiver (2026-10-17)

**Status: DRAFT; not M3JDK product-qualified.** No default branch has been modified by this branch. Reusable source/recipe authority remains [Synexia #12077](https://github.com/hsoliwal/com.synexia/pull/12077), exact recipe commit `6d110986e731e8cd58d570a36ac506a858e0e9a6`, based on `hsoliwal/com.synexia@892e3f6c51d78bc0948e8a8cf736055b4c70eafa`.

Product base is `hsoliwal/M3jdk21@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`, with five exact source-tree preimages/postimages sealed in `m3/compatibility/m3-default-template-source-seal.tsv`. New target source blobs equal donor recipe template Git blob identities. The named canonical Maven/OpenRewrite recipe `com.synexia.m3.JdkDefaultTemplateContracts20261017` reuses the original `M3HashPinnedJavaSnapshotRecipe` and must be actually run and verified before product promotion; this candidate branch does **not** claim an SDK replay occurred.

## Concrete changes
- Fix merged #1082 inheritance conflict: `Callable.progressMonitor()` default removed; `Runnable.progressMonitor()` retained, `Callable.call(ProgressMonitor)` retained. This correction may affect previously built M3-only consumers: run full public/protected/API binary and source-compat tests before release.
- Correct `ProgressMonitors.child(parent,name,0,total)`: preserve parent cancellation, with zero credited ticks; eliminate a retained String title when only used at creation.
- Add two opt-in functional template interfaces. `MonitoredRunnable.run()` defaults to cancellation-check + `runMonitored(progress)`; `MonitoredCallable.call()` defaults to cancellation-check + `callMonitored(progress)`. Their abstract hook is one SAM; arbitrary `Runnable` and `Callable` contracts are untouched.
- Add jtreg `M3MonitoredTemplateTest` exercising a class implementing both interfaces, lambda execution, borrowed scope, a real fixed executor, cancellation and zero-weight child.
- `m3/compatibility/m3-default-template-source-seal.tsv` is a receiver/naming disposition for Synexia COP and JDK functional interfaces; the central `m3/docs/name-mapping.json` owner requires an admitted update before release.

## Why not modify every JDK interface
The canonical `DEFAULT_TEMPLATE_AUDIT.tsv` categorizes 22 JDK/Synexia locations. `Function.apply(value,ProgressMonitor)` can hide an existing binary functional overload and `Consumer.accept(value,ProgressMonitor)` may have analogous risks. These were added in merged #1082 but are **HOLD** for a future repair decision; the current PR does not silently remove or deprecate them. `AbstractExecutorService.newTaskFor`, `ThreadPoolExecutor.beforeExecute/afterExecute`, Java IO bulk methods and concrete collection internals remain canonical owners. M3 collections' index/offset/mask/sort/filtered view PRs are independent.

## Evidence
- Executed local stock JDK21 compilation of progress core plus two new interfaces using `--patch-module java.base`, and executable test of task scopes, lambda hooks, cancellation and explicit thread-pool handoff: PASS.
- Negative compiler test: unrelated Runnable+Callable-style default `progressMonitor()` methods reject a dual implementer. Positive repaired dual implementer compiles in the bounded new fixture.
- Dynamic overload-selection test: unary-interface monitor overload diverts a call from a preexisting binary-method candidate. This is a recorded **risk**, not a passing compatibility gate.
- `python3 m3/compatibility/verify_m3_default_template_20261017.py` checks all five exact installed byte digests. `bash m3/compatibility/check_m3_default_template_java21.sh` compiles and runs the bounded stock JDK21 subset.

**Unexecuted/hold gates:** actual Synexia Maven/OpenRewrite SDK replay and fixed-point/refusal, full M3 OpenJDK `java.base` and HotSpot image build, jtreg on the modified runtime, bootstrap/JIT/GC/CDS/FFM/JNI/native, JDK API/ABI and method-reference collision corpus, Windows/Linux native platform matrix, performance/retained heap. A passing bounded CI job is not substitute evidence. No speculative native image is introduced because this change has no JNI ABI.
