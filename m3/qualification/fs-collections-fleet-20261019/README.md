<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Product-owned M3FS / collection convergence qualification

This branch starts at a3e88976ee8664d1ef5d0cade96aec1206b2133d and preserves its M3FS runtime. The bootstrap below changes no production source. It acquires exact existing public fleet sources and executes the unchanged M3FS baseline, so later Java/JNI source changes can be qualified against that exact owner closure rather than inferred from another PR's result.

Canonical task: hsoliwal/com.synexia, synexia-openrewrite-recipes/recipe-crates/m3-fs-collections-fleet-20261019, source commit e70a7e48f43a786fc75f9abb07152213d19f75a7. The bootstrap POM and product workflow are exact mirrors of bootstrap/pom.xml and bootstrap/target-workflow.yml there. No private repository data or credentials are collected by this public workflow; every runtime donor comes from this product repository at an explicit commit.

Inputs: base a3e88976ee8664d1ef5d0cade96aec1206b2133d; bitmap/JNI f2688ab690537500ef5a45d8af2d849d2d0e5eda; predicate 45008413898bc27a4dc61c2bf19e1d433a84db01; indexed-view candidate 9d3c0a6bfb790d033cc6753fd05c48597b3bb59c. Input acquisition and the tiny dependency-bootstrap test are not combined-product or transformation qualification. Retain failed and unexecuted gates explicitly. No default branch, peer PR, source seal or receiving phase is changed.
