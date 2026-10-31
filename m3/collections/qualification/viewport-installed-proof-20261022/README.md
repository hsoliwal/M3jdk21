# Received viewport verification and native build

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

Canonical Synexia PR12855 commit: `7087b720799e68d1c1f96d2d6574ba0320fcead5`.
Dataset: `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/viewport-installed-proof-20261022`.
Product preimage: `3ed252b92a5b029ea6ae00816ab6a4a4cda56ce1`.

This pass receives three actual Maven/OpenRewrite Results in the existing
`m3/collections/viewport-proof` directory: a CMake native build, ordered Python
verifier, and the original shell entrypoint delegated to that verifier. Reusable
recipe source, donor comparison, source seals and refusal tests remain in Synexia.
Runtime Java/C owners, original tests, public APIs and native exports are unchanged.

The original verifier tested Java before native compilation and reused a fixed
output directory. Its successor requires lint -> compile -> test -> runtime,
rejects existing/overlapping/symlink evidence output, preserves raw failure logs,
generates and compares JNI headers, builds the original native owner, installs it
to an explicit proof prefix and checks an exact relocated copy. Header-only JNI
linkage does not link a second libjvm into the host. No new runtime layer, widget,
collection wrapper or model store is introduced.

```sh
bash m3/collections/viewport-proof/verify.sh --output /absolute/new-proof
```

The original no-argument invocation also works; it creates a new retained evidence
directory under viewport-proof/build. The existing target workflow already watches
and invokes this entrypoint; no workflow edits are included.

The complete canonical entrypoint ran successfully: seven SDK tests, three emitted
Results, zero-change replay, 26 ordered product commands, nine refusal controls.
Original kernel/page Java oracles ran normally and interpreted; native parity ran
with checked JNI under Serial/G1/Parallel collectors, with installed/relocated and
UBSan libraries. Every source hash before and after matched. RECEIPT.json binds
exact source/binary identities and the full87-entry raw-log archive hash. Finite
repeated corpora overlap and are not added as independent coverage.

This is an installed native-library proof on Linux, NOT an installed JDK/HotSpot
image or complete SDK distribution. Native projectPage remains unwired in Java.
The Java21 parser blocker, Java-LST/nine-target receiving, complete filtered/sorted
providers, SWT/Nebula/GEF/RWT, GPU/theme/language/IME/accessibility, other platforms
and comparative resources remain open. No global receiving-phase advance or merge.
