#ifndef DOOM_LIB
#define DOOM_LIB

#include "doomtype.h"

void DOOM_INIT(int argc, char** argv);
void DOOM_RUNTICK(void);
boolean DOOM_IsPlayerReady(void);

int DOOM_GetScreenWidth(void);
int DOOM_GetScreenHeight(void);
byte* DOOM_GetScreenBuffer(void);
void DOOM_RENDERFRAME(void);
byte* DOOM_GetPalette(void);

#endif