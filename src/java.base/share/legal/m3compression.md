M3 compression atoms (module org.m3jdk.compression)
Copyright 2026 Hitesh Soliwal and contributors

Relocated from first-party hsoliwal/com.synexia Apache-2.0 sources by the recipe
recipe/compression-20261106 (relocate.py, RELOCATION_MAP.tsv); every donor
commit, blob and sha256 is recorded in recipe/compression-20261106/PROVENANCE.tsv.
OpenJDK retains its existing licensing. Since 2026-11-08 the code lives in java.base as the internal,
unexported package jdk.internal.m3.compression (moved from the m3/compression module; recipe
make/m3/tooling/jdk-naming/move_area.py, evidence make/m3/evidence/compression). It does not replace
java.util.zip or route any codec.

Formats, not code

The encoders and decoders implement two published wire formats, used as the contract:
  LZ4 block format  https://github.com/lz4/lz4/blob/dev/doc/lz4_Block_format.md
  Snappy raw format https://github.com/google/snappy/blob/main/format_description.txt
No code of lz4-java, snappy-java, Apache Commons Compress, aircompressor, liblz4
or libsnappy is contained in or required by this module (token-similarity check
and decision in recipe/compression-20261106/FOSS_REUSE_DECISION.tsv). Those
libraries served only as independent reference encoders and decoders in a
scratch differential outside the module (GATES.tsv); none is a dependency.
