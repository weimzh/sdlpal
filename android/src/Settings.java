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
/*
 2013/9 Modified by Martin Dieter
 */

package com.codeplex.sdlpal;

import java.util.TreeSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.util.Log;

import java.io.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.StatFs;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.Collections;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.lang.String;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.text.SpannedString;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.AlarmManager;

class Settings
{
	private static final String LOCALS_SETTINGS_FILENAME = "app_settings.ini";

	private static boolean globalsSettingsLoaded  = false;
	private static boolean globalsSettingsChanged = false;
	
	private static boolean localsSettingsLoaded  = false;
	//private static boolean localsSettingsChanged = false;

	private static void setupCurrentDirectory()
	{
		if(Globals.CurrentDirectoryPathForLauncher != null && !Globals.CurrentDirectoryPathForLauncher.equals("")){
			File curDirFile = new File(Globals.CurrentDirectoryPathForLauncher);
			if(!curDirFile.exists() || !curDirFile.isDirectory() || !curDirFile.canRead()){
				Globals.CurrentDirectoryPathForLauncher = null;
			}
		}
		Globals.CurrentDirectoryPath = null;
		
		ArrayList<String> curDirValidPathList = new ArrayList<String>();
		TreeSet<String> curDirPathSet = new TreeSet<String>();

		for(String curDirPathTemplate : Globals.CURRENT_DIRECTORY_PATH_TEMPLATE_ARRAY){
			if(curDirPathTemplate.indexOf("${SDCARD}") < 0){
				curDirPathSet.add(curDirPathTemplate);
			} else {
				String storagePath;
			
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", Environment.getExternalStorageDirectory().getAbsolutePath()));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/ext_card"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/flash"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/sdcard"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/sdcard/external_sd"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/sdcard-ext"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/storage/sdcard"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/udisk"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/mnt/usbdisk"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/sdcard"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/sdcard/sd"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/storage/sdcard"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/storage/sdcard0"));
				curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", "/storage/extSdCard"));
			
				storagePath = System.getenv("EXTERNAL_STORAGE");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("EXTERNAL_STORAGE2");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("EXTERNAL_ALT_STORAGE");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("SECOND_VOLUME_STORAGE");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("THIRD_VOLUME_STORAGE");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("SECONDARY_STORAGE");
				if(storagePath != null){
					curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePath));
				}
				storagePath = System.getenv("EXTERNAL_STORAGE_ALL");
				if(storagePath != null){
					String[] storagePathArray = storagePath.split(";");
					for(int i = 0; i < storagePathArray.length; i ++){
						curDirPathSet.add(curDirPathTemplate.replace("${SDCARD}", storagePathArray[i]));
					}
				}
			}
			
