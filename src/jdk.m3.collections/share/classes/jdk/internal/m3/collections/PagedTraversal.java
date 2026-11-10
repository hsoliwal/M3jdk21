/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Bounded descriptor paging with ordered filter-before-load execution.
 * Retains one descriptor page and one loaded value, excluding callback-owned state.
 * No worker, cache, global state or whole-input copy is created. Callback exceptions
 * propagate immediately; already performed effects are not rolled back. Null descriptors
 * and values are passed through to callbacks, which decide their own null policy.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/PagedTraversal.java}
 * blob {@code 7148b804f779e59443fa8a8154b151dc7f829a97} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PagedTraversal {
    private PagedTraversal() { }

    /** Operation counts, not a timing or allocation estimate. */
    public record Statistics(long pages, long examined, long delivered) { }

    /** Process in source order until the total or delivery budget is reached.
     * Bounds are validated before callbacks; zero work causes no fetch. A malformed page
     * fails before any descriptor from that page is processed. The descriptor predicate
     * must be semantically valid before payload loading; this API cannot prove purity.
     * @throws IllegalArgumentException for a negative count/budget or nonpositive page size
     * @throws IllegalStateException when a page is null or has an unexpected length
     */
    public static <D, T> Statistics run(long total, int pageSize, long maxDelivered,
            PageSource<D> source, Predicate<? super D> accept,
            Function<? super D, ? extends T> load, Consumer<? super T> consume) {
        if (total < 0 || pageSize <= 0 || maxDelivered < 0) {
            throw new IllegalArgumentException("invalid paging bounds");
        }
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(accept, "accept");
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(consume, "consume");
        long offset = 0;
        long pages = 0;
        long examined = 0;
        long delivered = 0;
        while (offset < total && delivered < maxDelivered) {
            int limit = (int) Math.min((long) pageSize, total - offset);
            List<D> page = source.fetch(offset, limit);
            if (page == null || page.size() != limit) {
                throw new IllegalStateException("page must contain exactly " + limit + " descriptors");
            }
            pages++;
            for (D descriptor : page) {
                examined++;
                if (accept.test(descriptor)) {
                    T value = load.apply(descriptor);
                    consume.accept(value);
                    delivered++;
                    if (delivered == maxDelivered) break;
                }
            }
            // limit <= total - offset, so this cannot overflow even at Long.MAX_VALUE.
            offset += limit;
        }
        return new Statistics(pages, examined, delivered);
    }
}
