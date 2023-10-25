#extension GL_EXT_gpu_shader4 : enable
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

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#ifdef normalFlag
varying vec3 v_normal;
#endif//normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif//alphaTestFlag
#endif//blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if    defined(ambientLightFlag) || defined(ambientCubemapFlag)
#define ambientFlag
#endif//ambientFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif//specularFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif//separateAmbientFlag

#endif//lightingFlag

// TerrorEffector uniforms

uniform float u_affectedByLight;
uniform vec3 u_shadowlessLightsColors[16];
uniform vec3 u_shadowlessLightsExtraData[16];
uniform vec3 u_shadowlessLightsPositions[16];
uniform vec3 u_nearbySimpleShadowsData[2];
uniform int u_numberOfShadowlessLights;
varying vec3 v_frag_pos;
uniform float u_screenWidth;
uniform float u_screenHeight;
uniform float u_modelWidth;
uniform float u_modelHeight;
uniform float u_modelDepth;
uniform float u_modelX;
uniform float u_modelY;
uniform float u_modelZ;
uniform vec2 u_playerScreenCoords;
uniform vec2 u_mouseScreenCoords;
uniform int u_numberOfNearbySimpleShadows;
uniform int u_floorAmbientOcclusion;
uniform int u_entityType;
uniform sampler2D u_shadows;
uniform vec3 u_flatColor;
uniform int u_fowSignature;
uniform int u_graySignature;
uniform int u_grayScale;

const float X_RAY_RADIUS = 50.0;
//

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

vec3 grayScale(vec3 color){
    float luminance = dot(color.rgb, vec3(0.099, 0.387, 0.014));
    vec3 grayscaleColor = vec3(luminance);
    return grayscaleColor;
}

vec4 grayFadeOneWay(float edgeCoord, float fragCoord){
    float normalized = 1.0 - min(1.0, max(fragCoord-(edgeCoord-0.5), 0.0)*4.0);
    vec4 color = vec4(gl_FragColor.rgb, 1.0);
    vec3 shadedColor = mix(color.rgb, grayScale(color.rgb), normalized);
    color.rgb = shadedColor;
    return color;
}

