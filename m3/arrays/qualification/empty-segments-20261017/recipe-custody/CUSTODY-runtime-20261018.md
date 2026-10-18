<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Qualified successor in the existing public recipe host

This successor reuses the four existing public recipe engines and the original
SDK, sealing and receiving tests in this directory. Synexia remains the reusable
recipe owner. The new datasets and evidence are exact qualified source bytes;
the Maven POM exposes a bounded 768 MiB test heap. No recipe implementation or
runtime dependency is added to the JDK or array module.

The earlier `CUSTODY.md`, `OWNER_CUSTODY.json`, upstream `SOURCE.json` and their
reports remain evidence for their recorded predecessor. Their original POM
hash is retained in the successor metadata. Fresh qualification is required
for the capped host and each new dataset. A later publication receipt pins the
canonical successor commit; the handoff record itself does not assert that a
remote publication occurred.

## Replay from this directory

Use Java 21, Maven and Python 3. Source for all four engines, the tests and the
selected recipes is already present in this public checkout. Maven `-o` works
when the existing pinned OpenRewrite 8.17.1 and test dependencies are cached;
an empty dependency cache must first be populated. The explicit `sdk-owner`
override preserves the original POM's default owner-path contract.

```sh
mvn -o -B -ntp -Dm3.dataset=fixture \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-functional-doclint \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-native-feature-guard \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-object-vtable \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-progress-doclint \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-progress-scope-lint \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-runtime-control \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -o -B -ntp -Dm3.dataset=m3jdk21-strings-doclint \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
```

Every `-Pcontract verify` runs the original five real SDK tests, eight sealing
tests and nine isolated receiving cases. Fresh SDK outputs remain under
`target/<dataset>/candidate`. They are checked against exact preimages,
postimages, engine bytes and complete source bindings. Before receiving a
dataset into an isolated target, use the original `receive.py --check`, then
the original receiver with the same `--recipe-owner "$PWD/sdk-owner"` argument.
An already received target must be a zero-write fixed point. Runtime behavior
and JNI are qualified by the original owning-module gates separately.

Evidence logs and the size-agent manifest have `.txt` appended where needed
by the existing text recipe's path contract; their bytes are unchanged. This
handoff does not contain its own receiving recipe recursively. A separate
qualified canonical custody dataset receives these source/evidence files.

See `HANDOFF-runtime-20261018.json` for exact source and evidence hashes, the
public baseline, inherited controls, and the immutable datasets in this pass.
