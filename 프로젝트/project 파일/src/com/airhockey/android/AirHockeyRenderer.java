/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.airhockey.android;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRONT_AND_BACK;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import com.airhockey.android.objects.Chair;
import com.airhockey.android.objects.Desk;

import com.airhockey.android.objects.Background;
import com.airhockey.android.objects.Mallet;
import com.airhockey.android.objects.NoteBook_Keyboard;
import com.airhockey.android.objects.NoteBook_Upper;
import com.airhockey.android.objects.Puck;
import com.airhockey.android.objects.Stand;
import com.airhockey.android.objects.Table;
import com.airhockey.android.programs.ColorShaderProgram;
import com.airhockey.android.programs.TextureShaderProgram;
import com.airhockey.android.util.Geometry;
import com.airhockey.android.util.Geometry.Plane;
import com.airhockey.android.util.Geometry.Point;
import com.airhockey.android.util.Geometry.Ray;
import com.airhockey.android.util.Geometry.Sphere;
import com.airhockey.android.util.Geometry.Vector;
import com.airhockey.android.util.MatrixHelper;
import com.airhockey.android.util.TextureHelper;

public class AirHockeyRenderer implements Renderer {    
    private final Context context;

    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    
    private float rotAngle = 0.0f;
    
    public static float camPosZ = 0.0f;
    
    
    // light        
    final float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};
    
    private final float[] pointLightPositions = new float[]
        {-3f, 4f, +5.0f, 1f,
          0f, 1f, 0f, 1f,
          1f, 1f, 0f, 1f};
    
    private final float[] pointLightColors = new float[]
        {1.00f, 1.00f, 0.8784313725490196f,         
         0.02f, 0.25f, 0.02f, 
         0.02f, 0.20f, 1.00f};

    private Table table;
    private Mallet mallet;
    private Puck puck;       
    
    private Desk desk;
    private Chair chair;
    private Stand stand;
    private NoteBook_Keyboard noteKeyboard;
    private NoteBook_Upper noteUpper;
    private Background background;

    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;

    private int texture;
    private int texture_wood;
    private int texture_noteUpper;
    private int texture_noteKeyboard;
    private int texture_chair;
    private int texture_stand;

    private boolean blueMalletPressed = false;
    private boolean redMalletPressed = false;
    private Point blueMalletPosition;    
    private Point redMalletPostion;
    
    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f;
    
    private Point previousBlueMalletPosition;
    private Point previousRedMalletPostion;
    
    private Point puckPosition;
    private Vector puckVector;

    public AirHockeyRenderer(Context context) {
        this.context = context;
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        
        Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        // Now test if this ray intersects with the mallet by creating a
        // bounding sphere that wraps the mallet.
        Sphere malletBoundingSphere_1 = new Sphere(new Point(
                blueMalletPosition.x, 
                blueMalletPosition.y, 
                blueMalletPosition.z),
            mallet.height / 2f);
        
        Sphere malletBoundingSphere_2 = new Sphere(new Point(
                redMalletPostion.x,
                redMalletPostion.y,
                redMalletPostion.z),
            mallet.height / 2f);

        // If the ray intersects (if the user touched a part of the screen that
        // intersects the mallet's bounding sphere), then set malletPressed =
        // true.
        blueMalletPressed = Geometry.intersects(malletBoundingSphere_1, ray);
        redMalletPressed = Geometry.intersects(malletBoundingSphere_2, ray);
    }
    
    private Ray convertNormalized2DPointToRay(
        float normalizedX, float normalizedY) {
        // We'll convert these normalized device coordinates into world-space
        // coordinates. We'll pick a point on the near and far planes, and draw a
        // line between them. To do this transform, we need to first multiply by
        // the inverse matrix, and then we need to undo the perspective divide.
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc =  {normalizedX, normalizedY,  1, 1};
        
        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        multiplyMV(
            nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(
            farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        // Why are we dividing by W? We multiplied our vector by an inverse
        // matrix, so the W value that we end up is actually the *inverse* of
        // what the projection matrix would create. By dividing all 3 components
        // by W, we effectively undo the hardware perspective divide.
        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        // We don't care about the W value anymore, because our points are now
        // in world coordinates.
        Point nearPointRay = 
            new Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
			
        Point farPointRay = 
            new Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        return new Ray(nearPointRay, 
                       Geometry.vectorBetween(nearPointRay, farPointRay));
    }        

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    
    public void handleTouchDrag(float normalizedX, float normalizedY) {
        
        if (blueMalletPressed) {
            Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            // Define a plane representing our air hockey table.
            Plane plane = new Plane(new Point(0, 0, 0), new Vector(0, 1, 0));
            // Find out where the touched point intersects the plane
            // representing our table. We'll move the mallet along this plane.
            Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            // Clamp to bounds                        
                        
            previousBlueMalletPosition = blueMalletPosition;            
            /*
            blueMalletPosition =
                new Point(touchedPoint.x, mallet.height / 2f, touchedPoint.z);
            */
            // Clamp to bounds            
            blueMalletPosition = new Point(
                clamp(touchedPoint.x, 
                      leftBound + mallet.radius, 
                      rightBound - mallet.radius),
                mallet.height / 2f, 
                clamp(touchedPoint.z, 
                      0f + mallet.radius, 
                      nearBound - mallet.radius));            
            
            // Now test if mallet has struck the puck.
            float distance = 
                Geometry.vectorBetween(blueMalletPosition, puckPosition).length();
            
            if (distance < (puck.radius + mallet.radius)) {
                // The mallet has struck the puck. Now send the puck flying
                // based on the mallet velocity.
                puckVector = Geometry.vectorBetween(
                    previousBlueMalletPosition, blueMalletPosition);                
            }
        }
        if (redMalletPressed) {
            Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            // Define a plane representing our air hockey table.
            Plane plane = new Plane(new Point(0, 0, 0), new Vector(0, 1, 0));
            // Find out where the touched point intersects the plane
            // representing our table. We'll move the mallet along this plane.
            Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            // Clamp to bounds                        
                        
            previousRedMalletPostion = redMalletPostion;            
            /*
            redMalletPostion =
                new Point(touchedPoint.x, mallet.height / 2f, touchedPoint.z);
            */
            // Clamp to bounds            
            redMalletPostion = new Point(
                clamp(touchedPoint.x, 
                      leftBound + mallet.radius, 
                      rightBound - mallet.radius),
                mallet.height / 2f, 
                clamp(touchedPoint.z, 
                      farBound + mallet.radius, 
                      0 - mallet.radius));            
            
            // Now test if mallet has struck the puck.
            float distance = 
                Geometry.vectorBetween(redMalletPostion, puckPosition).length();
            
            if (distance < (puck.radius + mallet.radius)) {
                // The mallet has struck the puck. Now send the puck flying
                // based on the mallet velocity.
                puckVector = Geometry.vectorBetween(
                    previousRedMalletPostion, redMalletPostion);                
            }
        }
        
    }
    
    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {                          
        
        gl.glDisable(GL10.GL_DITHER);
        gl.glEnable(GL_TEXTURE_2D);
        gl.glShadeModel(GL10.GL_SMOOTH);
        
        glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
            GL10.GL_NICEST); 

        
        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);
       
        desk = new Desk(1.0f, 1.0f, 1.0f);
        chair = new Chair(1.0f, 1.0f, 1.0f);
        stand = new Stand(1.0f, 1.0f, 16);
        
        noteKeyboard = new NoteBook_Keyboard(0.4f, 0.01f, 0.3f);
        noteUpper = new NoteBook_Upper(0.4f, 0.3f, 0.01f);
        background = new Background();
        
        blueMalletPosition = new Point(0f, mallet.height / 2f, 0.4f);
        redMalletPostion = new Point(0f, mallet.height / 2f, -0.4f);
        puckPosition = new Point(0f, puck.height / 2f, 0f);
        puckVector = new Vector(0f, 0f, 0f);

        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);

        texture = TextureHelper.loadTexture(context, R.drawable.back5);
        texture_wood = TextureHelper.loadTexture(context, R.drawable.wood_1);
        texture_chair = TextureHelper.loadTexture(context, R.drawable.chair);
        texture_noteKeyboard = TextureHelper.loadTexture(context, R.drawable.keyboard);
        texture_noteUpper = TextureHelper.loadTexture(context, R.drawable.shot);
        texture_stand = TextureHelper.loadTexture(context, R.drawable.stand);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {                
        // Set the OpenGL viewport to fill the entire surface.
        
        glEnable( GL_DEPTH_TEST );
        
        glViewport(0, 0, width, height);        

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
            / (float) height, 1f, 10f);

        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);        
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL10.GL_LIGHTING);
        
        // Translate the puck by its vector
        puckPosition = puckPosition.translate(puckVector);
                
        // If the puck struck a side, reflect it off that side.
        if (puckPosition.x < leftBound + puck.radius
         || puckPosition.x > rightBound - puck.radius) {
            puckVector = new Vector(-puckVector.x, puckVector.y, puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }        
        if (puckPosition.z < farBound + puck.radius
         || puckPosition.z > nearBound - puck.radius) {
            puckVector = new Vector(puckVector.x, puckVector.y, -puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }        
        // Clamp the puck position.
        puckPosition = new Point(
            clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius),
            puckPosition.y,
            clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius)
        );
        
        // Friction factor
        puckVector = puckVector.scale(0.99f);

        // Update the viewProjection matrix, and create an inverted matrix for
        // touch picking.
        
        float[] matView = new float[16];                
        Matrix.translateM(matView, 0, viewMatrix, 0, 0, 0, camPosZ);
        
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, matView, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);
        


        // Draw the mallets.
        positionObjectInScene(redMalletPostion.x, redMalletPostion.y, redMalletPostion.z);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        //mallet.draw();                        
  
        
        positionObjectInScene(0f, 1f, -1f);
        scaleObject(7.3f, 5, 1);
        textureProgram.useProgram();
        textureProgram.setUniforms(modelViewProjectionMatrix, texture, vectorToLight, pointLightPositions, pointLightColors);
        background.bindData(textureProgram);
        background.draw();
        
        // test
