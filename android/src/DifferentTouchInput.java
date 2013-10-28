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
 /*
 2013/9 Modified by Martin Dieter
 */

package jp.rikku2000.sdlpal;

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

class Mouse
{
	public static final int SDL_FINGER_DOWN = 0;
	public static final int SDL_FINGER_UP = 1;
	public static final int SDL_FINGER_MOVE = 2;
	public static final int SDL_FINGER_HOVER = 3;
}

abstract class DifferentTouchInput
{
	public interface OnInputEventListener
	{
		public void onKeyEvent(int keyCode, int pressed);
		public void onMotionEvent(int x, int y, int action, int pointerId, int pressure, int radius, int touchCount);
		public void onMouseButtonEvent(int buttonId, int pressed);
	}

	public abstract void process(final MotionEvent event);
	public abstract void processGenericEvent(final MotionEvent event);
	
	public static boolean ExternalMouseDetected = false;
	
	private static OnInputEventListener mOnInputEventListener = null;
	
	public static void setOnInputEventListener(OnInputEventListener listener)
	{
		mOnInputEventListener = listener;
	}
	
	public static DifferentTouchInput getInstance()
	{
		boolean multiTouchAvailable1 = false;
		boolean multiTouchAvailable2 = false;
		// Not checking for getX(int), getY(int) etc 'cause I'm lazy
		Method methods [] = MotionEvent.class.getDeclaredMethods();
		for(Method m: methods) 
		{
			if( m.getName().equals("getPointerCount") )
				multiTouchAvailable1 = true;
			if( m.getName().equals("getPointerId") )
				multiTouchAvailable2 = true;
		}
		try {
			if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH )
			{
				if(android.os.Build.MODEL.equals("GT-N7000") || android.os.Build.MODEL.equals("SGH-I717"))
					return GalaxyNoteIcsTouchInput.Holder.sInstance;
				return IcsTouchInput.Holder.sInstance;
			}
			if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD )
				return XperiaPlayTouchpadTouchInput.Holder.sInstance;
			if (multiTouchAvailable1 && multiTouchAvailable2)
				return MultiTouchInput.Holder.sInstance;
			else
				return SingleTouchInput.Holder.sInstance;
		} catch( Exception e ) {
			try {
				if (multiTouchAvailable1 && multiTouchAvailable2)
					return MultiTouchInput.Holder.sInstance;
				else
					return SingleTouchInput.Holder.sInstance;
			} catch( Exception ee ) {
				return SingleTouchInput.Holder.sInstance;
			}
		}
	}
	private static class SingleTouchInput extends DifferentTouchInput
	{
		private static class Holder
		{
			private static final SingleTouchInput sInstance = new SingleTouchInput();
		}
		@Override
		public void processGenericEvent(final MotionEvent event)
		{
			process(event);
		}
		public void process(final MotionEvent event)
		{
			int action = -1;
			if( event.getAction() == MotionEvent.ACTION_DOWN )
				action = Mouse.SDL_FINGER_DOWN;
			if( event.getAction() == MotionEvent.ACTION_UP )
				action = Mouse.SDL_FINGER_UP;
			if( event.getAction() == MotionEvent.ACTION_MOVE )
				action = Mouse.SDL_FINGER_MOVE;
			if ( action >= 0 ){
				if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( (int)event.getX(), (int)event.getY(), action, 0, (int)(event.getPressure() * 1000.0), (int)(event.getSize() * 1000.0), 1 );
			}
		}
	}
	private static class MultiTouchInput extends DifferentTouchInput
	{
		public static final int TOUCH_EVENTS_MAX = 16; // Max multitouch pointers
		
		private class touchEvent
		{
			public boolean down = false;
			public int x = 0;
			public int y = 0;
			public int pressure = 0;
			public int size = 0;
		}
		
		protected touchEvent touchEvents[];
		
		MultiTouchInput()
		{
			touchEvents = new touchEvent[TOUCH_EVENTS_MAX];
			for( int i = 0; i < TOUCH_EVENTS_MAX; i++ )
				touchEvents[i] = new touchEvent();
		}
		
		private static class Holder
		{
			private static final MultiTouchInput sInstance = new MultiTouchInput();
		}
		
		public void processGenericEvent(final MotionEvent event)
		{
			process(event);
		}
		public void process(final MotionEvent event)
		{
			int action = -1;
			
			//System.out.println("Got motion event, type " + (int)(event.getAction()) + " X " + (int)event.getX() + " Y " + (int)event.getY());
			if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP ||
			   (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_CANCEL )
			{
				action = Mouse.SDL_FINGER_UP;
				for( int i = 0; i < TOUCH_EVENTS_MAX; i++ )
				{
					if( touchEvents[i].down )
					{
						touchEvents[i].down = false;
						if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( touchEvents[i].x, touchEvents[i].y, action, i, touchEvents[i].pressure, touchEvents[i].size, event.getPointerCount() );
					}
				}
			}
			if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN )
			{
				action = Mouse.SDL_FINGER_DOWN;
				for( int i = 0; i < event.getPointerCount(); i++ )
				{
					int id = event.getPointerId(i);
					if( id >= TOUCH_EVENTS_MAX )
						id = TOUCH_EVENTS_MAX - 1;
					touchEvents[id].down = true;
					touchEvents[id].x = (int)event.getX(i);
					touchEvents[id].y = (int)event.getY(i);
					touchEvents[id].pressure = (int)(event.getPressure(i) * 1000.0);
					touchEvents[id].size = (int)(event.getSize(i) * 1000.0);
					if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( touchEvents[id].x, touchEvents[id].y, action, id, touchEvents[id].pressure, touchEvents[id].size, event.getPointerCount() );
				}
			}
			if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE ||
			   (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN ||
			   (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP )
			{
				/*
				 String s = "MOVE: ptrs " + event.getPointerCount();
				 for( int i = 0 ; i < event.getPointerCount(); i++ )
				 {
				 s += " p" + event.getPointerId(i) + "=" + (int)event.getX(i) + ":" + (int)event.getY(i);
				 }
				 System.out.println(s);
				 */
				int pointerReleased = -1;
				if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP )
					pointerReleased = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				
				for( int id = 0; id < TOUCH_EVENTS_MAX; id++ )
				{
					int ii;
					for( ii = 0; ii < event.getPointerCount(); ii++ )
					{
						if( id == event.getPointerId(ii) )
							break;
					}
					if( ii >= event.getPointerCount() )
					{
						// Up event
						if( touchEvents[id].down )
						{
							action = Mouse.SDL_FINGER_UP;
							touchEvents[id].down = false;
							if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( touchEvents[id].x, touchEvents[id].y, action, id, touchEvents[id].pressure, touchEvents[id].size, event.getPointerCount() );
						}
					}
					else
					{
						if( pointerReleased == id && touchEvents[pointerReleased].down )
						{
							action = Mouse.SDL_FINGER_UP;
							touchEvents[id].down = false;
						}
						else if( touchEvents[id].down )
						{
							action = Mouse.SDL_FINGER_MOVE;
						}
						else
						{
							action = Mouse.SDL_FINGER_DOWN;
							touchEvents[id].down = true;
						}
						touchEvents[id].x = (int)event.getX(ii);
						touchEvents[id].y = (int)event.getY(ii);
						touchEvents[id].pressure = (int)(event.getPressure(ii) * 1000.0);
						touchEvents[id].size = (int)(event.getSize(ii) * 1000.0);
						if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( touchEvents[id].x, touchEvents[id].y, action, id, touchEvents[id].pressure, touchEvents[id].size, event.getPointerCount() );
					}
				}
			}
			if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_HOVER_MOVE ) // Support bluetooth/USB mouse - available since Android 3.1
			{
				// TODO: it is possible that multiple pointers return that event, but we're handling only pointer #0
				if( touchEvents[0].down )
					action = Mouse.SDL_FINGER_UP;
				else
					action = Mouse.SDL_FINGER_HOVER;
				touchEvents[0].down = false;
				touchEvents[0].x = (int)event.getX();
				touchEvents[0].y = (int)event.getY();
				touchEvents[0].pressure = 0;
				touchEvents[0].size = 0;
				if(mOnInputEventListener != null) mOnInputEventListener.onMotionEvent( touchEvents[0].x, touchEvents[0].y, action, 0, touchEvents[0].pressure, touchEvents[0].size, event.getPointerCount() );
			}
		}
	}
	private static class XperiaPlayTouchpadTouchInput extends MultiTouchInput
	{
		private static class Holder
		{
			private static final XperiaPlayTouchpadTouchInput sInstance = new XperiaPlayTouchpadTouchInput();
		}
		
		float xmin = 0.0f;
		float xmax = 1.0f;
		float ymin = 0.0f;
		float ymax = 1.0f;
		float minRange = 1.0f;
		float xshift = 0.0f;
		
		XperiaPlayTouchpadTouchInput()
		{
			super();
			int[] devIds = InputDevice.getDeviceIds();
			for( int id : devIds )
			{
				InputDevice device = InputDevice.getDevice(id);
				if( device == null )
					continue;
				System.out.println("Engine: input device ID " + id + " type " + device.getSources()  + " name " + device.getName() );
				if( (device.getSources() & InputDevice.SOURCE_TOUCHPAD) != InputDevice.SOURCE_TOUCHPAD )
					continue;
				System.out.println("Engine: input device ID " + id + " type " + device.getSources()  + " name " + device.getName() + " is a touchpad" );
				InputDevice.MotionRange range = device.getMotionRange(MotionEvent.AXIS_X /*, InputDevice.SOURCE_TOUCHPAD*/);
				if(range != null)
				{
					xmin = range.getMin();
					xmax = range.getMax() - range.getMin();
					System.out.println("Engine: touch pad X range " + xmin + ":" + xmax );
				}
				range = device.getMotionRange(MotionEvent.AXIS_Y /*, InputDevice.SOURCE_TOUCHPAD*/);
				if(range != null)
				{
					ymin = range.getMin();
					ymax = range.getMax() - range.getMin();
					System.out.println("Engine: touch pad Y range " + ymin + ":" + ymax );
				}
				// Xperia Play has long wide touchpad with joystick-like embossing on the sides, so we'll leave only a left joystick to function
				// I don't know how to use the second joystick, so I'll just ignore it for now
				minRange = Math.min( Math.abs(ymax - ymin), Math.abs(xmax - xmin) );
				xshift = xmax - minRange;
			}
		}
		public void process(final MotionEvent event)
		{
			boolean hwMouseEvent = (	event.getSource() == InputDevice.SOURCE_MOUSE ||
									event.getSource() == InputDevice.SOURCE_STYLUS ||
									(event.getMetaState() & KeyEvent.FLAG_TRACKING) != 0 ); // Hack to recognize Galaxy Note Gingerbread stylus
			if( ExternalMouseDetected != hwMouseEvent )
			{
				ExternalMouseDetected = hwMouseEvent;
				//DemoGLSurfaceView.nativeHardwareMouseDetected(hwMouseEvent ? 1 : 0);
			}
			super.process(event);
		}
		public void processGenericEvent(final MotionEvent event)
		{
			if( event.getSource() != InputDevice.SOURCE_TOUCHPAD )
			{
				process(event);
				return;
			}
			/*
			 int x = (int)((event.getX() - xmin) / xmax * 65535.0f);
			 int y = (int)((event.getY() - ymin) / ymax * 65535.0f);
			 */
			// Use only right square part of a touch surface - I've heard reports that it breaks functionality, feel free to uncomment and test it.
			int x = (int)((event.getX() - xshift) / minRange * 65535.0f);
			int y = (int)((event.getY() - ymin) / minRange * 65535.0f);
			if( x > 65535 )
				x = 65535;
			if( x < 0 )
				x = 0;
			if( y > 65535 )
				y = 65535;
			if( y < 0 )
				y = 0;
			int down = 1;
			int multitouch = event.getPointerCount() - 1;
			if( (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP ||
			   (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_CANCEL )
				down = 0;
			// TODO: we're processing only one touch pointer, touchpad will most probably support multitouch
			//System.out.println("Engine: touch pad event: " + x + ":" + y + " action " + event.getAction() + " down " + down + " multitouch " + multitouch );
			
			//DemoGLSurfaceView.nativeTouchpad( x, 65535 - y, down, multitouch ); // Y axis is inverted, as you may have guessed
		}
	}
	private static class IcsTouchInput extends XperiaPlayTouchpadTouchInput
	{
		private static class Holder
		{
			private static final IcsTouchInput sInstance = new IcsTouchInput();
		}
		private int buttonState = 0;
		public void process(final MotionEvent event)
		{
			//System.out.println("Got motion event, type " + (int)(event.getAction()) + " X " + (int)event.getX() + " Y " + (int)event.getY() + " buttons " + buttonState + " source " + event.getSource());
			super.process(event); // Push mouse coordinate first
			int buttonStateNew = event.getButtonState();
			if( buttonStateNew != buttonState )
			{
				for( int i = 1; i <= MotionEvent.BUTTON_FORWARD; i *= 2 )
				{
					if( (buttonStateNew & i) != (buttonState & i) )
						if(mOnInputEventListener != null) mOnInputEventListener.onMouseButtonEvent(i, ((buttonStateNew & i) == 0) ? 0 : 1);
				}
				buttonState = buttonStateNew;
			}
		}
	}
	private static class GalaxyNoteIcsTouchInput extends IcsTouchInput
	{
		private static class Holder
		{
			private static final GalaxyNoteIcsTouchInput sInstance = new GalaxyNoteIcsTouchInput();
		}
		public void process(final MotionEvent event)
		{
			// HACK for Galaxy Note stylus, which pushes the cursor to the lower-right part of the screen, when you lift the stylus.
			// Also it reports the stylus as the mouse
			if(! (event.getSource() == InputDevice.SOURCE_MOUSE && (int)event.getX() == 0 && (int)event.getY() == 799))
				super.process(event);
		}
	}
}
