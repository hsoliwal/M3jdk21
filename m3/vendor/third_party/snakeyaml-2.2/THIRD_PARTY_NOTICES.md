# snakeyaml 2.2 source inclusion

snakeyaml is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as M3jdk21-original.

Upstream: https://bitbucket.org/snakeyaml/snakeyaml/src
Published sources jar: https://repo.maven.apache.org/maven2/org/yaml/snakeyaml/2.2/snakeyaml-2.2-sources.jar (SHA-1 575a7915b847393207642575c139177ac2b2ac23)
License as declared by the published pom: Apache License, Version 2.0

The complete upstream license text is retained in `LICENSE` (fetched from the upstream tag; see UPSTREAM_LICENSE.json) and packaged with the source-built JAR under META-INF/third-party/snakeyaml-2.2,
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header. The Maven build descriptor and integration metadata are M3jdk21 composition, licensed Apache-2.0; that license does not relicense the donor implementation.
Consumers use `org.yaml:snakeyaml:2.2-m3-source-1`, compiled from these files.
