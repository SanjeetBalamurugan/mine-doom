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
#include "i_system.h"
#include <setjmp.h>
#include "sounds.h"
#include "s_sound.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

extern jmp_buf doom_error_jmp;
extern int doom_error_armed;
static int doom_dead = 0;

#define GUARD(ret) \
    if (doom_dead) return ret; \
    if (setjmp(doom_error_jmp)) { doom_error_armed = 0; doom_dead = 1; M_hasQuit = true; return ret; } \
    doom_error_armed = 1

extern int myargc;
extern char** myargv;
extern boolean nodrawers;
extern boolean advancedemo;
extern boolean singletics;
boolean M_hasQuit;

extern int maketic;
extern ticcmd_t netcmds[MAXPLAYERS][BACKUPTICS];

static int current_mouse_buttons = 0;

void D_ResetWadFiles(void);
void D_ResetDisplay(void);
void I_ShutdownSound(void);
void I_ShutdownMusic(void);
void I_FreeZone(void);
void M_SaveDefaults(void);

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
    D_ResetWadFiles();
    doom_dead = 0;
    myargc = argc;
    myargv = argv;
    nodrawers = false;
    singletics = true;
    M_hasQuit = false;

    GUARD();
    D_DoomMain();
    doom_error_armed = 0;
}

void DOOM_RUNTICK(void)
{
    GUARD();
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
    doom_error_armed = 0;
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

boolean DOOM_HasQuit()
{
    return M_hasQuit;
}

void DOOM_SHUTDOWN(int save)
{
    printf("DOOM_SHUTDOWN: Shutting down DOOM and resetting all static states\n");

    D_QuitNetGame();
    I_ShutdownSound();
    I_ShutdownMusic();

    if (save)
        M_SaveDefaults();

    W_Shutdown();

    if (screens[0]) {
        free(screens[0]);
    }
    screens[0] = screens[1] = screens[2] = screens[3] = NULL;

    I_FreeZone();

    for (int i = 0; i < NUMSFX; i++) {
        S_sfx[i].lumpnum = S_sfx[i].usefulness = -1;
        S_sfx[i].data = NULL;
    }

    for (int i = 0; i < NUMMUSIC; i++) {
        S_music[i].lumpnum = 0;
        S_music[i].data = NULL;
        S_music[i].handle = 0;
    }
    D_ResetWadFiles();
    D_ResetDisplay();

    current_mouse_buttons = 0;
    gametic = 0;
    maketic = 0;
    advancedemo = false;
    memset(netcmds, 0, sizeof(netcmds));
    memset(players, 0, sizeof(players));
    memset(playeringame, 0, sizeof(playeringame));

    menuactive = false;
    automapactive = false;
    paused = false;
    demoplayback = false;
    netgame = false;

    wipegamestate = GS_DEMOSCREEN;
    gamestate = GS_DEMOSCREEN;
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
