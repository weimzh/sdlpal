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
2012/7 Modified by AKIZUKI Katane
*/

package jp.rikku2000.sdlpal;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.Context;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.ArrayList;
import android.view.KeyEvent;

import android.os.Environment;

class Globals {
	
	//Library Setting
	
	static {
		// Additional Library

		// do not change
		System.loadLibrary("sdl-1.2_jni");
		System.loadLibrary("sdl_main_jni");
		System.loadLibrary("stlport_shared");
		// do not change
	}
	
	//App Setting
	
	//public static final String APP_NAME    = "App";
	//public static final String APP_VERSION = "";
	//public static final String APP_ABOUT   = "";
	
	public static final boolean APP_LAUNCHER_USE = false;
	
	public static final boolean APP_CAN_RESUME = true;
	
	public static final String[] APP_MODULE_NAME_ARRAY = {
		"sdlpal",
	}; // libapp_$(APP_MODULE).so
	
	public static final String[][] APP_COMMAND_OPTIONS_ITEMS = {
		//{"Example","--example"},
		//{"Hello","--hello 100"}
	};
	
	public static final String[] APP_NEED_FILENAME_ARRAY = {
		"abc.mkf",
		"ball.mkf",
		"data.mkf",
		"fbp.mkf",
		"fire.mkf",
		"f.mkf",
		"gop.mkf",
		"map.mkf",
		"mgo.mkf",
		"m.msg",
		"mus.mkf",
		"pat.mkf",
		"rgm.mkf",
		"rng.mkf",
		"sss.mkf",
		"voc.mkf",
		"wor16.asc",
		"wor16.fon",
		"word.dat"
		// "example.dat",
		// "hello.ogg"
	};
	
	//Environment Setting
	
	public static final String[][] ENVIRONMENT_ITEMS = {
		// {"Example","KEY","VALUE"},
		// {"Hello","HELLO","100"}
	};
	
	//Current Directory Setting

	public static final boolean CURRENT_DIRECTORY_NEED_WRITABLE = true;
	public static final String[] CURRENT_DIRECTORY_PATH_TEMPLATE_ARRAY = {
		"${SDCARD}/sdlpal",
		"${SDCARD}/pal"
		// "${SDCARD}/example",
		// "${SDCARD}/hello",
		// "/tmp/hello"
	};
	
	public static String CurrentDirectoryPathForLauncher = null;  //do not change
	public static String CurrentDirectoryPath = null;             //do not change
	public static String[] CurrentDirectoryPathArray = null;      //do not change
	public static String[] CurrentDirectoryValidPathArray = null; //do not change
	
	//Video Setting
	
	public static final int[] VIDEO_DEPTH_BPP_ITEMS = {16, 32}; //do not change
	
	public static final int[][] VIDEO_RATIO_ITEMS = {
		{4,3},	//4:3
		{16,9},	//16:9
		{16,10},//16:10
		{0,0},	//FULL
		{1,1},
		{3,2},
		{3,4},
		{5,3},
		{5,4},
		{6,4},
		{8,5},
		{11,9},
		{15,9},
		{16,5},
		{16,8},
		{22,15},
		{23,16},
		{25,16},
		{32,15},
		{64,35},
		{112,75},
		{128,75},
		{256,135},
		{480,272},
		{512,307},
		{600,400},
		{640,400},
		{640,480},
		{800,480},
		{800,600},
		{1024,600},
		{1024,614},
		{1024,768},
		{1280,720},
		{1280,800}
	};

	public static boolean VIDEO_NEED_DEPTH_BUFFER = false;
	public static boolean VIDEO_NEED_STENCIL_BUFFER = false;
	public static boolean VIDEO_NEED_GLES2 = false;
	public static boolean VIDEO_NON_BLOCKING_SWAP_BUFFERS = false;
	
	//Audio Setting
	
	public static int AUDIO_BUFFER_CONFIG = 0; //???
	
	//Mouse Setting
	
	public static final boolean MOUSE_USE = false;
	
	public static boolean MouseCursorShowed = false;
	
	//Keycode
	
