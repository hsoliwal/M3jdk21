# M3JDK21 static language and affect geometry

## Product invariant

This is M3JDK21 product state, not an "M3 + JDK" adapter layer.

The language/dictionary plane is overwhelmingly immutable. Runtime novelty is a temporary
composition over frozen coordinates:

```text
M3 String / M3LEX001 CWID
  -> lexical + morphology + concept + subject facts
  -> pair/relation geometry
  -> phrase
  -> sentence
  -> paragraph
  -> context
  -> reason/evidence/discussion/action
```

No context operation mutates the lexicon, word profile, concept graph or emotion ontology.

## M3StaticLanguageGeometry

Each CWID maps directly into primitive lanes:

```text
frequency[cwid]
classMask[cwid]
morphologyMask[cwid]

conceptOffsets[cwid..cwid+1] -> concepts[]
subjectOffsets[cwid..cwid+1] -> subjects[]

pairOffsets[cwid..cwid+1]
  -> pairTargets[]
  -> pairCounts[]
  -> pairAssociationQ20[]
```

There is no retained per-word metadata object, Map.Entry, boxed word id, copied String or
runtime-trained word object.

The builder is cold-path only and is discarded after freeze().

## M3LuhnSummarizer

The extractive summarizer consumes M3LEX001 word IDs and exclusive sentence-end coordinates.
It combines three fixed-point scores:

1. Luhn significance cluster: significantWords^2 / clusterSpan;
2. precomputed pair association;
3. broad-class + exact-concept + subject/domain cohesion.

The default significance band is derived from frozen corpus frequency. Consumers may provide a
prequalified band.

OpenNLP-style tokenization/sentence detection can be used at text ingress, but once input resolves
to CWIDs it is no longer the semantic authority.

## Java/JNI split

M3LanguageNative accelerates only the significance-cluster mechanics.

JNI receives primitive arrays: CWIDs, sentence ends, the frozen frequency lane, frequency band,
maximum gap and caller-owned score/significant/span outputs.

There are no predicate callbacks, semantic Java objects or model mutation across JNI.
The Java implementation remains the oracle and fallback.

## General relation mechanics versus DAG

M3Dag remains the acyclic execution/structural specialization.

Emotion, evidence, social and semantic relation graphs can contain cycles. They use
M3RelationStore, which intentionally reuses the same primitive relation shape and CSR indexes
without imposing acyclicity: from atom + relation-word coordinate + to atom + ordinal + metadata
+ interaction kind.

## Affect and emotion

M3EmotionOntology stores categorical emotion labels as canonical M3AtomDomain.EMOTION atoms.
Core affect dimensions are signed Q15; the hot four are valence, arousal, dominance and polarity.
Additional sparse axes include SenticNet-style sentics and appraisal dimensions. Missing evidence
remains missing; it is not silently treated as zero.

The built-in Plutchik-style categorical spine includes eight primary emotions, stronger/weaker
intensity topology, four primary opposite pairs and eight adjacent-primary blends. It deliberately
does not invent empirical VAD values.

The GoEmotions helper embeds only the 27 emotion labels plus neutral. It does not embed annotated
comment data.

M3EmotionProfile is the dynamic layer: sorted emotion IDs + unsigned Q16 contextual weights.
It can compute weighted dimensional coordinates while the ontology itself stays immutable.

## String interaction semantics

The #1541 atom/DAG owner remains authoritative:

- statement -> fact: evidence;
- statement -> logic/derive: logic;
- statement -> reason: reason;
- statement vs statement/opposition: argument;
- statement vs statement/exploration: brainstorm;
- value -> context/apply: context action;
- AST relations: structural;
- DAG relations: dependency;
- emotion participation: emotional context.

No semantic role is stored in java.lang.String or M3 String.

## Donor boundary

See static-language-affect-donor-catalogue-20261020.tsv. Restricted datasets remain external import
candidates until their exact version/license/provenance is reviewed.

## Acceptance

Required before promotion: strict Java 21 compile; pure Java language/affect proofs; C11 JNI build;
Java/JNI differential Luhn parity; recipe fixed point/refusal; full receiving M3JDK21 image/jtreg
when this plane is wired into an installed runtime; performance claims only after paired measurements.

The focused module proof does not by itself qualify HotSpot, GC, CDS, JVMTI or every platform.
