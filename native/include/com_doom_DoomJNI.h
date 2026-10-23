#include <jni.h>

#ifndef _Included_com_doom_DoomJNI
#define _Included_com_doom_DoomJNI
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_doom_DoomJNI_init(JNIEnv *, jclass, jobjectArray);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_runTick(JNIEnv *, jclass);
JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_isPlayerReady(JNIEnv *, jclass);
JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getGameTic(JNIEnv *, jclass);
JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getLevelTime(JNIEnv *, jclass);
JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenWidth(JNIEnv *, jclass);
JNIEXPORT jint JNICALL Java_com_doom_DoomJNI_getScreenHeight(JNIEnv *, jclass);
JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getScreenBuffer(JNIEnv *, jclass);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_renderFrame(JNIEnv *, jclass);
JNIEXPORT jbyteArray JNICALL Java_com_doom_DoomJNI_getPalette(JNIEnv *, jclass);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyDown(JNIEnv *, jclass, jint);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_keyUp(JNIEnv *, jclass, jint);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseMove(JNIEnv *, jclass, jint, jint);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_mouseButton(JNIEnv *, jclass, jint, jboolean);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_sendKeyPress(JNIEnv *, jclass, jint);
JNIEXPORT jshortArray JNICALL Java_com_doom_DoomJNI_drainSound(JNIEnv *, jclass);
JNIEXPORT jboolean JNICALL Java_com_doom_DoomJNI_hasQuit(JNIEnv *, jclass);
JNIEXPORT void JNICALL Java_com_doom_DoomJNI_shutdown(JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
