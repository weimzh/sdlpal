/*
    SDL - Simple DirectMedia Layer
    Copyright (C) 1997-2010 Sam Lantinga

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Sam Lantinga
    slouken@libsdl.org
*/
#include "SDL_config.h"
#include "SDL_version.h"

/* General (mostly internal) pixel/color manipulation routines for SDL */

#include "SDL_endian.h"
#include "SDL_video-1.3.h"
#include "SDL_sysvideo-1.3.h"
#include "SDL_blit.h"
#include "SDL_pixels_c.h"


/* Helper functions */

/* Helper functions */

const char*
SDL_GetPixelFormatName(Uint32 format)
{
    switch (format) {
#define CASE(X) case X: return #X;
    CASE(SDL_PIXELFORMAT_INDEX1LSB)
    CASE(SDL_PIXELFORMAT_INDEX1MSB)
    CASE(SDL_PIXELFORMAT_INDEX4LSB)
    CASE(SDL_PIXELFORMAT_INDEX4MSB)
    CASE(SDL_PIXELFORMAT_INDEX8)
    CASE(SDL_PIXELFORMAT_RGB332)
    CASE(SDL_PIXELFORMAT_RGB444)
    CASE(SDL_PIXELFORMAT_RGB555)
    CASE(SDL_PIXELFORMAT_BGR555)
    CASE(SDL_PIXELFORMAT_ARGB4444)
    CASE(SDL_PIXELFORMAT_RGBA4444)
    CASE(SDL_PIXELFORMAT_ABGR4444)
    CASE(SDL_PIXELFORMAT_BGRA4444)
    CASE(SDL_PIXELFORMAT_ARGB1555)
    CASE(SDL_PIXELFORMAT_RGBA5551)
    CASE(SDL_PIXELFORMAT_ABGR1555)
    CASE(SDL_PIXELFORMAT_BGRA5551)
    CASE(SDL_PIXELFORMAT_RGB565)
    CASE(SDL_PIXELFORMAT_BGR565)
    CASE(SDL_PIXELFORMAT_RGB24)
    CASE(SDL_PIXELFORMAT_BGR24)
    CASE(SDL_PIXELFORMAT_RGB888)
    CASE(SDL_PIXELFORMAT_BGR888)
    CASE(SDL_PIXELFORMAT_ARGB8888)
    CASE(SDL_PIXELFORMAT_RGBA8888)
    CASE(SDL_PIXELFORMAT_ABGR8888)
    CASE(SDL_PIXELFORMAT_BGRA8888)
    CASE(SDL_PIXELFORMAT_ARGB2101010)
#if SDL_VERSION_ATLEAST(1,3,0)
    CASE(SDL_PIXELFORMAT_YV12)
    CASE(SDL_PIXELFORMAT_IYUV)
    CASE(SDL_PIXELFORMAT_YUY2)
    CASE(SDL_PIXELFORMAT_UYVY)
    CASE(SDL_PIXELFORMAT_YVYU)
#endif
#undef CASE
    default:
        return "SDL_PIXELFORMAT_UNKNOWN";
    }
}

