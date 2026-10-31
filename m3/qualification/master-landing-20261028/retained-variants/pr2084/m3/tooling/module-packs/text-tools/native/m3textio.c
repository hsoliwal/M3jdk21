/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * libm3textio: JNI atoms for the jdk.m3.io and jdk.m3.text modules.
 *
 * The wc kernel below ports the observable counting semantics of Unix4j wc
 * (https://github.com/tools4j/unix4j at 0a127602907978efbff7ea297bd553274ba70a8e:
 * unix4j-command org/unix4j/unix/wc/CounterType.java and Counters.java,
 * unix4j-base org/unix4j/util/StringUtil.java splitLines). No Unix4j source is
 * copied; the semantics are re-expressed in C. Unix4j is licensed as follows:
 *
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2012-2021 unix4j (tools4j), Ben Warner and Marco Terzer
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */
#include <jni.h>
#include <errno.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "m3_jni_core.h"
#include "m3textio_whitespace.h"
#ifndef _WIN32
#include <sys/types.h>
#include <sys/stat.h>
#endif

static jint m3_mkfifo(JNIEnv *env, jbyteArray input, jint mode) {
    if (input == NULL || (mode & ~0777) != 0) return EINVAL;
    jsize length = (*env)->GetArrayLength(env, input);
    if (length <= 0 || length > 65535) return EINVAL;
    char *path = (char *)malloc((size_t)length + 1);
    if (path == NULL) return ENOMEM;
    (*env)->GetByteArrayRegion(env, input, 0, length, (jbyte *)path);
    if ((*env)->ExceptionCheck(env)) { free(path); return EINVAL; }
    path[length] = '\0';
    if (memchr(path, '\0', (size_t)length) != NULL) { free(path); return EINVAL; }
#ifdef _WIN32
    jint result = ENOSYS;
#else
    jint result = mkfifo(path, (mode_t)mode) == 0 ? 0 : errno;
#endif
    free(path);
    return result;
}

JNIEXPORT jint JNICALL Java_com_synexia_unix4jm3_io_FileTools_mkfifo0
  (JNIEnv *env, jclass owner, jbyteArray path, jint mode) {
    (void)owner;
    return m3_mkfifo(env, path, mode);
}

JNIEXPORT jint JNICALL Java_jdk_m3_io_FileTools_mkfifo0
  (JNIEnv *env, jclass owner, jbyteArray path, jint mode) {
    (void)owner;
    return m3_mkfifo(env, path, mode);
}

/* ---- wc kernel (Unix4j semantics, MIT notice above) ----------------------
 * Units are UTF-16 code units (width 2, either byte order) or Latin-1 bytes
 * (width 1). A line ends at CR, LF, CRLF or LFCR (a CR/LF pair of different
 * units). Words are maximal runs of units for which Character.isWhitespace(char)
 * is false; every ending unit is whitespace, so stream words equal per-line
 * content words. Chars count every unit, endings included. A lone empty line
 * (one line whose content is empty) counts as zero lines.
 * The caller's long[] state carries everything across windows, so a String can
 * be fed leaf by leaf; no Java handle or address is retained between calls. */

/* long[] state ABI v1, mirrored by jdk.m3.text.WcNative. */
#define M3WC_LINES 0
#define M3WC_WORDS 1
#define M3WC_CHARS 2
#define M3WC_FLAGS 3
#define M3WC_MAGIC 4
#define M3WC_SLOTS 5
#define M3WC_MAGIC_V1 INT64_C(0x4D33574300000001)

#define M3WC_IN_WORD    UINT64_C(0x01) /* previous unit continued a word */
#define M3WC_PENDING_CR UINT64_C(0x02) /* CR ended a line; an LF may complete it */
#define M3WC_PENDING_LF UINT64_C(0x04) /* LF ended a line; a CR may complete it */
#define M3WC_CONTENT    UINT64_C(0x08) /* current open line has content units */
#define M3WC_LAST_EMPTY UINT64_C(0x10) /* last counted line had empty content */
#define M3WC_FINISHED   UINT64_C(0x20) /* totals are final */
#define M3WC_STREAM     UINT64_C(0x40) /* state fed by raw windows */
#define M3WC_LANES      UINT64_C(0x80) /* state fed by line lanes */
#define M3WC_KNOWN      UINT64_C(0xFF)
#define M3WC_LANE_BATCH 256

typedef struct {
    int64_t lines;
    int64_t words;
    int64_t chars;
    uint64_t flags;
} m3wc_state;

//FLLDCIM: BOP - atom wc.stream_kernel: unit decode, CR/LF/CRLF/LFCR line state, word runs, finish
static inline uint16_t m3wc_unit(const unsigned char *base, size_t index, jint width, int big) {
    if (width == 1) return base[index];
    const unsigned char *p = base + index * 2u;
    return big ? (uint16_t)((unsigned int)p[0] << 8 | p[1])
               : (uint16_t)(p[0] | (unsigned int)p[1] << 8);
}

