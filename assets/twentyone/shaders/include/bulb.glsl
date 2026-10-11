#ifndef _f
#define _f
vec3 ta(int ua,float va){float U=va*1200.0+float(ua)*17.3;float wa=0.9+0.1*sin(U*37.0+float(ua))*sin(U*13.0);float xa=floor(U*6.0);float ya=fract(sin(xa*12.9898+float(ua)*78.233)*43758.5453);float za=ya>0.93?0.25:1.0;return vec3(1.0,0.62,0.3)*wa*za;}
#endif
