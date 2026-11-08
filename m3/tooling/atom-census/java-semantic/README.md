<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Java and Java-native declaration semantic census

This JDK21 tooling leaf is additive to `M3RepositoryFileCensus`: it parses exact committed Java Git blobs with
`com.sun.source` and records explicit class, constructor, method, field, initializer, lambda, module and Java
`native` declarations. It does **not** alter standard Java/JDK public APIs, the VM, the JNI ABI or the baseline
four-file `FILE_CONTAINER` output.

Run after the committed-file census baseline has been received and independently verified:

```sh
java -cp <compiled-atom-census-classes> jdk.m3.tooling.atoms.repository.JavaSemanticCensus \
    <repository-path> <commit-or-revision> <output-directory>
```

The output is `JAVA_ATOMS.tsv`, `JAVA_EVIDENCE.tsv`, `JAVA_PARSE_STATUS.tsv`, and
`JAVA_SEMANTIC_RECEIPT.tsv`. A parse failure is recorded as `BLOCKED`, never discarded; the receipt always records
`admission=HOLD`. Only AST inventory and exact source-slice identity receive a `PASS`. Every missing contract,
pattern, JIT, JNI native ABI, runtime and benchmark gate stays `NOT_RUN`. An extracted native declaration is **not**
a verified exported JNI symbol. Only the actual receiver JDK21 product tests may close those gates.

Lexical signatures and occurrence IDs track source atoms within an exact tree. A move, split, merge or replacement
requires an explicit reviewed identity transition: name similarity is not evidence of equivalence. Other source
families (C/C++, HotSpot `.ad`, assembly, build, script and resources) retain their separate open atomization duties.
