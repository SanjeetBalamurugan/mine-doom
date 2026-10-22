#include <stddef.h>
#include "doom_lib.h"

#include "d_main.h"
#include "g_game.h"
#include "w_wad.h"
#include "z_zone.h"
#include "v_video.h"
#include "r_local.h"
#include "i_video.h"
#include "m_argv.h"
#include "m_menu.h"
#include "d_net.h"
#include "doomdef.h"
#include "doomstat.h"
#include "d_event.h"

extern int myargc;
extern char** myargv;
extern boolean nodrawers;
extern boolean advancedemo;
extern boolean singletics;

extern int maketic;
extern ticcmd_t netcmds[MAXPLAYERS][BACKUPTICS];

static int current_mouse_buttons = 0;

void D_Display(void);
void D_PostEvent(event_t* ev);
void D_DoAdvanceDemo(void);
void I_StartFrame(void);
void I_StartTic(void);
void D_ProcessEvents(void);
void G_BuildTiccmd(ticcmd_t* cmd);
void TryRunTics(void);

void DOOM_INIT(int argc, char** argv)
{
    myargc = argc;
    myargv = argv;
    nodrawers = false;
    singletics = true;

    D_DoomMain();
}

void DOOM_RUNTICK(void)
{
    I_StartFrame();

    if (singletics)
    {
        I_StartTic();
        D_ProcessEvents();
        G_BuildTiccmd(&netcmds[consoleplayer][maketic % BACKUPTICS]);
        if (advancedemo)
            D_DoAdvanceDemo();
        M_Ticker();
        G_Ticker();
        gametic++;
        maketic++;
    }
    else
    {
        TryRunTics();
    }
}

boolean DOOM_IsPlayerReady(void)
{
    if (consoleplayer < 0 || consoleplayer >= MAXPLAYERS) return false;
    return players[consoleplayer].mo != NULL;
}

int DOOM_GetScreenWidth(void)
{
    return SCREENWIDTH;
}

int DOOM_GetScreenHeight(void)
{
    return SCREENHEIGHT;
}

byte* DOOM_GetScreenBuffer(void)
{
    return screens[0];
}

void DOOM_RENDERFRAME(void)
{
    if (gametic <= 0 && !advancedemo) return;
    if (advancedemo)
        D_DoAdvanceDemo();
    D_Display();
}

byte* DOOM_GetPalette(void)
{
    return (byte*)W_CacheLumpName("PLAYPAL", PU_STATIC);
}

int DOOM_GetGameTic(void)
{
    return gametic;
}

int DOOM_GetLevelTime(void)
{
    return leveltime;
}

void DOOM_KeyDown(int key)
{
    event_t event;
    event.type = ev_keydown;
    event.data1 = key;
    event.data2 = 0;
    event.data3 = 0;
    D_PostEvent(&event);
}

void DOOM_KeyUp(int key)
{
    event_t event;
    event.type = ev_keyup;
    event.data1 = key;
    event.data2 = 0;
    event.data3 = 0;
    D_PostEvent(&event);
}

void DOOM_MouseMove(int x, int y)
{
    event_t event;
    event.type = ev_mouse;
    event.data1 = current_mouse_buttons;
    event.data2 = x << 2;
    event.data3 = y << 2;
    D_PostEvent(&event);
}

void DOOM_MouseButton(int button, boolean pressed)
{
    event_t event;
    if (button >= 0 && button < 3) {
        if (pressed)
            current_mouse_buttons |= (1 << button);
        else
            current_mouse_buttons &= ~(1 << button);
    }
    event.type = ev_mouse;
    event.data1 = current_mouse_buttons;
    event.data2 = 0;
    event.data3 = 0;
    D_PostEvent(&event);
}

void DOOM_SendKeyPress(int key)
{
    DOOM_KeyDown(key);
}
