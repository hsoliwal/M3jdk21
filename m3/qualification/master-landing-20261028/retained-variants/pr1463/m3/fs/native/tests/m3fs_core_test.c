// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int checks;

static int check(int condition, const char *label) {
  ++checks;
  if (condition) return 1;
  fprintf(stderr, "FAIL %s\n", label);
  return 0;
}

static void remove_database(const char *path) {
  char sidecar[1024];
  (void)remove(path);
  (void)snprintf(sidecar, sizeof(sidecar), "%s-wal", path);
  (void)remove(sidecar);
  (void)snprintf(sidecar, sizeof(sidecar), "%s-shm", path);
  (void)remove(sidecar);
}

typedef struct names {
  int count;
  int saw_a;
  int saw_b;
  unsigned mask;
} names;

static int list_cb(
    void *context,
    const char *name,
    uint32_t name_bytes,
    const m3fs_stat *stat) {
  names *value = (names *)context;
  ++value->count;
  const char *expected[] = {"a", "b", "sparse", "vector", "a2", "a-link"};
  for (unsigned i = 0U; i < 6U; ++i) {
    if (strlen(expected[i]) == name_bytes && memcmp(name, expected[i], name_bytes) == 0) value->mask |= 1U << i;
  }
  if (name_bytes == 1U && name[0] == 'a') value->saw_a = 1;
  if (name_bytes == 1U && name[0] == 'b') value->saw_b = 1;
  return stat->inode > 0U ? M3FS_OK : M3FS_CORRUPT;
}

