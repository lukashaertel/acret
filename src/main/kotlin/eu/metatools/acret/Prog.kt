package eu.metatools.acret

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.opengl.EXTGeometryShader4.*
import org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT
import org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT
import org.lwjgl.opengl.GL11.GL_AMBIENT
import org.lwjgl.opengl.GL11.GL_DIFFUSE
import org.lwjgl.opengl.GL11.GL_LIGHT0
import org.lwjgl.opengl.GL11.GL_MODELVIEW
import org.lwjgl.opengl.GL11.GL_POSITION
import org.lwjgl.opengl.GL11.GL_PROJECTION
import org.lwjgl.opengl.GL11.GL_SPECULAR
import org.lwjgl.opengl.GL11.glLoadIdentity
import org.lwjgl.opengl.GL11.glMatrixMode
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBPerlin
import java.lang.Math.round
import java.lang.Math.tan
import kotlin.system.measureNanoTime


val dataSize = Vector3i(128, 128, 128)
val cubeSize = Vector3f(32F, 32F, 32F)
val res = 1.00f
val cubeStep = Vector3f(res, res, res) / cubeSize
val isolevel = 0.5f
var viewOrient = Vector2f(2.5f, 0f)

val texName = "tex/Ground_Dirt_005_COLOR.jpg"
val norName = "tex/Ground_Dirt_005_NORM.jpg"


class Prog : Window("Program") {
    override fun onKey(key: Int, scancode: Int, action: Int, mods: Int) {
        if (action == GLFW_RELEASE && key == GLFW_KEY_ESCAPE)
            shouldClose = true
    }

    override fun onInit() {
        initGL()

        initShader()

        initShaderParams()
    }

    var program = 0
    var edgeTableTex = 0
    var triTableTex = 0
    var dataFieldTex = 0
    var gridDataBuffId = 0
    var gridDataBuffSize = 0

    var norTex = 0
    var colTex = 0

    /**
     * Initializes the shader.
     */
    private fun initShader() {
        // Create shaders.
        program = glCreateProgram()
        val vs = createShader("eu/metatools/acret/VS.glsl", GL_VERTEX_SHADER)
        val gs = createShader("eu/metatools/acret/GS.glsl", GL_GEOMETRY_SHADER)
        val fs = createShader("eu/metatools/acret/FS.glsl", GL_FRAGMENT_SHADER)

        // Attach to program.
        glAttachShader(program, vs)
        glAttachShader(program, gs)
        glAttachShader(program, fs)

        // Set parameters.
        glProgramParameteriEXT(program, GL_GEOMETRY_INPUT_TYPE_EXT, GL_POINTS)
        glProgramParameteriEXT(program, GL_GEOMETRY_OUTPUT_TYPE_EXT, GL_TRIANGLE_STRIP)
        glProgramParameteriEXT(program, GL_GEOMETRY_VERTICES_OUT_EXT, 16)

        // Link program.
        glLinkProgram(program)

        // Check program linking status.
        glGetProgrami(program, GL_LINK_STATUS).let { linked ->
            glGetProgramInfoLog(program).let { log ->
                if (!log.isNullOrEmpty())
                    System.err.println(log)
            }
            check(linked != 0)
        }
    }

    /**
     * Initializes the shader parameters
     */
    private fun initShaderParams() {
        // Use program.
        glUseProgram(program)

        // Initialize the edge table and set texture.
        initEdgeTable()

        // Initialize the tri table and set texture.
        initTriTable()

        // Initialize the data field and set texture.
        initDataField()

        initNM()
        initCol()

        // Initialize assignments to samplers..
        initSamplerAssignments()

        // Initialize vertex buffer object.
        initVBO()

        // Init light.
        initLighting()

        // Disable program.
        glUseProgram(0)
    }

