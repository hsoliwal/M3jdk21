# TypeFix publication

The saved compiler repair now has original upstream ancestry rather than export ancestry.
One existing Java recipe changes by two explicit SourceFile type arguments. The exact source
and test postimages reuse the same Git blobs as their templates. APIs, source guards, metadata
copy order, POM dependencies and configured coverage gates are unchanged.

Locally executed: full selected-file reads and hashes, Java21 syntax/shape probe compilation and
run, source/template identity checks. Actual OpenRewrite/JUnit/module verification awaits the
read-only hosted workflow. A passing syntax probe is not production type-linking or JUnit.
Maven remains unavailable locally; dependency DNS and binary download attempts failed.

The JNI String construction and backing-close contract failures remain in the explicit queue;
this publication neither narrows close() nor suppresses warnings to manufacture a JDK build.
No canonical merge or full-programme completion is performed.
