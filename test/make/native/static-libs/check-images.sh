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
topdir="$1"
spec="$2"
output_dir="$3"
make_command="$4"
class_archive="$5"
vm_archive="$6"
archive_suffix="$7"
library_prefix="$8"

class_name="${library_prefix}fixture${archive_suffix}"
vm_name="${library_prefix}jvm${archive_suffix}"
support="$output_dir/image-support"
hotspot="$output_dir/image-hotspot"
mkdir -p "$support/native/java.base/libfixture/static" \
    "$hotspot/variant-zero/libjvm/objs/static"
cp "$class_archive" "$support/native/java.base/libfixture/static/$class_name"
cp "$vm_archive" "$hotspot/variant-zero/libjvm/objs/static/$vm_name"

# Invoke the actual image makefile. Each case gets its own output tree so an
# earlier successful copy cannot hide a missing dependency or wrong destination.
for image_case in default all standard graal combined; do
    standard="$output_dir/images/$image_case/standard"
    graal="$output_dir/images/$image_case/graal"
    rm -f "$standard/lib/$class_name" "$standard/lib/zero/$vm_name" \
        "$graal/lib/$class_name" "$graal/lib/zero/$vm_name"
    goals=()
    case "$image_case" in
        all) goals=(all) ;;
        standard) goals=(static-libs-image) ;;
        graal) goals=(static-libs-graal-image) ;;
        combined) goals=(static-libs-image static-libs-graal-image) ;;
    esac
    "$make_command" -r -R -I "$topdir/make/common" \
        -f "$topdir/make/StaticLibsImage.gmk" "SPEC=$spec" \
        "ALL_MODULES=java.base" "JVM_VARIANTS=zero" \
        "SUPPORT_OUTPUTDIR=$support" "HOTSPOT_OUTPUTDIR=$hotspot" \
        "STATIC_LIBS_IMAGE_DIR=$standard" "STATIC_LIBS_GRAAL_IMAGE_DIR=$graal" \
        "${goals[@]}"
    if [[ "$image_case" != graal ]]; then
        diff "$class_archive" "$standard/lib/$class_name"
        diff "$vm_archive" "$standard/lib/zero/$vm_name"
    else
        test ! -e "$standard/lib/$class_name"
        test ! -e "$standard/lib/zero/$vm_name"
    fi
    if [[ "$image_case" == graal || "$image_case" == combined ]]; then
        diff "$class_archive" "$graal/lib/$class_name"
    else
        test ! -e "$graal/lib/$class_name"
    fi
    test ! -e "$graal/lib/zero/$vm_name"
done
