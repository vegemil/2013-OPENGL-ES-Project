/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/

package com.airhockey.android.objects;

import static com.airhockey.android.Constants.BYTES_PER_FLOAT;

import java.util.List;

import com.airhockey.android.data.VertexArray;
import com.airhockey.android.objects.ObjectBuilder.DrawCommand;
import com.airhockey.android.objects.ObjectBuilder.GeneratedData;
import com.airhockey.android.programs.ColorShaderProgram;
import com.airhockey.android.programs.TextureShaderProgram;
import com.airhockey.android.util.Geometry.Point;

public class Stand {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int NORMAL_COMPONENT_COUNT = 3;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT)
        * BYTES_PER_FLOAT;

    public final float radius;
    public final float height;

    private final VertexArray vertexArray;
    private final List<DrawCommand> drawList;

    public Stand(float radius, float height, int numPointsAroundMallet) {
        GeneratedData generatedData = ObjectBuilder.createStand(new Point(0f, 0f, 0f), radius, height, numPointsAroundMallet);

        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
    }

    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
            0, 
            textureProgram.getPositionAttributeLocation(), 
            POSITION_COMPONENT_COUNT,
            STRIDE);
        
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT, 
            textureProgram.getTextureCoordinatesAttributeLocation(),
            TEXTURE_COORDINATES_COMPONENT_COUNT, 
            STRIDE);
        
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT, 
            textureProgram.getNormalAttributeLocation(), 
            NORMAL_COMPONENT_COUNT, 
            STRIDE);  
    }

    public void draw() {
        for (DrawCommand drawCommand : drawList) {
            drawCommand.draw();
        }
    }
}
