package com.sputnik.fmsynthesizer

import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun generateIcons(rootDir: File) {
    val svgFile = File(rootDir, "webApp/src/webMain/resources/favicon.svg")
    if (!svgFile.exists()) {
        println("SVG file not found at ${svgFile.absolutePath}")
        return
    }

    val svgData = Data.makeFromBytes(svgFile.readBytes())
    val svgDom = SVGDOM(svgData)

    val outputDir = File(rootDir, "desktopApp/src/main/resources")
    outputDir.mkdirs()

    val composeResDir = File(rootDir, "shared/src/commonMain/composeResources/drawable")
    composeResDir.mkdirs()

    val png512File = File(outputDir, "icon.png")
    renderPng(svgDom, 512, 512, png512File)

    // Also write to commonMain composeResources so Res.drawable.app_icon is available across common code
    File(composeResDir, "app_icon.png").writeBytes(png512File.readBytes())

    val png256File = File(outputDir, "icon256.png")
    renderPng(svgDom, 256, 256, png256File)

    val icoFile = File(outputDir, "icon.ico")
    writeIco(png256File.readBytes(), icoFile)

    val icnsFile = File(outputDir, "icon.icns")
    icnsFile.writeBytes(png512File.readBytes())

    png256File.delete()
    println("Icons generated successfully in ${outputDir.absolutePath}")
}

private fun renderPng(svgDom: SVGDOM, width: Int, height: Int, outputFile: File) {
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    svgDom.setContainerSize(width.toFloat(), height.toFloat())
    svgDom.render(canvas)

    val image = surface.makeImageSnapshot()
    val pngData = image.encodeToData(EncodedImageFormat.PNG)
        ?: error("Failed to encode image to PNG")

    outputFile.writeBytes(pngData.bytes)
}

private fun writeIco(pngBytes: ByteArray, outputFile: File) {
    val headerAndDirSize = 6 + 16
    val buffer = ByteBuffer.allocate(headerAndDirSize + pngBytes.size).order(ByteOrder.LITTLE_ENDIAN)

    // ICO Header (6 bytes)
    buffer.putShort(0.toShort()) // Reserved
    buffer.putShort(1.toShort()) // Type (1 = ICO)
    buffer.putShort(1.toShort()) // Number of images (1)

    // Directory Entry (16 bytes)
    buffer.put(0.toByte()) // Width (256 -> 0)
    buffer.put(0.toByte()) // Height (256 -> 0)
    buffer.put(0.toByte()) // Color count (0 = >=256)
    buffer.put(0.toByte()) // Reserved
    buffer.putShort(1.toShort()) // Color planes
    buffer.putShort(32.toShort()) // Bits per pixel (32)
    buffer.putInt(pngBytes.size) // Image size
    buffer.putInt(headerAndDirSize) // Image offset (22)

    buffer.put(pngBytes) // PNG byte data

    outputFile.writeBytes(buffer.array())
}

fun main(args: Array<String>) {
    val rootDir = if (args.isNotEmpty()) File(args[0]) else {
        var dir = File(".").canonicalFile
        while (dir.parentFile != null && !File(dir, "webApp").exists()) {
            dir = dir.parentFile
        }
        dir
    }
    generateIcons(rootDir)
}
