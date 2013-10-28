/*
 2012/7 Created by AKIZUKI Katane
 */

package com.codeplex.sdlpal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.InputDevice;

import android.os.Build;

public class AndroidGingerBreadAPI
{
	public static int MotionEventGetSource(final MotionEvent event)
	{
		return event.getSource();
	}
	
	public static float MotionEventGetAxisValue(final MotionEvent event, final int axis)
	{
		return event.getAxisValue(axis);
	}
}
