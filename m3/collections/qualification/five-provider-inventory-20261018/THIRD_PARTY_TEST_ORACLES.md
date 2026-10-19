<!-- SPDX-License-Identifier: Apache-2.0 -->
# Superset differential-test oracles (test scope only)

These libraries are used ONLY as `<scope>test</scope>` differential oracles for the
`M3Superset*` classes. No source or bytecode from them is copied, adapted, or shipped;
the produced `m3-collections` artifact still depends on `java.base` only.

| Oracle | Coordinates | License | Role |
|--------|-------------|---------|------|
| Guava | com.google.guava:guava:33.4.0-jre | Apache-2.0 | ArrayListMultimap / HashMultiset / HashBiMap behavior oracle |
| Apache Commons Collections | org.apache.commons:commons-collections4:4.5.0 | Apache-2.0 | HashBag / MultiValuedMap cross-check oracle |
| Spring Framework | org.springframework:spring-core:6.2.0 | Apache-2.0 | LinkedMultiValueMap behavior oracle |
| JDK java.util | java.base (runtime link) | GPL-2.0 WITH Classpath-exception-2.0 | HashMap oracle; never copied |

Eclipse Collections (EPL-1.0/EDL-1.0) is deliberately absent: pattern/evidence only per
`FOSS_REUSE_DECISION.tsv`; it is neither copied nor added as a dependency in any scope.
