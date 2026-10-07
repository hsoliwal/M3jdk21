# Synexia canonical Java 21 FILE convergence receiver

Status: stacked receiver binding on M3JDK21 PR #280.

Canonical source:
- repository: `hsoliwal/com.synexia`
- revision: `0205fceb613fbe1fe0f32be84c50bd42c5f6ec28`
- canonical owner: `com.synexia.rewrite.M3Java21FileConvergenceRecipe`
- canonical source Git blob: `48dc2e55745928df249f0b8c281e93761694ac52`

M3JDK21 keeps:
- `com.m3.rewrite.M3Java21ConvergenceRecipe` as frozen historical compatibility/proof residue;
- `M3Java21ConvergenceCatalog` as the target-local named activation adapter;
- target-specific scope registry/build/runtime/promotion authority.

The declarative recipe `com.m3.rewrite.M3Java21Convergence` must activate the canonical Synexia
recipe rather than re-declare a second atom/pattern/documentation DAG.

The canonical Synexia recipe must remain behaviorally equivalent to the retained compatibility
oracle for the admitted Java 21 FILE domain:
inventory -> atomize -> patternize/IOP -> document -> safe unused-import cleanup.

Missing type attribution must fail closed for import cleanup. Non-Java input remains outside this
Java transformation surface.

Future reusable changes to this convergence DAG are mastered in Synexia first and re-imported
through the pinned recipe-home handoff.
