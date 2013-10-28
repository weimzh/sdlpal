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

/* The high-level video driver subsystem */

#include "SDL.h"
#include "SDL_video-1.3.h"
#include "SDL_sysvideo-1.3.h"
#include "SDL_androidvideo.h"
#include "SDL_blit.h"
#include "SDL_pixels_c.h"
#include "SDL_renderer_gl.h"
#include "SDL_renderer_gles.h"
#include "SDL_renderer_sw.h"
#ifdef ANDROID
#include <android/log.h>
#endif

#if SDL_VIDEO_OPENGL_ES
#include "SDL_opengles.h"
#endif /* SDL_VIDEO_OPENGL_ES */

#if SDL_VIDEO_OPENGL
#include "SDL_opengl.h"

/* On Windows, windows.h defines CreateWindow */
#ifdef CreateWindow
#undef CreateWindow
#endif
#endif /* SDL_VIDEO_OPENGL */

/* Available video drivers */
static VideoBootStrap *bootstrap[] = {
#if SDL_VIDEO_DRIVER_COCOA
    &COCOA_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_X11
    &X11_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_FBCON
    &FBCON_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_DIRECTFB
    &DirectFB_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_PS3
    &PS3_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_SVGALIB
    &SVGALIB_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_GAPI
    &GAPI_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_WIN32
    &WIN32_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_BWINDOW
    &BWINDOW_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_PHOTON
    &photon_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_QNXGF
    &qnxgf_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_EPOC
    &EPOC_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_RISCOS
    &RISCOS_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_NDS
    &NDS_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_UIKIT
    &UIKIT_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_ANDROID
	&ANDROID_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_DUMMY
    &DUMMY_bootstrap,
#endif
#if SDL_VIDEO_DRIVER_PANDORA
    &PND_bootstrap,
#endif
    NULL
};

static SDL_VideoDevice *_this = NULL;

#define CHECK_WINDOW_MAGIC(window, retval) \
    if (!_this) { \
        SDL_UninitializedVideo(); \
        return retval; \
    } \
    if (!window || window->magic != &_this->window_magic) { \
        SDL_SetError("Invalid window"); \
        return retval; \
    }

#define CHECK_TEXTURE_MAGIC(texture, retval) \
    if (!_this) { \
        SDL_UninitializedVideo(); \
        return retval; \
    } \
    if (!texture || texture->magic != &_this->texture_magic) { \
        SDL_SetError("Invalid texture"); \
        return retval; \
    }

/* Various local functions */
static void SDL_UpdateWindowGrab(SDL_Window * window);

static int
cmpmodes(const void *A, const void *B)
{
    SDL_DisplayMode a = *(const SDL_DisplayMode *) A;
    SDL_DisplayMode b = *(const SDL_DisplayMode *) B;

    if (a.w != b.w) {
        return b.w - a.w;
    }
    if (a.h != b.h) {
        return b.h - a.h;
    }
    if (SDL_BITSPERPIXEL(a.format) != SDL_BITSPERPIXEL(b.format)) {
        return SDL_BITSPERPIXEL(b.format) - SDL_BITSPERPIXEL(a.format);
    }
    if (SDL_PIXELLAYOUT(a.format) != SDL_PIXELLAYOUT(b.format)) {
        return SDL_PIXELLAYOUT(b.format) - SDL_PIXELLAYOUT(a.format);
    }
    if (a.refresh_rate != b.refresh_rate) {
        return b.refresh_rate - a.refresh_rate;
    }
    return 0;
}

static void
SDL_UninitializedVideo()
{
    SDL_SetError("Video subsystem has not been initialized");
}

int
SDL_GetNumVideoDrivers(void)
{
    return SDL_arraysize(bootstrap) - 1;
}

const char *
SDL_GetVideoDriver(int index)
{
    if (index >= 0 && index < SDL_GetNumVideoDrivers()) {
        return bootstrap[index]->name;
    }
    return NULL;
}

/*
 * Initialize the video and event subsystems -- determine native pixel format
 */
int SDL_VideoInit_1_3
(const char *driver_name, Uint32 flags)
{
    SDL_VideoDevice *video;
    int index;
    int i;

    /* Check to make sure we don't overwrite '_this' */
    if (_this != NULL) {
        SDL_VideoQuit();
    }

    /* Select the proper video driver */
    index = 0;
    video = NULL;

    video = ANDROID_CreateDevice_1_3(0);
    _this = video;
    _this->name = "android";
    _this->next_object_id = 1;


    /* Set some very sane GL defaults */
    _this->gl_config.driver_loaded = 0;
    _this->gl_config.dll_handle = NULL;
    _this->gl_config.red_size = 3;
    _this->gl_config.green_size = 3;
    _this->gl_config.blue_size = 2;
    _this->gl_config.alpha_size = 0;
    _this->gl_config.buffer_size = 0;
    _this->gl_config.depth_size = 16;
    _this->gl_config.stencil_size = 0;
    _this->gl_config.double_buffer = 1;
    _this->gl_config.accum_red_size = 0;
    _this->gl_config.accum_green_size = 0;
    _this->gl_config.accum_blue_size = 0;
    _this->gl_config.accum_alpha_size = 0;
    _this->gl_config.stereo = 0;
    _this->gl_config.multisamplebuffers = 0;
    _this->gl_config.multisamplesamples = 0;
    _this->gl_config.retained_backing = 1;
    _this->gl_config.accelerated = -1;  /* accelerated or not, both are fine */
    _this->gl_config.major_version = 2;
    _this->gl_config.minor_version = 1;

    /* Initialize the video subsystem */
    if (_this->VideoInit(_this) < 0) {
        SDL_VideoQuit();
        return -1;
    }
    /* Make sure some displays were added */
    if (_this->num_displays == 0) {
        SDL_SetError("The video driver did not add any displays");
        SDL_VideoQuit();
        return (-1);
    }
    /* The software renderer is always available */
    for (i = 0; i < _this->num_displays; ++i) {
        SDL_VideoDisplay *display = &_this->displays[i];
        if (_this->GL_CreateContext) {
#if SDL_VIDEO_RENDER_OGL
            SDL_AddRenderDriver(display, &GL_RenderDriver);
#endif
#if SDL_VIDEO_RENDER_OGL_ES
            SDL_AddRenderDriver(display, &GL_ES_RenderDriver);
#endif
        }
    }

    /* We're ready to go! */
    return 0;
}

const char *
SDL_GetCurrentVideoDriver()
{
    if (!_this) {
        SDL_UninitializedVideo();
        return NULL;
    }
    return _this->name;
}

SDL_VideoDevice *
SDL_GetVideoDevice(void)
{
    return _this;
}

int
SDL_AddBasicVideoDisplay(const SDL_DisplayMode * desktop_mode)
{
    SDL_VideoDisplay display;

    SDL_memset(&display, 0, sizeof(display));
    if (desktop_mode) {
        display.desktop_mode = *desktop_mode;
    }
    display.current_mode = display.desktop_mode;

    return SDL_AddVideoDisplay(&display);
}

int
SDL_AddVideoDisplay(const SDL_VideoDisplay * display)
{
    SDL_VideoDisplay *displays;
    int index = -1;

    displays =
        SDL_realloc(_this->displays,
                    (_this->num_displays + 1) * sizeof(*displays));
    if (displays) {
        index = _this->num_displays++;
        displays[index] = *display;
        displays[index].device = _this;
        _this->displays = displays;
    } else {
        SDL_OutOfMemory();
    }
    return index;
}

int
SDL_GetNumVideoDisplays(void)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return 0;
    }
    return _this->num_displays;
}

