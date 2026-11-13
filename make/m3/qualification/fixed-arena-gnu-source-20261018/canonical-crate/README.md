<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fixed arena feature macro prerequisite

`com.synexia.m3jdk.FixedArenaGnuSource20261018` applies the existing canonical
hash-pinned text engine to one existing M3JDK21 native owner. It wraps the owner's
`_GNU_SOURCE` definition in an `#ifndef` guard. The JDK build already supplies the
same feature macro; treating its redundant definition as a warning blocks the
unchanged warnings-as-errors build.

## Contract and qualification

The owner remains `src/java.base/linux/native/libjava/M3PrimitiveFixedArena.c`.
There are no JNI signature, mapping, permission, lifecycle or error-path changes.
The recipe admits only the sealed original or its exact reviewed postimage,
rejects missing, duplicate, drifted and late-mutated targets, preserves unrelated
files, and is a fixed point on the postimage. It uses actual OpenRewrite SDK
Results for materialization.

Fresh recovery qualification on 2026-10-19 passed all six SDK test groups.
`contract/verify_native_owner.py` compiles the entire original and candidate
native owner with `-std=c11 -O3 -g0 -fPIC -Wall -Wextra -Werror`, the verified
JDK 21 JNI includes, and both macro configurations. The original with an external
definition refuses the duplicate macro as expected. The original without that
definition and both candidate configurations compile without diagnostics and
produce identical object bytes. This isolates the prerequisite; full M3JDK21
image compilation is a separate integration gate, not inferred from this object
comparison. Exact commands, hashes and diagnostics are retained under `proof/`.

## Reuse decision

The entire existing native owner is preserved from M3JDK21 baseline
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The selected pattern is the conventional
conditional feature-macro guard already compatible with that owner's C
preprocessing contract. No new allocator, mapping implementation or third-party
source is introduced. Runtime GPL and Classpath notices remain in both sealed
snapshots; canonical recipe and verification tooling use the Synexia Apache-2.0
notice.

Run SDK verification from this crate with JDK 21:

```sh
mvn -B -ntp test
python3 contract/verify_native_owner.py --help
```

The canonical engine owners must be present in `../../src/main/java`, as in the
source PR. No private Synexia runtime artifact is added to M3JDK21.
