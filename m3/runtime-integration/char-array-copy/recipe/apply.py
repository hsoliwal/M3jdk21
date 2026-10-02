#!/usr/bin/env python3
"""Replay the source-sealed MIndex char-array copy improvement."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
ENGINE = "src/java.base/share/classes/java/lang/MIndexString.java"
TEST = "m3/runtime-integration/tests/MIndexStringInvariant.java"
WORKFLOW = ".github/workflows/m3-foundation.yml"

OLD_COPY = """    void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        String.checkBoundsBeginEnd(srcBegin, srcEnd, length);
        Objects.checkFromIndexSize(dstBegin, srcEnd - srcBegin, dst.length);
        for (int source = srcBegin, target = dstBegin; source < srcEnd; source++, target++) {
            dst[target] = charAt(source);
        }
    }
"""

NEW_COPY = """    void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        String.checkBoundsBeginEnd(srcBegin, srcEnd, length);
        Objects.checkFromIndexSize(dstBegin, srcEnd - srcBegin, dst.length);
        if (srcBegin == srcEnd) {
            return;
        }
        switch (storageKind) {
            case LOCAL -> {
                if (coder == String.LATIN1) {
                    StringLatin1.getChars(localValue, srcBegin, srcEnd, dst, dstBegin);
                } else {
                    StringUTF16.getChars(localValue, srcBegin, srcEnd, dst, dstBegin);
                }
            }
            case LEXICON, SHARED_LEXICON -> {
                for (int source = srcBegin, target = dstBegin; source < srcEnd; source++, target++) {
                    dst[target] = mappedChar(source);
                }
            }
            case JOINED -> {
                int segment = segmentAt(srcBegin);
                int segmentStart = segment == 0 ? 0 : ends[segment - 1];
                int source = srcBegin;
                int target = dstBegin;
                while (source < srcEnd) {
                    int limit = Math.min(srcEnd, ends[segment]);
                    int atomStart = offsets[segment] + source - segmentStart;
                    segments[segment].getChars(atomStart, atomStart + limit - source, dst, target);
                    target += limit - source;
                    source = limit;
                    segmentStart = ends[segment++];
                }
            }
            default -> throw new InternalError("invalid MIndex storage kind");
        }
    }
