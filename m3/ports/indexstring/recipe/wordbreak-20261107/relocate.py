"""relocate_ix2.py <donor-src-dir> <out-dir> : INDEXSTRING_RELOCATE_IX2 v1.
Relocates the indexstring vocabulary tree and word-break plan (MIndexVocabularySearchTree, MIndexWordBreakPlan) into
m3/ports/indexstring (package jdk.internal.m3.indexstring), on top of the IX1 trie core. Text rules:
  1. package com.synexia.indexstring -> jdk.internal.m3.indexstring; IProgressMonitor -> jdk.m3.collections
     M3ProgressMonitor, noop() -> new M3DefaultProgressMonitor() (IX1 rules).
  2. MIndexVocabularySearchTree owns its rows. The donor read each row through MIndexStringPrecomputation
     (chars(valueAt(row))); the relocated tree is compiled from a Collection of CharSequence spellings,
     deduplicated by spelling in first-occurrence order (the donor's LinkedHashMap over MIndexString, whose
     equals is spelling equality), each frozen once as FrozenChars. valueAt(row) exposes the frozen row.
  3. MIndexStringBitOps.regionEquals (a packed four-unit equality over CharSequence) becomes a private unit loop
     with the same result; the donor class and its two span helpers are not needed for one equality.
  4. MIndexWordBreakPlan: vocabulary MIndexStringPrecomputation -> MIndexVocabularySearchTree (the tree is the
     vocabulary, so words.vocabularyTree() -> words); inputs MIndexString -> CharSequence; segment views
     SubMIndexString -> CharSequence via source.subSequence(begin, end) (a String gives a String).
  5. Javadoc links to donor-only types are rewritten to their M3JDK counterparts.
Refuses when a rule site is absent (donor drift) or a donor-only type survives."""
import re
import sys
from pathlib import Path

src, out = Path(sys.argv[1]), Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
DONOR_ONLY = re.compile(r"\b(MIndexString|SubMIndexString|MIndexStringPrecomputation|MIndexStringBitOps|"
                        r"MIndexStringSearchTree|IProgressMonitor)\b")


def sub(text, old, new, name, count=1, regex=False):
    hits = len(re.findall(old, text)) if regex else text.count(old)
    if hits != count:
        sys.exit(f"REFUSE {name}: rule site {old!r} found {hits}x, expected {count} (donor drift)")
    return re.sub(old, new, text) if regex else text.replace(old, new)


def common(text, name):
    text = sub(text, "package com.synexia.indexstring;", "package jdk.internal.m3.indexstring;", name)
    text = sub(text, "import com.synexia.job.IProgressMonitor;\n",
               "import jdk.m3.collections.M3DefaultProgressMonitor;\nimport jdk.m3.collections.M3ProgressMonitor;\n", name)
    text = text.replace("IProgressMonitor.noop()", "new M3DefaultProgressMonitor()")
    return re.sub(r"\bIProgressMonitor\b", "M3ProgressMonitor", text)


REGION_EQUALS = '''
  /** UTF-16 unit equality of two ranges (the donor's packed four-unit form gives the same answer). */
  private static boolean regionEquals(
      CharSequence left, int leftOffset, CharSequence right, int rightOffset, int length) {
    for (int i = 0; i < length; i++) {
      if (left.charAt(leftOffset + i) != right.charAt(rightOffset + i)) return false;
    }
    return true;
  }
}
'''

COMPILE = '''  /**
   * Compiles the vocabulary tree over spellings. Rows are distinct spellings in first-occurrence order;
   * each row is frozen once (UTF-16 units, isolated surrogates kept) and never re-read from the caller.
   */
  public static MIndexVocabularySearchTree compile(Collection<? extends CharSequence> vocabulary) {
    Objects.requireNonNull(vocabulary, "vocabulary");
    Map<String, Integer> positions = new LinkedHashMap<>();
    for (CharSequence value : vocabulary) {
      Objects.requireNonNull(value, "vocabulary element");
      positions.putIfAbsent(value.toString(), positions.size());
    }
    FrozenChars[] frozen = new FrozenChars[positions.size()];
    int row = 0;
    for (String spelling : positions.keySet()) frozen[row++] = FrozenChars.copyOf(spelling.toCharArray());
    return new MIndexVocabularySearchTree(frozen);
  }

  /** The frozen spelling of one row. */
  public FrozenChars valueAt(int row) {
    return spellings[Objects.checkIndex(row, spellings.length)];
  }
'''

name = "MIndexVocabularySearchTree"
t = common((src / f"{name}.java").read_text(encoding="utf-8").replace("\r\n", "\n"), name)
t = sub(t, "import java.util.Arrays;\n", "import java.util.Arrays;\nimport java.util.Collection;\nimport java.util.LinkedHashMap;\nimport java.util.Map;\n", name)
t = sub(t, " * <p>This is intentionally distinct from {@link MIndexStringSearchTree}, which indexes all suffixes\n"
           " * inside one MIndexString.",
        " * <p>This is intentionally distinct from an all-suffix search tree, which indexes all suffixes\n"
        " * inside one string.", name)
t = sub(t, "  private final MIndexStringPrecomputation vocabulary;\n", "  private final FrozenChars[] spellings;\n", name)
t = sub(t, "  private MIndexVocabularySearchTree(MIndexStringPrecomputation vocabulary) {\n"
           "    this.vocabulary = Objects.requireNonNull(vocabulary, \"vocabulary\");\n",
        "  private MIndexVocabularySearchTree(FrozenChars[] spellings) {\n    this.spellings = spellings;\n", name)
t = sub(t, "  public static MIndexVocabularySearchTree compile(MIndexStringPrecomputation vocabulary) {\n"
           "    return new MIndexVocabularySearchTree(vocabulary);\n  }\n", COMPILE, name)
t = sub(t, "new int[vocabulary.size()]", "new int[spellings.length]", name)
t = sub(t, "    return vocabulary.chars(vocabulary.valueAt(row));\n", "    return spellings[row];\n", name)
t = sub(t, "MIndexStringBitOps.regionEquals(", "regionEquals(", name, count=2)
t = sub(t, r"\n}\n$", REGION_EQUALS, name, regex=True)
outputs = {name: t}

name = "MIndexWordBreakPlan"
p = common((src / f"{name}.java").read_text(encoding="utf-8").replace("\r\n", "\n"), name)
p = sub(p, "{@link MIndexStringPrecomputation#vocabularyTree()} owner.", "{@link MIndexVocabularySearchTree} owner.", name)
p = sub(p, ".view(", ".subSequence(", name)
p = sub(p, "words.vocabularyTree()\n              .precomputeTries(", "words\n              .precomputeTries(", name)
p = re.sub(r"\bSubMIndexString\b", "CharSequence", p)
p = re.sub(r"\bMIndexStringPrecomputation\b", "MIndexVocabularySearchTree", p)
p = re.sub(r"\bMIndexString\b", "CharSequence", p)
p = p.replace("arbitrary UTF-16 CharSequence values", "arbitrary UTF-16 CharSequence values (a String included)")
outputs[name] = p

for n, text in outputs.items():
    left = sorted(set(DONOR_ONLY.findall(text)))
    if left:
        sys.exit(f"REFUSE {n}: donor-only types survive: {left}")
    raw = (src / f"{n}.java").read_bytes()
    (out / f"{n}.java").write_bytes(text.encode("utf-8"))
    print(f"{n}\tbefore={len(raw)}\tafter={len(text.encode())}")
