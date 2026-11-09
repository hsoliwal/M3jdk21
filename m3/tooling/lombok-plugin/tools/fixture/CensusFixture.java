// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Census discriminator (parse-only input, never compiled): every construct a text-level census gets wrong.
package fixture;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.NonNull;

/** "@Getter in a comment is not an annotation" */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true, of = {"a", "b"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CensusFixture {
    String text = "@Setter inside a string literal";
    @EqualsAndHashCode.Include int a;
    @lombok.With(AccessLevel.PROTECTED) int b;
    @NonNull String c;
    @Getter static int shared;

    @AllArgsConstructor(onConstructor_ = @__({@Deprecated}))
    static class Inner {
        int x;
    }

    record Point(@lombok.NonNull Integer x) {
    }

    void run() {
        @Cleanup java.io.InputStream in = null;
        val y = 1;
    }

    @Builder
    CensusFixture(@lombok.NonNull String p) {
    }
}