//        float[] viewRotMatrix = new float[16];
//        Matrix.setIdentityM(viewRotMatrix, 0);
//        rotAngle++;
//        Matrix.rotateM(viewRotMatrix, 0, rotAngle, 0, 1, 0);
//        Matrix.multiplyMM(viewProjectionMatrix, 0, viewRotMatrix, 0, viewProjectionMatrix, 0);
        
        positionObjectInScene(0f, -0.15f, 0f);        
        textureProgram.useProgram();
        textureProgram.setUniforms(modelViewProjectionMatrix, texture_chair, vectorToLight, pointLightPositions, pointLightColors);
        desk.bindData(textureProgram);
        desk.draw();
        
        positionObjectInScene(0f, -0.15f, 0.6f);        
        textureProgram.useProgram();        
        textureProgram.setUniforms(modelViewProjectionMatrix, texture_wood, vectorToLight, pointLightPositions, pointLightColors);
        chair.bindData(textureProgram);
        chair.draw();
        
        positionObjectInScene(-0.7f, 0.45f, -0.3f);       
        textureProgram.useProgram();        
        textureProgram.setUniforms(modelViewProjectionMatrix, texture_stand, vectorToLight, pointLightPositions, pointLightColors);
        stand.bindData(textureProgram);
        stand.draw();   
        
        
        positionObjectInScene(0.5f, 0.45f, 0.4f);      
        roateObject(0, -45, 0);
        textureProgram.useProgram();        
        textureProgram.setUniforms(modelViewProjectionMatrix, texture_noteKeyboard, vectorToLight, pointLightPositions, pointLightColors);
        noteKeyboard.bindData(textureProgram);
        noteKeyboard.draw();
        
        
        positionObjectInScene(0.64f, 0.42f, 0.2f);     
        roateObject(0, -45, 0);
        textureProgram.useProgram();        
        textureProgram.setUniforms(modelViewProjectionMatrix, texture_noteUpper, vectorToLight, pointLightPositions, pointLightColors);
        noteUpper.bindData(textureProgram);
        noteUpper.draw();
    }

    private void positionTableInScene() {
        // The table is defined in terms of X & Y coordinates, so we rotate it
        // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0);
        //rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
            0, modelMatrix, 0);
    }

    // The mallets and the puck are positioned on the same plane as the table.
    private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
            0, modelMatrix, 0);
    }
    
    private void scaleObject(float x, float y, float z) {                
        Matrix.scaleM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }
    
    private void roateObject(float x, float y, float z)
    {        
        Matrix.rotateM(modelMatrix, 0, x, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, y, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, z, 0, 0, 1);        
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }
}