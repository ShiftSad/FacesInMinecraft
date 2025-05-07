package dev.shiftsad.faceMod

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sin

class Screen(
    val width: Int,
    val height: Int,
    var pos: Location,
    val world: World,
) {
    private val pixels: Array<TextDisplay> = Array(width * height) { index ->
        val x = index % width
        val y = index / width

        // Each pixel is 1/8th of a block apart, adjust as needed
        val pixelPos = pos.clone().add(
            x / 32.0,
            y / 32.0,
            0.0
        )
        spawnTextPixel(pixelPos, world)
    }

    operator fun get(x: Int, y: Int): TextDisplay {
        require(x in 0 until width && y in 0 until height) { "Coordinates out of bounds" }
        return pixels[y * width + x]
    }

    operator fun set(x: Int, y: Int, color: Int) {
        require(x in 0 until width && y in 0 until height) { "Coordinates out of bounds" }
        pixels[y * width + x].setColor(color)
    }

    fun updateLocation(newPos: Location) {
        val pixelSize = 1.0 / 32.0

        val halfW = width * pixelSize / 2.0

        val yawRad = Math.toRadians(newPos.yaw.toDouble())
        val c = cos(yawRad)
        val s = sin(yawRad)

        pixels.forEachIndexed { index, pixel ->
            val x = index % width
            val y = index / width

            val localX = (x + 0.5) * pixelSize - halfW
            val localZ = 0.0

            val worldX =  localX * c - localZ * s
            val worldZ =  localX * s + localZ * c

            val target = newPos.clone().add(worldX, y / 32.0, worldZ)

            pixel.teleport(target)
            pixel.setRotation(newPos.yaw, 0f)
            pixel.teleportDuration = 1
        }

        pos = newPos.clone()
    }

    fun paintBucket(color: Int) {
        pixels.forEach { it.setColor(color) }
    }

    fun drawRectangle(startX: Int, startY: Int, rectWidth: Int, rectHeight: Int, color: Int) {
        for (x in startX until (startX + rectWidth)) for (y in startY until (startY + rectHeight))
            if (x in 0 until width && y in 0 until height)
                this[x, y] = color
    }

    fun drawCircle(cx: Int, cy: Int, radius: Int, color: Int) {
        val rSquared = radius * radius
        for (x in (cx - radius)..(cx + radius)) {
            for (y in (cy - radius)..(cy + radius)) {
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= rSquared) {
                    if (x in 0 until width && y in 0 until height) {
                        this[x, y] = color
                    }
                }
            }
        }
    }

    private fun spawnTextPixel(loc: Location, world: World): TextDisplay {
        val entity = world.spawnEntity(loc, EntityType.TEXT_DISPLAY) as TextDisplay
        entity.text(Component.text("█", NamedTextColor.WHITE)) // Unicode full block for a pixel
        entity.isCustomNameVisible = false
        entity.billboard = org.bukkit.entity.Display.Billboard.FIXED
        entity.backgroundColor = Color.fromARGB(0, 0, 0, 0) // Transparent background
        entity.isShadowed = false
        entity.isSeeThrough = true
        entity.lineWidth = 0
        entity.brightness = null
        entity.interpolationDuration = 0
        entity.interpolationDelay = 0
        entity.viewRange = 128.0f
        entity.textOpacity = 254.toByte()
        entity.transformation = Transformation(
            entity.transformation.translation,
            entity.transformation.leftRotation,
            Vector3f(0.25f, 0.25f, 0.25f),
            entity.transformation.rightRotation,
        )
        return entity
    }

    // Extension function to set color
    private fun TextDisplay.setColor(argb: Int) {
        val pixel = argb.pixel()
        this.text(this.text().color(TextColor.color(pixel.red, pixel.green, pixel.blue)))
    }

    fun setFromImage(image: BufferedImage) {
        val minWidth = minOf(width, image.width)
        val minHeight = minOf(height, image.height)
        for (y in 0 until minHeight) {
            for (x in 0 until minWidth) {
                val argb = image.getRGB(x, minHeight - 1 - y)
                this[x, y] = argb
            }
        }
    }
}

data class PixelData(
    val alpha: Int,
    val red: Int,
    val green: Int,
    val blue: Int
) {
    fun int() = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

fun Int.pixel() = PixelData(
    (this shr 24) and 0xFF,
    (this shr 16) and 0xFF,
    (this shr 8) and 0xFF,
    this and 0xFF,
)