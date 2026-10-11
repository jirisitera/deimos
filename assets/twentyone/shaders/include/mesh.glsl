#ifndef _m
#define _m
#include <twentyone:tv.glsl>
#define _n ivec3(253,77,11)
#define _o -2
#define _p -3
#define _q -4
#define _r 16.0
#define _s 9
ivec3 Ka(sampler2D La,ivec2 Ha){return ivec3(round(texelFetch(La,Ha,0).rgb*255.0));}void Ma(sampler2D La,vec2 Na,vec3 Oa,vec3 Pa,vec4 A,int S,inout vec3 Qa,inout vec3 Ra,inout vec2 Sa,inout int Ta,inout vec3 ba){vec2 ga=vec2(textureSize(La,0));ivec2 Ua=ivec2(Na*ga);if(Ka(La,Ua)!=_n)return;ivec2 Ha=Ua+ivec2(1+S*5,0);ivec3 X=Ka(La,Ha),Va=Ka(La,Ha+ivec2(1,0)),F=Ka(La,Ha+ivec2(2,0));ivec3 Wa=Ka(La,Ha+ivec2(3,0)),Xa=Ka(La,Ha+ivec2(4,0));vec3 Ya=vec3(X.r*256+X.g,X.b*256+Va.r,Va.g*256+Va.b)/65535.0*(2.0*_r)-_r;vec2 Za=(vec2(F.r*256+F.g,F.b*256+Wa.r)-32768.0)/8.0;vec3 ab=(vec3(Wa.g,Wa.b,Xa.r)-128.0)/127.0;int bb=int(round(A.b*255.0));bool cb=bb>=8;float db=cb?1.0+float(bb-8)/20.0:1.0;int eb=int(round(A.g*255.0));ba=vec3(Ya.xy,cb?db:db+float(eb)*16.0);Ya.x*=db;Ya*=round(A.r*255.0)/64.0;vec3 fb=-normalize(Pa),gb=vec3(0.0,1.0,0.0),hb=cross(gb,fb);Qa=Oa+hb*Ya.x+gb*Ya.y+fb*Ya.z;Ra=normalize(hb*ab.x+gb*ab.y+fb*ab.z);Sa=(vec2(Ua)+Za)/ga;Ta=Xa.g==1?(cb?_q:_o):Xa.g==2?_p:_s;}
#endif
