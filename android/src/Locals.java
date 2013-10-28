/*
2012/7 Created by AKIZUKI Katane
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

class Locals {
	
	//App Setting

	public static String AppModuleName = Globals.APP_MODULE_NAME_ARRAY[0]; //do not change
	
	public static String AppCommandOptions = "";
	
	//Environment

	public static HashMap<String,String> EnvironmentMap = new HashMap<String,String>(); //do not change
	static {
		//EnvironmentMap.put("EXAMPLE_KEY", "ExampleValue");
	}
	
	//Screen Setting
	
	public static int ScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; //ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE or ActivityInfo.ORIENTATION_PORTRAIT	
	
	//Video Setting
	
	public static int VideoXPosition = 0;  //-1:Left 0:Center 1:Right
	public static int VideoYPosition = -1; //-1:Top  0:Center 1:Bottom
	public static int VideoXMargin = 0;
	public static int VideoYMargin = 0;
	
	public static int VideoXRatio = 480;     //r <= 0:FULL;
	public static int VideoYRatio = 272;     //r <= 0:FULL;
	public static int VideoDepthBpp = 32;  //16 or 32
	public static boolean VideoSmooth = true;
	
	//Touch Mode Setting
	
	public static String TouchMode = "GamePad";
	//"Invalid"
	//"Touch"
	//"TrackPad"
	//"GamePad"

	//AppConfig Setting
	
	public static boolean AppLaunchConfigUse = true;
	
	//Run Static Initializer
	
	public static boolean Run = false; //do not change
}
