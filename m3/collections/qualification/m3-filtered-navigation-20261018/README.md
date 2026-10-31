<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK filtered wrapper and mask reconciliation

This is a finite continuation of the existing Synexia collection refinement, based on M3JDK21 commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The canonical authoring owner is `hsoliwal/com.synexia`, under `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`, dataset `filtered-navigation-20261018`. `SOURCE_RECEIPT.json` identifies the published source checkpoint. The `recipe` directory is a pinned verification and receiving mirror, with the unchanged canonical engine sources under `sdk-owner`; it is not a runtime dependency.

## Synexia M3 invariant

Maintain reusable, validated transformation recipes; apply their actual Maven/OpenRewrite results through serial, contract-preserving per-file transformations; and continuously improve algorithm selection through problem-category catalogues and donor evaluation. Decompose a source into meaningful replaceable behavioral atoms when this improves reuse or verification. Preserve public callable interfaces, representation contracts and established valid-path behavior. Record contract defects explicitly instead of presenting their correction as equivalence to the defective behavior.

## Selected operations

| Existing owner | Reused operation and correction | Admission boundary |
| --- | --- | --- |
| `M3FilteredViews` | Resolve a legal null navigational candidate using a stored opposite boundary, including custom comparators that place null in the middle. | Live owner and comparator semantics remain authoritative; no fabricated null search key or retained membership cache. |
| `M3FilteredViews` | Use comparator membership and the existing stored-key atom for sorted-map entry membership. | Predicates receive stored representatives. Corrected comparator aliases are a disclosed contract repair. |
| `M3FilteredViews` | Restore strict read-only guards for sequenced endpoints and reversed sorted views. | Empty mutations also refuse; original backing views remain live. |
| `M3FilteredViews` | Replace known private filter decorator recursion with an ordered, depth-sized dynamic array of predicates or membership continuations. | No payload copy. Preserve inner-to-outer callback order and repeated representative scans; generic membership can still demand quadratic callbacks. Foreign wrappers and sorted membership remain opaque. |
| `M3IndexPathMasks` | Extend existing output/input identity preflight to required and forbidden mask vectors before either backend runs. | Empty and zero-limit windows also reject destructive aliases. Existing input/input sharing stays legal. Java and actual JNI use the same public validation. |

The wrapper atoms recover suitable first-party implementations from the earlier merged reconciliation, including M3JDK21 PR #1204, with current bounded-navigation behavior retained. The mask fix reuses the existing validation atom and actual C selectors. Native production sources, JNI descriptors and public callable APIs are unchanged. The catalogue retains GitHub donor pins and license decisions; LeetCode, HackerRank and GeeksforGeeks supply problem taxonomy and adversarial shapes, with no challenge or editorial code copied.

The unchanged ordinary read trace excludes the two repaired paths: comparator-based entry membership has corrected predicate/getter demand, and ambiguous null navigation can perform one added comparison against a real stored boundary. Strict endpoint recovery is limited to the Set factories and their derived views. The current Map factories retain their outer JDK unmodifiable facades, including `NoSuchElementException` for empty endpoints of some sequenced key/value/entry views. The stronger historical strict-map guarantee remains unreconciled; restoring it requires a separate admission covering concrete wrapper types, marker interfaces and delegation behavior.

## Reproduce the transformations

From the repository root with Java 21, Python 3 and Maven:

```sh
Q=m3/collections/qualification/m3-filtered-navigation-20261018
mvn -B -ntp -f "$Q/recipe/pom.xml" \
  -Dm3.dataset=filtered-navigation-20261018 \
  -Drecipe.owner.root="$PWD/$Q/recipe/sdk-owner" -Pcontract verify
python3 "$Q/recipe/receive.py" --dataset filtered-navigation-20261018 --target "$PWD" --check
```

The existing shared task requires five real OpenRewrite SDK tests, eight input-sealing tests and nine receiving refusal/fixed-point cases. The receiver admits every exact preimage or postimage before writing files serially; drift refuses. It receives retained SDK results, never handwritten output substitutes. A second application changes zero files. On the submitted tree, every target must already equal its qualified postimage; the focused workflow checks those fingerprints explicitly.

## Reproduce runtime qualification

On Linux with Java 21 in `JAVA_HOME`, GCC, Python 3 and Maven:

```sh
python3 m3/collections/qualification/m3-filtered-navigation-20261018/runtime/verify.py
```

The focused runtime POM compiles every current collection production Java source and the declared 61-source test closure with warnings as errors. It uses the genuine public JUnit 5.10.2 artifact explicitly for this isolated proof. Separate fresh Java-only and actual JNI runs execute the JUnit closure, existing array/offset/mask tests, explicit native-required checks, missing-library refusal and the 65,536-layer small-stack wrapper regression. The script compiles the actual three C source files with C17 warnings as errors and runs Java with `-Xcheck:jni`; inputs are fingerprinted and checked for concurrent change.

This focused POM does not replace the owning collection POM or its source-qualified JUnit coordinate. The original owning Maven gate remains in the existing workflow and must pass before promotion. Its prerequisite builds the genuine checked-in JUnit source closure with `mvn -f m3/vendor/third_party/pom.xml -pl junit-jupiter-5.10.2,junit-platform-launcher-1.10.2 -am -DskipTests install`, following `m3/docs/a3-vendored-build-prerequisite-20261015.md`; it does not relabel public binaries as source-derived artifacts. Full JDK builds, JTreg, non-Linux native platforms, specialized deep sorted membership/range construction and performance acceptance remain separate gates. No throughput or allocation benchmark improvement is claimed. The source inventory marks unreviewed operations as pending; these repairs do not advance the global String, arrays, collections, AST receiving order.
