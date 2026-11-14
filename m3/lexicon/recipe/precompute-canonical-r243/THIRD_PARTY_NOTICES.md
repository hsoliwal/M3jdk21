# Attribution

Copyright 2026 Hitesh Soliwal, Synexia. New work is Apache-2.0; existing source notices remain intact.

The exporter invokes the installed CPython JSON and floating-point APIs. No CPython conversion source is copied or ported. Reference source review: CPython v3.12.0 `Objects/floatobject.c`, `Python/pystrtod.c`, `Python/dtoa.c` (PSF license); executed runtime: CPython 3.12.6.

Java 21 Double.toString documentation and the installed Java 21 runtime provide the reference contract/oracle. No OpenJDK Schubfach/HotSpot code is copied. The unchanged M3 payload and text-recipe owners retain their Apache-2.0 notices.

Primary references:
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html#toString(double)
- https://docs.python.org/3.12/tutorial/floatingpoint.html
- https://github.com/python/cpython/tree/v3.12.0
