<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File binary content fingerprint contract

This candidate adds an observation operation to the existing
`com.m3.file.M3File` NIO owner. Existing constructors, metadata, channel, stream,
and bounded byte-read behavior remain unchanged. The original provider-owned
Path and existing regular-file / nofollow-open checks remain authoritative.

## New operation

`fingerprint(long maxBytes)` consumes the original owner's stream and returns
`Fingerprint(long size, String sha256)`. The descriptor retains only the number
of bytes actually consumed and the canonical lowercase hexadecimal SHA-256 of
those bytes. It retains no byte array, stream, channel, file descriptor, Path,
timestamp, class parser, AST, or payload cache.

The maximum accepted length is inclusive. Negative limits are rejected before
opening the file. Empty files accept a zero limit. An input longer than the
limit is rejected by observing at most one excess byte. The remaining-byte
arithmetic must not overflow at `Long.MAX_VALUE`. The operation uses one fixed
8 KiB byte buffer, checks actual streamed length, and closes the stream on both
success and failure. Missing files, directories, and symbolic-link file inputs
are rejected through the existing NIO owner. A positive-length read that makes
no progress is rejected instead of spinning indefinitely.

Every call reads content. File size and modification time never authorize
fingerprint reuse. Edits preserving length and modification time therefore
produce a different digest when their actual content differs.

## Evidence boundary

The returned fingerprint describes the bytes observed during that read. It
does not certify an atomic version of an externally mutable file, detect all
concurrent edits, or claim that file content remains unchanged after the call.
Ancestor symlink / directory replacement behavior remains that of the existing
NIO owner. Compiler input reuse must still consume the captured immutable input
and validate its complete context.

Digest equality is a cryptographic content-identity signal, not an ABI or
semantic-equivalence proof. Class-file ABI analysis, dependency propagation,
task scheduling, output validation, immutable payload custody, and successful
build publication remain with their existing owners. This operation is suitable
for raw `.class`, JAR, and resource files without decoding them as text.

## Required finite checks

Use real javac-generated class files and an independent full-byte SHA-256 oracle;
exercise empty, exact, over-limit and negative limits; binary NUL and multi-block
inputs; edits preserving file length and modification time; missing, directory,
and symlink refusal; metadata-only descriptor shape; canonical digest validation;
and all original M3File entry points after the additive change. These checks do
not establish complete Maven/Gradle integration, atomic filesystem snapshots,
exhaustive cross-provider behavior, or measured performance gains.
