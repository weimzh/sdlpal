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
#include <jni.h>
#include <android/log.h>
#include <sys/time.h>
#include <time.h>
#include <stdint.h>
#include <math.h>
#include <string.h> // for memset()

#include "SDL_config.h"

#include "SDL_version.h"
#include "SDL_mutex.h"
#include "SDL_events.h"

#include "../SDL_sysvideo.h"
#include "SDL_androidvideo.h"
#include "SDL_androidinput.h"
#include "jniwrapperstuff.h"
#include "atan2i.h"

#define MIN(X, Y) ((X) < (Y) ? (X) : (Y))
#define MAX(X, Y) ((X) > (Y) ? (X) : (Y))

static SDLKey SDL_android_keymap[KEYCODE_LAST+1];

static inline SDL_scancode TranslateKey(int scancode)
{
	if ( scancode >= SDL_arraysize(SDL_android_keymap) )
		scancode = KEYCODE_UNKNOWN;
	return SDL_android_keymap[scancode];
}

enum { MOUSE_HW_BUTTON_LEFT = 1, MOUSE_HW_BUTTON_RIGHT = 2, MOUSE_HW_BUTTON_MIDDLE = 4, MOUSE_HW_BUTTON_BACK = 8, MOUSE_HW_BUTTON_FORWARD = 16, MOUSE_HW_BUTTON_MAX = MOUSE_HW_BUTTON_FORWARD };

JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeMotionEvent) ( JNIEnv*  env, jobject  thiz, jint x, jint y)
{
	if( !SDL_CurrentVideoSurface )
		return;

	SDL_ANDROID_MainThreadPushMouseMotion(x, y);
}
/*
JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeTouchpad) ( JNIEnv*  env, jobject thiz, jint x, jint y, jint down, jint multitouch)
{
	if( !isMouseUsed )
		return;

	if( ! down )
	{
		SDL_ANDROID_MainThreadPushMouseButton( SDL_RELEASED, SDL_BUTTON_RIGHT );
		SDL_ANDROID_MainThreadPushMouseButton( SDL_RELEASED, SDL_BUTTON_LEFT );
		moveMouseWithKbX = -1;
		moveMouseWithKbY = -1;
		moveMouseWithKbAccelUpdateNeeded = 0;
	}
	else
	{
		// x and y from 0 to 65535
		if( moveMouseWithKbX < 0 )
		{
			moveMouseWithKbX = currentMouseX;
			moveMouseWithKbY = currentMouseY;
		}
		moveMouseWithKbSpeedX = (x - 32767) / 8192;
		moveMouseWithKbSpeedY = (y - 32767) / 8192;
		//moveMouseWithKbX += moveMouseWithKbSpeedX;
		//moveMouseWithKbY += moveMouseWithKbSpeedY;
		SDL_ANDROID_MainThreadPushMouseMotion(moveMouseWithKbX, moveMouseWithKbY);
		moveMouseWithKbAccelUpdateNeeded = 1;
		
		if( multitouch )
			SDL_ANDROID_MainThreadPushMouseButton( SDL_PRESSED, SDL_BUTTON_RIGHT );
		else
			if( abs(x - 32767) < 8192 && abs(y - 32767) < 8192 )
				SDL_ANDROID_MainThreadPushMouseButton( SDL_PRESSED, SDL_BUTTON_LEFT );
	}
}
*/

JNIEXPORT jint JNICALL
JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeKey) ( JNIEnv*  env, jobject thiz, jint key, jint action )
{
	if( !SDL_CurrentVideoSurface )
		return 1;
	
	SDL_scancode sdlKey = TranslateKey(key);

	if( sdlKey == SDLK_NO_REMAP || sdlKey == SDLK_UNKNOWN )
		return 0;
	
	if( sdlKey == SDLK_LCLICK ){
		SDL_ANDROID_MainThreadPushMouseButton( action ? SDL_PRESSED : SDL_RELEASED, SDL_BUTTON_LEFT );
	} else if( sdlKey == SDLK_RCLICK ) {
		SDL_ANDROID_MainThreadPushMouseButton( action ? SDL_PRESSED : SDL_RELEASED, SDL_BUTTON_RIGHT );
	} else {
		SDL_ANDROID_MainThreadPushKeyboardKey( action ? SDL_PRESSED : SDL_RELEASED, sdlKey );
	}
	return 1;
}

JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeMouseButtonsPressed) (JNIEnv* env, jobject thiz, jint buttonId, jint pressedState)
{
	if( !SDL_CurrentVideoSurface )
		return;
	
	int btn = SDL_BUTTON_LEFT;

	switch(buttonId)
	{
		case MOUSE_HW_BUTTON_LEFT:
			btn = SDL_BUTTON_LEFT;
			break;
		case MOUSE_HW_BUTTON_RIGHT:
			btn = SDL_BUTTON_RIGHT;
			break;
		case MOUSE_HW_BUTTON_MIDDLE:
			btn = SDL_BUTTON_MIDDLE;
			break;
		case MOUSE_HW_BUTTON_BACK:
			btn = SDL_BUTTON_X1;
			break;
		case MOUSE_HW_BUTTON_FORWARD:
			btn = SDL_BUTTON_X2;
			break;
	}
	
	SDL_ANDROID_MainThreadPushMouseButton( pressedState ? SDL_PRESSED : SDL_RELEASED, btn );
}

JNIEXPORT void JNICALL
JAVA_EXPORT_NAME(DemoGLSurfaceView_nativeRequestUpdateSurface) ( JNIEnv*  env, jobject thiz )
{
	SDL_ANDROID_MainThreadPushVideoExpose();
}

enum { MAX_BUFFERED_EVENTS = 64 };
static SDL_Event BufferedEvents[MAX_BUFFERED_EVENTS];
static int BufferedEventsStart = 0, BufferedEventsEnd = 0;
static SDL_mutex * BufferedEventsMutex = NULL;

#define SDL_ANDROID_BUFFERED_EVENT_RESIZE_WINDOW		(SDL_USEREVENT+1)
#define SDL_ANDROID_BUFFERED_EVENT_RESIZE_FAKEWINDOW	(SDL_USEREVENT+2)

static int sWindowWidthForMotion  = 0;
static int sWindowHeightForMotion = 0;
static int sFakeWindowWidthForMotion  = 0;
static int sFakeWindowHeightForMotion = 0;

