# Apache OpenNLP Tools 2.4.0 — full source

All 729 published Java sources are retained unchanged, including the CLI, trainers,
model readers/writers, classifiers, tokenizers, parsers, and feature generators.
`SOURCE_FILES.json` seals the original bytes. Models are application data; this
source intake does not include pretrained model packs or claim NLP accuracy.

Run the `synexia-openrewrite-recipes/crates/m3-nlp-search` recipe and full source
qualification to build the M3 versions of the tools. That build includes SLF4J's
real provider so CLI help and status work. No runtime dependency is downloaded.
The donor source and existing upstream namespaces remain authoritative API input.
