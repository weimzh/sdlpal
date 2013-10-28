/*
    SDL - Simple DirectMedia Layer
    Copyright (C) 1997-2009 Sam Lantinga

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
#ifndef _SDL_ANDROIDINPUT_H_
#define _SDL_ANDROIDINPUT_H_

#include "SDL_config.h"

#include "SDL_version.h"
#include "SDL_video.h"
#include "SDL_mouse.h"
#include "SDL_mutex.h"
#include "SDL_thread.h"
#include "../SDL_sysvideo.h"
#include "../SDL_pixels_c.h"
#include "SDL_events.h"
#include "SDL_keysym.h"
#include "../../events/SDL_events_c.h"
#include "SDL_joystick.h"
#include "../../joystick/SDL_sysjoystick.h"
#include "../../joystick/SDL_joystick_c.h"

#include "../SDL_sysvideo.h"
#include "SDL_androidvideo.h"
#include "javakeycodes.h"

/* JNI-C++ wrapper stuff */

// Special key to signal that key should be handled by Java internally, such as Volume Up/Down keys
#define SDLK_NO_REMAP 512
#define SDL_SCANCODE_NO_REMAP SDLK_NO_REMAP

#define SDL_KEY2(X) SDLK_ ## X
#define SDL_KEY(X) SDL_KEY2(X)

// Randomly redefining SDL 1.3 scancodes to SDL 1.2 keycodes
#define KP_0 KP0
#define KP_1 KP1
#define KP_2 KP2
#define KP_3 KP3
#define KP_4 KP4
#define KP_5 KP5
#define KP_6 KP6
#define KP_7 KP7
#define KP_8 KP8
#define KP_9 KP9
#define NUMLOCKCLEAR NUMLOCK
#define GRAVE DOLLAR
#define APOSTROPHE QUOTE
#define LGUI LMETA
#define RGUI RMETA
#define SCROLLLOCK SCROLLOCK
// Overkill haha
#define A a
#define B b
#define C c
#define D d
#define E e
#define F f
#define G g
#define H h
#define I i
#define J j
#define K k
#define L l
#define M m
#define N n
#define O o
#define P p
#define Q q
#define R r
#define S s
#define T t
#define U u
#define V v
#define W w
#define X x
#define Y y
#define Z z

typedef SDLKey SDL_scancode;
#define SDL_GetKeyboardState SDL_GetKeyState

#define SDL_KEY_VAL(X) X

enum MOUSE_ACTION { MOUSE_DOWN = 0, MOUSE_UP = 1, MOUSE_MOVE = 2, MOUSE_HOVER = 3 };

extern int SDL_ANDROID_processTouchscreenKeyboard(int x, int y, int action, int pointerId);
extern int SDL_ANDROID_isTouchscreenKeyboardUsed;

// Queue events to main thread
extern void SDL_ANDROID_MainThreadPushMouseMotion(int x, int y);
extern void SDL_ANDROID_MainThreadPushMouseButton(int pressed, int button);
extern void SDL_ANDROID_MainThreadPushKeyboardKey(int pressed, SDL_scancode key);
extern void SDL_ANDROID_MainThreadPushVideoExpose();
extern void SDL_ANDROID_MainThreadPushWindowResize(int w, int h);
extern void SDL_ANDROID_MainThreadPushFakeWindowResize(int w, int h);

extern void SDL_android_init_keymap(SDLKey *SDL_android_keymap);

#endif
