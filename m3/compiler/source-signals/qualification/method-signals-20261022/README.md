<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 attributed method-level source facts (opt-in)

**Product boundary:** M3JDK21 is the modified JDK21 itself. This changes its existing
`m3/compiler/source-signals` Plugin source owner in place; no parallel plugin, M3-on-JDK runtime,
method-level Java object table, consumer ABI or new JNI symbol is introduced.

Existing `M3SourceSignalPlugin` still generates the identical SHA-256-sealed
`M3JDK21-SOURCE-SIGNALS/1` S/T/E type-level facts for its original
`-Xplugin:M3SourceSignals out=FILE include=annotated|all` invocation. A new
optional `methodOut=FILE` option emits `M3JDK21-METHOD-SIGNALS/1` in a separate
SHA-256-sealed artifact from the same attributed ANALYZE trees.

The helper `M3MethodSignals` records actual source-declared method/constructor
signatures and 14 conservative AST source counters: calls, allocations, member refs,
direct/candidate/unresolved calls, loops, branches, try/catch, lambdas, field references
and syntax writes. Source-attributed call-edge occurrence counts remain distinct from
runtime invocation frequency or closed dynamic-dispatch identity. All output names are
exact escaped compiler signatures, not heuristic hash keys. Sorting and checksum sealing
are deterministic and idempotent.

**Current-source recipe:** Canonical `hsoliwal/com.synexia` Maven/OpenRewrite crate
`m3-method-source-signals-20261022` pins the current original plugin SHA-256, four
additive Java outputs and exact Git blob identities in SOURCE_MANIFEST.tsv. Product changes
were applied serially with existing-source SHA matching; preimage drift refuses receiving.
The original compiler package and public Plugin interface are retained.

**Focused executable gates:**
- Strict `javac --release 21 -proc:none -Xlint:all -Werror` on the actual original
  plugin + new helper/self-test; genuine JavacTask.parse/analyze attribution.
- Original JUnit Maven module `m3/compiler/source-signals` and new JUnit checks:
  unchanged S/T/E bytes with/without methodOut, independent method/source fixture,
  negative option/alias refusal, fixed-point and SHA-256 contract.
- Canonical Synexia Maven/OpenRewrite first apply, second fixed point and changed
  original plugin refusal.
- Full modified JDK21 compiler/JTreg/JCK/HotSpot/JIT/GC/CDS, cross-platform,
  memory/profiling and installation gates require independent execution. No computed
  source signal certifies runtime cost, purity, escape, inlining or method equivalence.

The independent local Java21 helper test passed **111** deterministic compiler-attribution
assertions with warnings-as-errors. That is isolated compiler evidence, **not** original
M3JDK21 plugin-module JUnit or installed JDK acceptance. The String→arrays→collections→
AST/compiler receiving order remains locked; this opt-in candidate does not advance it.