SDL_bool
SDL_PixelFormatEnumToMasks(Uint32 format, int *bpp, Uint32 * Rmask,
                           Uint32 * Gmask, Uint32 * Bmask, Uint32 * Amask)
{
    Uint32 masks[4];

    /* Initialize the values here */
    if (SDL_BYTESPERPIXEL(format) <= 2) {
        *bpp = SDL_BITSPERPIXEL(format);
    } else {
        *bpp = SDL_BYTESPERPIXEL(format) * 8;
    }
    *Rmask = *Gmask = *Bmask = *Amask = 0;

    if (format == SDL_PIXELFORMAT_RGB24) {
#if SDL_BYTEORDER == SDL_BIG_ENDIAN
        *Rmask = 0x00FF0000;
        *Gmask = 0x0000FF00;
        *Bmask = 0x000000FF;
#else
        *Rmask = 0x000000FF;
        *Gmask = 0x0000FF00;
        *Bmask = 0x00FF0000;
#endif
        return SDL_TRUE;
    }

    if (format == SDL_PIXELFORMAT_BGR24) {
#if SDL_BYTEORDER == SDL_BIG_ENDIAN
        *Rmask = 0x000000FF;
        *Gmask = 0x0000FF00;
        *Bmask = 0x00FF0000;
#else
        *Rmask = 0x00FF0000;
        *Gmask = 0x0000FF00;
        *Bmask = 0x000000FF;
#endif
        return SDL_TRUE;
    }

    if (SDL_PIXELTYPE(format) != SDL_PIXELTYPE_PACKED8 &&
        SDL_PIXELTYPE(format) != SDL_PIXELTYPE_PACKED16 &&
        SDL_PIXELTYPE(format) != SDL_PIXELTYPE_PACKED32) {
        /* Not a format that uses masks */
        return SDL_TRUE;
    }

    switch (SDL_PIXELLAYOUT(format)) {
    case SDL_PACKEDLAYOUT_332:
        masks[0] = 0x00000000;
        masks[1] = 0x000000E0;
        masks[2] = 0x0000001C;
        masks[3] = 0x00000003;
        break;
    case SDL_PACKEDLAYOUT_4444:
        masks[0] = 0x0000F000;
        masks[1] = 0x00000F00;
        masks[2] = 0x000000F0;
        masks[3] = 0x0000000F;
        break;
    case SDL_PACKEDLAYOUT_1555:
        masks[0] = 0x00008000;
        masks[1] = 0x00007C00;
        masks[2] = 0x000003E0;
        masks[3] = 0x0000001F;
        break;
    case SDL_PACKEDLAYOUT_5551:
        masks[0] = 0x0000F800;
        masks[1] = 0x000007C0;
        masks[2] = 0x0000003E;
        masks[3] = 0x00000001;
        break;
    case SDL_PACKEDLAYOUT_565:
        masks[0] = 0x00000000;
        masks[1] = 0x0000F800;
        masks[2] = 0x000007E0;
        masks[3] = 0x0000001F;
        break;
    case SDL_PACKEDLAYOUT_8888:
        masks[0] = 0xFF000000;
        masks[1] = 0x00FF0000;
        masks[2] = 0x0000FF00;
        masks[3] = 0x000000FF;
        break;
    case SDL_PACKEDLAYOUT_2101010:
        masks[0] = 0xC0000000;
        masks[1] = 0x3FF00000;
        masks[2] = 0x000FFC00;
        masks[3] = 0x000003FF;
        break;
    case SDL_PACKEDLAYOUT_1010102:
        masks[0] = 0xFFC00000;
        masks[1] = 0x003FF000;
        masks[2] = 0x00000FFC;
        masks[3] = 0x00000003;
        break;
    default:
        SDL_SetError("Unknown pixel format");
        return SDL_FALSE;
    }

    switch (SDL_PIXELORDER(format)) {
    case SDL_PACKEDORDER_XRGB:
        *Rmask = masks[1];
        *Gmask = masks[2];
        *Bmask = masks[3];
        break;
    case SDL_PACKEDORDER_RGBX:
        *Rmask = masks[0];
        *Gmask = masks[1];
        *Bmask = masks[2];
        break;
    case SDL_PACKEDORDER_ARGB:
        *Amask = masks[0];
        *Rmask = masks[1];
        *Gmask = masks[2];
        *Bmask = masks[3];
        break;
    case SDL_PACKEDORDER_RGBA:
        *Rmask = masks[0];
        *Gmask = masks[1];
        *Bmask = masks[2];
        *Amask = masks[3];
        break;
    case SDL_PACKEDORDER_XBGR:
        *Bmask = masks[1];
        *Gmask = masks[2];
        *Rmask = masks[3];
        break;
    case SDL_PACKEDORDER_BGRX:
        *Bmask = masks[0];
        *Gmask = masks[1];
        *Rmask = masks[2];
        break;
    case SDL_PACKEDORDER_BGRA:
        *Bmask = masks[0];
        *Gmask = masks[1];
        *Rmask = masks[2];
        *Amask = masks[3];
        break;
    case SDL_PACKEDORDER_ABGR:
        *Amask = masks[0];
        *Bmask = masks[1];
        *Gmask = masks[2];
        *Rmask = masks[3];
        break;
    default:
        SDL_SetError("Unknown pixel format");
        return SDL_FALSE;
    }
    return SDL_TRUE;
}

