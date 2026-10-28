# Empty natural-order lookup admission

The array AVL map now rejects null/non-Comparable lookup keys without calling
`compareTo` when its unbounded natural-order storage is empty. This also covers
its unbounded descending view. Fresh, cleared and removed-to-empty maps retain
their absent results, even when a valid Comparable would throw on comparison.
The production change is two added/two removed lines in `M3TreeMap.findSlot`;
public interfaces, arrays, slot links, heights and stored-object layout are unchanged.

Validation: strict Java 21 compilation; unchanged 88 API rows; frozen 132 entry and
113 bulk cases; 5,040 AVL permutations, 98,304 random operations, 1,438,195 checks,
238,593 invariant calls. Four normal module Jupiter tests passed (compiled against
the declared 5.10.2-m3-source-1 API; executed by the provisioned 1.11.4 console).
The full Maven reactor and hosted CI were not executed by this qualification.

The source-aware orchestrator froze the 640-case JDK-only original before editing
production. Separate opaque baseline execution and an isolated source-blind
candidate validator retained complete output, raw exceptions, callback sequences,
identities and map state. 144 previously different empty-natural cases now match;
630/640 complete candidate records match JDK. All 400 control records are unchanged
from the prior M3 baseline. Ten nonempty `getOrDefault` callback-count differences
remain inherited and visible; strict full-corpus admission failed control clause 4.
No new differences were introduced. Do not interpret the scoped qualification
or clean single Codex source review as universal JDK/EC contract equivalence.

Run the normal regression with Java 21 through the existing module:
`mvn -f m3/collections/pom.xml -Dtest=M3TreeMapEmptyLookupTest test`.
That command is a reproduction instruction, not a claim of an executed Maven run.
For the public observation fixture, compile both supplied Java files with
`javac --release21 -d <fixture-classes> CanonicalPutAllCalibration3.java EmptyRootLookupOracle.java`.
Run `EmptyRootLookupOracle java.util.TreeMap` with the fixture directory on the
classpath, `-DexpectedJavaHome=<Java 21-home>` and
`-DexpectedProviderOrigin=jrt:/java.base`. For M3, add the built core classes and
use `com.m3.collections.M3TreeMap` plus its exact `file:/.../classes/` code-source URL.
The existing calibration class is reused unchanged for its JSON formatting only.

The recipe retains exact pre/post SHA256 and Git blobs, occurrence cardinality,
fixed-point, inverse and unknown-preimage refusal proof. Evaluation category:
`ordered-map/empty-key-admission`. ADAPT the existing M3 atom; JDK 21 original and
[OpenJDK reference](https://github.com/openjdk/jdk21u/blob/98dbb49926d8e911b0713cefd9a2fc40955fa7f7/src/java.base/share/classes/java/util/TreeMap.java)
establish comparison admission. No OpenJDK source was imported; no throughput or
peak-performance claim is made. Owned sources retain Apache-2.0 headers.

This is a reviewable superset addition. Other five-library owners, the EC retained
iterator lifecycle/current 14 build and JNI work remain open.

The recorded controller retains the exact qualification workspace bindings; the JSON recipe describes the reusable file transformation independently.