int main(int argc, char **argv) {
  const char *path = argc > 1 ? argv[1] : "m3fs-native-test.sqlite";
  remove_database(path);

  m3fs *fs = NULL;
  int status = m3fs_open(path, M3FS_OPEN_CREATE, &fs);
  if (!check(status == M3FS_OK && fs != NULL, "create/open")) return 1;
  if (!check(m3fs_abi_version() == 1, "ABI v1")) return 1;
  if (!check(
          (m3fs_capabilities() & M3FS_CAP_WRITEV) != 0U,
          "writev capability")) return 1;
  if (!check(
          (m3fs_capabilities() & M3FS_CAP_READV) != 0U,
          "readv capability")) return 1;
  if (!check(m3fs_verify(fs) == M3FS_OK, "initial verify")) return 1;

  if (!check(m3fs_mkdir(fs, "/d", 0755U) == M3FS_OK, "mkdir")) return 1;
  if (!check(m3fs_create(fs, "/d/a", 0644U) == M3FS_OK, "create a")) return 1;
  if (!check(m3fs_create(fs, "/d/b", 0644U) == M3FS_OK, "create b")) return 1;

  unsigned char *block = (unsigned char *)malloc(M3FS_BLOCK_SIZE);
  if (!check(block != NULL, "block allocation")) return 1;
  memset(block, 'Q', M3FS_BLOCK_SIZE);
  if (!check(
          m3fs_write(fs, "/d/a", 0U, block, M3FS_BLOCK_SIZE, NULL)
              == M3FS_OK,
          "write a")) return 1;
  if (!check(
          m3fs_write(fs, "/d/b", 0U, block, M3FS_BLOCK_SIZE, NULL)
              == M3FS_OK,
          "write b")) return 1;

  m3fs_stats stats;
  if (!check(m3fs_get_stats(fs, &stats) == M3FS_OK, "stats")) return 1;
  if (!check(stats.unique_blocks == 1U, "exact block dedup")) return 1;
  if (!check(stats.block_references == 2U, "two logical block references")) return 1;
  if (!check(stats.physical_block_bytes == M3FS_BLOCK_SIZE, "one physical block")) return 1;
  if (!check(stats.logical_bytes == (uint64_t)M3FS_BLOCK_SIZE * 2U, "logical bytes")) return 1;

  if (!check(m3fs_create(fs, "/d/sparse", 0644U) == M3FS_OK, "create sparse")) return 1;
  static const unsigned char xyz[] = {'X', 'Y', 'Z'};
  if (!check(
          m3fs_write(
              fs, "/d/sparse", (uint64_t)M3FS_BLOCK_SIZE + 5U, xyz, 3U, NULL)
              == M3FS_OK,
          "sparse write")) return 1;
  unsigned char sparse[16];
  memset(sparse, 0xff, sizeof(sparse));
  uint32_t read = 0U;
  if (!check(
          m3fs_read(
              fs, "/d/sparse", M3FS_BLOCK_SIZE, sparse, sizeof(sparse), &read)
              == M3FS_OK,
          "sparse read")) return 1;
  if (!check(read == 8U, "sparse EOF count")) return 1;
  if (!check(
          sparse[0] == 0U && sparse[1] == 0U && sparse[2] == 0U
              && sparse[3] == 0U && sparse[4] == 0U
              && sparse[5] == 'X' && sparse[6] == 'Y' && sparse[7] == 'Z',
          "sparse zero fill")) return 1;

  if (!check(m3fs_create(fs, "/d/vector", 0644U) == M3FS_OK,
          "create vector")) return 1;
  static const unsigned char ab[] = {'A', 'B'};
  static const unsigned char cde[] = {'C', 'D', 'E'};
  m3fs_iovec vector[] = {{ab, 2U}, {NULL, 0U}, {cde, 3U}};
  uint64_t vector_size = 0U;
  if (!check(
          m3fs_writev(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE - 2U,
              vector, 3U, &vector_size) == M3FS_OK,
          "writev across block seam")) return 1;
  if (!check(vector_size == (uint64_t)M3FS_BLOCK_SIZE + 3U,
          "writev resulting size")) return 1;
  m3fs_stat vector_stat;
  if (!check(
          m3fs_stat_path(fs, "/d/vector", &vector_stat) == M3FS_OK
              && vector_stat.generation == 2U,
          "writev touches inode once")) return 1;
  unsigned char vector_read[8];
  memset(vector_read, 0xff, sizeof(vector_read));
  if (!check(
          m3fs_read(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE - 4U,
              vector_read, sizeof(vector_read), &read) == M3FS_OK,
          "read writev result")) return 1;
  if (!check(
          read == 7U
              && vector_read[0] == 0U && vector_read[1] == 0U
              && vector_read[2] == 'A' && vector_read[3] == 'B'
              && vector_read[4] == 'C' && vector_read[5] == 'D'
              && vector_read[6] == 'E',
          "writev concatenation order")) return 1;

  unsigned char readv_head[2] = {0xeeU, 0xeeU};
  unsigned char readv_tail[5] = {0xeeU, 0xeeU, 0xeeU, 0xeeU, 0xeeU};
  m3fs_mut_iovec read_vector[] = {
      {readv_head, 2U}, {NULL, 0U}, {readv_tail, 5U}};
  uint64_t readv_bytes = UINT64_MAX;
  if (!check(
          m3fs_readv(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE - 4U,
              read_vector, 3U, &readv_bytes) == M3FS_OK,
          "readv across block seam")) return 1;
  if (!check(
          readv_bytes == 7U
              && readv_head[0] == 0U && readv_head[1] == 0U
              && readv_tail[0] == 'A' && readv_tail[1] == 'B'
              && readv_tail[2] == 'C' && readv_tail[3] == 'D'
              && readv_tail[4] == 'E',
          "readv concatenation order")) return 1;

  unsigned char eof_head[1] = {0xeeU};
  unsigned char eof_tail[4] = {0xeeU, 0xeeU, 0xeeU, 0xeeU};
  m3fs_mut_iovec eof_vector[] = {{eof_head, 1U}, {eof_tail, 4U}};
  readv_bytes = UINT64_MAX;
  if (!check(
          m3fs_readv(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE + 1U,
              eof_vector, 2U, &readv_bytes) == M3FS_OK,
          "readv EOF clipping")) return 1;
  if (!check(
          readv_bytes == 2U && eof_head[0] == 'D' && eof_tail[0] == 'E'
              && eof_tail[1] == 0xeeU && eof_tail[2] == 0xeeU && eof_tail[3] == 0xeeU,
          "readv leaves suffix after EOF untouched")) return 1;

  m3fs_mut_iovec invalid_read = {NULL, 1U};
  readv_bytes = UINT64_MAX;
  if (!check(
          m3fs_readv(fs, "/d/vector", 0U, &invalid_read, 1U, &readv_bytes)
              == M3FS_INVALID && readv_bytes == 0U,
          "invalid readv rejected before read view")) return 1;
  readv_bytes = UINT64_MAX;
  if (!check(
          m3fs_readv(
              fs, "/d/vector", 0U, NULL, M3FS_MAX_READ_SEGMENTS + 1U, &readv_bytes)
              == M3FS_INVALID && readv_bytes == 0U,
          "readv segment budget")) return 1;
  readv_bytes = UINT64_MAX;
  if (!check(
          m3fs_readv(fs, "/d/vector", 0U, NULL, 0U, &readv_bytes)
              == M3FS_OK && readv_bytes == 0U,
          "empty readv")) return 1;

  static const unsigned char x[] = {'x'};
  static const unsigned char yz[] = {'y', 'z'};
  m3fs_iovec overwrite[] = {{x, 1U}, {yz, 2U}};
  if (!check(
          m3fs_writev(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE - 1U,
              overwrite, 2U, NULL) == M3FS_OK,
          "writev overwrite seam")) return 1;
  memset(vector_read, 0xff, sizeof(vector_read));
  if (!check(
          m3fs_read(
              fs, "/d/vector", (uint64_t)M3FS_BLOCK_SIZE - 2U,
              vector_read, 5U, &read) == M3FS_OK,
          "read writev overwrite")) return 1;
  if (!check(
          read == 5U && vector_read[0] == 'A' && vector_read[1] == 'x'
              && vector_read[2] == 'y' && vector_read[3] == 'z'
              && vector_read[4] == 'E',
          "writev overwrite bytes")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/vector", &vector_stat) == M3FS_OK
              && vector_stat.generation == 3U,
          "writev second generation")) return 1;

  m3fs_iovec invalid = {NULL, 1U};
  uint64_t failed_size = UINT64_C(1234567);
  if (!check(
          m3fs_writev(fs, "/d/vector", 0U, &invalid, 1U, &failed_size)
              == M3FS_INVALID,
          "invalid writev rejected before transaction")) return 1;
  if (!check(failed_size == UINT64_C(1234567), "failed writev does not publish size")) return 1;
  if (!check(
          m3fs_writev(fs, "/d/vector", UINT64_MAX, overwrite, 1U, NULL)
              == M3FS_INVALID,
          "writev offset overflow rejected")) return 1;
  if (!check(
          m3fs_writev(
              fs, "/d/vector", 0U, NULL, M3FS_MAX_WRITE_SEGMENTS + 1U, NULL)
              == M3FS_INVALID,
          "writev segment budget")) return 1;
  uint64_t unchanged_size = 0U;
  if (!check(
          m3fs_writev(fs, "/d/vector", 0U, NULL, 0U, &unchanged_size)
              == M3FS_OK
              && unchanged_size == vector_size,
          "empty writev no-op")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/vector", &vector_stat) == M3FS_OK
              && vector_stat.generation == 3U,
          "empty writev preserves generation")) return 1;

  if (!check(m3fs_link(fs, "/d/a", "/d/a2") == M3FS_OK, "hard link")) return 1;
  m3fs_stat stat;
  if (!check(m3fs_stat_path(fs, "/d/a2", &stat) == M3FS_OK, "hardlink stat")) return 1;
  if (!check(stat.nlink == 2U, "hardlink count")) return 1;
  if (!check(
          m3fs_rename(fs, "/d/a", "/d/a2") == M3FS_OK,
          "same-inode rename no-op")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/a", &stat) == M3FS_OK
              && stat.nlink == 2U,
          "same-inode rename retains source")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/a2", &stat) == M3FS_OK
              && stat.nlink == 2U,
          "same-inode rename retains target")) return 1;

  if (!check(
          m3fs_symlink(fs, "../d/a", "/d/a-link") == M3FS_OK,
          "symlink")) return 1;
  char target[64];
  uint32_t target_bytes = 0U;
  if (!check(
          m3fs_readlink(fs, "/d/a-link", target, sizeof(target), &target_bytes)
              == M3FS_OK,
          "readlink")) return 1;
  if (!check(
          target_bytes == strlen("../d/a")
              && memcmp(target, "../d/a", target_bytes) == 0,
          "symlink target")) return 1;

  names listed = {0, 0, 0, 0U};
  if (!check(m3fs_list(fs, "/d", list_cb, &listed) == M3FS_OK, "list")) return 1;
  if (!check(listed.count == 6 && listed.saw_a && listed.saw_b && listed.mask == 63U, "list contents")) return 1;

  if (!check(m3fs_rename(fs, "/d/b", "/d/c") == M3FS_OK, "rename")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/b", &stat) == M3FS_NOT_FOUND,
          "old rename path absent")) return 1;
  if (!check(
          m3fs_stat_path(fs, "/d/c", &stat) == M3FS_OK,
          "new rename path present")) return 1;

  if (!check(m3fs_truncate(fs, "/d/a", 3U) == M3FS_OK, "truncate down")) return 1;
  unsigned char ten[10];
  memset(ten, 0xaa, sizeof(ten));
  if (!check(m3fs_read(fs, "/d/a2", 0U, ten, sizeof(ten), &read) == M3FS_OK, "read truncated")) return 1;
  if (!check(read == 3U && ten[0] == 'Q' && ten[1] == 'Q' && ten[2] == 'Q', "truncate shared inode")) return 1;
  if (!check(m3fs_truncate(fs, "/d/a", 10U) == M3FS_OK, "truncate extend")) return 1;
  memset(ten, 0xaa, sizeof(ten));
  if (!check(m3fs_read(fs, "/d/a", 0U, ten, sizeof(ten), &read) == M3FS_OK, "read extended")) return 1;
  if (!check(read == 10U, "extended count")) return 1;
  if (!check(ten[0] == 'Q' && ten[1] == 'Q' && ten[2] == 'Q', "prefix retained")) return 1;
  for (int i = 3; i < 10; ++i) {
    if (!check(ten[i] == 0U, "truncated tail remains zero")) return 1;
  }

  if (!check(m3fs_rmdir(fs, "/d") == M3FS_NOT_EMPTY, "non-empty rmdir")) return 1;
  if (!check(m3fs_verify(fs) == M3FS_OK, "post-mutation verify")) return 1;
  if (!check(m3fs_checkpoint(fs) == M3FS_OK, "checkpoint")) return 1;

  free(block);
  m3fs_close(fs);
  fs = NULL;

  if (!check(m3fs_open(path, 0U, &fs) == M3FS_OK, "reopen")) return 1;
  memset(ten, 0xaa, sizeof(ten));
  if (!check(m3fs_read(fs, "/d/a", 0U, ten, sizeof(ten), &read) == M3FS_OK, "reopen read")) return 1;
  if (!check(read == 10U && ten[3] == 0U, "reopen bytes")) return 1;
  if (!check(m3fs_verify(fs) == M3FS_OK, "reopen verify")) return 1;
  m3fs_close(fs);

  fs = NULL;
  if (!check(
          m3fs_open(path, M3FS_OPEN_READ_ONLY, &fs) == M3FS_OK,
          "read-only reopen")) return 1;
  if (!check(
          m3fs_write(fs, "/d/a", 0U, xyz, 3U, NULL) == M3FS_READ_ONLY,
          "read-only mutation rejected")) return 1;
  m3fs_close(fs);

  remove_database(path);
  printf("PASS %d M3FS native checks\n", checks);
  return 0;
}
