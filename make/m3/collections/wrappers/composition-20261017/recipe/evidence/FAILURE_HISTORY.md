# Retained qualification failures

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

1. The initial Maven launch could not resolve Maven Central because Java/Maven
   did not automatically consume the environment proxy. No compiler or SDK
   gate ran. Task-independent Maven proxy settings and a copied JDK truststore
   augmented with the platform CA certificates restored normal verified TLS.
   No dependency or production-source gate was weakened.
2. The first actual SDK test launch reached Java 21 compilation and JUnit but
   lacked SLF4J for OpenRewrite's classpath recipe loader. Six tests ran,
   five errored with `NoClassDefFoundError: org/slf4j/LoggerFactory`; the report
   is retained in `initial-missing-slf4j.txt`. The task POM now reuses the
   predecessor's existing `slf4j-simple:1.7.36` test dependency.
3. The subsequent launch executed the positive source compositions but two
   refusal methods errored. The inherited SDK 8.89.0 test pattern expected
   refusals through `ExecutionContext`; SDK 8.17.1 can propagate the exact
   `IllegalStateException` directly from its scan/visitor boundary. The
   corrected test verifies the same exception type and signature for either
   reporting route. No invalid source becomes admissible. The failure report
   is retained in `initial-sdk-refusal-boundary.txt`.

The task now uses the existing SDK `YamlResourceLoader` to load its explicit
recipe descriptor. This avoids repeated classpath introspection attempts to
instantiate the generic snapshot provider without its required crate name;
the provider's constructor and validation remain unchanged.

Any outputs from an unsuccessful SDK campaign are not receiving authority.
Only the final successful campaign is used for source installation.
