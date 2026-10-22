#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <setjmp.h>

#include "doomdef.h"
#include "doom_lib.h"
#include "doom_sounds.h"
// #include "com_doom_DoomJNI.h"

#ifdef _WIN32
#include <direct.h>
#define CHDIR _chdir
#else
#include <unistd.h>
#define CHDIR chdir
#endif

static jmp_buf exit_jmp;
static int doom_should_exit = 0;
static int doom_initialized = 0;

void exit(int status) {
    doom_should_exit = 1;
    longjmp(exit_jmp, 1);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_init(JNIEnv* env, jclass cls, jobjectArray args)
{
    if (!args || doom_initialized) return;
    jsize n = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = (char**)calloc((size_t)n + 2, sizeof(char*));
    if (!argv) return;

    jsize i;
    #ifdef _WIN32
    _set_fmode(_O_BINARY);
    #endif

    argv[0] = strdup("doom");
    for (i = 0; i < n; i++)
    {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        if (s) {
            const char* c = (*env)->GetStringUTFChars(env, s, NULL);
            argv[i + 1] = strdup(c ? c : "");
            (*env)->ReleaseStringUTFChars(env, s, c);
            (*env)->DeleteLocalRef(env, s);
        } else {
            argv[i + 1] = strdup("");
        }
    }
    argv[n + 1] = NULL;

    printf("[Native JNI] Passing %d total arguments to engine:\n", n + 1);
    for (int j = 0; j < (n + 1); j++) {
        printf("  argv[%d]: %s\n", j, argv[j]);
    }
    fflush(stdout);

    doom_should_exit = 0;
    if (setjmp(exit_jmp) == 0) {
        DOOM_INIT((int)n + 1, argv);
        doom_initialized = 1;
    } else {
        doom_initialized = 0;
        doom_should_exit = 1;
    }

    for (int j = 0; j < (n + 1); j++) {
        free(argv[j]);
    }
    free(argv);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_runTick(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return;

    if (setjmp(exit_jmp) == 0) {
        int before, after;
        I_GetSoundBuffer(&before);
        DOOM_RUNTICK();
        I_GetSoundBuffer(&after);
        if (after == before)
        {
            I_UpdateSound();
            I_SubmitSound();
        }
    } else {
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_isPlayerReady(JNIEnv* env, jclass cls)
{
    return (doom_initialized && DOOM_IsPlayerReady() && !doom_should_exit) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getGameTic(JNIEnv* env, jclass cls)
{
    return DOOM_GetGameTic();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getLevelTime(JNIEnv* env, jclass cls)
{
    return DOOM_GetLevelTime();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenWidth(JNIEnv* env, jclass cls)
{
    return DOOM_GetScreenWidth();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenHeight(JNIEnv* env, jclass cls)
{
    return DOOM_GetScreenHeight();
}

JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getScreenBuffer(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return NULL;
    byte* p = DOOM_GetScreenBuffer();
    jsize len = (jsize)DOOM_GetScreenWidth() * (jsize)DOOM_GetScreenHeight();
    jbyteArray a;

    if (!p || len <= 0) return NULL;
    a = (*env)->NewByteArray(env, len);
    if (!a) return NULL;
    (*env)->SetByteArrayRegion(env, a, 0, len, (const jbyte*)p);
    return a;
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_renderFrame(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return;
    if (setjmp(exit_jmp) == 0) {
        DOOM_RENDERFRAME();
    } else {
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getPalette(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return NULL;
    byte* p = DOOM_GetPalette();
    jbyteArray a;

    if (!p) return NULL;
    a = (*env)->NewByteArray(env, 768);
    if (!a) return NULL;
    (*env)->SetByteArrayRegion(env, a, 0, 768, (const jbyte*)p);
    return a;
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyDown(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    if (setjmp(exit_jmp) == 0) {
        DOOM_KeyDown((int)key);
    } else {
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyUp(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    if (setjmp(exit_jmp) == 0) {
        DOOM_KeyUp((int)key);
    } else {
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseMove(JNIEnv* env, jclass cls, jint x, jint y)
{
    if (!doom_initialized || doom_should_exit) return;
    DOOM_MouseMove((int)x, (int)y);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseButton(JNIEnv* env, jclass cls, jint button, jboolean pressed)
{
    if (!doom_initialized || doom_should_exit) return;
    DOOM_MouseButton((int)button, pressed == JNI_TRUE ? true : false);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_sendKeyPress(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    DOOM_SendKeyPress((int)key);
}

JNIEXPORT jshortArray JNICALL Java_com_doom_DoomJNI_drainSound(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return NULL;
    int n = 0;
    signed short* p = I_GetSoundBuffer(&n);
    jshortArray a = (*env)->NewShortArray(env, n);

    if (!a) return NULL;
    if (p && n > 0) (*env)->SetShortArrayRegion(env, a, 0, n, (const jshort*)p);
    I_ClearSoundBuffer();
    return a;
}
