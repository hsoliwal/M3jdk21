// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/text/FindAndReplace.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.text;

import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Marker;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public final class FindAndReplace extends Recipe {
    @Option(displayName = "Find", description = "The text to find (and replace). This snippet can be multiline.", example = "blacklist")
    private final String find;
    @Option(displayName = "Replace", description = "The replacement text for `find`. This snippet can be multiline.", example = "denylist", required = false)
    @Nullable
    private final String replace;
    @Option(displayName = "Regex", description = "Default false. If true, `find` will be interpreted as a Regular Expression, and capture group contents will be available in `replace`.", required = false)
    @Nullable
    private final Boolean regex;
    @Option(displayName = "Case sensitive", description = "If `true` the search will be sensitive to case. Default `false`.", required = false)
    @Nullable
    private final Boolean caseSensitive;
    @Option(displayName = "Regex multiline mode", description = "When performing a regex search setting this to `true` allows \"^\" and \"$\" to match the beginning and end of lines, respectively. " + "When performing a regex search when this is `false` \"^\" and \"$\" will match only the beginning and ending of the entire source file, respectively." + "Has no effect when not performing a regex search. Default `false`.", required = false)
    @Nullable
    private final Boolean multiline;
    @Option(displayName = "Regex dot all", description = "When performing a regex search setting this to `true` allows \".\" to match line terminators." + "Has no effect when not performing a regex search. Default `false`.", required = false)
    @Nullable
    private final Boolean dotAll;
    @Option(displayName = "File pattern", description = "A glob expression that can be used to constrain which directories or source files should be searched. " + "Multiple patterns may be specified, separated by a semicolon `;`. " + "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " + "When not set, all source files are searched. ", required = false, example = "**/*.java")
    @Nullable
    private final String filePattern;

    @Override
    public String getDisplayName() {
        return "Find and replace";
    }

    @Override
    public String getDescription() {
        return "Simple text find and replace. When the original source file is a language-specific Lossless Semantic " + "Tree, this operation irreversibly converts the source file to a plain text file. Subsequent recipes " + "will not be able to operate on language-specific type.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                for (Marker marker : sourceFile.getMarkers().getMarkers()) {
                    if (marker instanceof AlreadyReplaced) {
                        AlreadyReplaced alreadyReplaced = (AlreadyReplaced) marker;
                        if (Objects.equals(find, alreadyReplaced.getFind()) && Objects.equals(replace, alreadyReplaced.getReplace())) {
                            return sourceFile;
                        }
                    }
                }
                String searchStr = find;
                if (!Boolean.TRUE.equals(regex)) {
                    searchStr = Pattern.quote(searchStr);
                }
                int patternOptions = 0;
                if (!Boolean.TRUE.equals(caseSensitive)) {
                    patternOptions |= Pattern.CASE_INSENSITIVE;
                }
                if (Boolean.TRUE.equals(multiline)) {
                    patternOptions |= Pattern.MULTILINE;
                }
                if (Boolean.TRUE.equals(dotAll)) {
                    patternOptions |= Pattern.DOTALL;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                Pattern pattern = Pattern.compile(searchStr, patternOptions);
                Matcher matcher = pattern.matcher(plainText.getText());
                if (!matcher.find()) {
                    return sourceFile;
                }
                String replacement = replace == null ? "" : replace;
                if (!Boolean.TRUE.equals(regex)) {
                    replacement = replacement.replace("$", "\\$");
                }
                String newText = matcher.replaceAll(replacement);
                return plainText.withText(newText).withMarkers(sourceFile.getMarkers().add(new AlreadyReplaced(randomId(), find, replace)));
            }
        };
        //noinspection DuplicatedCode
        if (filePattern != null) {
            //noinspection unchecked
            TreeVisitor<?, ExecutionContext> check = Preconditions.or(Arrays.stream(filePattern.split(";")).map(FindSourceFiles::new).map(Recipe::getVisitor).toArray(TreeVisitor[]::new));
            visitor = Preconditions.check(check, visitor);
        }
        return visitor;
    }

    public FindAndReplace(final String find, @Nullable final String replace, @Nullable final Boolean regex, @Nullable final Boolean caseSensitive, @Nullable final Boolean multiline, @Nullable final Boolean dotAll, @Nullable final String filePattern) {
        this.find = find;
        this.replace = replace;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
        this.multiline = multiline;
        this.dotAll = dotAll;
        this.filePattern = filePattern;
    }

    public String getFind() {
        return this.find;
    }

    @Nullable
    public String getReplace() {
        return this.replace;
    }

    @Nullable
    public Boolean getRegex() {
        return this.regex;
    }

    @Nullable
    public Boolean getCaseSensitive() {
        return this.caseSensitive;
    }

    @Nullable
    public Boolean getMultiline() {
        return this.multiline;
    }

    @Nullable
    public Boolean getDotAll() {
        return this.dotAll;
    }

    @Nullable
    public String getFilePattern() {
        return this.filePattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindAndReplace(find=" + this.getFind() + ", replace=" + this.getReplace() + ", regex=" + this.getRegex() + ", caseSensitive=" + this.getCaseSensitive() + ", multiline=" + this.getMultiline() + ", dotAll=" + this.getDotAll() + ", filePattern=" + this.getFilePattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindAndReplace)) return false;
        final FindAndReplace other = (FindAndReplace) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$regex = this.getRegex();
        final Object other$regex = other.getRegex();
        if (this$regex == null ? other$regex != null : !this$regex.equals(other$regex)) return false;
        final Object this$caseSensitive = this.getCaseSensitive();
        final Object other$caseSensitive = other.getCaseSensitive();
        if (this$caseSensitive == null ? other$caseSensitive != null : !this$caseSensitive.equals(other$caseSensitive)) return false;
        final Object this$multiline = this.getMultiline();
        final Object other$multiline = other.getMultiline();
        if (this$multiline == null ? other$multiline != null : !this$multiline.equals(other$multiline)) return false;
        final Object this$dotAll = this.getDotAll();
        final Object other$dotAll = other.getDotAll();
        if (this$dotAll == null ? other$dotAll != null : !this$dotAll.equals(other$dotAll)) return false;
        final Object this$find = this.getFind();
        final Object other$find = other.getFind();
        if (this$find == null ? other$find != null : !this$find.equals(other$find)) return false;
        final Object this$replace = this.getReplace();
        final Object other$replace = other.getReplace();
        if (this$replace == null ? other$replace != null : !this$replace.equals(other$replace)) return false;
        final Object this$filePattern = this.getFilePattern();
        final Object other$filePattern = other.getFilePattern();
        if (this$filePattern == null ? other$filePattern != null : !this$filePattern.equals(other$filePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindAndReplace;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $regex = this.getRegex();
        result = result * PRIME + ($regex == null ? 43 : $regex.hashCode());
        final Object $caseSensitive = this.getCaseSensitive();
        result = result * PRIME + ($caseSensitive == null ? 43 : $caseSensitive.hashCode());
        final Object $multiline = this.getMultiline();
        result = result * PRIME + ($multiline == null ? 43 : $multiline.hashCode());
        final Object $dotAll = this.getDotAll();
        result = result * PRIME + ($dotAll == null ? 43 : $dotAll.hashCode());
        final Object $find = this.getFind();
        result = result * PRIME + ($find == null ? 43 : $find.hashCode());
        final Object $replace = this.getReplace();
        result = result * PRIME + ($replace == null ? 43 : $replace.hashCode());
        final Object $filePattern = this.getFilePattern();
        result = result * PRIME + ($filePattern == null ? 43 : $filePattern.hashCode());
        return result;
    }
}
