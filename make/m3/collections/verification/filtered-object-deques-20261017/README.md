<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered object Deque contract

The current product owner is
`src/jdk.m3.collections/share/classes/jdk/m3/collections/FilteredDeques.java`.
Its stream dependency is the existing package-private `SlotSpliterators.java`
in the same directory. Both belong to the actual `jdk.m3.collections` product
module. The historical `m3/collections/src/main/java` paths and `M3FilteredDeques`
class spelling are not current production owners.

Run from a checkout with Java 21 on `PATH`:

```sh
bash m3/collections/verification/filtered-object-deques-20261017/verify.sh
```

The command compiles those two unchanged actual product owners with
`M3FilteredDequeContract.java`, Java 21 API selection, annotation processing
disabled, all lint warnings treated as errors, and an isolated source/class
path. It then executes the contract with assertions enabled. No generated stub,
private recipe, downloaded implementation or test-only replacement stands in
for either product owner.

The contract retains the original 40-seed, 800-operation-per-seed JDK-model
comparison and its original fixed cases. Additional cases cover empty and null
boundaries, a capacity-limited backing deque, iterator lookahead/removal and
predicate failure, callback counts, lazy stream binding, read-only mutation
refusal, aliased bulk insertion and writable reversed views. Hidden rows and
physical occurrence identity remain checked.

This is source-level Java 21 contract verification. It does not establish a
matched installed M3JDK21 image, full module/reactor build, jtreg, JNI/GC,
performance or complete provider parity. Runtime source and JNI symbols are
unchanged; the ordered receiving stage remains String qualification. Historical
`STATUS.tsv` and `COVERAGE.tsv` rows are retained as original receipt data, not
relabelled as current release acceptance.

## Historical receiving lineage

Original first-party Synexia source:
`synexia-primitives/src/main/java/com/synexia/primitives/M3FilteredDeques.java`.
Original receiving path:
`m3/collections/src/main/java/jdk/m3/collections/M3FilteredDeques.java`.
The historical source and output SHA-256 values were
`4861ecd5be1718dea49cb97daf1f0bfe6d057e762546e4143a598ba1b08b64ed` and
`6342d1acb4f406dc5b3c086d2db18966d7a5fc7f2b2a14c680ca520133165f0a`.
These identities describe that earlier receipt, not the current renamed files.

The [historical Synexia source link](https://github.com/hsoliwal/com.synexia/pull/12174)
and source revision `3307c77ae6ae6048d1665b27cddf1503fe3a1752` are preserved.
Canonical maintenance recipes and their correction memory remain in Synexia;
this target contains only the resulting verifier, product contract and documentation.
