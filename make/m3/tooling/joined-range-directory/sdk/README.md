<!-- SPDX-License-Identifier: Apache-2.0 -->
# Joined range directory recipe

Pre-code admissionec4309cde9a5311347aab535b1cee4a2be442742. Reuse the existing JoinedM3StringPiece prefix
search atom rather than introduce another layout. The exact refinement extracts that
atom for indexed reads and nonempty range endpoints, then allocates/visits only
intersecting leaves. Empty ranges retain an empty joined view. Four immutable owner
sources are explicit KEEP inputs; all constructor, backing, encoding and bounds
contracts remain unchanged.

The unchanged OpenRewrite adapter1c1b1b568d369f688f6953161636bf36b484bf5a,
manifest38df5f97bbeec8a8a8cfc77091d56bda925c07f2 and exact POMd79867bcc09ad6329e12418e15907e54b8ab025f
drive three named tests:16 forward/inverse/fixed-point schedules,30 refusal schedules,
and2 CREATE_NEW actual directional outputs. Full inverse fixture bytes recover all
five source owners. The closed input module has34 files/33 non-self pins.

Qualify actual Maven XML/counters/stdout/classes/output bytes before application.
Then compile the complete original core module and preserve original build.sh
contracts and JVM modes against authentic inverse baseline and forward candidate.
Run identical range/content/bounds/backing corpus and matched allocation/timing
measurements; retain all controls and failures. A smaller temporary directory is
source evidence only until actual execution. No product, whole JDK, JNI, String
integration or performance/retention result is claimed by these recipe sources.
