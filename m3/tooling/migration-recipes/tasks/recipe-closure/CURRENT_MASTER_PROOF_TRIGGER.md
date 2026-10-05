# Current-master proof trigger

Disposable verification-only change. This branch exists solely to invoke the repository's existing
`m3-jdk-proof.yml` pull-request path on exact master `fc6dedcb84e8eb9f95689d4e9ff9d1614775b529`.

It must not be merged. The resulting proof-kit artifact, if the hosted runner schedules the job, is
used to execute the full retained Maven/OpenRewrite/JUnit/JaCoCo control reactor without reconstructing
source from older artifacts.
