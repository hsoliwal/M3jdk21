# Receipt: atom-coverage hardening from com.synexia `m3jdk-atom-admission-count-20261101`

The com.synexia receiver crate `m3jdk-atom-admission-count-20261101` (develop `06082e32476e`) seals postimages for
`M3AtomCoverageLedger.java` (blob `09cd2ec9ca11`) and `M3AtomCoverageVerifierSelfTest.java` (blob `ee3d59f37c5a`).
Master took that crate's main change by another route: `86e18b6fb709` excludes optional FAIL from the admitted count.
Two hardening changes in the crate never reached master:

1. `requiredGateSatisfied`: the `CONTRACT_CHANGE_AUTHORIZED` gate needs an actual PASS. A NOT_APPLICABLE
   disposition no longer unlocks a public contract change. Before, any admissible status did.
2. `requireRelativePath` refuses Windows drive-relative paths such as `C:src/A.java` and `Z:`. Before, only `X:/`
   was refused.

## Method

The merge is three-way. The base is the predecessor crate's postimage
(`m3jdk-atom-coverage-contract-20261031`, blobs `8771fada3220` / `b9b412c3dc78`), ours is master, and theirs is
the crate postimage. Both files merge cleanly.

`memdiff.py` checks for duplicate members, because master received part of the crate by another route. It
reports one MISSING member per file (`requiredGateSatisfied`, `windowsDriveRelativePathIsRefused`) and no
duplicates. `admittedAtomCount` stays master's evolved body, now calling `requiredGateSatisfied`.

## Gates (OpenJDK 21, Linux; runner `gate.sh`)

`GATES.tsv`. The atom-census module comes from master `c7e5a561c081`, materialised through the Git Data API.
