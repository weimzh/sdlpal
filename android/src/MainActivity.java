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

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.text.InputType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.graphics.drawable.Drawable;
import android.content.res.Configuration;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import java.util.ArrayList;  
import java.util.TreeSet;
import java.util.Iterator;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import java.util.concurrent.Semaphore;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.media.AudioManager;
import android.graphics.Color;
import android.widget.CheckBox;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ); //for Test

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		instance = this;

		Settings.LoadGlobals(this);
		
		if(Globals.APP_LAUNCHER_USE){
			runAppLauncher();
		} else {
			runAppLaunchConfig();
		}
	}

	public boolean checkCurrentDirectory(boolean quitting)
	{
		String curDirPath = Globals.CurrentDirectoryPath;
		if(curDirPath != null && !curDirPath.equals("")){
			return true;
		}
		
		StringBuffer buf = new StringBuffer();
		for(String s: Globals.CurrentDirectoryPathArray){
			if(buf.length()>0) {
				buf.append("\n");
			}
			buf.append(s);
		}
		curDirPath = buf.toString();

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.error);
		alertDialogBuilder.setMessage(R.string.open_dir_error + "\n" + curDirPath);
		if(quitting){
			alertDialogBuilder.setPositiveButton(R.string.quit, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			});
		} else {
			alertDialogBuilder.setPositiveButton(R.string.ok, null);
		}
		alertDialogBuilder.setCancelable(false);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		
		return false;
	}
	
	//
	
	private class AppLauncherView extends LinearLayout implements Button.OnClickListener, DialogInterface.OnClickListener, AdapterView.OnItemClickListener
	{
		private MainActivity mActivity;
		
		private LinearLayout mCurDirLayout;
		private TextView mCurDirText;
		private Button mCurDirChgButton;
		private ListView mDirListView;
		
		private File [] mDirFileArray;
		
		public AppLauncherView(MainActivity activity)
		{
			super(activity);
			mActivity = activity;
			
			setOrientation(LinearLayout.VERTICAL);
			{
				mCurDirLayout = new LinearLayout(mActivity);
				{
					LinearLayout txtLayout = new LinearLayout(mActivity);
					txtLayout.setOrientation(LinearLayout.VERTICAL);
					{
						TextView txt1 = new TextView(mActivity);
						txt1.setTextSize(18.0f);
						txt1.setText(R.string.current_dir);
						txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						
						mCurDirText = new TextView(mActivity);
						mCurDirText.setPadding(5, 0, 0, 0);
						txtLayout.addView(mCurDirText, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
					}
					mCurDirLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT, 1));
					
					mCurDirChgButton = new Button(mActivity);
					mCurDirChgButton.setText(R.string.change_dir);
					mCurDirChgButton.setOnClickListener(this);
					mCurDirLayout.addView(mCurDirChgButton, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.FILL_PARENT));
				}
				addView(mCurDirLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

				mDirListView = new ListView(mActivity);
				mDirListView.setOnItemClickListener(this);
				addView(mDirListView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1) );
			}
			
			loadCurrentDirectory();
		}
		
		public void loadCurrentDirectory()
		{
			if(Globals.CurrentDirectoryPathForLauncher == null || Globals.CurrentDirectoryPathForLauncher.equals("")){
				mCurDirText.setText("");
			} else {
				mCurDirText.setText(Globals.CurrentDirectoryPathForLauncher);

				try {
					File searchDirFile = new File(Globals.CurrentDirectoryPathForLauncher);
				
					mDirFileArray = searchDirFile.listFiles(new FileFilter() {
						public boolean accept(File file) {
							return (!file.isHidden() && file.isDirectory() && file.canRead() && (!Globals.CURRENT_DIRECTORY_NEED_WRITABLE || file.canWrite()));
						}
					});
					
					Arrays.sort(mDirFileArray, new Comparator<File>(){
						public int compare(File src, File target){
							return src.getName().compareTo(target.getName());
						}
					});
				
					String[] dirPathArray = new String[mDirFileArray.length];
					for(int i = 0; i < mDirFileArray.length; i ++){
						dirPathArray[i] = mDirFileArray[i].getName();
					}

					ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, dirPathArray);
					mDirListView.setAdapter(arrayAdapter);
				} catch(Exception e){
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
					alertDialogBuilder.setTitle(R.string.error);
					alertDialogBuilder.setMessage(R.string.open_dir_error + "\n" + Globals.CurrentDirectoryPathForLauncher);
					alertDialogBuilder.setPositiveButton("OK", null);
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					
					ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, new String[0]);
					mDirListView.setAdapter(arrayAdapter);
				}
			}
		}
		
		public void onClick(View v)
		{
			String[] items = new String[Globals.CurrentDirectoryValidPathArray.length + 1];
			for(int i = 0; i < Globals.CurrentDirectoryValidPathArray.length; i ++){
				items[i] = Globals.CurrentDirectoryValidPathArray[i];
			}
			items[items.length - 1] = R.string.other;
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
			alertDialogBuilder.setTitle(R.string.choose_dir);
			alertDialogBuilder.setItems(items, this);
			alertDialogBuilder.setNegativeButton(R.string.cancel, null);
			alertDialogBuilder.setCancelable(true);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		
		private AlertDialog mDirBrowserDialog = null;
		private File[] mDirBrowserDirFileArray = null;
		private String mDirBrowserCurDirPath = null;
		
		public void onClick(DialogInterface dialog, int which)
		{
			if(dialog == mDirBrowserDialog){
				mDirBrowserCurDirPath   = mDirBrowserDirFileArray[which].getAbsolutePath();
				mDirBrowserDialog       = null;
				mDirBrowserDirFileArray = null;
			} else {
				if(which < Globals.CurrentDirectoryValidPathArray.length){
					Globals.CurrentDirectoryPathForLauncher = Globals.CurrentDirectoryValidPathArray[which];
					Settings.SaveGlobals(mActivity);
					loadCurrentDirectory();
					return;
				} else {
					if(Globals.CurrentDirectoryPathForLauncher == null || Globals.CurrentDirectoryPath.equals("")){
						mDirBrowserCurDirPath = "/";
					} else {
						mDirBrowserCurDirPath = Globals.CurrentDirectoryPathForLauncher;
					}
				}
			}
			
			try {
				File searchDirFile = new File(mDirBrowserCurDirPath);
				
				mDirBrowserDirFileArray = searchDirFile.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return (file.isDirectory() && file.canRead());
					}
				});
				
				Arrays.sort(mDirBrowserDirFileArray, new Comparator<File>(){
					public int compare(File src, File target){
						return src.getName().compareTo(target.getName());
					}
				});
				
				File parentFile = searchDirFile.getParentFile();
				if(parentFile != null){
					if(parentFile.canRead()){
						File[] newDirFileArray = new File[mDirBrowserDirFileArray.length + 1];
						newDirFileArray[0] = parentFile;
						for(int i=0; i < mDirBrowserDirFileArray.length; i ++){
							newDirFileArray[i+1] = mDirBrowserDirFileArray[i];
						}
						mDirBrowserDirFileArray = newDirFileArray;
					} else {
						parentFile = null;
					}
				}
				String[] dirPathArray = new String[mDirBrowserDirFileArray.length];
				for(int i = 0; i < mDirBrowserDirFileArray.length; i ++){
					dirPathArray[i] = mDirBrowserDirFileArray[i].getName();
				}
				if(parentFile != null && dirPathArray.length > 0){
					dirPathArray[0] = "..";
				}
				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
				alertDialogBuilder.setTitle(mDirBrowserCurDirPath);
				alertDialogBuilder.setItems(dirPathArray, this);
				alertDialogBuilder.setPositiveButton(R.string.set_dir, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton) {	
						Globals.CurrentDirectoryPathForLauncher = mDirBrowserCurDirPath;
						Settings.SaveGlobals(mActivity);
						loadCurrentDirectory();
					}
				});
				alertDialogBuilder.setNegativeButton(R.string.cancel, null);
				alertDialogBuilder.setCancelable(true);
				mDirBrowserDialog = alertDialogBuilder.create();
				mDirBrowserDialog.show();
			} catch(Exception e){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
				alertDialogBuilder.setTitle(R.string.error);
				alertDialogBuilder.setMessage(R.string.open_dir_error + "\n" + mDirBrowserCurDirPath);
				alertDialogBuilder.setPositiveButton(R.string.ok, null);
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}
		}
		
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		{
			Globals.CurrentDirectoryPath = mDirFileArray[position].getAbsolutePath();
			mDirFileArray = null;
			runAppLaunchConfig();
		}
	}
	
	public void runAppLauncher()
	{
		checkCurrentDirectory(false);
		AppLauncherView view = new AppLauncherView(this);
		setContentView(view);
	}
	
	//
	
	private class AppLaunchConfigView extends LinearLayout
	{
		MainActivity mActivity;

		ScrollView mConfView;
		LinearLayout mConfLayout;

		TextView mExecuteModuleText;
		TextView mVideoDepthText;
		TextView mScreenRatioText;
		TextView mScreenOrientationText;
		
		TextView[] mEnvironmentTextArray;
		Button[]   mEnvironmentButtonArray;
		
		Button mRunButton;
		
		public AppLaunchConfigView(MainActivity activity)
		{
			super(activity);
			mActivity = activity;
			
			setOrientation(LinearLayout.VERTICAL);
			{
				mConfView = new ScrollView(mActivity);
				{
					mConfLayout = new LinearLayout(mActivity);
					mConfLayout.setOrientation(LinearLayout.VERTICAL);
					{
						//Execute Module
						LinearLayout moduleLayout = new LinearLayout(mActivity);
						{
							LinearLayout txtLayout = new LinearLayout(mActivity);
							txtLayout.setOrientation(LinearLayout.VERTICAL);
							{
								TextView txt1 = new TextView(mActivity);
								txt1.setTextSize(18.0f);
								txt1.setText(R.string.exec_module);
								txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								mExecuteModuleText = new TextView(mActivity);
								mExecuteModuleText.setPadding(5, 0, 0, 0);
								mExecuteModuleText.setText(Locals.AppModuleName);
								txtLayout.addView(mExecuteModuleText, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							moduleLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
							
							if(Globals.APP_MODULE_NAME_ARRAY.length >= 2){
								Button btn = new Button(mActivity);
								btn.setText(R.string.change);
								btn.setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
										alertDialogBuilder.setTitle(R.string.exec_module);
										alertDialogBuilder.setItems(Globals.APP_MODULE_NAME_ARRAY, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface dialog, int which)
											{
												Locals.AppModuleName = Globals.APP_MODULE_NAME_ARRAY[which];
												mExecuteModuleText.setText(Locals.AppModuleName);
												Settings.SaveLocals(mActivity);
											}
										});
										alertDialogBuilder.setNegativeButton(R.string.cancel, null);
										alertDialogBuilder.setCancelable(true);
										AlertDialog alertDialog = alertDialogBuilder.create();
										alertDialog.show();
									}
								});
								moduleLayout.addView(btn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
						}
						mConfLayout.addView(moduleLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

						//Video Depth
						LinearLayout videoDepthLayout = new LinearLayout(mActivity);
						{
							LinearLayout txtLayout = new LinearLayout(mActivity);
							txtLayout.setOrientation(LinearLayout.VERTICAL);
							{
								TextView txt1 = new TextView(mActivity);
								txt1.setTextSize(18.0f);
								txt1.setText(R.string.video_depth);
								txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								mVideoDepthText = new TextView(mActivity);
								mVideoDepthText.setPadding(5, 0, 0, 0);
								mVideoDepthText.setText("" + Locals.VideoDepthBpp + "bpp");
								txtLayout.addView(mVideoDepthText, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							videoDepthLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
							
							if(Globals.VIDEO_DEPTH_BPP_ITEMS.length >= 2){
								Button btn = new Button(mActivity);
								btn.setText(R.string.change);
								btn.setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										String[] bppItems = new String[Globals.VIDEO_DEPTH_BPP_ITEMS.length];
										for(int i = 0; i < bppItems.length; i ++){
											bppItems[i] = "" + Globals.VIDEO_DEPTH_BPP_ITEMS[i] + "bpp";
										}
										
										AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
										alertDialogBuilder.setTitle(R.string.video_depth);
										alertDialogBuilder.setItems(bppItems, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface dialog, int which)
											{
												Locals.VideoDepthBpp = Globals.VIDEO_DEPTH_BPP_ITEMS[which];
												mVideoDepthText.setText("" + Locals.VideoDepthBpp + "bpp");
												Settings.SaveLocals(mActivity);
											}
										});
										alertDialogBuilder.setNegativeButton(R.string.cancel, null);
										alertDialogBuilder.setCancelable(true);
										AlertDialog alertDialog = alertDialogBuilder.create();
										alertDialog.show();
									}
								});
								videoDepthLayout.addView(btn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
						}
						mConfLayout.addView(videoDepthLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						
						//Screen Ratio
						LinearLayout screenRatioLayout = new LinearLayout(mActivity);
						{
							LinearLayout txtLayout = new LinearLayout(mActivity);
							txtLayout.setOrientation(LinearLayout.VERTICAL);
							{
								TextView txt1 = new TextView(mActivity);
								txt1.setTextSize(18.0f);
								txt1.setText(R.string.aspect_ratio);
								txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								mScreenRatioText = new TextView(mActivity);
								mScreenRatioText.setPadding(5, 0, 0, 0);
								if(Locals.VideoXRatio > 0 && Locals.VideoYRatio > 0){
									mScreenRatioText.setText("" + Locals.VideoXRatio + ":" + Locals.VideoYRatio);
								} else {
									mScreenRatioText.setText(R.string.full);
								}
								txtLayout.addView(mScreenRatioText, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							screenRatioLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
							
							Button btn1 = new Button(mActivity);
							btn1.setText(R.string.swap);
							btn1.setOnClickListener(new OnClickListener(){
								public void onClick(View v){
									int tmp = Locals.VideoXRatio;
									Locals.VideoXRatio = Locals.VideoYRatio;
									Locals.VideoYRatio = tmp;
									if(Locals.VideoXRatio > 0 && Locals.VideoYRatio > 0){
										mScreenRatioText.setText("" + Locals.VideoXRatio + ":" + Locals.VideoYRatio);
									} else {
										mScreenRatioText.setText(R.string.full);
									}
									Settings.SaveLocals(mActivity);
								}
							});
							screenRatioLayout.addView(btn1, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							
							if(Globals.VIDEO_RATIO_ITEMS.length >= 2){
								Button btn = new Button(mActivity);
								btn.setText(R.string.change);
								btn.setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										String[] ratioItems = new String[Globals.VIDEO_RATIO_ITEMS.length];
										for(int i = 0; i < ratioItems.length; i ++){
											int w = Globals.VIDEO_RATIO_ITEMS[i][0];
											int h = Globals.VIDEO_RATIO_ITEMS[i][1];
											if(w > 0 && h > 0){
												ratioItems[i] = "" + w + ":" + h;
											} else {
												ratioItems[i] = R.string.full;
											}
										}
										
										AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
										alertDialogBuilder.setTitle(R.string.screen_ratio);
										alertDialogBuilder.setItems(ratioItems, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface dialog, int which)
											{
												Locals.VideoXRatio = Globals.VIDEO_RATIO_ITEMS[which][0];
												Locals.VideoYRatio = Globals.VIDEO_RATIO_ITEMS[which][1];
												if(Locals.VideoXRatio > 0 && Locals.VideoYRatio > 0){
													mScreenRatioText.setText("" + Locals.VideoXRatio + ":" + Locals.VideoYRatio);
												} else {
													mScreenRatioText.setText(R.string.full);
												}
												Settings.SaveLocals(mActivity);
											}
										});
										alertDialogBuilder.setNegativeButton(R.string.cancel, null);
										alertDialogBuilder.setCancelable(true);
										AlertDialog alertDialog = alertDialogBuilder.create();
										alertDialog.show();
									}
								});
								screenRatioLayout.addView(btn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
						}
						mConfLayout.addView(screenRatioLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

						//Screen Orientation
						LinearLayout screenOrientationLayout = new LinearLayout(mActivity);
						{
							LinearLayout txtLayout = new LinearLayout(mActivity);
							txtLayout.setOrientation(LinearLayout.VERTICAL);
							{
								TextView txt1 = new TextView(mActivity);
								txt1.setTextSize(18.0f);
								txt1.setText(R.string.screen_orientation);
								txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								mScreenOrientationText = new TextView(mActivity);
								mScreenOrientationText.setPadding(5, 0, 0, 0);
								switch(Locals.ScreenOrientation){
									case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
										mScreenOrientationText.setText(R.string.portrait);
										break;
									case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
										mScreenOrientationText.setText(R.string.landscape);
										break;
									case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
										mScreenOrientationText.setText(R.string.r_portrait);
										break;
									case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
										mScreenOrientationText.setText(R.string.r_landscape);
										break;
									default:
										mScreenOrientationText.setText(R.string.unknown);
										break;
								}
								txtLayout.addView(mScreenOrientationText, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							screenOrientationLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
							
							Button btn = new Button(mActivity);
							btn.setText(R.string.change);
							btn.setOnClickListener(new OnClickListener(){
								public void onClick(View v){
									String[] screenOrientationItems;
									if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD){
										screenOrientationItems = new String[]{R.string.portrait, R.string.landscape, R.string.r_portrait, R.string.r_landscape};
									} else {
										screenOrientationItems = new String[]{R.string.portrait, R.string.landscape};
									}
									
									AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
									alertDialogBuilder.setTitle(R.string.screen_orientation);
									alertDialogBuilder.setItems(screenOrientationItems, new DialogInterface.OnClickListener(){
										public void onClick(DialogInterface dialog, int which)
										{
											switch(which){
												case 0:
													Locals.ScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
													mScreenOrientationText.setText(R.string.portrait);
													break;
												case 1:
													Locals.ScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
													mScreenOrientationText.setText(R.string.landscape);
													break;
												case 2:
													Locals.ScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
													mScreenOrientationText.setText(R.string.r_portrait);
													break;
												case 3:
													Locals.ScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
													mScreenOrientationText.setText(R.string.r_landscape);
													break;
											}
											Settings.SaveLocals(mActivity);
										}
									});
									alertDialogBuilder.setNegativeButton(R.string.cancel, null);
									alertDialogBuilder.setCancelable(true);
									AlertDialog alertDialog = alertDialogBuilder.create();
									alertDialog.show();
								}
							});
							screenOrientationLayout.addView(btn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						}
						mConfLayout.addView(screenOrientationLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
												
						//Smooth Video
						LinearLayout videoSmoothLayout = new LinearLayout(mActivity);
						{
							CheckBox chk = new CheckBox(mActivity);
							chk.setChecked(Locals.VideoSmooth);
							chk.setOnClickListener(new OnClickListener(){
								public void onClick(View v){
									CheckBox c = (CheckBox)v;
									Locals.VideoSmooth = c.isChecked();
									Settings.SaveLocals(mActivity);
								}
							});
							videoSmoothLayout.addView(chk, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							
							LinearLayout txtLayout = new LinearLayout(mActivity);
							txtLayout.setOrientation(LinearLayout.VERTICAL);
							{
								TextView txt1 = new TextView(mActivity);
								txt1.setTextSize(18.0f);
								txt1.setText(R.string.smooth_video);
								txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								TextView txt2 = new TextView(mActivity);
								txt2.setPadding(5, 0, 0, 0);
								txt2.setText(R.string.linear_filtering);
								txtLayout.addView(txt2, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							videoSmoothLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
						}
						mConfLayout.addView(videoSmoothLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

						//Command Options
						for(int i = 0; i < Globals.APP_COMMAND_OPTIONS_ITEMS.length; i ++){
							LinearLayout cmdOptLayout = new LinearLayout(mActivity);
							{
								final int index = i;
								
								CheckBox chk = new CheckBox(mActivity);
								chk.setChecked(Locals.AppCommandOptions.indexOf(Globals.APP_COMMAND_OPTIONS_ITEMS[index][1]) >= 0);
								chk.setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										CheckBox c = (CheckBox)v;
										if(!c.isChecked()){
											int start = Locals.AppCommandOptions.indexOf(Globals.APP_COMMAND_OPTIONS_ITEMS[index][1]);
											if(start == 0){
												Locals.AppCommandOptions = Locals.AppCommandOptions.replace(Globals.APP_COMMAND_OPTIONS_ITEMS[index][1], "");
											} else if(start >= 0){
												Locals.AppCommandOptions = Locals.AppCommandOptions.replace(" " + Globals.APP_COMMAND_OPTIONS_ITEMS[index][1], "");
											}
										} else {
											if(Locals.AppCommandOptions.equals("")){
												Locals.AppCommandOptions = Globals.APP_COMMAND_OPTIONS_ITEMS[index][1];
											} else {
												Locals.AppCommandOptions += " " + Globals.APP_COMMAND_OPTIONS_ITEMS[index][1];
											}
										}
										Settings.SaveLocals(mActivity);
									}
								});
								cmdOptLayout.addView(chk, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								LinearLayout txtLayout = new LinearLayout(mActivity);
								txtLayout.setOrientation(LinearLayout.VERTICAL);
								{
									TextView txt1 = new TextView(mActivity);
									txt1.setTextSize(18.0f);
									txt1.setText(Globals.APP_COMMAND_OPTIONS_ITEMS[index][0]);
									txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
									
									TextView txt2 = new TextView(mActivity);
									txt2.setPadding(5, 0, 0, 0);
									txt2.setText(Globals.APP_COMMAND_OPTIONS_ITEMS[index][1]);
									txtLayout.addView(txt2, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								}
								cmdOptLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
							}
							mConfLayout.addView(cmdOptLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						}
						
						//Environment
						mEnvironmentTextArray = new TextView[Globals.ENVIRONMENT_ITEMS.length];
						mEnvironmentButtonArray = new Button[Globals.ENVIRONMENT_ITEMS.length];
						for(int i = 0; i < Globals.ENVIRONMENT_ITEMS.length; i ++){
							LinearLayout envLayout = new LinearLayout(mActivity);
							{
								final int index = i;
								String value = Locals.EnvironmentMap.get(Globals.ENVIRONMENT_ITEMS[index][1]);
								
								CheckBox chk = new CheckBox(mActivity);
								chk.setChecked(value != null);
								chk.setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										CheckBox c = (CheckBox)v;
										if(!c.isChecked()){
											Locals.EnvironmentMap.remove(Globals.ENVIRONMENT_ITEMS[index][1]);
											mEnvironmentTextArray[index].setText("unset " + Globals.ENVIRONMENT_ITEMS[index][1]);
											mEnvironmentButtonArray[index].setVisibility(View.GONE);
										} else {
											Locals.EnvironmentMap.put(Globals.ENVIRONMENT_ITEMS[index][1], Globals.ENVIRONMENT_ITEMS[index][2]);
											mEnvironmentTextArray[index].setText(Globals.ENVIRONMENT_ITEMS[index][1] + "=" + Globals.ENVIRONMENT_ITEMS[index][2]);
											mEnvironmentButtonArray[index].setVisibility(View.VISIBLE);
										}
										Settings.SaveLocals(mActivity);
									}
								});
								envLayout.addView(chk, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								
								LinearLayout txtLayout = new LinearLayout(mActivity);
								txtLayout.setOrientation(LinearLayout.VERTICAL);
								{
									TextView txt1 = new TextView(mActivity);
									txt1.setTextSize(18.0f);
									txt1.setText(Globals.ENVIRONMENT_ITEMS[index][0]);
									txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
									
									mEnvironmentTextArray[i] = new TextView(mActivity);
									mEnvironmentTextArray[i].setPadding(5, 0, 0, 0);
									if(value == null){
										mEnvironmentTextArray[i].setText("unset" + Globals.ENVIRONMENT_ITEMS[index][1]);
									} else {
										mEnvironmentTextArray[i].setText(Globals.ENVIRONMENT_ITEMS[index][1] + "=" + value);
									}
									txtLayout.addView(mEnvironmentTextArray[i], new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
								}
								envLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
								
								mEnvironmentButtonArray[index] = new Button(mActivity);
								mEnvironmentButtonArray[index].setText("Change");
								mEnvironmentButtonArray[index].setOnClickListener(new OnClickListener(){
									public void onClick(View v){
										final EditText ed = new EditText(mActivity);
										ed.setInputType(InputType.TYPE_CLASS_TEXT);
										ed.setText(Locals.EnvironmentMap.get(Globals.ENVIRONMENT_ITEMS[index][1]));
										
										AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
										alertDialogBuilder.setTitle(Globals.ENVIRONMENT_ITEMS[index][1]);
										alertDialogBuilder.setView(ed);
										alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
											public void onClick(DialogInterface dialog, int whichButton) {
												String newval = ed.getText().toString();
												Locals.EnvironmentMap.put(Globals.ENVIRONMENT_ITEMS[index][1], newval);
												mEnvironmentTextArray[index].setText(Globals.ENVIRONMENT_ITEMS[index][1] + "=" + newval);
												Settings.SaveLocals(mActivity);
											}
										});
										alertDialogBuilder.setNegativeButton(R.string.cancel, null);
										alertDialogBuilder.setCancelable(true);
										AlertDialog alertDialog = alertDialogBuilder.create();
										alertDialog.show();
									}
								});
								mEnvironmentButtonArray[index].setVisibility(value != null ? View.VISIBLE : View.GONE);
								envLayout.addView(mEnvironmentButtonArray[index], new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
							}
							mConfLayout.addView(envLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						}
					}
					mConfView.addView(mConfLayout);
				}
				addView(mConfView, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, 0, 1) );
				
				View divider = new View(mActivity);
				divider.setBackgroundColor(Color.GRAY);
				addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 2) );
				
				LinearLayout runLayout = new LinearLayout(mActivity);
				
				//don't ask me again
				LinearLayout askLayout = new LinearLayout(mActivity);
				{
					CheckBox chk = new CheckBox(mActivity);
					chk.setChecked(!Locals.AppLaunchConfigUse);
					chk.setOnClickListener(new OnClickListener(){
						public void onClick(View v){
							CheckBox c = (CheckBox)v;
							Locals.AppLaunchConfigUse = !c.isChecked();
							Settings.SaveLocals(mActivity);
						}
					});
					askLayout.addView(chk, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
					
					LinearLayout txtLayout = new LinearLayout(mActivity);
					txtLayout.setOrientation(LinearLayout.VERTICAL);
					{
						TextView txt1 = new TextView(mActivity);
						txt1.setTextSize(16.0f);
						txt1.setText(R.string.dont_ask_again);
						txtLayout.addView(txt1, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
						
						TextView txt2 = new TextView(mActivity);
						txt2.setPadding(5, 0, 0, 0);
						txt2.setText(R.string.no_applaunchconfig);
						txtLayout.addView(txt2, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
					}
					askLayout.addView(txtLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
				}
				runLayout.addView(askLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
				
				mRunButton = new Button(mActivity);
				mRunButton.setText(" " + R.string.run + " ");
				mRunButton.setTextSize(24.0f);
				mRunButton.setOnClickListener(new OnClickListener(){
					public void onClick(View v){
						runApp();
					}
				});
				runLayout.addView(mRunButton, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT) );
				
				addView(runLayout, new LinearLayout.LayoutParams( LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) );
			}
		}
	}
	
	public void runAppLaunchConfig()
	{
		if(!checkCurrentDirectory(true)){
			return;
		}
		
		Settings.LoadLocals(this);
		
		if(!Locals.AppLaunchConfigUse){
			runApp();
			return;
		}
		
		AppLaunchConfigView view = new AppLaunchConfigView(this);
		setContentView(view);
	}
	
	//
	
	private boolean checkAppNeedFiles()
	{
		String missingFileNames = "";
		int missingCount = 0;
		
		for(String fileName : Globals.APP_NEED_FILENAME_ARRAY){
			String[] itemNameArray = fileName.split("\\|");
			boolean flag = false;
			for(String itemName : itemNameArray){
				File file = new File(Globals.CurrentDirectoryPath + "/" + itemName.trim());
				if(file.exists() && file.canRead()){
					flag = true;
					break;
				}
			}
			if(!flag){
				missingCount ++;
				missingFileNames += "[" + missingCount + "]" + fileName.replace("|"," or ") + "\n";
			}
		}
		
		if(!missingFileNames.equals("")){
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setTitle(R.string.error);
			alertDialogBuilder.setMessage(R.string.following + " " + missingCount + " " + R.string.missing_file + "\n" + missingFileNames);
			alertDialogBuilder.setPositiveButton(R.string.quit, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			});
			alertDialogBuilder.setCancelable(false);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			
			return false;
		}
		
		return true;
	}
	
	public void runApp()
	{
		if(!checkCurrentDirectory(true)){
			return;
		}
		
		Settings.LoadLocals(this);
		
		if(!checkAppNeedFiles()){
			return;
		}
		
		if(mView == null){
			mView = new MainView(this);
			setContentView(mView);
		}
		mView.setFocusableInTouchMode(true);
		mView.setFocusable(true);
		mView.requestFocus();
		
		System.gc();
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true;
	}
	
    @Override
	public boolean onPrepareOptionsMenu( Menu menu )
	{
		if(mView != null){
			return mView.onPrepareOptionsMenu(menu);
		}
		return true;
	}
	
    @Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		if(mView != null){
			return mView.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onPause() {
		if( !Globals.APP_CAN_RESUME && mView != null ){
			mView.exitApp();
			try {
				wait(3000);
			} catch(InterruptedException e){}
		}
		
		_isPaused = true;
		if( mView != null )
			mView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( mView != null )
		{
			mView.onResume();
		}
		_isPaused = false;
	}

	@Override
	protected void onDestroy() 
	{
		if( mView != null ){
			mView.exitApp();
			try {
				wait(3000);
			} catch(InterruptedException e){}
		}
		super.onDestroy();
		System.exit(0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Do nothing here
	}
/*
	public void showTaskbarNotification()
	{
		showTaskbarNotification("SDL application paused", "SDL application", "Application is paused, click to activate");
	}

	// Stolen from SDL port by Mamaich
	public void showTaskbarNotification(String text0, String text1, String text2)
	{
		NotificationManager NotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
		Notification n = new Notification(R.drawable.icon, text0, System.currentTimeMillis());
		n.setLatestEventInfo(this, text1, text2, pendingIntent);
		NotificationManager.notify(NOTIFY_ID, n);
	}

	public void hideTaskbarNotification()
	{
		NotificationManager NotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationManager.cancel(NOTIFY_ID);
	}

	public int getApplicationVersion()
	{
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			System.out.println("Engine: Cannot get the version of our own package: " + e);
		}
		return 0;
	}

	static int NOTIFY_ID = 12367098; // Random ID
*/
	public static MainActivity instance = null;
	public static MainView mView = null;

	boolean _isPaused = false;
}

