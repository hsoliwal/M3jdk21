<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Charcopy current-successor source qualification

This task admits the exact current eight-file receiver tuple as a zero-write source checkpoint and executes the unchanged historical char-array-copy recipe on isolated receiver-shaped fixtures. It preserves the historical manifest and its refusal of the current successor. Source foundation: `50cb3dc489c3f95a9781e50549d2e1866390f057`. Receiver baseline: `hsoliwal/M3jdk21@677437be295b23f7ccf7409be287008944d0e8b0`.

The current M3String source contains two additional identical 143-byte UTF-8 overflow guards. Removing exactly those two blocks recovers the entire historical 35ed998540e044eed8cb7596e2e2c019ae369bd3 file. Both guards remain intact in the current receiver. The historical script, manifest and seven original tests remain byte-identical and retain their old pins.

The SDK reuses the unchanged 1c1b recipe engine, 38df manifest kernel and d798 Maven POM. Its eight KEEP entries cover four current Java owners, three existing recipe files and the receiver LICENSE. The small scanner wrapper rejects the two normalized legacy source paths; ABSENT-to-ABSENT rows are unsupported by the existing manifest. The filesystem qualifier separately rejects symlinks and broken links. Target Java sources are PlainText data, not compiled JDK implementation classes.

## Bounded execution

No qualification has run for this authored checkpoint. A provisioned Linux JDK 21, Maven and Python 3 environment must run both Maven commands under the existing aggregate/process-group supervisor. Enforce a 512 MiB aggregate task limit, retain at least 8 GiB free, monitor source checkout plus output/cache roots, and reap the full inherited child process group on failure or timeout. The driver checks reserve at command boundaries; it does not independently enforce the aggregate limit or claim standalone descendant lifetime control.

Provision a fresh canonical absolute output directory Q with `artifacts`, `tmp`, `home`, `cache` and isolated Maven repository/settings locations. Set `M3_QUALIFICATION_ROOT=Q`, `M3_QUALIFICATION_ID` to a fresh safe identifier of 16-128 characters, `M3_RECEIVER_ROOT` to the exact sparse receiver checkout, `HOME=Q/home`, and `TMPDIR=TEMP=TMP=Q/tmp`. Q must be distinct from and outside the canonical task and receiver trees; either source checkout may live inside Q. Use the same isolated Maven repository/settings and supervised environment for both commands.

```sh
mvn -B -ntp -s "$Q/settings.xml" -Dmaven.repo.local="$Q/m2" -f "$TASK/sdk/pom.xml" test
mvn -B -ntp -s "$Q/settings.xml" -Dmaven.repo.local="$Q/m2" -f "$TASK/pom.xml" -Dm3.qualification.root="$Q" verify
```

The unchanged SDK POM writes its normal target output under `TASK/sdk/target`; the external supervisor must monitor that checkout too. Its validate hook checks sealed inputs only. The root proof refuses a missing, stale or incomplete named SDK receipt or Surefire report. An inputs-only check never qualifies the named recipe.

## Required evidence

The three named SDK JUnit methods require four actual current fixed-point runs and 37 refusal cases, both source orders, no generated outputs, and exact two-guard/full-historical-fixture identities. Refusals cover every owned drift/missing/duplicate, normalized legacy spelling and order, either one-guard mixture, and three mixed predecessor owners. All three methods must execute with zero skips, errors or failures.

The root Maven proof then requires three actual stdlib filesystem test methods with 38 refusals, and executes the receiver's unchanged apply.py as a separate program on complete original MIndexString/invariant fixtures: full forward, check, fixed point, inverse and inverse fixed point. A distinct exact 35ed superseding fixture must remain unchanged under check/forward/reverse and pass all seven unchanged historical tests. Seventeen further invocations must refuse with the expected diagnostic and no partial mutation, including the actual current successor and either single-guard mixture.

Our fixture directories are retained until ephemeral job teardown. The original unchanged seven-test executable may create and remove its own temporary fixture directories; the hosted census must account for legitimate temporary teardown through its established bounded complete-rescan policy. Do not modify that historical program or silently ignore arbitrary traversal errors to manufacture a pass.

Every canonical input and all eight actual receiver files are rehashed after proof execution. `INPUTS.json` records full source/Git/UTF-8 identities and is itself sealed by the caller's source binding. Evidence is written beneath `Q/artifacts`: fresh guard receipt, filesystem test log/receipt, individual bounded historical command logs, source-custody receipt and final execution receipt. The historical source and receiver files are never rewritten.

## Acceptance boundary

A future PASS_SOURCE_SUCCESSOR_CUSTODY proves named source-guard scheduling and isolated historical-program replay only. It does not qualify original private typed recipe 3bdee, target-Java compilation, matched-JDK/jtreg behavior, JNI, performance, retention, runtime application, whole-repository green CI or phase advancement. The separate downstream current concat-overflow test pin remains an unresolved frontier. Keep drafts and broader gates open until their own actual receipts exist.

Licensing and attribution are recorded in THIRD_PARTY_NOTICES.md. Headerless historical helper/test per-file licensing remains NOASSERTION; repository GPLv2 evidence does not create an Apache or Classpath grant.
