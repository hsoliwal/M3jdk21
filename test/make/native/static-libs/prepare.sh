#!/bin/bash
# Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
# SPDX-License-Identifier: Apache-2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail
source_dir="$1"
mkdir -p "$source_dir/common" "$source_dir/left" "$source_dir/right" \
    "$source_dir/combined" "$source_dir/controlmain"

printf 'int shared(void) { return 7; }\n' > "$source_dir/common/shared.c"
printf 'int left(void) { return 13; }\n' > "$source_dir/left/left.c"
printf 'int right(void) { return 17; }\n' > "$source_dir/right/right.c"

for part in {1..20}; do
    printf 'int part%d(void) { return %d; }\n' "$part" "$part" \
        > "$source_dir/common/part$part.c"
done

for program in combined controlmain; do
    {
        printf 'int shared(void);\nint right(void);\n'
        if [[ "$program" == combined ]]; then
            printf 'int left(void);\n'
        fi
        for part in {1..20}; do
            printf 'int part%d(void);\n' "$part"
        done
        printf 'int main(void) {\n  int sum = shared() + right();\n'
        for part in {1..20}; do
            printf '  sum += part%d();\n' "$part"
        done
        if [[ "$program" == combined ]]; then
            printf '  sum += left();\n  return sum == 247 ? 0 : 1;\n}\n'
        else
            printf '  return sum == 234 ? 0 : 1;\n}\n'
        fi
    } > "$source_dir/$program/main.c"
done
