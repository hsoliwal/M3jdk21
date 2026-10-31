<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Executed local qualification

The final generated tree passed the complete source-bootstrap/owning-module command.
The local tree was based on 0c24884dcf5b836a80c1732ec8b31315721f8988 with the qualified successor applied;
the receipt explicitly records this worktree state. Published source equality and hosted
head execution are separate checks.

- Source-JUnit closure: all nine modules built, source seals and packaged licenses/notices verified.
- Current module: 134 production and 161 test sources compiled.
- Java Maven verify: 236 discovered, zero failures/errors, 2 native-only tests skipped.
- Required-native Maven verify: 236 passed, zero failures/errors/skips.
- All nine product JNI libraries compiled from ten C sources with generated headers, strict C17 and fatal UBSan.
- Seventeen explicit native contracts passed; sparse/bounded transfer/fault fixtures and missing-library refusal passed.
- JAR: exact 321-class fresh inventory; exact legal/provenance resources; only java.base runtime dependency.
- Canonical OpenRewrite recipe: three JUnit tests passed, named composition, fixed point, deterministic replay and refusal.
- Thirty-six source-verifier root/integrity/refusal fixture checks passed.

The earlier failing runs remain in canonical history. The scoped target manifest is
unchanged by tests; no source-count or assertion-count substitutes for execution. Hosted
checks remain pending until GitHub runs them. No new performance or full JDK/whole-corpus
completion claim is made.
