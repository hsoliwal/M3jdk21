# Backport wave current-output consumer recipe

SPDX-License-Identifier: Apache-2.0

Pre-code admission: c095d7dd5c6acc2994cf469f12bfb13cfdbf3737, capability backport-wave-known-output-consumer-20261020. The actual wave workflow39375338020 rejected its historical hash expectation before executing tests. Applied-head proof39375338078 independently qualifies the current eight-file61836-byte checkout, including the new test and documentation postimages.

This source-sealed Maven/OpenRewrite proposal changes only the two historical hash strings in one workflow. It preserves all existing test, serialization and authority checks. Nine exact KEEP inputs include all eight actual owning files and the original0537c17d historical receipt. The planner, policy, catalogue and authority bytes remain unchanged. The original historical receipt is preserved as historical evidence rather than accepted as the current target identity.

Names: com.synexia.m3.BackportWaveCurrentOutput and com.synexia.m3.BackportWaveCurrentOutput.Inverse. Existing source-sealed providers and POM are unchanged. Three named JUnit methods cover both before/after states and input orders, forward/inverse round trips, fixed points, all ten owners' drift/missing/duplicate refusals, and the correspondence between the emitted workflow's three expected hashes and the actual frozen KEEP bytes. They retain real forward and inverse SourceFile outputs only after all three methods pass.

Expected actual evidence: 3 methods,16 successful recipe runs,60 refused runs,2 emitted workflows; zero failures/errors/skips; one exact XML stdout counter marker; Java21 providers; full source and pin closure. SDK files36/nonselfpins35. Actual Maven is NOT_RUN. No product workflow is applied.

Qualify with an existing bounded hosted Java21/Maven lane: python3 -I -S -B verify.py --inputs --self-test; mvn -B -ntp -Dstyle.color=never -DskipTests=false -Dmaven.test.skip=false test; python3 -I -S -B verify.py --reports. Then apply the actual emitted workflow and execute its owning integration job. Keep original private model, full reactor/JNI/JDK/Windows and performance/retention qualifications independent.