extern void SDL_ANDROID_PumpEvents()
{
	static int oldMouseButtons = 0;
	SDL_Event ev;

	SDL_mutexP(BufferedEventsMutex);
	while( BufferedEventsStart != BufferedEventsEnd )
	{
		ev = BufferedEvents[BufferedEventsStart];
		BufferedEvents[BufferedEventsStart].type = 0;
		BufferedEventsStart++;
		if( BufferedEventsStart >= MAX_BUFFERED_EVENTS )
			BufferedEventsStart = 0;
		SDL_mutexV(BufferedEventsMutex);
		
		switch( ev.type )
		{
			case SDL_MOUSEMOTION:
				//__android_log_print(ANDROID_LOG_INFO, "libSDL", "SDL_MOUSEMOTION REAL: x=%i y=%i", ev.motion.x, ev.motion.y);
				if( SDL_ANDROID_sWindowWidth > 0 && SDL_ANDROID_sWindowHeight > 0 ){
					int x = ev.motion.x * SDL_ANDROID_sFakeWindowWidth  / SDL_ANDROID_sWindowWidth;
					int y = ev.motion.y * SDL_ANDROID_sFakeWindowHeight / SDL_ANDROID_sWindowHeight;
					//__android_log_print(ANDROID_LOG_INFO, "libSDL", "SDL_MOUSEMOTION: x=%i y=%i", x, y);
					SDL_PrivateMouseMotion(0, 0, x, y);
				}
				break;
			case SDL_MOUSEBUTTONDOWN:
				//__android_log_print(ANDROID_LOG_INFO, "libSDL", "SDL_MOUSEBUTTONDOWN: %i %i", ev.button.button, ev.button.state);
				if( ((oldMouseButtons & SDL_BUTTON(ev.button.button)) != 0) != ev.button.state )
				{
					oldMouseButtons = (oldMouseButtons & ~SDL_BUTTON(ev.button.button)) | (ev.button.state ? SDL_BUTTON(ev.button.button) : 0);
					SDL_PrivateMouseButton( ev.button.state, ev.button.button, 0, 0 );
				}
				break;
			case SDL_KEYDOWN:
				//__android_log_print(ANDROID_LOG_INFO, "libSDL", "SDL_KEYDOWN: %i %i", ev.key.keysym.sym, ev.key.state);
				SDL_PrivateKeyboard( ev.key.state, &ev.key.keysym );
				break;
			
			//
			case SDL_VIDEOEXPOSE:
			{
				SDL_Surface* screen = SDL_GetVideoSurface();
				if(screen){
					SDL_Flip(screen);
				}
			}
				break;
			default:
				break;
		}

		SDL_mutexP(BufferedEventsMutex);
	}
	SDL_mutexV(BufferedEventsMutex);
};

// Queue events to main thread
static int getNextEventAndLock()
{
	int nextEvent;
	if( !BufferedEventsMutex )
		return -1;
	SDL_mutexP(BufferedEventsMutex);
	nextEvent = BufferedEventsEnd;
	nextEvent++;
	if( nextEvent >= MAX_BUFFERED_EVENTS )
		nextEvent = 0;
	while( nextEvent == BufferedEventsStart )
	{
		SDL_mutexV(BufferedEventsMutex);
		if( SDL_ANDROID_InsideVideoThread() )
			SDL_ANDROID_PumpEvents();
		else
			SDL_Delay(100);
		SDL_mutexP(BufferedEventsMutex);
		nextEvent = BufferedEventsEnd;
		nextEvent++;
		if( nextEvent >= MAX_BUFFERED_EVENTS )
			nextEvent = 0;
	}
	return nextEvent;
}

static int getPrevEventNoLock()
{
	int prevEvent;
	if(BufferedEventsStart == BufferedEventsEnd)
		return -1;
	prevEvent = BufferedEventsEnd;
	prevEvent--;
	if( prevEvent < 0 )
		prevEvent = MAX_BUFFERED_EVENTS - 1;
	return prevEvent;
}

extern void SDL_ANDROID_MainThreadPushMouseMotion(int x, int y)
{
	int nextEvent = getNextEventAndLock();
	if( nextEvent == -1 )
		return;
	
	int prevEvent = getPrevEventNoLock();
	if( prevEvent > 0 && BufferedEvents[prevEvent].type == SDL_MOUSEMOTION )
	{
		// Reuse previous mouse motion event, to prevent mouse movement lag
		BufferedEvents[prevEvent].motion.x = x;
		BufferedEvents[prevEvent].motion.y = y;
	}
	else
	{
		SDL_Event * ev = &BufferedEvents[BufferedEventsEnd];
		ev->type = SDL_MOUSEMOTION;
		ev->motion.x = x;
		ev->motion.y = y;
	}
	
	BufferedEventsEnd = nextEvent;
	SDL_mutexV(BufferedEventsMutex);
};

extern void SDL_ANDROID_MainThreadPushMouseButton(int pressed, int button)
{
	int nextEvent = getNextEventAndLock();
	if( nextEvent == -1 )
		return;
	
	SDL_Event * ev = &BufferedEvents[BufferedEventsEnd];
	
	ev->type = SDL_MOUSEBUTTONDOWN;
	ev->button.state = pressed;
	ev->button.button = button;
	
	BufferedEventsEnd = nextEvent;
	SDL_mutexV(BufferedEventsMutex);
};

