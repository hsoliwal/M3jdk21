# Synexia receiver packet scope

Status: target-specific compatibility/proof rule.

Reusable Maven/OpenRewrite semantics are canonical in `hsoliwal/com.synexia`.
M3JDK21 may keep exact target-preimage packets and thin receiver/proof code, but a
`synexia-*` hash-pinned Java crate must never become a backdoor for installing or modifying:

- OpenJDK product source under `src/**`;
- OpenJDK jtreg source under `test/**`;
- a canonical `com.synexia.*` recipe implementation.

The local `M3Jdk21HashPinnedSnapshotRecipe` remains target-specific because its exact packet
semantics are bound to M3JDK21 preimages. For `synexia-*` crates it is restricted to target-side
adapter/fixture/proof Java beneath M3 tooling.

The immediate regression packet repairs the A3 receipt test after migration to
`com.synexia.rewrite.atom.M3PureIntConvergenceRecipe`. It removes the stale assertion for the
former `com.m3.rewrite.M3Java21Convergence` identity. The production A3 implementation is
unchanged.

Verification requirements:

1. exact A3ApplyTest preimage hash;
2. one FILE-local hash-pinned recipe change;
3. Java 21 parse/round-trip;
4. drift refusal;
5. unchanged second pass;
6. receiver-scope tests proving `synexia-*` cannot target OpenJDK product/test source;
7. normal A3 test/compile gates.
