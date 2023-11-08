#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform int u_time;
uniform mat4 u_projTrans;

varying vec2 v_texCoords;
varying vec4 v_color;

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

vec2 crtCoords(vec2 uv, float bend)
{
    uv -= 0.5;
    uv *= 2.;
    uv.x *= 1. + pow(abs(uv.y)/bend, 2.);
    uv.y *= 1. + pow(abs(uv.x)/bend, 2.);

    uv /= 2.5;
    return uv + .5;
}

float scanline(vec2 uv, float lines, float speed)
{
    return sin(uv.y * lines + float(u_time) * speed);
}

void main()
{
    vec2 crtUv = crtCoords(v_texCoords, 2.);
    float s1 = scanline(crtUv, 300., -0.00000001);
    float s2 = scanline(crtUv, 400., -0.00000002);
    vec4 textureColor = vec4(0.0);
    textureColor.r = texture2D(u_texture, crtUv + vec2(0., 0.01)).r;
    textureColor.g = texture2D(u_texture, crtUv).g;
    textureColor.b = texture2D(u_texture, crtUv + vec2(0., -0.01)).b;
    textureColor.a = texture2D(u_texture, crtUv).a;
    vec4 color = mix(textureColor, vec4(s1+s2), 0.01);
    gl_FragColor = mix(color, vec4(noise(v_texCoords * 75.)), 0.02) * vignette(v_texCoords, 1.9, .6, 8.);
}