int
SDL_GetDisplayBounds(int index, SDL_Rect * rect)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    if (index < 0 || index >= _this->num_displays) {
        SDL_SetError("index must be in the range 0 - %d",
                     _this->num_displays - 1);
        return -1;
    }
    if (rect) {
        SDL_VideoDisplay *display = &_this->displays[index];

        if (_this->GetDisplayBounds) {
            if (_this->GetDisplayBounds(_this, display, rect) < 0) {
                return -1;
            }
        } else {
            /* Assume that the displays are left to right */
            if (index == 0) {
                rect->x = 0;
                rect->y = 0;
            } else {
                SDL_GetDisplayBounds(index-1, rect);
                rect->x += rect->w;
            }
            rect->w = display->desktop_mode.w;
            rect->h = display->desktop_mode.h;
        }
    }
    return 0;
}

int
SDL_SelectVideoDisplay(int index)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return (-1);
    }
    if (index < 0 || index >= _this->num_displays) {
        SDL_SetError("index must be in the range 0 - %d",
                     _this->num_displays - 1);
        return -1;
    }
    _this->current_display = index;
    return 0;
}

int
SDL_GetCurrentVideoDisplay(void)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return (-1);
    }
    return _this->current_display;
}

SDL_bool
SDL_AddDisplayMode(SDL_VideoDisplay * display,  const SDL_DisplayMode * mode)
{
    SDL_DisplayMode *modes;
    int i, nmodes;

    /* Make sure we don't already have the mode in the list */
    modes = display->display_modes;
    nmodes = display->num_display_modes;
    for (i = nmodes; i--;) {
        if (SDL_memcmp(mode, &modes[i], sizeof(*mode)) == 0) {
            return SDL_FALSE;
        }
    }

    /* Go ahead and add the new mode */
    if (nmodes == display->max_display_modes) {
        modes =
            SDL_realloc(modes,
                        (display->max_display_modes + 32) * sizeof(*modes));
        if (!modes) {
            return SDL_FALSE;
        }
        display->display_modes = modes;
        display->max_display_modes += 32;
    }
    modes[nmodes] = *mode;
    display->num_display_modes++;

    /* Re-sort video modes */
    SDL_qsort(display->display_modes, display->num_display_modes,
              sizeof(SDL_DisplayMode), cmpmodes);

    return SDL_TRUE;
}

int
SDL_GetNumDisplayModesForDisplay(SDL_VideoDisplay * display)
{
    if (!display->num_display_modes && _this->GetDisplayModes) {
        _this->GetDisplayModes(_this, display);
        SDL_qsort(display->display_modes, display->num_display_modes,
                  sizeof(SDL_DisplayMode), cmpmodes);
    }
    return display->num_display_modes;
}

int
SDL_GetNumDisplayModes()
{
    if (_this) {
        return SDL_GetNumDisplayModesForDisplay(SDL_CurrentDisplay);
    }
    return 0;
}

int
SDL_GetDisplayModeForDisplay(SDL_VideoDisplay * display, int index, SDL_DisplayMode * mode)
{
    if (index < 0 || index >= SDL_GetNumDisplayModesForDisplay(display)) {
        SDL_SetError("index must be in the range of 0 - %d",
                     SDL_GetNumDisplayModesForDisplay(display) - 1);
        return -1;
    }
    if (mode) {
        *mode = display->display_modes[index];
    }
    return 0;
}

int
SDL_GetDisplayMode(int index, SDL_DisplayMode * mode)
{
    return SDL_GetDisplayModeForDisplay(SDL_CurrentDisplay, index, mode);
}

int
SDL_GetDesktopDisplayModeForDisplay(SDL_VideoDisplay * display, SDL_DisplayMode * mode)
{
    if (mode) {
        *mode = display->desktop_mode;
    }
    return 0;
}

int
SDL_GetDesktopDisplayMode(SDL_DisplayMode * mode)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    return SDL_GetDesktopDisplayModeForDisplay(SDL_CurrentDisplay, mode);
}

int
SDL_GetCurrentDisplayModeForDisplay(SDL_VideoDisplay * display, SDL_DisplayMode * mode)
{
    if (mode) {
        *mode = display->current_mode;
    }
    return 0;
}

int
SDL_GetCurrentDisplayMode(SDL_DisplayMode * mode)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    return SDL_GetCurrentDisplayModeForDisplay(SDL_CurrentDisplay, mode);
}

SDL_DisplayMode *
SDL_GetClosestDisplayModeForDisplay(SDL_VideoDisplay * display,
                                    const SDL_DisplayMode * mode,
                                    SDL_DisplayMode * closest)
{
    Uint32 target_format;
    int target_refresh_rate;
    int i;
    SDL_DisplayMode *current, *match;

    if (!mode || !closest) {
        SDL_SetError("Missing desired mode or closest mode parameter");
        return NULL;
    }

    /* Default to the desktop format */
    if (mode->format) {
        target_format = mode->format;
    } else {
        target_format = display->desktop_mode.format;
    }

    /* Default to the desktop refresh rate */
    if (mode->refresh_rate) {
        target_refresh_rate = mode->refresh_rate;
    } else {
        target_refresh_rate = display->desktop_mode.refresh_rate;
    }

    match = NULL;
    for (i = 0; i < SDL_GetNumDisplayModesForDisplay(display); ++i) {
        current = &display->display_modes[i];

        if (current->w && (current->w < mode->w)) {
            /* Out of sorted modes large enough here */
            break;
        }
        if (current->h && (current->h < mode->h)) {
            if (current->w && (current->w == mode->w)) {
                /* Out of sorted modes large enough here */
                break;
            }
            /* Wider, but not tall enough, due to a different
               aspect ratio. This mode must be skipped, but closer
               modes may still follow. */
            continue;
        }
        if (!match || current->w < match->w || current->h < match->h) {
            match = current;
            continue;
        }
        if (current->format != match->format) {
            /* Sorted highest depth to lowest */
            if (current->format == target_format ||
                (SDL_BITSPERPIXEL(current->format) >=
                 SDL_BITSPERPIXEL(target_format)
                 && SDL_PIXELTYPE(current->format) ==
                 SDL_PIXELTYPE(target_format))) {
                match = current;
            }
            continue;
        }
        if (current->refresh_rate != match->refresh_rate) {
            /* Sorted highest refresh to lowest */
            if (current->refresh_rate >= target_refresh_rate) {
                match = current;
            }
        }
    }
    if (match) {
        if (match->format) {
            closest->format = match->format;
        } else {
            closest->format = mode->format;
        }
        if (match->w && match->h) {
            closest->w = match->w;
            closest->h = match->h;
        } else {
            closest->w = mode->w;
            closest->h = mode->h;
        }
        if (match->refresh_rate) {
            closest->refresh_rate = match->refresh_rate;
        } else {
            closest->refresh_rate = mode->refresh_rate;
        }
        closest->driverdata = match->driverdata;

        /*
         * Pick some reasonable defaults if the app and driver don't
         * care
         */
        if (!closest->format) {
            closest->format = SDL_PIXELFORMAT_RGB888;
        }
        if (!closest->w) {
            closest->w = 640;
        }
        if (!closest->h) {
            closest->h = 480;
        }
        return closest;
    }
    return NULL;
}

SDL_DisplayMode *
SDL_GetClosestDisplayMode(const SDL_DisplayMode * mode,
                          SDL_DisplayMode * closest)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return NULL;
    }
    return SDL_GetClosestDisplayModeForDisplay(SDL_CurrentDisplay, mode, closest);
}

