<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source version transition after older exact filtered/sorted guard refusal

Hosted [run 38986873534](https://github.com/hsoliwal/M3jdk21/actions/runs/38986873534)
and inherited [run 38986873533](https://github.com/hsoliwal/M3jdk21/actions/runs/38986873533)
failed exclusively in the historical source verifier, before Java/JNI compilation.
The new legitimate `M3LongFilteredView` was sealed under the 2026-10-17
source recipe, but old verifier expected its 2026-10-16 SHA-256.
Unchanged List/Deque/Set/Map/PriorityQueue source seals passed.

Canonical repair **first**: [Synexia #11926](https://github.com/hsoliwal/com.synexia/pull/11926)
source-sealed text recipe `M3Jdk21FilteredSealSuccessor20261017`,
commit `759c60aeb9bed2c2579631fa7a801b89076ca93d`. Only historical verification logic is changed to
admit exactly one target code postimage and exact three-file successor
SHA-256 manifest with every actual file present. Historical manifest
and every other source SHA remain unchanged. The new Python negative
suite tests the original and successor states and fail-closed tampering.

This branch installs those two exact reviewed text templates, records
the source-recipe manifest and reruns both old and new checks before
Java21/JNI. Do not count the old red CI as green, disable a legacy
gate, or claim SDK/JDK release completeness.
