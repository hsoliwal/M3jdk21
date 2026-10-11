// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/TeamcityBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.marker.ci;

import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.openrewrite.Tree.randomId;

public final class TeamcityBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String projectName;
    private final String buildNumber;
    private final String buildUrl;
    private final String version;

    public static TeamcityBuildEnvironment build(UnaryOperator<String> environment) {
        return new TeamcityBuildEnvironment(randomId(), environment.apply("TEAMCITY_PROJECT_NAME"), environment.apply("BUILD_NUMBER"), environment.apply("BUILD_URL"), environment.apply("TEAMCITY_VERSION"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        // Not enough information to build GitProvenance
        // https://www.jetbrains.com/help/teamcity/predefined-build-parameters.html#Predefined+Server+Build+Parameters
        throw new IncompleteGitConfigException();
    }

    public TeamcityBuildEnvironment(final UUID id, final String projectName, final String buildNumber, final String buildUrl, final String version) {
        this.id = id;
        this.projectName = projectName;
        this.buildNumber = buildNumber;
        this.buildUrl = buildUrl;
        this.version = version;
    }

    public UUID getId() {
        return this.id;
    }

    public String getProjectName() {
        return this.projectName;
    }

    public String getBuildNumber() {
        return this.buildNumber;
    }

    public String getBuildUrl() {
        return this.buildUrl;
    }

    public String getVersion() {
        return this.version;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TeamcityBuildEnvironment(id=" + this.getId() + ", projectName=" + this.getProjectName() + ", buildNumber=" + this.getBuildNumber() + ", buildUrl=" + this.getBuildUrl() + ", version=" + this.getVersion() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TeamcityBuildEnvironment)) return false;
        final TeamcityBuildEnvironment other = (TeamcityBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$projectName = this.getProjectName();
        final Object other$projectName = other.getProjectName();
        if (this$projectName == null ? other$projectName != null : !this$projectName.equals(other$projectName)) return false;
        final Object this$buildNumber = this.getBuildNumber();
        final Object other$buildNumber = other.getBuildNumber();
        if (this$buildNumber == null ? other$buildNumber != null : !this$buildNumber.equals(other$buildNumber)) return false;
        final Object this$buildUrl = this.getBuildUrl();
        final Object other$buildUrl = other.getBuildUrl();
        if (this$buildUrl == null ? other$buildUrl != null : !this$buildUrl.equals(other$buildUrl)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $projectName = this.getProjectName();
        result = result * PRIME + ($projectName == null ? 43 : $projectName.hashCode());
        final Object $buildNumber = this.getBuildNumber();
        result = result * PRIME + ($buildNumber == null ? 43 : $buildNumber.hashCode());
        final Object $buildUrl = this.getBuildUrl();
        result = result * PRIME + ($buildUrl == null ? 43 : $buildUrl.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TeamcityBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new TeamcityBuildEnvironment(id, this.projectName, this.buildNumber, this.buildUrl, this.version);
    }
}