int
SDL_SetDisplayModeForDisplay(SDL_VideoDisplay * display, const SDL_DisplayMode * mode)
{
    SDL_DisplayMode display_mode;
    SDL_DisplayMode current_mode;
    int ncolors;

    if (mode) {
        display_mode = *mode;

        /* Default to the current mode */
        if (!display_mode.format) {
            display_mode.format = display->current_mode.format;
        }
        if (!display_mode.w) {
            display_mode.w = display->current_mode.w;
        }
        if (!display_mode.h) {
            display_mode.h = display->current_mode.h;
        }
        if (!display_mode.refresh_rate) {
            display_mode.refresh_rate = display->current_mode.refresh_rate;
        }

        /* Get a good video mode, the closest one possible */
        if (!SDL_GetClosestDisplayModeForDisplay(display, &display_mode, &display_mode)) {
            SDL_SetError("No video mode large enough for %dx%d",
                         display_mode.w, display_mode.h);
            return -1;
        }
    } else {
        display_mode = display->desktop_mode;
    }

    /* See if there's anything left to do */
    SDL_GetCurrentDisplayModeForDisplay(display, &current_mode);
    if (SDL_memcmp(&display_mode, &current_mode, sizeof(display_mode)) == 0) {
        return 0;
    }

    /* Actually change the display mode */
    if (_this->SetDisplayMode(_this, display, &display_mode) < 0) {
        return -1;
    }
    display->current_mode = display_mode;

    return 0;
}

int
SDL_SetWindowDisplayMode(SDL_Window * window, const SDL_DisplayMode * mode)
{
    CHECK_WINDOW_MAGIC(window, -1);

    if (mode) {
        window->fullscreen_mode = *mode;
    } else {
        SDL_memset(&(window->fullscreen_mode), 0, sizeof(window->fullscreen_mode));
    }
    return 0;
}

int
SDL_GetWindowDisplayMode(SDL_Window * window, SDL_DisplayMode * mode)
{
    SDL_DisplayMode fullscreen_mode;

    CHECK_WINDOW_MAGIC(window, -1);

    fullscreen_mode = window->fullscreen_mode;
    if (!fullscreen_mode.w) {
        fullscreen_mode.w = window->w;
    }
    if (!fullscreen_mode.h) {
        fullscreen_mode.h = window->h;
    }

    if (!SDL_GetClosestDisplayModeForDisplay(window->display,
                                             &fullscreen_mode,
                                             &fullscreen_mode)) {
        SDL_SetError("Couldn't find display mode match");
        return -1;
    }

    if (mode) {
        *mode = fullscreen_mode;
    }
    return 0;
}

SDL_Window *
SDL_CreateWindow(const char *title, int x, int y, int w, int h, Uint32 flags)
{
    const Uint32 allowed_flags = (SDL_WINDOW_FULLSCREEN |
                                  SDL_WINDOW_OPENGL |
                                  SDL_WINDOW_BORDERLESS |
                                  SDL_WINDOW_RESIZABLE |
                                  SDL_WINDOW_INPUT_GRABBED);
    SDL_VideoDisplay *display;
    SDL_Window *window;

    display = SDL_CurrentDisplay;
    window = (SDL_Window *)SDL_calloc(1, sizeof(*window));
    window->magic = &_this->window_magic;
    window->id = _this->next_object_id++;
    window->x = x;
    window->y = y;
    window->w = w;
    window->h = h;
    window->flags = (flags & allowed_flags);
    window->display = display;
    window->next = display->windows;
    if (display->windows) {
        display->windows->prev = window;
    }
    display->windows = window;

    return window;
}

static __inline__ SDL_Renderer *
SDL_GetCurrentRenderer(SDL_bool create)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return NULL;
    }
    if (!SDL_CurrentRenderer) {
        if (!create) {
            SDL_SetError("Use SDL_CreateRenderer() to create a renderer");
            return NULL;
        }
        if (SDL_CreateRenderer(0, -1, 0) < 0) {
            return NULL;
        }
    }
    return SDL_CurrentRenderer;
}

void
SDL_DestroyWindow(SDL_Window * window)
{
    SDL_VideoDisplay *display;

    CHECK_WINDOW_MAGIC(window, );
    window->magic = NULL;

    if (window->title) {
        SDL_free(window->title);
    }
    if (window->renderer) {
        SDL_DestroyRenderer(window);
    }

    if (_this->DestroyWindow) {
        _this->DestroyWindow(_this, window);
    }

    /* Unlink the window from the list */
    display = window->display;
    if (window->next) {
        window->next->prev = window->prev;
    }
    if (window->prev) {
        window->prev->next = window->next;
    } else {
        display->windows = window->next;
    }

    SDL_free(window);
}

void
SDL_AddRenderDriver(SDL_VideoDisplay * display, const SDL_RenderDriver * driver)
{
    SDL_RenderDriver *render_drivers;

    render_drivers =
        SDL_realloc(display->render_drivers,
                    (display->num_render_drivers +
                     1) * sizeof(*render_drivers));
    if (render_drivers) {
        render_drivers[display->num_render_drivers] = *driver;
        display->render_drivers = render_drivers;
        display->num_render_drivers++;
    }
}

int
SDL_GetNumRenderDrivers(void)
{
    if (_this) {
        return SDL_CurrentDisplay->num_render_drivers;
    }
    return 0;
}

int
SDL_GetRenderDriverInfo(int index, SDL_RendererInfo * info)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    if (index < 0 || index >= SDL_GetNumRenderDrivers()) {
        SDL_SetError("index must be in the range of 0 - %d",
                     SDL_GetNumRenderDrivers() - 1);
        return -1;
    }
    *info = SDL_CurrentDisplay->render_drivers[index].info;
    return 0;
}

int
SDL_CreateRenderer(SDL_Window * window, int index, Uint32 flags)
{
    CHECK_WINDOW_MAGIC(window, -1);

    /* Free any existing renderer */
    SDL_DestroyRenderer(window);

    window->renderer = GL_ES_RenderDriver.CreateRenderer(window, flags);

    if (window->renderer == NULL) {
        /* Assuming renderer set its error */
        return -1;
    }

    SDL_SelectRenderer(window);

    return 0;
}

int
SDL_SelectRenderer(SDL_Window * window)
{
    SDL_Renderer *renderer;

    CHECK_WINDOW_MAGIC(window, -1);

    renderer = window->renderer;
    if (!renderer) {
        SDL_SetError("Use SDL_CreateRenderer() to create a renderer");
        return -1;
    }
    if (renderer->ActivateRenderer) {
        if (renderer->ActivateRenderer(renderer) < 0) {
            return -1;
        }
    }
    SDL_CurrentDisplay->current_renderer = renderer;
    return 0;
}

int
SDL_GetRendererInfo(SDL_RendererInfo * info)
{
    SDL_Renderer *renderer = SDL_GetCurrentRenderer(SDL_FALSE);
    if (!renderer) {
        return -1;
    }
    *info = renderer->info;
    return 0;
}

SDL_Texture *
SDL_CreateTexture(Uint32 format, int access, int w, int h)
{
    SDL_Renderer *renderer;
    SDL_Texture *texture;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return 0;
    }
    if (!renderer->CreateTexture) {
        SDL_Unsupported();
        return 0;
    }
    if (w <= 0 || h <= 0) {
        SDL_SetError("Texture dimensions can't be 0");
        return 0;
    }
    texture = (SDL_Texture *) SDL_calloc(1, sizeof(*texture));
    if (!texture) {
        SDL_OutOfMemory();
        return 0;
    }
    texture->magic = &_this->texture_magic;
    texture->format = format;
    texture->access = access;
    texture->w = w;
    texture->h = h;
    texture->r = 255;
    texture->g = 255;
    texture->b = 255;
    texture->a = 255;
    texture->renderer = renderer;
    texture->next = renderer->textures;
    if (renderer->textures) {
        renderer->textures->prev = texture;
    }
    renderer->textures = texture;

    if (renderer->CreateTexture(renderer, texture) < 0) {
        SDL_DestroyTexture(texture);
        return 0;
    }
    return texture;
}

