uniform mat4 u_Matrix;

uniform vec3 u_VectorToLight;             // In eye space
uniform vec4 u_PointLightPositions[3];    // In eye space
uniform vec3 u_PointLightColors[3];

attribute vec4 a_Position;  
attribute vec2 a_TextureCoordinates;
attribute vec3 a_Normal;

varying vec2 v_TextureCoordinates;
varying vec3 v_Color;


vec3 getAmbientLighting();
vec3 getDirectionalLighting();
vec3 getPointLighting();

void main()                    
{            
	v_Color = vec3(1.0, 1.0, 1.0);		
	
	v_Color = getDirectionalLighting() + getPointLighting() + getAmbientLighting();		
	                
    v_TextureCoordinates = a_TextureCoordinates;	  	  
    gl_Position = u_Matrix * a_Position;    
}        


vec3 getAmbientLighting() 
{    
    return vec3(0.2, 0.2, 0.2);      
}

vec3 getDirectionalLighting()
{   
    return v_Color  
         * max(dot(a_Normal, u_VectorToLight), 0.0);       
}

vec3 getPointLighting()
{
    vec3 lightingSum = vec3(0.0);    
    
    for (int i = 0; i < 1; i++) 
    {              
	    vec3 toPointLight = vec3(u_PointLightPositions[i]) - vec3(a_Position);          
        float distance = length(toPointLight);
        toPointLight = normalize(toPointLight);        
        
        float cosine = max(dot(a_Normal, toPointLight), 0.0); 
        lightingSum += (v_Color * u_PointLightColors[i] * 5.0 * cosine) / (distance);
                  
        
    } 
     
    
    return lightingSum;       
}
   