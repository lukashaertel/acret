package eu.metatools.acret

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.Callback

abstract class Window(title: String) {
    var windowHandle = 0L
        private set

    private var backingWindowTitle = title
    var windowTitle: String
        get() = backingWindowTitle
        set(value) {
            backingWindowTitle = windowTitle

            if (windowHandle != 0L)
                glfwSetWindowTitle(windowHandle, value)
        }

    private var backingWindowWidth = 1024
    var windowWidth: Int
        get() = backingWindowWidth
        set(value) {
            backingWindowWidth = value

            if (windowHandle != 0L)
                glfwSetWindowSize(windowHandle, backingWindowWidth, backingWindowHeight)
        }

    private var backingWindowHeight = 768
    var windowHeight: Int
        get() = backingWindowHeight
        set(value) {
            backingWindowHeight = value

            if (windowHandle != 0L)
                glfwSetWindowSize(windowHandle, backingWindowWidth, backingWindowHeight)
        }

    private var backingShouldClose = false
    var shouldClose: Boolean
        get() = backingShouldClose
        set(value) {
            backingShouldClose = value
            if (windowHandle != 0L)
                glfwSetWindowShouldClose(windowHandle, backingShouldClose)
        }


    lateinit var capabilities: GLCapabilities
        private set

    private val callbackError = GLFWErrorCallback.createPrint(System.err)

    private val callbackKey = object : GLFWKeyCallback() {
        override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
            onKey(key, scancode, action, mods)
        }
    }

    private val callbackSize = object : GLFWFramebufferSizeCallback() {
        override fun invoke(window: Long, width: Int, height: Int) {
            backingWindowWidth = width
            backingWindowHeight = height
            onSize(width, height)
        }
    }

    private var callbackDebugProc: Callback? = null

    private fun initGLFW() {
        // Set error callback.
        glfwSetErrorCallback(callbackError)

        // Initialize GLFW.
        check(glfwInit())

        // Set window hints.
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        // Create window.
        windowHandle = glfwCreateWindow(windowWidth, windowHeight, windowTitle, 0L, 0L)
        check(windowHandle != 0L)

        // Set callbacks.
        glfwSetFramebufferSizeCallback(windowHandle, callbackSize)
        glfwSetKeyCallback(windowHandle, callbackKey)

        // Get initial size.
        intArrayOf(0).let { w ->
            intArrayOf(0).let { h ->
                glfwGetFramebufferSize(windowHandle, w, h)
                backingWindowWidth = w.single()
                backingWindowHeight = h.single()
                onSize(backingWindowWidth, backingWindowHeight)
            }
        }

        // Make current and show.
        glfwMakeContextCurrent(windowHandle)
        glfwSwapInterval(1)
        glfwShowWindow(windowHandle)
    }

    private fun initGL() {
        // Base init.
        capabilities = GL.createCapabilities()
        callbackDebugProc = GLUtil.setupDebugMessageCallback()

        // User init.
        onInit()
    }

    open fun onInit() {
        // No operation.
    }

    open fun onKey(key: Int, scancode: Int, action: Int, mods: Int) {
        // No operation.
    }

    open fun onSize(width: Int, height: Int) {
        // No operation.
    }

    open fun onDraw() {

    }

    fun run() {
        try {
            initGLFW()
            initGL()

            while (!glfwWindowShouldClose(windowHandle)) {
                glfwPollEvents()
                glViewport(0, 0, windowWidth, windowHeight)
                onDraw()

                glfwSwapBuffers(windowHandle)
            }

            callbackDebugProc?.free()

            callbackError.free()
            callbackKey.free()
            callbackSize.free()
            glfwDestroyWindow(windowHandle)
        } finally {
            glfwTerminate()
        }
    }
}