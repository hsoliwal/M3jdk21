<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3IndexedLongList: primitive order-statistics AVL receiving receipt

- **Target owner:** `m3/collections/src/main/java/com/m3/collections/M3IndexedLongList.java` (M3-specific API, not `java.util.List`).
- **Target base:** `hsoliwal/M3jdk21@669a949d192345acefcfc5e9bb0b3724174d270c`.
- **Original Git blob:** `bfac8db258c5feee9b1da242f778b519a5594887`; original SHA-256 `6ed47b65608c3c5daa2e2589045170ab6ec1b7248f6dad0635cb0eed93fee248`; original eight operations threw `M3NotYetReceived`.
- **Canonical convergence and recipe:** `hsoliwal/com.synexia@c8fb27b37e688903ddff6db1e1242ae270e57520`, named `com.synexia.m3.M3IndexedLongListAvl20261029` through the existing `M3HashPinnedJavaSnapshotRecipe`. Public source and tests are **exact template postimages**; recipe implementation/templates remain private.
- **Product Java postimage Git blob:** `885d7c8edbc04c2b3ad51b3591106637de4a733e`; SHA-256 `2f5e4e5482765dc86cc6ef1655eb538d5da4a6d3bf4538a7b23954a8793a40e1`.
- **Product JUnit source blob:** `34420b7f487fd18a91654397fcd1f5e927a2c407`; SHA-256 `0dbad80366bf272069f60ae4cb7d115722e40b5879bbe80c7a65cdc40d3e9fc2`.

## Category and borrowing

The design compares Apache Commons Collections `TreeList` (Apache-2.0, positional AVL) and the existing M3 array-based collection family. It adapts the **algorithm**, rather than copying the Commons implementation: one `long[]` value lane; `int[]` left/right/size lanes; `byte[]` height facts; `long[]` occupancy/balance flags; one integer free-list. No retained per-element node objects, no boxed `Long` values, no new JNI dependencies. Java 21 `ArrayList<Long>` is the independent behavioral oracle for the M3-only sequence's shared positional operations.

## Public contract

Preserve the original constructor, `scaffoldDescriptor()` string, and `get(int)`, `add(int,long)`, `add(long)`, `set(int,long)`, `removeAt(int)`, `indexOf(long)`, `size()`, `clear()` descriptors. Operations now implement their documented behavior. Default JDK21 public classes and entry points are unchanged. **One-way superset only:** M3JDK21 runs conforming JDK21 code; standard OpenJDK need not contain or understand M3 extensions.

## Executed proof and open gates

- **PASS, isolated Java 21:** generated source compiled with Java 21; 40,000 deterministic mixed list operations were compared with `ArrayList<Long>`; 100,000 ordered insert/delete operations checked sequence, balancing and slot reuse.
- **PASS, isolated assertion harness:** the four JUnit-source methods were compiled and executed using throwing local assertion stubs. This is **not** a successful Maven Surefire/JUnit execution.
- **NOT EXECUTED:** the full private OpenRewrite Maven recipe test, real JUnit Jupiter against the public `m3/collections` reactor, JNI parity where applicable, product JDK image/jtreg, cross-platform builds, release acceptance or comparative CPU/heap benchmarks. Github Actions status needs independent verification.

Candidate optimizations cannot be represented as complete Java 21 runtime acceptance or guaranteed performance wins. Historical preservation, licensing and compiler/run gates remain mandatory before merging.
