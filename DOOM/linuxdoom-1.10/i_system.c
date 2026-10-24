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
//
//-----------------------------------------------------------------------------

static const char
rcsid[] = "$Id: m_bbox.c,v 1.1 1997/02/03 22:45:10 b1 Exp $";


#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <stdarg.h>
#include <sys/time.h>
#include <unistd.h>

#include "doomdef.h"
#include "m_misc.h"
#include "i_video.h"
#include "i_sound.h"

#include "d_net.h"
#include "g_game.h"

#ifdef __GNUG__
#pragma implementation "i_system.h"
#endif
#include "i_system.h"




int	mb_used = 32;


void
I_Tactile
( int	on,
  int	off,
  int	total )
{
  // UNUSED.
  on = off = total = 0;
}

ticcmd_t	emptycmd;
ticcmd_t*	I_BaseTiccmd(void)
{
    return &emptycmd;
}


int  I_GetHeapSize (void)
{
    return mb_used*1024*1024;
}

static byte* zonebase = NULL;
byte* I_ZoneBase (int*	size)
{
    *size = mb_used*1024*1024;
    zonebase = (byte *) malloc (*size);
    return zonebase;
}

void I_FreeZone (void)
{
    free(zonebase);
    zonebase = NULL;
}



//
// I_GetTime
// returns time in 1/70th second tics
//
int  I_GetTime (void)
{
    struct timeval	tp;
    struct timezone	tzp;
    int			newtics;
    static int		basetime=0;

    gettimeofday(&tp, &tzp);
    if (!basetime)
	basetime = tp.tv_sec;
    newtics = (tp.tv_sec-basetime)*TICRATE + tp.tv_usec*TICRATE/1000000;
    return newtics;
}



//
// I_Init
//
void I_Init (void)
{
    I_InitSound();
    //  I_InitGraphics();
}

//
// I_Quit
//
void I_Quit (void)
{
    M_hasQuit = true; // nga this is the worse bug i have written
}

void I_WaitVBL(int count)
{
#ifdef SGI
    sginap(1);
#else
#ifdef SUN
    sleep(0);
#else
    usleep (count * (1000000/70) );
#endif
#endif
}

void I_BeginRead(void)
{
}

void I_EndRead(void)
{
}

byte*	I_AllocLow(int length)
{
    byte*	mem;

    mem = (byte *)malloc (length);
    memset (mem,0,length);
    return mem;
}


//
// I_Error
//
extern boolean demorecording;
#include <setjmp.h>

jmp_buf doom_error_jmp;
int doom_error_armed = 0;

void I_Error (char *error, ...)
{
    va_list	argptr;

    // Message first.
    va_start (argptr,error);
    fprintf (stderr, "Error: ");
    vfprintf (stderr,error,argptr);
    fprintf (stderr, "\n");
    va_end (argptr);

    fflush( stderr );

    if (doom_error_armed)
	longjmp(doom_error_jmp, 1);

    // Shutdown. Here might be other errors.
    if (demorecording)
	G_CheckDemoStatus();

    D_QuitNetGame ();
    I_ShutdownGraphics();

    exit(-1);
}