	//please keep in sync with javakeycodes.h
	public static final int KEYCODE_LAST                             = 512; //do not change
	public static final int KEYCODE_USER_FIRST                       = 400; //do not change
	public static final int KEYCODE_USER_LAST                        = 512; //do not change
	public static final int KEYCODE_USER_MENU_FIRST                  = 400; //do not change
	public static final int KEYCODE_USER_MENU_LAST                   = 414; //do not change
	public static final int KEYCODE_USER_SUBMENU_FIRST               = 415; //do not change
	public static final int KEYCODE_USER_SUBMENU_LAST                = 429; //do not change
	public static final int KEYCODE_USER_BUTTON_LEFT_FIRST           = 430; //do not change
	public static final int KEYCODE_USER_BUTTON_LEFT_LAST            = 444; //do not change
	public static final int KEYCODE_USER_BUTTON_RIGHT_FIRST          = 445; //do not change
	public static final int KEYCODE_USER_BUTTON_RIGHT_LAST           = 459; //do not change
	public static final int KEYCODE_USER_BUTTON_TOP_FIRST            = 460; //do not change
	public static final int KEYCODE_USER_BUTTON_TOP_LAST             = 474; //do not change
	public static final int KEYCODE_USER_BUTTON_BOTTOM_FIRST         = 475; //do not change
	public static final int KEYCODE_USER_BUTTON_BOTTOM_LAST          = 489; //do not change
	public static final int KEYCODE_USER_GAMEPAD_BUTTON_ARROW_FIRST  = 490; //do not change
	public static final int KEYCODE_USER_GAMEPAD_BUTTON_ARROW_LAST   = 494; //do not change
	public static final int KEYCODE_USER_GAMEPAD_BUTTON_ACTION_FIRST = 495; //do not change
	public static final int KEYCODE_USER_GAMEPAD_BUTTON_ACTION_LAST  = 499; //do not change
	public static final int KEYCODE_USER_JOYSTICK_AXIS_FIRST         = 500; //do not change
	public static final int KEYCODE_USER_JOYSTICK_AXIS_LAST          = 503; //do not change
	public static final int KEYCODE_USER_JOYSTICK_AXISHAT_FIRST      = 504; //do not change
	public static final int KEYCODE_USER_JOYSTICK_AXISHAT_LAST       = 508; //do not change
	
	//Menu Setting
	
	public static final int MENU_KEY_NUM = 0; //NUM <= 15
	
	public static int[] MENU_KEY_ARRAY = new int[MENU_KEY_NUM]; //do not change
	static {
		//set unused keycode
		for(int i = 0; i < MENU_KEY_NUM; i ++) {             //do not change
			MENU_KEY_ARRAY[i] = KEYCODE_USER_MENU_FIRST + i; //do not change
		}                                                    //do not change
	}
	
	//SubMenu Setting

	public static final int SUBMENU_KEY_NUM = 0; //NUM <= 15
	
	public static int[] SUBMENU_KEY_ARRAY = new int[SUBMENU_KEY_NUM]; //do not change
	static {
		//set unused keycode
		for(int i = 0; i < SUBMENU_KEY_NUM; i ++) {                //do not change
			SUBMENU_KEY_ARRAY[i] = KEYCODE_USER_SUBMENU_FIRST + i; //do not change
		}                                                          //do not change
	}
	
	//Button Setting
	
	public static final boolean BUTTON_USE = false;
	
	public static boolean ButtonLeftEnabled   = false;
	public static boolean ButtonRightEnabled  = false;
	public static boolean ButtonTopEnabled    = false;
	public static boolean ButtonBottomEnabled = false;
	
	public static final int BUTTON_LEFT_MAX   = 15; //MAX <= 15
	public static final int BUTTON_RIGHT_MAX  = 15; //MAX <= 15
	public static final int BUTTON_TOP_MAX    = 15; //MAX <= 15
	public static final int BUTTON_BOTTOM_MAX = 15; //MAX <= 15
	
	public static int ButtonLeftNum   = 6; //Num <= BUTTON_LEFT_MAX;
	public static int ButtonRightNum  = 6; //Num <= BUTTON_RIGHT_MAX;
	public static int ButtonTopNum    = 6; //Num <= BUTTON_TOP_MAX;
	public static int ButtonBottomNum = 6; //Num <= BUTTON_BOTTOM_MAX;

