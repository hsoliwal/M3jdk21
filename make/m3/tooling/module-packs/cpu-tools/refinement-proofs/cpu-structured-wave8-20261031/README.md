<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wave 8 bounded product results — passive receiving note

This scope receives three first-party Java21 CPU primitives, two independent Java oracle fixtures, one opt-in target-owned verifier and this exact passive coverage note. The parent M3JDK21 product retains its existing module and public runtime authority. No Synexia recipe sources, Maven authoring crate or Java recipe tests are sent to the receiving product.

## Local proof, not final target acceptance

`python3 m3/tooling/module-packs/cpu-tools/fixtures/verify_wave8.py --root <fresh-receiver> --out <fresh-outside-root>` compiles only the five received Java files using `javac --release 21 -proc:none -Xlint:all -Werror`, then independently checks JIT and interpreted mode oracles and sealed input identities. It is not an actual M3JDK21 JMOD build, does not run Maven/OpenRewrite, cannot prove full-platform compatibility, and is not a performance claim.

- `M3TwoSat`: bounded exact 2-CNF, not full SAT/SMT.
- `M3ArcConsistency`: sound binary CSP local filtering, not complete CSP decision.
- `M3DagReduction`: exact transitive reduction for strictly forward, sorted CSR DAGs only.

Maintain String -> arrays -> collections -> AST/compiler admission order. Production JDK/API/JNI contracts, native/toolchain/build licenses and provenance must qualify independently before promotion. The 91-family CPU catalogue remains partial.
