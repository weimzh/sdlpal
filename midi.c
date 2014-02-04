//
// Copyright (c) 2011, Wei Mingzhi <whistler_wmz@users.sf.net>.
// All rights reserved.
//
// This file is part of SDLPAL.
//
// SDLPAL is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

#include "main.h"

static INT iMidCurrent = -1;
static BOOL fMidLoop = FALSE;

#ifdef TIMIDITY
#include "timidity/timidity.h"

static MidiSong *g_pMid = NULL;
#else
static NativeMidiSong *g_pMid = NULL;
#endif

VOID
MIDI_Play(
   INT       iNumRIX,
   BOOL      fLoop
)
/*++
  Purpose:

    Start playing the specified music in MIDI format.

  Parameters:

    [IN]  iNumRIX - number of the music. 0 to stop playing current music.

    [IN]  fLoop - Whether the music should be looped or not.

  Return value:

    None.

--*/
{
   FILE            *fp;
   unsigned char   *buf;
   int              size;
   SDL_RWops       *rw;

#ifdef TIMIDITY
   if (g_pMid != NULL && iNumRIX == iMidCurrent && Timidity_Active())
#else
   if (g_pMid != NULL && iNumRIX == iMidCurrent && native_midi_active())
#endif
   {
      return;
   }

   SOUND_PlayCDA(-1);
#ifdef TIMIDITY
   Timidity_FreeSong(g_pMid);
#else
   native_midi_freesong(g_pMid);
#endif
   g_pMid = NULL;
   iMidCurrent = -1;

   if (g_fNoMusic || iNumRIX <= 0)
   {
      return;
   }

   fp = fopen(PAL_PREFIX "midi.mkf", "rb");
   if (fp == NULL)
   {
      return;
   }

   if (iNumRIX > PAL_MKFGetChunkCount(fp))
   {
      fclose(fp);
      return;
   }

   size = PAL_MKFGetChunkSize(iNumRIX, fp);
   if (size <= 0)
   {
      fclose(fp);
      return;
   }

   buf = (unsigned char *)UTIL_malloc(size);

   PAL_MKFReadChunk((LPBYTE)buf, size, iNumRIX, fp);
   fclose(fp);

   rw = SDL_RWFromConstMem((const void *)buf, size);

#ifdef TIMIDITY
   g_pMid = Timidity_LoadSong_RW(rw);
#else
   g_pMid = native_midi_loadsong_RW(rw);
#endif
   if (g_pMid != NULL)
   {
#ifdef TIMIDITY
      Timidity_Start(g_pMid);
#else
      native_midi_start(g_pMid);
#endif

      iMidCurrent = iNumRIX;
      fMidLoop = fLoop;
   }

   SDL_RWclose(rw);
   free(buf);
}

VOID
MIDI_CheckLoop(
   VOID
)
{
#ifdef TIMIDITY
   if (fMidLoop && g_pMid != NULL && !Timidity_Active())
#else
   if (fMidLoop && g_pMid != NULL && !native_midi_active())
#endif
   {
      MIDI_Play(iMidCurrent, TRUE);
   }
}
