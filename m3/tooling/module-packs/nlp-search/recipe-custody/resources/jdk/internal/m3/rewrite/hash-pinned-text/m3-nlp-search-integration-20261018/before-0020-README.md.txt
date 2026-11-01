<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->
# Full OpenNLP and Lucene tool receiver

This builds the actual tool implementations and packages eight JMODs. Original
public APIs/module names remain intact. The canonical naming policy/map stays in
`m3/docs/name-mapping.json`; String runtime qualification remains the active phase.

The complete OpenNLP source is in `m3/vendor/third_party/opennlp-tools`. Existing
Lucene and SLF4J vendor source is reused. The two exact recipe outputs under
`m3/vendor/third_party-m3/nlp-search` replace internal owner files during the full
source build. They are not runtime adapters. The canonical recipes and tests live
in Synexia's `synexia-openrewrite-recipes/crates/m3-nlp-search`; `SOURCE_REF.json`
pins that handoff. The small source-build mirror retains its canonical source
identity and all locks. Improvements return to Synexia.

```sh
python3 m3/tooling/module-packs/nlp-search/build.py \
  --jdk-home "$JAVA_HOME" --out build/m3-nlp-search
build/m3-nlp-search/pack/image/bin/java \
  -m org.apache.opennlp.tools/opennlp.tools.cmdline.CLI
```

Linux Java 21 source/image qualification passed locally, including CLI, model
training/reload and disk-backed search. No Synexia runtime classpath, service or
package download is required. Full JDK build/jtreg, full upstream suites, other
platforms and representative performance remain pending. The existing mixed
literal String receiver CI failure remains open; no phase advancement is implied.
Apache/embedded donor notices and SLF4J MIT notices are retained per file.
