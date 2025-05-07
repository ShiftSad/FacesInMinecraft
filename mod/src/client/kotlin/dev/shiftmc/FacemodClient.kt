package dev.shiftmc

import com.github.sarxos.webcam.Webcam
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

object FacemodClient : ClientModInitializer {

	private var camera: Webcam? = null
	private var webcamNames: List<String> = emptyList()
	private var webcamsInitialized = false
	private var isOpen = false

	override fun onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register {
			if (!webcamsInitialized) {
				webcamNames = try {
					Webcam.getWebcams().map { it.name }
				} catch (_: Exception) {
					emptyList()
				}
				webcamsInitialized = true
			}

			if (!isOpen) return@register
			val path = Path.of("camera")
			if (!Files.exists(path)) {
				Files.createDirectories(path)
			}
			val file = path.resolve("${System.currentTimeMillis()}.png")
			camera?.image?.let { image ->
				val outputStream = Files.newOutputStream(file)
				ImageIO.write(image, "png", outputStream)
				outputStream.close()
			}
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(ClientCommandManager.literal("camera")
				.then(
					ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.suggests { context, builder ->
							CommandSource.suggestMatching(webcamNames, builder)
						}
						.executes {
							val cameraName = it.getArgument("name", String::class.java)
							try {
								camera?.close() // Close previous camera if open
								camera = Webcam.getWebcamByName(cameraName)
								camera?.setCustomViewSizes(Dimension(64, 34))
								camera?.open()
								isOpen = true
								it.source.sendFeedback(Text.literal("Camera opened: $cameraName"))
							} catch (e: Exception) {
								it.source.sendError(Text.of("Camera not found: $cameraName"))
								isOpen = false
								return@executes 0
							}
							1
						}
				))
		}
	}
}