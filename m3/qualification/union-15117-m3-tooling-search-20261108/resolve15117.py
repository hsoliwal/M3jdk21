"""resolve15117.py : resolve the two diff3 conflicts in each #15117 merge (mf/15117_*) into union/*.

Java conflict 1: chain empty-interval block, then #15117's merge-mode guard (mergeWithinBudget).
Java conflict 2: #15117's adjacent-duplicate reuse around the chain's lower-bound-reusing search.
C conflict 1: chain only (its zero-key and empty-range fast paths supersede #15117's copies of them).
C conflict 2: #15117's previous-key memo around the chain's lower-bound-reusing search."""
import re
import sys
from pathlib import Path

BLOCK = re.compile(r"^<<<<<<< [^\n]*\n(.*?)^\|\|\|\|\|\|\| [^\n]*\n(.*?)^=======\n(.*?)^>>>>>>> [^\n]*\n",
                   re.S | re.M)


def sides(text):
    return [(m.group(1), m.group(2), m.group(3)) for m in BLOCK.finditer(text)]


def resolve(text, choices):
    it = iter(choices)
    out = BLOCK.sub(lambda m: next(it), text)
    if "<<<<<<<" in out or ">>>>>>>" in out:
        sys.exit("unresolved markers")
    return out


java = Path("mf/15117_M3SortedIntBounds.java").read_text(encoding="utf-8")
(o1, _, t1), (o2, _, t2) = sides(java)
guard = "        if (keys.length >= 128 && nonDecreasing(keys)\n"
ours_guard = guard.replace("\n", ") {\n")
assert o1.endswith(ours_guard) and t1.startswith(guard), "java conflict 1 shape"
j1 = o1[: -len(ours_guard)] + t1
dup_head = ("                if (i > 0 && needle == keys[i - 1]) {\n"
            "                    // Exact adjacent duplicate: the preceding bounds have identical search semantics.\n"
            "                    result[i * 2] = result[(i - 1) * 2];\n"
            "                    result[i * 2 + 1] = result[(i - 1) * 2 + 1];\n"
            "                } else {\n")
assert t2.startswith(dup_head), "java conflict 2 shape"
j2 = dup_head + "".join("    " + line for line in o2.splitlines(keepends=True)) + "                }\n"
Path("union").mkdir(exist_ok=True)
Path("union/M3SortedIntBounds.java").write_text(resolve(java, [j1, j2]), encoding="utf-8", newline="\n")

c = Path("mf/15117_m3_native_sorted_bounds.c").read_text(encoding="utf-8")
(co1, _, _), (co2, _, ct2) = sides(c)
expect = ("                /* Sorted input guarantees upper bound >= lower bound. */\n"
          "                jint lower = m3_lower(region, range_length, from, needle);\n"
          "                jint offset = lower - from;\n"
          "                output_window[2 * i] = lower;\n"
          "                output_window[2 * i + 1] = m3_upper(region + offset,\n"
          "                        range_length - offset, lower, needle);\n")
assert co2 == expect, "c conflict 2 ours shape"
assert ct2.startswith("                if (!has_previous || needle != previous) {\n"), "c conflict 2 theirs shape"
c2 = ("                if (!has_previous || needle != previous) {\n"
      "                    /* Sorted input guarantees upper bound >= lower bound. */\n"
      "                    lower_cursor = m3_lower(region, range_length, from, needle);\n"
      "                    jint offset = lower_cursor - from;\n"
      "                    upper_cursor = m3_upper(region + offset,\n"
      "                            range_length - offset, lower_cursor, needle);\n"
      "                    previous = needle;\n"
      "                    has_previous = 1;\n"
      "                }\n"
      "                output_window[2 * i] = lower_cursor;\n"
      "                output_window[2 * i + 1] = upper_cursor;\n")
Path("union/m3_native_sorted_bounds.c").write_text(resolve(c, [co1, c2]), encoding="utf-8", newline="\n")
print("RESOLVED java+c")
