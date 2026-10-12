# re2j 1.8 source inclusion

re2j is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as M3jdk21-original.

Upstream: https://github.com/google/re2j.git
Published sources jar: https://repo.maven.apache.org/maven2/com/google/re2j/re2j/1.8/re2j-1.8-sources.jar (SHA-1 e943b75d6f46a37dad72e881127557a595439932)
License as declared by the published pom: Go License

The complete upstream license text is retained in `LICENSE` (fetched from the upstream tag; see UPSTREAM_LICENSE.json) and packaged with the source-built JAR under META-INF/third-party/re2j,
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header. The Maven build descriptor and integration metadata are M3jdk21 composition, licensed Apache-2.0; that license does not relicense the donor implementation.
Consumers use `com.google.re2j:re2j:1.8-m3-source-1`, compiled from these files.
