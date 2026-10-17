# Initial fixture-assembly failure

The exact compiler descriptor compiled successfully, and jmod recreated the compiler module. Recreating the already decorated stock `java.base` descriptor with another target-platform option produced `More than one ModuleTarget attribute` when the result was described. The retained command and logs reproduce that invalid fixture assembly. No upstream test or product class failed here.

The final proof preserves the existing version/target fields, uses the stock JDK classfile API to remove only the package/hash attributes that standard jmod regenerates, and verifies the resulting structural descriptor and every other recorded hash. It does not disable module hash validation.
