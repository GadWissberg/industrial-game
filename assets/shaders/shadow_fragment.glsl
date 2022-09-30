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

varying vec3 v_normal;

varying vec4 v_position;
varying vec4 v_positionLightTrans;
uniform float u_maxBias;
uniform float u_minBias;
void main()
{
    // Default is to not add any color
    float intensity=0.08;
    // Vector light-current position
    vec3 lightDirection=v_position.xyz-u_lightPosition;
    float lenToLight=length(lightDirection)/u_cameraFar;
    lightDirection = normalize(lightDirection);
    // By default assume shadow
    float lenDepthMap=-1.0;

    // Directional light, check if in field of view and get the depth
    if (u_type==1.0){
        vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w)*0.5+0.5;
        if (v_positionLightTrans.z>=0.0 && (depth.x >= 0.0) && (depth.x <= 1.0) && (depth.y >= 0.0) && (depth.y <= 1.0)) {
            lenDepthMap = texture2D(u_depthMapDir, depth.xy).a;
        }
    }
    // Point light, just get the depth given light vector
    else if (u_type==0.0){
        lenDepthMap = textureCube(u_depthMapCube, lightDirection).a;
    }

    float bias = max(u_maxBias * (1.0 - dot(v_normal, lightDirection)), u_minBias);
    if (lenDepthMap>lenToLight - bias && lenToLight < u_radius*0.1){
        float attenuation = 16.0 / ((128.0*lenToLight) + (256.0*lenToLight*lenToLight) + (128.0*lenToLight*lenToLight*lenToLight));
        intensity += attenuation;
    }

    gl_FragColor     = vec4(intensity);

}

