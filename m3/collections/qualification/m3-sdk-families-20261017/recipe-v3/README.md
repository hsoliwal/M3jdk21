<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 SDK sorted collection facade and constructor warning correction

This is a **bounded** recipe-first follow-on to Synexia SDK family catalogue PR #12113 and
M3JDK21 SDK receiver PR #1127. It does **not** claim complete cross-provider
Guava/Eclipse/Spring/Apache/JDK parity.

## Behavior and source choice
- Preserve exact `java.util.SortedSet` and `java.util.SortedMap` public names.
- Expose the already available `M3FilteredViews.filter(SortedSet,...)` and
  `filterEntries(SortedMap,...)` under the M3 SDK facade. No new element/payload
  arrays or mutable Map.Entry storage.
- Read-only filtered views are live; their comparator, range bounds, predicate
  handling, and storage identity remain with the existing owner.
- Existing `M3ObjectStateSlots28` has the same public zero-argument constructor
  explicitly declared so Java 21 module compilation with `-Xlint:all -Werror`
  does not produce `missing-explicit-ctor`. The body is intentionally empty.
- No JNI boundary changes. Existing bitmap/index JNI compaction stays in its
  established owner, not a Java predicate callback.

## Source and postimage receipts (Git object identities)

| Receiver source | Admitted previous Git blob | Exact generated Git blob |
| --- | --- | --- |
| M3CollectionSdk.java | `042069cd167191e1faa1710fb9a9e65bca14983c` | `0962823301c3f80dbe4956600fe572af2067439b` |
| M3CollectionSdkSelfTest.java | `a62b63a3399d926dabe6645f342381d2a28107ca` | `8d01cb9fbefb76f65a70759d93153cfc20a20008` |
| M3ObjectStateSlots28.java | `c497ef68f62c86814ac6e04eecf73e57496c7209` | `96baeb2098d9100e0decdcf25d7b4f230da2f5e3` |

The recipe detects each exact predecessor and generates the postimage; already
generated outputs are fixed points. Different or partially changed files refuse
before writes. The synthetic self-test covers missing source, unexpected drift,
refusal, and repeated application. The receiving workflow must additionally
reconstruct all three real preimages, execute `--apply`, run `--check`, and
`cmp` them to exact checked-in outputs; *that* verifies true receiver application.

## Build and validation

```sh
mvn -B -ntp -f synexia-m3-recipe/recipes/m3-sdk-sorted-contract-20261017/pom.xml verify
java -cp synexia-m3-recipe/recipes/m3-sdk-sorted-contract-20261017/target/classes \
  com.synexia.m3.M3SdkSortedContractRecipe --apply /path/to/receiver
java -cp synexia-m3-recipe/recipes/m3-sdk-sorted-contract-20261017/target/classes \
  com.synexia.m3.M3SdkSortedContractRecipe --check /path/to/receiver
```

The exact Java 21 SDK oracle tests sorted set/map visibility, bounds, live
backing updates, rejected writes, and entry mutation refusal. JVM/JNI,
full Maven, donor-version API substitution, memory/CPU benchmarking and full
installed JDK gates remain independent. No third-party source copied; API
identities and challenge problem categories are research references only.
The generator and original test additions are first-party Apache-2.0.