static void m3wc_stream_unit(m3wc_state *s, uint16_t unit) {
    uint64_t f = s->flags;
    if (unit == 0x0D || unit == 0x0A) {
        uint64_t self = unit == 0x0D ? M3WC_PENDING_CR : M3WC_PENDING_LF;
        uint64_t other = unit == 0x0D ? M3WC_PENDING_LF : M3WC_PENDING_CR;
        if ((f & other) != 0) {
            f &= ~other; /* second unit of a CRLF or LFCR ending */
        } else {
            uint64_t empty = (f & M3WC_CONTENT) != 0 ? 0 : M3WC_LAST_EMPTY;
            f = (f & ~(M3WC_PENDING_CR | M3WC_PENDING_LF | M3WC_CONTENT | M3WC_LAST_EMPTY)) | self | empty;
            s->lines++;
        }
        f &= ~M3WC_IN_WORD;
    } else {
        f = (f & ~(M3WC_PENDING_CR | M3WC_PENDING_LF)) | M3WC_CONTENT;
        if (m3textio_is_whitespace(unit)) {
            f &= ~M3WC_IN_WORD;
        } else if ((f & M3WC_IN_WORD) == 0) {
            s->words++;
            f |= M3WC_IN_WORD;
        }
    }
    s->flags = f;
}

static void m3wc_finish(m3wc_state *s) {
    if ((s->flags & M3WC_CONTENT) != 0) { /* unterminated last line */
        s->lines++;
        s->flags &= ~M3WC_LAST_EMPTY;
    }
    if (s->lines == 1 && (s->flags & M3WC_LAST_EMPTY) != 0) s->lines = 0;
    s->flags = (s->flags & ~(M3WC_IN_WORD | M3WC_PENDING_CR | M3WC_PENDING_LF | M3WC_CONTENT))
            | M3WC_FINISHED;
}

//FLLDCIM: IDL - atom wc.jni_boundary: state ABI v1 load/store and direct-buffer window validation
/* Loads and validates the caller state; returns 0 with a pending exception on failure. */
static int m3wc_load(JNIEnv *env, jlongArray state, uint64_t mode, m3wc_state *s) {
    if (state == NULL) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "wc state");
        return 0;
    }
    if ((*env)->GetArrayLength(env, state) != M3WC_SLOTS) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc state length");
        return 0;
    }
    jlong slots[M3WC_SLOTS];
    (*env)->GetLongArrayRegion(env, state, 0, M3WC_SLOTS, slots);
    if ((*env)->ExceptionCheck(env)) return 0;
    uint64_t flags = (uint64_t)slots[M3WC_FLAGS];
    if (slots[M3WC_MAGIC] != M3WC_MAGIC_V1 || (flags & ~M3WC_KNOWN) != 0
            || slots[M3WC_LINES] < 0 || slots[M3WC_WORDS] < 0 || slots[M3WC_CHARS] < 0) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc state is not ABI v1");
        return 0;
    }
    if ((flags & M3WC_FINISHED) != 0) {
        m3_jni_throw_new(env, "java/lang/IllegalStateException", "wc state already finished");
        return 0;
    }
    if ((flags & (M3WC_STREAM | M3WC_LANES) & ~mode) != 0) {
        m3_jni_throw_new(env, "java/lang/IllegalStateException", "wc state mixes windows and lanes");
        return 0;
    }
    s->lines = slots[M3WC_LINES];
    s->words = slots[M3WC_WORDS];
    s->chars = slots[M3WC_CHARS];
    s->flags = flags | mode;
    return 1;
}

static void m3wc_store(JNIEnv *env, jlongArray state, const m3wc_state *s) {
    jlong slots[M3WC_SLOTS] = {s->lines, s->words, s->chars, (jlong)s->flags, M3WC_MAGIC_V1};
    (*env)->SetLongArrayRegion(env, state, 0, M3WC_SLOTS, slots);
}

/* Borrowed direct-buffer view; valid only for the current native call. */
typedef struct {
    const unsigned char *base;
    jlong capacity;
} m3wc_direct;

static int m3wc_resolve(JNIEnv *env, jobject buffer, m3wc_direct *direct) {
    if (buffer == NULL) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "wc window");
        return 0;
    }
    direct->capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (direct->capacity < 0 || direct->capacity > INT32_MAX) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc window must be a direct buffer");
        return 0;
    }
    direct->base = (const unsigned char *)(*env)->GetDirectBufferAddress(env, buffer);
    if (direct->base == NULL) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc window has no address");
        return 0;
    }
    return 1;
}

/* [byte_offset, byte_offset + bytes) inside the view; NULL with a pending exception. */
static const unsigned char *m3wc_range(JNIEnv *env, const m3wc_direct *direct, jlong byte_offset, jlong bytes) {
    if (byte_offset < 0 || byte_offset > INT32_MAX || bytes < 0 || bytes > INT32_MAX
            || !m3_jni_valid_window((jsize)direct->capacity, (jint)byte_offset, (jint)bytes)) {
        m3_jni_throw_new(env, "java/lang/IndexOutOfBoundsException", "wc window range");
        return NULL;
    }
    return direct->base + byte_offset;
}

