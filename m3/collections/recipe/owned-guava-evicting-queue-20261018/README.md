# Exact owned Guava evicting queue receiving host

This host receives one production source:
m3/collections/implementations/guava-evicting-queue/src/main/java/com/m3/collections/guava/M3EvictingQueue.java.
The manifest permits ABSENT -> the exact 16979-byte postimage
12e10948c33dedf38961027f23c01a3a912889d2585dfe8181f35ecb43426dfb, or that exact postimage at fixed point.
Runtime fixtures, qualification records, docs and SDK sources are outside the target set.

The canonical M3HashPinnedJavaSnapshotRecipe is copied byte-identically into engine-src:
Git blob 907f176d32b55d78f13e3756a407f6877f25ca7d,
SHA256 9dc65793cb1f4fb20866f99f4e5d0043d54734aa03cf0fe297c0cadbba2d1c37, 13805 bytes.
Named YAML activation uses com.synexia.m3.OwnedGuavaEvictingQueue20261018,
crate owned-guava-evicting-queue, lst:false. PlainText execution supplies source custody.
It does not supply Java type checking, provider type assignability or runtime behavior proof.

The two prepared JUnit methods cover eight successful scenario executions:
ABSENT/PRESENT x two input orders x with/without unrelated source.
There are four presence maps and five unique ordered path/text input batches; empty/singleton orders coincide.
Each execution reparses delivered bytes into fresh marker-free PlainText inputs, activates a NEW
named recipe with a NEW canonical engine instance, and checks its fixed point.
Four refused inputs cover drift, identical duplicates and both mixed duplicate orders.
The borrowed observer requires no supplied-batch additions/replacements/deletions before refusal,
and retains provided source count/order/references, IDs, markers, paths and printed UTF8 bytes.
These observations apply to this single installer, without a transactionality promise for arbitrary compositions.

Receiving tests, local compilation/JUnit, Maven and hosted CI remain pending for this host.
Root owns those executions and the separate owner runtime/provider receipts.
No provider-class assignability, complete Guava/five-library coverage, queue JNI body,
performance or installed-image admission is made by these prepared source files.

The POM reuses the completed FIFO host: OpenRewrite8.17.1, Jupiter5.10.2,
maven-resources-plugin3.3.1, compiler3.13.0 and Surefire3.2.5.
It routes build outputs through m3.recipe.output and compiles only the existing canonical engine.
After storage/cache/output gates, the planned receiving command is:

```text
mvn -B -ntp -f recipe/pom.xml -Dm3.recipe.output=<verified-task-output> test
```

The literal owner template retains its exact source/license headers.
Root supplies complete Guava attribution and provenance separately.
FOSS_RECEIVING_DECISION.tsv records both receiving-host/observer reuse decisions before adaptation.

This unpublished installer now seals the private serialization V2 source. Its canonical engine and scenario/refusal logic are unchanged; only owner bytes and exact hash/length constants changed. The original V1 host is preserved in the local before-fix receipt. Fresh V2 execution is qualified separately by root.
