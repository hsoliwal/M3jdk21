# DB private atom pass

Four cohesive blocks were extracted into private operations of the existing
store/codec. No public API, dependency, POM, workflow, persisted format or
coverage threshold changes. All51 existing database JUnit tests are byte-identical.
Ten new private-atom/fault tests and one additional recipe test are supplied.
The recovery, parser and build recipe afterimages are synchronized, and DbAtom
uses the existing exact-snapshot engine. Historical beforeimages remain retained.

Executed locally: static/config/manifest checks; strict compilation of all8 DB
main sources and dependency-free probes on Java21; identical public/protected
API descriptors; byte-identical wire results for three path/Unicode fixtures;
four unchanged bounded corruption probes; size/move/text/edge fault probes;
and all three missing-SHA failure contracts in a separate32MB child JVM.
The initial combined environment experiment incorrectly used ZIP/temp-file
operations in the no-provider JVM and failed at ZIP close due unavailable RNG.
The published digest probe deliberately avoids those unrelated operations and
passes. ZIP movement runs separately with the normal provider configuration.

Actual JUnit/OpenRewrite and JaCoCo execution of this candidate remain pending.
The local environment has no Maven executable; the attempted Maven Central
network access failed. No fake dependencies or inferred coverage replace the
hosted gates. The existing workflow must report DB verify and focused recipe
execution independently. The earlier51-test run measured663/681 lines and
235/241 branches, not a99percent pass.

This is a bounded DB proof pass, not transactional SQL, Java source parsing,
modified-JDK build, JNI parity, jtreg completion or exhaustive backport delivery.
No master/develop write, merge, squash, rebase or force-push is performed.
