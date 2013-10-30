/*
 2012/7 Created by AKIZUKI Katane
 */

package com.codeplex.sdlpal;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;
import android.util.Log;
import android.content.pm.ActivityInfo;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Set;
import android.app.AlertDialog;
//import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.InputDevice;

public class MainView extends AbsoluteLayout
{
	public MainView(MainActivity context)
	{
		super(context);
		
		mActivity = context;
		setBackgroundColor(Color.BLACK);
		setScreenOrientation(Locals.ScreenOrientation);

		//

		mAudioThread = new AudioThread(this);
		
		mGLView = new DemoGLSurfaceView(context);
		this.addView(mGLView);

		mMouseCursor = new MouseCursor(context);
		this.addView(mMouseCursor);
		showMouseCursor(Globals.MouseCursorShowed);
		
		int vw = mDisplayWidth;
		int vh = mDisplayHeight;
		if(Locals.VideoXRatio > 0 && Locals.VideoYRatio > 0){
			if(vw * Locals.VideoYRatio > vh * Locals.VideoXRatio){
				vw = vh * Locals.VideoXRatio / Locals.VideoYRatio;
			} else {
				vh = vw * Locals.VideoYRatio / Locals.VideoXRatio;
			}
		}
		
		setGLViewRect(0, 0, vw, vh);
		
		mTouchMode = TouchMode.getTouchMode(Locals.TouchMode, this);
		mTouchMode.setup();
		mTouchMode.update();
		
		mTouchInput = DifferentTouchInput.getInstance();
		mTouchInput.setOnInputEventListener(mTouchMode);
		
		nativeInitInputJavaCallbacks();
	}
	
	protected void onPause()
	{
		_isPaused = true;
		mGLView.onPause();
	}
	
	protected void onResume()
	{
		mGLView.onResume();
		DimSystemStatusBar.get().dim(this);
		DimSystemStatusBar.get().dim(mGLView);
		_isPaused = false;
	}
	
	public boolean isPaused()
	{
		return _isPaused;
	}
	
	public void exitApp()
	{
		mGLView.exitApp();
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) 
	{
		mTouchInput.process(event);
		//mGLView.limitEventRate(event);

		return true;
	};
	
	private int mJoyStickState    = 0;
	private int mJoyStickHatState = 0;
	
	@Override
	public boolean onGenericMotionEvent (final MotionEvent event)
	{
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD){
			if(AndroidGingerBreadAPI.MotionEventGetSource(event) == InputDevice.SOURCE_JOYSTICK){
				final float BORDER = 0.5f;
				int state, diff;
				
				//AXIS
				
				float x = AndroidGingerBreadAPI.MotionEventGetAxisValue( event, MotionEvent.AXIS_X );
				float y = AndroidGingerBreadAPI.MotionEventGetAxisValue( event, MotionEvent.AXIS_Y );
				
				state = 0;
				if( x < -BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_LEFT_INDEX;
				} else if( x > BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_RIGHT_INDEX;
				}
				if( y < -BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_UP_INDEX;
				} else if( y > BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_DOWN_INDEX;
				}
				
				diff = state ^ mJoyStickState;
				mJoyStickState = state;
				
				for(int i = 0; diff != 0 ; diff>>= 1, state>>= 1, i ++){
					if( (diff & 1) != 0 ){
						if(mTouchMode != null){
							mTouchMode.onKeyEvent(Globals.JOYSTICK_AXIS_KEY_ARRAY[i], ((state & 1) != 0 ? 1 : 0));
						}
					}
				}
				
				//AXIS_HAT
				
				float hatx = AndroidGingerBreadAPI.MotionEventGetAxisValue( event, MotionEvent.AXIS_HAT_X );
				float haty = AndroidGingerBreadAPI.MotionEventGetAxisValue( event, MotionEvent.AXIS_HAT_Y );
				
				state = 0;
				if( hatx < -BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_LEFT_INDEX;
				} else if( hatx > BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_RIGHT_INDEX;
				}
				if( haty < -BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_UP_INDEX;
				} else if( haty > BORDER ){
					state |= 1 << Globals.JOYSTICK_AXIS_DOWN_INDEX;
				}
				
				diff = state ^ mJoyStickHatState;
				mJoyStickHatState = state;
				
				for(int i = 0; diff != 0 ; diff>>= 1, state>>= 1, i ++){
					if( (diff & 1) != 0 ){
						if(mTouchMode != null){
							mTouchMode.onKeyEvent(Globals.JOYSTICK_AXISHAT_KEY_ARRAY[i], ((state & 1) != 0 ? 1 : 0));
						}
					}
				}
				return true;
			}
		}
		
