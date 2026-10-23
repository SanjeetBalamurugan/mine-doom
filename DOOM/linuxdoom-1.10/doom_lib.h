#ifndef DOOM_LIB
#define DOOM_LIB

#include "doomtype.h"
#include "doomdef.h"

void DOOM_INIT(int argc, char** argv);
void DOOM_RUNTICK(void);
boolean DOOM_IsPlayerReady(void);
int DOOM_GetGameTic(void);
int DOOM_GetLevelTime(void);
boolean DOOM_HasQuit();
void DOOM_SHUTDOWN();

int DOOM_GetScreenWidth(void);
int DOOM_GetScreenHeight(void);
byte* DOOM_GetScreenBuffer(void);
void DOOM_RENDERFRAME(void);
byte* DOOM_GetPalette(void);

void DOOM_KeyDown(int key);
void DOOM_KeyUp(int key);
void DOOM_MouseMove(int x, int y);
void DOOM_MouseButton(int button, boolean pressed);
void DOOM_SendKeyPress(int key);

#define DOOM_KEY_RIGHTARROW  KEY_RIGHTARROW
#define DOOM_KEY_LEFTARROW   KEY_LEFTARROW
#define DOOM_KEY_UPARROW     KEY_UPARROW
#define DOOM_KEY_DOWNARROW   KEY_DOWNARROW
#define DOOM_KEY_ESCAPE      KEY_ESCAPE
#define DOOM_KEY_ENTER       KEY_ENTER
#define DOOM_KEY_TAB         KEY_TAB
#define DOOM_KEY_F1          KEY_F1
#define DOOM_KEY_F2          KEY_F2
#define DOOM_KEY_F3          KEY_F3
#define DOOM_KEY_F4          KEY_F4
#define DOOM_KEY_F5          KEY_F5
#define DOOM_KEY_F6          KEY_F6
#define DOOM_KEY_F7          KEY_F7
#define DOOM_KEY_F8          KEY_F8
#define DOOM_KEY_F9          KEY_F9
#define DOOM_KEY_F10         KEY_F10
#define DOOM_KEY_F11         KEY_F11
#define DOOM_KEY_F12         KEY_F12
#define DOOM_KEY_BACKSPACE   KEY_BACKSPACE
#define DOOM_KEY_PAUSE       KEY_PAUSE
#define DOOM_KEY_EQUALS      KEY_EQUALS
#define DOOM_KEY_MINUS       KEY_MINUS
#define DOOM_KEY_RSHIFT      KEY_RSHIFT
#define DOOM_KEY_RCTRL       KEY_RCTRL
#define DOOM_KEY_RALT        KEY_RALT
#define DOOM_KEY_LALT        KEY_RALT

#define DOOM_KEY_SPACE       ' '
#define DOOM_KEY_w           'w'
#define DOOM_KEY_a           'a'
#define DOOM_KEY_s           's'
#define DOOM_KEY_d           'd'
#define DOOM_KEY_e           'e'

#define DOOM_MOUSE_LEFT      1
#define DOOM_MOUSE_MIDDLE    2
#define DOOM_MOUSE_RIGHT     4

#endif