vec4 grayFadeDiagonal(vec2 cornerCoord, vec2 fragCoord){
    float distanceThreshold = 0.4;
    float distance = distance(fragCoord, cornerCoord) / length(vec2(0.5));
    vec4 color = vec4(gl_FragColor.rgb, 1.0);
    if (distance < distanceThreshold) {
        float normalized = smoothstep(distanceThreshold, 0.0, distance);
        color.rgb = mix(color.rgb, grayScale(color.rgb), normalized);
    }
    return color;
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

void main() {
    #if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor;
    #elif defined(diffuseTextureFlag) && defined(colorFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * v_color;
    #elif defined(diffuseTextureFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
    #elif defined(diffuseColorFlag) && defined(colorFlag)
    vec4 diffuse = u_diffuseColor * v_color;
    #elif defined(diffuseColorFlag)
    vec4 diffuse = u_diffuseColor;
    #elif defined(colorFlag)
    vec4 diffuse = v_color;
    #else
    vec4 diffuse = vec4(1.0);
    #endif

    #if defined(emissiveColorFlag)
    vec4 emissive = u_emissiveColor;
    #else
    vec4 emissive = vec4(0.0);
    #endif

    #if (!defined(lightingFlag))
    gl_FragColor.rgb = diffuse.rgb + emissive.rgb;
    #elif (!defined(specularFlag))
    #if defined(ambientFlag) && defined(separateAmbientFlag)
    gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse)) + emissive.rgb;
    #else
    gl_FragColor.rgb = vec3(0.0);
    vec3 finalColor = vec3(0.0);

    if (shouldDiscardFragment()){
        discard;
    }

    if (u_flatColor.x < 0.0){
        if (u_affectedByLight != 0.0){
            if (u_numberOfShadowlessLights > 0) {
                for (int i = 0; i< u_numberOfShadowlessLights; i++){
                    vec3 light = u_shadowlessLightsPositions[i];
                    vec3 sub = light.xyz - v_frag_pos.xyz;
                    vec3 lightDir = normalize(sub);
                    float distance = length(sub);
                    vec3 extra = u_shadowlessLightsExtraData[i];
                    if (distance <= extra.y){
                        int light_color_index = int(extra.z);
                        vec3 light_color;
                        if (light_color_index > -1){
                            light_color = vec3(u_shadowlessLightsColors[light_color_index]);
                        } else {
                            light_color = vec3(0.0);
                        }
                        float attenuation = 4.0 * extra.x / (1.0 + (0.01*distance) + (0.9*distance*distance));
                        float dot_value = dot(v_normal, lightDir);
                        float intensity = max(dot_value, 0.0);
                        vec3 value_to_add = (diffuse.rgb *light_color.rgb* (attenuation * intensity));
                        value_to_add *= distance > (extra.y*5.0/6.0) ? 0.5 : 1.0;
                        finalColor += value_to_add;
                    }
                }
                finalColor += emissive.rgb;
            }
            vec2 c= gl_FragCoord.xy;
            c.x/=u_screenWidth;
            c.y/=u_screenHeight;
            vec4 staticLightsColor= (u_entityType < 2) ? texture2D(u_shadows, c) : vec4(0.5);
            finalColor.rgb += (diffuse.rgb * (v_lightDiffuse + staticLightsColor.rgb)) + emissive.rgb;

            if (u_grayScale == 1){
                finalColor = grayScale(finalColor);
            }
            gl_FragColor.rgb = finalColor;

            float minDistToChar = 21390950.0;
            float shadowRadius = 1.0;
            for (int i = 0; i< u_numberOfNearbySimpleShadows; i++){
                vec2 sub = u_nearbySimpleShadowsData[i].xy - v_frag_pos.xz;
                float distance = length(sub);
                if (distance < minDistToChar){
                    minDistToChar = distance;
                    shadowRadius = u_nearbySimpleShadowsData[i].z;
                }
            }

            const float SHADOW_MAX_OPACITY = 0.2;
            if (minDistToChar < shadowRadius){
                gl_FragColor.rgb*=1.0 - min(SHADOW_MAX_OPACITY, 1.0/(1.0 + minDistToChar));
            }

            if (u_floorAmbientOcclusion > 0){
                const float AO_STRENGTH = 0.45;
                const float FLOOR_DIAG_AO_STRENGTH = AO_STRENGTH*2.0;
                if ((u_floorAmbientOcclusion & 1) == 1){ // South-East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*max(v_frag_pos.z - u_modelZ, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 2) == 2){ // South
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.z - u_modelZ, 0.0)*AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 4) == 4){ // South-West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*max(v_frag_pos.z - u_modelZ, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 8) == 8){ // East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 32) == 32){ // West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 64) == 64){ // North-East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*max(u_modelZ - v_frag_pos.z, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 128) == 128){ // North
                    gl_FragColor.rgb *= 1.0 - max(u_modelZ - v_frag_pos.z, 0.0)*AO_STRENGTH;
                }
                if ((u_floorAmbientOcclusion & 256) == 256){ // North-West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*max(u_modelZ - v_frag_pos.z, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
            }
            if (u_fowSignature > 0){
                const float AO_STRENGTH = 2.0;
                const float FLOOR_DIAG_AO_STRENGTH = AO_STRENGTH*2.0;
                if ((u_fowSignature & 1) == 1){ // South-East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*max(v_frag_pos.z - u_modelZ, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_fowSignature & 2) == 2){ // South
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.z - u_modelZ, 0.0)*AO_STRENGTH;
                }
                if ((u_fowSignature & 4) == 4){ // South-West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*max(v_frag_pos.z - u_modelZ, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_fowSignature & 8) == 8){ // East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*AO_STRENGTH;
                }
                if ((u_fowSignature & 32) == 32){ // West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*AO_STRENGTH;
                }
                if ((u_fowSignature & 64) == 64){ // North-East
                    gl_FragColor.rgb *= 1.0 - max(v_frag_pos.x - u_modelX, 0.0)*max(u_modelZ - v_frag_pos.z, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
                if ((u_fowSignature & 128) == 128){ // North
                    gl_FragColor.rgb *= 1.0 - max(u_modelZ - v_frag_pos.z, 0.0)*AO_STRENGTH;
                }
                if ((u_fowSignature & 256) == 256){ // North-West
                    gl_FragColor.rgb *= 1.0 - max(u_modelX - v_frag_pos.x, 0.0)*max(u_modelZ - v_frag_pos.z, 0.0)*FLOOR_DIAG_AO_STRENGTH;
                }
            } else if (u_entityType == 1){
                float WALL_BOTTOM_AO_MAX_HEIGHT = u_modelY + 0.5;
                float WALL_TOP_AO_MIN_HEIGHT = u_modelY + u_modelHeight - 0.5;
                const float WALL_AO_STRENGTH = 0.85;

                // Bottom AO.
                gl_FragColor.rgb *= 1.0 - max(WALL_BOTTOM_AO_MAX_HEIGHT - v_frag_pos.y, 0.0)*WALL_AO_STRENGTH;

                // Top AO.
                gl_FragColor.rgb *= 1.0 - max(0.5 - (u_modelY + u_modelHeight - v_frag_pos.y), 0.0)*WALL_AO_STRENGTH;
            }
            if (u_graySignature > 0){
                if ((u_graySignature & 16) == 16){
                    float gray = dot(gl_FragColor.rgb, vec3(0.099, 0.387, 0.014));
                    gl_FragColor.rgb = vec3(gray);
                } else {
                    const float AO_STRENGTH = 2.0;
                    const float FLOOR_DIAG_AO_STRENGTH = AO_STRENGTH*2.0;
                    if ((u_graySignature & 1) == 1){ // South-East
                        gl_FragColor = grayFadeDiagonal(vec2(v_frag_pos.x, v_frag_pos.z), vec2(u_modelX+0.5, u_modelZ+0.5));
                    }
                    if ((u_graySignature & 2) == 2){ // South
                        gl_FragColor = grayFadeOneWay(v_frag_pos.z, u_modelZ);
                    }
                    if ((u_graySignature & 4) == 4){ // South-West
                        gl_FragColor = grayFadeDiagonal(vec2(v_frag_pos.x, v_frag_pos.z), vec2(u_modelX-0.5, u_modelZ+0.5));
                    }
                    if ((u_graySignature & 8) == 8){ // East
                        gl_FragColor = grayFadeOneWay(v_frag_pos.x, u_modelX);
                    }
                    if ((u_graySignature & 32) == 32){ // West
                        gl_FragColor = grayFadeOneWay(u_modelX, v_frag_pos.x);
                    }
                    if ((u_graySignature & 64) == 64){ // North-East
                        gl_FragColor = grayFadeDiagonal(vec2(v_frag_pos.x, v_frag_pos.z), vec2(u_modelX+0.5, u_modelZ-0.5));
                    }
                    if ((u_graySignature & 128) == 128){ // North
                        gl_FragColor = grayFadeOneWay(u_modelZ, v_frag_pos.z);
                    }
                    if ((u_graySignature & 256) == 256){ // North-West
                        gl_FragColor = grayFadeDiagonal(vec2(v_frag_pos.x, v_frag_pos.z), vec2(u_modelX-0.5, u_modelZ-0.5));
                    }
                }
            }
        } else {
            gl_FragColor.rgb = diffuse.rgb;
        }
    } else {
        gl_FragColor.rgb = u_flatColor;
    }

    #endif
    #else
    #if defined(specularTextureFlag) && defined(specularColorFlag)
    vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * u_specularColor.rgb * v_lightSpecular;
    #elif defined(specularTextureFlag)
    vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * v_lightSpecular;
    #elif defined(specularColorFlag)
    vec3 specular = u_specularColor.rgb * v_lightSpecular;
    #else
    vec3 specular = v_lightSpecular;
    #endif

    #if defined(ambientFlag) && defined(separateAmbientFlag)
    #ifdef shadowMapFlag
    gl_FragColor.rgb = (diffuse.rgb * (getShadow() * v_lightDiffuse + v_ambientLight)) + specular + emissive.rgb;
    #else
    gl_FragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight)) + specular + emissive.rgb;
    #endif//shadowMapFlag
    #else
    #ifdef shadowMapFlag
    gl_FragColor.rgb = getShadow() * ((diffuse.rgb * v_lightDiffuse) + specular) + emissive.rgb;
    #else
    gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular + emissive.rgb;
    #endif//shadowMapFlag
    #endif
    #endif//lightingFlag

    #ifdef blendedFlag
    gl_FragColor.a = diffuse.a * v_opacity;
    #ifdef alphaTestFlag
    if (gl_FragColor.a <= v_alphaTest)
    discard;
    #endif
    #else
    gl_FragColor.a = 1.0;
    #endif

    gl_FragColor.rgba *= applyXRayFading(u_playerScreenCoords);
    gl_FragColor.rgba *= applyXRayFading(u_mouseScreenCoords);
}
