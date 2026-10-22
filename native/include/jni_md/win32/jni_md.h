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

typedef uint8_t  jboolean; /* unsigned 8 bits */
typedef int8_t   jbyte;    /* signed 8 bits */
typedef uint16_t jchar;    /* unsigned 16 bits */
typedef int16_t  jshort;   /* signed 16 bits */
typedef int32_t  jint;     /* signed 32 bits */
typedef int64_t  jlong;    /* signed 64 bits */
typedef float    jfloat;   /* 32-bit IEEE 754 */
typedef double   jdouble;  /* 64-bit IEEE 754 */

typedef jint     jsize;

#endif
