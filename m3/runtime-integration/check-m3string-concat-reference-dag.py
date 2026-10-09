#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
helper = (ROOT / "src/java.base/share/classes/java/lang/StringConcatHelper.java").read_text(encoding="utf-8")
pool = (ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java").read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")
tuple_source = (ROOT / "src/java.base/share/classes/java/lang/M3StringTuple.java").read_text(
    encoding="utf-8"
)
canonical_test = (
    ROOT
    / "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
    "jdk21-hash-pinned/jdk22-m3-string-canonical-dag-master-repair/"
    "02-M3StringCanonicalDagTest.java.txt"
).read_text(encoding="utf-8")

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_STRING_CANONICAL_DAG_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


start = helper.find("static String m3Concat(String[] constants, Object[] args)")
if start < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|missing m3Concat")
end = helper.find("\n    /**", start)
if end < 0:
    raise SystemExit("M3_STRING_CONCAT_RECEIVER_FAIL|cannot bound m3Concat")
body = helper[start:end]

for fragment in [
    "M3String result = M3String.empty();",
    "String argument = stringOf(args[index]);",
    "M3String.canonicalize(constant)",
    "M3String.canonicalize(argument)",
    "result.concat(piece)",
    "return new String(result);",
]:
    require("typed-concat:" + fragment, body, fragment)

for fragment in [
    "String[] pieces",
    "new String[args.length * 2 + 1]",
    'String.join("", "", "", pieces',
    "ArrayList<",
]:
    if fragment in body:
        raise SystemExit(f"M3_STRING_CONCAT_RECEIVER_FAIL|staging={fragment}")
    checks.append("no-staging:" + fragment)

require("stringOf-authority", helper, "static String stringOf(Object value)")
require(
    "M3String-concat-owner",
    m3,
    'return M3StringPool.concat(this, Objects.requireNonNull(other, "other"));',
)
for fragment in [
    "private static M3String concatBalanced(M3String left, M3String right)",
    "return balance(branch.left, concatBalanced(branch.right, right));",
    "return balance(concatBalanced(left, branch.left), branch.right);",
    "if (left.length() == 0) return right;",
    "if (right.length() == 0) return left;",
    "if (left.owner() == right.owner() && left.end() == right.start())",
    "sameLeafSequence(M3String.whole(existing), left, right)",
]:
    require("pool:" + fragment, pool, fragment)

for fragment in [
    "final M3String left;",
    "final M3String right;",
    "final int height;",
    "final long canonicalId;",
    "Math.addExact(left.length(), right.length())",
    "(byte) (left.coder() | right.coder())",
    "left.hashCodeValue() * M3String.pow31(right.length()) + right.hashCodeValue()",
    "this.height = 1 + Math.max(childHeight(left), childHeight(right));",
    "M3StringFacts.compose(left.facts(), right.facts())",
]:
    require("tuple:" + fragment, tuple_source, fragment)

for fragment in [
    "concat parenthesization must not define canonical tuple identity",
    "adjacent ranges of one canonical atom normalize across tuple boundaries",
    "candidate hash collisions require exact coordinate verification",
    "4096-piece chain must stay balanced",
    'System.out.println("M3_STRING_CANONICAL_DAG_PASS checks=" + checks + " height=" + height);',
]:
    require("canonical-test:" + fragment, canonical_test, fragment)

print("M3_STRING_CONCAT_REFERENCE_DAG_PASS|no_staging=true|balanced_owner=true")
print(f"M3_STRING_CANONICAL_DAG_SOURCE_PASS checks={len(checks)}")
