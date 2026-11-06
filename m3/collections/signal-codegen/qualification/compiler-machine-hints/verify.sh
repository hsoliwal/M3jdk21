#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Seal historical five-file source receipt and the exact MOP successor.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
historic="$root/m3/collections/signal-codegen/qualification/m3-compiler-signal-facts-20261022/SOURCE_MANIFEST.tsv"
successor="$root/m3/collections/signal-codegen/qualification/compiler-machine-hints/SOURCE_MANIFEST.tsv"
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
    # Retain exact historical processor/test blobs; a separately reviewed,
    # source-sealed successor owns their current product images.
    if [[ "$kind" == PROCESSOR || "$kind" == PROCESSOR_TEST ]]; then
        [[ "$(git -C "$root" cat-file blob "$blob" | sha256sum | cut -d' ' -f1)" == "$after" ]] ||
            fail "historical successor blob lost $path"
    else
        assert_source "$path" "$after" "$blob"
    fi
    prev="$path"
    successor_count=$((successor_count+1))
done < "$successor"
[[ "$successor_count" -eq 5 ]] || fail "expected five successor items"
record_manifest="$root/m3/collections/signal-codegen/qualification/record-component-accessor/SOURCE_MANIFEST.tsv"
[[ -f "$record_manifest" ]] || fail "record successor manifest missing"
prev='' record_count=0
while IFS=$'\t' read -r kind path before after blob || [[ -n "${kind:-}" ]]; do
    [[ -z "${kind:-}" || "${kind:0:1}" == '#' ]] && continue
    [[ "$path" > "$prev" ]] || fail "record successor order"
    case "$kind:$path" in
        PROCESSOR:"$processor")
            [[ "$before" == 55a742f32ce86f6425267ecfbcf6c35b2c447841b07d2094fd391c21b67af025 &&
               "$after" == 62b3dba8c30ca2440f6bb6fa355bc06ad27dd52470b1beffe8b049b02defa506 &&
               "$blob" == e2a8b3f007e06370eb5a8608f48a73f6d6e8b926 ]] ||
                fail "record processor receipt changed" ;;
        PROCESSOR_TEST:"$processor_test")
            [[ "$before" == dfec3ce0db75774065345ccd65c1abf9a24a89e97c9a1894639599c19370f743 &&
               "$after" == 535d1e26df2d06796be02fb56acb0e640f5eaeb949acb6dbf0b4858a89d58097 &&
               "$blob" == ff4af3b79edc4626cb68de5c2b0b7e3aec2e6b12 ]] ||
                fail "record test receipt changed" ;;
        *) fail "unexpected record successor $kind:$path" ;;
    esac
    assert_source "$path" "$after" "$blob"
    prev="$path"
    record_count=$((record_count+1))
done < "$record_manifest"
[[ "$record_count" -eq 2 ]] || fail "record successor must contain exactly two sources"
echo "M3_MACHINE_COMPILER_SUCCESSION_PASS historical=5 successor=5 record=2"
echo "NOTE: This source seal is not an executed compiler/JNI/JIT semantic proof."
