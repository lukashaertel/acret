#version 120


varying vec4 position;

uniform vec3 dataStep;
uniform sampler3D dataFieldTex;
uniform sampler2D norTex;
uniform sampler2D colTex;

const int numLights = 1;
const int iterations = 64;
const float epsilon = 0.05;
const float k = 2;

const float minDataStep() {
    return min(dataStep.x, min(dataStep.y, dataStep.z));
}

void main(void)
{
    // Displacement for swizzles.
    vec4 displace = vec4(dataStep, 0.0);

    // Get normal vector.
    vec3 normal = -normalize(vec3(
    texture3D(dataFieldTex, (position.xyz + displace.xww + 1.0f) / 2.0f).r
        - texture3D(dataFieldTex, (position.xyz - displace.xww + 1.0f) / 2.0f).r,
    texture3D(dataFieldTex, (position.xyz + displace.wyw + 1.0f) / 2.0f).r
        - texture3D(dataFieldTex, (position.xyz - displace.wyw + 1.0f) / 2.0f).r,
    texture3D(dataFieldTex, (position.xyz + displace.wwz + 1.0f) / 2.0f).r
        - texture3D(dataFieldTex, (position.xyz - displace.wwz + 1.0f) / 2.0f).r));

    // Blend value.
    vec3 blend = abs(normal * normal * normal * normal);
    blend /= blend.x + blend.y + blend.z;

    // Triplanar uvs
    vec2 uvX = position.zy;
    vec2 uvY = position.xz;
    vec2 uvZ = position.xy;

    // Get the sign (-1 or 1) of the surface normal
    vec3 axisSign = sign(normal);

    // Flip UVs to correct for mirroring
    uvX.x *= axisSign.x;
    uvY.x *= axisSign.y;
    uvZ.x *= -axisSign.z;

    // Tangent space normal maps
    vec3 tnormalX = texture2D(norTex, uvX).rgb * 2.0 - 1.0;
    vec3 tnormalY = texture2D(norTex, uvY).rgb * 2.0 - 1.0;
    vec3 tnormalZ = texture2D(norTex, uvZ).rgb * 2.0 - 1.0;

    // Swizzle world normals into tangent space and apply Whiteout blend
    tnormalX = vec3(tnormalX.xy + normal.zy, abs(tnormalX.z) * normal.x);
    tnormalY = vec3(tnormalY.xy + normal.xz, abs(tnormalY.z) * normal.y);
    tnormalZ = vec3(tnormalZ.xy + normal.xy, abs(tnormalZ.z) * normal.z);

    // Flip normals to correct for the flipped UVs
    tnormalX.x *= axisSign.x;
    tnormalY.x *= axisSign.y;
    tnormalZ.x *= -axisSign.z;

    // Swizzle tangent normals to match world orientation and triblend
    vec3 normalVec = normalize(
        tnormalX.zyx * blend.x +
        tnormalY.xzy * blend.y +
        tnormalZ.xyz * blend.z
    );

    vec4 tcolorX = texture2D(colTex, uvX) ;
    vec4 tcolorY = texture2D(colTex, uvY);
    vec4 tcolorZ = texture2D(colTex, uvZ);

    vec4 colorVec =
        tcolorX * blend.x +
        tcolorY * blend.y +
        tcolorZ * blend.z;

    vec4 finalColor = vec4(0.0, 0.0, 0.0, 0.0);
    for (int i = 0; i < numLights; i++) {
        vec3 L = normalize(gl_LightSource[i].position.xyz - position.xyz);
        vec3 E = normalize(-position.xyz);
        vec3 R = normalize(-reflect(L.xyz, normalVec));

        float o = 1.0;
        for (int j = 0; j < iterations; j++) {
            float d = texture3D(dataFieldTex, ((position.xyz + L.xyz * minDataStep() * float(k + j))  + 1.0f) / 2.0f).r;
            o *= clamp((1.0 - d) / k, 0.0, 1.0);
            if(o <= epsilon)
                break;
        }

        vec4 Iamb = gl_LightSource[i].ambient;
        vec4 Idiff = clamp(gl_LightSource[i].diffuse * max(dot(normalVec, L.xyz) * o, 0.0), 0.0, 1.0);
        //vec4 Ispec = clamp(gl_LightSource[i].specular * pow(max(dot(R, E), 0.0), 0.3 * gl_FrontMaterial.shininess), 0.0, 1.0);

        finalColor += (Iamb + Idiff) * colorVec;// + Ispec;
    }

    gl_FragColor = finalColor;
}