extern void SDL_ANDROID_MainThreadPushKeyboardKey(int pressed, SDL_scancode key)
{
	int nextEvent = getNextEventAndLock();
	if( nextEvent == -1 )
		return;
	
	SDL_Event * ev = &BufferedEvents[BufferedEventsEnd];

	ev->type = SDL_KEYDOWN;
	ev->key.state = pressed;
	ev->key.keysym.scancode = key;
	ev->key.keysym.sym = key;
	ev->key.keysym.mod = KMOD_NONE;
	ev->key.keysym.unicode = 0;
	if ( SDL_TranslateUNICODE )
		ev->key.keysym.unicode = key;

	BufferedEventsEnd = nextEvent;
	SDL_mutexV(BufferedEventsMutex);
};

extern void SDL_ANDROID_MainThreadPushVideoExpose()
{
	int nextEvent = getNextEventAndLock();
	if( nextEvent == -1 )
		return;
	
	SDL_Event * ev = &BufferedEvents[BufferedEventsEnd];
	
	ev->type = SDL_VIDEOEXPOSE;
	
	BufferedEventsEnd = nextEvent;
	SDL_mutexV(BufferedEventsMutex);
};

void ANDROID_InitOSKeymap()
{
	if( !BufferedEventsMutex )
		BufferedEventsMutex = SDL_CreateMutex();
}

JNIEXPORT jint JNICALL 
JAVA_EXPORT_NAME(Settings_nativeGetKeymapKey) (JNIEnv* env, jobject thiz, jint code)
{
	if( code < 0 || code > KEYCODE_LAST )
		return SDL_KEY(UNKNOWN);
	return SDL_android_keymap[code];
}

JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(Settings_nativeSetKeymapKey) (JNIEnv* env, jobject thiz, jint javakey, jint key)
{
	if( javakey < 0 || javakey > KEYCODE_LAST )
		return;
	SDL_android_keymap[javakey] = key;
}

JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(Settings_nativeInitKeymap) ( JNIEnv*  env, jobject thiz )
{
	SDL_android_init_keymap(SDL_android_keymap);
}

//

extern JavaVM* g_JavaVM;

static jclass JavaMainViewClass = NULL;
static jobject JavaMainView = NULL;
static jmethodID JavaSetMousePointForNative = NULL;

JNIEXPORT void JNICALL 
JAVA_EXPORT_NAME(MainView_nativeInitInputJavaCallbacks) ( JNIEnv*  env, jobject thiz )
{
	JNIEnv* JavaEnv = env;
	JavaMainView = (*JavaEnv)->NewGlobalRef( JavaEnv, thiz );
	
	JavaMainViewClass = (*JavaEnv)->GetObjectClass(JavaEnv, thiz);
	JavaSetMousePointForNative = (*JavaEnv)->GetMethodID(JavaEnv, JavaMainViewClass, "setMousePointForNative", "(II)V");
}

void SDL_ANDROID_WarpMouse(int x, int y)
{
	if(SDL_ANDROID_sFakeWindowWidth > 0 && SDL_ANDROID_sFakeWindowHeight > 0){
		int wx = x * SDL_ANDROID_sWindowWidth  / SDL_ANDROID_sFakeWindowWidth;
		int wy = y * SDL_ANDROID_sWindowHeight / SDL_ANDROID_sFakeWindowHeight;
	
		if(JavaMainView && JavaSetMousePointForNative){		
		JNIEnv *env;
		jint ret = (*g_JavaVM)->GetEnv(g_JavaVM, (void**)&env, JNI_VERSION_1_6);
		if (ret == JNI_OK) {
			(*env)->CallVoidMethod( env, JavaMainView, JavaSetMousePointForNative, wx, wy );
		}
		}
	}
};