	public static int[] BUTTON_LEFT_KEY_ARRAY = new int[BUTTON_LEFT_MAX]; //do not change
	public static int[] BUTTON_RIGHT_KEY_ARRAY = new int[BUTTON_RIGHT_MAX]; //do not change
	public static int[] BUTTON_TOP_KEY_ARRAY = new int[BUTTON_TOP_MAX]; //do not change
	public static int[] BUTTON_BOTTOM_KEY_ARRAY = new int[BUTTON_BOTTOM_MAX]; //do not change
	static {
		//set unused keycode
		for(int i = 0; i < BUTTON_LEFT_MAX; i ++) {                            //do not change
			BUTTON_LEFT_KEY_ARRAY[i] = KEYCODE_USER_BUTTON_LEFT_FIRST + i;     //do not change
		}                                                                      //do not change
		for(int i = 0; i < BUTTON_RIGHT_MAX; i ++) {                           //do not change
			BUTTON_RIGHT_KEY_ARRAY[i] = KEYCODE_USER_BUTTON_RIGHT_FIRST + i;   //do not change
		}                                                                      //do not change
		for(int i = 0; i < BUTTON_TOP_MAX; i ++) {                             //do not change
			BUTTON_TOP_KEY_ARRAY[i] = KEYCODE_USER_BUTTON_TOP_FIRST + i;       //do not change
		}                                                                      //do not change
		for(int i = 0; i < BUTTON_BOTTOM_MAX; i ++) {                          //do not change
			BUTTON_BOTTOM_KEY_ARRAY[i] = KEYCODE_USER_BUTTON_BOTTOM_FIRST + i; //do not change
		}                                                                      //do not change
	}
	
	//GamePad (Touch)

	public static final int GAMEPAD_BUTTON_ARROW_NUM  = 4; //do not change
	public static final int GAMEPAD_BUTTON_ACTION_NUM = 4; //do not change
	
	public static final int GAMEPAD_BUTTON_ARROW_UP_INDEX    = 0; //do not change
	public static final int GAMEPAD_BUTTON_ARROW_RIGHT_INDEX = 1; //do not change
	public static final int GAMEPAD_BUTTON_ARROW_DOWN_INDEX  = 2; //do not change
	public static final int GAMEPAD_BUTTON_ARROW_LEFT_INDEX  = 3; //do not change
	
	public static final int GAMEPAD_BUTTON_ACTION_UP_INDEX    = 0; //do not change
	public static final int GAMEPAD_BUTTON_ACTION_RIGHT_INDEX = 1; //do not change
	public static final int GAMEPAD_BUTTON_ACTION_DOWN_INDEX  = 2; //do not change
	public static final int GAMEPAD_BUTTON_ACTION_LEFT_INDEX  = 3; //do not change
	
	public static int[] GAMEPAD_BUTTON_ARROW_KEY_ARRAY  = new int[GAMEPAD_BUTTON_ARROW_NUM];  //do not change
	public static int[] GAMEPAD_BUTTON_ACTION_KEY_ARRAY = new int[GAMEPAD_BUTTON_ACTION_NUM]; //do not change
	static {
		//set unused keycode
		for(int i = 0; i < GAMEPAD_BUTTON_ARROW_NUM; i ++) {                                   //do not change
			GAMEPAD_BUTTON_ARROW_KEY_ARRAY[i] = KEYCODE_USER_GAMEPAD_BUTTON_ARROW_FIRST + i;   //do not change
		}                                                                                      //do not change
		for(int i = 0; i < GAMEPAD_BUTTON_ACTION_NUM; i ++) {                                  //do not change
			GAMEPAD_BUTTON_ACTION_KEY_ARRAY[i] = KEYCODE_USER_GAMEPAD_BUTTON_ACTION_FIRST + i; //do not change
		}                                                                                      //do not change
	}
	
	public static boolean GamePadArrowButtonAsAxis = true;
	public static int GamePadSize = 50; //percent
	public static int GamePadOpacity = 30;//percent
	public static int GamePadPosition = 100; //percent 0:Top 100:Bottom
	
	//JoyStick Axis
	
	public static final int JOYSTICK_AXIS_NUM = 4; //do not change
	
	public static final int JOYSTICK_AXIS_UP_INDEX    = 0; //do not change
	public static final int JOYSTICK_AXIS_RIGHT_INDEX = 1; //do not change
	public static final int JOYSTICK_AXIS_DOWN_INDEX  = 2; //do not change
	public static final int JOYSTICK_AXIS_LEFT_INDEX  = 3; //do not change
	
	public static int[] JOYSTICK_AXIS_KEY_ARRAY    = new int[JOYSTICK_AXIS_NUM]; //do not change
	public static int[] JOYSTICK_AXISHAT_KEY_ARRAY = new int[JOYSTICK_AXIS_NUM]; //do not change
	static {
		//set unused keycode
		for(int i = 0; i < JOYSTICK_AXIS_NUM; i ++) {                                //do not change
			JOYSTICK_AXIS_KEY_ARRAY[i]    = KEYCODE_USER_JOYSTICK_AXIS_FIRST + i;    //do not change
			JOYSTICK_AXISHAT_KEY_ARRAY[i] = KEYCODE_USER_JOYSTICK_AXISHAT_FIRST + i; //do not change
		}                                                                            //do not change
	}
	
