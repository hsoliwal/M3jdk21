set -euo pipefail
java -Xmx128m -ea -cp "$M3_BIGSTRING_CLASSES" com.m3.text.M3BigStringSelfTest
