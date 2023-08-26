#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform int u_time;
varying vec4 v_color;
uniform mat4 u_projTrans;

float vignette(vec2 uv, float size, float smoothness, float edgeRounding)
{
    uv -= .5;
    uv *= size;
    float amount = sqrt(pow(abs(uv.x), edgeRounding) + pow(abs(uv.y), edgeRounding));
    amount = 1. - amount;
    return smoothstep(0.0, smoothness, amount);
}

float random(vec2 uv)
{
    return fract(sin(dot(uv, vec2(15.5151, 42.2561))) * 12341.14122 * sin(float(u_time) * 0.03));
}

float noise(vec2 uv)
{
    vec2 i = floor(uv);
    vec2 f = fract(uv);

    float a = random(i);
    float b = random(i + vec2(1., 0.));
    float c = random(i + vec2(0., 1.));
    float d = random(i + vec2(1.));

    vec2 u = smoothstep(0., 1., f);

    return mix(a, b, u.x) + (c - a) * u.y * (1. - u.x) + (d - b) * u.x * u.y;
}

void main()
{
    vec4 textureColor = texture2D(u_texture, v_texCoords);
    vec4 color = v_color * textureColor;
    color = mix(color, vec4(noise(v_texCoords * 75.)), 0.05);
    gl_FragColor = color;
}