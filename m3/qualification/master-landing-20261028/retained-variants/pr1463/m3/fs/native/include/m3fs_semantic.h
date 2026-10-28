// SPDX-License-Identifier: Apache-2.0
#ifndef M3FS_SEMANTIC_H
#define M3FS_SEMANTIC_H

#include "m3fs.h"

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct m3fs_mindex_info_data {
  uint32_t value_count;
  uint32_t token_count;
  uint32_t utf16_count;
  uint32_t image_bytes;
} m3fs_mindex_info_data;

typedef struct m3fs_mindex_value {
  uint32_t token_count;
  uint32_t utf16_length;
  int32_t java_hash;
  uint32_t reserved;
  uint64_t signal64;
} m3fs_mindex_value;

enum {
  M3FS_REGEX_MATCHES = 0,
  M3FS_REGEX_FIND = 1,
  M3FS_REGEX_LOOKING_AT = 2
};

enum {
  M3FS_REGEX_OP_CHAR = 1,
  M3FS_REGEX_OP_SPLIT = 2,
  M3FS_REGEX_OP_JUMP = 3,
  M3FS_REGEX_OP_ASSERT_START = 4,
  M3FS_REGEX_OP_ASSERT_END = 5,
  M3FS_REGEX_OP_MATCH = 6,
  M3FS_REGEX_OP_ASSERT_WORD_BOUNDARY = 7,
  M3FS_REGEX_OP_ASSERT_NOT_WORD_BOUNDARY = 8
};

typedef struct m3fs_regex_program {
  const uint8_t *op;
  const int32_t *out1;
  const int32_t *out2;
  const int32_t *class_id;
  uint32_t state_count;
  const uint64_t *ascii_low;
  const uint64_t *ascii_high;
  const int32_t *range_offset;
  const int32_t *range_count;
  uint32_t class_count;
  const uint16_t *range_low;
  const uint16_t *range_high;
  uint32_t range_count_total;
  uint32_t start_state;
  uint32_t match_state;
  int32_t min_length;
  int32_t max_length;
  uint64_t required_presence64;
  uint32_t may_consume_surrogate_unit;
} m3fs_regex_program;

M3FS_EXPORT int m3fs_mindex_info(
    m3fs *fs, const char *path, m3fs_mindex_info_data *out);
M3FS_EXPORT int m3fs_mindex_value_info(
    m3fs *fs, const char *path, uint32_t value,
    m3fs_mindex_value *out);
M3FS_EXPORT int m3fs_mindex_token(
    m3fs *fs, const char *path, uint32_t value, uint32_t ordinal,
    uint64_t *out_token);
M3FS_EXPORT int m3fs_mindex_char(
    m3fs *fs, const char *path, uint32_t value, uint32_t ordinal,
    uint16_t *out_unit);
M3FS_EXPORT int m3fs_mindex_compare(
    m3fs *fs, const char *path, uint32_t left, uint32_t right,
    int32_t *out_comparison);
M3FS_EXPORT int m3fs_mindex_token_index_of(
    m3fs *fs, const char *path, uint32_t value,
    const uint64_t *needle, uint32_t needle_count, uint32_t from_token,
    int32_t *out_index);
M3FS_EXPORT int m3fs_regex_match_all(
    m3fs *fs,
    const char *path,
    const m3fs_regex_program *program,
    uint32_t mode,
    uint64_t *match_words,
    uint64_t *unsupported_words,
    uint32_t word_capacity,
    uint32_t *value_count,
    uint32_t *words_required);

#ifdef __cplusplus
}
#endif
#endif
