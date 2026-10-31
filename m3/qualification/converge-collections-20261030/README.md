# Converge the collections into M3JDK (2026-10-30)

User directive: every private convergence repo and every collection fork must be optimized and converged into
M3JDK, under "absorb first": each class or shape lands as a first-party M3 owner, is proven by contract and differential
tests against its source, and only then is the copy retired.

Inventory, from git trees and a class-name match against M3JDK master (a first signal only):

| Source | Classes | Already in M3JDK by name | Gap |
|---|---:|---:|---:|
| com.synexia core (synexia-common/collections, synexia-primitives, synexia-mindex/collections, synexia-algo) | 473 | 38 | 435 |
| synexia-m3scale-apache2 | 316 | 0 | 316 |
| synexia-apps | 90 | 4 | 86 |
| synexia-platform | 15 | 1 | 14 |

Collection forks and their M3 deltas against upstream (FORK_DELTA.tsv): guava +55, eclipse-collections +34,
commons-collections +43, spring-framework +8, jackson-datatypes-collections +1, swift-collections 0. M3JDK already
carries relocated copies of several of them under m3/collections/implementations; the sanitize census
(THIRD_PARTY_INVENTORY.tsv, absorbing owner per donor file) is the work list for those.

Leaf order: hot families first (hash probe/insert/backward-shift delete, deque ring mask, sorted dense arrays, bit
sets, heaps), using the leetcodify recipe (com.synexia #14190) and the catalogue techniques.
