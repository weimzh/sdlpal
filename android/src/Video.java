/*
Simple DirectMedia Layer
Java source code (C) 2009-2011 Sergii Pylypenko
  
This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
  
1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required. 
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

package com.codeplex.sdlpal;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.Window;
import android.view.WindowManager;
import android.os.Environment;
import java.io.File;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.content.res.Resources;
import android.content.res.AssetManager;
import android.widget.Toast;

import android.widget.TextView;
import java.lang.Thread;
import java.util.concurrent.locks.ReentrantLock;
import android.os.Build;
import java.lang.reflect.Method;
import java.util.LinkedList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

class DemoRenderer extends GLSurfaceView_SDL.Renderer
{	
	public DemoRenderer(MainActivity _context)
	{
		context = _context;
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.i("Engine","Engine: DemoRenderer.onSurfaceCreated(): paused " + mPaused + " mFirstTimeStart " + mFirstTimeStart );
		mGlSurfaceCreated = true;
		mGl = gl;
		if( ! mPaused && ! mFirstTimeStart )
			nativeGlContextRecreated();
		mFirstTimeStart = false;
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		Log.i("Engine","Engine: DemoRenderer.onSurfaceChanged(): paused " + mPaused + " mFirstTimeStart " + mFirstTimeStart );
		mWidth = w;
		mHeight = h;
		mGl = gl;
		nativeResize(w, h, 0);
	}
	
	public void onSurfaceDestroyed() {
		Log.i("Engine","Engine: DemoRenderer.onSurfaceDestroyed(): paused " + mPaused + " mFirstTimeStart " + mFirstTimeStart );
		mGlSurfaceCreated = false;
		mGlContextLost = true;
		nativeGlContextLost();
	}

	public void onDrawFrame(GL10 gl) {

		mGl = gl;
		//DrawLogo(mGl);
		SwapBuffers();

		nativeInitJavaCallbacks();
		
		// Make main thread priority lower so audio thread won't get underrun
		// Thread.currentThread().setPriority((Thread.currentThread().getPriority() + Thread.MIN_PRIORITY)/2);
		
		mGlContextLost = false;

		Settings.Apply(context);
		//accelerometer = new AccelerometerReader(context);
		// Tweak video thread priority, if user selected big audio buffer
		if(Globals.AUDIO_BUFFER_CONFIG >= 2)
			Thread.currentThread().setPriority( (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2 ); // Lower than normal
		 // Calls main() and never returns, hehe - we'll call eglSwapBuffers() from native code
		
		String commandLine = Locals.AppModuleName;
		if(Locals.AppCommandOptions != null && !Locals.AppCommandOptions.equals("")){
			commandLine += " " + Locals.AppCommandOptions;
		}
		
		nativeInit( Globals.CurrentDirectoryPath, commandLine );
		
		System.exit(0); // The main() returns here - I don't bother with deinit stuff, just terminate process
	}

	public int swapBuffers() // Called from native code
	{
		if( ! super.SwapBuffers() && Globals.VIDEO_NON_BLOCKING_SWAP_BUFFERS )
		{
			synchronized(this)
			{
				this.notify();
			}
			return 0;
		}

		if(mGlContextLost) {
			mGlContextLost = false;
			//Settings.SetupTouchscreenKeyboardGraphics(context); // Reload on-screen buttons graphics
			//DrawLogo(mGl);
			super.SwapBuffers();
		}

		// Unblock event processing thread only after we've finished rendering
		synchronized(this)
		{
			this.notify();
		}
		return 1;
	}

	public void exitApp()
	{
		 nativeDone();
	}
	
	//

	private native void nativeInitJavaCallbacks();
	private native void nativeInit(String currentPath, String commandLine);
	private native void nativeResize(int w, int h, int keepAspectRatio);
	private native void nativeDone();
	private native void nativeGlContextLost();
	public native void nativeGlContextRecreated();

	private MainActivity context = null;
	
	private GL10 mGl = null;
	private EGL10 mEgl = null;
	private EGLDisplay mEglDisplay = null;
	private EGLSurface mEglSurface = null;
	private EGLContext mEglContext = null;
	private boolean mGlContextLost = false;
	public boolean mGlSurfaceCreated = false;
	public boolean mPaused = false;
	private boolean mFirstTimeStart = true;
	public int mWidth = 0;
	public int mHeight = 0;
}

class DemoGLSurfaceView extends GLSurfaceView_SDL {
	public DemoGLSurfaceView(MainActivity context) {
		super(context);
		
		setEGLConfigChooser(Locals.VideoDepthBpp, Globals.VIDEO_NEED_DEPTH_BUFFER, Globals.VIDEO_NEED_STENCIL_BUFFER, Globals.VIDEO_NEED_GLES2);
		mRenderer = new DemoRenderer(context);
		setRenderer(mRenderer);
	}

	public void limitEventRate(final MotionEvent event)
	{
		// Wait a bit, and try to synchronize to app framerate, or event thread will eat all CPU and we'll lose FPS
		// With Froyo the rate of touch events seems to be limited by OS, but they are arriving faster then we're redrawing anyway
		if((event.getAction() == MotionEvent.ACTION_MOVE ||
			event.getAction() == MotionEvent.ACTION_HOVER_MOVE))
		{
			synchronized(mRenderer)
			{
				try
				{
					mRenderer.wait(300L); // And sometimes the app decides not to render at all, so this timeout should not be big.
				} catch (InterruptedException e) { }
			}
		}
	}


	public void exitApp() {
		mRenderer.exitApp();
	};

	@Override
	public void onPause() {
		//if( mRenderer.accelerometer != null ) // For some reason it crashes here often - are we getting this event before initialization?
		//	mRenderer.accelerometer.stop();
		Log.i("Engine","Engine: DemoGLSurfaceView.onPause():");
		super.onPause();
		mRenderer.mPaused = true;
		nativeRequestUpdateSurface();
	};
	
	public boolean isPaused() {
		return mRenderer.mPaused;
	}

	@Override
	public void onResume() {
		super.onResume();
		mRenderer.mPaused = false;
		Log.i("Engine","Engine: DemoGLSurfaceView.onResume(): mRenderer.mGlSurfaceCreated " + mRenderer.mGlSurfaceCreated + " mRenderer.mPaused " + mRenderer.mPaused);
		if( mRenderer.mGlSurfaceCreated && ! mRenderer.mPaused || Globals.VIDEO_NON_BLOCKING_SWAP_BUFFERS )
			mRenderer.nativeGlContextRecreated();
		//if( mRenderer.accelerometer != null ) // For some reason it crashes here often - are we getting this event before initialization?
		//	mRenderer.accelerometer.start();
	};

	DemoRenderer mRenderer;

	public static native int  nativeKey( int keyCode, int down );
	public static native void nativeMotionEvent( int x, int y );
	public static native void nativeMouseButtonsPressed( int buttonId, int pressedState );
	public static native void nativeRequestUpdateSurface();
}


