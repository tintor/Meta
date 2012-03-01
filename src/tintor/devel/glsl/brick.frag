uniform vec3  BrickColor, MortarColor;
uniform vec3  BrickSize;
uniform vec3  BrickPct;

varying vec3  MCposition;
varying float LightIntensity;

void main()
{
    vec3  color;
    vec3  position;
    vec3  useBrick;
    
    position = MCposition / BrickSize;

    if (fract(position.y * 0.5) > 0.5)
    {
        position.x += 0.5;
        position.z += 0.5;
    }

    position = fract(position);

    useBrick = step(position, BrickPct);

    color  = mix(MortarColor, BrickColor, useBrick.x * useBrick.y * useBrick.z);
    color *= LightIntensity;
    gl_FragColor = vec4(color, 1.0);
}