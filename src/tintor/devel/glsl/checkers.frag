varying vec3  MCposition;
varying float LightIntensity;

void main()
{
	vec3 color;
	float a;
	vec3 position;

	position = floor(MCposition * 5);
	a = mod(position.x + position.y + position.z, 2.0);
	color = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), a);

	gl_FragColor = vec4(color * LightIntensity, 1.0);
}