varying float LightIntensity;
varying vec3  MCposition;

void main()
{
	vec3 color = noise3(MCposition);
	gl_FragColor = vec4(color * LightIntensity, 1.0);
}