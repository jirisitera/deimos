#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
#include <twentyone:seat.glsl>
#include <twentyone:ui.glsl>
uniform sampler2D InSampler;
uniform sampler2D CursorSampler;
uniform sampler2D BloomSampler;
uniform sampler2D AoSampler;
uniform sampler2D UiFontSampler;
uniform sampler2D UiTextSampler;
uniform sampler2D UiLogoSampler;
uniform sampler2D UiIconsSampler;
uniform sampler2D UiTrumpsSampler;
layout(location=0)in vec2 texCoord;
layout(location=0)out vec4 fragColor;
#define _jd 16
#define _kd 1.0
#define _ld 1.05
#define _md 0.9
#define _nd 3.0
#define _od 16.0
#define _pd 0.35
#define _qd 0.07
#define _rd 1.0
#define _sd 0.2
#define _td vec3(0.95,1.03,0.9)
#define _ud 0.15
bool vg(out vec2 wg) {
    wg=vec2(0.0);
    if(ivec3(round(texelFetch(InSampler,ivec2(28,_jd),0).rgb*255.0))!=ivec3(91,126,209))return false;
    ivec3 X=ivec3(round(texelFetch(InSampler,ivec2(26,_jd),0).rgb*255.0));
    ivec3 Va=ivec3(round(texelFetch(InSampler,ivec2(27,_jd),0).rgb*255.0));
    wg=vec2(float(X.r*256+X.g)/65535.0*360.0-180.0,float(X.b*256+Va.r)/65535.0*180.0-90.0);
    return true;
}
float xg(vec2 Ya) {
    const int hd[16]=int[](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
    ivec2 ua=ivec2(mod(Ya,4.0));
    return(float(hd[ua.y*4+ua.x])+0.5)/16.0;
}
vec3 yg(vec2 Sa) {
    vec2 ga=vec2(textureSize(InSampler,0));
    ivec2 na=ivec2(Sa*ga);
    if(na.y>=_jd-2&&na.y<=_e+2&&na.x<48)Sa.y+=9.0/ga.y;
    return texture(InSampler,Sa).rgb;
}
float kg(vec2 Sa) {
    vec2 tg=1.0/vec2(textureSize(AoSampler,0));
    float og=0.0;
    for(int la=-1;la<=1;la++)for(int bd=-1;bd<=1;bd++)og+=texture(AoSampler,Sa+vec2(bd,la)*tg*1.5).r;
    return og/9.0;
}
vec3 zg(vec3 F,float fd) {
    float Qb=dot(F,vec3(0.2126,0.7152,0.0722));
    return mix(vec3(Qb),F,fd);
}
void main() {
    vec2 ga=vec2(textureSize(InSampler,0));
    vec2 na=1.0/ga;
    float U=GameTime*1200.0;
    vec2 Wc=na*_nd;
    vec2 Sa=floor(texCoord/Wc)*Wc+Wc*0.5;
    vec2 cc=Sa-0.5;
    float mg=length(cc);
    float Ag=_qd*mg*mg;
    vec3 Nd=vec3(yg(Sa+cc*Ag).r,yg(Sa).g,yg(Sa-cc*Ag).b)*kg(Sa)+texture(BloomSampler,Sa).rgb;
    Nd*=_kd;
    Nd=mix(Nd,Nd*_td,_ud);
    float Va=xg(floor(Sa/Wc));
    Nd=floor(Nd*_od+Va*_pd)/(_od-1.0);
    Nd=(Nd-0.5)*_ld+0.5;
    Nd=clamp(zg(Nd,_md),0.0,1.0);
    Nd*=clamp(1.0-smoothstep(0.5,0.9,mg)*_rd,0.0,1.0);
    float ab=fract(sin(dot(floor(Sa/Wc),vec2(12.9898,78.233))+U*37.0)*43758.5453);
    Nd+=(ab-0.5)*0.08*_sd;
    vec2 wg;
    bool Bg=vg(wg);
    vec2 dg=vec2(0.0);
    if(Bg) {
        dg=clamp(vec2(wg.x/90.0,wg.y/90.0),-1.0,1.0);
    }
    vec2 Ke=vec2(0.5+dg.x*0.5,0.5+dg.y*0.5);
    Je st=ui_state(InSampler,GameTime);
    vec2 te=vec2(gl_FragCoord.x,ga.y-gl_FragCoord.y);
    if(st.ok)Nd=Bf(Nd,st,te,ga,Ke,GameTime,UiFontSampler,UiTextSampler,UiLogoSampler,UiIconsSampler,UiTrumpsSampler);
    if(Bg&&(!st.ok||st.screen>=_Y)) {
        vec2 Cg=Ke*ga;
        float Dg=max(1.0,round(ga.y/540.0));
        vec2 ba=floor((te-Cg)/Dg);
        if(all(greaterThanEqual(ba,vec2(0.0)))&&all(lessThan(ba,vec2(32.0)))) {
            vec4 F=texelFetch(CursorSampler,ivec2(ba),0);
            Nd=mix(Nd,F.rgb,F.a);
        }
    }
    fragColor=vec4(clamp(Nd,0.0,1.0),1.0);
}
