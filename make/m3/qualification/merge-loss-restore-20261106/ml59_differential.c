/* ML59_DIFFERENTIAL v1: master vs restored advance0 under a fake JNIEnv (array regions, exception flag). */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
typedef struct { jsize n; void *data; } FakeArr;
static int pending;
static jsize JNICALL fLen(JNIEnv *e, jarray a) { (void) e; return ((FakeArr *) a)->n; }
static void JNICALL fInt(JNIEnv *e, jintArray a, jsize s, jsize n, jint *b) { (void) e; memcpy(b, (jint *) ((FakeArr *) a)->data + s, (size_t) n * sizeof(jint)); }
static void JNICALL fLong(JNIEnv *e, jlongArray a, jsize s, jsize n, jlong *b) { (void) e; memcpy(b, (jlong *) ((FakeArr *) a)->data + s, (size_t) n * sizeof(jlong)); }
static jboolean JNICALL fExc(JNIEnv *e) { (void) e; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL fFind(JNIEnv *e, const char *n) { (void) e; (void) n; return (jclass) (void *) 1; }
static jint JNICALL fThrow(JNIEnv *e, jclass c, const char *m) { (void) e; (void) c; (void) m; pending = 1; return 0; }
static void JNICALL fDel(JNIEnv *e, jobject o) { (void) e; (void) o; }
jlong JNICALL adv_master(JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean, jlong, jintArray, jlongArray);
jlong JNICALL adv_restored(JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean, jlong, jintArray, jlongArray);
static uint64_t rs = 20261107u;
static uint32_t rnd(uint32_t n) { rs ^= rs << 13; rs ^= rs >> 7; rs ^= rs << 17; return (uint32_t) (rs % n); }
int main(int argc, char **argv) {
    struct JNINativeInterface_ fn; JNIEnv env = &fn; long cases = argc > 1 ? atol(argv[1]) : 2000000L;
    long bad = 0, refused = 0, alive = 0, w1 = 0; uint64_t digest = 11;
    static const char alpha[] = "abc.";
    memset(&fn, 0, sizeof fn);
    fn.GetArrayLength = fLen; fn.GetIntArrayRegion = fInt; fn.GetLongArrayRegion = fLong; fn.ExceptionCheck = fExc;
    fn.FindClass = fFind; fn.ThrowNew = fThrow; fn.DeleteLocalRef = fDel;
    for (long c = 0; c < cases; c++) {
        jint atoms[32]; jlong clo[33]; unsigned char text[2 * 400 + 8]; FakeArr A, C;
        int count = 1 + (int) rnd(rnd(4) == 0 ? 32 : 10), width = rnd(4) == 0 ? 2 : 1, start = (int) rnd(4), len = (int) rnd(rnd(3) == 0 ? 400 : 40);
        uint64_t inc = 0; jlong r1, r2; int p1, p2;
        for (int i = 0; i < count; i++) atoms[i] = (jint) ((uint32_t) alpha[rnd(rnd(5) == 0 ? 4 : 3)] | (rnd(3) == 0 ? 65536u : 0u));
        clo[count] = (jlong) (UINT64_C(1) << count);
        for (int i = count - 1; i >= 0; i--) clo[i] = (jlong) ((UINT64_C(1) << i) | (((uint32_t) atoms[i] & 65536u) ? (uint64_t) clo[i + 1] : 0));
        for (int i = 0; i <= count; i++) if (rnd(3) == 0) inc |= (uint64_t) clo[i];
        if (rnd(10) == 0) inc = (uint64_t) clo[0];
        for (int i = 0; i < (start + len) * width; i++) { uint32_t k = rnd(12); text[i] = k < 7 ? (unsigned char) "abcabca"[k] : k == 7 ? 10 : k == 8 ? 0x85 : k == 9 ? 0xd8 : (unsigned char) rnd(256); }
        if (rnd(50) == 0) inc |= UINT64_C(1) << count | 1; /* sometimes non-closed */
        A.n = count; A.data = atoms; C.n = count + 1; C.data = clo;
        jboolean be = (jboolean) rnd(2);
        pending = 0; r1 = adv_master(&env, NULL, (jlong) (uintptr_t) text, start + len, start, len, width, be, (jlong) inc, (jintArray) &A, (jlongArray) &C); p1 = pending;
        pending = 0; r2 = adv_restored(&env, NULL, (jlong) (uintptr_t) text, start + len, start, len, width, be, (jlong) inc, (jintArray) &A, (jlongArray) &C); p2 = pending;
        if (r1 != r2 || p1 != p2) { if (bad++ < 5) printf("MISMATCH case=%ld count=%d width=%d len=%d master=%lld/%d restored=%lld/%d\n", c, count, width, len, (long long) r1, p1, (long long) r2, p2); }
        if (p1) refused++; else if (r1 > 0) alive++;
        if (width == 1) w1++;
        digest = digest * 31 + (uint64_t) r1 * 7 + (uint64_t) p1;
    }
    printf("ML59_DIFFERENTIAL cases=%ld width1=%ld refused=%ld alive=%ld mismatches=%ld digest=%016llx\n", cases, w1, refused, alive, bad, (unsigned long long) digest);
    return bad != 0;
}
