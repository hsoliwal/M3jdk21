<!-- SPDX-License-Identifier: Apache-2.0 -->
# Executed target receiver proof — 2026-10-17

Scope: JDK-only **read-only** candidate ledger verification; this is not
admission to M3JDK21's OpenJDK code or an OpenRewrite runtime port.

## Source-exact local execution

Local Linux x86_64, OpenJDK 21.0.12.1. The three locally executed inputs
match the **exact committed Git blob identities**:

| Reviewed branch file | Exact SHA-1 Git blob |
| --- | --- |
| `M3AtomicLeafReceiverVerifier.java` | `96052c0d0bb51542ac6ad40073bd56068b64931c` |
| `m3-four-leaf-candidate-20261017.tsv` | `c5386fc4699c1ed75c28dc956a5d3f3a80f305e3` |
| `m3-filtered-wrapper-candidates-20261017.tsv` | `7ce3b59216c18a86b26224b0ffb38959eef06d53` |

Executed using the exact files:

```sh
javac --release 21 -Xlint:all -Werror -d build \
  m3/compatibility/M3AtomicLeafReceiverVerifier.java
java -cp build M3AtomicLeafReceiverVerifier --self-test \
  m3/compatibility/m3-four-leaf-candidate-20261017.tsv \
  m3/compatibility/m3-filtered-wrapper-candidates-20261017.tsv
```

Observed output:

```text
M3_RECEIVER_REFUSAL_PASS cases=4
M3_RECEIVER_CANDIDATES_PASS leaves=4 wrapper_workstreams=4 runtime_admitted=0
```

The four negative controls reject duplicate leaf IDs, false runtime
admission, drifted Git blob IDs and false wrapper promotion. The source
M3 leaf proposal remains **Synexia draft #11925**; it has separate bounded
Java21/JNI evidence but its real OpenRewrite Maven execution is pending.

The verifier does not fetch another repository or prove its current
HEAD matches the pinned blobs. A version-change requires a deliberate
receiver-ledger and pin review. Full native OpenJDK build, Java ABI,
HotSpot/JNI, jtreg/JCK and collection provider/runtime compatibility are
NOT RUN. This receiver grants no public API or runtime promotion.
