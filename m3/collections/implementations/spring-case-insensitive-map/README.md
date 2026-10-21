# Owned Spring case-insensitive map

`com.m3.collections.spring.M3LinkedCaseInsensitiveMap<V>` is a standalone
Java 21 `Map<String,V>` owner derived from the Spring 6.2.0 public policy.
It offers all four constructors, the complete mapped public instance surface,
seven inherited Map defaults and both protected hooks. It compiles with the
JDK alone. Locale capture and full Unicode `String.toLowerCase(locale)`,
original key spelling/order, live views, selected ghost/alias state, callback
effects, clone policies and object-graph serialization are qualified by the
unchanged finite fixtures in the companion qualification directory.

Each internal table owns key/value arrays and primitive bucket/hash/link/
position/state arrays. API entries are temporary uncached facades; no stored
Entry nodes or provider map bodies are used. Empty huge capacity hints allocate
lazily and retain the provider's observed constructor behavior on a fixed
64 MiB heap. No arbitrary public capacity cap was added.

The original protocol reports 196782 common clean checks over 4096 operations;
241 required policy records and eight required Entry records match the exact
provider. Seven Entry lifecycle/identity/private-wire diagnostics remain
separate and differ; they are not a parity claim. Fifteen frozen supplemental
caller-effect cases and 32 separate empty constructor calls also match. The
complete metadata proof covers four constructors, 34 nonbridge instance
methods, four compiler bridges, seven Map defaults and two protected hooks.

The owner has its own nominal and serialization identity. It is not assignable
to Spring's concrete class and does not promise identical wire bytes. Hook and
clone behavior follows the selected observed policy, including captured
original-owner coupling; this is not independent clone-hook semantics.

Lookup is O(1) on average and O(n) in the worst case for colliding Strings;
there are no JDK HashMap tree bins. Clone copies retained capacity arrays.
No speedup, universal allocation reduction, insertion-at-MAX or resource parity
has been measured or claimed. Optional JNI qualification projects primitive
masks over public views/snapshots through the existing kernel; the map's storage
and CRUD remain Java, with no native map ABI.

The pinned POM packages Java21 debug classes and exact LICENSE.txt, NOTICE.txt
and THIRD_PARTY_NOTICES.md resources under META-INF. Local Maven and hosted
Maven execution are separate from the recorded strict javac/JUnit proofs.
One owner does not complete the active full five-library collections goal.
