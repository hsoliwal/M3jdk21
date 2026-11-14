#!/bin/bash
# gate_native.sh <tree> <out> : builds the seven m3/search JNI libraries as CMakeLists.txt does (MinGW gcc, C11,
# -Wall -Wextra -Werror -O3) into <out>/native and prints the java -D options surefire passes.
set -u
export MSYS_NO_PATHCONV=1 PATH="/c/ProgramData/mingw64/mingw64/bin:$PATH"
T=$1; O=$2; mkdir -p $O/native; O=$(cd $O && pwd -W); N=$T/search/src/main/native
INC='-IC:/Program Files/Java/jdk-21/include'; INC2='-IC:/Program Files/Java/jdk-21/include/win32'
build() { gcc -std=c11 -Wall -Wextra -Werror -O3 -shared "$INC" "$INC2" -o $O/native/$1.dll "${@:2}" > $O/native/$1.log 2>&1; echo "native $1 rc=$?"; }
build m3search $N/jni_search.c $N/jni_z_similarity.c
build m3coverwindow $N/minimum_cover_window_jni.c
build m3subseqwindow $N/minimum_subsequence_window_jni.c
build m3wordconcat $N/fixed_word_concat_jni.c
build m3batchsubseq $N/batch_subsequence_match_jni.c
build m3bitap $N/jni_bitap_exact.c
build m3pairsum $N/pair_sum_search.c