    private fun initNM() {
        norTex = intArrayOf(0).also(::glGenTextures).single()

        glActiveTexture(GL_TEXTURE3)
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, norTex)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_REPEAT)

        //val ma = floatArrayOf(0f).also { glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, it) }.single()
        //glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, ma)

        intArrayOf(0).let { x ->
            intArrayOf(0).let { y ->
                intArrayOf(0).let { cif ->
                    val bb = STBImage.stbi_load(norName, x, y, cif, 0)
                    val format = if (cif.single() == 3) GL_RGB else GL_RGBA
                    glTexImage2D(
                        GL_TEXTURE_2D, 0, format,
                        x.single(), y.single(), 0, format, GL_UNSIGNED_BYTE, bb
                    )
                    STBImage.stbi_image_free(bb)
                }
            }
        }
    }


    private fun initCol() {
        colTex = intArrayOf(0).also(::glGenTextures).single()

        glActiveTexture(GL_TEXTURE4)
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, colTex)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_REPEAT)

        // val ma = floatArrayOf(0f).also { glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, it) }.single()
        // glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, ma)

        intArrayOf(0).let { x ->
            intArrayOf(0).let { y ->
                intArrayOf(0).let { cif ->
                    val bb = STBImage.stbi_load(texName, x, y, cif, 0)
                    val format = if (cif.single() == 3) GL_RGB else GL_RGBA
                    glTexImage2D(
                        GL_TEXTURE_2D, 0, format,
                        x.single(), y.single(), 0, format, GL_UNSIGNED_BYTE, bb
                    )
                    STBImage.stbi_image_free(bb)
                }
            }
        }
    }

    /**
     * Initialize the VBO.
     */
    private fun initVBO() {
        val gridData = mutableListOf<Float>()

        var k = -1f
        while (k < 1.0f) {
            var j = -1f
            while (j < 1.0f) {
                var i = -1f
                while (i < 1.0f) {
                    gridData += i
                    gridData += j
                    gridData += k
                    i += cubeStep.x
                }
                j += cubeStep.y
            }
            k += cubeStep.z
        }

        gridDataBuffId = intArrayOf(0).also(::glGenBuffers).single()
        gridDataBuffSize = gridData.size

        glBindBuffer(GL_ARRAY_BUFFER, gridDataBuffId)
        glBufferData(GL_ARRAY_BUFFER, gridData.toFloatArray(), GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    /**
     * Initialize the light sources.
     */
    private fun initLighting() {

        val lightAmbient = floatArrayOf(0.88f, 0.65f, 0.55f, 1.0f)
        val lightDiffuse = floatArrayOf(0.91f, 0.89f, 0.76f, 1.0f)
        val lightPosition = floatArrayOf(5.0f, 5.0f, 5.0f, 1.0f)
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient)
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse)
        glLightfv(GL_LIGHT0, GL_SPECULAR, lightDiffuse)
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition)
        glEnable(GL_LIGHT0)
    }

    /**
     * Initialize the table determining the edges from assigned endpoints.
     */
    private fun initEdgeTable() {
        edgeTableTex = intArrayOf(0).also(::glGenTextures).single()

        glActiveTexture(GL_TEXTURE1)
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, edgeTableTex)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

        //We create an integer texture with new GL_EXT_texture_integer formats
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16I, 256, 1, 0, GL_RED_INTEGER, GL_INT, edgeTable)
    }

    /**
     * Initializes the table assigning edges to triangles.
     */
    private fun initTriTable() {
        triTableTex = intArrayOf(0).also(::glGenTextures).single()
        glActiveTexture(GL_TEXTURE2)
        glEnable(GL_TEXTURE_2D)

        glBindTexture(GL_TEXTURE_2D, triTableTex)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)

        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16I, 16, 256, 0, GL_RED_INTEGER, GL_INT, triTable)
    }

    /**
     * Initializes the data field.
     */
    private fun initDataField() {
        dataFieldTex = intArrayOf(0).also(::glGenTextures).single()
        glActiveTexture(GL_TEXTURE0)
        glEnable(GL_TEXTURE_3D)
        glBindTexture(GL_TEXTURE_3D, dataFieldTex)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)


        val dataField = FloatArray(dataSize.x * dataSize.y * dataSize.z)
        val center = Vector3f(dataSize.x / 2f, dataSize.y / 2f, dataSize.z / 2f)

        fun p(a: Float, p: Float, x: Float, y: Float, z: Float) =
            a * STBPerlin.stb_perlin_noise3(x / p, y / p, z / p, 0, 0, 0)

        val f = { x: Float, y: Float, z: Float ->
            var r = 0f
            r += -y
            var p = Vector3f(x, y, z)
            var d = Vector3f(
                p(16f, 250f, x, y, z),
                p(16f, 250f, y, z, x),
                p(16f, 250f, z, x, y)
            )

            r += p(50.0f, 55f, p.x, p.y, p.z)
            p.add(d)
            r += p(20.0f, 45f, p.x, p.y, p.z)
            p.add(d)
            r += p(10.0f, 35f, p.x, p.y, p.z)
            p.add(d)
            r += p(22.0f, 36f, p.x, p.y, p.z)
            p.add(d)
            r += p(14.0f, 21f, p.x, p.y, p.z)
            p.add(d)
            r += p(5.0f, 17f, p.x, p.y, p.z)
            p.add(d)
            r += p(4.0f, 8f, p.x, p.y, p.z)
            p.add(d)
            r += p(3.0f, 7f, p.x, p.y, p.z)
            r
        }

        for (k in 0 until dataSize.z)
            for (j in 0 until dataSize.y)
                for (i in 0 until dataSize.x) {
                    dataField[i + j * dataSize.x + k * dataSize.x * dataSize.y] =
                        f(i.toFloat() - center.x, j.toFloat() - center.y, k.toFloat() - center.z)
                }

        // Set 3D texture value.
        glTexImage3D(
            GL_TEXTURE_3D,
            0,
            GL_R32F,
            dataSize.x,
            dataSize.y,
            dataSize.z,
            0,
            GL_RED,
            GL_FLOAT,
            dataField
        )
    }

    /**
     * Initializes bindings to uniforms.
     */
    private fun initSamplerAssignments() {
        // Assign textures.
        glUniform1i(glGetUniformLocation(program, "dataFieldTex"), 0)
        glUniform1i(glGetUniformLocation(program, "edgeTableTex"), 1)
        glUniform1i(glGetUniformLocation(program, "triTableTex"), 2)
        glUniform1i(glGetUniformLocation(program, "norTex"), 3)
        glUniform1i(glGetUniformLocation(program, "colTex"), 4)

        // Assign iso level.
        glUniform1f(glGetUniformLocation(program, "isolevel"), isolevel)

        // Assign data step size.
        glUniform3f(
            glGetUniformLocation(program, "dataStep"),
            1.0f / dataSize.x,
            1.0f / dataSize.y,
            1.0f / dataSize.z
        )

        // Assign displacements.
        glUniform3f(glGetUniformLocation(program, "vertDecals[0]"), 0.0f, 0.0f, 0.0f)
        glUniform3f(glGetUniformLocation(program, "vertDecals[1]"), cubeStep.x, 0.0f, 0.0f)
        glUniform3f(glGetUniformLocation(program, "vertDecals[2]"), cubeStep.x, cubeStep.y, 0.0f)
        glUniform3f(glGetUniformLocation(program, "vertDecals[3]"), 0.0f, cubeStep.y, 0.0f)
        glUniform3f(glGetUniformLocation(program, "vertDecals[4]"), 0.0f, 0.0f, cubeStep.z)
        glUniform3f(glGetUniformLocation(program, "vertDecals[5]"), cubeStep.x, 0.0f, cubeStep.z)
        glUniform3f(glGetUniformLocation(program, "vertDecals[6]"), cubeStep.x, cubeStep.y, cubeStep.z)
        glUniform3f(glGetUniformLocation(program, "vertDecals[7]"), 0.0f, cubeStep.y, cubeStep.z)
    }

    private fun initGL() {
        glShadeModel(GL_SMOOTH)

        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LIGHTING)

        //Form multi-face view
        glEnable(GL_CULL_FACE)

        glDepthMask(true)
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        glClearDepth(1.0)
    }

    fun perspectiveGL(fovY: Double, aspect: Double, zNear: Double, zFar: Double) {
        val fH = tan(fovY / 360 * Math.PI) * zNear
        val fW = fH * aspect
        glFrustum(-fW, fW, -fH, fH, zNear, zFar)
    }

    val times = arrayListOf<Long>()
    val timesSize = 60
    override fun onDraw() {
        measureNanoTime {
            viewOrient.y += 0.01f
            //Viewport clearing
            glDepthMask(true)
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            //States setting
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_STENCIL_TEST)
            glDisable(GL_ALPHA_TEST)

            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()
            perspectiveGL(45.0, windowWidth.toDouble() / windowHeight, 0.10, 1000.0)

            //Activate modelview
            glMatrixMode(GL_MODELVIEW)
            glLoadIdentity()
            glPushMatrix()

            //View positioning
            glTranslatef(0f, 0f, -3f)
            glRotatef(viewOrient.x * 10, 1f, 0f, 0f)
            glRotatef(viewOrient.y * 10, 0f, 1f, 0f)

            glColor4f(1f, 1f, 1f, 1f)
            //Shader program binding
            glUseProgram(program)
            //Current isolevel uniform parameter setting
            glUniform1f(glGetUniformLocation(program, "isolevel"), isolevel)

            glPolygonMode(GL_FRONT, GL_FILL)

            //Initial geometries are points. One point is generated per marching cube.
            // TODO: OCT-TREE or VBOs.

            glBindBuffer(GL_ARRAY_BUFFER, gridDataBuffId)

            glEnableClientState(GL_VERTEX_ARRAY)
            glVertexPointer(3, GL_FLOAT, 0, 0L)
            glDrawArrays(GL_POINTS, 0, gridDataBuffSize)
            glDisableClientState(GL_VERTEX_ARRAY)

            glBindBuffer(GL_ARRAY_BUFFER, 0)

            // glBegin(GL_POINTS)
            // var k = -1f
            // while (k < 1.0f) {
            //     var j = -1f
            //     while (j < 1.0f) {
            //         var i = -1f
            //         while (i < 1.0f) {
            //             glVertex3f(i, j, k)
            //             i += cubeStep.x
            //         }
            //         j += cubeStep.y
            //     }
            //     k += cubeStep.z
            // }
//
            // glEnd()


            //Disable shader program
            glUseProgram(0)
            glPopMatrix()
        }.let { time ->
            times.add(time)
            if (times.size > timesSize)
                times.removeAt(0)
            val avgTime = times.average()
            windowTitle = "Frame time: ${round(avgTime / 1e6)} ms, ${round(1e9 / avgTime)} FPS"
        }
    }
}

fun main() {
    Prog().run()
}