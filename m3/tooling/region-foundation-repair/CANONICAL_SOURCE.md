<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Frozen Synexia task custody, not a second recipe owner

Canonical repository: hsoliwal/com.synexia
Canonical commit: 199b6541ef63bcd16ee60880e5821ac8e1af87ac
Canonical directory: synexia-openrewrite-recipes/recipe-crates/m3jdk-region-foundation-20261016
Target source baseline: 855afe57a679aeb454052630de7beddcb25f5f8f

All Java task sources, tests, templates, POM, prepare.py, qualify.py, qualify.sh and postimages.tsv
are exact source-custody mirrors at that revision. The canonical README records contract,
upstream JDK-8316879 provenance, donor decisions and pending product gates. Improve the
Synexia owner and re-pin this custody; do not independently evolve these copies. They are
build/proof tooling only, never a Java/HotSpot/JNI runtime dependency. Existing receiver
engines are borrowed by exact source hash into the isolated Maven build, not forked here.

Apache-2.0 applies to the authored recipe/tooling; the target jtreg template explicitly keeps
GPL-2.0-only. Original OpenJDK source headers remain unchanged. Frozen postimages.tsv admits
exactly four generated target paths, including the additive test's ABSENT preimage.

The isolated Maven provider coordinates qualify this bounded task only. They neither replace
root custom *-m3-source-1 providers nor prove full reactor or matched-M3JDK/runtime acceptance.
Do not equate stock-JDK regression, source-prefix proof or CI with a complete M3 runtime.
The String -> arrays -> collections -> AST/compiler -> remaining-families order stays intact.
