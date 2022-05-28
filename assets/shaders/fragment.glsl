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
#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag) || defined(emissiveTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
#endif

#ifdef emissiveTextureFlag
varying MED vec2 v_emissiveUV;
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

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif //specularFlag

    #if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
#endif // fogFlag

// Necronemes custom uniforms

uniform float u_affectedByLight;
uniform vec3 u_shadowlessLightsColors[16];
uniform vec3 u_shadowlessLightsExtraData[16];
uniform vec3 u_shadowlessLightsPositions[16];
uniform vec2 u_nearbyCharactersPositions[2];
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
uniform int u_numberOfNearbyCharacters;
uniform int u_floorAmbientOcclusion;
uniform int u_isWall;
uniform sampler2D u_shadows;
uniform vec3 u_flatColor;
uniform int u_fowSignature;

//

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

void main() {

    
    
    
    #if defined(normalFlag)
    vec3 normal = v_normal;
    #endif // normalFlag

    #if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor * v_color;
    #elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
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

    #if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    vec4 emissive = texture2D(u_emissiveTexture, v_emissiveUV) * u_emissiveColor;
    #elif defined(emissiveTextureFlag)
    vec4 emissive = texture2D(u_emissiveTexture, v_emissiveUV);
    #elif defined(emissiveColorFlag)
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
                            light_color = vec3(1.0);
                        }
                        float attenuation = 4.0 * extra.x / (1.0 + (0.01*distance) + (0.9*distance*distance));
                        float dot_value = dot(v_normal, lightDir);
                        float intensity = max(dot_value, 0.0);
                        vec3 value_to_add = (diffuse.rgb *light_color.rgb* (attenuation * intensity));
                        value_to_add *= distance > (extra.y*5.0/6.0) ? 0.5 : 1.0;
                        gl_FragColor.rgb += value_to_add;
                    }
                }
                gl_FragColor.rgb += emissive.rgb;
            }
            gl_FragColor.rgb += (diffuse.rgb * v_lightDiffuse) + emissive.rgb;
            vec2 c= gl_FragCoord.xy;
            c.x/=u_screenWidth;
            c.y/=u_screenHeight;
            vec4 color=texture2D(u_shadows, c);
            color.rgba *= color.a*5.0;
            gl_FragColor.rgb+=vec3(gl_FragColor.r*color.a, gl_FragColor.g*color.a, gl_FragColor.b*color.a);

            float minDistToChar = 21390950.0;
            for (int i = 0; i< u_numberOfNearbyCharacters; i++){
                vec2 sub = u_nearbyCharactersPositions[i].xy - v_frag_pos.xz;
                float distance = length(sub);
                if (distance < minDistToChar){
                    minDistToChar = distance;
                }
            }

            const float SHADOW_RADIUS = 0.25;
            const float SHADOW_MAX_OPACITY = 0.2;
            if (minDistToChar < SHADOW_RADIUS){
                gl_FragColor.rgb*=1.0 - min(SHADOW_MAX_OPACITY, 1.0/(1.0 + minDistToChar));
            }

            if (u_floorAmbientOcclusion > 0){
                const float AO_STRENGTH = 0.25;
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
            } else if (u_isWall == 1){
                const float WALL_AO_MAX_HEIGHT = 0.5;
                const float WALL_AO_STRENGTH = 0.55;
                gl_FragColor.rgb *= 1.0 - max(WALL_AO_MAX_HEIGHT - v_frag_pos.y - u_modelY, 0.0)*WALL_AO_STRENGTH;
                gl_FragColor.rgb *= 1.0 - max(WALL_AO_MAX_HEIGHT - (u_modelHeight - v_frag_pos.y), 0.0)*WALL_AO_STRENGTH;
            }
        } else {
            gl_FragColor.rgb = diffuse.rgb;
        }
    }else{
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
    #endif //shadowMapFlag
    #else
    #ifdef shadowMapFlag
    gl_FragColor.rgb = getShadow() * ((diffuse.rgb * v_lightDiffuse) + specular) + emissive.rgb;
    #else
    gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular + emissive.rgb;
    #endif //shadowMapFlag
    #endif
    #endif //lightingFlag

    #ifdef fogFlag
    gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);
    #endif // end fogFlag

    #ifdef blendedFlag
    gl_FragColor.a = diffuse.a * v_opacity;
    #ifdef alphaTestFlag
    if (gl_FragColor.a <= v_alphaTest)
    discard;
    #endif
    #else
    gl_FragColor.a = 1.0;
    #endif

}