	//Key Setting
	public static TreeMap<Integer,String> SDLKeyFunctionNameMap = new TreeMap<Integer,String>(); //do not change
	static {
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN), "None");
		
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_UP), "Up");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_DOWN), "Down");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_LEFT), "Left");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_RIGHT), "Right");
		
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_RETURN), "Enter");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_ESCAPE), "Cancel");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_a), "A");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_e), "E");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_f), "F");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_q), "Q");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_r), "R");
		SDLKeyFunctionNameMap.put(new Integer(SDL_1_2_Keycodes.SDLK_w), "W");
	}
	
	public static TreeMap<Integer,Integer> SDLKeyAdditionalKeyMap = new TreeMap<Integer,Integer>(); //do not change
	static {
		//Menu
		/* SDLKeyAdditionalKeyMap.put(new Integer(MENU_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(MENU_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN)); */

		//SubMenu
		/* SDLKeyAdditionalKeyMap.put(new Integer(SUBMENU_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(SUBMENU_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(SUBMENU_KEY_ARRAY[2]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(SUBMENU_KEY_ARRAY[3]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN)); */

		//Button
		/* SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[2]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[3]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[4]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_LEFT_KEY_ARRAY[5]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[2]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[3]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[4]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_RIGHT_KEY_ARRAY[5]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[2]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[3]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[4]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_TOP_KEY_ARRAY[5]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[0]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[1]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[2]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[3]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[4]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(BUTTON_BOTTOM_KEY_ARRAY[5]), new Integer(SDL_1_2_Keycodes.SDLK_UNKNOWN)); */

		// GamePad (Touch)
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ARROW_KEY_ARRAY[GAMEPAD_BUTTON_ARROW_UP_INDEX]),    new Integer(SDL_1_2_Keycodes.SDLK_UP));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ARROW_KEY_ARRAY[GAMEPAD_BUTTON_ARROW_RIGHT_INDEX]), new Integer(SDL_1_2_Keycodes.SDLK_RIGHT));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ARROW_KEY_ARRAY[GAMEPAD_BUTTON_ARROW_DOWN_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_DOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ARROW_KEY_ARRAY[GAMEPAD_BUTTON_ARROW_LEFT_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_LEFT));		
		
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ACTION_KEY_ARRAY[GAMEPAD_BUTTON_ACTION_UP_INDEX]),    new Integer(SDL_1_2_Keycodes.SDLK_r));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ACTION_KEY_ARRAY[GAMEPAD_BUTTON_ACTION_RIGHT_INDEX]), new Integer(SDL_1_2_Keycodes.SDLK_RETURN));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ACTION_KEY_ARRAY[GAMEPAD_BUTTON_ACTION_LEFT_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_e));
		SDLKeyAdditionalKeyMap.put(new Integer(GAMEPAD_BUTTON_ACTION_KEY_ARRAY[GAMEPAD_BUTTON_ACTION_DOWN_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_ESCAPE));
		
		// JoyStick Axis
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXIS_KEY_ARRAY[JOYSTICK_AXIS_UP_INDEX]),    new Integer(SDL_1_2_Keycodes.SDLK_UP));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXIS_KEY_ARRAY[JOYSTICK_AXIS_RIGHT_INDEX]), new Integer(SDL_1_2_Keycodes.SDLK_RIGHT));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXIS_KEY_ARRAY[JOYSTICK_AXIS_DOWN_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_DOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXIS_KEY_ARRAY[JOYSTICK_AXIS_LEFT_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_LEFT));
		
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXISHAT_KEY_ARRAY[JOYSTICK_AXIS_UP_INDEX]),    new Integer(SDL_1_2_Keycodes.SDLK_UP));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXISHAT_KEY_ARRAY[JOYSTICK_AXIS_RIGHT_INDEX]), new Integer(SDL_1_2_Keycodes.SDLK_RIGHT));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXISHAT_KEY_ARRAY[JOYSTICK_AXIS_DOWN_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_DOWN));
		SDLKeyAdditionalKeyMap.put(new Integer(JOYSTICK_AXISHAT_KEY_ARRAY[JOYSTICK_AXIS_LEFT_INDEX]),  new Integer(SDL_1_2_Keycodes.SDLK_LEFT));
		
		//Key
		SDLKeyAdditionalKeyMap.put(new Integer(KeyEvent.KEYCODE_BACK), new Integer(SDL_1_2_Keycodes.SDLK_ESCAPE));
	}
	
	//Run Static Initializer
	
	public static boolean Run = false; //do not change
}
