<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Packed-token Java/JNI convergence

This continuation combines the existing packed-token DFA Java/JNI work with the
collection convergence delivery, and refines private mask construction into one
store per 64-token word. The catalogue records candidate contracts and refusals
so future passes can reuse this evaluation. No extra parser or runtime service
is introduced. See CONTRACT.md for the frozen scope and exclusions.

The existing class-backed OpenRewrite owner is unchanged. Dataset
`token-donor-20261018` receives three exact donor files; `token-mask-final-20261018`
then refines one Java owner and adds a Java boundary oracle. A native test
registration helper remains in canonical qualification sources. Each dataset passes five SDK tests, eight sealing cases
and nine receiver cases. Replaying the final dataset writes zero files.
The earlier donor dataset deliberately refuses a later refined source image.

Both the unchanged donor and refinement pass the original 36,020 checks and the
new 903,440 Java boundary checks. The refinement passes 903,641 checks in each
linked native and native interpreter mode. The native helper registers the exact
production kernel with a call counter; it proves public native dispatch rather
than merely observing a Java fallback. Registration produces an expected platform
native re-registration warning. Production remains libjava-owned.

Strict Java 21 and C17 compilation pass. The two internal classes' public
descriptors match the token donor byte for byte, as does the generated JNI header.
The existing String-compatible entry points and construction/getter contracts
remain; the donor's new token/chunk entry points are additive internal APIs.

Three fresh JVM samples per variant measured 1,000 preparations of a 64-state,
1,025-token dense vocabulary. Both variants allocated 173,936,312 bytes and
retained 139,904 primitive payload bytes. Median thread CPU was approximately
321 ms for the refinement and 419 ms for the donor; these are only three short samples per variant.
This is finite workload evidence, not a general throughput claim. Per-child
RSS and CPU are retained in runtime/COMMANDS.json. RSS includes the whole JVM;
it is not a native-allocation-only measurement. Linked native runs use fatal
undefined-behavior sanitizer checks and -Xcheck:jni.

The first resource runner failed because /usr/bin/time was absent. Its exact
source and log are retained in history. The corrected runner obtains per-child
resource usage with wait4. The first jtreg attempt failed against a sparse test
checkout; its log is retained. VERIFICATION.json records the final jtreg result
and distinguishes patched host execution from source-built product acceptance.

The 32-family lineage gate passes with no phase promotion. The source-built
modified JDK/libjava image, full JDK regression matrix, HotSpot/GC/CDS and
non-Linux gates remain unproven. Canonical full-reactor and hosted CI acceptance
are separate from these finite SDK/component tests. Keep the PRs draft.

Typed-token predicate syntax, captures, lexical projection and scope-correct
rewriting remain separate work. M3RX7 composes an ASCII character DFA across
existing token slices; it does not implement those features. The catalogue pins
IndexRegex, MIndexTokenPattern, MIndexASTQuery, M3AST and T-Rex candidates rather
than claiming their contracts have converged. The three previously documented
collection standalone failures also remain open.

Reproduce from this canonical crate with Java 21 and the existing cached SDK:

```sh
mvn -B -ntp -Dm3.dataset=token-donor-20261018 test
mvn -B -ntp -Dm3.dataset=token-mask-final-20261018 test
python3 contract/test_prepare.py
python3 contract/verify_receiver.py --crate . --dataset token-mask-final-20261018 --recipe-owner ../.. --output /tmp/new-rx7-receiver
python3 receive.py --dataset token-donor-20261018 --target /path/to/pinned-M3JDK21
python3 receive.py --dataset token-mask-final-20261018 --target /path/to/pinned-M3JDK21
python3 evidence/token-convergence-20261018/verify_component.py --target /path/to/pinned-M3JDK21 --output /tmp/new-rx7-proof --jdk /path/to/jdk21
```

All paths in raw receipts describe the executed environment. SHA-256 seals,
dataset manifests and canonical commit identities establish portable source
identity. Regeneration uses the same source paths in a fresh checkout.
