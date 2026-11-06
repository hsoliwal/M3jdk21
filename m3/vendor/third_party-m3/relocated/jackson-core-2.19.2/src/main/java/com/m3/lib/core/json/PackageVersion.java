package com.m3.lib.core.json;

import com.m3.lib.core.Version;
import com.m3.lib.core.Versioned;
import com.m3.lib.core.util.VersionUtil;

/**
 * Automatically generated from PackageVersion.java.in during
 * packageVersion-generate execution of maven-replacer-plugin in
 * pom.xml.
 */
public final class PackageVersion implements Versioned {
    public final static Version VERSION = VersionUtil.parseVersion(
        "2.19.2", "com.m3.lib.core", "jackson-core");

    @Override
    public Version version() {
        return VERSION;
    }
}
