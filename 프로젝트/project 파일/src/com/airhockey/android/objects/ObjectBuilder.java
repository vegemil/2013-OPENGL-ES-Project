/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.airhockey.android.objects;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;

import java.util.ArrayList;
import java.util.List;

import android.opengl.Matrix;
import android.util.FloatMath;

import com.airhockey.android.util.Geometry.Circle;
import com.airhockey.android.util.Geometry.Cube;
import com.airhockey.android.util.Geometry.Cylinder;
import com.airhockey.android.util.Geometry.Point;
import com.airhockey.android.util.Geometry.Vector;

class ObjectBuilder {
    private static final int FLOATS_PER_VERTEX = 3;

    static interface DrawCommand {
        void draw();
    }

    static class GeneratedData {
        final float[] vertexData;
        final List<DrawCommand> drawList;

        GeneratedData(float[] vertexData, List<DrawCommand> drawList) {
            this.vertexData = vertexData;
            this.drawList = drawList;
        }
    }

    static GeneratedData createPuck(Cylinder puck, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints)
                 + sizeOfOpenCylinderInVertices(numPoints);
        
        ObjectBuilder builder = new ObjectBuilder(size);

        Circle puckTop = new Circle(
            puck.center.translateY(puck.height / 2f),
            puck.radius);
        
        builder.appendCircle(puckTop, numPoints);
        //builder.appendCircle(puck, numPoints);

