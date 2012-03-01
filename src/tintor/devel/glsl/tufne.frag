varying vec3  MCposition;
varying float LightIntensity;

void main()
{
	vec3 color;
	float a;
	vec3 position;

	if (length(fract(MCposition) - vec3(0.5, 0.5, 0.5)) <= 0.4)
		color = vec3(1.0, 1.0, 1.0);
	else
		color = vec3(1.0, 0.0, 0.0);
	gl_FragColor = vec4(color * LightIntensity, 1.0);
}