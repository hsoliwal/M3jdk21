<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Existing ProgressJob and monitored templates: current-source validation

Product pin: d6d76b563050fc13c4fa79bc494edf29f1614b37.
No product source or historical proof was modified. No new API or scheduler is proposed.

## Original verifier: actual product image unavailable

The unchanged product `m3/runtime-integration/progress-job-20261018/verify_progress_job.py`
was invoked with the real current baseline/candidate source tree, genuine stock Java21,
and checksum-pinned Eclipse inputs. All eight original product/dependency source seals
match DONOR-REVIEW.json exactly, and all eight Eclipse artifact seals match.

Its output under `original-verifier-held` records FAIL because an actual M3JDK21 image's
`bin/javac` is absent. The missing path explicitly uses the name UNAVAILABLE-M3JDK21-image.
This is a retained environmental admission hold, not a ProgressJob behavioral failure.
The existing verifier and its historical receipts were not altered or relaxed.

## Additional executed stock-patch scope

`verify_stock_patch.py` imports only the original verifier's flags, copy/hash helpers,
and constants. It independently orchestrates the existing current-source witnesses.
It compiles nine actual source owners into an explicit stock21 java.base patch: the
reviewed seven dependencies, ProgressJob, and existing MonitoredRunnable needed by
M3MonitoredTemplateTest. Source copies and exact SHA-256 identities are in its receipt.

Java: verified Temurin 21.0.12.1+1. API compilation retains -proc:none, -Xlint:all,
-Xlint:-this-escape, -Werror, -Xdoclint:all/protected and the original doclint package
filter. Strict consumers retain -Xlint:all -Werror. Both compilations emitted no diagnostics.

All executed checks passed in mixed and interpreter modes with assertions and -Xcheck:jni:

- Existing M3ProgressJob: 81 assertions per mode, covering result/exception identity,
  scopes, pre/post cancellation, interruption, inner-Future binding, submitMonitored,
  ordinary submit's lack of scope capture and simultaneous independent worker bindings.
- Real EclipseStatusIdentity: 37 assertions per mode against original checksum-pinned
  IStatus/Status/MultiStatus jars; the exact values and child identities are preserved.
- Existing M3MonitoredTemplateTest: direct Runnable/Callable compatibility, monitored
  Runnable/Callable default methods, worker progress, cancellation and scope cleanup.
- Existing OldCallableClient compiled on stock Java21 runs unchanged under stock and
  patched runtimes. Recompiling against the patch produces three byte-identical classes;
  frozen classes remain unchanged after execution.
- Existing negative ProgressJobRunnableConflict witness fails for the required
  incompatible-return and weaker-access diagnostics, not unresolved API symbols.

Receipt: `stock-patch-current-source/receipt.json` contains all 16 command observations,
source/artifact/toolchain/class hashes, and PASS_BOUNDED_STOCK_PATCH_ONLY.
Command outputs remain individually retained. No witnesses were replaced with stubs.

## Contract and admission boundaries

Returning Status.CANCEL_STATUS is not Future.cancel. A monitor-thrown cancellation inside
FutureTask becomes exceptional completion with preserved cause identity. Binding outside
FutureTask can prevent FutureTask.run and leave it pending; the existing core witness
confirms that counterexample and uses inner binding as its supported composition.

ProgressJob remains Callable-oriented and cannot also implement Runnable because their
run(ProgressMonitor) return/access contracts differ. This existing limitation is preserved.
The old dual-interface client remains a distinct finite positive compatibility witness.

No runtime defect appeared in these bounded current-source executions. Actual built
M3JDK21 image execution, full-image API admission, source-built image/jtreg/JCK/platform
qualification, whole Synexia IJobResult context, and complete Eclipse Job scheduling,
families/rules/UI/status-conversion contracts remain unqualified. STRING phase and the
Object universal compatibility HOLD remain unchanged. A stock java.base patch is not
reported as an integrated M3JDK21 image.
