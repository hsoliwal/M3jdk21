# M3JDK21 adaptive representation receiving contract

This draft adds package-private M3StringRepresentationPlan, a 26-kind strategy catalogue and a source-level jtreg candidate. Its single implemented decision kernel chooses EMPTY/SCALAR/RANGE/RUNS/BITMAP based on exact source-mask geometry and primitive metadata cost. The class is **not yet wired into java.lang.String or M3String**: existing String, M3StringOwner, tuple, pool, JNI and HotSpot paths are untouched.

Canonical recipe and donor authority belongs in hsoliwal/com.synexia; the paired source PR adds M3RepresentationPlanner and its proof. The two planner implementations have the same selection contract but separate product/source legal ownership. The paired PR has not yet qualified source-sealed generated target files through the actual M3 Maven/OpenRewrite replay; that is an explicit promotion gate.

Runtime invariants: M3String retains exactly owner plus coordinate; no new spelling pool; no per-value bitmap; no retained caller boolean[]; UTF-16 semantics preserve unpaired surrogates, cross-gap pairs and logical selected positions. Any subsequent actual view must ensure that an immutable boolean mask snapshot (or compressed equivalent) is separately retained; a Selection record alone cannot map visible ordinals back to source units.

Follow-up receiving: port the tested adaptive projection class using an exact current-source and jtreg gate, connect the strategy to selected operations only if measured faster, and prove full JVM/JNI/HotSpot compatibility. This draft must not be auto-merged and does not claim 26 implemented storage types. Existing recovery PR #972 and foundation PR #963 remain blockers.
