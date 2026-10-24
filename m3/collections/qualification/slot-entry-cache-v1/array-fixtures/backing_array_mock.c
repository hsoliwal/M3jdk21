/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * TEST ONLY: link actual emitted JNI C + unchanged core; no VM/GC claim.
 */
#include <jni.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
extern jobjectArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeAllocateObjects(JNIEnv *, jclass, jint);
extern jbyteArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeAllocateBytes(JNIEnv *, jclass, jint);
extern jintArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeAllocateInts(JNIEnv *, jclass, jint);
extern jlongArray JNICALL Java_com_m3_arrays_M3ArrayNative_nativeAllocateLongs(JNIEnv *, jclass, jint);
enum scenario {
ENTRY_PENDING, NEGATIVE, SUCCESS, ALLOC_PENDING, ALLOC_SILENT,
OBJECT_PENDING, OBJECT_SILENT, LOOKUP_PENDING, LOOKUP_SILENT,
THROW_PENDING, THROW_SILENT
};
enum { OBJECT_CLASS, NASE_CLASS, OOM_CLASS };
struct token { int tag; };
static struct token classes[3] = {{1}, {2}, {3}};
static struct token arrays[4] = {{4}, {5}, {6}, {7}};
static struct token marker = {8}, nase = {9}, oom = {10};
static unsigned cases, checks;
static struct {
enum scenario scenario;
int type;
jint length;
jthrowable pending;
unsigned exception_checks, finds[3], deletes[3], allocs[4], throws, forbidden;
unsigned live[3], allocated;
const char *message;
int thrown_class;
} state;
static void check(int condition, const char *message) {
checks++;
if (!condition) {
  fprintf(stderr, "MOCK_FAIL case=%u scenario=%d type=%d length=%d check=%u %s\n",
      cases, (int)state.scenario, state.type, (int)state.length, checks, message);
  exit(1);
}
}
static jclass class_handle(int which) { return (jclass)(void *)&classes[which]; }
static jthrowable marker_handle(void) { return (jthrowable)(void *)&marker; }
static int class_index(jobject value) {
for (int i = 0; i < 3; i++) if (value == (jobject)class_handle(i)) return i;
return -1;
}
static void forbidden(const char *name) {
state.forbidden++;
check(0, name);
}
static jboolean JNICALL exception_check(JNIEnv *env) {
(void)env;
state.exception_checks++;
return state.pending != NULL ? JNI_TRUE : JNI_FALSE;
}
static jclass JNICALL find_class(JNIEnv *env, const char *name) {
(void)env;
check(state.pending == NULL, "lookup while pending");
int which = strcmp(name, "java/lang/Object") == 0 ? OBJECT_CLASS
    : strcmp(name, "java/lang/NegativeArraySizeException") == 0 ? NASE_CLASS
    : strcmp(name, "java/lang/OutOfMemoryError") == 0 ? OOM_CLASS : -1;
check(which >= 0, "unknown class");
state.finds[which]++;
check(state.finds[which] == 1, "duplicate class lookup");
if (which == OBJECT_CLASS && (state.scenario == OBJECT_PENDING || state.scenario == OBJECT_SILENT)) {
  if (state.scenario == OBJECT_PENDING) state.pending = marker_handle();
  return NULL;
}
if (which == OOM_CLASS && (state.scenario == LOOKUP_PENDING || state.scenario == LOOKUP_SILENT)) {
  if (state.scenario == LOOKUP_PENDING) state.pending = marker_handle();
  return NULL;
}
check(state.live[which] == 0, "duplicate live class");
state.live[which] = 1;
return class_handle(which);
}
static void JNICALL delete_local(JNIEnv *env, jobject value) {
(void)env;
check(value != NULL, "DeleteLocalRef NULL");
int which = class_index(value);
check(which >= 0, "unowned local deletion");
check(state.live[which] == 1, "DeleteLocalRef double delete");
state.live[which] = 0;
state.deletes[which]++;
}
static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
(void)env;
check(state.pending == NULL, "replacing pending throwable");
int which = class_index((jobject)type);
check(which == NASE_CLASS || which == OOM_CLASS, "unexpected exception class");
check(state.live[which] == 1, "dead ThrowNew class");
state.throws++;
check(state.throws == 1, "repeated exception reporting");
state.message = message;
state.thrown_class = which;
if (state.scenario == THROW_PENDING) { state.pending = marker_handle(); return JNI_ERR; }
if (state.scenario == THROW_SILENT) return JNI_ERR;
state.pending = which == NASE_CLASS ? (jthrowable)(void *)&nase : (jthrowable)(void *)&oom;
return JNI_OK;
}
static jobject new_array(int type, jint length) {
check(state.pending == NULL, "allocation with pending throwable");
check(type == state.type, "wrong typed allocator");
check(length == state.length, "length narrowed or changed");
check(state.allocated == 0, "repeated allocation");
state.allocated = 1;
state.allocs[type]++;
if (state.scenario == SUCCESS) return (jobject)(void *)&arrays[type];
if (state.scenario == ALLOC_PENDING) state.pending = marker_handle();
return NULL;
}
static jobjectArray JNICALL new_objects(JNIEnv *env, jsize length, jclass component, jobject initial) {
(void)env;
check(component == class_handle(OBJECT_CLASS), "Object component type");
check(state.live[OBJECT_CLASS] == 1, "dead Object class");
check(initial == NULL, "nonnull initial");
return (jobjectArray)new_array(0, length);
}
static jbyteArray JNICALL new_bytes(JNIEnv *env, jsize length) {
(void)env; return (jbyteArray)new_array(1, length);
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize length) {
(void)env; return (jintArray)new_array(2, length);
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize length) {
(void)env; return (jlongArray)new_array(3, length);
}
#define BAD_VOID(name, declaration, ignored) \
static void JNICALL name declaration { ignored; forbidden(#name); }
#define BAD_RETURN(name, type, declaration, ignored) \
static type JNICALL name declaration { ignored; forbidden(#name); return 0; }
BAD_VOID(clear_exception, (JNIEnv *e), (void)e)
BAD_VOID(describe_exception, (JNIEnv *e), (void)e)
BAD_RETURN(new_global, jobject, (JNIEnv *e, jobject o), (void)e; (void)o)
BAD_VOID(delete_global, (JNIEnv *e, jobject o), (void)e; (void)o)
BAD_RETURN(new_weak, jweak, (JNIEnv *e, jobject o), (void)e; (void)o)
BAD_VOID(delete_weak, (JNIEnv *e, jweak o), (void)e; (void)o)
BAD_RETURN(critical, void *, (JNIEnv *e, jarray a, jboolean *c), (void)e; (void)a; (void)c)
BAD_VOID(release_critical, (JNIEnv *e, jarray a, void *p, jint m), (void)e; (void)a; (void)p; (void)m)
BAD_RETURN(direct_address, void *, (JNIEnv *e, jobject o), (void)e; (void)o)
#define BAD_PRIMITIVE(tag, unit, array) \
BAD_RETURN(get_##tag, unit *, (JNIEnv *e, array a, jboolean *c), (void)e; (void)a; (void)c) \
BAD_VOID(release_##tag, (JNIEnv *e, array a, unit *p, jint m), (void)e; (void)a; (void)p; (void)m) \
BAD_VOID(read_##tag, (JNIEnv *e, array a, jsize s, jsize n, unit *p), (void)e; (void)a; (void)s; (void)n; (void)p) \
BAD_VOID(write_##tag, (JNIEnv *e, array a, jsize s, jsize n, const unit *p), (void)e; (void)a; (void)s; (void)n; (void)p)
BAD_PRIMITIVE(bytes, jbyte, jbyteArray)
BAD_PRIMITIVE(ints, jint, jintArray)
BAD_PRIMITIVE(longs, jlong, jlongArray)
BAD_PRIMITIVE(chars, jchar, jcharArray)
static const struct JNINativeInterface_ table = {
.ExceptionCheck = exception_check, .FindClass = find_class,
.ThrowNew = throw_new, .DeleteLocalRef = delete_local,
.NewObjectArray = new_objects, .NewByteArray = new_bytes,
.NewIntArray = new_ints, .NewLongArray = new_longs,
.ExceptionClear = clear_exception, .ExceptionDescribe = describe_exception,
.NewGlobalRef = new_global, .DeleteGlobalRef = delete_global,
.NewWeakGlobalRef = new_weak, .DeleteWeakGlobalRef = delete_weak,
.GetPrimitiveArrayCritical = critical, .ReleasePrimitiveArrayCritical = release_critical,
.GetDirectBufferAddress = direct_address,
.GetByteArrayElements = get_bytes, .ReleaseByteArrayElements = release_bytes,
.GetByteArrayRegion = read_bytes, .SetByteArrayRegion = write_bytes,
.GetIntArrayElements = get_ints, .ReleaseIntArrayElements = release_ints,
.GetIntArrayRegion = read_ints, .SetIntArrayRegion = write_ints,
.GetLongArrayElements = get_longs, .ReleaseLongArrayElements = release_longs,
.GetLongArrayRegion = read_longs, .SetLongArrayRegion = write_longs,
.GetCharArrayElements = get_chars, .ReleaseCharArrayElements = release_chars,
.GetCharArrayRegion = read_chars, .SetCharArrayRegion = write_chars
};
static jobject invoke(JNIEnv *env, int type, jint length) {
switch (type) {
  case 0: return (jobject)Java_com_m3_arrays_M3ArrayNative_nativeAllocateObjects(env, NULL, length);
  case 1: return (jobject)Java_com_m3_arrays_M3ArrayNative_nativeAllocateBytes(env, NULL, length);
  case 2: return (jobject)Java_com_m3_arrays_M3ArrayNative_nativeAllocateInts(env, NULL, length);
  default: return (jobject)Java_com_m3_arrays_M3ArrayNative_nativeAllocateLongs(env, NULL, length);
}
}
static void run_case(int type, enum scenario scenario, jint length) {
memset(&state, 0, sizeof(state));
state.type = type; state.scenario = scenario; state.length = length; state.thrown_class = -1;
cases++;
if (scenario == ENTRY_PENDING) state.pending = marker_handle();
JNIEnv env = &table;
jobject result = invoke(&env, type, length);
int object_lookup = type == 0 && scenario != ENTRY_PENDING && scenario != NEGATIVE;
int object_failed = scenario == OBJECT_PENDING || scenario == OBJECT_SILENT;
int allocations = scenario != ENTRY_PENDING && scenario != NEGATIVE && !object_failed;
int oom_lookup = scenario == ALLOC_SILENT || scenario == OBJECT_SILENT
    || scenario == LOOKUP_PENDING || scenario == LOOKUP_SILENT
    || scenario == THROW_PENDING || scenario == THROW_SILENT;
int reports = scenario == NEGATIVE || scenario == ALLOC_SILENT || scenario == OBJECT_SILENT
    || scenario == THROW_PENDING || scenario == THROW_SILENT;
int oom_found = oom_lookup && scenario != LOOKUP_PENDING && scenario != LOOKUP_SILENT;
jthrowable expected = scenario == ENTRY_PENDING || scenario == ALLOC_PENDING
    || scenario == OBJECT_PENDING || scenario == LOOKUP_PENDING || scenario == THROW_PENDING
    ? marker_handle() : scenario == NEGATIVE ? (jthrowable)(void *)&nase
    : scenario == ALLOC_SILENT || scenario == OBJECT_SILENT ? (jthrowable)(void *)&oom : NULL;
check(result == (scenario == SUCCESS ? (jobject)(void *)&arrays[type] : NULL), "result identity/NULL");
check(state.pending == expected, "pending identity");
check(state.exception_checks == (unsigned)(scenario == ENTRY_PENDING || scenario == SUCCESS ? 1 : 2),
    "exact ExceptionCheck count");
for (int i = 0; i < 4; i++) check(state.allocs[i] == (unsigned)(allocations && i == type), "allocator call count");
check(state.allocated == (unsigned)allocations, "unexpected allocation");
check(state.finds[OBJECT_CLASS] == (unsigned)object_lookup, "Object lookup count");
check(state.finds[NASE_CLASS] == (unsigned)(scenario == NEGATIVE), "NASE lookup count");
check(state.finds[OOM_CLASS] == (unsigned)oom_lookup, "OOME lookup count");
check(state.throws == (unsigned)reports, "ThrowNew count");
check(state.deletes[OBJECT_CLASS] == (unsigned)(object_lookup && !object_failed), "Object local release");
check(state.deletes[NASE_CLASS] == (unsigned)(scenario == NEGATIVE), "NASE local release");
check(state.deletes[OOM_CLASS] == (unsigned)oom_found, "OOME local release");
for (int i = 0; i < 3; i++) check(state.live[i] == 0, "class ref leak");
check(state.forbidden == 0, "forbidden JNI operation");
if (reports) {
  check(state.thrown_class == (scenario == NEGATIVE ? NASE_CLASS : OOM_CLASS), "error class");
  check(state.message != NULL && strcmp(state.message, scenario == NEGATIVE
      ? "negative backing array length" : "native backing array allocation failed") == 0,
      "exact exception message");
} else {
  check(state.thrown_class == -1 && state.message == NULL, "unexpected reporting");
}
}
int main(void) {
const jint lengths[] = {0, 1, 257, INT_MAX};
for (int type = 0; type < 4; type++) {
  run_case(type, ENTRY_PENDING, 1);
  run_case(type, ENTRY_PENDING, -1);
  run_case(type, NEGATIVE, -1);
  run_case(type, NEGATIVE, INT_MIN);
  for (unsigned i = 0; i < sizeof(lengths) / sizeof(lengths[0]); i++) run_case(type, SUCCESS, lengths[i]);
  run_case(type, ALLOC_PENDING, 257);
  run_case(type, ALLOC_SILENT, 257);
  run_case(type, LOOKUP_PENDING, 257);
  run_case(type, LOOKUP_SILENT, 257);
  run_case(type, THROW_PENDING, 257);
  run_case(type, THROW_SILENT, 257);
}
run_case(0, OBJECT_PENDING, 257);
run_case(0, OBJECT_SILENT, 257);
check(cases == 58, "case count");
check(checks > 0, "assertion count");
printf("M3_BACKING_ARRAY_MOCK_PASS cases=%u checks=%u\n", cases, checks);
return 0;
}
