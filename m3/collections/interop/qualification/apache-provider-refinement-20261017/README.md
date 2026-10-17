# Apache provider contract refinement

This addition validates the existing two Apache Commons Collections 4.5.0
multimap factories against the actual provider binary. The production adapter
continues to compose M3LinkedHashMap, M3LinkedList and M3LinkedHashSet. These
owners retain array slots and indices; provider-required transient API entries
are allowed. This is not implementation coverage for all collection libraries.

The oracle now compares insertion order, duplicate/null handling, remove/reinsert,
MapIterator lifecycle and retained live views across clear/reinsert. Public
adapter interfaces are unchanged. The existing source-generating recipe remains
immutable; a separate hash-pinned successor refines only the contract file and
refuses unrelated bytes. Qualification verifies both generations' lineage.

The recipe host directly reuses the canonical Synexia Java and Text snapshot
engines, with their Apache-2.0 notices intact. Complete YAML activation covers
32 partial installation states and marker-free fresh-byte replay. The successor
also proves exact replacement, idempotence and drift/duplicate/missing refusals.
The workflow compiles interop into a separate output directory so module-info
from the core does not change Apache/JUnit classpath compilation visibility.

Executed on Windows with Java 21, actual commons-collections4:4.5.0 and cached
OpenRewrite:8.17.1/Jupiter:5.10.2: strict compilation of 80 inventoried M3 files;
strict adapter/oracle compilation; 389838 differential checks over 4600 randomized
operations plus deterministic traces; one Jupiter provider test; eight recipe
tests; and source-custody verification. No stubs were used. The 80-file local
inventory is identified in its separate catalogue; it is not asserted to be the
complete latest target branch source tree.

Linux GitHub Actions and independent review remain pending. No performance or
all-library coverage claim is made. The native refinement is a separate PR.