Uint32
SDL_MasksToPixelFormatEnum(int bpp, Uint32 Rmask, Uint32 Gmask, Uint32 Bmask,
                           Uint32 Amask)
{
    switch (bpp) {
    case 8:
        switch (Rmask) {
        case 0:
            return SDL_PIXELFORMAT_INDEX8;
        case 0xE0:
            return SDL_PIXELFORMAT_RGB332;
        }
        break;
    case 12:
        switch (Rmask) {
        case 0x0F00:
            return SDL_PIXELFORMAT_RGB444;
        }
        break;
    case 15:
        switch (Rmask) {
        case 0x001F:
            return SDL_PIXELFORMAT_BGR555;
        case 0x7C00:
            return SDL_PIXELFORMAT_RGB555;
        }
        break;
    case 16:
        switch (Rmask) {
        case 0xF000:
            return SDL_PIXELFORMAT_RGBA4444;
        case 0x0F00:
            return SDL_PIXELFORMAT_ARGB4444;
        case 0x00F0:
            return SDL_PIXELFORMAT_BGRA4444;
        case 0x000F:
            return SDL_PIXELFORMAT_ABGR4444;
        case 0x001F:
            if (Gmask == 0x07E0) {
                return SDL_PIXELFORMAT_BGR565;
            }
            return SDL_PIXELFORMAT_ABGR1555;
        case 0x7C00:
            return SDL_PIXELFORMAT_ARGB1555;
        case 0xF800:
            if (Gmask == 0x07E0) {
                return SDL_PIXELFORMAT_RGB565;
            }
            return SDL_PIXELFORMAT_RGBA5551;
        }
        break;
    case 24:
        switch (Rmask) {
        case 0x00FF0000:
#if SDL_BYTEORDER == SDL_BIG_ENDIAN
            return SDL_PIXELFORMAT_RGB24;
#else
            return SDL_PIXELFORMAT_BGR24;
#endif
        case 0x000000FF:
#if SDL_BYTEORDER == SDL_BIG_ENDIAN
            return SDL_PIXELFORMAT_BGR24;
#else
            return SDL_PIXELFORMAT_RGB24;
#endif
        case 0x00000000:
            /* FIXME: At this point we can't distinguish */
            /* if this format is RGB24 or BGR24          */
            return SDL_PIXELFORMAT_RGB24;
        }
    case 32:
        switch (Rmask) {
        case 0xFF000000:
            if (Amask == 0x000000FF) {
                return SDL_PIXELFORMAT_RGBA8888;
            }
            break;
        case 0x00FF0000:
            if (Amask == 0xFF000000) {
                return SDL_PIXELFORMAT_ARGB8888;
            } else {
                return SDL_PIXELFORMAT_RGB888;
            }
            break;
        case 0x0000FF00:
            if (Amask == 0x000000FF) {
                return SDL_PIXELFORMAT_BGRA8888;
            }
            break;
        case 0x000000FF:
            if (Amask == 0xFF000000) {
                return SDL_PIXELFORMAT_ABGR8888;
            } else {
                return SDL_PIXELFORMAT_BGR888;
            }
            break;
        case 0x3FF00000:
            return SDL_PIXELFORMAT_ARGB2101010;
        }
    }
    return SDL_PIXELFORMAT_UNKNOWN;
}

SDL_PixelFormat *
SDL_InitFormat(SDL_PixelFormat * format, int bpp, Uint32 Rmask, Uint32 Gmask,
               Uint32 Bmask, Uint32 Amask)
{
    Uint32 mask;

    /* Set up the format */
    SDL_memset(format, 0, sizeof(*format));
    format->BitsPerPixel = bpp;
    format->BytesPerPixel = (bpp + 7) / 8;
    if (Rmask || Bmask || Gmask) {      /* Packed pixels with custom mask */
        format->Rshift = 0;
        format->Rloss = 8;
        if (Rmask) {
            for (mask = Rmask; !(mask & 0x01); mask >>= 1)
                ++format->Rshift;
            for (; (mask & 0x01); mask >>= 1)
                --format->Rloss;
        }
        format->Gshift = 0;
        format->Gloss = 8;
        if (Gmask) {
            for (mask = Gmask; !(mask & 0x01); mask >>= 1)
                ++format->Gshift;
            for (; (mask & 0x01); mask >>= 1)
                --format->Gloss;
        }
        format->Bshift = 0;
        format->Bloss = 8;
        if (Bmask) {
            for (mask = Bmask; !(mask & 0x01); mask >>= 1)
                ++format->Bshift;
            for (; (mask & 0x01); mask >>= 1)
                --format->Bloss;
        }
        format->Ashift = 0;
        format->Aloss = 8;
        if (Amask) {
            for (mask = Amask; !(mask & 0x01); mask >>= 1)
                ++format->Ashift;
            for (; (mask & 0x01); mask >>= 1)
                --format->Aloss;
        }
        format->Rmask = Rmask;
        format->Gmask = Gmask;
        format->Bmask = Bmask;
        format->Amask = Amask;
    } else if (bpp > 8) {       /* Packed pixels with standard mask */
        /* R-G-B */
        if (bpp > 24)
            bpp = 24;
        format->Rloss = 8 - (bpp / 3);
        format->Gloss = 8 - (bpp / 3) - (bpp % 3);
        format->Bloss = 8 - (bpp / 3);
        format->Rshift = ((bpp / 3) + (bpp % 3)) + (bpp / 3);
        format->Gshift = (bpp / 3);
        format->Bshift = 0;
        format->Rmask = ((0xFF >> format->Rloss) << format->Rshift);
        format->Gmask = ((0xFF >> format->Gloss) << format->Gshift);
        format->Bmask = ((0xFF >> format->Bloss) << format->Bshift);
    } else {
        /* Palettized formats have no mask info */
        format->Rloss = 8;
        format->Gloss = 8;
        format->Bloss = 8;
        format->Aloss = 8;
        format->Rshift = 0;
        format->Gshift = 0;
        format->Bshift = 0;
        format->Ashift = 0;
        format->Rmask = 0;
        format->Gmask = 0;
        format->Bmask = 0;
        format->Amask = 0;
    }
    format->palette = NULL;

    return format;
}

/* vi: set ts=4 sw=4 expandtab: */
