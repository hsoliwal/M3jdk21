<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact recipe-provider custody

These four sources are unchanged copies of the existing canonical
`com.synexia.rewrite` providers. [SOURCES.json](SOURCES.json) identifies their
canonical paths, revision, Git blob IDs and SHA-256 digests. Their package names,
interfaces and behavior are unchanged.

The receiving repository already contains older global providers. The seven
authoring POMs in this packet compile these exact, task-local sources so that the
publicly mirrored recipe can reproduce its qualification independently of those
older files. This packet does not replace or modify the incumbent global recipe
providers. The canonical implementation remains their owner; this directory is
source custody, with no separate implementation or recipe engine.

Run `python3 integration/qualify_provider_custody.py --help` from the packet root
for the source-integrity and clean Maven compilation gate. The original recipe
SDK tests continue to verify actual transformation, refusal and fixed-point
behavior. Moving their unchanged provider inputs does not change any production
postimage.
