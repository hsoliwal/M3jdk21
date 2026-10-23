# Distinct C conflict hunks (superset head vs PR), with base

## H1 29c016895157 MOD_MOD M3IndexMaskWindow.c prs=1761 1769 1797 1802 1804 1814 1817 1823 1824 1825 1836 1841 1843 1851

context:
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
--- ours (superset):
            if (matches == M3_MASK_WINDOW_CHUNK && pending == 0) {
                /* Every ordinal in this chunk is selected; values is already contiguous. */
                written += matches;
                (*env)->SetIntArrayRegion(env, output,
                        output_offset + written - matches, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                next += consumed;
                continue;
            }
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
--- base:
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
--- theirs (PR):
            /* A contiguous selected span already is the compacted output. Bypass
             * identity compaction only when the original loop would flush exactly
             * this batch here. JNI call order, ranges and failure points stay equal. */
            if (matches == span && pending == 0
                    && (matches == M3_MASK_WINDOW_CHUNK || matches == remaining)) {
                (*env)->SetIntArrayRegion(env, output, output_offset + written, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                written += matches;
            } else {
                for (jint i = 0; i < matches; i++) {
                    selected[pending++] = values[(jint) offsets[i] - first_match];
                    written++;
                    if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                        (*env)->SetIntArrayRegion(env, output,
                                output_offset + written - pending, pending, selected);
                        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                        pending = 0;
                    }
--- after:
                }
            }

## H2 089500d960e5 MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
--- ours (superset):
    /* Keep the original O(selected) scratch and single-commit writeback.
     * Alias and JNI-read failures must not publish a partially selected result. */
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
--- base:
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
--- theirs (PR):
    jint *scratch = (jint *) malloc((size_t) needed * sizeof(jint));
--- after:
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");

## H3 0e100095afcf MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
--- ours (superset):
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
--- base:
    /* Pinned/copy source elements must be released before the writeback,
       so target-source aliasing does not let an abort undo output. */
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
--- theirs (PR):
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    (*env)->SetIntArrayRegion(env, target, targetOffset, needed, scratch);
--- after:
    free(scratch);
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H4 1c7105082789 MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        goto release;
>>>>>>> f20ee459858ab67efc1279b7409d415b83c2274c
    }
--- ours (superset):
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
--- base:
    /* Never mutate the destination until every possible input validation
       has succeeded, including aliasing of target with either input array. */
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
--- theirs (PR):
    /* Stage no more than the visible page. Aliased sources must not be modified
       until both input arrays have been released without write-back. */
    if ((size_t) needed > SIZE_MAX / sizeof(jint)) {
--- after:
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;

## H5 3c4fa51cbac3 MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        return 0;
    }
    jint cursor = 0;
--- ours (superset):
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        /* Complete preflight proves a dense window under the existing stable-
         * input contract. Do not fetch state bytes twice when every row passes. */
        if (selected == length) {
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
            begin += take;
            continue;
--- base:
    for (jint i = first; i < first + length; ++i) {
        if (matches(states[i], required, forbidden)) {
            scratch[cursor++] = src[i];
--- theirs (PR):
    jint skip = paged ? skipVisible : 0;
    for (jint i = first; i < first + length; ++i) {
        if (matches(states[i], required, forbidden)) {
            if (skip != 0) {
                --skip;
            } else {
                scratch[cursor++] = src[i];
                if (cursor == needed) break;
            }
--- after:
        }
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {

## H6 4fba3744074d MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
--- ours (superset):
    if (selected == 0) return 0;
    if (selected <= M3_VIEWPORT_CHUNK) {
        (*env)->SetIntArrayRegion(env, target, targetOffset, selected, inlineSelection);
        return (*env)->ExceptionCheck(env) ? 0 : selected;
--- base:
    if (selected == 0) {
        goto release;
--- theirs (PR):
    if (needed == 0) {
        goto release;
--- after:
    }
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H7 74c55224d72c MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
    return (value & yes) == yes && (value & no) == 0U;
}

--- ours (superset):
/* Never acquire or retain a pointer to either complete Java input array. */
static int read_chunk(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, int read_states, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (!read_states) return 1;
    (*env)->GetIntArrayRegion(env, flags, first, length, states);
    return !(*env)->ExceptionCheck(env);
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_project(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jintArray target, jint targetOffset) {
    (void) cls;
--- base:
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_project(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jintArray target, jint targetOffset) {
    (void) cls;
--- theirs (PR):
/* Shared full-window and visible-page selection over borrowed aligned arrays.
 * Preflight validates every source offset, including rows beyond the page.
 * Only the accepted page is staged before releasing the borrowed JNI arrays. */
static jint project_window(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, jint required, jint forbidden,
        jintArray target, jint targetOffset, jint skipVisible,
        jint maxVisible, jboolean paged) {
--- after:
    if (offsets == NULL || flags == NULL || target == NULL) {
        raise(env, "java/lang/NullPointerException", "null projection array");

## H8 7942cfe81ede MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
    (*env)->SetIntArrayRegion(env, target, targetOffset, needed, scratch);
>>>>>>> f20ee459858ab67efc1279b7409d415b83c2274c
    free(scratch);
--- ours (superset):
    return (*env)->ExceptionCheck(env) ? 0 : selected;
}

/* The Java page contract validates the full input even for an empty result.
 * Publication is one region write, so overlapping owners need only page scratch. */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_projectPage(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jint skipVisible, jint maxVisible,
        jintArray target, jint targetOffset) {
    (void) cls;
    if (offsets == NULL) {
        raise(env, "java/lang/NullPointerException", "logicalOffsets");
        return 0;
    }
    if (flags == NULL) {
        raise(env, "java/lang/NullPointerException", "states");
        return 0;
    }
    if (target == NULL) {
        raise(env, "java/lang/NullPointerException", "target");
        return 0;
    }
    const jsize sourceSize = (*env)->GetArrayLength(env, offsets);
    const jsize flagsSize = (*env)->GetArrayLength(env, flags);
    const jsize outputSize = (*env)->GetArrayLength(env, target);
    if (sourceSize != flagsSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if (first < 0 || length < 0 || first > sourceSize - length ||
            targetOffset < 0 || targetOffset > outputSize) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
    if (skipVisible < 0 || maxVisible < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "negative visible page bounds");
        return 0;
    }
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Capture a bounded page while validating. This buffer is call-local,
     * never retained and never published before complete preflight succeeds. */
    jint inlinePage[M3_VIEWPORT_CHUNK];
    jint available = 0;
    jint needed = 0;
    jint pageFirst = end;
    jint pageEnd = end;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
                /* No skip+limit arithmetic: both can independently be INT_MAX. */
                if (available >= skipVisible && needed < maxVisible) {
                    if (needed == 0) pageFirst = begin + i;
                    pageEnd = begin + i + 1;
                    if (needed < M3_VIEWPORT_CHUNK) inlinePage[needed] = src[i];
                    ++needed;
                }
                ++available;
            }
        }
        begin += take;
    }
    if (needed > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (needed == 0) return 0;
    if (needed <= M3_VIEWPORT_CHUNK) {
        /* A small returned page needs neither heap scratch nor a second input
         * pass. Source/target aliases are safe: all input reads are complete. */
        (*env)->SetIntArrayRegion(env, target, targetOffset, needed, inlinePage);
        return (*env)->ExceptionCheck(env) ? 0 : needed;
    }
    if ((size_t) needed > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
    jint *scratch = (jint *) malloc((size_t) needed * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        return 0;
    }
    jint cursor = 0;
    for (jint begin = pageFirst; begin < pageEnd;) {
        jint take = pageEnd - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (pageEnd - pageFirst == needed) {
            /* Preflight proves this page is contiguous even in a sparse window.
             * Only its offsets are needed on the second pass. */
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
        } else {
            if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {
                free(scratch);
                return 0;
            }
            for (jint i = 0; i < take; ++i) {
                if (all_states || (read_states && matches(states[i], required, forbidden))) {
                    if (cursor == needed) {
                        free(scratch);
                        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
                        return 0;
                    }
                    scratch[cursor++] = src[i];
                }
            }
        }
        begin += take;
    }
    if (cursor != needed) {
        free(scratch);
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
    (*env)->SetIntArrayRegion(env, target, targetOffset, needed, scratch);
    free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : needed;
--- base:
    return (*env)->ExceptionCheck(env) ? 0 : selected;
release:
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    return 0;
--- theirs (PR):
    return (*env)->ExceptionCheck(env) ? 0 : needed;
release:
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    return 0;
--- after:
}


## H9 c57ced4c0800 MOD_MOD m3maskviewport.c prs=1765 1780 1791 1795 1805 1808 1816

context:
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
--- ours (superset):
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Reuse page capture for a small full selection. Keep all input reads and
     * validation before publication, including late errors and aliased output. */
    jint inlineSelection[M3_VIEWPORT_CHUNK];
--- base:
    jint *src = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (src == NULL) {
        return 0; /* JVM has an exception pending. */
    }
    jint *states = (*env)->GetIntArrayElements(env, flags, NULL);
    if (states == NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
        return 0;
    }
--- theirs (PR):
    if (paged && (skipVisible < 0 || maxVisible < 0)) {
        raise(env, "java/lang/IndexOutOfBoundsException", "negative visible page bounds");
        return 0;
    }
    jint *src = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (src == NULL) {
        return 0; /* JVM has an exception pending. */
    }
    jint *states = (*env)->GetIntArrayElements(env, flags, NULL);
    if (states == NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
        return 0;
    }
--- after:
    jint selected = 0;
    for (jint begin = first; begin < end;) {

## H10 6368749a62f8 MOD_MOD m3_index_mask_compactor.c prs=1366 1368 1375 1479 1522

context:
    }
}

--- ours (superset):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
--- base:
/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- theirs (PR):
/* Reuse the existing count atom for zero-result verification before reading rows.
 * The second state pass preserves the existing count/emit drift diagnostic.
 */
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask);

static int m3_verify_empty(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    jint observed = m3_count_window(env, states, state_offset, length, mask);
    if (observed < 0) return 0;
    if (observed != 0) {
        m3_throw_state_drift(env);
        return 0;
    }
    return 1;
}

/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- after:
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */

## H11 595548e58b1f MOD_MOD m3_index_mask_compactor.c prs=1538 1546 1577

context:
        jsize first, jsize state_first, jsize n, jint required_mask) {
    jbyte marks[M3_CHUNK];
    jint row_ids[M3_CHUNK];
--- ours (superset):
    jsize count = m3_count_window(env, states, state_first, n, required_mask);
    if (count < 0) return NULL;
--- base:
    jsize count = required_mask == 0 ? n : 0;
    for (jsize offset = 0; required_mask != 0 && offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) count++;
        }
        offset += chunk;
    }
--- theirs (PR):
    unsigned char offsets[M3_CHUNK];
    jsize count = required_mask == 0 ? n : 0;
    for (jsize offset = 0; required_mask != 0 && offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) count++;
        }
        offset += chunk;
    }
--- after:
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL) return NULL;

## H12 94a94c259479 ADD_ADD m3_state_mask_histogram.c prs=1426 1435 1463

context:
            if ((*env)->ExceptionCheck(env)) return;
            buckets[(unsigned char) state]++;
        }
--- ours (superset):
    } else {
        /* Batch selected points into the existing bounded scratch region. Cast before
         * negation so even INT_MIN strides are safe for empty/singleton paths. */
        jlong magnitude = stride > 0 ? (jlong) stride : -(jlong) stride;
        jint batch_limit = (jint) (INT64_C(1) + (M3_STATE_CHUNK - 1) / magnitude);
        for (jint offset = 0; offset < count;) {
            jint take = count - offset;
            if (take > batch_limit) take = batch_limit;
            jlong span = INT64_C(1) + ((jlong) take - INT64_C(1)) * magnitude;
            jlong at = (jlong) first + (jlong) offset * (jlong) stride;
            jlong begin = stride > 0 ? at : at - (span - INT64_C(1));
            (*env)->GetByteArrayRegion(env, source, (jsize) begin, (jsize) span, chunk);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < take; i++) {
                jlong cell = stride > 0 ? (jlong) i * magnitude
                        : span - INT64_C(1) - (jlong) i * magnitude;
                buckets[(unsigned char) chunk[(jsize) cell]]++;
            }
            offset += take;
        }
    }
    (*env)->SetIntArrayRegion(env, result, 0, M3_STATE_COUNT, buckets);
}

/* Additive physical-index histogram ABI. Source offsets and state masks retain
 * Java parent ownership. Region copies are bounded to 256 scalars, no critical
 * pins, heap buffers, callbacks, or persistent native pointers.
 */
