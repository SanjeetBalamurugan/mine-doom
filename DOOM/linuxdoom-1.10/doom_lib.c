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

extern int maketic;
extern ticcmd_t netcmds[MAXPLAYERS][BACKUPTICS];

void D_Display(void);
void D_PostEvent(event_t* ev);
void I_StartTic(void);
void D_ProcessEvents(void);
void G_BuildTiccmd(ticcmd_t* cmd);

void DOOM_INIT(int argc, char** argv)
{
    myargc = argc;
    myargv = argv;
    nodrawers = false;

    D_DoomMain();
}

// process one or more tics
void DOOM_RUNTICK(void)
{
    if (singletics)
	{
	    I_StartTic ();
	    D_ProcessEvents ();
	    G_BuildTiccmd (&netcmds[consoleplayer][maketic%BACKUPTICS]);
	    if (advancedemo)
		    D_DoAdvanceDemo ();
	    M_Ticker ();
	    G_Ticker ();
	    gametic++;
	    maketic++;
	}
	else
	{
	    TryRunTics (); // will run at least one tic
	}
}

boolean DOOM_IsPlayerReady(void)
{
    return players[consoleplayer].mo != NULL;
}

int DOOM_GetScreenWidth(void) {
    return SCREENWIDTH;
}

int DOOM_GetScreenHeight(void) {
    return SCREENHEIGHT;
}

byte* DOOM_GetScreenBuffer(void)
{
    return screens[0]; // screens[0] is the main screen
}

void DOOM_RENDERFRAME(void)
{
    D_Display();
}

byte* DOOM_GetPalette(void)
{
    return (byte*)W_CacheLumpName("PLAYPAL", PU_CACHE);
}