"""

OLD_CALL = """        eq(JOINED, kind(storage(repeated)), "repeat remains MIndex tuple");

        System.out.println("""
NEW_CALL = """        eq(JOINED, kind(storage(repeated)), "repeat remains MIndex tuple");
        verifySegmentedArrays();

        System.out.println("""

OLD_HELPER = """    private static Object storage(String value) throws IllegalAccessException {
"""
NEW_HELPER = """    private static void verifySegmentedArrays() throws Exception {
        // String.join accepts CharSequence; each mutable char[] first becomes an immutable atom.
        char[][] inputs = new char[127][];
        String[] parts = new String[inputs.length];
        int expectedLength = 0;
        for (int index = 0; index < inputs.length; index++) {
            inputs[index] = switch (index % 11) {
                case 0 -> new char[0];
                case 1 -> new char[] {'\\ud83d'};
                case 2 -> new char[] {'\\ude00'};
                case 3 -> new char[] {'\\u0100'};
                case 4 -> new char[] {'\\u0000', 'X'};
                default -> new char[] {(char) ('a' + index % 26)};
            };
            expectedLength += inputs[index].length;
            parts[index] = new String(inputs[index]);
        }
        char[] expected = new char[expectedLength];
        int position = 0;
        for (char[] input : inputs) {
            System.arraycopy(input, 0, expected, position, input.length);
            position += input.length;
        }
        String joined = String.join("", parts);
        Object joinedStorage = storage(joined);
        eq(JOINED, kind(joinedStorage), "dynamic array join stays MIndex-backed");
        same(joinedStorage, storage(String.join("", parts.clone())),
                "equal dynamic array joins reuse their canonical tuple");
        for (char[] input : inputs) {
            Arrays.fill(input, '!');
        }
        check(Arrays.equals(expected, joined.toCharArray()), "mutating sources cannot change join");
        char[] padded = new char[expectedLength + 6];
        Arrays.fill(padded, '\\uffff');
        joined.getChars(0, expectedLength, padded, 3);
        for (int index = 0; index < expectedLength; index++) {
            check(expected[index] == padded[index + 3], "full getChars range " + index);
        }
        check(padded[0] == '\\uffff' && padded[expectedLength + 3] == '\\uffff',
                "getChars preserves destination outside range");
        joined.getChars(1, 1, padded, 2);
        check(padded[2] == '\\uffff', "empty getChars range");
        int middle = expectedLength / 2;
        char[] middleChars = new char[9];
        joined.getChars(middle - 3, middle + 4, middleChars, 1);
        check(Arrays.equals(Arrays.copyOfRange(middleChars, 1, 8),
                Arrays.copyOfRange(expected, middle - 3, middle + 4)),
                "getChars spanning atom boundaries");
        String slice = joined.substring(1, expectedLength - 1);
        check(Arrays.equals(Arrays.copyOfRange(expected, 1, expectedLength - 1),
                slice.toCharArray()), "slice projects atom ranges");
        // The bounded String.join path is not the only composition entry:
        // repeated concat remains segmented beyond its 127-element threshold.
        String extended = joined;
        char[] extendedExpected = Arrays.copyOf(expected, expectedLength + 130);
        for (int index = 0; index < 130; index++) {
            char next = (char) ('A' + index % 26);
            extendedExpected[expectedLength + index] = next;
            extended = extended.concat(new String(new char[] {next}));
        }
        Object extendedStorage = storage(extended);
        eq(JOINED, kind(extendedStorage), "more than 127 arrays stay segmented");
        check(Arrays.equals(extendedExpected, extended.toCharArray()),
                "extended dynamic array projection");
        check(MATERIALIZED.get(extendedStorage) == null,
                "extended projection does not allocate a byte cache");
        check(MATERIALIZED.get(joinedStorage) == null,
                "char[] projections leave the joined byte cache unallocated");
    }

    private static Object storage(String value) throws IllegalAccessException {
"""

OLD_WORKFLOW = """      - name: Verify deterministic source-bound recipe
        run: |
          python3 m3/runtime-integration/recipe/apply.py --check
          python3 m3/runtime-integration/recipe/test_recipe.py
"""
NEW_WORKFLOW = """      - name: Verify layered source-bound recipes
        run: |
          python3 m3/runtime-integration/char-array-copy/recipe/apply.py --check
          python3 m3/runtime-integration/char-array-copy/recipe/test_recipe.py
          python3 m3/runtime-integration/char-array-copy/recipe/apply.py --reverse
          trap 'python3 m3/runtime-integration/char-array-copy/recipe/apply.py >/dev/null' EXIT
          python3 m3/runtime-integration/recipe/apply.py --check
          python3 m3/runtime-integration/recipe/test_recipe.py
          python3 m3/runtime-integration/char-array-copy/recipe/apply.py
          trap - EXIT
          python3 m3/runtime-integration/char-array-copy/recipe/apply.py --check
"""

REPLACEMENTS = {
    ENGINE: ((OLD_COPY, NEW_COPY),),
    TEST: ((OLD_CALL, NEW_CALL), (OLD_HELPER, NEW_HELPER)),
    WORKFLOW: ((OLD_WORKFLOW, NEW_WORKFLOW),),
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def transform(name, data, reverse=False):
    text = data.decode("utf-8")
    for old, new in REPLACEMENTS[name]:
        before, after = (new, old) if reverse else (old, new)
        if text.count(before) != 1:
            raise ValueError("recipe anchor drift: " + name)
        text = text.replace(before, after, 1)
    return text.encode("utf-8")


def apply(target=ROOT, reverse=False, check=False):
    target = Path(target).resolve()
    manifest = json.loads((HERE / "manifest.json").read_text())
    states = set()
    files = {}
    for name, hashes in manifest["files"].items():
        safe = PurePosixPath(name)
        if safe.is_absolute() or ".." in safe.parts or name not in REPLACEMENTS:
            raise ValueError("unsafe path: " + name)
        path = target / name
        if path.is_symlink() or target not in path.resolve().parents:
            raise ValueError("symlink path: " + name)
        data = path.read_bytes()
        actual = digest(data)
        if actual == hashes["before"]:
            states.add("before")
        elif actual == hashes["after"]:
            states.add("after")
        else:
            raise ValueError("source drift: " + name)
        files[name] = (path, data)
    if len(states) != 1 or set(files) != set(REPLACEMENTS):
        raise ValueError("mixed source state")
    state = states.pop()
    desired = "before" if reverse else "after"
    if state == desired or check:
        return state
    outputs = {
        name: transform(name, data, reverse)
        for name, (_, data) in files.items()
    }
    for name, output in outputs.items():
        if digest(output) != manifest["files"][name][desired]:
            raise ValueError("postimage drift: " + name)
    for name, output in outputs.items():
        files[name][0].write_bytes(output)
    return desired


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, default=ROOT)
    parser.add_argument("--reverse", action="store_true")
    parser.add_argument("--check", action="store_true")
    arguments = parser.parse_args()
    print("M3_CHAR_ARRAY_COPY_RECIPE state="
          + apply(arguments.target, arguments.reverse, arguments.check))
