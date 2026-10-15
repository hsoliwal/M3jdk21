# A3 backport completion fan-in

A3 backport completion is an evidence fan-in, not product-source promotion.

The full JDK 22-27 upstream commit inventory and Java-21 compatibility queue remain owned by
`m3/backports/inventory.py` and `compatibility_queue.py`. A3 owns the current-tree inventory and
FILE preparation frontier.

The canonical sequence is:

```text
OpenJDK 22..27 GA intervals
 -> complete upstream commit inventory
 -> compatibility queue
 -> current M3JDK21 src/test inventory
 -> A3Work per-file atoms
 -> A3Complete
      -> work.tsv
      -> java-preparation-paths.txt
      -> scope-holds.tsv
      -> receipt.tsv
 -> mastered atomize/patternize candidate execution under m3/build
 -> diff/lint/compile/test/runtime
 -> serial scope recomposition/promotion
```

`A3Complete` never writes `src/**` or `test/**`. Its outputs are restricted by `A3Fs` to
`m3/build/**`.

`java-preparation-paths.txt` contains only current Java files already classified by `A3Work` as
`A3_JAVA_ATOMIZE_PATTERNIZE`. It is suitable as the input list for a later mastered `A3 apply`
execution after a valid Synexia portable mastery receipt/root is supplied.

`scope-holds.tsv` retains one row per feature whose recomposition scope is broader than FILE.
File-local preparation therefore never masquerades as PACKAGE, MODULE, MULTI_MODULE or LIBRARY_API
approval.

The receipt is content-addressed and records zero product-write authority and zero promotion
authority.

One Maven/CI completion run builds the real queue from all six GA intervals, inventories the current
tree, creates the preparation frontier, and uploads the complete evidence bundle. Camel, Airflow and
Drools remain optional projections of the canonical M3 backport DAG; they do not own semantic
authority.


## Single Maven completion build

After `COMPATIBILITY_QUEUE.tsv` exists, the current-tree inventory and fan-in are lifecycle-bound:

```bash
mvn -B -ntp -f m3/tooling/a3/pom.xml \
  -Pm3-a3-backport-completion \
  -Dm3.a3.root="$PWD" \
  -Dm3.a3.queue=m3/build/backports/COMPATIBILITY_QUEUE.tsv \
  -Dm3.a3.completion.out=m3/build/a3/completion \
  verify
```

The profile runs inventory first and completion fan-in second in the same Maven `verify` phase.
