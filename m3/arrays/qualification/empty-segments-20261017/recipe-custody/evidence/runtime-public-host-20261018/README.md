<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Public runtime recipe replay

All seven runtime datasets were freshly qualified by the public repository's
existing Maven host with its checked-in `sdk-owner` override. The run used no
private recipe source dependency. Each dataset passed the original five actual
OpenRewrite SDK cases, eight input-sealing tests and nine isolated receiver cases.

The recipes were then applied serially to source exported from the immutable
receiving commit. Each pass had an immediate zero-write replay. The 27 final
composed target files matched the built JDK receiving tree byte for byte, and
all 31 current receiver regression methods passed. Historical predecessor
manifests remain exact. An old documentation dataset is not silently allowed to
overwrite a newer scope-annotation successor.

`RECEIPT.json` records commands, source bindings and results. SDK input/result
manifests and complete JUnit XML/text reports accompany each dataset. Full Maven
and checker stdout/stderr are retained with `.txt` suffixes. Each dataset's
`receiver-traces.json` losslessly contains all 18 individual receiver stdout/stderr
files plus the case receipt. Re-encoding a `utf8` field gives the original bytes;
every length and SHA-256 was round-trip checked before this input was sealed.

These are source transformation and custody gates. The separately retained full
image/JNI results determine those runtime gates. The known universal Object
adaptation compatibility counterexamples remain a hold.
