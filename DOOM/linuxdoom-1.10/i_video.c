// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id:$
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
// $Log:$
//
// DESCRIPTION:
//	DOOM graphics stuff for X11, UNIX.
//
//-----------------------------------------------------------------------------

static const char
rcsid[] = "$Id: i_x.c,v 1.6 1997/02/03 22:45:10 b1 Exp $";

#include <stdlib.h>
#include <unistd.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#include <errno.h>

#include <stdarg.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <netinet/in.h>
#include <errno.h>
#include <signal.h>

#include "doomstat.h"
#include "i_system.h"
#include "v_video.h"
#include "m_argv.h"
#include "d_main.h"

#include "doomdef.h"



//
//  Translates the key currently in X_event
//

// int xlatekey(void)
// {

//     int rc;

//     switch(rc = XKeycodeToKeysym(X_display, X_event.xkey.keycode, 0))
//     {
//       case XK_Left:	rc = KEY_LEFTARROW;	break;
//       case XK_Right:	rc = KEY_RIGHTARROW;	break;
//       case XK_Down:	rc = KEY_DOWNARROW;	break;
//       case XK_Up:	rc = KEY_UPARROW;	break;
//       case XK_Escape:	rc = KEY_ESCAPE;	break;
//       case XK_Return:	rc = KEY_ENTER;		break;
//       case XK_Tab:	rc = KEY_TAB;		break;
//       case XK_F1:	rc = KEY_F1;		break;
//       case XK_F2:	rc = KEY_F2;		break;
//       case XK_F3:	rc = KEY_F3;		break;
//       case XK_F4:	rc = KEY_F4;		break;
//       case XK_F5:	rc = KEY_F5;		break;
//       case XK_F6:	rc = KEY_F6;		break;
//       case XK_F7:	rc = KEY_F7;		break;
//       case XK_F8:	rc = KEY_F8;		break;
//       case XK_F9:	rc = KEY_F9;		break;
//       case XK_F10:	rc = KEY_F10;		break;
//       case XK_F11:	rc = KEY_F11;		break;
//       case XK_F12:	rc = KEY_F12;		break;
	
//       case XK_BackSpace:
//       case XK_Delete:	rc = KEY_BACKSPACE;	break;

//       case XK_Pause:	rc = KEY_PAUSE;		break;

//       case XK_KP_Equal:
//       case XK_equal:	rc = KEY_EQUALS;	break;

//       case XK_KP_Subtract:
//       case XK_minus:	rc = KEY_MINUS;		break;

//       case XK_Shift_L:
//       case XK_Shift_R:
// 	rc = KEY_RSHIFT;
// 	break;
	
//       case XK_Control_L:
//       case XK_Control_R:
// 	rc = KEY_RCTRL;
// 	break;
	
//       case XK_Alt_L:
//       case XK_Meta_L:
//       case XK_Alt_R:
//       case XK_Meta_R:
// 	rc = KEY_RALT;
// 	break;
	
//       default:
// 	if (rc >= XK_space && rc <= XK_asciitilde)
// 	    rc = rc - XK_space + ' ';
// 	if (rc >= 'A' && rc <= 'Z')
// 	    rc = rc - 'A' + 'a';
// 	break;
//     }

//     return rc;

// }

void I_ShutdownGraphics(void)
{
  // Paranoia.
	//   image->data = NULL;
}



//
// I_StartFrame
//
void I_StartFrame (void)
{
    // er?

}

static int	lastmousex = 0;
static int	lastmousey = 0;
boolean		mousemoved = false;
boolean		shmFinished;

void I_GetEvent(void)
{
}

//
// I_StartTic
//
void I_StartTic (void)
{

}


//
// I_UpdateNoBlit
//
void I_UpdateNoBlit (void)
{
    // what is this?
}

//
// I_FinishUpdate
//
void I_FinishUpdate (void)
{

    static int	lasttic;
    int		tics;
    int		i;
    // UNUSED static unsigned char *bigscreen=0;

    // draws little dots on the bottom of the screen
    if (devparm)
    {

	i = I_GetTime();
	tics = i - lasttic;
	lasttic = i;
	if (tics > 20) tics = 20;

	for (i=0 ; i<tics*2 ; i+=2)
	    screens[0][ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0xff;
	for ( ; i<20*2 ; i+=2)
	    screens[0][ (SCREENHEIGHT-1)*SCREENWIDTH + i] = 0x0;
    
    }

}


//
// I_ReadScreen
//
void I_ReadScreen (byte* scr)
{
    memcpy (scr, screens[0], SCREENWIDTH*SCREENHEIGHT);
}


//
// Palette stuff.
//
// static XColor	colors[256];

// void UploadNewPalette(Colormap cmap, byte *palette)
// {

//     register int	i;
//     register int	c;
//     static boolean	firstcall = true;

// #ifdef __cplusplus
//     if (X_visualinfo.c_class == PseudoColor && X_visualinfo.depth == 8)
// #else
//     if (X_visualinfo.class == PseudoColor && X_visualinfo.depth == 8)
// #endif
// 	{
// 	    // initialize the colormap
// 	    if (firstcall)
// 	    {
// 		firstcall = false;
// 		for (i=0 ; i<256 ; i++)
// 		{
// 		    colors[i].pixel = i;
// 		    colors[i].flags = DoRed|DoGreen|DoBlue;
// 		}
// 	    }

// 	    // set the X colormap entries
// 	    for (i=0 ; i<256 ; i++)
// 	    {
// 		c = gammatable[usegamma][*palette++];
// 		colors[i].red = (c<<8) + c;
// 		c = gammatable[usegamma][*palette++];
// 		colors[i].green = (c<<8) + c;
// 		c = gammatable[usegamma][*palette++];
// 		colors[i].blue = (c<<8) + c;
// 	    }

// 	    // store the colors to the current colormap
// 	    XStoreColors(X_display, cmap, colors, 256);

// 	}
// }

//
// I_SetPalette
//
void I_SetPalette (byte* palette)
{
    
}

void I_InitGraphics(void)
{
	int i;
	const int MAX_MULTIPLIERS = 4;

	for (i = 0; i < MAX_MULTIPLIERS; i++)
	{
		if (screens[i] == NULL)
		{
			screens[i] = (unsigned char *) malloc (SCREENWIDTH * SCREENHEIGHT);
			if (screens[i] == NULL)
            {
                fprintf(stderr, "Failed to allocate screen buffer %d\n", i);
                exit(1);
            }
		}
		
	}
	
}
