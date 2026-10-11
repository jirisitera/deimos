#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D DepthSampler;layout(location=0)in vec2 texCoord;layout(location=0)out vec4 fragColor;
#define _fd 0.05
#define _gd 0.7
#define _hd 0.35
#define _id 8
vec2 ga;vec3 bg(ivec2 na){float Wa=texelFetch(DepthSampler,na,0).r;float cg=_fd/max(Wa,1e-6);vec2 dg=(vec2(na)+0.5)/ga*2.0-1.0;return vec3(dg*vec2(ga.x/ga.y,1.0)*_gd*cg,-cg);}float eg(vec2 Ya){return fract(sin(dot(Ya,vec2(12.9898,78.233)))*43758.5453);}void main(){ga=vec2(textureSize(DepthSampler,0));ivec2 na=ivec2(gl_FragCoord.xy);float Wa=texelFetch(DepthSampler,na,0).r;if(Wa<=0.0){fragColor=vec4(1.0);return;}vec3 Ya=bg(na);vec3 Qb=bg(na-ivec2(1,0)),I=bg(na+ivec2(1,0)),Wf=bg(na-ivec2(0,1)),fg=bg(na+ivec2(0,1));vec3 gg=abs(I.z-Ya.z)<abs(Ya.z-Qb.z)?I-Ya:Ya-Qb;vec3 gf=abs(fg.z-Ya.z)<abs(Ya.z-Wf.z)?fg-Ya:Ya-Wf;vec3 ab=normalize(cross(gg,gf));float hg=ga.y*0.5/_gd;float ig=_hd*hg/-Ya.z;float jg=eg(vec2(na))*6.2831;float kg=0.0;for(int ua=0;ua<_id;ua++){float xa=(float(ua)+0.5)/float(_id);float X=float(ua)*2.39996+jg;vec2 Za=vec2(cos(X),sin(X))*sqrt(xa)*ig;ivec2 D=clamp(na+ivec2(Za),ivec2(0),ivec2(ga)-1);vec3 fd=bg(D);vec3 sa=fd-Ya;float mg=length(sa);float ng=max(dot(ab,sa/max(mg,1e-4))-0.1,0.0);kg+=ng*smoothstep(_hd*2.5,_hd*0.5,mg);}float og=1.0-kg/float(_id)*1.6;fragColor=vec4(vec3(clamp(og,0.0,1.0)),1.0);}