#define M3_INDEX_HIST_CHUNK 256

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3StateMaskHistogram_indexedAbiNative(
        JNIEnv *env, jclass owner) {
    (void) env;
    (void) owner;
    return 1;
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3StateMaskHistogram_tallyIndexedNative(
        JNIEnv *env, jclass owner, jintArray offsets, jbyteArray states,
        jint first, jint count, jint stride, jintArray result) {
    (void) owner;
    if (offsets == NULL || states == NULL || result == NULL || count < 0 || stride == 0) {
        m3_invalid(env, "invalid indexed histogram inputs");
        return;
    }
    jsize offset_length = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return;
    jsize state_length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return;
    jsize result_length = (*env)->GetArrayLength(env, result);
    if ((*env)->ExceptionCheck(env)) return;
    if (first < 0 || first > offset_length || result_length != M3_STATE_COUNT) {
        m3_invalid(env, "invalid indexed histogram geometry");
        return;
    }
    if (count > 0) {
        jlong last = (jlong) first + ((jlong) count - 1) * (jlong) stride;
        if (first == offset_length || last < 0 || last >= offset_length) {
            m3_invalid(env, "indexed histogram path outside offset lane");
            return;
        }
    }

    jint buckets[M3_STATE_COUNT] = {0};
    jint index_region[M3_INDEX_HIST_CHUNK];
    jbyte state_region[M3_INDEX_HIST_CHUNK];
    jlong cached_page = -1;
    jlong magnitude = stride < 0 ? -(jlong) stride : (jlong) stride;

    for (jint processed = 0; processed < count;) {
        jint take = count - processed;
        if (take > M3_INDEX_HIST_CHUNK) take = M3_INDEX_HIST_CHUNK;
        jlong at = (jlong) first + (jlong) processed * (jlong) stride;
        jlong span = 1 + ((jlong) take - 1) * magnitude;
        jlong index_start = stride > 0 ? at : at - span + 1;
        int dense = span <= M3_INDEX_HIST_CHUNK;
        if (dense) {
            (*env)->GetIntArrayRegion(env, offsets, (jsize) index_start,
                    (jsize) span, index_region);
            if ((*env)->ExceptionCheck(env)) return;
        }
        for (jint i = 0; i < take; i++) {
            jint physical;
            if (dense) {
                jsize element = (jsize) (stride > 0 ? (jlong) i * magnitude
                        : span - 1 - (jlong) i * magnitude);
                physical = index_region[element];
            } else {
                jlong selected = at + (jlong) i * (jlong) stride;
                (*env)->GetIntArrayRegion(env, offsets, (jsize) selected, 1, &physical);
                if ((*env)->ExceptionCheck(env)) return;
            }
            if (physical < 0 || physical >= state_length) {
                m3_invalid(env, "physical offset outside indexed state lane");
                return;
            }
            jlong page = ((jlong) physical / M3_INDEX_HIST_CHUNK) * M3_INDEX_HIST_CHUNK;
            if (page != cached_page) {
                jsize remaining = state_length - (jsize) page;
                jsize chunk = remaining < M3_INDEX_HIST_CHUNK
                        ? remaining : M3_INDEX_HIST_CHUNK;
                (*env)->GetByteArrayRegion(env, states, (jsize) page, chunk, state_region);
                if ((*env)->ExceptionCheck(env)) return;
                cached_page = page;
            }
            buckets[(unsigned char) state_region[(jsize) (physical - cached_page)]]++;
        }
        processed += take;
--- base:

--- theirs (PR):
    } else {
        /* Batch selected points into the existing bounded scratch region. Cast before
         * negation so even INT_MIN strides are safe for empty/singleton paths. */
        jlong magnitude = stride > 0 ? (jlong) stride : -(jlong) stride;
        jint batch_limit = (jint) (INT64_C(1) + (M3_STATE_CHUNK - 1) / magnitude);
        for (jint offset = 0; offset < count;) {
            jint take = count - offset;
            if (take > batch_limit) take = batch_limit;
            jlong span = INT64_C(1) + ((jlong) take - INT64_C(1)) * magnitude;
            jlong at = (jlong) first + (jlong) offset * (jlong) stride;
            jlong begin = stride > 0 ? at : at - (span - INT64_C(1));
            (*env)->GetByteArrayRegion(env, source, (jsize) begin, (jsize) span, chunk);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < take; i++) {
                jlong cell = stride > 0 ? (jlong) i * magnitude
                        : span - INT64_C(1) - (jlong) i * magnitude;
                buckets[(unsigned char) chunk[(jsize) cell]]++;
            }
            offset += take;
        }
--- after:
    }
    (*env)->SetIntArrayRegion(env, result, 0, M3_STATE_COUNT, buckets);

## H13 109ff1deb35a ADD_ADD M3RX7.c prs=1381 1456 1463

context:
    if (packed != NULL) (*env)->ReleaseByteArrayElements(env, packedArray, packed, JNI_ABORT);
    if (dfa != NULL) (*env)->ReleaseShortArrayElements(env, dfaArray, dfa, JNI_ABORT);
}
--- ours (superset):

