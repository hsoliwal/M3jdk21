#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

FILES = {
    "regex": ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3RegexAutomaton.java",
    "fst": ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3TermFst.java",
    "program": ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3ReasoningProgram.java",
    "closure": ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3ReasoningClosure.java",
    "hash": ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3PrecomputeHash.java",
    "mapping": ROOT / "m3/docs/name-mapping.json",
    "invariants": ROOT / "m3/docs/m3-runtime-invariants.tsv",
    "port": ROOT / "m3/docs/synexia-string-precompute-port-map.tsv",
    "test": ROOT / "test/jdk/jdk/internal/mindex/M3OwnedPrecomputeTest.java",
}

def fail(message: str) -> None:
    raise SystemExit("M3_OWNED_PRECOMPUTE_INVARIANT_FAIL " + message)

texts = {}
for key, path in FILES.items():
    if not path.is_file():
        fail(f"missing {path}")
    texts[key] = path.read_text(encoding="utf-8")

runtime = "\n".join(texts[key] for key in ("regex", "fst", "program", "closure", "hash"))

for forbidden in (
    "com.google.re2j",
    "org.apache.lucene",
    "org.tweetyproject",
    "net.sf.tweety",
):
    if forbidden in runtime:
        fail(f"third-party runtime dependency leaked: {forbidden}")

for donor_runtime_name in (
    "MIndexRegexAutomaton",
    "MIndexTermFst",
    "MIndexReasoningProgram",
    "MIndexReasoningClosure",
):
    if donor_runtime_name in runtime:
        fail(f"Synexia donor class name leaked into M3 runtime: {donor_runtime_name}")

required_regex = (
    "class M3RegexAutomaton",
    "MAX_NFA_STATES",
    "MAX_DFA_STATES",
    "determinize(",
    "prefixCanStillMatch(",
    "backreferences are not supported",
)
for fragment in required_regex:
    if fragment not in texts["regex"]:
        fail(f"regex invariant missing: {fragment}")

required_fst = (
    "class M3TermFst",
    "MAX_TERM_UNITS",
    "minimize(",
    "longestPrefixLength(",
    "hasPrefix(",
)
for fragment in required_fst:
    if fragment not in texts["fst"]:
        fail(f"term-FST invariant missing: {fragment}")

required_reasoning = (
    "class M3ReasoningProgram",
    "rule ",
    "support ",
    "attack ",
    "MAX_SYMBOLS",
    "MAX_RULES",
)
for fragment in required_reasoning:
    if fragment not in texts["program"]:
        fail(f"reasoning program invariant missing: {fragment}")

for fragment in (
    "class M3ReasoningClosure",
    "least-fixed-point",
    "inconsistent(",
    "proofRule(",
):
    if fragment not in texts["closure"]:
        fail(f"reasoning closure invariant missing: {fragment}")

mapping = texts["mapping"]
for source, target in (
    ("com.synexia.precompute.MIndexRegexAutomaton", "jdk.internal.mindex.M3RegexAutomaton"),
    ("com.synexia.precompute.MIndexTermFst", "jdk.internal.mindex.M3TermFst"),
    ("com.synexia.precompute.MIndexReasoningProgram", "jdk.internal.mindex.M3ReasoningProgram"),
    ("com.synexia.precompute.MIndexReasoningClosure", "jdk.internal.mindex.M3ReasoningClosure"),
):
    if source not in mapping or target not in mapping:
        fail(f"name mapping missing: {source} -> {target}")

for law in (
    "SYNEXIA_OWNED_IMPL_IS_PORT_AUTHORITY",
    "NO_RE2J_LUCENE_TWEETY_RUNTIME_DEPENDENCY",
    "M3_REGEX_AUTOMATON_IS_INTERNAL_FINITE_ENGINE",
    "M3_TERM_FST_IS_INTERNAL_OWNED_IMAGE",
    "M3_REASONING_LANGUAGE_IS_OWNED",
):
    if law not in texts["invariants"]:
        fail(f"runtime invariant missing: {law}")

if "M3OwnedPrecomputeTest" not in texts["test"]:
    fail("jtreg proof missing")

print("M3_OWNED_PRECOMPUTE_INVARIANTS_PASS")
