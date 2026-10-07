# Nebula mastered-DAG receiver

Status: **evidence only; mechanical application blocked**.

Nebula is the proving target for the portable M3 Java-21 recipe/DAG shape. M3JDK21 does not treat
a branch, PR, recipe name, or historical test as proof. The machine receipt is:

`m3/synexia-import/nebula-mastered-dag-receiver.tsv`

The receiver is fail-closed. Mechanical application becomes admissible only when one exact receipt
binds all of these gates to the same Nebula proof commit:

- proof state `VERIFIED_GREEN`;
- recipe-first convergence `SUCCESS`;
- final transfer proof `SUCCESS`;
- semantic line coverage `LINE>=0.99`;
- second application fixed point `SUCCESS`;
- original Nebula Maven/Tycho build on the transformed candidate `SUCCESS`.

The current receipt binds Nebula PR #96 head
`75277c72c16c86ac66b11661661f2b5a22c9a9b4` and records the actual failed/pending state.
It therefore **cannot** authorize OpenJDK source mutation.

Once the exact Nebula receipt is green, the transferred DAG still enters M3JDK21 through the
existing target-owned control plane:

```text
Nebula proof receipt
 -> M3JDK21 inventory
 -> compatibility proof
 -> dependency closure
 -> exact file delta
 -> A3 FILE preparation
 -> source-sealed recipe crate
 -> recipe JUnit
 -> diff
 -> lint
 -> compile
 -> jtreg
 -> runtime
 -> serial promotion
```

The portable recipe/DAG never bypasses JDK compatibility, class-file/API/ABI, HotSpot/JNI, native,
jtreg, runtime, or licensing gates. Camel, Airflow and Drools/KIE remain projections/admission
mechanisms over the canonical dependency graph and gain no source-mutation or promotion authority.

Backport inventory continues independently while the Nebula proof is pending. Compatible JEP/non-JEP
candidates can be catalogued, dependency-closed and recipe-prepared; only the mechanical transfer
shortcut stays blocked.
