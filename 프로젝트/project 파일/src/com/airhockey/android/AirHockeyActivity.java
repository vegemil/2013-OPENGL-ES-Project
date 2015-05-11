/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.airhockey.android;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.widget.Toast;

public class AirHockeyActivity extends Activity {
    /**
     * Hold a reference to our GLSurfaceView
     */
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);

        // Check if the system supports OpenGL ES 2.0.
        ActivityManager activityManager = 
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager
            .getDeviceConfigurationInfo();
        // Even though the latest emulator supports OpenGL ES 2.0,
        // it has a bug where it doesn't set the reqGlEsVersion so
        // the above check doesn't work. The below will detect if the
        // app is running on an emulator, and assume that it supports
        // OpenGL ES 2.0.
        final boolean supportsEs2 =
            configurationInfo.reqGlEsVersion >= 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                 && (Build.FINGERPRINT.startsWith("generic")
                  || Build.FINGERPRINT.startsWith("unknown")
                  || Build.MODEL.contains("google_sdk")
                  || Build.MODEL.contains("Emulator")
                  || Build.MODEL.contains("Android SDK built for x86")));

        final AirHockeyRenderer airHockeyRenderer = new AirHockeyRenderer(this);
        
        if (supportsEs2) {
            // ...
            // Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);

            // Assign our renderer.
            glSurfaceView.setRenderer(airHockeyRenderer);
            rendererSet = true;
        } else {
            /*
             * This is where you could create an OpenGL ES 1.x compatible
             * renderer if you wanted to support both ES 1 and ES 2. Since 
             * we're not doing anything, the app will crash if the device 
             * doesn't support OpenGL ES 2.0. If we publish on the market, we 
             * should also add the following to AndroidManifest.xml:
             * 
             * <uses-feature android:glEsVersion="0x00020000"
             * android:required="true" />
             * 
             * This hides our app from those devices which don't support OpenGL
             * ES 2.0.
             */
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
                Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(glSurfaceView);
    }
    
    // 드래그 모드인지 핀치줌 모드인지 구분
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
        
    // 드래그시 좌표 저장
    int posX1=0, posX2=0, posY1=0, posY2=0;
        
    // 핀치시 두좌표간의 거리 저장
    float oldDist = 1f;
    float newDist = 1f;    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int act = event.getAction();
        String strMsg = "";
      
        switch(act & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:    //첫번째 손가락 터치(드래그 용도)
                posX1 = (int) event.getX();
                posY1 = (int) event.getY();                        
                mode = DRAG;                                
                break;
            case MotionEvent.ACTION_MOVE: 
                if(mode == DRAG) {  // 드래그 중
                    posX2 = (int) event.getX();
                    posY2 = (int) event.getY();
         
                    if(Math.abs(posX2-posX1)>20 || Math.abs(posY2-posY1)>20) {
                        posX1 = posX2;
                        posY1 = posY2;                                  
                        
                    }
                } else if (mode == ZOOM) {    // 핀치 중
                    newDist = spacing(event);         
         
                    if (newDist - oldDist > 20) { // zoom in
                        AirHockeyRenderer.camPosZ += (newDist - oldDist) * 0.003f;
                        
                        oldDist = newDist;                                                
          
                        strMsg = "zoom in";
                        Toast toast = Toast.makeText(this, strMsg, Toast.LENGTH_SHORT);
                        //toast.show();
                    } else if(oldDist - newDist > 20) { // zoom out
                        AirHockeyRenderer.camPosZ -= (oldDist - newDist) * 0.003f;
                        oldDist = newDist;                                                
          
                        strMsg = "zoom out";                        
                        Toast toast = Toast.makeText(this, strMsg, Toast.LENGTH_SHORT);
                        //toast.show();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:    // 첫번째 손가락을 떼었을 경우
            case MotionEvent.ACTION_POINTER_UP:  // 두번째 손가락을 떼었을 경우
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:  
            //두번째 손가락 터치(손가락 2개를 인식하였기 때문에 핀치 줌으로 판별)
                mode = ZOOM;
        
                newDist = spacing(event);
                oldDist = spacing(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            default : 
                break;
        }
      
        return super.onTouchEvent(event);            
    }
    
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }    

    @Override
    protected void onPause() {
        super.onPause();
        
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }
}