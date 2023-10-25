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
uniform float u_depthMapSize;
uniform float u_cameraFar;
uniform vec3 u_lightPosition;
uniform float u_radius;
uniform float u_maxBias;
uniform float u_minBias;
uniform vec3 u_lightColor;
uniform float u_intensity;
uniform vec2 u_playerScreenCoords;
uniform vec2 u_mouseScreenCoords;

varying vec3 v_normal;
varying vec4 v_position;
varying vec4 v_positionLightTrans;

const float X_RAY_RADIUS = 50.0;

bool fragmentExposedToLight(vec3 lightDirection, vec3 offset, float lenToLight, float bias){
    return (textureCube(u_depthMapCube, lightDirection + offset).a)>lenToLight - bias;
}

bool shouldDiscardFragment(){
    return !gl_FrontFacing
    || (u_playerScreenCoords != vec2(0.0) && length(u_playerScreenCoords.xy - gl_FragCoord.xy) < X_RAY_RADIUS)
    || (u_mouseScreenCoords != vec2(0.0) && length(u_mouseScreenCoords.xy - gl_FragCoord.xy) < X_RAY_RADIUS);
}

float applyXRayFading(vec2 coords){
    float distance = length(coords.xy - gl_FragCoord.xy);
    float minDistance = X_RAY_RADIUS;
    float maxDistance = 75.0;
    float alpha = smoothstep(minDistance, maxDistance, distance);
    alpha = mix(1.0, alpha, float(length(coords) > 0.0));
    return alpha;
}

void main()
{
    if (shouldDiscardFragment()){
        discard;
    }

    float final_intensity=0.00;
    vec3 lightDirection=v_position.xyz-u_lightPosition;
    float lenToLight=length(lightDirection)/u_cameraFar;
    lightDirection = normalize(lightDirection);
    float lenDepthMap=-1.0;
    vec3 color = vec3(1.0);
    float bias = max(u_maxBias * (1.0 - dot(v_normal, lightDirection)), u_minBias);
    float pcfSum = 0.0;

    if (abs(v_normal.y) - 0.01 >= 0){
        for (int y = -1; y<=1; y++){
            for (int x = -1; x<=1; x++){
                vec3 offset = vec3(x, 0, y) * (1.0/u_depthMapSize);
                if (fragmentExposedToLight(lightDirection, offset, lenToLight, bias)){
                    pcfSum++;
                }
            }
        }
    } else {
        if (fragmentExposedToLight(lightDirection, vec3(0.0), lenToLight, bias)){
            pcfSum = 9.0;
        }
    }

    final_intensity=(u_intensity)*(1.0-((lenToLight)/(u_radius/u_cameraFar)))*((pcfSum)/9.0);
    color = u_lightColor;
    gl_FragColor = vec4(color.r*final_intensity, color.g*final_intensity, color.b*final_intensity, final_intensity);

    gl_FragColor.rgba *= applyXRayFading(u_playerScreenCoords);
    gl_FragColor.rgba *= applyXRayFading(u_mouseScreenCoords);
}

