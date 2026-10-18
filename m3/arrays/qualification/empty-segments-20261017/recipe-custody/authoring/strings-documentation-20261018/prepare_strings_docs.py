#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Author and seal documentation for one pinned public String utility owner.

This prepares a candidate for the canonical Maven/OpenRewrite receiver. It never
edits a receiving source. Embedded target documentation retains GPLv2+CPE.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
TARGET = "src/java.base/share/classes/java/util/Strings.java"
BEFORE = "cbadcbce521f89c53fbd92d9644402e7a72bb5e7e56e8943e8c29fbdc0fadb9d"

# Each entry names an exact declaration in the sealed preimage. Executable tokens
# are verified separately by compiling both images with debug attributes disabled.
DOCS = {
    "boolean isNullOrEmpty(CharSequence source)": """Returns whether the sequence is null or has no UTF-16 code units.

@param source the sequence to inspect, or null
@return true if the sequence is null or empty""",
    "String nullToEmpty(String source)": """Returns the empty string for a null reference.

@param source the string to normalize, or null
@return the source itself when non-null, otherwise the empty string""",
    "String emptyToNull(String source)": """Converts an empty string to null, retaining nonempty string identity.

@param source the string to normalize, or null
@return null for a null or empty source, otherwise the source itself""",
    "boolean isNullOrBlank(CharSequence source)": """Returns whether the sequence is null, empty, or contains only code points
classified as whitespace by {@link Character#isWhitespace(int)}.
This is the whitespace rule used by {@link String#isBlank()}.

@param source the sequence to inspect, or null
@return true if the sequence has no non-whitespace code point""",
    "boolean hasText(CharSequence source)": """Returns whether the sequence contains a non-whitespace code point.

@param source the sequence to inspect, or null
@return true if the source is non-null and is not blank""",
    "String defaultIfEmpty(String source, String fallback)": """Returns the fallback for a null or empty source.

@param source the string to inspect, or null
@param fallback the replacement, which may be null
@return the fallback if the source is null or empty, otherwise the source""",
    "String defaultIfBlank(String source, String fallback)": """Returns the fallback for a null or blank source.

@param source the string to inspect, or null
@param fallback the replacement, which may be null
@return the fallback if the source is null or blank, otherwise the source""",
    "String repeat(String source, int copies)": """Repeats the source through {@link String#repeat(int)}.

@param source the string to repeat
@param copies the nonnegative number of copies
@return the repeated string
@throws NullPointerException if the source is null
@throws IllegalArgumentException if the number of copies is negative""",
    "String padStart(String source, int minLength, char pad)": """Pads the start of the source to at least the requested UTF-16 length.
A source already meeting that length is returned unchanged.

@param source the string to pad
@param minLength the nonnegative minimum number of UTF-16 code units
@param pad the UTF-16 code unit to prepend as needed
@return the padded string, or the source itself if no padding is needed
@throws NullPointerException if the source is null
@throws IllegalArgumentException if the minimum length is negative""",
    "String padEnd(String source, int minLength, char pad)": """Pads the end of the source to at least the requested UTF-16 length.
A source already meeting that length is returned unchanged.

@param source the string to pad
@param minLength the nonnegative minimum number of UTF-16 code units
@param pad the UTF-16 code unit to append as needed
@return the padded string, or the source itself if no padding is needed
@throws NullPointerException if the source is null
@throws IllegalArgumentException if the minimum length is negative""",
    "String commonPrefix(String first, String second)": """Returns the longest equal UTF-16 prefix that does not split a valid
surrogate pair in either source. Unpaired surrogates retain String semantics.

@param first the first string
@param second the second string
@return their common prefix, or the empty string when they have none
@throws NullPointerException if either argument is null""",
    "String commonSuffix(String first, String second)": """Returns the longest equal UTF-16 suffix that does not split a valid
surrogate pair in either source. Unpaired surrogates retain String semantics.

@param first the first string
@param second the second string
@return their common suffix, or the empty string when they have none
@throws NullPointerException if either argument is null""",
    "String removePrefix(String source, String prefix)": """Removes a matching prefix, using String's case-sensitive comparison.

@param source the string to inspect
@param prefix the prefix to remove
@return the suffix following a match, otherwise the source itself
@throws NullPointerException if either argument is null""",
    "String removeSuffix(String source, String suffix)": """Removes a matching suffix, using String's case-sensitive comparison.

@param source the string to inspect
@param suffix the suffix to remove
@return the prefix preceding a match, otherwise the source itself
@throws NullPointerException if either argument is null""",
    "String substringBefore(String source, String delimiter)": """Returns the part before the first delimiter, or the source when absent.
An empty delimiter produces the empty string.

@param source the string to inspect
@param delimiter the case-sensitive delimiter
@return the part before the first match, or the source if there is no match
@throws NullPointerException if either argument is null""",
    "String substringAfter(String source, String delimiter)": """Returns the part after the first delimiter, or the empty string when absent.
An empty delimiter produces the full source.

@param source the string to inspect
@param delimiter the case-sensitive delimiter
@return the part after the first match, or the empty string if absent
@throws NullPointerException if either argument is null""",
    "String substringBeforeLast(String source, String delimiter)": """Returns the part before the last delimiter, or the source when absent.
An empty delimiter produces the full source.

@param source the string to inspect
@param delimiter the case-sensitive delimiter
@return the part before the last match, or the source if absent or empty
@throws NullPointerException if either argument is null""",
    "String substringAfterLast(String source, String delimiter)": """Returns the part after the last delimiter, or the empty string when absent.
An empty delimiter produces the empty string.

@param source the string to inspect
@param delimiter the case-sensitive delimiter
@return the part after the last match, or the empty string if absent or empty
@throws NullPointerException if either argument is null""",
    "int countOccurrences(String source, String needle)": """Counts non-overlapping, case-sensitive occurrences of the needle.
An empty needle has zero occurrences for this operation.

@param source the string to search
@param needle the text to count
@return the number of non-overlapping matches
@throws NullPointerException if either argument is null""",
    "boolean startsWithIgnoreCase(String source, String prefix)": """Tests the prefix using {@link String#regionMatches(boolean,int,String,int,int)}
with case ignored, without locale-sensitive case rewriting.

@param source the string to inspect
@param prefix the prefix to test
@return true if the prefix matches while ignoring case
@throws NullPointerException if either argument is null""",
    "boolean endsWithIgnoreCase(String source, String suffix)": """Tests the suffix using {@link String#regionMatches(boolean,int,String,int,int)}
with case ignored, without locale-sensitive case rewriting.

@param source the string to inspect
@param suffix the suffix to test
@return true if the suffix matches while ignoring case
@throws NullPointerException if either argument is null""",
    "int indexOfIgnoreCase(String source, String needle, int fromIndex)": """Finds the first match from the requested UTF-16 index using String's
region comparison with case ignored. Negative starting indices become zero.
An empty needle matches at the starting index clamped to the source length.

@param source the string to search
@param needle the text to find
@param fromIndex the first UTF-16 index to inspect
@return the first matching index, or -1 if no match exists
@throws NullPointerException if either string is null""",
    "int indexOfIgnoreCase(String source, String needle)": """Finds the first match with case ignored, starting at index zero.

@param source the string to search
@param needle the text to find
@return the first matching UTF-16 index, or -1 if no match exists
@throws NullPointerException if either argument is null""",
    "boolean containsIgnoreCase(String source, String needle)": """Tests whether the source contains a match with case ignored.
An empty needle is contained in every non-null source.

@param source the string to search
@param needle the text to find
@return true if a match exists
@throws NullPointerException if either argument is null""",
    "String reverse(String source)": """Reverses the source through {@link StringBuilder#reverse()}, preserving
valid UTF-16 surrogate pairs according to that operation's contract.

@param source the string to reverse
@return the reversed string, or the source itself when shorter than two units
@throws NullPointerException if the source is null""",
    "String stripToNull(String source)": """Strips surrounding Unicode whitespace using {@link String#strip()}.

@param source the string to strip, or null
@return null for a null source or empty stripped result, otherwise the result""",
    "String stripToEmpty(String source)": """Strips surrounding Unicode whitespace using {@link String#strip()}.

@param source the string to strip, or null
@return the empty string for null, otherwise the stripped result""",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline-root", type=Path, required=True)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    args = parser.parse_args()
    base = args.baseline_root.resolve()
    owner = args.recipe_owner.resolve()
    here = args.work_dir.resolve()
    crate = owner / "recipe-crates/m3-collections-contract-refinement-20261017"
    if here.is_relative_to(base):
        raise ValueError("candidate output must be outside the receiving source tree")
    before = (base / TARGET).read_bytes()
    if hashlib.sha256(before).hexdigest() != BEFORE:
        raise ValueError("Strings target preimage drift")
    after = before.decode("utf-8")
    for declaration, body in DOCS.items():
        signature = "    public static " + declaration + " {"
        if after.count(signature) != 1:
            raise ValueError("ambiguous declaration: " + declaration)
        at = after.index(signature)
        prefix = after[:at]
        start = at
        if prefix.rstrip().endswith("*/"):
            start = prefix.rfind("    /**")
            if start < 0 or prefix[start:].count("*/") != 1:
                raise ValueError("ambiguous documentation boundary")
        comment = "    /**\n" + "\n".join("     *" + (" " + line if line else "")
                                          for line in body.splitlines()) + "\n     */\n"
        after = after[:start] + comment + after[at:]
    candidate = here / "candidate" / TARGET
    candidate.parent.mkdir(parents=True, exist_ok=True)
    candidate.write_text(after, encoding="utf-8")
    spec = importlib.util.spec_from_file_location("strings_recipe_prepare", crate / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(crate, "m3jdk21-strings-doclint", "hsoliwal/M3jdk21", base,
                            [here / "candidate"], "4b24824c3aa09d802121447f8970d442a855f064", owner)
    receipt = {"before_sha256": BEFORE, "after_sha256": hashlib.sha256(after.encode()).hexdigest(),
               "public_methods_documented": len(DOCS), "candidate_only": True,
               "gate": "Maven/OpenRewrite and bytecode equivalence must execute before receiving",
               "metadata": metadata}
    (here / "authoring-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