/* Bounded token-symbol execution; no text decoding or retained Java addresses. */
JNIEXPORT jint JNICALL Java_jdk_internal_mindex_M3RX7_advanceTokens0(
        JNIEnv *env, jclass ignored, jshortArray rowArray, jint tokens, jint state,
        jintArray idArray, jint offset, jint length) {
    (void) ignored;
    if (rowArray == NULL || idArray == NULL) {
        invalid(env, "missing token-symbol arrays");
        return -1;
    }
    jsize cells = (*env)->GetArrayLength(env, rowArray);
    jsize ids = (*env)->GetArrayLength(env, idArray);
    if (tokens <= 0 || cells <= 0 || cells > M3_MAX_CELLS || cells % tokens != 0
            || cells / tokens > M3_MAX_STATES || state < -1 || state >= cells / tokens
            || offset < 0 || length < 0 || length > 4096 || offset > ids
            || length > ids - offset) {
        invalid(env, "invalid token-symbol page geometry");
        return -1;
    }
    jshort *rows = (*env)->GetShortArrayElements(env, rowArray, NULL);
    if (rows == NULL) return -1;
    jint page[256];
    int valid = 1;
    for (jint done = 0; done < length && valid;) {
        jint count = length - done < 256 ? length - done : 256;
        (*env)->GetIntArrayRegion(env, idArray, offset + done, count, page);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
            return -1;
        }
        for (jint i = 0; i < count; i++) {
            if (page[i] < 0 || page[i] >= tokens) { valid = 0; break; }
            if (state >= 0) {
                state = rows[(size_t) state * (size_t) tokens + (size_t) page[i]];
                if (state < -1 || state >= cells / tokens) { valid = 0; break; }
            }
        }
        done += count;
    }
    (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
    if (!valid) {
        invalid(env, "invalid token-symbol ID or transition");
        return -1;
    }
    return state;
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_compose0(
        JNIEnv *env, jclass ignored, jshortArray leftArray,
        jshortArray rightArray, jshortArray outArray) {
    jshort *left = NULL, *right = NULL, *out = NULL;
    int written = 0;
    (void) ignored;
    if (leftArray == NULL || rightArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, leftArray);
    if (states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, rightArray)
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA transfer dimensions");
        return;
    }
    if ((*env)->IsSameObject(env, leftArray, outArray)
            || (*env)->IsSameObject(env, rightArray, outArray)) {
        invalid(env, "DFA transfer output aliases input");
        return;
    }
    left = (*env)->GetShortArrayElements(env, leftArray, NULL);
    if (left == NULL) goto done;
    right = (*env)->GetShortArrayElements(env, rightArray, NULL);
    if (right == NULL) goto done;
    if (!valid_transfer(left, states) || !valid_transfer(right, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    compose_transfer(left, right, out, states);
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (right != NULL) (*env)->ReleaseShortArrayElements(env, rightArray, right, JNI_ABORT);
    if (left != NULL) (*env)->ReleaseShortArrayElements(env, leftArray, left, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_repeat0(
        JNIEnv *env, jclass ignored, jshortArray rowsArray,
        jlong count, jshortArray outArray) {
    jshort *rows = NULL, *out = NULL, *workspace = NULL;
    int written = 0;
    (void) ignored;
    if (rowsArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, rowsArray);
    if (count < 0 || states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA repetition dimensions or count");
        return;
    }
    if ((*env)->IsSameObject(env, rowsArray, outArray)) {
        invalid(env, "DFA repetition output aliases input");
        return;
    }
    rows = (*env)->GetShortArrayElements(env, rowsArray, NULL);
    if (rows == NULL) goto done;
    if (!valid_transfer(rows, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    workspace = (jshort *) malloc((size_t) states * 2 * sizeof(jshort));
    if (workspace == NULL) {
        allocation_failed(env);
        goto done;
    }
    jshort *result = workspace;
    jshort *scratch = workspace + states;
    for (jsize state = 0; state < states; state++) {
        result[state] = count == 0 ? (jshort) state : rows[state];
    }
    /* The leading set bit is represented by rows. Unsigned shifts are defined
       even for the highest admitted exponent, INT64_MAX. */
    uint64_t bit = 0;
    for (uint64_t remaining = (uint64_t) count; remaining > 1; remaining >>= 1) {
        bit = bit == 0 ? UINT64_C(1) : bit << 1;
    }
    for (; bit != 0; bit >>= 1) {
        compose_transfer(result, result, scratch, states);
        jshort *previous = result;
        result = scratch;
        scratch = previous;
        if (((uint64_t) count & bit) != 0) {
            compose_transfer(result, rows, scratch, states);
            previous = result;
            result = scratch;
            scratch = previous;
        }
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    for (jsize state = 0; state < states; state++) out[state] = result[state];
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    free(workspace);
    if (rows != NULL) (*env)->ReleaseShortArrayElements(env, rowsArray, rows, JNI_ABORT);
}
--- base:

--- theirs (PR):

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_compose0(
        JNIEnv *env, jclass ignored, jshortArray leftArray,
        jshortArray rightArray, jshortArray outArray) {
    jshort *left = NULL, *right = NULL, *out = NULL;
    int written = 0;
    (void) ignored;
    if (leftArray == NULL || rightArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, leftArray);
    if (states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, rightArray)
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA transfer dimensions");
        return;
    }
    if ((*env)->IsSameObject(env, leftArray, outArray)
            || (*env)->IsSameObject(env, rightArray, outArray)) {
        invalid(env, "DFA transfer output aliases input");
        return;
    }
    left = (*env)->GetShortArrayElements(env, leftArray, NULL);
    if (left == NULL) goto done;
    right = (*env)->GetShortArrayElements(env, rightArray, NULL);
    if (right == NULL) goto done;
    if (!valid_transfer(left, states) || !valid_transfer(right, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    compose_transfer(left, right, out, states);
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (right != NULL) (*env)->ReleaseShortArrayElements(env, rightArray, right, JNI_ABORT);
    if (left != NULL) (*env)->ReleaseShortArrayElements(env, leftArray, left, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_repeat0(
        JNIEnv *env, jclass ignored, jshortArray rowsArray,
        jlong count, jshortArray outArray) {
    jshort *rows = NULL, *out = NULL, *workspace = NULL;
    int written = 0;
    (void) ignored;
    if (rowsArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, rowsArray);
    if (count < 0 || states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA repetition dimensions or count");
        return;
    }
    if ((*env)->IsSameObject(env, rowsArray, outArray)) {
        invalid(env, "DFA repetition output aliases input");
        return;
    }
    rows = (*env)->GetShortArrayElements(env, rowsArray, NULL);
    if (rows == NULL) goto done;
    if (!valid_transfer(rows, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    workspace = (jshort *) malloc((size_t) states * 2 * sizeof(jshort));
    if (workspace == NULL) {
        allocation_failed(env);
        goto done;
    }
    jshort *result = workspace;
    jshort *scratch = workspace + states;
    for (jsize state = 0; state < states; state++) {
        result[state] = count == 0 ? (jshort) state : rows[state];
    }
    /* The leading set bit is represented by rows. Unsigned shifts are defined
       even for the highest admitted exponent, INT64_MAX. */
    uint64_t bit = 0;
    for (uint64_t remaining = (uint64_t) count; remaining > 1; remaining >>= 1) {
        bit = bit == 0 ? UINT64_C(1) : bit << 1;
    }
    for (; bit != 0; bit >>= 1) {
        compose_transfer(result, result, scratch, states);
        jshort *previous = result;
        result = scratch;
        scratch = previous;
        if (((uint64_t) count & bit) != 0) {
            compose_transfer(result, rows, scratch, states);
            previous = result;
            result = scratch;
            scratch = previous;
        }
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    for (jsize state = 0; state < states; state++) out[state] = result[state];
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    free(workspace);
    if (rows != NULL) (*env)->ReleaseShortArrayElements(env, rowsArray, rows, JNI_ABORT);
}
--- after:


## H14 03e23095d01c MOD_MOD m3_index_mask_compactor.c prs=1538 1546

context:
                span = (jsize)offsets[accepted - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        if (accepted > 0) {
            (*env)->GetIntArrayRegion(env, rows, first + offset + first_match, span, row_ids);
            if ((*env)->ExceptionCheck(env)) return NULL;
            if (required_mask != 0) {
                for (jsize i = 0; i < accepted; i++) {
                    row_ids[i] = row_ids[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H15 275b8b504b16 MOD_MOD m3_index_mask_compactor.c prs=1538 1546

context:
                span = (jsize)offsets[batch - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- base:
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- theirs (PR):
        if (batch > 0) {
            (*env)->GetLongArrayRegion(env, rows, row_offset + position + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            /* Sorted ordinals keep each destination at or before its unread source. */
            if (mask != 0) {
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H16 5db35ce81858 SAME m3_index_mask_compactor.c prs=1515 1616

context:
    return 1;
}

--- ours (superset):

--- base:
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}
--- theirs (PR):

--- after:

/* ABI probe for direct logical-position selection from the state lane. */

## H17 73f1cbe2b0b4 MOD_MOD m3_index_mask_compactor.c prs=1538 1546

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    unsigned char offsets[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        jsize batch = chunk;
        jsize first_match = 0;
        jsize span = chunk;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            batch = m3_matching_offsets(marks, chunk, mask, offsets);
            if (batch > 0) {
                first_match = (jsize)offsets[0];
                span = (jsize)offsets[batch - 1] - first_match + 1;
            }
        }
        if (batch > 0) {
            (*env)->GetIntArrayRegion(env, rows, row_offset + position + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            /* Sorted ordinals keep each destination at or before its unread source. */
            if (mask != 0) {
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)offsets[i] - first_match];
                }
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H18 8e4cb74e1ecf MOD_MOD m3_index_mask_compactor.c prs=1368 1375

context:
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
--- ours (superset):
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- base:
                if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
--- theirs (PR):
                /* The accepted prefix never overtakes this input ordinal. */
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- after:
            }
        }

## H19 b7c7ff187d60 MOD_MOD m3_index_mask_compactor.c prs=1479 1522

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    if (kept == 0) {
        return m3_verify_empty(env, states, state_offset, length, mask) ? result : NULL;
    }
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H20 da566c926d7b MOD_MOD m3_index_mask_compactor.c prs=1368 1375

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint accepted[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, mask == 0 ? values : accepted);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    if (kept == 0) {
        return m3_verify_empty(env, states, state_offset, length, mask) ? result : NULL;
    }
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                /* The accepted prefix never overtakes this input ordinal. */
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H21 c8d5c8a8d1aa MOD_MOD m3_index_mask_window.c prs=1292 1375

context:
>>>>>>> 6f15b4990435a69de2baba574f56ab6c32cc4008
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
--- ours (superset):
                offsets[matches++] = (unsigned char) consumed;
            }
            consumed++;
        }
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
--- base:
                selected[pending++] = values[i];
--- theirs (PR):
                selected[pending++] = values[i - first_match];
--- after:
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {

## H22 36104faf3295 MOD_MOD m3_index_path_masks.c prs=1294 1368

context:
    return 1;
}

--- ours (superset):
/* ART-friendly bounded metadata cache: no heap allocation or pinned Java pointers.
 * Stage count above the bound follows the unchanged original JNI reference kernel.
 * All local references are released on every normal/exceptional exit.
 */
#define M3_CACHED_STAGE_LIMIT 8

typedef struct {
    jintArray plane[M3_CACHED_STAGE_LIMIT];
    jsize length[M3_CACHED_STAGE_LIMIT];
    jint needs[M3_CACHED_STAGE_LIMIT];
    jint denies[M3_CACHED_STAGE_LIMIT];
    jsize count;
} M3StageCache;

static void m3_release_stages(JNIEnv *env, M3StageCache *cache) {
    for (jsize i = 0; i < cache->count; ++i) {
        if (cache->plane[i] != NULL) {
            (*env)->DeleteLocalRef(env, cache->plane[i]);
            cache->plane[i] = NULL;
        }
    }
}

static int m3_prepare_stages(JNIEnv *env, jobjectArray planes,
        jintArray required, jintArray forbidden, jsize count, M3StageCache *cache) {
    cache->count = count;
    for (jsize i = 0; i < count; ++i) {
        (*env)->GetIntArrayRegion(env, required, i, 1, &cache->needs[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
        (*env)->GetIntArrayRegion(env, forbidden, i, 1, &cache->denies[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
        jobject reference = (*env)->GetObjectArrayElement(env, planes, i);
        if ((*env)->ExceptionCheck(env)) goto failure;
        if (reference == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            goto failure;
        }
        cache->plane[i] = (jintArray) reference;
        cache->length[i] = (*env)->GetArrayLength(env, cache->plane[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
    }
    return 1;
failure:
    m3_release_stages(env, cache);
    return 0;
}

/* -1 is an exception; 0 and 1 are rejected and admitted respectively. */
static int m3_matches_cached(JNIEnv *env, const M3StageCache *cache, jint slot) {
    for (jsize i = 0; i < cache->count; ++i) {
        if (slot < 0 || slot >= cache->length[i]) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException",
                    "physical offset outside state plane");
            return -1;
        }
        jint state;
        (*env)->GetIntArrayRegion(env, cache->plane[i], slot, 1, &state);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((state & cache->needs[i]) != cache->needs[i]
                || (state & cache->denies[i]) != 0) return 0;
    }
    return 1;
}

static jintArray m3_select_cached(JNIEnv *env, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride, jsize stages) {
    M3StageCache cache = {0};
    if (!m3_prepare_stages(env, planes, required, forbidden, stages, &cache)) return NULL;
    jintArray result = NULL;
    jsize matching = 0;
    for (jint pass = 0; pass < 2; ++pass) {
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL) goto failure;
        }
        jsize written = 0;
        jlong at = first;
        for (jint ordinal = 0; ordinal < count; ++ordinal, at += stride) {
            jint physical;
            (*env)->GetIntArrayRegion(env, offsets, (jsize)at, 1, &physical);
            if ((*env)->ExceptionCheck(env)) goto failure;
            int keep = m3_matches_cached(env, &cache, physical);
            if (keep < 0) goto failure;
            if (keep == 0) continue;
            if (pass == 0) {
                ++matching;
            } else {
                if (written >= matching) {
                    m3_throw(env, "java/util/ConcurrentModificationException",
                            "mask stages changed during JNI selection");
                    goto failure;
                }
                jint row;
                (*env)->GetIntArrayRegion(env, rows, (jsize)at, 1, &row);
                if ((*env)->ExceptionCheck(env)) goto failure;
                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);
                if ((*env)->ExceptionCheck(env)) goto failure;
            }
        }
        if (pass == 1) {
            if (written != matching) {
                m3_throw(env, "java/util/ConcurrentModificationException",
                        "mask stages changed during JNI selection");
                goto failure;
            }
            m3_release_stages(env, &cache);
            return result;
        }
    }
failure:
    m3_release_stages(env, &cache);
    return NULL;
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,
--- base:
JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,
--- theirs (PR):
/** Shared exact two-pass selection core for int[] and long[] row IDs. */
static jarray m3_select(JNIEnv *env, jarray rows, jintArray offsets,
--- after:
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride, int long_rows) {

## H23 3bd81deb573d MOD_MOD classLoader.cpp prs=1424 1426

context:
}

int ClassLoader::compute_Object_vtable() {
--- ours (superset):
  // Object declares five legacy virtual methods and M3's non-final toAdapter.
  // This count must match java.lang.Object before array klasses are created.
  const int Object_vtable_size = 6;
  return Object_vtable_size * vtableEntry::size();
--- base:
  // hardwired for JDK1.2 -- would need to duplicate class file parsing
  // code to determine actual value from file
  // Would be value '11' if finals were in vtable
  int JDK_1_2_Object_vtable_size = 5;
  return JDK_1_2_Object_vtable_size * vtableEntry::size();
--- theirs (PR):
  // M3JDK21 Object has six non-final instance methods: hashCode, equals,
  // clone, toString, finalize, and toAdapter. Reserve their slots before
  // Object is loaded so early array klass metadata has the same vtable size.
  const int M3_Object_vtable_size = 6;
  return M3_Object_vtable_size * vtableEntry::size();
--- after:
}


## H24 d917bc046b0c ADD_ADD M3StateRelation.c prs=1522 1619

context:
#include <jni.h>
#include <string.h>

--- ours (superset):
/* Only called for nonzero state masks; the builtin is undefined at zero.
 * The portable lane has at most six tests, independent of the highest set bit. */
static unsigned int relation_trailing_zeros(uint64_t bits) {
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_STATE_RELATION_PORTABLE_CTZ)
  return (unsigned int) __builtin_ctzll((unsigned long long) bits);
#else
  unsigned int count = 0;
  if ((bits & UINT64_C(0xffffffff)) == 0) { count += 32; bits >>= 32; }
  if ((bits & UINT64_C(0xffff)) == 0) { count += 16; bits >>= 16; }
  if ((bits & UINT64_C(0xff)) == 0) { count += 8; bits >>= 8; }
  if ((bits & UINT64_C(0xf)) == 0) { count += 4; bits >>= 4; }
  if ((bits & UINT64_C(0x3)) == 0) { count += 2; bits >>= 2; }
  if ((bits & UINT64_C(0x1)) == 0) ++count;
  return count;
#endif
}


--- base:

--- theirs (PR):
#if !defined(M3_RELATION_PORTABLE_SCAN) && (defined(__GNUC__) || defined(__clang__))
/* Reuse the existing regex atom bit-scan on nonzero masks. Other compilers keep
 * this owner's original shift loop, avoiding repeated portable prefix scans. */
static uint32_t relation_lowest_state(uint64_t bits) {
  return (uint32_t)__builtin_ctzll((unsigned long long)bits);
}

int m3_state_relation_compose(const uint64_t *left, const uint64_t *right,
                                 uint64_t *output, size_t states) {
  uint64_t temporary[64];
  uint64_t mask;
  uint64_t ceiling = 0;
  uint64_t previous = 0;
  uint64_t value = 0;
  if (left == NULL || right == NULL || output == NULL || states == 0 || states > 64) {
    return -1;
  }
  mask = states == 64 ? UINT64_MAX : (UINT64_C(1) << states) - UINT64_C(1);
  for (size_t row = 0; row < states; ++row) {
    if (((left[row] | right[row]) & ~mask) != 0) return -1;
    ceiling |= right[row];
  }
  for (size_t row = 0; row < states; ++row) {
    /* No source row is written before complete validation/computation, preserving aliases.
     * Repeated frontiers reuse an exact invocation-local result, never a hash/filter hit. */
    const uint64_t active = left[row];
    if (row == 0 || active != previous) {
      uint64_t bits = active;
      /* The full incoming frontier selects exactly the precomputed union. */
      value = active == mask ? ceiling : 0;
      while (bits != 0 && value != ceiling) {
        value |= right[relation_lowest_state(bits)];
        bits &= bits - UINT64_C(1);
      }

      previous = active;
    }
    temporary[row] = value;
  }
  memcpy(output, temporary, states * sizeof(uint64_t));
  return 0;
}

#else
/* Exact original composition body on compilers without the intrinsic. */
--- after:
int m3_state_relation_compose(const uint64_t *left, const uint64_t *right,
                                 uint64_t *output, size_t states) {

## H25 1811e90862ef ADD_ADD m3maskviewport.c prs=1497

context:
    return (value & yes) == yes && (value & no) == 0U;
}

--- ours (superset):
/* Never acquire or retain a pointer to either complete Java input array. */
static int read_chunk(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, int read_states, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (!read_states) return 1;
    (*env)->GetIntArrayRegion(env, flags, first, length, states);
    return !(*env)->ExceptionCheck(env);
}

--- base:

--- theirs (PR):
/* Never retain a pin or copy of an entire parent-owned array. */
static int read_region(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    (*env)->GetIntArrayRegion(env, flags, first, length, states);
    return !(*env)->ExceptionCheck(env);
}

--- after:
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_project(JNIEnv *env, jclass cls,

## H26 54b0c82b435d MOD_MOD m3maskviewport.c prs=1497

context:
    jint src[REGION_SIZE], states[REGION_SIZE], output[REGION_SIZE];
>>>>>>> d0cec9717801b9f81f308053dd1648ba43a25af9
    jint selected = 0;
--- ours (superset):
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
                if (selected < M3_VIEWPORT_CHUNK) inlineSelection[selected] = src[i];
                ++selected;
            }
--- base:
    for (jint i = first; i < first + length; ++i) {
        if (src[i] < 0) {
            raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
            goto release;
        }
        if (matches(states[i], required, forbidden)) {
            ++selected;
--- theirs (PR):
    const jint end = first + length;
    /* Finish every validation before writing, including invalid unselected
       offsets in later regions and capacity failures after a full scan. */
    for (jint start = first; start < end;) {
        jint n = end - start < REGION_SIZE ? end - start : REGION_SIZE;
        if (!read_region(env, offsets, flags, start, n, src, states)) return 0;
        for (jint i = 0; i < n; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (matches(states[i], required, forbidden)) ++selected;
--- after:
        }
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H27 8f7ecec7bda6 MOD_MOD m3maskviewport.c prs=1497

context:
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
--- ours (superset):
    if (selected == 0) return 0;
    if (selected <= M3_VIEWPORT_CHUNK) {
        (*env)->SetIntArrayRegion(env, target, targetOffset, selected, inlineSelection);
        return (*env)->ExceptionCheck(env) ? 0 : selected;
    }
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
    /* Keep the original O(selected) scratch and single-commit writeback.
     * Alias and JNI-read failures must not publish a partially selected result. */
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        return 0;
    }
    jint cursor = 0;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        /* Complete preflight proves a dense window under the existing stable-
         * input contract. Do not fetch state bytes twice when every row passes. */
        if (selected == length) {
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
            begin += take;
            continue;
--- base:
    if (selected == 0) {
        goto release;
    }
    /* Never mutate the destination until every possible input validation
       has succeeded, including aliasing of target with either input array. */
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        goto release;
    }
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        goto release;
    }
    jint cursor = 0;
    for (jint i = first; i < first + length; ++i) {
        if (matches(states[i], required, forbidden)) {
            scratch[cursor++] = src[i];
--- theirs (PR):
    if (selected == 0) return 0;

    /* All sums are bounded by validated array lengths. Disjoint windows on
       one array are as safe to stream as different destination arrays. */
    const int overlaps = targetOffset < end && first < targetOffset + selected;
    const int snapshot = overlaps &&
            ((*env)->IsSameObject(env, target, offsets) ||
             (*env)->IsSameObject(env, target, flags));
    jint *scratch = NULL;
    if (snapshot) {
        if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
            raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
            return 0;
        }
        scratch = (jint *) malloc((size_t) selected * sizeof(jint));
        if (scratch == NULL) {
            raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
            return 0;
--- after:
        }
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {

## H28 ac470156c398 MOD_MOD m3maskviewport.c prs=1497

context:
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
--- ours (superset):
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Reuse page capture for a small full selection. Keep all input reads and
     * validation before publication, including late errors and aliased output. */
    jint inlineSelection[M3_VIEWPORT_CHUNK];
--- base:
    jint *src = (*env)->GetIntArrayElements(env, offsets, NULL);
    if (src == NULL) {
        return 0; /* JVM has an exception pending. */
    }
    jint *states = (*env)->GetIntArrayElements(env, flags, NULL);
    if (states == NULL) {
        (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
        return 0;
    }
--- theirs (PR):
    jint src[REGION_SIZE], states[REGION_SIZE], output[REGION_SIZE];
--- after:
    jint selected = 0;
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H29 c2afb8705ffe THEIRS_EMPTY m3maskviewport.c prs=1497

context:
    }
>>>>>>> d0cec9717801b9f81f308053dd1648ba43a25af9
    return (*env)->ExceptionCheck(env) ? 0 : selected;
--- ours (superset):
}

/* The Java page contract validates the full input even for an empty result.
 * Publication is one region write, so overlapping owners need only page scratch. */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_projectPage(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jint skipVisible, jint maxVisible,
        jintArray target, jint targetOffset) {
    (void) cls;
    if (offsets == NULL) {
        raise(env, "java/lang/NullPointerException", "logicalOffsets");
        return 0;
    }
    if (flags == NULL) {
        raise(env, "java/lang/NullPointerException", "states");
        return 0;
    }
    if (target == NULL) {
        raise(env, "java/lang/NullPointerException", "target");
        return 0;
    }
    const jsize sourceSize = (*env)->GetArrayLength(env, offsets);
    const jsize flagsSize = (*env)->GetArrayLength(env, flags);
    const jsize outputSize = (*env)->GetArrayLength(env, target);
    if (sourceSize != flagsSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if (first < 0 || length < 0 || first > sourceSize - length ||
            targetOffset < 0 || targetOffset > outputSize) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
    if (skipVisible < 0 || maxVisible < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "negative visible page bounds");
        return 0;
    }
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Capture a bounded page while validating. This buffer is call-local,
     * never retained and never published before complete preflight succeeds. */
    jint inlinePage[M3_VIEWPORT_CHUNK];
    jint available = 0;
    jint needed = 0;
    jint pageFirst = end;
    jint pageEnd = end;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
                /* No skip+limit arithmetic: both can independently be INT_MAX. */
                if (available >= skipVisible && needed < maxVisible) {
                    if (needed == 0) pageFirst = begin + i;
                    pageEnd = begin + i + 1;
                    if (needed < M3_VIEWPORT_CHUNK) inlinePage[needed] = src[i];
                    ++needed;
                }
                ++available;
            }
        }
        begin += take;
    }
    if (needed > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (needed == 0) return 0;
    if (needed <= M3_VIEWPORT_CHUNK) {
        /* A small returned page needs neither heap scratch nor a second input
         * pass. Source/target aliases are safe: all input reads are complete. */
        (*env)->SetIntArrayRegion(env, target, targetOffset, needed, inlinePage);
        return (*env)->ExceptionCheck(env) ? 0 : needed;
    }
    if ((size_t) needed > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
    jint *scratch = (jint *) malloc((size_t) needed * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        return 0;
    }
    jint cursor = 0;
    for (jint begin = pageFirst; begin < pageEnd;) {
        jint take = pageEnd - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (pageEnd - pageFirst == needed) {
            /* Preflight proves this page is contiguous even in a sparse window.
             * Only its offsets are needed on the second pass. */
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
        } else {
            if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {
                free(scratch);
                return 0;
            }
            for (jint i = 0; i < take; ++i) {
                if (all_states || (read_states && matches(states[i], required, forbidden))) {
                    if (cursor == needed) {
                        free(scratch);
                        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
                        return 0;
                    }
                    scratch[cursor++] = src[i];
                }
            }
        }
        begin += take;
    }
    if (cursor != needed) {
        free(scratch);
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
    (*env)->SetIntArrayRegion(env, target, targetOffset, needed, scratch);
    free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : needed;
--- base:
release:
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    return 0;
--- theirs (PR):

--- after:
}


## H30 eb3a30cf561b ADD_ADD m3maskviewport.c prs=1497

context:
            if (matches(states[i], required, forbidden)) ++selected;
>>>>>>> d0cec9717801b9f81f308053dd1648ba43a25af9
        }
--- ours (superset):
        begin += take;
--- base:

--- theirs (PR):
        start += n;
--- after:
    }
    if (selected > outputSize - targetOffset) {

## H31 ebb28270d398 ADD_ADD m3maskviewport.c prs=1497

context:
#include <stdint.h>
#include <stdlib.h>

--- ours (superset):
/* Reuse the existing mask-window owner's bounded-region pattern, not its
 * byte-mask/alias contract. These inputs retain all 32 state bits. */
#define M3_VIEWPORT_CHUNK 128

--- base:

--- theirs (PR):
/* A transfer bound, never a bound on the caller's logical window. */
enum { REGION_SIZE = 1024 };

--- after:
static void raise(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);

## H32 fae437dd1418 MOD_MOD m3maskviewport.c prs=1497

context:
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
--- ours (superset):
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
    free(scratch);
--- base:
    /* Pinned/copy source elements must be released before the writeback,
       so target-source aliasing does not let an abort undo output. */
    (*env)->ReleaseIntArrayElements(env, flags, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, offsets, src, JNI_ABORT);
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
    free(scratch);
--- theirs (PR):
    jint cursor = 0;
    for (jint start = first; start < end;) {
        jint n = end - start < REGION_SIZE ? end - start : REGION_SIZE;
        if (!read_region(env, offsets, flags, start, n, src, states)) {
            free(scratch);
            return 0;
        }
        jint used = 0;
        for (jint i = 0; i < n; ++i) {
            if (matches(states[i], required, forbidden)) {
                if (snapshot) scratch[cursor + used] = src[i];
                else output[used] = src[i];
                ++used;
            }
        }
        if (!snapshot && used != 0) {
            (*env)->SetIntArrayRegion(env, target, targetOffset + cursor, used, output);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        cursor += used;
        start += n;
    }
    if (snapshot) {
        (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
        free(scratch);
    }
--- after:
    return (*env)->ExceptionCheck(env) ? 0 : selected;
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H33 1c517f437f97 MOD_MOD m3_index_mask_compactor.c prs=1577

context:
                span = (jsize)offsets[batch - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- base:
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- theirs (PR):
        if (batch > 0) {
            (*env)->GetLongArrayRegion(env, rows, row_offset + position + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            /* Sorted ordinals keep each destination at or before its unread source. */
            /* A contiguous accepted span is already packed by the region read. */
            if (batch < span) {
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H34 2c5e52f2237f MOD_MOD m3_index_mask_compactor.c prs=1577

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    unsigned char offsets[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        jsize batch = chunk;
        jsize first_match = 0;
        jsize span = chunk;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
            batch = m3_matching_offsets(marks, chunk, mask, offsets);
            if (batch > 0) {
                first_match = (jsize)offsets[0];
                span = (jsize)offsets[batch - 1] - first_match + 1;
            }
        }
        if (batch > 0) {
            (*env)->GetIntArrayRegion(env, rows, row_offset + position + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            /* Sorted ordinals keep each destination at or before its unread source. */
            /* A contiguous accepted span is already packed by the region read. */
            if (batch < span) {
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)offsets[i] - first_match];
                }
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H35 2dd2b5a9a6ec MOD_MOD m3_index_mask_compactor.c prs=1515

context:
                span = (jsize)offsets[batch - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- base:
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
--- theirs (PR):
        if (batch > 0) {
            (*env)->GetLongArrayRegion(env, rows, row_offset + position + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            if (mask != 0 && batch != chunk) {
                /* offsets[i]-first_match >= i: forward compaction preserves unread slots. */
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H36 2e9e4acc4772 MOD_MOD m3_index_mask_compactor.c prs=1513

context:
        (*env)->GetIntArrayRegion(env, rows, first + offset + begin, stop - begin, row_ids);
>>>>>>> ada1ab5b3b6791bddc58892ad3ad7b03b6efb32a
        if ((*env)->ExceptionCheck(env)) return NULL;
--- ours (superset):
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        jsize accepted = 0;
        for (jsize i = begin; i < stop; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i - begin];
--- after:
            }
        }

## H37 3c236ba486b1 MOD_MOD m3_index_mask_compactor.c prs=1616

context:
    }
}

--- ours (superset):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
--- base:
/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- theirs (PR):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal; zero-mask never reads uninitialized marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. All bounds validated by exports.
--- after:
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */

## H38 62e757a56ffa MOD_MOD m3_index_mask_compactor.c prs=1515

context:
    }
}

--- ours (superset):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
--- base:
/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- theirs (PR):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* Emit-local relative ordinals; a 256-element chunk has offsets 0..255.
 * Donor #1538 selection spans composed with #1547 unsigned mask semantics. */
static jsize m3_matching_offsets(const jbyte *marks, jsize chunk, jint mask,
        unsigned char *offsets) {
    jsize accepted = 0;
    for (jsize i = 0; i < chunk; i++) {
        if (m3_jni_matches_u8(marks[i], mask, 0)) {
            offsets[accepted++] = (unsigned char)i;
        }
    }
    return accepted;
}

/* One core for legacy whole-index and independent-offset windows. All bounds validated by exports.
--- after:
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */

## H39 64ff5edcd3ae MOD_MOD m3_index_mask_compactor.c prs=1513

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, mask, &stop);
        if (begin == stop) { position += chunk; continue; }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position + begin, stop - begin, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = stop - begin;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i - begin, so unread values stay intact. */
            batch = 0;
            for (jsize i = begin; i < stop; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i - begin];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H40 735d866aef9b MOD_MOD m3_index_mask_compactor.c prs=1513

context:
            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
--- base:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
--- theirs (PR):
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, required_mask, &stop);
        if (begin == stop) { offset += chunk; continue; }
        (*env)->GetIntArrayRegion(env, rows, first + offset + begin, stop - begin, row_ids);
--- after:
        if ((*env)->ExceptionCheck(env)) return NULL;
<<<<<<< 5e4859b81981e7469d855a1b5ff03eaa41ff429c

## H41 750778c89cf6 MOD_MOD m3_index_mask_compactor.c prs=1627

context:
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
--- base:
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
--- theirs (PR):
        if (kept != 0) {
            (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- after:
        jsize batch = chunk;
        if (mask != 0) {

## H42 8d27eb38508b MOD_MOD m3_index_mask_compactor.c prs=1627

context:
            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        if (count != 0) {
            (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                if (count != 0) row_ids[accepted] = row_ids[i];
                accepted++;
--- after:
            }
        }

## H43 90791d9af839 MOD_MOD m3_index_mask_compactor.c prs=1513

context:
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i - begin, so unread values stay intact. */
            batch = 0;
--- ours (superset):
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
--- base:
            for (jsize i = 0; i < chunk; i++) {
--- theirs (PR):
            for (jsize i = begin; i < stop; i++) {
--- after:
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i - begin];

## H44 9375fd4facf8 MOD_MOD m3_index_mask_compactor.c prs=1619

context:
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
--- ours (superset):
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
--- base:
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
--- theirs (PR):
        jsize first_admitted, span;
        jsize batch = m3_admit_chunk(env, states, state_offset + position, chunk,
                mask, marks, &first_admitted, &span);
        if (batch < 0) return NULL;
--- after:
        if (batch > kept - written) {
            m3_throw_state_drift(env);

## H45 96533223e094 MOD_MOD m3_index_mask_compactor.c prs=1619

context:
    jsize written = 0;
    for (jsize offset = 0; offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
--- ours (superset):
        if (required_mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
            }
        }
--- base:
        if (required_mask != 0) {
            (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
            }
        }
--- theirs (PR):
        jsize first_admitted, span;
        jsize accepted = m3_admit_chunk(env, states, offset, chunk, required_mask,
                marks, &first_admitted, &span);
        if (accepted < 0) return NULL;
--- after:
        if (accepted > count - written) {
            m3_throw_state_drift(env);

## H46 b5fd15e47f38 MOD_MOD m3_index_mask_compactor.c prs=1513

context:
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, mask);
        if (begin == chunk) {
            position += chunk;
            continue;
        }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
--- base:
        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);
--- theirs (PR):
        jsize stop;
        jsize begin = m3_selected_span(marks, chunk, mask, &stop);
        if (begin == stop) { position += chunk; continue; }
        (*env)->GetLongArrayRegion(env, rows, row_offset + position + begin, stop - begin, values);
--- after:
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = stop - begin;

## H47 ba79280137c0 MOD_MOD m3_index_mask_compactor.c prs=1603

context:
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
--- ours (superset):
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        /* A zero mask selects this entire chunk, already in output order. */
        jsize accepted = required_mask == 0 ? chunk : 0;
        if (required_mask != 0) {
            for (jsize i = 0; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- after:
            }
        }

## H48 c8f811274973 MOD_MOD m3_index_mask_compactor.c prs=1619

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        jsize first_admitted, span;
        jsize batch = m3_admit_chunk(env, states, state_offset + position, chunk,
                mask, marks, &first_admitted, &span);
        if (batch < 0) return NULL;
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            (*env)->GetIntArrayRegion(env, rows, row_offset + position + first_admitted,
                    span, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            if (mask != 0) {
                for (jsize i = 0; i < batch; i++) {
                    values[i] = values[(jsize)(unsigned char)marks[i] - first_admitted];
                }
            }
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H49 ccae0d99039b MOD_MOD m3_index_mask_compactor.c prs=1513

context:
    }
}

--- ours (superset):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
--- base:
/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- theirs (PR):
/* Reuse the state lane to bound each row fetch. Empty selection chunks never
 * cross JNI for row values. Bounds are local offsets; no heap scratch or new ABI.
 */
static jsize m3_selected_span(const jbyte *marks, jsize length, jint mask, jsize *end) {
    jsize first = 0;
    *end = length;
    if (mask == 0) return first;
    while (first < length && (((unsigned char) marks[first] & (unsigned int) mask) != (unsigned int) mask)) first++;
    if (first == length) { *end = first; return first; }
    while (*end > first && (((unsigned char) marks[*end - 1] & (unsigned int) mask) != (unsigned int) mask)) (*end)--;
    return first;
}

/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- after:
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */

## H50 d0f78b446de5 MOD_MOD m3_index_mask_compactor.c prs=1577

context:
                span = (jsize)offsets[accepted - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        if (accepted > 0) {
            (*env)->GetIntArrayRegion(env, rows, first + offset + first_match, span, row_ids);
            if ((*env)->ExceptionCheck(env)) return NULL;
            /* A contiguous accepted span is already packed by the region read. */
            if (accepted < span) {
                for (jsize i = 0; i < accepted; i++) {
                    row_ids[i] = row_ids[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H51 dbd5339094fe ADD_ADD m3_index_mask_compactor.c prs=1616

context:
            (*env)->GetByteArrayRegion(env, states, state_first + offset, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
--- base:

--- theirs (PR):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Preserve the second state scan and count-drift guard, omit rejected payload. */
            offset += chunk;
            continue;
        }
--- after:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;

## H52 dfa5fd2c05b9 MOD_MOD m3_index_mask_compactor.c prs=1515

context:
                span = (jsize)offsets[accepted - 1] - first_match + 1;
            }
        }
--- ours (superset):
        jsize begin = m3_first_selected(marks, chunk, required_mask);
        if (begin == chunk) {
            /* Keep the second state scan/drift check, but do not fetch rejected payload. */
            offset += chunk;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = chunk;
        if (required_mask != 0) {
            accepted = 0;
            for (jsize i = begin; i < chunk; i++) {
                unsigned int bits = (unsigned int)(unsigned char)marks[i];
                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                    row_ids[accepted++] = row_ids[i];
                }
--- base:
        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize accepted = 0;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = required_mask == 0 ? 0u : (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {
                row_ids[accepted++] = row_ids[i];
--- theirs (PR):
        if (accepted > 0) {
            (*env)->GetIntArrayRegion(env, rows, first + offset + first_match, span, row_ids);
            if ((*env)->ExceptionCheck(env)) return NULL;
            if (required_mask != 0 && accepted != chunk) {
                for (jsize i = 0; i < accepted; i++) {
                    row_ids[i] = row_ids[(jsize)offsets[i] - first_match];
                }
--- after:
            }
        }

## H53 e54543f4fe8b MOD_MOD m3_index_mask_compactor.c prs=1515

context:
        jsize first, jsize state_first, jsize n, jint required_mask) {
    jbyte marks[M3_CHUNK];
    jint row_ids[M3_CHUNK];
--- ours (superset):
    jsize count = m3_count_window(env, states, state_first, n, required_mask);
    if (count < 0) return NULL;
--- base:
    jsize count = required_mask == 0 ? n : 0;
    for (jsize offset = 0; required_mask != 0 && offset < n;) {
        jsize chunk = n - offset < M3_CHUNK ? n - offset : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, offset, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int bits = (unsigned int)(unsigned char)marks[i];
            if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) count++;
        }
        offset += chunk;
    }
--- theirs (PR):
    unsigned char offsets[M3_CHUNK];
    jsize count = m3_count_window(env, states, state_first, n, required_mask);
    if (count < 0) return NULL;
--- after:
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL) return NULL;

## H54 e7b8308ee621 MOD_MOD m3_index_mask_compactor.c prs=1366

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint accepted[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, mask == 0 ? values : accepted);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    if (kept == 0) {
        return m3_verify_empty(env, states, state_offset, length, mask) ? result : NULL;
    }
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint accepted[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) accepted[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, mask == 0 ? values : accepted);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H55 e8160f3296ec MOD_MOD m3_index_mask_compactor.c prs=1627

context:
    if (!m3_window_valid(env, rows, states, row_offset, state_offset, length, mask)) {
        return NULL;
    }
--- ours (superset):
    return m3_select_range(env, rows, states, row_offset, state_offset, length, mask);
--- base:
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- theirs (PR):
    jint kept = m3_count_window(env, states, state_offset, length, mask);
    if (kept < 0) return NULL;
    jintArray result = (*env)->NewIntArray(env, kept);
    if (result == NULL) return NULL;
    jbyte marks[M3_CHUNK];
    jint values[M3_CHUNK];
    jint written = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        if (mask != 0) {
            (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        if (kept != 0) {
            (*env)->GetIntArrayRegion(env, rows, row_offset + position, chunk, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
        jsize batch = chunk;
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
            for (jsize i = 0; i < chunk; i++) {
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) {
                    if (kept != 0) values[batch] = values[i];
                    batch++;
                }
            }
        }
        if (batch > kept - written) {
            m3_throw_state_drift(env);
            return NULL;
        }
        if (batch > 0) {
            /* Zero-mask identity copies the row chunk without reading aligned states. */
            (*env)->SetIntArrayRegion(env, result, written, batch, values);
            if ((*env)->ExceptionCheck(env)) return NULL;
            written += batch;
        }
        position += chunk;
    }
    if (written != kept) {
        m3_throw_state_drift(env);
        return NULL;
    }
    return result;
--- after:
}


## H56 f068c37ad456 MOD_MOD m3_index_mask_compactor.c prs=1619

context:
    }
}

--- ours (superset):
static jint m3_count_window(JNIEnv *env, jbyteArray states,
        jint state_offset, jint length, jint mask) {
    if (mask == 0) return length;
    jbyte marks[M3_CHUNK];
    jint kept = 0;
    for (jint position = 0; position < length;) {
        jint remaining = length - position;
        jsize chunk = remaining < M3_CHUNK ? remaining : M3_CHUNK;
        (*env)->GetByteArrayRegion(env, states, state_offset + position, chunk, marks);
        if ((*env)->ExceptionCheck(env)) return -1;
        for (jsize i = 0; i < chunk; i++) {
            unsigned int flags = (unsigned int)(unsigned char)marks[i];
            if ((flags & (unsigned int)mask) == (unsigned int)mask) kept++;
        }
        position += chunk;
    }
    return kept;
}

/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */
static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {
    if (mask == 0) return 0;
    jsize first = 0;
    while (first < size) {
        unsigned int flags = (unsigned int)(unsigned char)marks[first];
        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;
        first++;
    }
    return first;
}

/* One core for legacy whole-index and independent-offset windows. Bounds are validated by exports.
--- base:
/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- theirs (PR):
/* Reuse the mask-window owner's relative-ordinal atom without another scratch lane.
 * Each admitted ordinal replaces an already consumed mark: accepted <= i. A byte
 * represents all 256 chunk ordinals when read unsigned. No payload is read here.
 */
static jsize m3_admit_chunk(JNIEnv *env, jbyteArray states, jsize offset,
        jsize chunk, jint mask, jbyte *ordinals, jsize *first, jsize *span) {
    *first = 0;
    *span = chunk;
    if (mask == 0) return chunk;
    (*env)->GetByteArrayRegion(env, states, offset, chunk, ordinals);
    if ((*env)->ExceptionCheck(env)) return -1;
    unsigned char *positions = (unsigned char *)ordinals;
    jsize accepted = 0;
    for (jsize i = 0; i < chunk; i++) {
        unsigned int bits = (unsigned int)(unsigned char)ordinals[i];
        if ((bits & (unsigned int)mask) == (unsigned int)mask) {
            positions[accepted++] = (unsigned char)i;
        }
    }
    if (accepted != 0) {
        *first = (jsize)(unsigned char)ordinals[0];
        *span = (jsize)(unsigned char)ordinals[accepted - 1] - *first + 1;
    } else {
        *span = 0;
    }
    return accepted;
}

/* One core for legacy whole-index and offset windows. All bounds validated by exports.
--- after:
 * Prefix compaction is safe in row_ids: accepted never exceeds the current input slot.
 */

## H57 f4915972a720 MOD_MOD m3_index_mask_compactor.c prs=1616

context:
        if (mask != 0) {
            /* Stable prefix compaction: batch <= i, so unread values stay intact. */
            batch = 0;
--- ours (superset):
            /* Prefix compaction cannot overwrite an unread row: batch <= i. */
            for (jsize i = begin; i < chunk; i++) {
--- base:
            for (jsize i = 0; i < chunk; i++) {
--- theirs (PR):
            for (jsize i = begin; i < chunk; i++) {
--- after:
                unsigned int flags = (unsigned int)(unsigned char)marks[i];
                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];

## H58 61c80cf21ed1 MOD_MOD m3_index_mask_window.c prs=1375

context:
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
--- ours (superset):
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint matches = 0;
        jint consumed = 0;
        jint remaining = limit - written;
        while (consumed < take && matches < remaining) {
            unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
--- base:
        /* Locate the first admitted state before loading row payload. The emit loop
         * has no per-selected-value load flag, keeping dense chunks straightforward. */
        jint first_match = 0;
        while (first_match < take) {
            unsigned int flags = (unsigned int) (unsigned char) marks[first_match];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) break;
            first_match++;
        }
        if (first_match == take) {
            next += take;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, next, take, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        next += first_match;
        for (jint i = first_match; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
--- theirs (PR):
        /* Locate the first admitted state before loading row payload. The emit loop
         * has no per-selected-value load flag, keeping dense chunks straightforward. */
        jint first_match = 0;
        while (first_match < take) {
            unsigned int flags = (unsigned int) (unsigned char) marks[first_match];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) break;
            first_match++;
        }
        if (first_match == take) {
            next += take;
            continue;
        }
        /* The rejected prefix needs no row transfer; both ABI paths share this range atom. */
        (*env)->GetIntArrayRegion(env, rows, next + first_match, take - first_match, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        next += first_match;
        for (jint i = first_match; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
--- after:
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {

## H59 dc95da21f29f MOD_MOD m3_index_mask_window.c prs=1292

context:
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
--- ours (superset):
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint matches = 0;
        jint consumed = 0;
        jint remaining = limit - written;
        while (consumed < take && matches < remaining) {
            unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
--- base:
        /* Locate the first admitted state before loading row payload. The emit loop
         * has no per-selected-value load flag, keeping dense chunks straightforward. */
        jint first_match = 0;
        while (first_match < take) {
            unsigned int flags = (unsigned int) (unsigned char) marks[first_match];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) break;
            first_match++;
        }
        if (first_match == take) {
            next += take;
            continue;
        }
        (*env)->GetIntArrayRegion(env, rows, next, take, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        next += first_match;
        for (jint i = first_match; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
--- theirs (PR):
        /* Locate the first admitted state before loading row payload. The emit loop
         * has no per-selected-value load flag, keeping dense chunks straightforward. */
        jint first_match = 0;
        while (first_match < take) {
            unsigned int flags = (unsigned int) (unsigned char) marks[first_match];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) break;
            first_match++;
        }
        if (first_match == take) {
            next += take;
            continue;
        }
        /* The rejected prefix cannot contribute output. Preserve region/call
         * batching while transferring only the suffix whose row IDs can be used. */
        (*env)->GetIntArrayRegion(env, rows, next + first_match,
                take - first_match, values);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        next += first_match;
        for (jint i = first_match; i < take; i++) {
            unsigned int flags = (unsigned int) (unsigned char) marks[i];
--- after:
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {

## H60 05a27b983690 ADD_ADD m3_index_path_masks.c prs=1553

context:
 */
#include <jni.h>
#include <stddef.h>
--- ours (superset):
#include "../../../../../native/include/m3_jni_core.h"
--- base:

--- theirs (PR):
#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"
--- after:

static void m3_throw(JNIEnv *env, const char *type, const char *message) {

## H61 4662ee86a0a5 MOD_MOD m3_index_path_masks.c prs=1616

context:
            }
            begin = end;
        }
--- ours (superset):
        if (slot < 0 || slot >= length) {
            (*env)->DeleteLocalRef(env, plane_object);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, plane, slot, 1, &state);
        (*env)->DeleteLocalRef(env, plane_object);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (!m3_jni_matches_u32(state, required, forbidden)) return 0;
--- base:
        if (slot < 0 || slot >= length) {
            (*env)->DeleteLocalRef(env, plane_object);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, plane, slot, 1, &state);
        (*env)->DeleteLocalRef(env, plane_object);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((state & required) != required || (state & forbidden) != 0) return 0;
--- theirs (PR):
        (*env)->DeleteLocalRef(env, object);
--- after:
    }
    return remaining;

## H62 62c23e82c923 MOD_MOD m3_index_path_masks.c prs=1553

context:
>>>>>>> 53aefb4ed4b4280e7cda56f4a04829bcf2248f34

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
--- ours (superset):
    m3_jni_throw_new(env, type, message);
--- base:
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (void)(*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
--- theirs (PR):
    m3_jni_throw_by_name(env, type, message);
--- after:
}


## H63 ef3c15878b9c ADD_ADD m3_index_path_masks.c prs=1616

context:
            return NULL;
        }
    }
--- ours (superset):
    if (stages <= M3_CACHED_STAGE_LIMIT && count >= 8) {
        return m3_select_cached(env, rows, offsets, planes, required, forbidden,
                first, count, stride, stages);
    }
--- base:

--- theirs (PR):
    jint physical[M3_CHUNK], row_buffer[M3_CHUNK], selected[M3_CHUNK];
    unsigned char alive[M3_CHUNK];
--- after:
    jsize matching = 0;
    jintArray result = NULL;

## H64 05a27b983690 ADD_ADD m3_residency_state_mask.c prs=1553

context:
#include <jni.h>
#include <stdint.h>
#include "com_m3_collections_M3ResidencyStateMaskIndex.h"
--- ours (superset):
#include "../../../../../native/include/m3_jni_core.h"
--- base:

--- theirs (PR):
#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"
--- after:

/* Only ephemeral primitive index/word/state snapshots cross JNI; no Java object handles. */

## H65 4e538903640b MOD_MOD m3_residency_state_mask.c prs=1553

context:
#define M3_CHUNK 256

static void fail(JNIEnv *env, const char *name, const char *message) {
--- ours (superset):
    m3_jni_throw_new(env, name, message);
--- base:
    if ((*env)->ExceptionCheck(env)) return;
    jclass kind = (*env)->FindClass(env, name);
    if (kind == NULL) return;
    (void) (*env)->ThrowNew(env, kind, message);
    (*env)->DeleteLocalRef(env, kind);
--- theirs (PR):
    if ((*env)->ExceptionCheck(env)) return;
    m3_jni_throw_by_name(env, name, message);
--- after:
}


## H66 e8c1f94d6074 MOD_MOD classLoader.cpp prs=1368

context:
}

int ClassLoader::compute_Object_vtable() {
--- ours (superset):
  // Object declares five legacy virtual methods and M3's non-final toAdapter.
  // This count must match java.lang.Object before array klasses are created.
  const int Object_vtable_size = 6;
  return Object_vtable_size * vtableEntry::size();
--- base:
  // hardwired for JDK1.2 -- would need to duplicate class file parsing
  // code to determine actual value from file
  // Would be value '11' if finals were in vtable
  int JDK_1_2_Object_vtable_size = 5;
  return JDK_1_2_Object_vtable_size * vtableEntry::size();
--- theirs (PR):
  // Required before Object is loaded, when bootstrap array klasses are created.
  // M3JDK21 adds the overridable toAdapter(Class) method to the five legacy
  // Object slots. klassVtable verifies this reservation against parsed methods.
  const int M3_Object_vtable_size = 6;
  return M3_Object_vtable_size * vtableEntry::size();
--- after:
}


## H67 ef4ed27d5b97 MOD_MOD classLoader.cpp prs=1344

context:
}

int ClassLoader::compute_Object_vtable() {
--- ours (superset):
  // Object declares five legacy virtual methods and M3's non-final toAdapter.
  // This count must match java.lang.Object before array klasses are created.
  const int Object_vtable_size = 6;
  return Object_vtable_size * vtableEntry::size();
--- base:
  // hardwired for JDK1.2 -- would need to duplicate class file parsing
  // code to determine actual value from file
  // Would be value '11' if finals were in vtable
  int JDK_1_2_Object_vtable_size = 5;
  return JDK_1_2_Object_vtable_size * vtableEntry::size();
--- theirs (PR):
  // This count is needed before parsing Object, to create array klasses.
  // M3 has the five standard entries plus non-final Object.toAdapter(Class).
  int M3_Object_vtable_size = 6;
  return M3_Object_vtable_size * vtableEntry::size();
--- after:
}


## H68 b5871609c721 ADD_ADD M3IndexMaskWindow.c prs=1619

context:

--- ours (superset):
/* Modified 2026 by Hitesh Soliwal and contributors: trim JNI row spans to admitted ordinals. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Additional ABI in the existing m3_collections_mask shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include <jni.h>
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

/* Reuse #1131's sparse JNI scan atom for legacy and bounded source windows. */
static jlong m3_window_scan_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jint first, jint end, jboolean full_source, jint required, jint excluded,
        jintArray output, jint output_offset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (full_source) end = n;
    if (first < 0 || first > n || end < first || end > n
            || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    /* Remain inside this ABI: an unavailable required native symbol must still refuse. */
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    jint values[M3_MASK_WINDOW_CHUNK];
    if ((required | excluded) == 0) {
        jint count = end - first;
        if (count > limit) count = limit;
        for (jint copied = 0; copied < count;) {
            jint take = count - copied;
            if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
            (*env)->GetIntArrayRegion(env, rows, first + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            copied += take;
        }
        return m3_window_result(first + count, count);
    }

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    unsigned char offsets[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < end && written < limit) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint matches = 0;
        jint consumed = 0;
        jint remaining = limit - written;
        while (consumed < take && matches < remaining) {
            unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
                offsets[matches++] = (unsigned char) consumed;
            }
            consumed++;
        }
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            if (matches == M3_MASK_WINDOW_CHUNK && pending == 0) {
                /* Every ordinal in this chunk is selected; values is already contiguous. */
                written += matches;
                (*env)->SetIntArrayRegion(env, output,
                        output_offset + written - matches, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                next += consumed;
                continue;
            }
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
                }
            }
        }
        next += consumed;
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);

}

/* Internal M3JDK21 ABI: retains the original full-remainder semantics. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, 0, JNI_TRUE,
            required, excluded, output, output_offset, limit);
}

/* JDK libjava bounded JNI ABI: caller-admitted exclusive end ordinal. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint end, jint required, jint excluded, jintArray output,
        jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, end, JNI_FALSE,
            required, excluded, output, output_offset, limit);
}
--- base:

--- theirs (PR):
/* Modified 2026 by Hitesh Soliwal and contributors: trim JNI row spans to admitted ordinals. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Internal mask-window ABI in the JDK-owned libjava shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include "jdk_internal_m3_M3IndexMaskWindow.h"
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

/* Pure selection atom over caller-owned scratch, shared by both JNI entry points.
 * Preconditions: 0 <= take <= M3_MASK_WINDOW_CHUNK, remaining > 0, valid
 * unsigned-byte masks. Offsets are stable source ordinals, not row values.
 * No JNI calls, allocation, retained state, or payload mutation occur here.
 * Preserve the incumbent two-condition predicate: its fused native alternative
 * was rejected by the recorded resource gate. */
static jint m3_window_select_offsets(const jbyte *marks, jint take,
        jint required, jint excluded, jint remaining, unsigned char *offsets,
        jint *consumed_out) {
    jint matches = 0;
    jint consumed = 0;
    while (consumed < take && matches < remaining) {
        unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
        if ((flags & (unsigned int) required) == (unsigned int) required
                && (flags & (unsigned int) excluded) == 0U) {
            offsets[matches++] = (unsigned char) consumed;
        }
        consumed++;
    }
    *consumed_out = consumed;
    return matches;
}

/* Reuse #1131's sparse JNI scan atom for legacy and bounded source windows. */
static jlong m3_window_scan_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jint first, jint end, jboolean full_source, jint required, jint excluded,
        jintArray output, jint output_offset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (full_source) end = n;
    if (first < 0 || first > n || end < first || end > n
            || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    /* Remain inside this ABI: an unavailable required native symbol must still refuse. */
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    jint values[M3_MASK_WINDOW_CHUNK];
    if ((required | excluded) == 0) {
        jint count = end - first;
        if (count > limit) count = limit;
        for (jint copied = 0; copied < count;) {
            jint take = count - copied;
            if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
            (*env)->GetIntArrayRegion(env, rows, first + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            copied += take;
        }
        return m3_window_result(first + count, count);
    }

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    unsigned char offsets[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < end && written < limit) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint consumed;
        jint remaining = limit - written;
        jint matches = m3_window_select_offsets(marks, take, required, excluded,
                remaining, offsets, &consumed);
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            /* A contiguous selected span already is the compacted output. Bypass
             * identity compaction only when the original loop would flush exactly
             * this batch here. JNI call order, ranges and failure points stay equal. */
            if (matches == span && pending == 0
                    && (matches == M3_MASK_WINDOW_CHUNK || matches == remaining)) {
                (*env)->SetIntArrayRegion(env, output, output_offset + written, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                written += matches;
            } else {
                for (jint i = 0; i < matches; i++) {
                    selected[pending++] = values[(jint) offsets[i] - first_match];
                    written++;
                    if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                        (*env)->SetIntArrayRegion(env, output,
                                output_offset + written - pending, pending, selected);
                        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                        pending = 0;
                    }
                }
            }
        }
        next += consumed;
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);

}

/* Internal M3JDK21 ABI: retains the original full-remainder semantics. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, 0, JNI_TRUE,
            required, excluded, output, output_offset, limit);
}

/* JDK libjava bounded JNI ABI: caller-admitted exclusive end ordinal. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint end, jint required, jint excluded, jintArray output,
        jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, end, JNI_FALSE,
            required, excluded, output, output_offset, limit);
}


/* State-only reduction. Keep the established two-condition native predicate;
 * the prior selection predicate-fusion candidate remains rejected. */
static jint m3_window_count_matches(const jbyte *marks, jint take,
        jint required, jint excluded) {
    jint count = 0;
    for (jint i = 0; i < take; i++) {
        unsigned int flags = (unsigned int) (unsigned char) marks[i];
        if ((flags & (unsigned int) required) == (unsigned int) required
                && (flags & (unsigned int) excluded) == 0U) {
            count++;
        }
    }
    return count;
}

/* Additive count ABI: only bounded state reads, 128 scratch bytes plus locals.
 * No row-ID reads, output writes, allocation, callbacks or critical-array pins. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_countBoundedNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint first, jint end,
        jint required, jint excluded) {
    (void) owner;
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (states == NULL || required < 0 || required > 255
            || excluded < 0 || excluded > 255) {
        return m3_window_invalid(env, "invalid mask-window count arguments");
    }
    jsize length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (first < 0 || first > length || end < first || end > length) {
        return m3_window_invalid(env, "invalid mask-window count geometry");
    }
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    if ((required | excluded) == 0) return m3_window_result(end, end - first);
    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint count = 0;
    for (jint next = first; next < end;) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        count += m3_window_count_matches(marks, take, required, excluded);
        next += take;
    }
    return m3_window_result(end, count);
}
--- after:


## H69 fc6529771a7b ADD_ADD M3IndexMaskWindow.c prs=1755

context:

--- ours (superset):
/* Modified 2026 by Hitesh Soliwal and contributors: trim JNI row spans to admitted ordinals. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Additional ABI in the existing m3_collections_mask shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include <jni.h>
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

/* Reuse #1131's sparse JNI scan atom for legacy and bounded source windows. */
static jlong m3_window_scan_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jint first, jint end, jboolean full_source, jint required, jint excluded,
        jintArray output, jint output_offset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (full_source) end = n;
    if (first < 0 || first > n || end < first || end > n
            || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    /* Remain inside this ABI: an unavailable required native symbol must still refuse. */
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    jint values[M3_MASK_WINDOW_CHUNK];
    if ((required | excluded) == 0) {
        jint count = end - first;
        if (count > limit) count = limit;
        for (jint copied = 0; copied < count;) {
            jint take = count - copied;
            if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
            (*env)->GetIntArrayRegion(env, rows, first + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            copied += take;
        }
        return m3_window_result(first + count, count);
    }

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    unsigned char offsets[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < end && written < limit) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint matches = 0;
        jint consumed = 0;
        jint remaining = limit - written;
        while (consumed < take && matches < remaining) {
            unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
                offsets[matches++] = (unsigned char) consumed;
            }
            consumed++;
        }
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            if (matches == M3_MASK_WINDOW_CHUNK && pending == 0) {
                /* Every ordinal in this chunk is selected; values is already contiguous. */
                written += matches;
                (*env)->SetIntArrayRegion(env, output,
                        output_offset + written - matches, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                next += consumed;
                continue;
            }
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
                }
            }
        }
        next += consumed;
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);

}

/* Internal M3JDK21 ABI: retains the original full-remainder semantics. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, 0, JNI_TRUE,
            required, excluded, output, output_offset, limit);
}

/* JDK libjava bounded JNI ABI: caller-admitted exclusive end ordinal. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint end, jint required, jint excluded, jintArray output,
        jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, end, JNI_FALSE,
            required, excluded, output, output_offset, limit);
}
--- base:

--- theirs (PR):
/* Modified 2026 by Hitesh Soliwal and contributors: trim JNI row spans to admitted ordinals. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Internal mask-window ABI in the JDK-owned libjava shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include "jdk_internal_m3_M3IndexMaskWindow.h"
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

/* Reuse #1131's sparse JNI scan atom for legacy and bounded source windows. */
static jlong m3_window_scan_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jint first, jint end, jboolean full_source, jint required, jint excluded,
        jintArray output, jint output_offset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (full_source) end = n;
    if (first < 0 || first > n || end < first || end > n
            || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    /* Remain inside this ABI: an unavailable required native symbol must still refuse. */
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    jint values[M3_MASK_WINDOW_CHUNK];
    if ((required | excluded) == 0) {
        jint count = end - first;
        if (count > limit) count = limit;
        for (jint copied = 0; copied < count;) {
            jint take = count - copied;
            if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
            (*env)->GetIntArrayRegion(env, rows, first + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            copied += take;
        }
        return m3_window_result(first + count, count);
    }

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    unsigned char offsets[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < end && written < limit) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint matches = 0;
        jint consumed = 0;
        jint remaining = limit - written;
        while (consumed < take && matches < remaining) {
            unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
            if ((flags & (unsigned int) required) == (unsigned int) required
                    && (flags & (unsigned int) excluded) == 0U) {
                offsets[matches++] = (unsigned char) consumed;
            }
            consumed++;
        }
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
                }
            }
        }
        next += consumed;
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);

}

/* Internal M3JDK21 ABI: retains the original full-remainder semantics. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, 0, JNI_TRUE,
            required, excluded, output, output_offset, limit);
}

/* JDK libjava bounded JNI ABI: caller-admitted exclusive end ordinal. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint end, jint required, jint excluded, jintArray output,
        jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, end, JNI_FALSE,
            required, excluded, output, output_offset, limit);
}
--- after:


## H70 773bdfe6c8c7 ADD_ADD M3RX7.c prs=1553

context:
#include <stddef.h>
#include <stdlib.h>
#include "jdk_internal_mindex_M3RX7.h"
--- ours (superset):
#include "jdk_internal_mindex_M3RX7Transitions.h"
--- base:

--- theirs (PR):
#include "m3_jni_util.h"
--- after:

#define M3_ASCII 128

## H71 e45b047c911b ADD_ADD M3RX7.c prs=1357

context:

--- ours (superset):
/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception (OpenJDK java.base native destination).
 * M3JDK21 DFA token transition precompute: bounded primitive array arithmetic only.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include "jdk_internal_mindex_M3RX7.h"
#include "jdk_internal_mindex_M3RX7Transitions.h"

#define M3_ASCII 128
#define M3_MAX_STATES 4096
#define M3_MAX_PACKED (1 << 20)
#define M3_MAX_TOKEN 4096
#define M3_MAX_CELLS (1 << 20)
#define M3_MAX_WORK (32 << 20)

static void invalid(JNIEnv *env, const char *detail) {
    jclass clazz = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, detail);
        (*env)->DeleteLocalRef(env, clazz);
    }
}

static void allocation_failed(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, "bounded DFA transfer workspace");
        (*env)->DeleteLocalRef(env, clazz);
    }
}

static int valid_transfer(const jshort *rows, jsize states) {
    for (jsize state = 0; state < states; state++) {
        if (rows[state] < -1 || rows[state] >= states) return 0;
    }
    return 1;
}

/* Inputs may coincide for squaring. Output is always separate storage. */
static void compose_transfer(const jshort *left, const jshort *right,
        jshort *out, jsize states) {
    for (jsize state = 0; state < states; state++) {
        jint intermediate = left[state];
        out[state] = intermediate < 0 ? (jshort) -1 : right[intermediate];
    }
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7_buildPacked0(
        JNIEnv *env, jclass ignored, jshortArray dfaArray, jbyteArray packedArray,
        jintArray startArray, jintArray lengthArray, jshortArray outArray) {
    jshort *dfa = NULL;
    jbyte *packed = NULL;
    jint *starts = NULL, *lengths = NULL;
    jshort *out = NULL;
    jsize states, count, size;
    int valid = 1;
    int written = 0;
    (void) ignored;

    if (dfaArray == NULL || packedArray == NULL || startArray == NULL
            || lengthArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA token arrays");
        return;
    }
    size = (*env)->GetArrayLength(env, dfaArray);
    states = size / M3_ASCII;
    count = (*env)->GetArrayLength(env, startArray);
    jsize packedSize = (*env)->GetArrayLength(env, packedArray);
    if (size <= 0 || size % M3_ASCII != 0 || states > M3_MAX_STATES
            || packedSize > M3_MAX_PACKED || count != (*env)->GetArrayLength(env, lengthArray)
            || (int64_t) states * count > M3_MAX_CELLS
            || (*env)->GetArrayLength(env, outArray) != (int64_t) states * count) {
        invalid(env, "invalid DFA token array dimensions");
        return;
    }
    dfa = (*env)->GetShortArrayElements(env, dfaArray, NULL);
    if (dfa == NULL) goto done;
    packed = (*env)->GetByteArrayElements(env, packedArray, NULL);
    if (packed == NULL) goto done;
    starts = (*env)->GetIntArrayElements(env, startArray, NULL);
    if (starts == NULL) goto done;
    lengths = (*env)->GetIntArrayElements(env, lengthArray, NULL);
    if (lengths == NULL) goto done;

    int64_t totalBytes = 0;
    for (jsize i = 0; i < size; i++) {
        if (dfa[i] < -1 || dfa[i] >= states) { valid = 0; break; }
    }
    for (jsize i = 0; valid && i < count; i++) {
        if (starts[i] < 0 || lengths[i] < 1 || lengths[i] > M3_MAX_TOKEN
                || starts[i] > packedSize || lengths[i] > packedSize - starts[i]) {
            valid = 0;
            break;
        }
        totalBytes += lengths[i];
        if (totalBytes * states > M3_MAX_WORK) { valid = 0; break; }
        for (jint p = starts[i], end = p + lengths[i]; p < end; p++) {
            if (packed[p] < 0) { valid = 0; break; }
        }
    }
    if (!valid) {
        invalid(env, "invalid DFA or token bytes");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;

    for (jsize state = 0; state < states; state++) {
        for (jsize token = 0; token < count; token++) {
            jint cursor = state;
            jint end = starts[token] + lengths[token];
            for (jint at = starts[token]; at < end && cursor >= 0; at++) {
                cursor = dfa[cursor * M3_ASCII + (unsigned char) packed[at]];
            }
            out[(size_t) state * (size_t) count + (size_t) token] = (jshort) cursor;
        }
    }
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (lengths != NULL) (*env)->ReleaseIntArrayElements(env, lengthArray, lengths, JNI_ABORT);
    if (starts != NULL) (*env)->ReleaseIntArrayElements(env, startArray, starts, JNI_ABORT);
    if (packed != NULL) (*env)->ReleaseByteArrayElements(env, packedArray, packed, JNI_ABORT);
    if (dfa != NULL) (*env)->ReleaseShortArrayElements(env, dfaArray, dfa, JNI_ABORT);
}

/* Bounded token-symbol execution; no text decoding or retained Java addresses. */
JNIEXPORT jint JNICALL Java_jdk_internal_mindex_M3RX7_advanceTokens0(
        JNIEnv *env, jclass ignored, jshortArray rowArray, jint tokens, jint state,
        jintArray idArray, jint offset, jint length) {
    (void) ignored;
    if (rowArray == NULL || idArray == NULL) {
        invalid(env, "missing token-symbol arrays");
        return -1;
    }
    jsize cells = (*env)->GetArrayLength(env, rowArray);
    jsize ids = (*env)->GetArrayLength(env, idArray);
    if (tokens <= 0 || cells <= 0 || cells > M3_MAX_CELLS || cells % tokens != 0
            || cells / tokens > M3_MAX_STATES || state < -1 || state >= cells / tokens
            || offset < 0 || length < 0 || length > 4096 || offset > ids
            || length > ids - offset) {
        invalid(env, "invalid token-symbol page geometry");
        return -1;
    }
    jshort *rows = (*env)->GetShortArrayElements(env, rowArray, NULL);
    if (rows == NULL) return -1;
    jint page[256];
    int valid = 1;
    for (jint done = 0; done < length && valid;) {
        jint count = length - done < 256 ? length - done : 256;
        (*env)->GetIntArrayRegion(env, idArray, offset + done, count, page);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
            return -1;
        }
        for (jint i = 0; i < count; i++) {
            if (page[i] < 0 || page[i] >= tokens) { valid = 0; break; }
            if (state >= 0) {
                state = rows[(size_t) state * (size_t) tokens + (size_t) page[i]];
                if (state < -1 || state >= cells / tokens) { valid = 0; break; }
            }
        }
        done += count;
    }
    (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
    if (!valid) {
        invalid(env, "invalid token-symbol ID or transition");
        return -1;
    }
    return state;
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_compose0(
        JNIEnv *env, jclass ignored, jshortArray leftArray,
        jshortArray rightArray, jshortArray outArray) {
    jshort *left = NULL, *right = NULL, *out = NULL;
    int written = 0;
    (void) ignored;
    if (leftArray == NULL || rightArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, leftArray);
    if (states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, rightArray)
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA transfer dimensions");
        return;
    }
    if ((*env)->IsSameObject(env, leftArray, outArray)
            || (*env)->IsSameObject(env, rightArray, outArray)) {
        invalid(env, "DFA transfer output aliases input");
        return;
    }
    left = (*env)->GetShortArrayElements(env, leftArray, NULL);
    if (left == NULL) goto done;
    right = (*env)->GetShortArrayElements(env, rightArray, NULL);
    if (right == NULL) goto done;
    if (!valid_transfer(left, states) || !valid_transfer(right, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    compose_transfer(left, right, out, states);
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (right != NULL) (*env)->ReleaseShortArrayElements(env, rightArray, right, JNI_ABORT);
    if (left != NULL) (*env)->ReleaseShortArrayElements(env, leftArray, left, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_repeat0(
        JNIEnv *env, jclass ignored, jshortArray rowsArray,
        jlong count, jshortArray outArray) {
    jshort *rows = NULL, *out = NULL, *workspace = NULL;
    int written = 0;
    (void) ignored;
    if (rowsArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, rowsArray);
    if (count < 0 || states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA repetition dimensions or count");
        return;
    }
    if ((*env)->IsSameObject(env, rowsArray, outArray)) {
        invalid(env, "DFA repetition output aliases input");
        return;
    }
    rows = (*env)->GetShortArrayElements(env, rowsArray, NULL);
    if (rows == NULL) goto done;
    if (!valid_transfer(rows, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    workspace = (jshort *) malloc((size_t) states * 2 * sizeof(jshort));
    if (workspace == NULL) {
        allocation_failed(env);
        goto done;
    }
    jshort *result = workspace;
    jshort *scratch = workspace + states;
    for (jsize state = 0; state < states; state++) {
        result[state] = count == 0 ? (jshort) state : rows[state];
    }
    /* The leading set bit is represented by rows. Unsigned shifts are defined
       even for the highest admitted exponent, INT64_MAX. */
    uint64_t bit = 0;
    for (uint64_t remaining = (uint64_t) count; remaining > 1; remaining >>= 1) {
        bit = bit == 0 ? UINT64_C(1) : bit << 1;
    }
    for (; bit != 0; bit >>= 1) {
        compose_transfer(result, result, scratch, states);
        jshort *previous = result;
        result = scratch;
        scratch = previous;
        if (((uint64_t) count & bit) != 0) {
            compose_transfer(result, rows, scratch, states);
            previous = result;
            result = scratch;
            scratch = previous;
        }
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    for (jsize state = 0; state < states; state++) out[state] = result[state];
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    free(workspace);
    if (rows != NULL) (*env)->ReleaseShortArrayElements(env, rowsArray, rows, JNI_ABORT);
}
--- base:

--- theirs (PR):
/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception (OpenJDK java.base native destination).
 * M3JDK21 DFA token transition precompute: bounded primitive array arithmetic only.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include "jdk_internal_mindex_M3RX7.h"

#define M3_ASCII 128
#define M3_MAX_STATES 4096
#define M3_MAX_PACKED (1 << 20)
#define M3_MAX_TOKEN 4096
#define M3_MAX_CELLS (1 << 20)
#define M3_MAX_WORK (32 << 20)

static void invalid(JNIEnv *env, const char *detail) {
    jclass clazz = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, detail);
        (*env)->DeleteLocalRef(env, clazz);
    }
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7_buildPacked0(
        JNIEnv *env, jclass ignored, jshortArray dfaArray, jbyteArray packedArray,
        jintArray startArray, jintArray lengthArray, jshortArray outArray) {
    jshort *dfa = NULL;
    jbyte *packed = NULL;
    jint *starts = NULL, *lengths = NULL;
    jshort *out = NULL;
    jsize states, count, size;
    int valid = 1;
    int written = 0;
    (void) ignored;

    if (dfaArray == NULL || packedArray == NULL || startArray == NULL
            || lengthArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA token arrays");
        return;
    }
    size = (*env)->GetArrayLength(env, dfaArray);
    states = size / M3_ASCII;
    count = (*env)->GetArrayLength(env, startArray);
    jsize packedSize = (*env)->GetArrayLength(env, packedArray);
    if (size <= 0 || size % M3_ASCII != 0 || states > M3_MAX_STATES
            || packedSize > M3_MAX_PACKED || count != (*env)->GetArrayLength(env, lengthArray)
            || (int64_t) states * count > M3_MAX_CELLS
            || (*env)->GetArrayLength(env, outArray) != (int64_t) states * count) {
        invalid(env, "invalid DFA token array dimensions");
        return;
    }
    dfa = (*env)->GetShortArrayElements(env, dfaArray, NULL);
    if (dfa == NULL) goto done;
    packed = (*env)->GetByteArrayElements(env, packedArray, NULL);
    if (packed == NULL) goto done;
    starts = (*env)->GetIntArrayElements(env, startArray, NULL);
    if (starts == NULL) goto done;
    lengths = (*env)->GetIntArrayElements(env, lengthArray, NULL);
    if (lengths == NULL) goto done;

    int64_t totalBytes = 0;
    for (jsize i = 0; i < size; i++) {
        if (dfa[i] < -1 || dfa[i] >= states) { valid = 0; break; }
    }
    for (jsize i = 0; valid && i < count; i++) {
        if (starts[i] < 0 || lengths[i] < 1 || lengths[i] > M3_MAX_TOKEN
                || starts[i] > packedSize || lengths[i] > packedSize - starts[i]) {
            valid = 0;
            break;
        }
        totalBytes += lengths[i];
        if (totalBytes * states > M3_MAX_WORK) { valid = 0; break; }
        for (jint p = starts[i], end = p + lengths[i]; p < end; p++) {
            if (packed[p] < 0) { valid = 0; break; }
        }
    }
    if (!valid) {
        invalid(env, "invalid DFA or token bytes");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;

    for (jsize state = 0; state < states; state++) {
        for (jsize token = 0; token < count; token++) {
            jint cursor = state;
            jint end = starts[token] + lengths[token];
            for (jint at = starts[token]; at < end && cursor >= 0; at++) {
                cursor = dfa[cursor * M3_ASCII + (unsigned char) packed[at]];
            }
            out[(size_t) state * (size_t) count + (size_t) token] = (jshort) cursor;
        }
    }
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (lengths != NULL) (*env)->ReleaseIntArrayElements(env, lengthArray, lengths, JNI_ABORT);
    if (starts != NULL) (*env)->ReleaseIntArrayElements(env, startArray, starts, JNI_ABORT);
    if (packed != NULL) (*env)->ReleaseByteArrayElements(env, packedArray, packed, JNI_ABORT);
    if (dfa != NULL) (*env)->ReleaseShortArrayElements(env, dfaArray, dfa, JNI_ABORT);
}
--- after:


## H72 b954bbd376e9 ADD_ADD M3StringPool.c prs=1426

context:

--- ours (superset):
/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include "java_lang_M3StringPool.h"
#if defined(__linux__)
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#elif defined(_WIN32)
#include <windows.h>
#endif

static void mapping_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/io/IOException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static int valid_range(jlong address, jlong length) {
    if (address <= 0 || length <= 0 || length > INT_MAX
            || ((uint64_t)address & UINT64_C(65535)) != 0
            || (uint64_t)address > (uint64_t)UINTPTR_MAX
            || (uint64_t)length > (uint64_t)SIZE_MAX) return 0;
    return (uint64_t)(length - 1) <= (uint64_t)UINTPTR_MAX - (uint64_t)address;
}

JNIEXPORT jobject JNICALL
Java_java_lang_M3StringPool_mapLexicon0(JNIEnv *env, jclass ignored,
        jint descriptor, jlong handle, jlong address, jlong length) {
    void *mapped;
    jobject buffer;
    (void)ignored;
    if (!valid_range(address, length)) {
        mapping_error(env, "Invalid fixed M3 lexicon address or size");
        return NULL;
    }
#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
    struct stat info;
    long page_size;
    (void)handle;
    if (descriptor < 0 || fstat(descriptor, &info) != 0
            || !S_ISREG(info.st_mode) || info.st_size != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon descriptor");
        return NULL;
    }
    page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0 || (uint64_t)address % (uint64_t)page_size != 0) {
        mapping_error(env, "Unaligned fixed M3 lexicon address");
        return NULL;
    }
    mapped = mmap((void *)(uintptr_t)address, (size_t)length,
            PROT_READ, MAP_SHARED | MAP_FIXED_NOREPLACE, descriptor, 0);
    if (mapped == MAP_FAILED) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)munmap(mapped, (size_t)length);
        mapping_error(env, "Kernel did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)munmap(mapped, (size_t)length);
    return buffer;
#elif defined(_WIN32)
    HANDLE file = (HANDLE)(uintptr_t)handle;
    HANDLE section;
    LARGE_INTEGER size;
    (void)descriptor;
    if (file == NULL || file == INVALID_HANDLE_VALUE || GetFileType(file) != FILE_TYPE_DISK
            || !GetFileSizeEx(file, &size) || size.QuadPart != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon handle");
        return NULL;
    }
    section = CreateFileMappingW(file, NULL, PAGE_READONLY, 0, 0, NULL);
    if (section == NULL) {
        mapping_error(env, "Cannot create fixed M3 lexicon file mapping");
        return NULL;
    }
    mapped = MapViewOfFileEx(section, FILE_MAP_READ, 0, 0, (SIZE_T)length,
            (LPVOID)(uintptr_t)address);
    (void)CloseHandle(section);
    if (mapped == NULL) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)UnmapViewOfFile(mapped);
        mapping_error(env, "System did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)UnmapViewOfFile(mapped);
    return buffer;
#else
    (void)descriptor;
    (void)handle;
    (void)mapped;
    (void)buffer;
    mapping_error(env, "Fixed M3 lexicon mapping is unsupported on this platform");
    return NULL;
#endif
}

JNIEXPORT jboolean JNICALL
Java_java_lang_M3StringPool_unmapLexicon0(JNIEnv *env, jclass ignored,
        jlong address, jlong length) {
    (void)env;
    (void)ignored;
    if (!valid_range(address, length)) return JNI_FALSE;
#if defined(__linux__)
    return munmap((void *)(uintptr_t)address, (size_t)length) == 0 ? JNI_TRUE : JNI_FALSE;
#elif defined(_WIN32)
    return UnmapViewOfFile((void *)(uintptr_t)address) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

/*
 * Fixed-array JNI concatenation of canonical owner/range views.
 * No joined byte[]/char[] payload and no persistent native pointers.
 * Incoming M3String[] keeps the OS-mapped and local owners reachable.
 */
// FLLDCIM: ATOM — native fixed-stack concatenation; M3StringPool owns canonical merging.
#define M3_CONCAT_LEVELS 32

static void m3_concat_throw(JNIEnv *env, const char *class_name, const char *message) {
    jclass error_class = (*env)->FindClass(env, class_name);
    if (error_class != NULL) {
        (void)(*env)->ThrowNew(env, error_class, message);
        (*env)->DeleteLocalRef(env, error_class);
    }
}

JNIEXPORT jobject JNICALL
Java_java_lang_M3StringPool_concatViewsNative0(JNIEnv *env, jclass pool, jobjectArray parts) {
    jobject levels[M3_CONCAT_LEVELS] = {NULL};
    jobject result = NULL;
    jclass string_class = NULL;
    jfieldID coordinate_field = NULL;
    jmethodID concat_method = NULL;
    jmethodID empty_method = NULL;
    jlong accumulated = 0;

    if (parts == NULL) {
        m3_concat_throw(env, "java/lang/NullPointerException", "M3String parts");
        return NULL;
    }
    string_class = (*env)->FindClass(env, "java/lang/M3String");
    if (string_class == NULL) goto cleanup;
    coordinate_field = (*env)->GetFieldID(env, string_class, "value", "J");
    if (coordinate_field == NULL) goto cleanup;
    concat_method = (*env)->GetStaticMethodID(env, pool, "concat",
            "(Ljava/lang/M3String;Ljava/lang/M3String;)Ljava/lang/M3String;");
    if (concat_method == NULL) goto cleanup;
    empty_method = (*env)->GetStaticMethodID(env, string_class, "empty", "()Ljava/lang/M3String;");
    if (empty_method == NULL) goto cleanup;

    const jsize count = (*env)->GetArrayLength(env, parts);
    for (jsize index = 0; index < count; index++) {
        jobject part = (*env)->GetObjectArrayElement(env, parts, index);
        if (part == NULL) {
            if (!(*env)->ExceptionCheck(env)) {
                m3_concat_throw(env, "java/lang/NullPointerException", "null interned M3String part");
            }
            goto cleanup;
        }
        const jlong coordinate = (*env)->GetLongField(env, part, coordinate_field);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, part);
            goto cleanup;
        }
        const uint64_t units = (uint64_t) coordinate & UINT64_C(0xffffffff);
        if (units > (uint64_t) INT_MAX || units > (uint64_t) (INT_MAX - accumulated)) {
            (*env)->DeleteLocalRef(env, part);
            m3_concat_throw(env, "java/lang/OutOfMemoryError", "Required length exceeds implementation limit");
            goto cleanup;
        }
        accumulated += (jlong) units;
        if (units == 0) {
            (*env)->DeleteLocalRef(env, part);
            continue;
        }

        int level;
        for (level = 0; level < M3_CONCAT_LEVELS; level++) {
            if (levels[level] == NULL) {
                levels[level] = part;
                part = NULL;
                break;
            }
            jobject merged = (*env)->CallStaticObjectMethod(env, pool, concat_method,
                    levels[level], part);
            (*env)->DeleteLocalRef(env, levels[level]);
            levels[level] = NULL;
            (*env)->DeleteLocalRef(env, part);
            part = merged;
            if ((*env)->ExceptionCheck(env) || part == NULL) {
                if (part != NULL) (*env)->DeleteLocalRef(env, part);
                goto cleanup;
            }
        }
        if (part != NULL) {
            (*env)->DeleteLocalRef(env, part);
            m3_concat_throw(env, "java/lang/OutOfMemoryError", "Too many M3 concatenation parts");
            goto cleanup;
        }
    }

    /* Higher occupied levels hold earlier pieces. Fold low-to-high for source order. */
    for (int level = 0; level < M3_CONCAT_LEVELS; level++) {
        if (levels[level] == NULL) continue;
        if (result == NULL) {
            result = levels[level];
            levels[level] = NULL;
        } else {
            jobject merged = (*env)->CallStaticObjectMethod(env, pool, concat_method,
                    levels[level], result);
            (*env)->DeleteLocalRef(env, levels[level]);
            levels[level] = NULL;
            (*env)->DeleteLocalRef(env, result);
            result = merged;
            if ((*env)->ExceptionCheck(env) || result == NULL) goto cleanup;
        }
    }
    if (result == NULL) result = (*env)->CallStaticObjectMethod(env, string_class, empty_method);

cleanup:
    for (int level = 0; level < M3_CONCAT_LEVELS; level++) {
        if (levels[level] != NULL) (*env)->DeleteLocalRef(env, levels[level]);
    }
    if (string_class != NULL) (*env)->DeleteLocalRef(env, string_class);
    if ((*env)->ExceptionCheck(env)) {
        if (result != NULL) (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}
--- base:

--- theirs (PR):
/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include "java_lang_M3StringPool.h"
#if defined(__linux__)
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#elif defined(_WIN32)
#include <windows.h>
#endif

static void mapping_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/io/IOException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static int valid_range(jlong address, jlong length) {
    if (address <= 0 || length <= 0 || length > INT_MAX
            || ((uint64_t)address & UINT64_C(65535)) != 0
            || (uint64_t)address > (uint64_t)UINTPTR_MAX
            || (uint64_t)length > (uint64_t)SIZE_MAX) return 0;
    return (uint64_t)(length - 1) <= (uint64_t)UINTPTR_MAX - (uint64_t)address;
}

JNIEXPORT jobject JNICALL
Java_java_lang_M3StringPool_mapLexicon0(JNIEnv *env, jclass ignored,
        jint descriptor, jlong handle, jlong address, jlong length) {
    void *mapped;
    jobject buffer;
    (void)ignored;
    if (!valid_range(address, length)) {
        mapping_error(env, "Invalid fixed M3 lexicon address or size");
        return NULL;
    }
#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
    struct stat info;
    long page_size;
    (void)handle;
    if (descriptor < 0 || fstat(descriptor, &info) != 0
            || !S_ISREG(info.st_mode) || info.st_size != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon descriptor");
        return NULL;
    }
    page_size = sysconf(_SC_PAGESIZE);
    if (page_size <= 0 || (uint64_t)address % (uint64_t)page_size != 0) {
        mapping_error(env, "Unaligned fixed M3 lexicon address");
        return NULL;
    }
    mapped = mmap((void *)(uintptr_t)address, (size_t)length,
            PROT_READ, MAP_SHARED | MAP_FIXED_NOREPLACE, descriptor, 0);
    if (mapped == MAP_FAILED) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)munmap(mapped, (size_t)length);
        mapping_error(env, "Kernel did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)munmap(mapped, (size_t)length);
    return buffer;
#elif defined(_WIN32)
    HANDLE file = (HANDLE)(uintptr_t)handle;
    HANDLE section;
    LARGE_INTEGER size;
    (void)descriptor;
    if (file == NULL || file == INVALID_HANDLE_VALUE || GetFileType(file) != FILE_TYPE_DISK
            || !GetFileSizeEx(file, &size) || size.QuadPart != length) {
        mapping_error(env, "Invalid or changed fixed M3 lexicon handle");
        return NULL;
    }
    section = CreateFileMappingW(file, NULL, PAGE_READONLY, 0, 0, NULL);
    if (section == NULL) {
        mapping_error(env, "Cannot create fixed M3 lexicon file mapping");
        return NULL;
    }
    mapped = MapViewOfFileEx(section, FILE_MAP_READ, 0, 0, (SIZE_T)length,
            (LPVOID)(uintptr_t)address);
    (void)CloseHandle(section);
    if (mapped == NULL) {
        mapping_error(env, "Fixed M3 lexicon address unavailable");
        return NULL;
    }
    if ((uintptr_t)mapped != (uintptr_t)address) {
        (void)UnmapViewOfFile(mapped);
        mapping_error(env, "System did not honor the fixed M3 lexicon address");
        return NULL;
    }
    buffer = (*env)->NewDirectByteBuffer(env, mapped, length);
    if (buffer == NULL) (void)UnmapViewOfFile(mapped);
    return buffer;
#else
    (void)descriptor;
    (void)handle;
    (void)mapped;
    (void)buffer;
    mapping_error(env, "Fixed M3 lexicon mapping is unsupported on this platform");
    return NULL;
#endif
}

JNIEXPORT jboolean JNICALL
Java_java_lang_M3StringPool_unmapLexicon0(JNIEnv *env, jclass ignored,
        jlong address, jlong length) {
    (void)env;
    (void)ignored;
    if (!valid_range(address, length)) return JNI_FALSE;
#if defined(__linux__)
    return munmap((void *)(uintptr_t)address, (size_t)length) == 0 ? JNI_TRUE : JNI_FALSE;
#elif defined(_WIN32)
    return UnmapViewOfFile((void *)(uintptr_t)address) ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}
--- after:


## H73 61c419730053 ADD_ADD String.c prs=1684

context:
        return NULL;
    }

--- ours (superset):
    if (!m3_check_shadow_range(env, value, start, length,
                               "java/lang/StringIndexOutOfBoundsException")) {
        return NULL;
    }

--- base:

--- theirs (PR):
    if (!m3_check_shadow_range(env, value, start, length,
            "java/lang/StringIndexOutOfBoundsException")) return NULL;

--- after:
    jcharArray result = (*env)->NewCharArray(env, length);
    if (result == NULL || length == 0) return result;

