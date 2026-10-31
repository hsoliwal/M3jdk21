// SPDX-License-Identifier: Apache-2.0
/* Instrument the actual engine only. SQLite's own allocations/copies are not counted here. */
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>

static uint64_t allocations, copies, zeroes;
static void *count_malloc(size_t n) { ++allocations; return malloc(n); }
static void *count_copy(void *out, const void *in, size_t n) { copies += n; return memcpy(out, in, n); }
static void *count_zero(void *out, int value, size_t n) { zeroes += n; return memset(out, value, n); }
#define malloc count_malloc
#define memcpy count_copy
#define memset count_zero
#include "../src/m3fs.c"
#undef malloc
#undef memcpy
#undef memset

static unsigned checks;
#define CHECK(c) do { ++checks; if (!(c)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
#define OK(c) CHECK((c) == M3FS_OK)
static uint32_t seed = 20260928U;
static uint32_t next_random(void) { seed ^= seed << 13U; seed ^= seed >> 17U; seed ^= seed << 5U; return seed; }
static void reset_counts(void) { allocations = copies = zeroes = 0U; }
static int runs(sqlite3_stmt *stmt) { return stmt == NULL ? 0 : sqlite3_stmt_status(stmt, SQLITE_STMTSTATUS_RUN, 0); }

