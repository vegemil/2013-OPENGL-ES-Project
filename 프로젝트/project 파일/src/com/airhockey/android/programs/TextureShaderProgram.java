/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.airhockey.android.programs;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import android.content.Context;
import com.airhockey.android.R;

public class TextureShaderProgram extends ShaderProgram {
    // Uniform locations
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;
    private final int uVectorToLightLocation;
    private final int uPointLightPositionsLocation;
    private final int uPointLightColorsLocation;
    
    // Attribute locations
    private final int aPositionLocation;
    private final int aTextureCoordinatesLocation;
    private final int aNormalLocation;

    public TextureShaderProgram(Context context) {
        super(context, R.raw.texture_vertex_shader,             R.raw.texture_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
        uVectorToLightLocation = glGetUniformLocation(program, U_VECTOR_TO_LIGHT);
        uPointLightPositionsLocation = glGetUniformLocation(program, U_POINT_LIGHT_POSITIONS);
        uPointLightColorsLocation = glGetUniformLocation(program, U_POINT_LIGHT_COLORS);
        
        // Retrieve attribute locations for the shader program.               
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
        aNormalLocation = glGetAttribLocation(program, A_NORMAL);
    }

    public void setUniforms(float[] matrix, int textureId, 
        float[] vectorToDirectionalLight, 
        float[] pointLightPositions,
        float[] pointLightColors)
    {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        glUniform3fv(uVectorToLightLocation, 1, vectorToDirectionalLight, 0);
        
        glUniform4fv(uPointLightPositionsLocation, 3, pointLightPositions, 0);            
        glUniform3fv(uPointLightColorsLocation, 3, pointLightColors, 0);

        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0);
    }
    
    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }
    
    public int getNormalAttributeLocation() {
        return aNormalLocation;
    }
}