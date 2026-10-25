<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Provider stream bridges into interop (E5), 2026-10-24

The forks' stream-superset bridges move into `com.m3.collections.interop`. Their source is the
`m3/stream-interop` module of the guava, commons-collections and eclipse-collections fork masters and of the
spring-framework fork main (branch `m3/stream-superset-bridge-20261018`, all merged). They are
`M3GuavaStreams`, `M3ApacheStreams` (from `M3CommonsStreams`; token "apache" per naming R1), `M3EclipseStreams` and
`M3SpringStreams`, each with its test.

The change is recipe-first. `rewrite.yml` (`com.synexia.m3.interop.ProviderStreamsToInterop20261024`, OpenRewrite
ChangePackage/ChangeType) moves the packages and retargets the relocated Commons and Eclipse imports. They now point
to the donor packages that `interop/pom.xml` builds against. A second run over the output changes nothing (fixed
point). The two-line SPDX header is then replaced by the Synexia header.

Gates (local, JDK 21.0.9):
- interop main, 36 sources, `javac --release 21 -Xlint:all,-module -Werror` on core plus the pom's donor jars: 0
  warnings;
- the four new tests: 8/8.

The census successor `../current-census-superset-20261024` admits the eight files.
