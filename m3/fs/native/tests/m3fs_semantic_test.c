// SPDX-License-Identifier: Apache-2.0
#include "m3fs_semantic.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define IMG_MAGIC UINT64_C(0x4d334d4944583031)
#define IMG_FORMAT 1U
#define IMG_HEADER 64U

static void put16(unsigned char *data, size_t offset, uint16_t value) {
  data[offset] = (unsigned char)(value >> 8);
  data[offset + 1U] = (unsigned char)value;
}

static void put32(unsigned char *data, size_t offset, uint32_t value) {
  data[offset] = (unsigned char)(value >> 24);
  data[offset + 1U] = (unsigned char)(value >> 16);
  data[offset + 2U] = (unsigned char)(value >> 8);
  data[offset + 3U] = (unsigned char)value;
}

static void put64(unsigned char *data, size_t offset, uint64_t value) {
  for (size_t index = 0U; index < 8U; ++index) {
    data[offset + 7U - index] = (unsigned char)(value >> (index * 8U));
  }
}

static int32_t java_hash(const uint16_t *value, uint32_t length) {
  uint32_t hash = 0U;
  for (uint32_t index = 0U; index < length; ++index) {
    hash = hash * 31U + value[index];
  }
  return (int32_t)hash;
}

static int fail(m3fs *fs, const char *message, int status) {
  if (fs != NULL) m3fs_close(fs);
  (void)fprintf(stderr, "FAIL %s status=%d\n", message, status);
  return 1;
}

