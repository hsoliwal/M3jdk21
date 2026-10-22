<!-- SPDX-License-Identifier: EPL-2.0 -->
# Bounded A3 fixture parser candidate

The original full recipe-first reactor timed out twice in the A3 lab; R3 stopped after
1803.83 seconds, with only A3Apply's four tests complete. The original receipt is preserved.
The lab previously rebuilt OpenRewrite's parser before every application and postimage parse.
This candidate confines one parser to a serial fixture and invokes the donor's full reset
before every parse. Every schedule replay builds an independent fresh parser. No static
parser or stored compiler/behavior verdict is introduced.

The original 48 fixtures, six schedules, four complete lab executions, every individual
compile/behavior/contract/payload comparison, pass budget and textual fixed point remain.
The original Maven POM, strict compilation and integration profiles remain byte-identical.
The new discriminator changes same-name method signatures across primitive and generic
types, compares fresh attribution and checks recovery after malformed input.

The exact existing snapshot owner upgrades only the two known original lab/test hashes.
The mastery installer's generated resources and blob pins match the new source exactly.
The A3Mastery activation performs that exact upgrade before the existing installer.
Unknown drift is refused. This remains historical proving-fixture maintenance; canonical
Synexia recipe classes and Nebula widget/product interfaces remain separately authoritative.

Focused proof: four tests passed, all 54 main and 31 test sources compiled under the original
Java21 -Xlint:all -Werror -proc:none POM. Exact upgrade, fresh replay, drift/foreign-path/plaintext
refusal and same-name attribution checks passed; source review is clean.

The full verify run with semantic-coverage,dag-manifest,transfer-contract stopped after
754.56 seconds with REFUSED_STORAGE_RESERVE when K: fell below 8 GiB. Only A3Apply's
four tests had completed. Its terminal receipt and complete log are included. The four
focused tests above passed separately before this run. No full-reactor/coverage/DAG/transfer,
native, timing or product-promotion claim is granted. This remains a draft candidate.
