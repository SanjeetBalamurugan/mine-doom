#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <setjmp.h>

#include "doomdef.h"
#include "doom_lib.h"
#include "doom_sounds.h"
#include "com_doom_DoomJNI.h"

#ifdef _WIN32
#include <direct.h>
#define CHDIR _chdir
#else
#include <unistd.h>
#define CHDIR chdir
#endif

static jmp_buf exit_jmp;
static int exit_jmp_active = 0;
static int doom_should_exit = 0;
static int doom_initialized = 0;
static char** saved_argv = NULL;
static int saved_argc = 0;

static void free_saved_argv(void)
{
    if (!saved_argv) return;
    for (int i = 0; i < saved_argc; i++) {
        free(saved_argv[i]);
    }
    free(saved_argv);
    saved_argv = NULL;
    saved_argc = 0;
}

void exit(int status)
{
    doom_should_exit = 1;
    doom_initialized = 0;
    if (exit_jmp_active) {
        exit_jmp_active = 0;
        longjmp(exit_jmp, 1);
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_init(JNIEnv* env, jclass cls, jobjectArray args)
{
    if (!env || !args || doom_initialized) return;

    free_saved_argv();

    jsize n = (*env)->GetArrayLength(env, args);
    if (n < 0) n = 0;

    char** argv = (char**)calloc((size_t)n + 2, sizeof(char*));
    if (!argv) return;

    argv[0] = strdup("doom");
    for (jsize i = 0; i < n; i++) {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        if (s) {
            const char* c = (*env)->GetStringUTFChars(env, s, NULL);
            argv[i + 1] = strdup(c ? c : "");
            if (c) (*env)->ReleaseStringUTFChars(env, s, c);
            (*env)->DeleteLocalRef(env, s);
        } else {
            argv[i + 1] = strdup("");
        }
    }
    argv[n + 1] = NULL;

    saved_argv = argv;
    saved_argc = (int)n + 1;

    printf("[Native JNI] Passing %d total arguments to engine:\n", saved_argc);
    for (int j = 0; j < saved_argc; j++) {
        printf("  argv[%d]: %s\n", j, argv[j] ? argv[j] : "");
    }
    fflush(stdout);

    doom_should_exit = 0;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_INIT(saved_argc, argv);
        DOOM_RUNTICK();
        exit_jmp_active = 0;
        doom_initialized = 1;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_runTick(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        int before = 0;
        int after = 0;
        I_GetSoundBuffer(&before);
        DOOM_RUNTICK();
        I_GetSoundBuffer(&after);
        if (after == before) {
            I_UpdateSound();
            I_SubmitSound();
        }
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_isPlayerReady(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return JNI_FALSE;
    jboolean ready = JNI_FALSE;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        ready = DOOM_IsPlayerReady() ? JNI_TRUE : JNI_FALSE;
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
    return ready;
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getGameTic(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return 0;
    return (jint)DOOM_GetGameTic();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getLevelTime(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return 0;
    return (jint)DOOM_GetLevelTime();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenWidth(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return 320;
    return (jint)DOOM_GetScreenWidth();
}

JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenHeight(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit) return 200;
    return (jint)DOOM_GetScreenHeight();
}

JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getScreenBuffer(JNIEnv* env, jclass cls)
{
    if (!env || !doom_initialized || doom_should_exit) return NULL;

    byte* p = NULL;
    jsize len = 0;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        p = DOOM_GetScreenBuffer();
        len = (jsize)DOOM_GetScreenWidth() * (jsize)DOOM_GetScreenHeight();
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
        return NULL;
    }

    if (!p || len <= 0) return NULL;

    jbyteArray a = (*env)->NewByteArray(env, len);
    if (!a) return NULL;
    (*env)->SetByteArrayRegion(env, a, 0, len, (const jbyte*)p);
    return a;
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_renderFrame(JNIEnv* env, jclass cls)
{
    if (!doom_initialized || doom_should_exit || DOOM_GetGameTic() <= 0) return;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_RENDERFRAME();
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getPalette(JNIEnv* env, jclass cls)
{
    if (!env || !doom_initialized || doom_should_exit) return NULL;

    byte* p = NULL;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        p = DOOM_GetPalette();
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
        return NULL;
    }

    if (!p) return NULL;

    jbyteArray a = (*env)->NewByteArray(env, 768);
    if (!a) return NULL;
    (*env)->SetByteArrayRegion(env, a, 0, 768, (const jbyte*)p);
    return a;
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyDown(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_KeyDown((int)key);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyUp(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_KeyUp((int)key);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseMove(JNIEnv* env, jclass cls, jint x, jint y)
{
    if (!doom_initialized || doom_should_exit) return;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_MouseMove((int)x, (int)y);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseButton(JNIEnv* env, jclass cls, jint button, jboolean pressed)
{
    if (!doom_initialized || doom_should_exit) return;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_MouseButton((int)button, pressed == JNI_TRUE ? 1 : 0);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_sendKeyPress(JNIEnv* env, jclass cls, jint key)
{
    if (!doom_initialized || doom_should_exit) return;
    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        DOOM_SendKeyPress((int)key);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
    }
}

JNIEXPORT jshortArray JNICALL Java_com_doom_DoomJNI_drainSound(JNIEnv* env, jclass cls)
{
    if (!env || !doom_initialized || doom_should_exit) return NULL;

    int n = 0;
    signed short* p = NULL;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0) {
        p = I_GetSoundBuffer(&n);
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
        return NULL;
    }

    if (!p || n <= 0) {
        I_ClearSoundBuffer();
        return NULL;
    }

    jshortArray a = (*env)->NewShortArray(env, (jsize)n);
    if (!a) return NULL;
    (*env)->SetShortArrayRegion(env, a, 0, (jsize)n, (const jshort*)p);
    I_ClearSoundBuffer();
    return a;
}

JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_hasQuit(JNIEnv* env, jclass cls)
{
    if (!env || !doom_initialized || doom_should_exit) return JNI_FALSE;
    jboolean q = 0;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0)
    {
        q = (jboolean)DOOM_HasQuit();
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
        return JNI_FALSE;
    }

    doom_should_exit = q;
    return q;

}
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_shutdown(JNIEnv* env, jclass cls)
{
    if (!env || !doom_initialized || doom_should_exit) return;

    exit_jmp_active = 1;
    if (setjmp(exit_jmp) == 0)
    {
        doom_should_exit = 1;
        DOOM_SHUTDOWN();
        exit_jmp_active = 0;
    } else {
        exit_jmp_active = 0;
        doom_initialized = 0;
        doom_should_exit = 1;
        return;
    }
}
