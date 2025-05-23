package dev.shiftsad.faceMod

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class FaceMod : JavaPlugin(), PluginMessageListener, Listener {

    override fun onEnable() {
        server.messenger.registerIncomingPluginChannel(this, "sadness:face", this)
        server.messenger.registerOutgoingPluginChannel(this, "sadness:face")

        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)
    }

    @EventHandler
    fun onChat(event: PlayerChatEvent) {
        val player = event.player
        val message = event.message

        if (message == "reset") screens[player] = null
    }

    private val screens = mutableMapOf<Player, Screen?>()

    override fun onPluginMessageReceived(
        channel: String,
        player: Player,
        message: ByteArray
    ) {
        if (channel != "sadness:face") return

        val inputStream = message.inputStream()
        val image = ImageIO.read(inputStream)

        val width = 16 * 4
        val height = 9 * 4
        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = resizedImage.createGraphics()
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()

        if (screens[player] == null) {
            val screen = Screen(this, width, height, player.location, player.world)
            screens[player] = screen
        }

        val screen = screens[player] ?: return
        screen.setFromImage(image)
        screen.updateLocation(player.location.add(0.0, 2.0, 0.0))
    }
}