		mTouchInput.processGenericEvent(event);
		//mGLView.limitEventRate(event);
		
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		//Log.i("Event", "keyDown : " + keyCode);
		
		switch(keyCode){
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				return super.onKeyDown(keyCode, event);
			case KeyEvent.KEYCODE_MENU:
				if(!mIsKeyConfigMode){
					return super.onKeyDown(keyCode, event);
				}
			case KeyEvent.KEYCODE_BACK:
			default:
				if(mTouchMode != null){
					mTouchMode.onKeyEvent(keyCode, 1);
				}
				break;
		}
		
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		//Log.i("Event", "keyUp : " + keyCode);
		
		switch(keyCode){
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return super.onKeyUp(keyCode, event);
			case KeyEvent.KEYCODE_MENU:
				if(!mIsKeyConfigMode){
					return super.onKeyUp(keyCode, event);
				}
				leaveKeyConfigMode();
				break;
			case KeyEvent.KEYCODE_BACK:
			default:
				if(mTouchMode != null){
					mTouchMode.onKeyEvent(keyCode, 0);
				}
				break;
		}
		
		if(keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK){
			DimSystemStatusBar.get().dim(this);
			DimSystemStatusBar.get().dim(mGLView);
		}
		
		return true;
	}
	
	//
	
	private static final int MENU_ITEM_ID_USER_MENU_KEY_FIRST = Menu.FIRST + 100;
	private static final int MENU_ITEM_ID_USER_MENU_KEY_LAST = MENU_ITEM_ID_USER_MENU_KEY_FIRST + Globals.MENU_KEY_NUM - 1;
	
	private static final int MENU_ITEM_ID_USER_SUBMENU_KEY_FIRST = Menu.FIRST + 200;
	private static final int MENU_ITEM_ID_USER_SUBMENU_KEY_LAST = MENU_ITEM_ID_USER_SUBMENU_KEY_FIRST + Globals.SUBMENU_KEY_NUM - 1;

	private static final int MENU_ITEM_ID_TOUCHMODE_FIRST = Menu.FIRST + 300;
	private static final int MENU_ITEM_ID_TOUCHMODE_INVALID  = MENU_ITEM_ID_TOUCHMODE_FIRST;
	private static final int MENU_ITEM_ID_TOUCHMODE_TOUCH    = MENU_ITEM_ID_TOUCHMODE_FIRST + 1;
	private static final int MENU_ITEM_ID_TOUCHMODE_TRACKPAD = MENU_ITEM_ID_TOUCHMODE_FIRST + 2;
	private static final int MENU_ITEM_ID_TOUCHMODE_GAMEPAD  = MENU_ITEM_ID_TOUCHMODE_FIRST + 3;

	private static final int MENU_ITEM_ID_SETTING_KEYCONFIG = Menu.FIRST + 400;
	
	private static final int MENU_ITEM_ID_SETTING_MOUSECURSOR_FIRST = Menu.FIRST + 500;
	private static final int MENU_ITEM_ID_SETTING_MOUSECURSOR_SHOW = MENU_ITEM_ID_SETTING_MOUSECURSOR_FIRST;
	private static final int MENU_ITEM_ID_SETTING_MOUSECURSOR_HIDE = MENU_ITEM_ID_SETTING_MOUSECURSOR_FIRST + 1;
	
	private static final int MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST = Menu.FIRST + 600;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_LEFT = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_CENTER = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST + 1;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_RIGHT = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST + 2;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_TOP = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST + 3;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_CENTER = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST + 4;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_BOTTOM = MENU_ITEM_ID_SETTING_VIDEO_POSITION_FIRST + 5;

	private static final int MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST = Menu.FIRST + 620;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_LAST  = MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST + 12;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST = Menu.FIRST + 640;
	private static final int MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_LAST  = MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST + 12;
	
	private static final int MENU_ITEM_ID_SETTING_BUTTON_LEFT_FIRST = Menu.FIRST + 700;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_ENABLE  = MENU_ITEM_ID_SETTING_BUTTON_LEFT_FIRST;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_DISABLE = MENU_ITEM_ID_SETTING_BUTTON_LEFT_FIRST + 1;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_FIRST = MENU_ITEM_ID_SETTING_BUTTON_LEFT_FIRST + 10;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_LAST  = MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_FIRST + Globals.BUTTON_LEFT_MAX;
	
	private static final int MENU_ITEM_ID_SETTING_BUTTON_RIGHT_FIRST = Menu.FIRST + 800;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_ENABLE  = MENU_ITEM_ID_SETTING_BUTTON_RIGHT_FIRST;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_DISABLE = MENU_ITEM_ID_SETTING_BUTTON_RIGHT_FIRST + 1;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_FIRST = MENU_ITEM_ID_SETTING_BUTTON_RIGHT_FIRST + 10;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_LAST  = MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_FIRST + Globals.BUTTON_RIGHT_MAX;
	
	private static final int MENU_ITEM_ID_SETTING_BUTTON_TOP_FIRST = Menu.FIRST + 900;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_ENABLE  = MENU_ITEM_ID_SETTING_BUTTON_TOP_FIRST;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_DISABLE = MENU_ITEM_ID_SETTING_BUTTON_TOP_FIRST + 1;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_FIRST = MENU_ITEM_ID_SETTING_BUTTON_TOP_FIRST + 10;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_LAST  = MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_FIRST + Globals.BUTTON_TOP_MAX;
	
	private static final int MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_FIRST = Menu.FIRST + 1000;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_ENABLE  = MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_FIRST;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_DISABLE = MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_FIRST + 1;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_FIRST = MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_FIRST + 10;
	private static final int MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_LAST  = MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_FIRST + Globals.BUTTON_BOTTOM_MAX;

	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST = Menu.FIRST + 1100;
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_LAST  = MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST + 10;
	
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST = Menu.FIRST + 1120;
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_LAST  = MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST + 10;
	
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST = Menu.FIRST + 1140;
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_LAST  = MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST + 10;
	
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_FIRST     = Menu.FIRST + 1160;
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_AXIS   = MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_FIRST;
	private static final int MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_BUTTON = MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_FIRST + 1;
	
	private static final int MENU_ITEM_ID_SETTING_APPCONFIG_USE = Menu.FIRST + 9990;
	private static final int MENU_ITEM_ID_SETTING_APPCONFIG_NOTUSE = Menu.FIRST + 9991;
	
	private static final int MENU_ITEM_ID_ABOUT = Menu.FIRST + 9998;
	private static final int MENU_ITEM_ID_QUIT = Menu.FIRST + 9999;
	
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		menu.clear();
		
		for(int i = 0; i < Globals.MENU_KEY_NUM; i ++){
			Integer sdlKey = (Integer)Globals.SDLKeyAdditionalKeyMap.get(new Integer(Globals.MENU_KEY_ARRAY[i]));
			if(sdlKey != null && sdlKey.intValue() != SDL_1_2_Keycodes.SDLK_UNKNOWN){
				String name = Globals.SDLKeyFunctionNameMap.get(sdlKey);
				if(name != null){
					menu.add(Menu.NONE, (MENU_ITEM_ID_USER_MENU_KEY_FIRST+i), Menu.NONE, name);
				}
			}
		}
		
		//if(Globals.SUBMENU_KEY_NUM > 0){
			SubMenu menu_userfunc = menu.addSubMenu(getResources().getString(R.string.function));
			for(int i = 0; i < Globals.SUBMENU_KEY_NUM; i ++){
				Integer sdlKey = (Integer)Globals.SDLKeyAdditionalKeyMap.get(new Integer(Globals.SUBMENU_KEY_ARRAY[i]));
				if(sdlKey != null && sdlKey.intValue() != SDL_1_2_Keycodes.SDLK_UNKNOWN){
					String name = Globals.SDLKeyFunctionNameMap.get(sdlKey);
					if(name != null){
						menu_userfunc.add(Menu.NONE, (MENU_ITEM_ID_USER_SUBMENU_KEY_FIRST+i), Menu.NONE, name);
					}
				}
			}
		//}
		
		SubMenu menu_touchmode = menu.addSubMenu(getResources().getString(R.string.touch_mode));
		menu_touchmode.add(Menu.NONE, MENU_ITEM_ID_TOUCHMODE_INVALID, Menu.NONE, getResources().getString(R.string.touch_mode_invalid));
		if(Globals.MOUSE_USE){
			menu_touchmode.add(Menu.NONE, MENU_ITEM_ID_TOUCHMODE_TOUCH, Menu.NONE, getResources().getString(R.string.touch_mode_touch));
			menu_touchmode.add(Menu.NONE, MENU_ITEM_ID_TOUCHMODE_TRACKPAD, Menu.NONE, getResources().getString(R.string.touch_mode_trackpad));
		}
		menu_touchmode.add(Menu.NONE, MENU_ITEM_ID_TOUCHMODE_GAMEPAD, Menu.NONE, getResources().getString(R.string.touch_mode_gamepad));

		menu.add(Menu.NONE, (MENU_ITEM_ID_QUIT), Menu.NONE, getResources().getString(R.string.quit));
		
		//
		
		//menu.add(Menu.NONE, (MENU_ITEM_ID_ABOUT), Menu.NONE, "About");

		menu.add(Menu.NONE, MENU_ITEM_ID_SETTING_KEYCONFIG, Menu.NONE, getResources().getString(R.string.key_config));
		
		SubMenu menu_mousecursor = menu.addSubMenu(getResources().getString(R.string.mouse_cursor));
		menu_mousecursor.add(Menu.NONE, MENU_ITEM_ID_SETTING_MOUSECURSOR_SHOW, Menu.NONE, getResources().getString(R.string.show));
		menu_mousecursor.add(Menu.NONE, MENU_ITEM_ID_SETTING_MOUSECURSOR_HIDE, Menu.NONE, getResources().getString(R.string.hide));
		
		if(Globals.BUTTON_USE){
			SubMenu menu_btn_left = menu.addSubMenu(getResources().getString(R.string.button_left));
			if(Globals.ButtonLeftEnabled){
				menu_btn_left.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_DISABLE, Menu.NONE, getResources().getString(R.string.disable));
				for(int i = 1; i <= Globals.BUTTON_LEFT_MAX; i ++){
					menu_btn_left.add(Menu.NONE, (MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_FIRST + i), Menu.NONE, "" + i + "unit");
				}
			} else {
				menu_btn_left.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_ENABLE, Menu.NONE, getResources().getString(R.string.enable));
			}
			
			SubMenu menu_btn_right = menu.addSubMenu(getResources().getString(R.string.button_right));
			if(Globals.ButtonRightEnabled){
				menu_btn_right.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_DISABLE, Menu.NONE, getResources().getString(R.string.disable));
				for(int i = 1; i <= Globals.BUTTON_RIGHT_MAX; i ++){
					menu_btn_right.add(Menu.NONE, (MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_FIRST + i), Menu.NONE, "" + i + " unit");
				}
			} else {
				menu_btn_right.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_ENABLE, Menu.NONE, getResources().getString(R.string.enable));
			}
			
			SubMenu menu_btn_top = menu.addSubMenu(getResources().getString(R.string.button_top));
			if(Globals.ButtonTopEnabled){
				menu_btn_top.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_DISABLE, Menu.NONE, getResources().getString(R.string.disable));
				for(int i = 1; i <= Globals.BUTTON_TOP_MAX; i ++){
					menu_btn_top.add(Menu.NONE, (MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_FIRST + i), Menu.NONE, "" + i + "unit");
				}
			} else {
				menu_btn_top.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_ENABLE, Menu.NONE, getResources().getString(R.string.enable));
			}
			
			SubMenu menu_btn_bottom = menu.addSubMenu(getResources().getString(R.string.button_bottom));
			if(Globals.ButtonBottomEnabled){
				menu_btn_bottom.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_DISABLE, Menu.NONE, getResources().getString(R.string.disable));
				for(int i = 1; i <= Globals.BUTTON_BOTTOM_MAX; i ++){
					menu_btn_bottom.add(Menu.NONE, (MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_FIRST + i), Menu.NONE, "" + i + "unit");
				}
			} else {
				menu_btn_bottom.add(Menu.NONE, MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_ENABLE, Menu.NONE, getResources().getString(R.string.enable));
			}
			
			SubMenu menu_video_xmargin = menu.addSubMenu(R.string.video_x_margin);
			for(int i = 0; i <= (MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_LAST - MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST); i ++){
				menu_video_xmargin.add(Menu.NONE, (MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST + i), Menu.NONE, "" + i);
			}
			
			SubMenu menu_video_ymargin = menu.addSubMenu(getResources().getString(R.string.video_y_margin));
			for(int i = 0; i <= (MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_LAST - MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST); i ++){
				menu_video_ymargin.add(Menu.NONE, (MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST + i), Menu.NONE, "" + i);
			}
		}
		
		SubMenu menu_video_xpos = menu.addSubMenu(getResources().getString(R.string.video_x_pos));
		menu_video_xpos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_LEFT, Menu.NONE, getResources().getString(R.string.left));
		menu_video_xpos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_CENTER, Menu.NONE, getResources().getString(R.string.center));
		menu_video_xpos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_RIGHT, Menu.NONE, getResources().getString(R.string.right));
		
		SubMenu menu_video_ypos = menu.addSubMenu(getResources().getString(R.string.video_y_pos));
		menu_video_ypos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_TOP, Menu.NONE, getResources().getString(R.string.top));
		menu_video_ypos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_CENTER, Menu.NONE, getResources().getString(R.string.center));
		menu_video_ypos.add(Menu.NONE, MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_BOTTOM, Menu.NONE, getResources().getString(R.string.bottom));
		
		SubMenu menu_gamepad_position = menu.addSubMenu(getResources().getString(R.string.gamepad_pos));
		menu_gamepad_position.add(Menu.NONE, MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST, Menu.NONE, "0 (" + getResources().getString(R.string.top) + ")");
		for(int i = 1; i < (MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_LAST - MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST); i ++){
			menu_gamepad_position.add(Menu.NONE, (MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST + i), Menu.NONE, "" + (i * 10));
		}
		menu_gamepad_position.add(Menu.NONE, MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_LAST, Menu.NONE, "100 (" + getResources().getString(R.string.bottom) + ")");
		
		SubMenu menu_gamepad_size = menu.addSubMenu(getResources().getString(R.string.gamepad_size));
		for(int i = 1; i <= (MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_LAST - MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST); i ++){
			menu_gamepad_size.add(Menu.NONE, (MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST + i), Menu.NONE, "" + (i * 10) + "%");
		}
		
		SubMenu menu_gamepad_opacity = menu.addSubMenu(getResources().getString(R.string.gamepad_opacity));
		for(int i = 0; i <= (MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_LAST - MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST); i ++){
			menu_gamepad_opacity.add(Menu.NONE, (MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST + i), Menu.NONE, "" + (i * 10) + "%");
		}
		
		SubMenu menu_gamepad_arrow_button = menu.addSubMenu(getResources().getString(R.string.gamepad_arrow_button));
		menu_gamepad_arrow_button.add(Menu.NONE, MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_AXIS, Menu.NONE, getResources().getString(R.string.as_axis));
		menu_gamepad_arrow_button.add(Menu.NONE, MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_AXIS, Menu.NONE, getResources().getString(R.string.as_button));

		SubMenu menu_appconfig = menu.addSubMenu(getResources().getString(R.string.app_launch_config));
		if(Locals.AppLaunchConfigUse){
			menu_appconfig.add(Menu.NONE, MENU_ITEM_ID_SETTING_APPCONFIG_NOTUSE, Menu.NONE, getResources().getString(R.string.disable));
		} else {
			menu_appconfig.add(Menu.NONE, MENU_ITEM_ID_SETTING_APPCONFIG_USE, Menu.NONE, getResources().getString(R.string.enable));
		}
		
		return true;
	}
	
	public boolean onOptionsItemSelected( MenuItem item )
	{
		int d = item.getItemId();
		
		//Log.i("MainView","onOptionsItemSelected : " + d);
		
		if(d >= (MENU_ITEM_ID_USER_MENU_KEY_FIRST) && d <= (MENU_ITEM_ID_USER_MENU_KEY_LAST)){
			int index = d - MENU_ITEM_ID_USER_MENU_KEY_FIRST;
			int key = Globals.MENU_KEY_ARRAY[index];
			nativeKey( key, 1 );
			nativeKey( key, 0 );
		} else if(d >= (MENU_ITEM_ID_USER_SUBMENU_KEY_FIRST) && d <= (MENU_ITEM_ID_USER_SUBMENU_KEY_LAST)){
			int index = d - MENU_ITEM_ID_USER_SUBMENU_KEY_FIRST;
			int key = Globals.SUBMENU_KEY_ARRAY[index];
			nativeKey( key, 1 );
			nativeKey( key, 0 );
		} else if(d == MENU_ITEM_ID_TOUCHMODE_INVALID){
			if(mTouchMode != null){
				mTouchMode.cleanup();
			}
			Locals.TouchMode = "Invalid";
			Settings.SaveLocals(mActivity);
			mTouchMode = TouchMode.getTouchMode(Locals.TouchMode, this);
			mTouchMode.setup();
			mTouchMode.update();
			mTouchInput.setOnInputEventListener(mTouchMode);
		} else if(d == MENU_ITEM_ID_TOUCHMODE_TOUCH){
			if(mTouchMode != null){
				mTouchMode.cleanup();
			}
			Locals.TouchMode = "Touch";
			Settings.SaveLocals(mActivity);
			mTouchMode = TouchMode.getTouchMode(Locals.TouchMode, this);
			mTouchMode.setup();
			mTouchMode.update();
			mTouchInput.setOnInputEventListener(mTouchMode);
		} else if(d == MENU_ITEM_ID_TOUCHMODE_TRACKPAD){
			if(mTouchMode != null){
				mTouchMode.cleanup();
			}
			Locals.TouchMode = "TrackPad";
			Settings.SaveLocals(mActivity);
			mTouchMode = TouchMode.getTouchMode(Locals.TouchMode, this);
			mTouchMode.setup();
			mTouchMode.update();
			mTouchInput.setOnInputEventListener(mTouchMode);
		} else if(d == MENU_ITEM_ID_TOUCHMODE_GAMEPAD){
			if(mTouchMode != null){
				mTouchMode.cleanup();
			}
			Locals.TouchMode = "GamePad";
			Settings.SaveLocals(mActivity);
			mTouchMode = TouchMode.getTouchMode(Locals.TouchMode, this);
			mTouchMode.setup();
			mTouchMode.update();
			mTouchInput.setOnInputEventListener(mTouchMode);
		} else if(d == MENU_ITEM_ID_SETTING_KEYCONFIG){
			enterKeyConfigMode();
		} else if(d == MENU_ITEM_ID_SETTING_MOUSECURSOR_SHOW){
			Globals.MouseCursorShowed = true;
			Settings.SaveGlobals(mActivity);
			showMouseCursor(Globals.MouseCursorShowed);
		} else if(d == MENU_ITEM_ID_SETTING_MOUSECURSOR_HIDE){
			Globals.MouseCursorShowed = false;
			Settings.SaveGlobals(mActivity);
			showMouseCursor(Globals.MouseCursorShowed);
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_LEFT){
			Locals.VideoXPosition = -1;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_CENTER){
			Locals.VideoXPosition = 0;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_XPOSITION_RIGHT){
			Locals.VideoXPosition = 1;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_TOP){
			Locals.VideoYPosition = -1;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_CENTER){
			Locals.VideoYPosition = 0;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_VIDEO_YPOSITION_BOTTOM){
			Locals.VideoYPosition = 1;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST && d <= MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_LAST){
			Locals.VideoXMargin = d - MENU_ITEM_ID_SETTING_VIDEO_XMARGIN_FIRST;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST && d <= MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_LAST){
			Locals.VideoYMargin = d - MENU_ITEM_ID_SETTING_VIDEO_YMARGIN_FIRST;
			Settings.SaveLocals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_ENABLE){
			Globals.ButtonLeftEnabled = true;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_LEFT_ENABLED_DISABLE){
			Globals.ButtonLeftEnabled = false;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_FIRST && d <= MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_LAST){
			Globals.ButtonLeftNum = d - MENU_ITEM_ID_SETTING_BUTTON_LEFT_NUM_FIRST;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_ENABLE){
			Globals.ButtonRightEnabled = true;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_RIGHT_ENABLED_DISABLE){
			Globals.ButtonRightEnabled = false;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_FIRST && d <= MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_LAST){
			Globals.ButtonRightNum = d - MENU_ITEM_ID_SETTING_BUTTON_RIGHT_NUM_FIRST;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_ENABLE){
			Globals.ButtonTopEnabled = true;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_TOP_ENABLED_DISABLE){
			Globals.ButtonTopEnabled = false;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_FIRST && d <= MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_LAST){
			Globals.ButtonTopNum = d - MENU_ITEM_ID_SETTING_BUTTON_TOP_NUM_FIRST;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_ENABLE){
			Globals.ButtonBottomEnabled = true;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_ENABLED_DISABLE){
			Globals.ButtonBottomEnabled = false;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_FIRST && d <= MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_LAST){
			Globals.ButtonBottomNum = d - MENU_ITEM_ID_SETTING_BUTTON_BOTTOM_NUM_FIRST;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST && d <= MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_LAST){
			Globals.GamePadPosition = (d - MENU_ITEM_ID_SETTING_GAMEPAD_POSITION_FIRST) * 10;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST && d <= MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_LAST){
			Globals.GamePadSize = (d - MENU_ITEM_ID_SETTING_GAMEPAD_SIZE_FIRST) * 10;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d >= MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST && d <= MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_LAST){
			Globals.GamePadOpacity = (d - MENU_ITEM_ID_SETTING_GAMEPAD_OPACITY_FIRST) * 10;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_AXIS){
			Globals.GamePadArrowButtonAsAxis = true;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_GAMEPAD_ARROW_BUTTON_AS_BUTTON){
			Globals.GamePadArrowButtonAsAxis = false;
			Settings.SaveGlobals(mActivity);
			update();
		} else if(d == MENU_ITEM_ID_SETTING_APPCONFIG_USE){
			Locals.AppLaunchConfigUse = true;
			Settings.SaveLocals(mActivity);
		} else if(d == MENU_ITEM_ID_SETTING_APPCONFIG_NOTUSE){
			Locals.AppLaunchConfigUse = false;
			Settings.SaveLocals(mActivity);
		} else if(d == MENU_ITEM_ID_ABOUT){
			//
		} else if(d == MENU_ITEM_ID_QUIT){
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
			alertDialogBuilder.setTitle(getResources().getString(R.string.close_appli));
			alertDialogBuilder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					exitApp();
				}
			});
			alertDialogBuilder.setNegativeButton(getResources().getString(R.string.no), null);
			alertDialogBuilder.setCancelable(true);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		return true;
	}
	
	//
	
	public int getMousePointX()
	{
		return mMousePointX;
	}
	
	public int getMousePointY()
	{
		return mMousePointY;
	}
	
	public void setMousePoint(int x, int y)
	{
		mMousePointX = x;
		mMousePointY = y;
		
		updateMouseCursor();
	}
	
	public void setMousePointForNative(int x, int y) //call from native
	{
		class Callback implements Runnable
		{
			public MainView v;
			public int x;
			public int y;
			public void run()
			{
				v.setMousePoint(x, y);
			}
		}
		Callback cb = new Callback();
		cb.v = this;
		cb.x = x;
		cb.y = y;
		if(mActivity != null)
			mActivity.runOnUiThread(cb);
	}

	public boolean isMouseCursorShowed()
	{
		return (mMouseCursor.getVisibility() == View.VISIBLE);
	}
	
	public void showMouseCursor(boolean show)
	{
		if(show){
			mMouseCursor.setVisibility(View.VISIBLE);
		} else {
			mMouseCursor.setVisibility(View.GONE);
		}
	}
	
	public void setMouseCursorRGB(int fillColorR, int fillColorG, int fillColorB, int strokeColorR, int strokeColorG, int strokeColorB)
	{
		mMouseCursor.setMouseCursorRGB(fillColorR, fillColorG, fillColorB, strokeColorR, strokeColorG, strokeColorB);
	}
	
	public void updateMouseCursor()
	{
		mMouseCursor.setLayoutParams(new AbsoluteLayout.LayoutParams(MouseCursor.WIDTH, MouseCursor.HEIGHT, mGLViewX + mMousePointX, mGLViewY + mMousePointY));
		//mMouseCursor.invalidate();
	}
	
	public void update()
	{
		mTouchMode.update();
	}

	//
	
	public int getGLViewX()
	{
		return mGLViewX;
	}
	
	public int getGLViewY()
	{
		return mGLViewY;
	}
	
	public int getGLViewWidth()
	{
		return mGLViewWidth;
	}
	
	public int getGLViewHeight()
	{
		return mGLViewHeight;
	}
	
	public void setGLViewPos(int x, int y)
	{
		setGLViewRect(x, y, -1, -1);
	}
	
	public void setGLViewRect(int x, int y, int w, int h)
	{
		if(mGLView != null){
			if(w < 0){
				w = mGLViewWidth;
			}
			if(h < 0){
				h = mGLViewHeight;
			}
			
			if(mGLViewWidth > 0 && mGLViewHeight > 0){
				mMousePointX = mMousePointX * w / mGLViewWidth;
				mMousePointY = mMousePointY * h / mGLViewHeight;
			}
			
			mGLViewX = x;
			mGLViewY = y;
			mGLViewWidth  = w;
			mGLViewHeight = h;
			mGLView.setLayoutParams(new AbsoluteLayout.LayoutParams(mGLViewWidth, mGLViewHeight, mGLViewX, mGLViewY));
			
			updateMouseCursor();
			
			//Log.i("MainView", "mGLView : x,y,w,h=" + mGLViewX + "," + mGLViewY + "," + mGLViewWidth + "," + mGLViewHeight);
		}
	}
	
	public void setScreenOrientation(int orientation)
	{
		mActivity.setRequestedOrientation(orientation);
		/*
		if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
			mActivity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT );
		} else if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
			mActivity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE );
		} else {
			if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD ){
				if( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT ){
					mActivity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT );
				} else {
					mActivity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE );
				}
			} else {
				mActivity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
			}
		}
		 */

		Display disp = ((WindowManager)mActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		mDisplayWidth  = disp.getWidth();
		mDisplayHeight = disp.getHeight();
	}
	
	public int getDisplayWidth()
	{
		return mDisplayWidth;
	}
	
	public int getDisplayHeight()
	{
		return mDisplayHeight;
	}
	
	private class OnClickListenerForKeyConfigDialog implements DialogInterface.OnClickListener
	{
		private MainView mMainView;
		private int   mJavaKeyCode;
		private int[] mSdlKeyArray;
		private String[] mSdlKeyFunctionNameArray;
		
		public OnClickListenerForKeyConfigDialog(MainView mainView, int javaKeyCode, int[] sdlKeyArray, String[] sdlKeyFunctionNameArray)
		{
			mMainView = mainView;
			mJavaKeyCode = javaKeyCode;
			mSdlKeyArray = sdlKeyArray;
			mSdlKeyFunctionNameArray = sdlKeyFunctionNameArray;
		}
		
		public void onClick(DialogInterface dialog, int whichItem)
		{
			Settings.setKeymapKey(mJavaKeyCode, mSdlKeyArray[whichItem]);
			mMainView.update();
			Toast.makeText(getActivity(), "Set:" + mSdlKeyFunctionNameArray[whichItem], Toast.LENGTH_LONG).show();
		}
	}

	public int nativeKey( int keyCode, int down )
	{
		if(!mIsKeyConfigMode){
			return DemoGLSurfaceView.nativeKey( keyCode, down );
		} else if(down == 0){
			int length = Globals.SDLKeyFunctionNameMap.size();
			int[] keyArray = new int[length];
			String[] nameArray = new String[length];
			Iterator ite = Globals.SDLKeyFunctionNameMap.keySet().iterator();
			for (int i = 0; i < length && ite.hasNext(); i ++) {
				Integer key = (Integer)ite.next();
				keyArray[i] = key.intValue();
				nameArray[i] = Globals.SDLKeyFunctionNameMap.get(key);
			}
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
			alertDialogBuilder.setTitle("Select Key Function : " + keyCode);
			alertDialogBuilder.setItems(nameArray, new OnClickListenerForKeyConfigDialog(this, keyCode, keyArray, nameArray));
			alertDialogBuilder.setCancelable(true);
			alertDialogBuilder.setNegativeButton("Cancel", null);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		return 0;
	}
	
	public void nativeMotionEvent( int x, int y )
	{
		if(!mIsKeyConfigMode){
			DemoGLSurfaceView.nativeMotionEvent( x, y );
		}
	}
	
	public void nativeMouseButtonsPressed( int buttonId, int pressedState )
	{
		if(!mIsKeyConfigMode){
			DemoGLSurfaceView.nativeMouseButtonsPressed( buttonId, pressedState );
		}
	}
	
	public MainActivity getActivity()
	{
		return mActivity;
	}
	
	public void enterKeyConfigMode()
	{
		if(mIsKeyConfigMode){
			return;
		}
		
		mIsKeyConfigMode = true;
		
		mKeyConfigTextView = new TextView(getActivity());
		mKeyConfigTextView.setText(getResources().getString(R.string.key_cfg_mode));
		mKeyConfigTextView.setTextColor(Color.WHITE);
		mKeyConfigTextView.setBackgroundColor(Color.BLACK);
		
		addView(mKeyConfigTextView, new AbsoluteLayout.LayoutParams(mGLViewWidth, AbsoluteLayout.LayoutParams.WRAP_CONTENT, mGLViewX, mGLViewY));
		
		Toast.makeText(getActivity(), getResources().getString(R.string.press_key_btn), Toast.LENGTH_LONG).show();
	}
	
	public void leaveKeyConfigMode()
	{
		if(!mIsKeyConfigMode){
			return;
		}
		if(mKeyConfigTextView != null){
			removeView(mKeyConfigTextView);
			mKeyConfigTextView = null;
		}
		
		mIsKeyConfigMode = false;
		
		Settings.SaveGlobals(mActivity);
	}
	
	//
	private boolean _isPaused = false;
	
	private AudioThread mAudioThread = null;
	private DemoGLSurfaceView mGLView = null;
	
	private int mGLViewX = 0;
	private int mGLViewY = 0;
	private int mGLViewWidth  = 0;
	private int mGLViewHeight = 0;
	
	private MainActivity mActivity = null;
	private DifferentTouchInput mTouchInput = null;
	private TouchMode mTouchMode = null;
	
	private MouseCursor mMouseCursor = null;
	private int mMousePointX = 0;
	private int mMousePointY = 0;
	
	private int mDisplayWidth  = 0;
	private int mDisplayHeight = 0;
	
	private boolean mIsKeyConfigMode = false;
	private TextView mKeyConfigTextView = null;

	public native void nativeInitInputJavaCallbacks();
}

// *** HONEYCOMB / ICS FIX FOR FULLSCREEN MODE, by lmak ***
abstract class DimSystemStatusBar
{
	public static DimSystemStatusBar get()
	{
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
			return DimSystemStatusBarHoneycomb.Holder.sInstance;
		else
			return DimSystemStatusBarDummy.Holder.sInstance;
	}
	public abstract void dim(final View view);
	
	private static class DimSystemStatusBarHoneycomb extends DimSystemStatusBar
	{
		private static class Holder
		{
			private static final DimSystemStatusBarHoneycomb sInstance = new DimSystemStatusBarHoneycomb();
		}
	    public void dim(final View view)
	    {
			/*
	         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			 // ICS has the same constant redefined with a different name.
			 hiddenStatusCode = android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;
	         }
	         */
			view.setSystemUiVisibility(android.view.View.STATUS_BAR_HIDDEN);
		}
	}
	private static class DimSystemStatusBarDummy extends DimSystemStatusBar
	{
		private static class Holder
		{
			private static final DimSystemStatusBarDummy sInstance = new DimSystemStatusBarDummy();
		}
		public void dim(final View view)
		{
		}
	}
}