int main(int argc, char **argv) {
  static const uint16_t v0[] = {'a', 'b'};
  static const uint16_t v1[] = {'z', 'a', 'b', 'z'};
  static const uint16_t v2[] = {'A', ' ', 'B'};
  static const uint16_t v3[] = {0xd83dU, 0xde00U};
  static const uint16_t *values[] = {v0, v1, v2, v3};
  static const uint32_t lengths[] = {2U, 4U, 3U, 2U};

  if (argc != 2 || argv[1] == NULL) return 2;

  const uint32_t value_count = 4U;
  const uint32_t token_count = 11U;
  const uint32_t unit_count = 11U;
  const uint32_t token_starts = IMG_HEADER;
  const uint32_t utf16_starts = token_starts + 5U * 4U;
  const uint32_t token_ids = utf16_starts + 5U * 4U;
  const uint32_t utf16_units = token_ids + token_count * 8U;
  const uint32_t hashes = utf16_units + unit_count * 2U;
  const uint32_t signals = hashes + value_count * 4U;
  const uint32_t end = signals + value_count * 8U;
  unsigned char *image = (unsigned char *)calloc(end, 1U);
  if (image == NULL) return 3;

  put64(image, 0U, IMG_MAGIC);
  put32(image, 8U, IMG_FORMAT);
  put32(image, 12U, value_count);
  put32(image, 16U, token_count);
  put32(image, 20U, unit_count);
  put32(image, 24U, token_starts);
  put32(image, 28U, utf16_starts);
  put32(image, 32U, token_ids);
  put32(image, 36U, utf16_units);
  put32(image, 40U, hashes);
  put32(image, 44U, signals);
  put32(image, 48U, end);

  uint32_t token_cursor = 0U;
  uint32_t char_cursor = 0U;
  uint64_t next_token = UINT64_C(100);
  uint64_t saved_v1_tokens[2] = {0U, 0U};
  for (uint32_t value = 0U; value < value_count; ++value) {
    put32(image, token_starts + value * 4U, token_cursor);
    put32(image, utf16_starts + value * 4U, char_cursor);
    for (uint32_t ordinal = 0U; ordinal < lengths[value]; ++ordinal) {
      uint64_t token = next_token++;
      if (value == 1U && (ordinal == 1U || ordinal == 2U)) {
        saved_v1_tokens[ordinal - 1U] = token;
      }
      put64(image, token_ids + token_cursor * 8U, token);
      put16(image, utf16_units + char_cursor * 2U, values[value][ordinal]);
      ++token_cursor;
      ++char_cursor;
    }
    put32(image, hashes + value * 4U, (uint32_t)java_hash(values[value], lengths[value]));
    put64(image, signals + value * 8U, UINT64_MAX);
  }
  put32(image, token_starts + value_count * 4U, token_cursor);
  put32(image, utf16_starts + value_count * 4U, char_cursor);

  m3fs *fs = NULL;
  int status = m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs);
  if (status != M3FS_OK) {
    free(image);
    return fail(fs, "open", status);
  }
  if ((m3fs_capabilities() & (M3FS_CAP_NATIVE_MINDEX | M3FS_CAP_NATIVE_REGEX))
      != (M3FS_CAP_NATIVE_MINDEX | M3FS_CAP_NATIVE_REGEX)) {
    free(image);
    return fail(fs, "capabilities", M3FS_INVALID);
  }
  status = m3fs_create(fs, "/values.midx", 0644U);
  if (status == M3FS_OK) {
    uint64_t size = 0U;
    status = m3fs_write(fs, "/values.midx", 0U, image, end, &size);
    if (status == M3FS_OK && size != end) status = M3FS_CORRUPT;
  }
  free(image);
  if (status != M3FS_OK) return fail(fs, "publish image", status);

  m3fs_mindex_info_data info;
  status = m3fs_mindex_info(fs, "/values.midx", &info);
  if (status != M3FS_OK
      || info.value_count != 4U
      || info.token_count != 11U
      || info.utf16_count != 11U) {
    return fail(fs, "image info", status);
  }

  m3fs_mindex_value value_info;
  status = m3fs_mindex_value_info(fs, "/values.midx", 1U, &value_info);
  if (status != M3FS_OK
      || value_info.token_count != 4U
      || value_info.utf16_length != 4U
      || value_info.java_hash != java_hash(v1, 4U)) {
    return fail(fs, "value info", status);
  }

  uint16_t unit = 0U;
  status = m3fs_mindex_char(fs, "/values.midx", 1U, 2U, &unit);
  if (status != M3FS_OK || unit != (uint16_t)'b') {
    return fail(fs, "char", status);
  }

  int32_t index = -1;
  status =
      m3fs_mindex_token_index_of(
          fs, "/values.midx", 1U, saved_v1_tokens, 2U, 0U, &index);
  if (status != M3FS_OK || index != 1) {
    return fail(fs, "token index", status);
  }

  static const uint8_t op[] = {
      M3FS_REGEX_OP_CHAR,
      M3FS_REGEX_OP_CHAR,
      M3FS_REGEX_OP_MATCH};
  static const int32_t out1[] = {1, 2, -1};
  static const int32_t out2[] = {-1, -1, -1};
  static const int32_t class_id[] = {0, 1, -1};
  static const uint64_t ascii_low[] = {0U, 0U};
  static const uint64_t ascii_high[] = {
      UINT64_C(1) << ('a' - 64),
      UINT64_C(1) << ('b' - 64)};
  static const int32_t range_offset[] = {0, 0};
  static const int32_t range_count[] = {0, 0};

  m3fs_regex_program regex;
  memset(&regex, 0, sizeof(regex));
  regex.op = op;
  regex.out1 = out1;
  regex.out2 = out2;
  regex.class_id = class_id;
  regex.state_count = 3U;
  regex.ascii_low = ascii_low;
  regex.ascii_high = ascii_high;
  regex.range_offset = range_offset;
  regex.range_count = range_count;
  regex.class_count = 2U;
  regex.range_low = NULL;
  regex.range_high = NULL;
  regex.range_count_total = 0U;
  regex.start_state = 0U;
  regex.match_state = 2U;
  regex.min_length = 2;
  regex.max_length = 2;

  uint64_t matches[1] = {0U};
  uint64_t unsupported[1] = {0U};
  uint32_t values_out = 0U;
  uint32_t words = 0U;
  status =
      m3fs_regex_match_all(
          fs, "/values.midx", &regex, M3FS_REGEX_FIND,
          matches, unsupported, 1U, &values_out, &words);
  if (status != M3FS_OK
      || values_out != 4U
      || words != 1U
      || (matches[0] & UINT64_C(0x3)) != UINT64_C(0x3)
      || unsupported[0] != 0U) {
    return fail(fs, "regex find", status);
  }

  regex.may_consume_surrogate_unit = 1U;
  static const uint16_t range_low[] = {128U};
  static const uint16_t range_high[] = {0xffffU};
  static const uint64_t any_low[] = {UINT64_MAX};
  static const uint64_t any_high[] = {UINT64_MAX};
  static const int32_t one_range_offset[] = {0};
  static const int32_t one_range_count[] = {1};
  static const int32_t any_class[] = {0, -1};
  static const uint8_t any_op[] = {
      M3FS_REGEX_OP_CHAR,
      M3FS_REGEX_OP_MATCH};
  static const int32_t any_out1[] = {1, -1};
  static const int32_t any_out2[] = {-1, -1};

  regex.op = any_op;
  regex.out1 = any_out1;
  regex.out2 = any_out2;
  regex.class_id = any_class;
  regex.state_count = 2U;
  regex.ascii_low = any_low;
  regex.ascii_high = any_high;
  regex.range_offset = one_range_offset;
  regex.range_count = one_range_count;
  regex.class_count = 1U;
  regex.range_low = range_low;
  regex.range_high = range_high;
  regex.range_count_total = 1U;
  regex.start_state = 0U;
  regex.match_state = 1U;
  regex.min_length = 1;
  regex.max_length = 1;

  matches[0] = 0U;
  unsupported[0] = 0U;
  status =
      m3fs_regex_match_all(
          fs, "/values.midx", &regex, M3FS_REGEX_FIND,
          matches, unsupported, 1U, &values_out, &words);
  if (status != M3FS_OK || (unsupported[0] & (UINT64_C(1) << 3)) == 0U) {
    return fail(fs, "surrogate fallback", status);
  }

  status = m3fs_verify(fs);
  if (status != M3FS_OK) return fail(fs, "verify", status);
  m3fs_close(fs);
  (void)printf("PASS MINDEXFS_NATIVE_SEMANTIC\n");
  return 0;
}
