<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-preserving correction of stacked Set/Map CI failure

Previous Java 21 [priority stack run](https://github.com/hsoliwal/M3jdk21/actions/runs/38890172102)
failed compiling the two earlier Set/Map source files, *before* reaching
the independent PriorityQueue test. The duplicated signatures were generated
in the earlier Synexia recipe postimages; the canonical corrected source is
preserved in the [Synexia recipe #11870](https://github.com/hsoliwal/com.synexia/pull/11870)
and received into [M3JDK21 #974](https://github.com/hsoliwal/M3jdk21/pull/974).

This branch follows source-first governance: replace the two target files
only with those exact qualified recipe templates from canonical Synexia
commit `6e304478489f103f41d3d2ef23c0717474e4181f`, and update the
SHA-256 receipt. All existing primitive List/Deque/Set/Map/PriorityQueue
algorithms, signatures and ABI behavior stay as previously designed.

This repair itself is not a declaration of test or installed-JDK success.