SDL_Texture *
SDL_CreateTextureFromSurface(Uint32 format, SDL_Surface * surface)
{
    SDL_Texture *texture;
    Uint32 requested_format = format;
    SDL_PixelFormat *fmt;
    SDL_Renderer *renderer;
    int bpp;
    Uint32 Rmask, Gmask, Bmask, Amask;

    if (!surface) {
        SDL_SetError("SDL_CreateTextureFromSurface() passed NULL surface");
        return 0;
    }
    fmt = surface->format;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return 0;
    }

    if (format) {
        if (!SDL_PixelFormatEnumToMasks
            (format, &bpp, &Rmask, &Gmask, &Bmask, &Amask)) {
            SDL_SetError("Unknown pixel format");
            return 0;
        }
    } else {
        if (surface->format->Amask
            || !(surface->flags &
                 (SDL_SRCCOLORKEY | SDL_SRCALPHA))) {
            Uint32 it;
            int pfmt;

            /* Pixel formats, sorted by best first */
            static const Uint32 sdl_pformats[] = {
                SDL_PIXELFORMAT_ARGB8888,
                SDL_PIXELFORMAT_RGBA8888,
                SDL_PIXELFORMAT_ABGR8888,
                SDL_PIXELFORMAT_BGRA8888,
                SDL_PIXELFORMAT_RGB888,
                SDL_PIXELFORMAT_BGR888,
                SDL_PIXELFORMAT_RGB24,
                SDL_PIXELFORMAT_BGR24,
                SDL_PIXELFORMAT_RGB565,
                SDL_PIXELFORMAT_BGR565,
                SDL_PIXELFORMAT_ARGB1555,
                SDL_PIXELFORMAT_ABGR1555,
                SDL_PIXELFORMAT_RGBA5551,
                SDL_PIXELFORMAT_RGB555,
                SDL_PIXELFORMAT_BGR555,
                SDL_PIXELFORMAT_ARGB4444,
                SDL_PIXELFORMAT_ABGR4444,
                SDL_PIXELFORMAT_RGBA4444,
                SDL_PIXELFORMAT_RGB444,
                SDL_PIXELFORMAT_ARGB2101010,
                SDL_PIXELFORMAT_INDEX8,
                SDL_PIXELFORMAT_INDEX4LSB,
                SDL_PIXELFORMAT_INDEX4MSB,
                SDL_PIXELFORMAT_RGB332,
                SDL_PIXELFORMAT_INDEX1LSB,
                SDL_PIXELFORMAT_INDEX1MSB,
                SDL_PIXELFORMAT_UNKNOWN
            };

            bpp = fmt->BitsPerPixel;
            Rmask = fmt->Rmask;
            Gmask = fmt->Gmask;
            Bmask = fmt->Bmask;
            Amask = fmt->Amask;

            format =
                SDL_MasksToPixelFormatEnum(bpp, Rmask, Gmask, Bmask, Amask);
            if (!format) {
                SDL_SetError("Unknown pixel format");
                return 0;
            }

            /* Search requested format in the supported texture */
            /* formats by current renderer                      */
            for (it = 0; it < renderer->info.num_texture_formats; it++) {
                if (renderer->info.texture_formats[it] == format) {
                    break;
                }
            }

            /* If requested format can't be found, search any best */
            /* format which renderer provides                      */
            if (it == renderer->info.num_texture_formats) {
                pfmt = 0;
                for (;;) {
                    if (sdl_pformats[pfmt] == SDL_PIXELFORMAT_UNKNOWN) {
                        break;
                    }

                    for (it = 0; it < renderer->info.num_texture_formats;
                         it++) {
                        if (renderer->info.texture_formats[it] ==
                            sdl_pformats[pfmt]) {
                            break;
                        }
                    }

                    if (it != renderer->info.num_texture_formats) {
                        /* The best format has been found */
                        break;
                    }
                    pfmt++;
                }

                /* If any format can't be found, then return an error */
                if (it == renderer->info.num_texture_formats) {
                    SDL_SetError
                        ("Any of the supported pixel formats can't be found");
                    return 0;
                }

                /* Convert found pixel format back to color masks */
                if (SDL_PixelFormatEnumToMasks
                    (renderer->info.texture_formats[it], &bpp, &Rmask, &Gmask,
                     &Bmask, &Amask) != SDL_TRUE) {
                    SDL_SetError("Unknown pixel format");
                    return 0;
                }
            }
        } else {
            /* Need a format with alpha */
            Uint32 it;
            int apfmt;

            /* Pixel formats with alpha, sorted by best first */
            static const Uint32 sdl_alpha_pformats[] = {
                SDL_PIXELFORMAT_ARGB8888,
                SDL_PIXELFORMAT_RGBA8888,
                SDL_PIXELFORMAT_ABGR8888,
                SDL_PIXELFORMAT_BGRA8888,
                SDL_PIXELFORMAT_ARGB1555,
                SDL_PIXELFORMAT_ABGR1555,
                SDL_PIXELFORMAT_RGBA5551,
                SDL_PIXELFORMAT_ARGB4444,
                SDL_PIXELFORMAT_ABGR4444,
                SDL_PIXELFORMAT_RGBA4444,
                SDL_PIXELFORMAT_ARGB2101010,
                SDL_PIXELFORMAT_UNKNOWN
            };

            if (surface->format->Amask) {
                /* If surface already has alpha, then try an original */
                /* surface format first                               */
                bpp = fmt->BitsPerPixel;
                Rmask = fmt->Rmask;
                Gmask = fmt->Gmask;
                Bmask = fmt->Bmask;
                Amask = fmt->Amask;
            } else {
                bpp = 32;
                Rmask = 0x00FF0000;
                Gmask = 0x0000FF00;
                Bmask = 0x000000FF;
                Amask = 0xFF000000;
            }

            format =
                SDL_MasksToPixelFormatEnum(bpp, Rmask, Gmask, Bmask, Amask);
            if (!format) {
                SDL_SetError("Unknown pixel format");
                return 0;
            }

            /* Search this format in the supported texture formats */
            /* by current renderer                                 */
            for (it = 0; it < renderer->info.num_texture_formats; it++) {
                if (renderer->info.texture_formats[it] == format) {
                    break;
                }
            }

            /* If this format can't be found, search any best       */
            /* compatible format with alpha which renderer provides */
            if (it == renderer->info.num_texture_formats) {
                apfmt = 0;
                for (;;) {
                    if (sdl_alpha_pformats[apfmt] == SDL_PIXELFORMAT_UNKNOWN) {
                        break;
                    }

                    for (it = 0; it < renderer->info.num_texture_formats;
                         it++) {
                        if (renderer->info.texture_formats[it] ==
                            sdl_alpha_pformats[apfmt]) {
                            break;
                        }
                    }

                    if (it != renderer->info.num_texture_formats) {
                        /* Compatible format has been found */
                        break;
                    }
                    apfmt++;
                }

                /* If compatible format can't be found, then return an error */
                if (it == renderer->info.num_texture_formats) {
                    SDL_SetError("Compatible pixel format can't be found");
                    return 0;
                }

                /* Convert found pixel format back to color masks */
                if (SDL_PixelFormatEnumToMasks
                    (renderer->info.texture_formats[it], &bpp, &Rmask, &Gmask,
                     &Bmask, &Amask) != SDL_TRUE) {
                    SDL_SetError("Unknown pixel format");
                    return 0;
                }
            }
        }

        format = SDL_MasksToPixelFormatEnum(bpp, Rmask, Gmask, Bmask, Amask);
        if (!format) {
            SDL_SetError("Unknown pixel format");
            return 0;
        }
    }

    texture =
        SDL_CreateTexture(format, SDL_TEXTUREACCESS_STATIC, surface->w,
                          surface->h);
    if (!texture && !requested_format) {
        SDL_DisplayMode desktop_mode;
        SDL_GetDesktopDisplayMode(&desktop_mode);
        format = desktop_mode.format;
        texture =
            SDL_CreateTexture(format, SDL_TEXTUREACCESS_STATIC, surface->w,
                              surface->h);
    }
    if (!texture) {
        return 0;
    }
    if (bpp == fmt->BitsPerPixel && Rmask == fmt->Rmask && Gmask == fmt->Gmask
        && Bmask == fmt->Bmask && Amask == fmt->Amask) {
        if (SDL_MUSTLOCK(surface)) {
            SDL_LockSurface(surface);
            SDL_UpdateTexture(texture, NULL, surface->pixels,
                              surface->pitch);
            SDL_UnlockSurface(surface);
        } else {
            SDL_UpdateTexture(texture, NULL, surface->pixels,
                              surface->pitch);
        }
    } else {
        SDL_PixelFormat dst_fmt;
        SDL_Surface *dst = NULL;

        /* Set up a destination surface for the texture update */
        SDL_InitFormat(&dst_fmt, bpp, Rmask, Gmask, Bmask, Amask);
        dst = SDL_ConvertSurface(surface, &dst_fmt, 0);
        if (dst) {
            SDL_UpdateTexture(texture, NULL, dst->pixels, dst->pitch);
            SDL_FreeSurface(dst);
        }

        if (!dst) {
            SDL_DestroyTexture(texture);
            return 0;
        }
    }

    {
        Uint8 r, g, b, a;
        int blendMode;
        int scaleMode;

        if(surface->flags & SDL_SRCALPHA) {
            SDL_SetTextureAlphaMod(texture, surface->format->alpha);
            SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_BLEND);
        }
    }
    return texture;
}

