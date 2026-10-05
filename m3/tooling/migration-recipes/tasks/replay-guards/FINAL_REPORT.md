# Replay guards

The existing donor-pair descriptor guard now rejects leading ../ after the existing separator normalization; all previous guards and valid relative spelling behavior remain. Its original three tests remain plus two path-matrix tests.

The existing proof-kit workflow gains two pull-request path filters for migration-recipes and A3. Its original security, steps, pins, failure behavior and artifact retention remain unchanged. The workflow change was produced by the named A3Kit configuration of the existing text snapshot engine, with exact template pins and three actual JUnit tests.

Recovered real Maven3.9.16/OpenRewrite8.17.1 executed 16 selected tests: five donor-pair, three A3Kit and eight parent A3 tests; zero failures/errors/skips. Actual standalone invocation of that same named recipe wrote one workflow output and proved the subsequent no-op. Output Git blob 5d21cbb0eeb283e019072f6972dafa813897558c matches the reviewed template.

These are selected-source results on the reconstructed archive at b0c2e495b8465dc802ef309c06810844b04e08e9 with exact current owners restored, not an assertion of current whole-repository acceptance. Full static analysis, coverage, reactor and modified-JDK tests remain outstanding. The real String/JNI test failures discovered in the parent work are retained. No POM, dependency, gate threshold or exclusion was changed; no canonical history rewrite or automatic merge.
