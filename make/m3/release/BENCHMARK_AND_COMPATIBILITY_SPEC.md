# M3JDK21 benchmark and compatibility evidence specification

## Purpose

This specification prevents a correct implementation from being weakened by poor measurement and
prevents a promising result from being overstated.

## Baseline discipline

Every comparative result must bind:

- M3JDK21 candidate commit/tree;
- exact OpenJDK 21 baseline commit/build;
- source and binary checksums;
- machine and OS identity;
- JVM flags and environment;
- test/benchmark source revision.

Unless the experiment specifically studies a variable, all other relevant variables must be held
constant.

## Repetition and statistics

Use established harnesses such as JMH for microbenchmarks where applicable. Preserve warmup and
measurement configuration and all raw forks/iterations.

Do not publish a single best run as evidence. Report enough information to expose variance, such as
score and error or median plus useful percentiles. Preserve the raw result from which the summary
was computed.

## Required resource dimensions

A speed result alone is insufficient for M3. Record, when relevant:

- wall-clock throughput/latency;
- process and/or thread CPU time;
- allocation rate and total allocation;
- live/retained heap;
- RSS or native-memory tracking;
- GC frequency and pause/time contribution;
- startup and class-loading impact;
- generated-code or image size;
- precompute preparation cost;
- warm reusable-state cost.

A precompute win must include both **cold preparation** and **warm reuse**. Moving work out of the
timed region without accounting for it is not a valid improvement claim.

## M3 String compatibility matrix

Qualify the exact affected surface, including:

- Latin-1 and UTF-16 paths;
- supplementary characters and unpaired surrogates;
- empty and very large values;
- substring/range/view boundaries;
- equals, hashCode, compareTo and contentEquals;
- indexOf/lastIndexOf, startsWith/endsWith and regionMatches;
- case conversion and locale-sensitive boundaries where applicable;
- concat/join/repeat/replace;
- chars/codePoints;
- intern;
- serialization/reflection/JNI boundaries where applicable;
- exception type/timing and null behavior.

Run product configurations relevant to the change: interpreter, C1, C2, GC/lifetime modes and CDS
where affected.

## Regex evidence

Maintain at least 10,000 deterministic regex cases spanning:

- literal and escaped text;
- alternation and grouping;
- greedy/reluctant/possessive quantifiers;
- character classes and Unicode;
- anchors/boundaries;
- captures/backreferences;
- lookaround;
- flags/options;
- malformed patterns and expected failures;
- long and adversarial inputs within declared supported bounds.

For each precomputed artifact bind the exact pattern UTF-16 content, flags/options, version, owner
or snapshot identity, and any locale/Unicode/runtime assumptions. Test cache hit, miss,
invalidation, corruption, version mismatch and unauthorized/wrong-owner reuse.

## JNI/native evidence

For every native path preserve a Java semantic oracle and differential tests. Verify:

- symbols and generated header/ABI;
- encoding and bounds;
- exceptions;
- allocation failure;
- thread/lifetime behavior;
- GC/pinning/acquire-release behavior where applicable;
- fallback or explicit-unavailable behavior;
- parity across the supported input corpus.

JNI/native is an optimization/implementation boundary, not a permission to change Java semantics.

## Collections evidence

After String and arrays qualify, admit collections one concrete backend at a time. Preserve null,
equality/identity, order, views, iterator/spliterator behavior, serialization, subclass behavior,
JMM/concurrency/linearization/progress contracts as applicable.

Measure the intended M3 advantage directly: retained structural objects, allocation, CPU, heap,
native memory and traversal/search cost.

## Publishing rule

Every public quantitative claim must cite or link to a release proof artifact containing its raw
evidence and exact reproduction command. If that artifact is absent, publish the capability as
experimental/planned rather than as a measured win.
