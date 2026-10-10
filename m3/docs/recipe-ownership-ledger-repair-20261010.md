# Recipe ownership ledger repair — evidence and scope

Date: 2026-10-10
Baseline: `e617717424bd0af23881fb383777ae14dc7c8bb4`
Branch: `aix/repair-recipe-ownership-ledger-20261010`

## Purpose

The M3 foundation CI and Synexia Apache import proof both fail at
`m3/runtime-integration/verify-recipe-ownership.py`: eight current Java recipe
classes have no rows in `synexia-recipe-ownership-classification.tsv`.
This repair inventories those exact files and records the target-side role.
It does not alter recipe implementation, generated source, API/ABI, runtime
behavior, or promotion policy.

## Source inventory

| Target class | Git blob | Observed role | Canonical provenance owner |
|---|---|---|---|
| `M3BulkExecutorGateRepairRecipe.java` | `7a6f66a674ee3b51d29c43993658242889a4dc80` | Thin receiver that composes the existing Java and text hash-pinned receivers for the named crate | `com.synexia.rewrite.M3JdkHandoff` |
| `M3BulkExecutorNameMappingRecipe.java` | `11fee9eab8e81c3c61e691ec7858b545e9927072` | Target-specific internal JDK name mapping; no public API or promotion authority | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3BulkExecutorPortRecipe.java` | `ebb3efce355d7d0f89516657560bce3fbc3fd0a3` | Target-specific port composition of Java/text receivers and the target name map | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3Jep474ProofRefreshRecipe.java` | `aed8fe640796c9c7f84d5ef04138e623eed9e278` | JEP 474 / Java 21 proof-workflow and receipt refresh; explicitly excludes product-source mutation | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3JniSaM3FieldRecipe.java` | `0f4cfe9efb6b32c9275e6161b8c3b28dfd833077` | Target-specific JNI / Serviceability Agent field-layout compatibility packet | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3RegexStringPrecomputeSupersetRecipe.java` | `d461e6158caad78b950dbffadd3f68ee5e0d4728` | Target-specific regex/String trial-precompute packet; no semantic or promotion authority | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3TornadoBulkAdapterMappingRecipe.java` | `c8f924878fe45a5685f217a19e5ba704e4d6f2fa` | Target-specific mapping and catalogue state for an optional provider; explicitly not promotion | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |
| `M3TornadoBulkAdapterPortRecipe.java` | `ed3080e3c9b58a13f69487f578c7d1183b72249c` | Target-specific optional-provider receiving packet; no promotion authority | `com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner` |

## Owner verification

The canonical source classes were fetched from `hsoliwal/com.synexia` branch
`develop`. `M3JdkHandoff.java` blob is
`023d2f4cdaf763d9b230cbf23109c6a4f8bb37a7`; it documents a deterministic
source-side handoff builder that packages already-reviewed, target-ready bytes
without semantic adaptation. `M3Jdk21GitBlobRecipeCratePlanner.java` blob is
`425572eb1923e550de552a4277fb4b04ed4e3c79`; it documents candidate-only JDK 21
Git-blob drift planning and explicitly grants no mutation, donor-copy, execution,
or promotion authority. These existing owner classes are referenced rather
than copied or reimplemented.

## Classification rule

Only the gate-repair wrapper is classified `TARGET_ADAPTER_ONLY`: it is a thin
receiver composed from the existing source-sealed receiver formats. The other
seven classes are `JDK_TARGET_SPECIFIC`: each has target-side JDK mapping,
porting, proof, or optional-provider behavior and is not a generic reusable
recipe owner. The ledger's existing schema requires a canonical Synexia owner
and a non-empty target reason for every row.

## Verification boundary

This is a documentation-first inventory and ledger repair. The ownership
verifier must pass in CI after the ledger change. It does not repair the
separate stale `M3String.java` superseded-owner pin, parser/type assumptions,
declarative-wrapper assertion, collection corpus pin, broader recipe tests, or
JaCoCo gate. Those remain separate leaves and must not be represented as fixed
by this change.
