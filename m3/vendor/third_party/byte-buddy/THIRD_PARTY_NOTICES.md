# byte-buddy 1.14.11 source inclusion

byte-buddy is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as M3jdk21-original.

Upstream: https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy/1.14.11/
Published sources jar: https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy-dep/1.14.11/byte-buddy-dep-1.14.11-sources.jar (SHA-1 2590c8c1573731296f7cc796285c0281e7f151eb) - the upstream source module of net.bytebuddy:byte-buddy:1.14.11
License as declared by the published pom: Apache License, Version 2.0 (from the license text; pom declares none)

The complete upstream license text is retained in `LICENSE` (from the sources jar) and packaged with the source-built JAR under META-INF/third-party/byte-buddy,
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header. The Maven build descriptor and integration metadata are M3jdk21 composition, licensed Apache-2.0; that license does not relicense the donor implementation.
Consumers use `net.bytebuddy:byte-buddy:1.14.11-m3-source-1`, compiled from these files.
The jar is assembled the way upstream assembles it (https://github.com/raphw/byte-buddy/blob/byte-buddy-1.14.11/byte-buddy/pom.xml (sha256 f779579ac9fd188247678234b2cfb3a3d2cd51fb5a60fa32575c258d96507791), profile shade-current): the shade step relocates org.objectweb.asm to net.bytebuddy.jar.asm from the in-repo source modules asm-9.6, asm-commons-9.6, which keep their own upstream licenses and notices. No relocated copy of those sources is carried here.
