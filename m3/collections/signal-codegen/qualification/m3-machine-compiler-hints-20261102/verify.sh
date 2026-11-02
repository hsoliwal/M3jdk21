#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Seal historical five-file source receipt and the exact MOP successor.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
historic="$root/m3/collections/signal-codegen/qualification/m3-compiler-signal-facts-20261022/SOURCE_MANIFEST.tsv"
successor="$root/m3/collections/signal-codegen/qualification/m3-machine-compiler-hints-20261102/SOURCE_MANIFEST.tsv"
processor='m3/collections/signal-codegen/src/main/java/com/m3/collections/sortable/SortableProcessor.java'
processor_test='m3/collections/signal-codegen/src/test/java/com/m3/collections/sortable/SortableProcessorSelfTest.java'
baseline='ce005d25b29fb6246e013bffb462afd5ed070362'

fail() { echo "M3_HINT_SEAL_REFUSED: $*" >&2; exit 1; }
hash() { sha256sum "$1" | cut -d' ' -f1; }
assert_source() {
    local path="$1" after="$2" blob="$3"
    [[ -f "$root/$path" ]] || fail "missing $path"
    [[ "$(hash "$root/$path")" == "$after" ]] || fail "sha256 drift $path"
    [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || fail "git blob drift $path"
}
prev='' historic_count=0
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$prev" && "$path" != /* && "$path" != *'..'* ]] || fail "historic manifest ordering"
    [[ "$after" =~ ^[0-9a-f]{64}$ && "$blob" =~ ^[0-9a-f]{40}$ ]] || fail "historic blob geometry"
    [[ "$(git -C "$root" cat-file blob "$blob" | sha256sum | cut -d' ' -f1)" == "$after" ]] || fail "historic blob lost $path"
    if [[ "$path" != "$processor" && "$path" != "$processor_test" ]]; then
        assert_source "$path" "$after" "$blob"
    fi
    prev="$path"
    historic_count=$((historic_count+1))
done < "$historic"
[[ "$historic_count" -eq 5 ]] || fail "expected five historic compiler sources"

prev='' successor_count=0
while IFS=$'\t' read -r kind path before after blob || [[ -n "${kind:-}" ]]; do
    [[ -z "${kind:-}" || "${kind:0:1}" == '#' ]] && continue
    [[ "$path" > "$prev" && "$path" != /* && "$path" != *'..'* ]] || fail "successor path ordering"
    [[ "$after" =~ ^[0-9a-f]{64}$ && "$blob" =~ ^[0-9a-f]{40}$ ]] || fail "successor blob geometry"
    case "$kind:$path" in
        JAVA_MAIN:m3/collections/signal-codegen/src/main/java/com/m3/collections/sortable/M3MachineHintRules.java|\
        JAVA_TEST:m3/collections/signal-codegen/src/test/java/com/m3/collections/sortable/M3MachineHintRulesContract.java)
            [[ "$before" == ABSENT ]] || fail "new file preimage"
            if git -C "$root" cat-file -e "$baseline:$path" 2>/dev/null; then
                fail "new file existed at baseline $path"
            fi
            ;;
        PROCESSOR:"$processor")
            [[ "$before" == db7ba492e25113caa7d1d0abb7c9024c51f2b48e78d08eba76b634762aaa936f ]] ||
                fail "processor preimage mismatch"
            ;;
        PROCESSOR_TEST:"$processor_test")
            [[ "$before" == 39c988866f042fb3716092055c6fbe3fc7d27ca771d30c6261096ec06902e0b2 ]] ||
                fail "processor test preimage mismatch"
            ;;
        VERIFY_SCRIPT:m3/collections/signal-codegen/verify.sh)
            [[ "$before" == 99b65d3f4a1d3ee100d4e4ceac31e98c0a91f1b7d659a27caee9d39ebf56b05e ]] ||
                fail "verifier preimage mismatch"
            ;;
        *) fail "unknown successor kind/path $kind:$path";;
    esac
    assert_source "$path" "$after" "$blob"
    prev="$path"
    successor_count=$((successor_count+1))
done < "$successor"
[[ "$successor_count" -eq 5 ]] || fail "expected five successor items"
echo "M3_MACHINE_COMPILER_SUCCESSION_PASS historical=5 successor=5"
echo "NOTE: This source seal is not an executed compiler/JNI/JIT semantic proof."