int
SDL_QueryTexture(SDL_Texture * texture, Uint32 * format, int *access,
                 int *w, int *h)
{
    CHECK_TEXTURE_MAGIC(texture, -1);

    if (format) {
        *format = texture->format;
    }
    if (access) {
        *access = texture->access;
    }
    if (w) {
        *w = texture->w;
    }
    if (h) {
        *h = texture->h;
    }
    return 0;
}

int
SDL_QueryTexturePixels(SDL_Texture * texture, void **pixels, int *pitch)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->QueryTexturePixels) {
        SDL_Unsupported();
        return -1;
    }
    return renderer->QueryTexturePixels(renderer, texture, pixels, pitch);
}

int
SDL_SetTexturePalette(SDL_Texture * texture, const SDL_Color * colors,
                      int firstcolor, int ncolors)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->SetTexturePalette) {
        SDL_Unsupported();
        return -1;
    }
    return renderer->SetTexturePalette(renderer, texture, colors, firstcolor,
                                       ncolors);
}

int
SDL_GetTexturePalette(SDL_Texture * texture, SDL_Color * colors,
                      int firstcolor, int ncolors)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->GetTexturePalette) {
        SDL_Unsupported();
        return -1;
    }
    return renderer->GetTexturePalette(renderer, texture, colors, firstcolor,
                                       ncolors);
}

int
SDL_SetTextureColorMod(SDL_Texture * texture, Uint8 r, Uint8 g, Uint8 b)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->SetTextureColorMod) {
        SDL_Unsupported();
        return -1;
    }
    if (r < 255 || g < 255 || b < 255) {
        texture->modMode |= SDL_TEXTUREMODULATE_COLOR;
    } else {
        texture->modMode &= ~SDL_TEXTUREMODULATE_COLOR;
    }
    texture->r = r;
    texture->g = g;
    texture->b = b;
    return renderer->SetTextureColorMod(renderer, texture);
}

int
SDL_GetTextureColorMod(SDL_Texture * texture, Uint8 * r, Uint8 * g,
                       Uint8 * b)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (r) {
        *r = texture->r;
    }
    if (g) {
        *g = texture->g;
    }
    if (b) {
        *b = texture->b;
    }
    return 0;
}

int
SDL_SetTextureAlphaMod(SDL_Texture * texture, Uint8 alpha)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->SetTextureAlphaMod) {
        SDL_Unsupported();
        return -1;
    }
    if (alpha < 255) {
        texture->modMode |= SDL_TEXTUREMODULATE_ALPHA;
    } else {
        texture->modMode &= ~SDL_TEXTUREMODULATE_ALPHA;
    }
    texture->a = alpha;
    return renderer->SetTextureAlphaMod(renderer, texture);
}

int
SDL_GetTextureAlphaMod(SDL_Texture * texture, Uint8 * alpha)
{
    CHECK_TEXTURE_MAGIC(texture, -1);

    if (alpha) {
        *alpha = texture->a;
    }
    return 0;
}

int
SDL_SetTextureBlendMode(SDL_Texture * texture, int blendMode)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->SetTextureBlendMode) {
        SDL_Unsupported();
        return -1;
    }
    texture->blendMode = blendMode;
    return renderer->SetTextureBlendMode(renderer, texture);
}

int
SDL_GetTextureBlendMode(SDL_Texture * texture, int *blendMode)
{
    CHECK_TEXTURE_MAGIC(texture, -1);

    if (blendMode) {
        *blendMode = texture->blendMode;
    }
    return 0;
}

int
SDL_SetTextureScaleMode(SDL_Texture * texture, int scaleMode)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->SetTextureScaleMode) {
        SDL_Unsupported();
        return -1;
    }
    texture->scaleMode = scaleMode;
    return renderer->SetTextureScaleMode(renderer, texture);
}

int
SDL_GetTextureScaleMode(SDL_Texture * texture, int *scaleMode)
{
    CHECK_TEXTURE_MAGIC(texture, -1);

    if (scaleMode) {
        *scaleMode = texture->scaleMode;
    }
    return 0;
}

int
SDL_UpdateTexture(SDL_Texture * texture, const SDL_Rect * rect,
                  const void *pixels, int pitch)
{
    SDL_Renderer *renderer;
    SDL_Rect full_rect;

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = texture->renderer;
    if (!renderer->UpdateTexture) {
        SDL_Unsupported();
        return -1;
    }
    if (!rect) {
        full_rect.x = 0;
        full_rect.y = 0;
        full_rect.w = texture->w;
        full_rect.h = texture->h;
        rect = &full_rect;
    }
    return renderer->UpdateTexture(renderer, texture, rect, pixels, pitch);
}

int
SDL_LockTexture(SDL_Texture * texture, const SDL_Rect * rect, int markDirty,
                void **pixels, int *pitch)
{
    SDL_Renderer *renderer;
    SDL_Rect full_rect;

    CHECK_TEXTURE_MAGIC(texture, -1);

    if (texture->access != SDL_TEXTUREACCESS_STREAMING) {
        SDL_SetError("SDL_LockTexture(): texture must be streaming");
        return -1;
    }
    renderer = texture->renderer;
    if (!renderer->LockTexture) {
        SDL_Unsupported();
        return -1;
    }
    if (!rect) {
        full_rect.x = 0;
        full_rect.y = 0;
        full_rect.w = texture->w;
        full_rect.h = texture->h;
        rect = &full_rect;
    }
    return renderer->LockTexture(renderer, texture, rect, markDirty, pixels,
                                 pitch);
}

