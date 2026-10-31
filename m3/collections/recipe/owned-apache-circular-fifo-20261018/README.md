# Exact owned Apache circular FIFO receiving host

This host receives one production source: `m3/collections/implementations/apache-circular-fifo/src/main/java/jdk/m3/collections/apache/M3CircularFifoQueue.java`.
The manifest permits ABSENT -> the exact12743-byte postimage
`71c4a3a325a99526f67228f6f34429b6f21b27d0bbfa8a5a3e5799442e8e9514`, or that exact postimage at fixed point.
Fixtures, qualification records and SDK sources are outside the target set.

The unchanged canonical `M3HashPinnedJavaSnapshotRecipe` is copied into `engine-src`:
Git blob`907f176d32b55d78f13e3756a407f6877f25ca7d`,
SHA256`9dc65793cb1f4fb20866f99f4e5d0043d54734aa03cf0fe297c0cadbba2d1c37`,13805bytes.
Named YAML activation uses `com.synexia.m3.OwnedApacheCircularFifoQueue20261018`,
crate`owned-apache-circular-fifo`,`lst:false`. This is PlainText source custody using the existing engine.

Two JUnit methods cover8 successful scenario executions
(ABSENT/PRESENT x2input orders xwith/without unrelated source), with fresh marker-free parsing,
new named activation and fixed points after every execution.
Empty/singleton orders coincide; the matrix is not8unique input batches.
Four refused inputs cover drift, identical duplicates and both mixedduplicate orders.
Test observations require zero supplied-batch additions/replacements/deletions before refusal,
and unchanged source count/order/reference identities, paths, IDs, markers and bytes.
These assertions concern this single installer; they do not promise transactionality for arbitrary composed recipes.

The parent separately reports actual pinned Commons4.5 donor and ownedV2 each
1033638checks/95060operations with protocol digest
`d126cd959383dd63699f0df74a9935568e7e554a90c34034d11631c1e442842a`.
Those are runtime receipts, not execution of this receiving host.
Local Java21 compilation and the official JUnit Platform 1.10.2 receiving run passed both tests. Local Maven was not run to keep the existing C offline cache read-only; hosted Maven is a separate pending check.
No complete Apache/five-library, new queue JNI body, performance or installed-image admission is made here.

The POM reuses the completed Counted host's exact dependency/plugin versions.
It pins maven-resources-plugin3.3.1 and routes build outputs through `m3.recipe.output`.
Root performs local compilation/JUnit with the provisioned read-only cache and records hosted Maven separately.
A planned host command, after storage/output/cache gates, is:

```text
mvn -B -ntp -f recipe/pom.xml -Dm3.recipe.output=<verified-task-output> test
```

The literal queue template retains its ASF/source headers unchanged.
Root supplies the complete Apache attribution/license/provenance records separately.
FOSS_REUSE_DECISION.tsv records the receiving-host/helper reuse before this test/config adaptation.
