set -euo pipefail
printf '%s  %s\n' 'ec7142df904511f455e07650081db9eb6518a1fab138ea7ae44be15192cde343' 'm3/core/src/jdk/m3/text/M3BigString.java' | sha256sum --check -
printf '%s  %s\n' '6a86ba78d034b115bd3895fc2b66916a4c00a485ad01e60ff6e30bbbf410fc61' 'm3/core/test/jdk/m3/text/M3BigStringSelfTest.java' | sha256sum --check -
git diff --check
