# rewrite-java-21 8.17.1 source inclusion

rewrite-java-21 is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as M3jdk21-original.

Upstream: https://github.com/openrewrite/rewrite
Published sources jar: https://repo.maven.apache.org/maven2/org/openrewrite/rewrite-java-21/8.17.1/rewrite-java-21-8.17.1-sources.jar (SHA-1 f133bd0ba96af8944d46df8997decc80fe01ef13)
License as declared by the published pom: The Apache Software License, Version 2.0

The complete upstream license text is retained in `LICENSE` (fetched from the upstream tag; see UPSTREAM_LICENSE.json) and packaged with the source-built JAR under META-INF/third-party/rewrite-java-21,
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header except the 2 files under src/main/java that Lombok uses: those are the Lombok 1.18.30 delombok expansion (with the upstream lombok.config kept as UPSTREAM_LOMBOK.config), each marked by a notice line, with the unchanged upstream file kept under upstream-lombok/. The Maven build descriptor and integration metadata are M3jdk21 composition, licensed Apache-2.0; that license does not relicense the donor implementation.
Consumers use `org.openrewrite:rewrite-java-21:8.17.1-m3-source-1`, compiled from these files.