        return builder.build();
    }

    static GeneratedData createMallet(
        Point center, float radius, float height, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) * 2
                 + sizeOfOpenCylinderInVertices(numPoints) * 2;
        
        ObjectBuilder builder = new ObjectBuilder(size);                                      
        
        // First, generate the mallet base.
        float baseHeight = height * 0.25f;
        
        Circle baseCircle = new Circle(
            center.translateY(-baseHeight), 
            radius);
        Cylinder baseCylinder = new Cylinder(
            baseCircle.center.translateY(-baseHeight / 2f), 
            radius, baseHeight);

        builder.appendCircle(baseCircle, numPoints);
        //builder.appendCircle(baseCylinder, numPoints);
                
        // Now generate the mallet handle.
        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;
        
        Circle handleCircle = new Circle(
            center.translateY(height * 0.5f), 
            handleRadius);        
        Cylinder handleCylinder = new Cylinder(
            handleCircle.center.translateY(-handleHeight / 2f),
            handleRadius, handleHeight);                

        builder.appendCircle(handleCircle, numPoints);
        //builder.appendOpenCylinder(handleCylinder, numPoints);

        return builder.build();
    }    

    private static int sizeOfCircleInVertices(int numPoints) {
        return 1 + (numPoints + 1);
    }

    private static int sizeOfOpenCylinderInVertices(int numPoints) {
        return (numPoints + 1) * 2;
    }

    private final float[] vertexData;
    private final List<DrawCommand> drawList = new ArrayList<DrawCommand>();
    private int offset = 0;

    private ObjectBuilder(int sizeInVertices) {
        vertexData = new float[sizeInVertices * FLOATS_PER_VERTEX];
    }

    private void appendCircle(Circle circle, int numPoints) {
        final int startVertex = offset / (FLOATS_PER_VERTEX + 5);
        final int numVertices = sizeOfCircleInVertices(numPoints);

        // Center point of fan
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;
        vertexData[offset++] = 0.0f; // uv
        vertexData[offset++] = 0.0f;
        vertexData[offset++] = 0.0f; // normal
        vertexData[offset++] = 1.0f;
        vertexData[offset++] = 0.0f;

        // Fan around center point. <= is used because we want to generate
        // the point at the starting angle twice to complete the fan.
        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = 
                  ((float) i / (float) numPoints)
                * ((float) Math.PI * 2f);
            
            vertexData[offset++] = 
                  circle.center.x 
                + circle.radius * FloatMath.cos(angleInRadians);
            
            vertexData[offset++] = circle.center.y;
            
            vertexData[offset++] = 
                  circle.center.z 
                + circle.radius * FloatMath.sin(angleInRadians);
            
            vertexData[offset++] = 0.0f; // uv
            vertexData[offset++] = 0.0f;
            vertexData[offset++] = 0.0f; // normal
            vertexData[offset++] = 1.0f;
            vertexData[offset++] = 0.0f;
        }

        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                glDrawArrays(GL_TRIANGLE_FAN, startVertex,
                    numVertices);
            }
        });
    }

    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }
       
    private void appendCube(Cube cube) {
        final int startVertex = offset / (FLOATS_PER_VERTEX + 2 + 3);
        final int numVertices = 36;

        //Triangle (Front)
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;        
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;
        
        //Triangle (Back)
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
         
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;
                
        //Triangle (Left)
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        //Triangle (Right)
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = -1f;  // normal x, y, z
        vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;
        
        //Triangle (Top)
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y + cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;
        
        //Triangle (Bottom)
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
               
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x - cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z + cube.thickness * 0.5f;
        vertexData[offset++] = 0f;  vertexData[offset++] = 1f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
        
        vertexData[offset++] = cube.center.x + cube.width * 0.5f;
        vertexData[offset++] = cube.center.y - cube.height * 0.5f;
        vertexData[offset++] = cube.center.z - cube.thickness * 0.5f;
        vertexData[offset++] = 1f;  vertexData[offset++] = 0f;
        vertexData[offset++] = 0f;  // normal x, y, z
        vertexData[offset++] = -1f;
        vertexData[offset++] = 0f;
        
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {                                
                glDrawArrays(GL_TRIANGLES, startVertex, numVertices);   
            }
        });
    }
      
    static GeneratedData createDesk(Point center, float width, float height, float thickness)
    {
        int size = 4800; 
        
        ObjectBuilder builder  = new ObjectBuilder(size);      
        
        width *= 2.0f;
        height *= 0.8f;            
        
        float deskLegSize = 0.1f;                
        Point upCenter = center.translate(new Vector(0f, (height) * 0.5f, 0f));
        Point leg1Center = center.translate(new Vector(-width * 0.5f + width * deskLegSize  * 0.5f, 0.0f , thickness * 0.5f - thickness * deskLegSize));
        Point leg2Center = center.translate(new Vector(+width * 0.5f - width * deskLegSize  * 0.5f, 0.0f , thickness * 0.5f - thickness * deskLegSize));
        Point leg3Center = center.translate(new Vector(-width * 0.5f + width * deskLegSize  * 0.5f, 0.0f , -(thickness * 0.5f - thickness * deskLegSize)));
        Point leg4Center = center.translate(new Vector(+width * 0.5f - width * deskLegSize  * 0.5f, 0.0f , -(thickness * 0.5f - thickness * deskLegSize)));
        
        //중심점을 책상의 중심으로 이동 
        Cube deskUpper = new Cube(upCenter , width ,height * 0.08f, thickness);                      
        Cube deskLeg_1 = new Cube(leg1Center, width * deskLegSize * 0.8f , height, thickness * deskLegSize);
        Cube deskLeg_2 = new Cube(leg2Center, -width * deskLegSize * 0.8f , height, thickness * deskLegSize); 
        Cube deskLeg_3 = new Cube(leg3Center , width * deskLegSize * 0.8f , height, -thickness * deskLegSize);
        Cube deskLeg_4 = new Cube(leg4Center , -width * deskLegSize * 0.8f , height, -thickness * deskLegSize); 
        
        builder.appendCube(deskUpper);
        builder.appendCube(deskLeg_1);                     
        builder.appendCube(deskLeg_2);
        builder.appendCube(deskLeg_3);
        builder.appendCube(deskLeg_4);
       
        return builder.build();
    }
    
    static GeneratedData createChair(Point center, float width, float height, float thickness)
    {
        int size = 1200;
        float scale = 0.8f;
        ObjectBuilder builder  = new ObjectBuilder(size);
        height *= 0.6 * scale;
        width *= 0.8 * scale;
        thickness *= 0.8 * scale;                
        
        float deskLegSize = 0.1f;                
        Point upCenter = center.translate(new Vector(0f, (height) * 0.5f, 0f));
        Point leg1Center = center.translate(new Vector(-width * 0.5f + width * deskLegSize  * 0.5f, height  * 0.5f, thickness * 0.5f - thickness * deskLegSize));
        Point leg2Center = center.translate(new Vector(+width * 0.5f - width * deskLegSize  * 0.5f, height * 0.5f , thickness * 0.5f - thickness * deskLegSize));
        Point leg3Center = center.translate(new Vector(-width * 0.5f + width * deskLegSize  * 0.5f, 0.0f , -(thickness * 0.5f - thickness * deskLegSize)));
        Point leg4Center = center.translate(new Vector(+width * 0.5f - width * deskLegSize  * 0.5f, 0.0f , -(thickness * 0.5f - thickness * deskLegSize)));
        Point backCenter = center.translate(new Vector(0f, height * 1.3f, thickness * 0.5f - thickness * deskLegSize));
        
        //중심점을 책상의 중심으로 이동 
        Cube deskUpper = new Cube(upCenter , width ,height * 0.08f, thickness);                      
        Cube deskLeg_1 = new Cube(leg1Center, width * deskLegSize * 0.8f , height * 2.0f, thickness * deskLegSize);
        Cube deskLeg_2 = new Cube(leg2Center, -width * deskLegSize * 0.8f , height * 2.0f, thickness * deskLegSize); 
        Cube deskLeg_3 = new Cube(leg3Center , width * deskLegSize * 0.8f , height, -thickness * deskLegSize);
        Cube deskLeg_4 = new Cube(leg4Center , -width * deskLegSize * 0.8f , height, -thickness * deskLegSize);
        Cube deskBack = new Cube(backCenter, width, height * 0.5f, thickness * deskLegSize);
        
        builder.appendCube(deskUpper);
        builder.appendCube(deskLeg_1);           //Left Front leg                     
        builder.appendCube(deskLeg_2);        //Right Front leg
        builder.appendCube(deskLeg_3);          //Left Back leg
        builder.appendCube(deskLeg_4);        //Right Back leg
        builder.appendCube(deskBack);     
               
        return builder.build();
    }
    
    static GeneratedData createStand(Point center, float radius, float height, int numPoints) 
    {
        int size = 1000;
        
        ObjectBuilder builder = new ObjectBuilder(size);                                      
        
        radius *= 0.3f;
        height *= 0.3f;
        
        // First, generate the mallet base.
        float baseHeight = height * 0.25f;
        
        Circle baseCircle = new Circle(center.translateY(-baseHeight), radius * 0.7f);
        Cylinder baseCylinder = new Cylinder(baseCircle.center.translateY(-baseHeight / 2f), radius * 0.7f , baseHeight);

        builder.appendCircle(baseCircle, numPoints);
        builder.appendOpenCylinder(baseCylinder, numPoints, 0 ,0, 0);
                
        // Now generate the mallet handle.
        float handleHeight = height;
        float handleRadius = radius / 4f;
        
        Circle standNeckCircle = new Circle(center.translateY(height * 0.5f), handleRadius);        
        Cylinder standNeckCylinder_1 = new Cylinder(standNeckCircle.center.translateY(-handleHeight / 2f), handleRadius, handleHeight);                
        Cylinder standNeckCylinder_2 = new Cylinder(standNeckCircle.center.translate(new Vector(-0.05f, 0.1f, 0f)),
                                                                                    handleRadius, handleHeight);
        Cylinder standNeckCylinder_3 = new Cylinder(standNeckCircle.center.translate(new Vector(-0.35f, 0.05f, 0f)), handleRadius, handleHeight * 1.2f);
        
        //builder.appendCircle(standNeckCircle, numPoints);
        builder.appendOpenCylinder(standNeckCylinder_1, numPoints, 0, 0, 0);
        builder.appendOpenCylinder(standNeckCylinder_2, numPoints, 0, 0, -20.0f);
        builder.appendOpenCylinder(standNeckCylinder_3, numPoints, 0, 0, -90.0f);

        return builder.build();
    }    
    
    private void appendOpenCylinder(Cylinder cylinder, int numPoints, float x, float y, float z) {
        final int startVertex = offset / (FLOATS_PER_VERTEX + 5);
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        final float yStart = cylinder.center.y - (cylinder.height / 2f);
        final float yEnd = cylinder.center.y + (cylinder.height / 2f);
        
        float[] vector = new float[8];
        float[] matModel = new float[16];

        // Generate strip around center point. <= is used because we want to
        // generate the points at the starting angle twice, to complete the
        // strip.
        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = 
                  ((float) i / (float) numPoints)
                * ((float) Math.PI * 2f);
            
            float xPosition = 
                  cylinder.center.x 
                + cylinder.radius * FloatMath.cos(angleInRadians);
            
            float zPosition = 
                  cylinder.center.z 
                + cylinder.radius * FloatMath.sin(angleInRadians);

//            vertexData[offset++] = xPosition;
//            vertexData[offset++] = yStart;
//            vertexData[offset++] = zPosition;
//
//            vertexData[offset++] = xPosition;
//            vertexData[offset++] = yEnd;
//            vertexData[offset++] = zPosition;
            
            
            vector[0] = xPosition;
            vector[1] = yStart;
            vector[2] = zPosition;
            vector[3] = 0.0f;
            
            vector[4] = xPosition;
            vector[5] = yEnd;
            vector[6] = zPosition;
            vector[7] = 0;            
             
            setIdentityM(matModel, 0);            
            rotateM(matModel, 0, x, 1f, 0f, 0f);
            rotateM(matModel, 0, y, 0f, 1f, 0f);
            rotateM(matModel, 0, z, 0f, 0f, 1f);
           Matrix.multiplyMV(vector, 0, matModel, 0, vector, 0);
           Matrix.multiplyMV(vector, 4, matModel, 0, vector, 4);
           
           // Normal Calc                     
           Vector vecNormal = new Vector(xPosition - cylinder.center.x, 0, zPosition - cylinder.center.z);
           vecNormal = vecNormal.scale(1.0f / vecNormal.length());
           
           vertexData[offset++] = vector[0];          
           vertexData[offset++] = vector[1];
           vertexData[offset++] = vector[2];
           vertexData[offset++] = 0.0f; // uv
           vertexData[offset++] = 0.0f;
           vertexData[offset++] = vecNormal.x; // normal
           vertexData[offset++] = vecNormal.y;
           vertexData[offset++] = vecNormal.z;
           
           
           vertexData[offset++] = vector[4];
           vertexData[offset++] = vector[5];           
           vertexData[offset++] = vector[6];
           vertexData[offset++] = 0.0f; // uv
           vertexData[offset++] = 0.0f;
           vertexData[offset++] = vecNormal.x; // normal
           vertexData[offset++] = vecNormal.y;
           vertexData[offset++] = vecNormal.z;
           
            
        }
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {                
                glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices);
            }
        });        
    }

    static GeneratedData createNotebookUpper(Point center, float width, float height, float thickness)
    {
        int size = 200; 
        
        ObjectBuilder builder  = new ObjectBuilder(size);           
                  
        Point upCenter = center.translate(new Vector(0f, (height) * 0.5f, 0f));
        
        //중심점을 책상의 중심으로 이동 
        Cube deskUpper = new Cube(upCenter , width ,height, thickness);                      
        
        builder.appendCube(deskUpper);
               
        return builder.build();
    }
    
    static GeneratedData createNotebookKeyboard(Point center, float width, float height, float thickness)
    {
        int size = 200; 
        
        ObjectBuilder builder  = new ObjectBuilder(size);           
                  
        Point upCenter = center.translate(new Vector(0f, (height) * 0.5f, 0f));
        
        //중심점을 책상의 중심으로 이동 
        Cube deskUpper = new Cube(upCenter , width ,height, thickness);                      
        
        builder.appendCube(deskUpper);
               
        return builder.build();
    }

}
