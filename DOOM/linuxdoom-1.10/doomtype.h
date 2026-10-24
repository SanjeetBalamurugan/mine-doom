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
// DESCRIPTION:
//	Simple basic typedefs, isolated here to make it easier
//	 separating modules.
//
//-----------------------------------------------------------------------------


#ifndef __DOOMTYPE__
#define __DOOMTYPE__


#ifndef __BYTEBOOL__
#define __BYTEBOOL__

#ifdef __cplusplus
typedef bool boolean;
#elif defined(_WIN32)
#ifdef boolean
#undef boolean
#endif
#define boolean int

#ifndef true
#define true 1
#endif
#ifndef false
#define false 0
#endif
#else
typedef enum {false, true} boolean;
#endif

typedef unsigned char byte;
#endif

typedef char boolean_size_check[sizeof(boolean) == sizeof(int) ? 1 : -1];


// Predefined with some OS.
#include <limits.h>

#ifndef MAXCHAR
#define MAXCHAR  CHAR_MAX
#endif

#ifndef MAXSHORT
#define MAXSHORT SHRT_MAX
#endif

#ifndef MAXINT
#define MAXINT   INT_MAX
#endif

#ifndef MAXLONG
#define MAXLONG  LONG_MAX
#endif

#ifndef MINCHAR
#define MINCHAR  CHAR_MIN
#endif

#ifndef MINSHORT
#define MINSHORT SHRT_MIN
#endif

#ifndef MININT
#define MININT   INT_MIN
#endif

#ifndef MINLONG
#define MINLONG  LONG_MIN
#endif




#endif
//-----------------------------------------------------------------------------
//
// $Log:$
//
//-----------------------------------------------------------------------------
