// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/JenkinsBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.openrewrite.Tree.randomId;

public final class JenkinsBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildNumber;
    private final String buildId;
    private final String buildUrl;
    private final String host;
    private final String job;
    /**
     * Local branch name, e.g. main.
     * <p>
     * When the option to set the local branch is disabled in Jenkins this won't be present.
     */
    @Nullable
    private final String localBranch;
    /**
     * Remote branch name, e.g. origin/main.
     */
    private final String branch;

    public static JenkinsBuildEnvironment build(UnaryOperator<String> environment) {
        return new JenkinsBuildEnvironment(randomId(), environment.apply("BUILD_NUMBER"), environment.apply("BUILD_ID"), environment.apply("BUILD_URL"), environment.apply("JENKINS_URL"), environment.apply("JOB_NAME"), environment.apply("GIT_LOCAL_BRANCH"), environment.apply("GIT_BRANCH"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        throw new IncompleteGitConfigException();
    }

    public JenkinsBuildEnvironment(final UUID id, final String buildNumber, final String buildId, final String buildUrl, final String host, final String job, @Nullable final String localBranch, final String branch) {
        this.id = id;
        this.buildNumber = buildNumber;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.host = host;
        this.job = job;
        this.localBranch = localBranch;
        this.branch = branch;
    }

    public UUID getId() {
        return this.id;
    }

    public String getBuildNumber() {
        return this.buildNumber;
    }

    public String getBuildId() {
        return this.buildId;
    }

    public String getBuildUrl() {
        return this.buildUrl;
    }

    public String getHost() {
        return this.host;
    }

    public String getJob() {
        return this.job;
    }

    /**
     * Local branch name, e.g. main.
     * <p>
     * When the option to set the local branch is disabled in Jenkins this won't be present.
     */
    @Nullable
    public String getLocalBranch() {
        return this.localBranch;
    }

    /**
     * Remote branch name, e.g. origin/main.
     */
    public String getBranch() {
        return this.branch;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "JenkinsBuildEnvironment(id=" + this.getId() + ", buildNumber=" + this.getBuildNumber() + ", buildId=" + this.getBuildId() + ", buildUrl=" + this.getBuildUrl() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", localBranch=" + this.getLocalBranch() + ", branch=" + this.getBranch() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof JenkinsBuildEnvironment)) return false;
        final JenkinsBuildEnvironment other = (JenkinsBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$buildNumber = this.getBuildNumber();
        final Object other$buildNumber = other.getBuildNumber();
        if (this$buildNumber == null ? other$buildNumber != null : !this$buildNumber.equals(other$buildNumber)) return false;
        final Object this$buildId = this.getBuildId();
        final Object other$buildId = other.getBuildId();
        if (this$buildId == null ? other$buildId != null : !this$buildId.equals(other$buildId)) return false;
        final Object this$buildUrl = this.getBuildUrl();
        final Object other$buildUrl = other.getBuildUrl();
        if (this$buildUrl == null ? other$buildUrl != null : !this$buildUrl.equals(other$buildUrl)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        final Object this$job = this.getJob();
        final Object other$job = other.getJob();
        if (this$job == null ? other$job != null : !this$job.equals(other$job)) return false;
        final Object this$localBranch = this.getLocalBranch();
        final Object other$localBranch = other.getLocalBranch();
        if (this$localBranch == null ? other$localBranch != null : !this$localBranch.equals(other$localBranch)) return false;
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $buildNumber = this.getBuildNumber();
        result = result * PRIME + ($buildNumber == null ? 43 : $buildNumber.hashCode());
        final Object $buildId = this.getBuildId();
        result = result * PRIME + ($buildId == null ? 43 : $buildId.hashCode());
        final Object $buildUrl = this.getBuildUrl();
        result = result * PRIME + ($buildUrl == null ? 43 : $buildUrl.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        final Object $job = this.getJob();
        result = result * PRIME + ($job == null ? 43 : $job.hashCode());
        final Object $localBranch = this.getLocalBranch();
        result = result * PRIME + ($localBranch == null ? 43 : $localBranch.hashCode());
        final Object $branch = this.getBranch();
        result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JenkinsBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new JenkinsBuildEnvironment(id, this.buildNumber, this.buildId, this.buildUrl, this.host, this.job, this.localBranch, this.branch);
    }
}