void
SDL_UnlockTexture(SDL_Texture * texture)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, );

    if (texture->access != SDL_TEXTUREACCESS_STREAMING) {
        return;
    }
    renderer = texture->renderer;
    if (!renderer->UnlockTexture) {
        return;
    }
    renderer->UnlockTexture(renderer, texture);
}

void
SDL_DirtyTexture(SDL_Texture * texture, int numrects,
                 const SDL_Rect * rects)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, );

    if (texture->access != SDL_TEXTUREACCESS_STREAMING) {
        return;
    }
    renderer = texture->renderer;
    if (!renderer->DirtyTexture) {
        return;
    }
    renderer->DirtyTexture(renderer, texture, numrects, rects);
}

int
SDL_SetRenderDrawColor(Uint8 r, Uint8 g, Uint8 b, Uint8 a)
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    renderer->r = r;
    renderer->g = g;
    renderer->b = b;
    renderer->a = a;
    if (renderer->SetDrawColor) {
        return renderer->SetDrawColor(renderer);
    } else {
        return 0;
    }
}

int
SDL_GetRenderDrawColor(Uint8 * r, Uint8 * g, Uint8 * b, Uint8 * a)
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (r) {
        *r = renderer->r;
    }
    if (g) {
        *g = renderer->g;
    }
    if (b) {
        *b = renderer->b;
    }
    if (a) {
        *a = renderer->a;
    }
    return 0;
}

int
SDL_SetRenderDrawBlendMode(int blendMode)
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    renderer->blendMode = blendMode;
    if (renderer->SetDrawBlendMode) {
        return renderer->SetDrawBlendMode(renderer);
    } else {
        return 0;
    }
}

int
SDL_GetRenderDrawBlendMode(int *blendMode)
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    *blendMode = renderer->blendMode;
    return 0;
}

int
SDL_RenderClear()
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderClear) {
        int blendMode = renderer->blendMode;
        int status;

        if (blendMode >= SDL_BLENDMODE_BLEND) {
            SDL_SetRenderDrawBlendMode(SDL_BLENDMODE_NONE);
        }

        status = SDL_RenderFillRect(NULL);

        if (blendMode >= SDL_BLENDMODE_BLEND) {
            SDL_SetRenderDrawBlendMode(blendMode);
        }
        return status;
    }
    return renderer->RenderClear(renderer);
}

#if SDL_VIDEO_RENDER_RESIZE

static inline void
SDL_RESIZE_resizePoints(int realW, int fakeW, int realH, int fakeH,
                        const SDL_Point * src, SDL_Point * dest, int count )
{
    int i;
    for( i = 0; i < count; i++ ) {
        dest[i].x = src[i].x * realW / fakeW;
        dest[i].y = src[i].y * realH / fakeH;
    }
}

static inline void
SDL_RESIZE_resizeRects(int realW, int fakeW, int realH, int fakeH,
                       const SDL_Rect ** src, SDL_Rect * dest, int count )
{
    int i;
    for( i = 0; i < count; i++ ) {
        // Calculate bottom-right corner instead of width/height, and substract upper-left corner,
        // otherwise we'll have rounding errors and holes between textures
        dest[i].x = src[i]->x * realW / fakeW;
        dest[i].y = src[i]->y * realH / fakeH;
        dest[i].w = (src[i]->w + src[i]->x) * realW / fakeW - dest[i].x;
        dest[i].h = (src[i]->h + src[i]->y) * realH / fakeH - dest[i].y;
    }
}

#endif

int
SDL_RenderDrawPoint(int x, int y)
{
    SDL_Point point;

    point.x = x;
    point.y = y;

    return SDL_RenderDrawPoints(&point, 1);
}

int
SDL_RenderDrawPoints(const SDL_Point * points, int count)
{
    SDL_Renderer *renderer;
#if SDL_VIDEO_RENDER_RESIZE
    int realW, realH, fakeW, fakeH, ret;
#endif

    if (!points) {
        SDL_SetError("SDL_RenderDrawPoints(): Passed NULL points");
        return -1;
    }

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderDrawPoints) {
        SDL_Unsupported();
        return -1;
    }
    if (count < 1) {
        return 0;
    }

#if SDL_VIDEO_RENDER_RESIZE
    realW = renderer->window->display->desktop_mode.w;
    realH = renderer->window->display->desktop_mode.h;
    fakeW = renderer->window->w;
    fakeH = renderer->window->h;
    //if( fakeW > realW || fakeH > realH )
    {
        SDL_Point * resized = SDL_stack_alloc( SDL_Point, count );
        if( ! resized ) {
            SDL_OutOfMemory();
            return -1;
        }
        SDL_RESIZE_resizePoints( realW, fakeW, realH, fakeH, points, resized, count );
        ret = renderer->RenderDrawPoints(renderer, resized, count);
        SDL_stack_free(resized);
        return ret;
    }
#endif

    return renderer->RenderDrawPoints(renderer, points, count);
}

int
SDL_RenderDrawLine(int x1, int y1, int x2, int y2)
{
    SDL_Point points[2];

    points[0].x = x1;
    points[0].y = y1;
    points[1].x = x2;
    points[1].y = y2;
    return SDL_RenderDrawLines(points, 2);
}

int
SDL_RenderDrawLines(const SDL_Point * points, int count)
{
    SDL_Renderer *renderer;
#if SDL_VIDEO_RENDER_RESIZE
    int realW, realH, fakeW, fakeH, ret;
#endif

    if (!points) {
        SDL_SetError("SDL_RenderDrawLines(): Passed NULL points");
        return -1;
    }

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderDrawLines) {
        SDL_Unsupported();
        return -1;
    }
    if (count < 2) {
        return 0;
    }

#if SDL_VIDEO_RENDER_RESIZE
    realW = renderer->window->display->desktop_mode.w;
    realH = renderer->window->display->desktop_mode.h;
    fakeW = renderer->window->w;
    fakeH = renderer->window->h;
    //if( fakeW > realW || fakeH > realH )
    {
        SDL_Point * resized = SDL_stack_alloc( SDL_Point, count );
        if( ! resized ) {
            SDL_OutOfMemory();
            return -1;
        }
        SDL_RESIZE_resizePoints( realW, fakeW, realH, fakeH, points, resized, count );
        ret = renderer->RenderDrawLines(renderer, resized, count);
        SDL_stack_free(resized);
        return ret;
    }
#endif

    return renderer->RenderDrawLines(renderer, points, count);
}

int
SDL_RenderDrawRect(const SDL_Rect * rect)
{
    return SDL_RenderDrawRects(&rect, 1);
}

int
SDL_RenderDrawRects(const SDL_Rect ** rects, int count)
{
    SDL_Renderer *renderer;
    int i;
#if SDL_VIDEO_RENDER_RESIZE
    int realW, realH, fakeW, fakeH, ret;
#endif

    if (!rects) {
        SDL_SetError("SDL_RenderDrawRects(): Passed NULL rects");
        return -1;
    }

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderDrawRects) {
        SDL_Unsupported();
        return -1;
    }
    if (count < 1) {
        return 0;
    }
    /* Check for NULL rect, which means fill entire window */
    for (i = 0; i < count; ++i) {
        if (rects[i] == NULL) {
            SDL_Window *window = renderer->window;
            SDL_Rect full_rect;
            const SDL_Rect *rect;

            full_rect.x = 0;
            full_rect.y = 0;
            full_rect.w = window->w;
            full_rect.h = window->h;
            rect = &full_rect;
            return renderer->RenderDrawRects(renderer, &rect, 1);
        }
    }

