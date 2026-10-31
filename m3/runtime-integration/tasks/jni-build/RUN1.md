# First hosted execution

Run37183562154/job111380786644 checked out exact812d9dbbee4baebaac852c7c43289abc3a755f18.
All five genuine OpenRewrite/JUnit installer tests passed with zero skips. The next Maven native
backend test invocation failed before testing: Python unittest received -p without its required
value. No native build ran and no product branch was saved.

The POM now invokes the existing test_fix.py entry point directly instead of forwarding unittest
-discovery switches through exec. It runs the identical six test methods; none are removed. Both
the exact POM resource and its SHA seal are updated. Locally the exact new command runs all six
tests, and the five real OpenRewrite/JUnit tests pass again. Maven/backend/native execution must
be checked on the next real hosted run. Failed outputs remain preserved in artifact11296171484.
