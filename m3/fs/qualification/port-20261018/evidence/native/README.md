<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Fresh native and JNI qualification receipts

These are **staging qualification results from the 2026-10-19 reconstruction**.
The `port-20261018` directory identifies the continuing port campaign; it is not
the execution date of these receipts. No results from the lost 2026-10-18
workspace are presented as evidence for the reconstructed implementation.

## Scope and result

| Gate | Result | Evidence |
| --- | --- | --- |
| Native CTest | 12 of 12 passed | `ctest.log` |
| ASan, UBSan, and leak detection | 12 of 12 passed | `sanitizers-ctest.log` |
| Generated JNI declarations | Java 21 compiled all 33 production sources and emitted two headers | `jni-abi.json`, `jni-abi.log` |
| JNI export coverage | 45 declared functions equal 45 defined JNI exports; none missing or unexpected | `jni-abi.json` |
| JNI C type checking | Both C translation units passed with their generated Java headers force-included | `jni-abi.log` |
| Donor serialization compatibility | Four donor and target canonical records match byte for byte | `precompute-donor-format1.tsv`, `precompute-target-format1.tsv` |

`source-inventory.json` binds all 25 staged native files to their byte counts and
SHA-256 hashes. The donor repository is `hsoliwal/com.synexia`, pinned at
`289e32f130c9c8e4a1cf0edc1066327d699393bf`; the campaign's `DONORS.tsv`
contains the independently verified donor paths, Git blob identities, and
licenses. Sparse supplemental source atoms belong to the same pinned commit.

The regular and sanitizer runs use the frozen source tree at
`/tmp/m3fs-native-work/postimage/m3/fs/native`. The final JNI check uses the
complete Maven staging module at `/tmp/m3fs-root-work/qualification/m3/fs` and
its actual `target/native/libm3fs.so`. These paths identify observed staging
inputs. They are not generated-receiver build paths. The JNI receipt records
source, generated-header, native-header, toolchain-header, and binary hashes,
plus the actual command argument arrays. `reproduce.txt` records the build and
signature-check commands with the original toolchain locations.

The native ABI remains 1. The supported capability mask is 16383 and the
persisted semantic feature mask is 2097151. SQLite schema version 1 and the
MIndex serialized image magic/version remain compatible with the donor.

## Reading the auxiliary logs

`suite-details.log` concatenates the original detailed regular and sanitized
CTest outputs, each under its source filename. This preserves command lines,
individual assertions, and the original logger's timestamps/time-zone labels.

`precompute-qualification.log` contains focused array/format and prepared-state
qualification, followed by the publication-race witness and the corrected
implementation's regression results. The `recovery-directory-race-preimage.log`
section is an **expected failure of the previous helper**, used to demonstrate
the bug. The corrected helper passed the four focused publication/mapped tests,
including 46,335 retained assertions and 10,000 randomized byte cases. These
milestone counts overlap and must not be added to the final backend test total.
The final combined Maven gate is documented by the backend evidence owner.

The initial leak-detector attempt was blocked by the sandbox's restriction on
inspection of the process's own `/proc` task entries. The preserved sanitizer
PASS came from the authorized unrestricted execution of the same local suite,
with `detect_leaks=1`. It does not silently disable leak detection.

FUSE was compiled with strict warnings; mounting was not tested. This evidence
covers Linux x86_64, not Windows or macOS execution. It does not establish a
full modified OpenJDK build or full OpenJDK test-suite qualification.
