#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "doomdef.h"
#include "doom_lib.h"
#include "doom_sounds.h"
#include "com_doom_DoomJNI.h"

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_init(JNIEnv* env, jclass cls, jobjectArray args)
{
    jsize n = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = (char**)calloc((size_t)n + 2, sizeof(char*));
    jsize i;

    argv[0] = strdup("doom");
    for (i = 0; i < n; i++)
    {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char* c = s ? (*env)->GetStringUTFChars(env, s, NULL) : NULL;
        argv[i + 1] = strdup(c ? c : "");
        if (c) (*env)->ReleaseStringUTFChars(env, s, c);
        if (s) (*env)->DeleteLocalRef(env, s);
    }
    DOOM_INIT((int)n + 1, argv);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_runTick(JNIEnv* env, jclass cls)
{
    int before, after;

    I_GetSoundBuffer(&before);
    DOOM_RUNTICK();
    I_GetSoundBuffer(&after);
    if (after == before)
    {
        I_UpdateSound();
        I_SubmitSound();
    }
}

JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_isPlayerReady(JNIEnv* env, jclass cls)
{
    return DOOM_IsPlayerReady() ? JNI_TRUE : JNI_FALSE;
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
    DOOM_RENDERFRAME();
}

JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getPalette(JNIEnv* env, jclass cls)
{
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
    DOOM_KeyDown((int)key);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyUp(JNIEnv* env, jclass cls, jint key)
{
    DOOM_KeyUp((int)key);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseMove(JNIEnv* env, jclass cls, jint x, jint y)
{
    DOOM_MouseMove((int)x, (int)y);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseButton(JNIEnv* env, jclass cls, jint button, jboolean pressed)
{
    DOOM_MouseButton((int)button, pressed == JNI_TRUE ? true : false);
}

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_sendKeyPress(JNIEnv* env, jclass cls, jint key)
{
    DOOM_SendKeyPress((int)key);
}

JNIEXPORT jshortArray JNICALL Java_com_doom_DoomJNI_drainSound(JNIEnv* env, jclass cls)
{
    int n = 0;
    signed short* p = I_GetSoundBuffer(&n);
    jshortArray a = (*env)->NewShortArray(env, n);

    if (!a) return NULL;
    if (p && n > 0) (*env)->SetShortArrayRegion(env, a, 0, n, (const jshort*)p);
    I_ClearSoundBuffer();
    return a;
}
