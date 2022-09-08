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

#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main()
{
    vec4 texColor = texture2D(u_texture, v_texCoords);
    gl_FragColor = vec4(v_color.r * texColor.w, v_color.g * texColor.w, v_color.b * texColor.w, v_color.w * texColor.w);
}