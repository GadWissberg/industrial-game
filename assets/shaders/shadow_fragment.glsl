#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

uniform sampler2D u_depthMapDir;
uniform samplerCube u_depthMapCube;
uniform float u_cameraFar;
uniform vec3 u_lightPosition;
uniform float u_type;
uniform float u_radius;
uniform float u_maxBias;
uniform float u_minBias;
uniform vec3 u_lightColor;
uniform float u_intensity;

varying vec3 v_normal;
varying vec4 v_position;
varying vec4 v_positionLightTrans;

void main()
{
    float final_intensity=0.00;
    vec3 lightDirection=v_position.xyz-u_lightPosition;
    float lenToLight=length(lightDirection)/u_cameraFar;
    lightDirection = normalize(lightDirection);
    float lenDepthMap=-1.0;
    if (u_type==1.0){
        vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w)*0.5+0.5;
        if (v_positionLightTrans.z>=0.0 && (depth.x >= 0.0) && (depth.x <= 1.0) && (depth.y >= 0.0) && (depth.y <= 1.0)) {
            lenDepthMap = texture2D(u_depthMapDir, depth.xy).a;
        }
    }
    else if (u_type==0.0){
        lenDepthMap = textureCube(u_depthMapCube, lightDirection).a;
    }
    vec3 color = vec3(1.0);
    float bias = max(u_maxBias * (1.0 - dot(v_normal, lightDirection)), u_minBias);
    if (lenDepthMap>lenToLight - bias){
        final_intensity=(u_intensity)*(1.0-((lenToLight)/(u_radius/u_cameraFar)));
        color = u_lightColor;
    }
    gl_FragColor = vec4(color.r*final_intensity, color.g*final_intensity, color.b*final_intensity, final_intensity);
}

