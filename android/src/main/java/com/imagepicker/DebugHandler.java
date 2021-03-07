package com.imagepicker;

import android.util.Log;

public class DebugHandler {

    public static void logException(Exception e){
        e.printStackTrace();
    }

    public static void log(String text){
            Log.d("Log", text);
    }
}