			Iterator<String> curDirIterator = curDirPathSet.iterator();
			while(curDirIterator.hasNext()){
				File curDirFile = new File(curDirIterator.next());
				if(curDirFile.exists() && curDirFile.isDirectory() && curDirFile.canRead() && (!Globals.CURRENT_DIRECTORY_NEED_WRITABLE || curDirFile.canWrite())){
					String path = curDirFile.getAbsolutePath();
					if(Globals.CurrentDirectoryPathForLauncher == null || Globals.CurrentDirectoryPathForLauncher.equals("")){
						Globals.CurrentDirectoryPathForLauncher = path;
					}
					if(Globals.CurrentDirectoryPath == null || Globals.CurrentDirectoryPath.equals("")){
						Globals.CurrentDirectoryPath = path;
					}
					curDirValidPathList.add(path);
				}
			}
		}
		
		Globals.CurrentDirectoryPathArray = (String[])curDirPathSet.toArray(new String[0]);
		Globals.CurrentDirectoryValidPathArray = (String[])curDirValidPathList.toArray(new String[0]);
	}

	public static void LoadGlobals( MainActivity activity )
	{
		if(globalsSettingsLoaded) // Prevent starting twice
		{
			return;
		}
		
		Globals.Run = true;

		nativeInitKeymap();
		if( (android.os.Build.MODEL.equals("GT-N7000") || android.os.Build.MODEL.equals("SGH-I717"))
		   && android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1 )
		{
			// Samsung Galaxy Note generates a keypress when you hover a stylus over the screen, and that messes up OpenTTD dialogs
			// ICS update sends events in a proper way
			nativeSetKeymapKey(112, SDL_1_2_Keycodes.SDLK_UNKNOWN);
		}

		//Load
		
		SharedPreferences sp = activity.getSharedPreferences("pref", Context.MODE_PRIVATE);
		Globals.CurrentDirectoryPathForLauncher = sp.getString("CurrentDirectoryPathForLauncher", Globals.CurrentDirectoryPathForLauncher);
		Globals.MouseCursorShowed = sp.getBoolean("MouseCursorShowed", Globals.MouseCursorShowed);
		Globals.ButtonLeftEnabled = sp.getBoolean("ButtonLeftEnabled", Globals.ButtonLeftEnabled);
		Globals.ButtonRightEnabled = sp.getBoolean("ButtonRightEnabled", Globals.ButtonRightEnabled);
		Globals.ButtonTopEnabled = sp.getBoolean("ButtonTopEnabled", Globals.ButtonTopEnabled);
		Globals.ButtonBottomEnabled = sp.getBoolean("ButtonBottomEnabled", Globals.ButtonBottomEnabled);
		Globals.ButtonLeftNum = sp.getInt("ButtonLeftNum", Globals.ButtonLeftNum);
		Globals.ButtonRightNum = sp.getInt("ButtonRightNum", Globals.ButtonRightNum);
		Globals.ButtonTopNum = sp.getInt("ButtonTopNum", Globals.ButtonTopNum);
		Globals.ButtonBottomNum = sp.getInt("ButtonBottomNum", Globals.ButtonBottomNum);
		Globals.GamePadSize = sp.getInt("GamePadSize", Globals.GamePadSize);
		Globals.GamePadOpacity = sp.getInt("GamePadOpacity", Globals.GamePadOpacity);
		Globals.GamePadPosition = sp.getInt("GamePadPosition", Globals.GamePadPosition);
		Globals.GamePadArrowButtonAsAxis = sp.getBoolean("GamePadArrowButtonAsAxis", Globals.GamePadArrowButtonAsAxis);
		
		//Log.i("Engine(Java)", "SDLKeyAdditionalKeyMap:" + sp.getString("SDLKeyAdditionalKeyMap", ""));
		
		String[] keyMapArray = sp.getString("SDLKeyAdditionalKeyMap", "").split(",");
		for(String keyMap : keyMapArray){
			String[] keyVal = keyMap.split("=");
			if(keyVal.length == 2){
				try {
					Globals.SDLKeyAdditionalKeyMap.put(new Integer(keyVal[0]), new Integer(keyVal[1]));
				} catch(NumberFormatException e){}
			}
		}
		
		//Setup
		
		for(Iterator it = Globals.SDLKeyAdditionalKeyMap.entrySet().iterator(); it.hasNext();) {
			TreeMap.Entry<Integer,Integer> entry = (TreeMap.Entry<Integer,Integer>)it.next();
			Integer javaKey = (Integer)entry.getKey();
			Integer sdlKey = (Integer)entry.getValue();
			nativeSetKeymapKey(javaKey, sdlKey);
		}

		setupCurrentDirectory();
		
		globalsSettingsLoaded = true;
	}
	
	public static void SaveGlobals( MainActivity activity )
	{
		SharedPreferences sp = activity.getSharedPreferences("pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.clear();
		if(Globals.CurrentDirectoryPathForLauncher != null){
			editor.putString("CurrentDirectoryPathForLauncher", Globals.CurrentDirectoryPathForLauncher);
		}
		editor.putBoolean("MouseCursorShowed", Globals.MouseCursorShowed);
		editor.putBoolean("ButtonLeftEnabled", Globals.ButtonLeftEnabled);
		editor.putBoolean("ButtonRightEnabled", Globals.ButtonRightEnabled);
		editor.putBoolean("ButtonTopEnabled", Globals.ButtonTopEnabled);
		editor.putBoolean("ButtonBottomEnabled", Globals.ButtonBottomEnabled);
		editor.putInt("ButtonLeftNum", Globals.ButtonLeftNum);
		editor.putInt("ButtonRightNum", Globals.ButtonRightNum);
		editor.putInt("ButtonTopNum", Globals.ButtonTopNum);
		editor.putInt("ButtonBottomNum", Globals.ButtonBottomNum);
		editor.putInt("GamePadSize", Globals.GamePadSize);
		editor.putInt("GamePadOpacity", Globals.GamePadOpacity);
		editor.putInt("GamePadPosition", Globals.GamePadPosition);
		editor.putBoolean("GamePadArrowButtonAsAxis", Globals.GamePadArrowButtonAsAxis);
		
		StringBuffer buf = new StringBuffer();
		for(Iterator it = Globals.SDLKeyAdditionalKeyMap.entrySet().iterator(); it.hasNext();) {
			TreeMap.Entry<Integer,Integer> entry = (TreeMap.Entry<Integer,Integer>)it.next();
			Integer javaKey = (Integer)entry.getKey();
			Integer sdlKey = (Integer)entry.getValue();
			if(buf.length() > 0){
				buf.append(",");
			}
			buf.append(javaKey.intValue());
			buf.append("=");
			buf.append(sdlKey.intValue());
		}
		editor.putString("SDLKeyAdditionalKeyMap", buf.toString());
		
		editor.commit();
	}
	
	public static void LoadLocals( MainActivity activity )
	{
		if(localsSettingsLoaded)
		{
			return;
		}
		
		Locals.Run = true;
		
		//

		String path = Globals.CurrentDirectoryPath + "/" + LOCALS_SETTINGS_FILENAME;
		
		FileInputStream   fis = null;
		InputStreamReader isr = null;
		BufferedReader    br  = null;
		try {
            fis = new FileInputStream(path);
            isr = new InputStreamReader(fis, "UTF-8");
			br  = new BufferedReader(isr);
			
			Locals.EnvironmentMap.clear();
			
			boolean isEnv = false;
			
			String line;
            while((line = br.readLine()) != null){
				String lineTrimed = line.trim();
				char c = lineTrimed.charAt(0);
				if(c == '#'){
					continue;
				} else if(c == '['){
					isEnv = lineTrimed.equals("[Environment]");
				} else {
					int d = lineTrimed.indexOf("=");
					if(d >= 0){
						String key = lineTrimed.substring(0, d).trim();
						String val = lineTrimed.substring(d + 1).trim();
						if(isEnv){
							Locals.EnvironmentMap.put(key, val);
						} else {
							if(key.equals("AppModuleName")){
								Locals.AppModuleName = val;
							} else if(key.equals("AppCommandOptions")){
								Locals.AppCommandOptions = val;
							} else if(key.equals("ScreenOrientation")){
								try {
									Locals.ScreenOrientation = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoXPosition")){
								try {
									Locals.VideoXPosition = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoYPosition")){
								try {
									Locals.VideoYPosition = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoXMargin")){
								try {
									Locals.VideoXMargin = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoYMargin")){
								try {
									Locals.VideoYMargin = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoXRatio")){
								try {
									Locals.VideoXRatio = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoYRatio")){
								try {
									Locals.VideoYRatio = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoDepthBpp")){
								try {
									Locals.VideoDepthBpp = Integer.parseInt(val);
								} catch(NumberFormatException ne){}
							} else if(key.equals("VideoSmooth")){
								Locals.VideoSmooth = Boolean.parseBoolean(val);
							} else if(key.equals("TouchMode")){
								Locals.TouchMode = val;
							} else if(key.equals("AppLaunchConfigUse")){
								Locals.AppLaunchConfigUse = Boolean.parseBoolean(val);
							//} else if(key.equals("StdOutRedirect")){
							//	Locals.StdOutRedirect = Boolean.parseBoolean(val);
							} else {
								Log.w("Engine(Java)","[Settings.LoadLocals()] Unknown Key=" + key + " Value=" + val);
							}
						}
					}
				}
            }
        } catch(IOException e) {
			//Log.e("Engine", "Settings.SaveLocals" + e);
        } finally {
			if (br  != null) try { br.close();  } catch (IOException e) {}
			if (isr != null) try { isr.close(); } catch (IOException e) {}
			if (fis != null) try { fis.close(); } catch (IOException e) {}
		}
		
		//
		
		boolean check;
		
		check = false;
		for(String moduleName : Globals.APP_MODULE_NAME_ARRAY){
			if(Locals.AppModuleName.equals(moduleName)){
				check = true;
			}
		}
		if(!check){
			Locals.AppModuleName = Globals.APP_MODULE_NAME_ARRAY[0];
		}
		
		localsSettingsLoaded = true;
	}
	
	public static void SaveLocals( MainActivity activity )
	{
		String path = Globals.CurrentDirectoryPath + "/" + LOCALS_SETTINGS_FILENAME;
		
		FileOutputStream   fos  = null;
		OutputStreamWriter osw = null;
		BufferedWriter     bw  = null;
		try {
            fos = new FileOutputStream(path);
            osw = new OutputStreamWriter(fos, "UTF-8");
			bw  = new BufferedWriter(osw);
			
			bw.write("[App]");
			bw.newLine();
			
			bw.write("AppModuleName=" + Locals.AppModuleName);
			bw.newLine();
			bw.write("AppCommandOptions=" + Locals.AppCommandOptions);
			bw.newLine();
			bw.write("ScreenOrientation=" + Locals.ScreenOrientation);
			bw.newLine();
			bw.write("VideoXPosition=" + Locals.VideoXPosition);
			bw.newLine();
			bw.write("VideoYPosition=" + Locals.VideoYPosition);
			bw.newLine();
			bw.write("VideoXMargin=" + Locals.VideoXMargin);
			bw.newLine();
			bw.write("VideoYMargin=" + Locals.VideoYMargin);
			bw.newLine();
			bw.write("VideoXRatio=" + Locals.VideoXRatio);
			bw.newLine();
			bw.write("VideoYRatio=" + Locals.VideoYRatio);
			bw.newLine();
			bw.write("VideoDepthBpp=" + Locals.VideoDepthBpp);
			bw.newLine();
			bw.write("VideoSmooth=" + Locals.VideoSmooth);
			bw.newLine();
			bw.write("TouchMode=" + Locals.TouchMode);
			bw.newLine();
			bw.write("AppLaunchConfigUse=" + Locals.AppLaunchConfigUse);
			bw.newLine();
			//bw.write("StdOutRedirect=" + Locals.StdOutRedirect);
			//bw.newLine();
			
			bw.write("[Environment]");
			bw.newLine();
			for(Iterator it = Locals.EnvironmentMap.entrySet().iterator(); it.hasNext();) {
				HashMap.Entry<String,String> entry = (HashMap.Entry<String,String>)it.next();
				String key = (String)entry.getKey();
				String val = (String)entry.getValue();
				bw.write(key + "=" + val);
				bw.newLine();
			}
			
			bw.flush();
        } catch(IOException e) {
			//Log.e("Engine", "Settings.SaveLocals" + e);
        } finally {
			if (bw  != null) try { bw.close();  } catch (IOException e) {}
			if (osw != null) try { osw.close(); } catch (IOException e) {}
			if (fos != null) try { fos.close(); } catch (IOException e) {}
		}
	}
	
	// ===============================================================================================
	
	public static void Apply(Activity p)
	{
		nativeSetVideoDepth(Locals.VideoDepthBpp, Globals.VIDEO_NEED_GLES2 ? 1 : 0);
		if(Locals.VideoSmooth){
			nativeSetSmoothVideo();
		}
		
		/*
		 String lang = new String(Locale.getDefault().getLanguage());
		 if( Locale.getDefault().getCountry().length() > 0 )
		 lang = lang + "_" + Locale.getDefault().getCountry();
		System.out.println( "Engine: setting envvar LANGUAGE to '" + lang + "'");
		nativeSetEnv( "LANG", lang );
		nativeSetEnv( "LANGUAGE", lang );
*/
		// TODO: get current user name and set envvar USER, the API is not availalbe on Android 1.6 so I don't bother with this
	}
	
	public static void setKeymapKey(int javakey, int key)
	{		
		Globals.SDLKeyAdditionalKeyMap.put(new Integer(javakey), new Integer(key));
		
		nativeSetKeymapKey(javakey, key);
	}

	private static native void nativeSetSmoothVideo();
	private static native void nativeSetVideoDepth(int bpp, int gles2);
	private static native void nativeSetCompatibilityHacks();
	private static native void nativeSetVideoMultithreaded();
	private static native void nativeInitKeymap();
	private static native int  nativeGetKeymapKey(int key);
	private static native void nativeSetKeymapKey(int javakey, int key);
	
	public static native void  nativeSetEnv(final String name, final String value);
	public static native int   nativeChmod(final String name, int mode);
}

