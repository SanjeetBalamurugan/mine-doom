#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#include <stdint.h> // Enforce cross-platform exact width bit-mask primitives

#if defined(_WIN32) || defined(__CYGWIN__)
    #define JNIEXPORT __declspec(dllexport)
    #define JNIIMPORT __declspec(dllimport)
    #define JNICALL __stdcall
#else
    #define JNIEXPORT __attribute__((visibility("default")))
    #define JNIIMPORT
    #define JNICALL
#endif

typedef int32_t jint;
typedef int64_t jlong;
typedef int8_t  jbyte;

#endif
