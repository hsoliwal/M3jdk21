# m3/compiler/reference-flow

`jdk.m3.compiler.reference` (module `org.m3jdk.compiler.reference`, artifact `com.m3:m3-compiler-reference-flow`):
read-only javac reference-flow facts over attributed trees. Census2 leaves P2-132 (`M3CRefPathCertainty`) and
P2-136 (`M3CompilerReferenceCensus`) with their closure.

| Class | Fact |
|-------|------|
| `M3CompilerFactsAudit` | the attributed-tree snapshot every analysis is bound to: source digest, analyzer identity (engine digest), proof key, loop and allocation facts |
| `M3LoopFacts` | loop trip-count classification |
| `M3SourceLiveness` | per-method CFG over local slots (use/def/successors) and liveness, plain and extended |
| `M3Liveness` | backward liveness solver (64-slot words and wide `BitSet` form) |
| `M3ReferenceFlow` | source-local reference origins per alias, sites, escape evidence; projection to boundaries where origins stop being visible |
| `M3CompilerReferenceCensus` | per flow edge: alias counts before/after, possibly ending, signal masks, count lanes into caller arrays |
| `M3CRefPathCertainty` | path-sensitive MAY/MUST local alias certainty per snapshot |

The result records of the six analysis classes (all but `M3LoopFacts`, whose counts are an input) say what they
do not authorize: `authorizesReclamation()`, and where present `authorizesRcElision()` and
`authorizesSourceMutation()`, return false. The facts are evidence for receivers such as `jdk.m3.gc.M3GcUsePlan` and
`jdk.m3.compiler.lifetime.M3DoopLifetimeSlotPlanner`; they never release, reuse or rewrite anything themselves.

## Owner

A new module under `m3/compiler`, next to `nullness-facts` (same shape: `requires transitive jdk.compiler`). The
census proposed `jdk.internal.m3.indexstring.compiler` in `m3/ports/indexstring`; that would add `jdk.compiler`
to the indexstring port, which needs nothing from javac. Progress types map to the existing
`jdk.m3.collections.M3ProgressMonitor` / `M3ProgressState` (relocations of the same donor interface and record).

## Recipe

`recipe/relocate.py <donor-checkout> [--check] [--commit=SHA]` rewrites the package line and the two progress types
of the 13 FILE rows in `recipe/RELOCATION_MAP.tsv`, keeps every other byte, refuses on a site-count mismatch or a
surviving donor name, and writes `PROVENANCE.tsv` (donor blob and sha256 of each input, sha256 of each output).
All donor names live in `RELOCATION_MAP.tsv`, so the recipe and the module are a fixed point of the JDK-naming
rewrite. Corrections are in `RECIPE_MEMORY.tsv`.

## Gates

See `GATES.tsv`. `recipe/differential.py` runs the donor closure and this module side by side (the three donor
proofs and `recipe/CrefFactProbe.java`: random Java units through every analysis, records dumped reflectively with
only the engine-derived digests masked) under JIT and `-Xint`; `recipe/mutants.py` checks that single-site mutants
of five classes are killed.
