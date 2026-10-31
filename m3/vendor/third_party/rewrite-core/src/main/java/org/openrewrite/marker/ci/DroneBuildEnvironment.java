// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/DroneBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.OperatingSystemProvenance.hostname;

public final class DroneBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildId;
    private final String host;
    private final String job;
    private final String branch;
    private final String tag;
    private final String remoteURL;
    private final String commitSha;

    public static DroneBuildEnvironment build(UnaryOperator<String> environment) {
        return new DroneBuildEnvironment(randomId(), environment.apply("DRONE_BUILD_NUMBER"), hostname(), environment.apply("DRONE_REPO"), environment.apply("DRONE_BRANCH"), environment.apply("DRONE_TAG"), environment.apply("DRONE_REMOTE_URL"), environment.apply("DRONE_COMMIT_SHA"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(remoteURL) || (StringUtils.isBlank(branch) && StringUtils.isBlank(tag)) || StringUtils.isBlank(commitSha)) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), remoteURL, StringUtils.isBlank(branch) ? tag : branch, commitSha, null, null, emptyList());
    }

    public String getBuildUrl() {
        return "http://" + host + "/build/" + buildId;
    }

    public DroneBuildEnvironment(final UUID id, final String buildId, final String host, final String job, final String branch, final String tag, final String remoteURL, final String commitSha) {
        this.id = id;
        this.buildId = buildId;
        this.host = host;
        this.job = job;
        this.branch = branch;
        this.tag = tag;
        this.remoteURL = remoteURL;
        this.commitSha = commitSha;
    }

    public UUID getId() {
        return this.id;
    }

    public String getBuildId() {
        return this.buildId;
    }

    public String getHost() {
        return this.host;
    }

    public String getJob() {
        return this.job;
    }

    public String getBranch() {
        return this.branch;
    }

    public String getTag() {
        return this.tag;
    }

    public String getRemoteURL() {
        return this.remoteURL;
    }

    public String getCommitSha() {
        return this.commitSha;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DroneBuildEnvironment(id=" + this.getId() + ", buildId=" + this.getBuildId() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", branch=" + this.getBranch() + ", tag=" + this.getTag() + ", remoteURL=" + this.getRemoteURL() + ", commitSha=" + this.getCommitSha() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DroneBuildEnvironment)) return false;
        final DroneBuildEnvironment other = (DroneBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$buildId = this.getBuildId();
        final Object other$buildId = other.getBuildId();
        if (this$buildId == null ? other$buildId != null : !this$buildId.equals(other$buildId)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        final Object this$job = this.getJob();
        final Object other$job = other.getJob();
        if (this$job == null ? other$job != null : !this$job.equals(other$job)) return false;
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
        final Object this$tag = this.getTag();
        final Object other$tag = other.getTag();
        if (this$tag == null ? other$tag != null : !this$tag.equals(other$tag)) return false;
        final Object this$remoteURL = this.getRemoteURL();
        final Object other$remoteURL = other.getRemoteURL();
        if (this$remoteURL == null ? other$remoteURL != null : !this$remoteURL.equals(other$remoteURL)) return false;
        final Object this$commitSha = this.getCommitSha();
        final Object other$commitSha = other.getCommitSha();
        if (this$commitSha == null ? other$commitSha != null : !this$commitSha.equals(other$commitSha)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $buildId = this.getBuildId();
        result = result * PRIME + ($buildId == null ? 43 : $buildId.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        final Object $job = this.getJob();
        result = result * PRIME + ($job == null ? 43 : $job.hashCode());
        final Object $branch = this.getBranch();
        result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
        final Object $tag = this.getTag();
        result = result * PRIME + ($tag == null ? 43 : $tag.hashCode());
        final Object $remoteURL = this.getRemoteURL();
        result = result * PRIME + ($remoteURL == null ? 43 : $remoteURL.hashCode());
        final Object $commitSha = this.getCommitSha();
        result = result * PRIME + ($commitSha == null ? 43 : $commitSha.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public DroneBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new DroneBuildEnvironment(id, this.buildId, this.host, this.job, this.branch, this.tag, this.remoteURL, this.commitSha);
    }
}