#if SDL_VIDEO_RENDER_RESIZE
    realW = renderer->window->display->desktop_mode.w;
    realH = renderer->window->display->desktop_mode.h;
    fakeW = renderer->window->w;
    fakeH = renderer->window->h;
    //if( fakeW > realW || fakeH > realH )
    {
        SDL_Rect * resized = SDL_stack_alloc( SDL_Rect, count );
        if( ! resized ) {
            SDL_OutOfMemory();
            return -1;
        }

        const SDL_Rect ** resizedPtrs = SDL_stack_alloc( const SDL_Rect *, count );
        if( ! resizedPtrs ) {
            SDL_OutOfMemory();
            return -1;
        }

        for( i = 0; i < count; i++ ) {
            resizedPtrs[i] = &(resized[i]);
        }
        SDL_RESIZE_resizeRects( realW, fakeW, realH, fakeH, rects, resized, count );
        ret = renderer->RenderDrawRects(renderer, resizedPtrs, count);
        SDL_stack_free(resizedPtrs);
        SDL_stack_free(resized);
        return ret;
    }
#endif

    return renderer->RenderDrawRects(renderer, rects, count);
}

int
SDL_RenderFillRect(const SDL_Rect * rect)
{
    return SDL_RenderFillRects(&rect, 1);
}

int
SDL_RenderFillRects(const SDL_Rect ** rects, int count)
{
    SDL_Renderer *renderer;
    int i;
#if SDL_VIDEO_RENDER_RESIZE
    int realW, realH, fakeW, fakeH, ret;
#endif

    if (!rects) {
        SDL_SetError("SDL_RenderFillRects(): Passed NULL rects");
        return -1;
    }

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderFillRects) {
        SDL_Unsupported();
        return -1;
    }
    if (count < 1) {
        return 0;
    }
    /* Check for NULL rect, which means fill entire window */
    for (i = 0; i < count; ++i) {
        if (rects[i] == NULL) {
            SDL_Window *window = renderer->window;
            SDL_Rect full_rect;
            const SDL_Rect *rect;

            full_rect.x = 0;
            full_rect.y = 0;
            full_rect.w = window->w;
            full_rect.h = window->h;
            rect = &full_rect;
            return renderer->RenderFillRects(renderer, &rect, 1);
        }
    }

#if SDL_VIDEO_RENDER_RESIZE
    realW = renderer->window->display->desktop_mode.w;
    realH = renderer->window->display->desktop_mode.h;
    fakeW = renderer->window->w;
    fakeH = renderer->window->h;
    //if( fakeW > realW || fakeH > realH )
    {
        SDL_Rect * resized = SDL_stack_alloc( SDL_Rect, count );
        if( ! resized ) {
            SDL_OutOfMemory();
            return -1;
        }

        const SDL_Rect ** resizedPtrs = SDL_stack_alloc( const SDL_Rect *, count );
        if( ! resizedPtrs ) {
            SDL_OutOfMemory();
            return -1;
        }

        for( i = 0; i < count; i++ ) {
            resizedPtrs[i] = &(resized[i]);
        }
        SDL_RESIZE_resizeRects( realW, fakeW, realH, fakeH, rects, resized, count );
        ret = renderer->RenderFillRects(renderer, resizedPtrs, count);
        SDL_stack_free(resizedPtrs);
        SDL_stack_free(resized);
        return ret;
    }
#endif

    return renderer->RenderFillRects(renderer, rects, count);
}

int
SDL_RenderCopy(SDL_Texture * texture, const SDL_Rect * srcrect,
               const SDL_Rect * dstrect)
{
    SDL_Renderer *renderer;
    SDL_Window *window;
    SDL_Rect real_srcrect;
    SDL_Rect real_dstrect;
#if SDL_VIDEO_RENDER_RESIZE
    int realW;
    int realH;
    int fakeW;
    int fakeH;
#endif

    CHECK_TEXTURE_MAGIC(texture, -1);

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (texture->renderer != renderer) {
        SDL_SetError("Texture was not created with this renderer");
        return -1;
    }
    if (!renderer->RenderCopy) {
        SDL_Unsupported();
        return -1;
    }
    window = renderer->window;

    real_srcrect.x = 0;
    real_srcrect.y = 0;
    real_srcrect.w = texture->w;
    real_srcrect.h = texture->h;
    if (srcrect) {
        if (!SDL_IntersectRect(srcrect, &real_srcrect, &real_srcrect)) {
            return 0;
        }
    }

    real_dstrect.x = 0;
    real_dstrect.y = 0;
    real_dstrect.w = window->w;
    real_dstrect.h = window->h;
    if (dstrect) {
        if (!SDL_IntersectRect(dstrect, &real_dstrect, &real_dstrect)) {
            return 0;
        }
        /* Clip srcrect by the same amount as dstrect was clipped */
        if (dstrect->w != real_dstrect.w) {
            int deltax = (real_dstrect.x - dstrect->x);
            int deltaw = (real_dstrect.w - dstrect->w);
            real_srcrect.x += (deltax * real_srcrect.w) / dstrect->w;
            real_srcrect.w += (deltaw * real_srcrect.w) / dstrect->w;
        }
        if (dstrect->h != real_dstrect.h) {
            int deltay = (real_dstrect.y - dstrect->y);
            int deltah = (real_dstrect.h - dstrect->h);
            real_srcrect.y += (deltay * real_srcrect.h) / dstrect->h;
            real_srcrect.h += (deltah * real_srcrect.h) / dstrect->h;
        }
    }

#if SDL_VIDEO_RENDER_RESIZE
    realW = window->display->desktop_mode.w;
    realH = window->display->desktop_mode.h;
    fakeW = window->w;
    fakeH = window->h;
    //if( fakeW > realW || fakeH > realH )
    {
        // Calculate bottom-right corner instead of width/height, and substract upper-left corner,
        // otherwise we'll have rounding errors and holes between textures
        real_dstrect.w = (real_dstrect.w + real_dstrect.x) * realW / fakeW;
        real_dstrect.h = (real_dstrect.h + real_dstrect.y) * realH / fakeH;
        real_dstrect.x = real_dstrect.x * realW / fakeW;
        real_dstrect.y = real_dstrect.y * realH / fakeH;
        real_dstrect.w -= real_dstrect.x;
        real_dstrect.h -= real_dstrect.y;
        //__android_log_print(ANDROID_LOG_INFO, "libSDL", "SDL_RenderCopy dest %d:%d+%d+%d desktop_mode %d:%d", (int)real_dstrect.x, (int)real_dstrect.y, (int)real_dstrect.w, (int)real_dstrect.h, (int)realW, (int)realH);
    }
#endif

    return renderer->RenderCopy(renderer, texture, &real_srcrect,
                                &real_dstrect);
}

int
SDL_RenderReadPixels(const SDL_Rect * rect, Uint32 format,
                     void * pixels, int pitch)
{
    SDL_Renderer *renderer;
    SDL_Window *window;
    SDL_Rect real_rect;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderReadPixels) {
        SDL_Unsupported();
        return -1;
    }
    window = renderer->window;

    if (!format) {
        format = window->display->current_mode.format;
    }

    real_rect.x = 0;
    real_rect.y = 0;
    real_rect.w = window->w;
    real_rect.h = window->h;
    if (rect) {
        if (!SDL_IntersectRect(rect, &real_rect, &real_rect)) {
            return 0;
        }
        if (real_rect.y > rect->y) {
            pixels = (Uint8 *)pixels + pitch * (real_rect.y - rect->y);
        }
        if (real_rect.x > rect->x) {
            Uint32 format = SDL_CurrentDisplay->current_mode.format;
            int bpp = SDL_BYTESPERPIXEL(format);
            pixels = (Uint8 *)pixels + bpp * (real_rect.x - rect->x);
        }
    }

    return renderer->RenderReadPixels(renderer, &real_rect,
                                      format, pixels, pitch);
}