//FLLDCIM: IDL - atom wc.jni_count0: raw window entry (UTF-16 LE/BE or Latin-1) with carry state
JNIEXPORT void JNICALL Java_jdk_m3_text_WcNative_count0
  (JNIEnv *env, jclass owner, jobject window, jint byteOffset, jint units, jint width,
   jboolean bigEndian, jboolean last, jlongArray state) {
    (void)owner;
    m3wc_state s;
    if (!m3wc_load(env, state, M3WC_STREAM, &s)) return;
    if ((width != 1 && width != 2) || units < 0) {
        m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc width or units");
        return;
    }
    if (units > 0) {
        m3wc_direct direct;
        if (!m3wc_resolve(env, window, &direct)) return;
        const unsigned char *bytes = m3wc_range(env, &direct, byteOffset, (jlong)units * width);
        if (bytes == NULL) return;
        if (s.chars > INT64_MAX - units) {
            m3_jni_throw_new(env, "java/lang/ArithmeticException", "wc char count overflow");
            return;
        }
        int big = bigEndian == JNI_TRUE;
        for (jint i = 0; i < units; i++) m3wc_stream_unit(&s, m3wc_unit(bytes, (size_t)i, width, big));
        s.chars += units;
    }
    if (last == JNI_TRUE) m3wc_finish(&s);
    m3wc_store(env, state, &s);
}

//FLLDCIM: BOP - atom wc.lane_words: per-line content word runs over a UTF-16 lane range
static int64_t m3wc_content_words(const unsigned char *bytes, jint units, int big) {
    int64_t words = 0;
    int in_word = 0;
    for (jint i = 0; i < units; i++) {
        if (m3textio_is_whitespace(m3wc_unit(bytes, (size_t)i, 2, big))) {
            in_word = 0;
        } else if (!in_word) {
            words++;
            in_word = 1;
        }
    }
    return words;
}

//FLLDCIM: IDL - atom wc.jni_countLines0: SoA lane entry (UTF-16 buffer, content/ending int lanes)
JNIEXPORT void JNICALL Java_jdk_m3_text_WcNative_countLines0
  (JNIEnv *env, jclass owner, jobject utf16, jint byteOffset, jboolean bigEndian,
   jintArray contentLengths, jintArray endingLengths, jint fromLine, jint toLine,
   jboolean last, jlongArray state) {
    (void)owner;
    m3wc_state s;
    if (!m3wc_load(env, state, M3WC_LANES, &s)) return;
    if (contentLengths == NULL || endingLengths == NULL) {
        m3_jni_throw_new(env, "java/lang/NullPointerException", "wc lanes");
        return;
    }
    jsize content_size = (*env)->GetArrayLength(env, contentLengths);
    jsize ending_size = (*env)->GetArrayLength(env, endingLengths);
    if (fromLine > toLine || !m3_jni_valid_window(content_size, fromLine, toLine - fromLine)
            || !m3_jni_valid_window(ending_size, fromLine, toLine - fromLine)) {
        m3_jni_throw_new(env, "java/lang/IndexOutOfBoundsException", "wc lane range");
        return;
    }
    m3wc_direct direct;
    if (!m3wc_resolve(env, utf16, &direct)) return;
    int big = bigEndian == JNI_TRUE;
    jlong at = byteOffset;
    jint content[M3WC_LANE_BATCH];
    jint ending[M3WC_LANE_BATCH];
    for (jint from = fromLine; from < toLine; from += M3WC_LANE_BATCH) {
        jint count = toLine - from < M3WC_LANE_BATCH ? toLine - from : M3WC_LANE_BATCH;
        (*env)->GetIntArrayRegion(env, contentLengths, from, count, content);
        (*env)->GetIntArrayRegion(env, endingLengths, from, count, ending);
        if ((*env)->ExceptionCheck(env)) return;
        for (jint i = 0; i < count; i++) {
            if (content[i] < 0 || ending[i] < 0 || ending[i] > 2) {
                m3_jni_throw_new(env, "java/lang/IllegalArgumentException", "wc lane length");
                return;
            }
            jlong units = (jlong)content[i] + ending[i];
            const unsigned char *bytes = m3wc_range(env, &direct, at, units * 2);
            if (bytes == NULL) return;
            s.words += m3wc_content_words(bytes, content[i], big);
            s.lines++;
            s.chars += units;
            s.flags = content[i] == 0 ? (s.flags | M3WC_LAST_EMPTY) : (s.flags & ~M3WC_LAST_EMPTY);
            at += units * 2;
        }
    }
    if (last == JNI_TRUE) m3wc_finish(&s);
    m3wc_store(env, state, &s);
}