static void idle_cache(m3fs *fs) {
  sqlite3_stmt *allowed[] = {fs->read_block, fs->read_range, fs->lookup_child,
      fs->stat_inode, fs->insert_block, fs->find_block, fs->link_block,
      fs->touch_inode, fs->directory_page, fs->insert_integrity, fs->read_integrity};
  unsigned count = 0U;
  for (sqlite3_stmt *stmt = sqlite3_next_stmt(fs->db, NULL); stmt != NULL;
      stmt = sqlite3_next_stmt(fs->db, stmt)) {
    CHECK(sqlite3_stmt_busy(stmt) == 0);
    int recognized = 0;
    for (size_t i = 0U; i < sizeof(allowed) / sizeof(allowed[0]); ++i) {
      if (allowed[i] == stmt) recognized = 1;
    }
    CHECK(recognized);
    ++count;
    /* Every retained parameter is SQL NULL after its call, including borrowed BLOBs. */
    char *expanded = sqlite3_expanded_sql(stmt);
    CHECK(expanded != NULL && strstr(expanded, "x'") == NULL);
    sqlite3_free(expanded);
  }
  CHECK(count <= 11U);
}
static int sink(void *ctx, const char *name, uint32_t bytes, const m3fs_stat *stat) {
  (void)ctx; (void)name; (void)bytes; (void)stat; return M3FS_OK;
}
static int stop(void *ctx, const char *name, uint32_t bytes, const m3fs_stat *stat) {
  (void)ctx; (void)name; (void)bytes; (void)stat; return M3FS_IO;
}
static int deny_insert(void *ctx, int action, const char *table, const char *b, const char *db, const char *trigger) {
  (void)ctx; (void)b; (void)db; (void)trigger;
  return action == SQLITE_INSERT && table != NULL && strcmp(table, "blocks") == 0 ? SQLITE_DENY : SQLITE_OK;
}
static void verify_range_plan(m3fs *fs) {
  sqlite3_stmt *stmt = NULL;
  CHECK(sqlite3_prepare_v2(fs->db,
      "EXPLAIN QUERY PLAN SELECT f.block_no,b.data FROM file_blocks f LEFT JOIN blocks b ON b.id=f.block_id"
      " WHERE f.inode=?1 AND f.block_no>=?2 AND f.block_no<=?3 ORDER BY f.block_no",
      -1, &stmt, NULL) == SQLITE_OK);
  unsigned rows = 0U;
  int rc;
  while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
    const char *detail = (const char *)sqlite3_column_text(stmt, 3);
    CHECK(detail != NULL && strstr(detail, "SCAN ") == NULL && strstr(detail, "TEMP B-TREE") == NULL);
    puts(detail);
    ++rows;
  }
  CHECK(rc == SQLITE_DONE && rows == 2U);
  CHECK(sqlite3_finalize(stmt) == SQLITE_OK);
}
int main(int argc, char **argv) {
  CHECK(argc == 2);
  const uint32_t bytes = M3FS_BLOCK_SIZE * 8U;
  unsigned char *data = (unsigned char *)malloc(bytes);
  unsigned char *out = (unsigned char *)malloc(bytes + 32U);
  unsigned char *expected = (unsigned char *)malloc(bytes);
  CHECK(data != NULL && out != NULL && expected != NULL);
  for (uint32_t i = 0U; i < bytes; ++i) data[i] = (unsigned char)next_random();
  memcpy(expected, data, bytes);
  m3fs *fs = NULL, *snapshot = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  OK(m3fs_mkdir(fs, "/dir", 0700U));
  OK(m3fs_create(fs, "/dir/value", 0600U));
  reset_counts();
  OK(m3fs_write(fs, "/dir/value", 0U, data, bytes, NULL));
  CHECK(allocations == 0U && copies == 0U && fs->block_scratch == NULL);
  CHECK(fs->read_block == NULL && fs->find_block == NULL);
  CHECK(runs(fs->insert_block) == 8 && runs(fs->link_block) == 8);
  puts("MECHANISM full-write blocks=8 old-block-reads=0 engine-copy-bytes=0 scratch-allocations=0 exact-id-lookups=0");
  idle_cache(fs);
  /* Duplicate values use exact lookup, preserving both identity and unique-block counts. */
  OK(m3fs_write(fs, "/dir/value", 0U, data, bytes, NULL));
  CHECK(runs(fs->find_block) == 8 && fs->block_scratch == NULL);
  m3fs_stats stats;
  OK(m3fs_get_stats(fs, &stats));
  CHECK(stats.unique_blocks == 8U && stats.block_references == 8U);
  uint32_t got = 0U;
  memset(out, 0xee, bytes + 32U);
  reset_counts();
  OK(m3fs_read(fs, "/dir/value", 0U, out + 16U, bytes, &got));
  CHECK(got == bytes && memcmp(out + 16U, data, bytes) == 0);
  CHECK(allocations == 0U && copies == bytes && zeroes == 0U && fs->block_scratch == NULL);
  CHECK(runs(fs->read_range) == 1 && fs->read_block == NULL);
  for (uint32_t i = 0U; i < 16U; ++i) CHECK(out[i] == 0xee && out[bytes + 16U + i] == 0xee);
  puts("MECHANISM dense-read blocks=8 range-executions=1 engine-copy-bytes=524288 scratch-allocations=0 zero-overwrite-bytes=0");
  reset_counts();
  OK(m3fs_read(fs, "/dir/value", 7U, out, 1U, &got));
  CHECK(got == 1U && out[0] == data[7] && copies == 1U && zeroes == 0U && allocations == 0U);
  /* Thousands of unaligned/zero/EOF reads validate exact clipping and untouched guards. */
  for (uint32_t i = 0U; i < 2000U; ++i) {
    uint32_t start = next_random() % (bytes + 1U);
    uint32_t count = next_random() % 70003U;
    uint32_t available = bytes - start;
    uint32_t want = count < available ? count : available;
    memset(out, 0xee, count + 32U);
    OK(m3fs_read(fs, "/dir/value", start, out + 16U, count, &got));
    CHECK(got == want && memcmp(out + 16U, data + start, want) == 0);
    CHECK(out[15] == 0xee && out[16U + got] == 0xee);
  }
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT, &snapshot));
  uint64_t cloned = 0U;
  OK(m3fs_clone_file(fs, "/dir/value", "/dir/clone", &cloned));
  CHECK(cloned != 0U);
  for (uint32_t i = 0U; i < 100U; ++i) {
    uint32_t at = next_random() % (bytes - 2001U);
    uint32_t count = 1U + next_random() % 2000U;
    memset(out, (int)(i & 255U), count);
    OK(m3fs_write(fs, "/dir/clone", at, out, count, NULL));
    memcpy(expected + at, out, count);
    CHECK(fs->block_scratch != NULL);
  }
  unsigned char *scratch = fs->block_scratch;
  OK(m3fs_read(fs, "/dir/clone", 0U, out, bytes, &got));
  CHECK(got == bytes && memcmp(out, expected, bytes) == 0);
  OK(m3fs_read(snapshot, "/dir/value", 0U, out, bytes, &got));
  CHECK(memcmp(out, data, bytes) == 0 && snapshot->block_scratch == NULL);
  OK(m3fs_truncate(fs, "/dir/clone", 65537U));
  CHECK(fs->block_scratch == scratch);
  OK(m3fs_truncate(fs, "/dir/clone", bytes));
  memset(expected + 65537U, 0, bytes - 65537U);
  OK(m3fs_read(fs, "/dir/clone", 0U, out, bytes, &got));
  CHECK(memcmp(out, expected, bytes) == 0);
  /* Sparse gaps must be zero-filled once, without point queries per logical block. */
  OK(m3fs_create(fs, "/sparse", 0600U));
  OK(m3fs_truncate(fs, "/sparse", bytes));
  OK(m3fs_write(fs, "/sparse", M3FS_BLOCK_SIZE * 4U, data, M3FS_BLOCK_SIZE, NULL));
  int before = runs(fs->read_range);
  reset_counts();
  OK(m3fs_read(fs, "/sparse", 0U, out, bytes, &got));
  CHECK(got == bytes && copies == M3FS_BLOCK_SIZE && zeroes == bytes - M3FS_BLOCK_SIZE);
  CHECK(allocations == 0U && runs(fs->read_range) == before + 1);
  for (uint32_t i = 0U; i < bytes; ++i) {
    unsigned char wanted = i >= 4U * M3FS_BLOCK_SIZE && i < 5U * M3FS_BLOCK_SIZE
        ? data[i - 4U * M3FS_BLOCK_SIZE] : 0;
    CHECK(out[i] == wanted);
  }
  puts("MECHANISM sparse-read logical-blocks=8 allocated-blocks=1 range-executions=1 copied=65536 zeroed=458752");
  uint32_t count = 0U, more = 0U;
  OK(m3fs_list_page(fs, "/dir", NULL, 1U, sink, NULL, &count, &more));
  sqlite3_stmt *page = fs->directory_page;
  CHECK(page != NULL && count == 1U && more == 1U);
  CHECK(m3fs_list_page(fs, "/dir", NULL, 1U, stop, NULL, &count, &more) == M3FS_IO);
  CHECK(count == 0U && more == 0U);
  OK(m3fs_list_page(fs, "/dir", "clone", 1U, sink, NULL, &count, &more));
  CHECK(fs->directory_page == page && count == 1U && more == 0U);
  idle_cache(fs);
  /* Cached namespace SQL must never cache the old answer. */
  OK(m3fs_rename(fs, "/dir/value", "/dir/moved"));
  CHECK(m3fs_read(fs, "/dir/value", 0U, out, 1U, &got) == M3FS_NOT_FOUND);
  OK(m3fs_read(snapshot, "/dir/value", 0U, out, 1U, &got));
  CHECK(out[0] == data[0]);
  OK(m3fs_create(fs, "/dir/value", 0600U));
  OK(m3fs_read(fs, "/dir/value", 0U, out, 1U, &got));
  CHECK(got == 0U);
  /* Mutation failure after one complete block rolls back data, metadata, references and admission. */
  CHECK(sqlite3_exec(fs->db,
      "CREATE TEMP TRIGGER fail_ref BEFORE UPDATE ON file_blocks WHEN NEW.block_no=1"
      " BEGIN SELECT RAISE(ABORT,'injected'); END;", NULL, NULL, NULL) == SQLITE_OK);
  memset(expected, 0x5a, bytes);
  CHECK(m3fs_write(fs, "/dir/moved", 0U, expected, bytes, NULL) != M3FS_OK);
  idle_cache(fs);
  OK(m3fs_read(fs, "/dir/moved", 0U, out, bytes, &got));
  CHECK(memcmp(out, data, bytes) == 0);
  CHECK(sqlite3_exec(fs->db, "DROP TRIGGER fail_ref", NULL, NULL, NULL) == SQLITE_OK);
  CHECK(sqlite3_set_authorizer(fs->db, deny_insert, NULL) == SQLITE_OK);
  CHECK(m3fs_write(fs, "/dir/moved", 0U, expected, bytes, NULL) != M3FS_OK);
  idle_cache(fs);
  CHECK(sqlite3_set_authorizer(fs->db, NULL, NULL) == SQLITE_OK);
  OK(m3fs_write(fs, "/dir/moved", 0U, expected, bytes, NULL));
  memset(expected, 0, bytes); /* Borrowed source must no longer be reachable through SQLite bindings. */
  OK(m3fs_read(fs, "/dir/moved", 0U, out, bytes, &got));
  for (uint32_t i = 0U; i < got; ++i) CHECK(out[i] == 0x5a);
  idle_cache(fs);
  verify_range_plan(fs);
  OK(m3fs_verify(fs));
  m3fs_close(snapshot);
  m3fs_close(fs);
  free(expected); free(out); free(data);
  printf("PASS hotpath-native checks=%u seed=20260928 cache-bound=11 scratch-bound=65536\n", checks);
  return 0;
}
