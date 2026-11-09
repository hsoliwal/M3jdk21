"""relocate_ix1.py <donor-src-dir> <out-dir> : INDEXSTRING_RELOCATE_IX1 v1.
Relocates the synexia-indexstring trie core (MIndexTrieNavigation, MIndexLiteralSearchTree) into M3JDK's
m3/ports/indexstring (package jdk.internal.m3.indexstring). Steps, each a pure text rule:
  1. package com.synexia.indexstring -> jdk.internal.m3.indexstring (JDK naming rule PKG).
  2. com.synexia.job.IProgressMonitor -> jdk.m3.collections.M3ProgressMonitor (same rule as the m3/search leaves).
  3. MIndexStringEditDistancePlan -> jdk.m3.precompute.M3EditDistancePlan (the existing M3JDK owner of the
     compiled query snapshot; the trie only uses compile(CharSequence) and the CharSequence view).
  4. Methods whose parameter list names the donor MIndexString type are removed together with their Javadoc.
     They are token-arena fast paths of the CharSequence overloads; in M3JDK a String is the CharSequence input.
  5. Prose that names a donor type with no M3JDK owner (SubMIndexString) is rewritten to the String form.
  6. M3ProgressMonitor.noop() -> new M3DefaultProgressMonitor() (no static noop on the M3JDK monitor).
Writes <out-dir>/<Name>.java (LF, UTF-8) and prints per file the bytes before/after and the removed methods.
Refuses when a rule finds no site (donor drift) or when a donor-only type survives the rewrite."""
import re
import sys
from pathlib import Path

src, out = Path(sys.argv[1]), Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
FILES = ["MIndexTrieNavigation", "MIndexLiteralSearchTree"]
DONOR_ONLY = re.compile(r"\b(MIndexString|SubMIndexString|MIndexStringEditDistancePlan|IProgressMonitor|"
                        r"IndexTextMetricsResolver|MIndexStringPrecomputation)\b")


def remove_mindex_overloads(text, name, removed):
    """Remove each method declaration whose parameters include 'MIndexString input' (brace matched)."""
    while True:
        m = re.search(r"\n(  public [^\n;{]*\(\s*MIndexString input[^)]*\)\s*\{)", text)
        if not m:
            return text
        start = m.start() + 1
        doc = text.rfind("/**", 0, start)
        if doc >= 0 and text[text.find("*/", doc) + 2:start].strip() == "":
            start = text.rfind("\n", 0, doc) + 1
        depth, i = 0, text.index("{", m.start(1))
        while True:
            c = text[i]
            depth += c == "{"
            depth -= c == "}"
            i += 1
            if depth == 0:
                break
        end = i + 1 if text[i:i + 1] == "\n" else i
        if text[end:end + 1] == "\n":
            end += 1
        removed.append(name + "." + re.sub(r"\s+", " ", m.group(1)).split("(")[0].split()[-1])
        text = text[:start] + text[end:]


def need(pattern, text, name):
    if not re.search(pattern, text, re.M):
        sys.exit(f"REFUSE {name}: rule site '{pattern}' absent (donor drift)")


for name in FILES:
    raw = (src / f"{name}.java").read_text(encoding="utf-8").replace("\r\n", "\n")
    text = raw
    need(r"^package com\.synexia\.indexstring;", text, name)
    text = re.sub(r"^package com\.synexia\.indexstring;", "package jdk.internal.m3.indexstring;", text, flags=re.M)
    need(r"^import com\.synexia\.job\.IProgressMonitor;", text, name)
    text = re.sub(r"^import com\.synexia\.job\.IProgressMonitor;", "import jdk.m3.collections.M3ProgressMonitor;",
                  text, flags=re.M)
    text = re.sub(r"\bIProgressMonitor\b", "M3ProgressMonitor", text)
    if "MIndexStringEditDistancePlan" in text:
        text = re.sub(r"\bMIndexStringEditDistancePlan\b", "M3EditDistancePlan", text)
        text = text.replace("import jdk.m3.collections.M3ProgressMonitor;\n",
                            "import jdk.m3.collections.M3ProgressMonitor;\nimport jdk.m3.precompute.M3EditDistancePlan;\n", 1)
    if "M3ProgressMonitor.noop()" in text:
        # jdk.m3.collections.M3ProgressMonitor has no static noop(); its no-op owner is M3DefaultProgressMonitor
        # (same literal rule as the m3/search leaves, rules_v2.tsv row 3).
        text = text.replace("M3ProgressMonitor.noop()", "new M3DefaultProgressMonitor()")
        text = text.replace("import jdk.m3.collections.M3ProgressMonitor;\n",
                            "import jdk.m3.collections.M3DefaultProgressMonitor;\nimport jdk.m3.collections.M3ProgressMonitor;\n", 1)
    removed = []
    text = remove_mindex_overloads(text, name, removed)
    text = text.replace("SubMIndexString is accepted through CharSequence", "a String slice is accepted as a CharSequence")
    left = sorted(set(DONOR_ONLY.findall(text)))
    if left:
        sys.exit(f"REFUSE {name}: donor-only types survive: {left}")
    (out / f"{name}.java").write_bytes(text.encode("utf-8"))
    print(f"{name}\tbefore={len(raw.encode())}\tafter={len(text.encode())}\tremoved={','.join(removed) or '-'}")
