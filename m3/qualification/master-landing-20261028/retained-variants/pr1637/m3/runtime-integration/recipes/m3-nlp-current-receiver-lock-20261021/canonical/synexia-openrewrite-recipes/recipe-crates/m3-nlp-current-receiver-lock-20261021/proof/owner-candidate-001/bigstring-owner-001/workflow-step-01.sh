set -euo pipefail
printf '%s  %s\n' 'e09815cea88c44e4da89ca8979ba247cddeee6c97b496d1aa2309a48916a90de' 'm3/core/src/com/m3/text/M3BigString.java' | sha256sum --check -
printf '%s  %s\n' '5f47db4885a99b1ec7dc95957276d0130f2eab7cba16614907995c722fa6f9a6' 'm3/core/test/com/m3/text/M3BigStringSelfTest.java' | sha256sum --check -
git diff --check
