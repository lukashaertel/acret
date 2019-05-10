package eu.metatools.acret

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun createShader(resource: String, type: Int): Int {
    fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
        val newBuffer = BufferUtils.createByteBuffer(newCapacity)
        buffer.flip()
        newBuffer.put(buffer)
        return newBuffer
    }

    fun ioResourceToByteBuffer(resource: String, bufferSize: Int): ByteBuffer {
        var buffer: ByteBuffer
        val url = Thread.currentThread().contextClassLoader.getResource(resource)
        val file = File(url!!.file)
        if (file.isFile) {
            val fis = FileInputStream(file)
            val fc = fis.channel
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
            fc.close()
            fis.close()
        } else {
            buffer = BufferUtils.createByteBuffer(bufferSize)
            val source = url.openStream() ?: throw FileNotFoundException(resource)
            try {
                val buf = ByteArray(8192)
                while (true) {
                    val bytes = source.read(buf, 0, buf.size)
                    if (bytes == -1)
                        break
                    if (buffer.remaining() < bytes)
                        buffer = resizeBuffer(buffer, buffer.capacity() * 2)
                    buffer.put(buf, 0, bytes)
                }
                buffer.flip()
            } finally {
                source.close()
            }
        }
        return buffer
    }

    val shader = glCreateShader(type)

    val source = ioResourceToByteBuffer(resource, 8192)

    val strings = BufferUtils.createPointerBuffer(1)
    val lengths = BufferUtils.createIntBuffer(1)

    strings.put(0, source)
    lengths.put(0, source.remaining())

    glShaderSource(shader, strings, lengths)
    glCompileShader(shader)

    val compiled = glGetShaderi(shader, GL_COMPILE_STATUS)
    val shaderLog = glGetShaderInfoLog(shader)

    if (shaderLog.trim { it <= ' ' }.isNotEmpty()) {
        System.err.println(shaderLog)
    }
    if (compiled == 0) {
        throw AssertionError("Could not compile shader")
    }
    return shader
}