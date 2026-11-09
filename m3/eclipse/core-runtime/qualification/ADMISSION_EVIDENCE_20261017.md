<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse progress bridge: bounded admission evidence (2026-10-17)

## Repository identities
- Target: hsoliwal/M3jdk21 draft PR #1145.
- Canonical Synexia recipe: hsoliwal/com.synexia draft PR #12135.
- Receiver initial commit: 4bc05689c4779ac80253be17230fbc8102f5a493.
- Canonical recipe intake snapshot: cf6a79e42f637f6a9cb2ee9cdf58df58c06cfeea.
- Provenance: Synexia Eclipse nested progress adapter; unchanged M3 collections progress SPI.

## Actual completed checks
Java 21 `javac --release 21 -Xlint:all -Werror` compiled the new
`M3EclipseProgressMonitor.java`, its JUnit-style test source, and isolated
minimal stubs of Eclipse `IProgressMonitor`, M3 progress SPI and JUnit assertions.
Five test methods were executed in-process:

```
PASS ownedModePreservesEclipseLifecycleAndFractionalProgress
PASS borrowedModeDoesNotChangeParentLifecycle
PASS ignoresNonFiniteAndNegativeWorkAndKeepsUnknownTotal
PASS rejectsIllegalLifecycleAndDoesNotStartOnInvalidWork
PASS preservesM3LongOverflowSaturationAndSingleOwner
PASS stubbed Java21 bridge behavioral checks: 5
```

These were **stubbed API-level checks**, not evidence that real Eclipse Maven/OSGi
or real M3 binaries compile or run. Do not claim a Maven/JUnit pass from them.

The following remote target-file SHA-256 values were separately recalculated
on GitHub-provided UTF-8 source and checked equal to both the recipe manifest
and the corresponding source template:

```text
M3EclipseProgressMonitor.java
526edc5c2a9c6640fa9e266b1f46ac747b92ee1662da2b95f9565c54f7966058
M3EclipseProgressMonitorTest.java
f4b9be7ac510346d18f01ca7cd622a43e70b82396929db1fb6e64407ca8a2571
```

## Pending promotion gates
- Exact-head GitHub Actions run `39035130616`: queued at last inspection,
  no claimed CI pass.
- Maven builds with actual `org.eclipse.core.runtime:3.35.0`.
- Actual OpenRewrite source/LST/fixed-point/refusal tests.
- OSGi/PDE host-runtime resolver, extension lifecycle, UI thread and SWT native
  contracts (outside this bounded adapter).
- No claim that the entire Eclipse framework, Eclipse Collections, SWT,
  Nebula, JFace, JDT, EMF, Equinox or Workbench has been migrated.

Retain this candidate draft until observed head workflows and target-side
tests supply affirmative evidence; a zero-run or queued status is not proof.
