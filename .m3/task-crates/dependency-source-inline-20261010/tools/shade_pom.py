#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""DEPENDENCY-SOURCE-INLINE (M3jdk21): maven-shade-plugin aggregator over the vendored source modules.

Writes m3/vendor/third_party/m3-tooling-shaded/pom.xml: one self-contained jar
(com.m3.vendor:m3-tooling-third-party) built only from the in-repo source modules (<version>-m3-source-1) the
tooling consumes, so the tooling can run from a single artifact that the JDK repository builds itself.
Shade configuration:
  * ServicesResourceTransformer (META-INF/services merged), ApacheLicense/ApacheNotice transformers (legal texts
    merged), AppendingTransformer for META-INF/rewrite/*.yml is not needed (distinct file names)
  * jar signature files stripped (signed jgit/EC entries cannot survive a merge)
  * no relocation: OpenRewrite resolves recipes, markers and Jackson type ids by class name, so relocating would
    change behaviour; every third-party license text travels inside the jar under META-INF/third-party/<module>
  * enforcer-style closure check: shade_closure.py lists every artifact that went into the jar and fails if any is
    not a vendored -m3-source-1 module (that would be a downstream dependency)
"""
from __future__ import annotations
import argparse, csv, io, sys
from pathlib import Path

ROOTS = ["rewrite-core", "rewrite-java", "rewrite-java-21", "rewrite-test", "rewrite-xml", "rewrite-yaml"]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", type=Path, required=True)
    ap.add_argument("--modules", type=Path, required=True, help="M3_THIRD_PARTY_MODULES.tsv")
    ap.add_argument("--shade-version", default="3.6.0")
    args = ap.parse_args()
    mods = [m for m in csv.DictReader(io.open(args.modules, encoding="utf-8", newline=""), delimiter="\t") if m["kind"] == "MODULE"]
    roots = [m for m in mods if m["artifact"] in ROOTS]
    missing = sorted(set(ROOTS) - set(m["artifact"] for m in roots))
    deps = "".join("      <dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version></dependency>\n" % (m["group"], m["artifact"], m["version"]) for m in sorted(roots, key=lambda x: x["artifact"]))
    pom = f"""<!-- SPDX-License-Identifier: Apache-2.0; shade aggregator only. Every merged class keeps its upstream license; the texts are merged under META-INF and kept per module under META-INF/third-party/<module>. -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.m3.vendor</groupId><artifactId>m3-tooling-third-party</artifactId><version>8.17.1-m3-shaded-1</version>
  <name>M3jdk21 tooling third-party stack (shaded from in-repo source builds)</name>
  <description>OpenRewrite 8.17.1 and its runtime closure, compiled from the vendored sources in m3/vendor/third_party and merged into one jar with maven-shade-plugin. No relocation (OpenRewrite resolves recipes and Jackson type ids by class name).</description>
  <properties><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <dependencies>
{deps}  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>{args.shade_version}</version>
        <executions><execution><phase>package</phase><goals><goal>shade</goal></goals>
          <configuration>
            <createDependencyReducedPom>true</createDependencyReducedPom>
            <filters><filter><artifact>*:*</artifact><excludes><exclude>META-INF/*.SF</exclude><exclude>META-INF/*.RSA</exclude><exclude>META-INF/*.DSA</exclude><exclude>META-INF/*.EC</exclude><exclude>module-info.class</exclude><exclude>META-INF/versions/*/module-info.class</exclude></excludes></filter></filters>
            <transformers>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer"/>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer"><addHeader>false</addHeader></transformer>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"><manifestEntries><Automatic-Module-Name>com.m3.vendor.tooling.thirdparty</Automatic-Module-Name><Multi-Release>true</Multi-Release></manifestEntries></transformer>
            </transformers>
          </configuration>
        </execution></executions>
      </plugin>
    </plugins>
  </build>
</project>
"""
    out = args.repo / "m3" / "vendor" / "third_party" / "m3-tooling-shaded" / "pom.xml"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(pom, encoding="utf-8", newline="\n")
    print("shade pom", out, "roots", len(roots), "missing roots", missing)
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
