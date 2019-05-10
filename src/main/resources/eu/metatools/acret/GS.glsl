/**** Geometry Shader Marching Cubes
	* Copyright Cyril Crassin, Junuary 2007.
	* This code is partially based on the example of
	* Paul Bourke "Polygonising a scalar field" located at :
	* http://local.wasp.uwa.edu.au/~pbourke/geometry/polygonise/
****/

//GLSL version 1.20
#version 120
//New G80 extensions
#extension GL_EXT_geometry_shader4 : enable
#extension GL_EXT_gpu_shader4 : enable


uniform mat4 normToPos;
uniform mat4 posToNorm;

//Volume data field texture
uniform sampler3D dataFieldTex;
//Edge table texture
uniform isampler2D edgeTableTex;
//Triangles table texture
uniform isampler2D triTableTex;

//Global iso level // TODO: Constant
uniform float isolevel;

//Marching cubes vertices decal
uniform vec3 vertDecals[8];

//Vertices position for fragment shader
varying vec4 position;

//Get vertex i position within current marching cube
const vec3 cubePos(in int i){
    return gl_PositionIn[0].xyz + vertDecals[i];
}

//Get vertex i value within current marching cube
const float cubeVal(in int i){
    return texture3D(dataFieldTex, cubePos(i)).r;
}

//Get edge table value
const int edgeTableValue(in int i){
    return texelFetch2D(edgeTableTex, ivec2(i, 0), 0).r;
}

//Get triangle table value
const int triTableValue(in int i, in int j){
    return texelFetch2D(triTableTex, ivec2(j, i), 0).r;
}

//Compute interpolated vertex along an edge
const vec3 vertexInterp(in vec3 v0, in float l0, in vec3 v1, in float l1){
    return mix(v0, v1, (isolevel - l0) / (l1 - l0));
}

//Geometry Shader entry point
void main(void) {
    int cubeindex = 0;

    //Determine the index into the edge table which
    //tells us which vertices are inside of the surface
    if (cubeVal(0) < isolevel) cubeindex = cubeindex | 1;
    if (cubeVal(1) < isolevel) cubeindex = cubeindex | 2;
    if (cubeVal(2) < isolevel) cubeindex = cubeindex | 4;
    if (cubeVal(3) < isolevel) cubeindex = cubeindex | 8;
    if (cubeVal(4) < isolevel) cubeindex = cubeindex | 16;
    if (cubeVal(5) < isolevel) cubeindex = cubeindex | 32;
    if (cubeVal(6) < isolevel) cubeindex = cubeindex | 64;
    if (cubeVal(7) < isolevel) cubeindex = cubeindex | 128;


    //Cube is entirely in/out of the surface
    if (edgeTableValue(cubeindex) == 0)
        return;

    vec3 vertlist[12];

    //Find the vertices where the surface intersects the cube
    if ((edgeTableValue(cubeindex) & 1)!=0)
        vertlist[0] = vertexInterp(cubePos(0), cubeVal(0), cubePos(1), cubeVal(1));
    if ((edgeTableValue(cubeindex) & 2)!=0)
        vertlist[1] = vertexInterp(cubePos(1), cubeVal(1), cubePos(2), cubeVal(2));
    if ((edgeTableValue(cubeindex) & 4)!=0)
        vertlist[2] = vertexInterp(cubePos(2), cubeVal(2), cubePos(3), cubeVal(3));
    if ((edgeTableValue(cubeindex) & 8)!=0)
        vertlist[3] = vertexInterp(cubePos(3), cubeVal(3), cubePos(0), cubeVal(0));
    if ((edgeTableValue(cubeindex) & 16)!=0)
        vertlist[4] = vertexInterp(cubePos(4), cubeVal(4), cubePos(5), cubeVal(5));
    if ((edgeTableValue(cubeindex) & 32)!=0)
        vertlist[5] = vertexInterp(cubePos(5), cubeVal(5), cubePos(6), cubeVal(6));
    if ((edgeTableValue(cubeindex) & 64)!=0)
        vertlist[6] = vertexInterp(cubePos(6), cubeVal(6), cubePos(7), cubeVal(7));
    if ((edgeTableValue(cubeindex) & 128)!=0)
        vertlist[7] = vertexInterp(cubePos(7), cubeVal(7), cubePos(4), cubeVal(4));
    if ((edgeTableValue(cubeindex) & 256)!=0)
        vertlist[8] = vertexInterp(cubePos(0), cubeVal(0), cubePos(4), cubeVal(4));
    if ((edgeTableValue(cubeindex) & 512)!=0)
        vertlist[9] = vertexInterp(cubePos(1), cubeVal(1), cubePos(5), cubeVal(5));
    if ((edgeTableValue(cubeindex) & 1024)!=0)
        vertlist[10] = vertexInterp(cubePos(2), cubeVal(2), cubePos(6), cubeVal(6));
    if ((edgeTableValue(cubeindex) & 2048)!=0)
        vertlist[11] = vertexInterp(cubePos(3), cubeVal(3), cubePos(7), cubeVal(7));


    // Create the triangle
    gl_FrontColor = gl_FrontColorIn[0];

    // Emit all until count.
    for (int i = 0; i < 16; i++) {
        // Find triangle value, return if end.
        int tri = triTableValue(cubeindex, i);
        if (tri == -1)
            break;

        // Fill varying and position, then emit vertex.
        position = normToPos * vec4(vertlist[tri], 1);
        gl_Position = gl_ModelViewProjectionMatrix * position;
        EmitVertex();

        //End triangle strip after three vertice.
        if ((i + 1) % 3 == 0)
            EndPrimitive();
    }
}
