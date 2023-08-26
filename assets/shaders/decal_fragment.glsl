#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec3 u_colorNotAffectedByLight;
void main()
{
    vec4 textureColor = texture2D(u_texture, v_texCoords);
    if (v_color.w == 1.0
    && abs(textureColor.r - u_colorNotAffectedByLight.r) < 0.01
    && abs(textureColor.g - u_colorNotAffectedByLight.g) < 0.01
    && abs(textureColor.b - u_colorNotAffectedByLight.b) < 0.01){
        gl_FragColor = textureColor;
    } else {
        gl_FragColor = v_color * textureColor;
    }
}