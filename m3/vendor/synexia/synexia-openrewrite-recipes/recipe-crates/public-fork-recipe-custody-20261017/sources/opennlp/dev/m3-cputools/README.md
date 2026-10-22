<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# M3 composite-text receiver: hsoliwal/opennlp

Canonical recipe ownership: `hsoliwal/com.synexia`, PR11556. This receiving
pass continues PR9; it is not whole-project or whole-CPUTOOLS completion.

The original Maven build remains the product authority. The following Maven
entrypoint checks the exact postimage of `com.synexia.m3.OpenNlpRuntimePass` through
the existing Synexia manifest engine, not a duplicate transformer:

```sh
mvn -f dev/m3-cputools/pom.xml -Dsynexia.recipe.home=/path/to/com.synexia verify
```

This control check is not a product build or runtime proof. For explicit serial application:

```sh
python3 /path/to/com.synexia/synexia-openrewrite-recipes/recipes/cputools-runtime-20261015/verify.py --kind opennlp --target /path/to/opennlp --apply
```

The earlier `cputools-native-20261014` recipe is historical; its exact old postimage
is deliberately not the current postimage. Apply the initial recipe then this subsequent
pass on their source-pinned preimages; do not weaken a hash guard to admit foreign changes.
Fix the canonical payload, qualify, and rerun instead of hand-patching receiving output.
Existing String APIs, immutable caller-owned text, and original whitespace/UTF-16 behavior
remain authoritative. No alternate interner or runtime payload store is introduced.

## Runtime gates

The original Java CI stopped in RAT at this README before compiling any module.
This pass adds a recognized Apache header instead of suppressing the license check.
`M3TokenizerDifferentialTest` compares against `M3BaselineSimpleTokenizer`, a test-only
frozen original with only class identifiers renamed. It covers all UTF-16 code units,
both newline settings, random composites, cancellation and concurrent readers.
Actual OpenNLP/JUnit test reports remain the runtime authority. Source-seal replay alone
is not proof that tests ran. This is span tokenization, not POS, NER, OCR or model inference.