int
SDL_RenderWritePixels(const SDL_Rect * rect, Uint32 format,
                      const void * pixels, int pitch)
{
    SDL_Renderer *renderer;
    SDL_Window *window;
    SDL_Rect real_rect;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer) {
        return -1;
    }
    if (!renderer->RenderWritePixels) {
        SDL_Unsupported();
        return -1;
    }
    window = renderer->window;

    if (!format) {
        format = window->display->current_mode.format;
    }

    real_rect.x = 0;
    real_rect.y = 0;
    real_rect.w = window->w;
    real_rect.h = window->h;
    if (rect) {
        if (!SDL_IntersectRect(rect, &real_rect, &real_rect)) {
            return 0;
        }
        if (real_rect.y > rect->y) {
            pixels = (const Uint8 *)pixels + pitch * (real_rect.y - rect->y);
        }
        if (real_rect.x > rect->x) {
            Uint32 format = SDL_CurrentDisplay->current_mode.format;
            int bpp = SDL_BYTESPERPIXEL(format);
            pixels = (const Uint8 *)pixels + bpp * (real_rect.x - rect->x);
        }
    }

    return renderer->RenderWritePixels(renderer, &real_rect,
                                       format, pixels, pitch);
}

void
SDL_RenderPresent(void)
{
    SDL_Renderer *renderer;

    renderer = SDL_GetCurrentRenderer(SDL_TRUE);
    if (!renderer || !renderer->RenderPresent) {
        return;
    }
    renderer->RenderPresent(renderer);
}

void
SDL_DestroyTexture(SDL_Texture * texture)
{
    SDL_Renderer *renderer;

    CHECK_TEXTURE_MAGIC(texture, );
    texture->magic = NULL;

    renderer = texture->renderer;
    if (texture->next) {
        texture->next->prev = texture->prev;
    }
    if (texture->prev) {
        texture->prev->next = texture->next;
    } else {
        renderer->textures = texture->next;
    }

    renderer->DestroyTexture(renderer, texture);
    SDL_free(texture);
}

void
SDL_DestroyRenderer(SDL_Window * window)
{
    SDL_Renderer *renderer;

    CHECK_WINDOW_MAGIC(window, );

    renderer = window->renderer;
    if (!renderer) {
        return;
    }

    /* Free existing textures for this renderer */
    while (renderer->textures) {
        SDL_DestroyTexture(renderer->textures);
    }

    /* Free the renderer instance */
    renderer->DestroyRenderer(renderer);

    /* Clear references */
    window->renderer = NULL;
    if (SDL_CurrentDisplay->current_renderer == renderer) {
        SDL_CurrentDisplay->current_renderer = NULL;
    }
}

SDL_bool
SDL_IsScreenSaverEnabled()
{
    if (!_this) {
        return SDL_TRUE;
    }
    return _this->suspend_screensaver ? SDL_FALSE : SDL_TRUE;
}

void
SDL_EnableScreenSaver()
{
    if (!_this) {
        return;
    }
    if (!_this->suspend_screensaver) {
        return;
    }
    _this->suspend_screensaver = SDL_FALSE;
    if (_this->SuspendScreenSaver) {
        _this->SuspendScreenSaver(_this);
    }
}

void
SDL_DisableScreenSaver()
{
    if (!_this) {
        return;
    }
    if (_this->suspend_screensaver) {
        return;
    }
    _this->suspend_screensaver = SDL_TRUE;
    if (_this->SuspendScreenSaver) {
        _this->SuspendScreenSaver(_this);
    }
}

SDL_bool
SDL_GL_ExtensionSupported(const char *extension)
{
#if SDL_VIDEO_OPENGL || SDL_VIDEO_OPENGL_ES
    const GLubyte *(APIENTRY * glGetStringFunc) (GLenum);
    const char *extensions;
    const char *start;
    const char *where, *terminator;

    /* Extension names should not have spaces. */
    where = SDL_strchr(extension, ' ');
    if (where || *extension == '\0') {
        return SDL_FALSE;
    }
    /* See if there's an environment variable override */
    start = SDL_getenv(extension);
    if (start && *start == '0') {
        return SDL_FALSE;
    }
    /* Lookup the available extensions */
    glGetStringFunc = SDL_GL_GetProcAddress("glGetString");
    if (glGetStringFunc) {
        extensions = (const char *) glGetStringFunc(GL_EXTENSIONS);
    } else {
        extensions = NULL;
    }
    if (!extensions) {
        return SDL_FALSE;
    }
    /*
     * It takes a bit of care to be fool-proof about parsing the OpenGL
     * extensions string. Don't be fooled by sub-strings, etc.
     */

    start = extensions;

    for (;;) {
        where = SDL_strstr(start, extension);
        if (!where)
            break;

        terminator = where + SDL_strlen(extension);
        if (where == start || *(where - 1) == ' ')
            if (*terminator == ' ' || *terminator == '\0')
                return SDL_TRUE;

        start = terminator;
    }
    return SDL_FALSE;
#else
    return SDL_FALSE;
#endif
}

SDL_GLContext
SDL_GL_CreateContext(SDL_Window * window)
{
    CHECK_WINDOW_MAGIC(window, NULL);

    if (!(window->flags & SDL_WINDOW_OPENGL)) {
        SDL_SetError("The specified window isn't an OpenGL window");
        return NULL;
    }
    return _this->GL_CreateContext(_this, window);
}

int
SDL_GL_MakeCurrent(SDL_Window * window, SDL_GLContext context)
{
    CHECK_WINDOW_MAGIC(window, -1);

    if (!(window->flags & SDL_WINDOW_OPENGL)) {
        SDL_SetError("The specified window isn't an OpenGL window");
        return -1;
    }
    if (!context) {
        window = NULL;
    }
    return _this->GL_MakeCurrent(_this, window, context);
}

int
SDL_GL_SetSwapInterval(int interval)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    if (_this->GL_SetSwapInterval) {
        return _this->GL_SetSwapInterval(_this, interval);
    } else {
        SDL_SetError("Setting the swap interval is not supported");
        return -1;
    }
}

int
SDL_GL_GetSwapInterval(void)
{
    if (!_this) {
        SDL_UninitializedVideo();
        return -1;
    }
    if (_this->GL_GetSwapInterval) {
        return _this->GL_GetSwapInterval(_this);
    } else {
        SDL_SetError("Getting the swap interval is not supported");
        return -1;
    }
}

void
SDL_GL_SwapWindow(SDL_Window * window)
{
    CHECK_WINDOW_MAGIC(window, );

    if (!(window->flags & SDL_WINDOW_OPENGL)) {
        SDL_SetError("The specified window isn't an OpenGL window");
        return;
    }
    _this->GL_SwapWindow(_this, window);
}

void
SDL_GL_DeleteContext(SDL_GLContext context)
{
    if (!_this || !_this->gl_data || !context) {
        return;
    }
    _this->GL_MakeCurrent(_this, NULL, NULL);
    _this->GL_DeleteContext(_this, context);
}


/* vi: set ts=4 sw=4 expandtab: */
