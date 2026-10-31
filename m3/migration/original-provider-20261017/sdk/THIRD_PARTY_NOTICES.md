# Provider and fixture provenance

The snapshot adapter, recipe tests, custody tooling and provider wrapper retain their Apache-2.0 source notices. Maven/OpenRewrite and JUnit are used through their published artifacts at the pinned POM versions. No Java parser or replacement JSON Schema engine is introduced.

The six unmodified Python releases retain their complete licenses in their installed wheels. Their exact source license bytes and release provenance are recorded in the outer provider-inventory.json; five use MIT, and typing_extensions retains its complete PSF license. No optional format extras are installed.

Exact receiver source fixtures retain their original per-file notices. OpenJDK source fixtures remain under their original GPLv2/Classpath/assembly terms; licenses/LICENSE, licenses/ADDITIONAL_LICENSE_INFO and licenses/ASSEMBLY_EXCEPTION are verbatim from hsoliwal/M3jdk21 commit4e29dc9d4141261e8763fbe9ffd6a20e017b1aff. They are verification fixtures, not a relicensing or an installed-JDK replacement. Every original mapping/receipt is historical evidence with its existing scope.

Infrastructure donors: unchanged adapter blob4a38e39d0755462d40c2b22548b87abde938ea0b; Maven shell d79867bcc09ad6329e12418e15907e54b8ab025f; SDK tests406bbd972956883e2ea989727d60ffa6662e34ae; custody helper20fdc0b6de51de46bcce615f16ad1dc0dabb1cd4; bounded hosted controls8416ce181bd1962389d9fde189715a43b79810c3. The physical FOSS gate preceded all new implementation.
