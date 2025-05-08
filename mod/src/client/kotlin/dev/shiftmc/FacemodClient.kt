package dev.shiftmc

import com.github.sarxos.webcam.Webcam
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.command.CommandSource
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket
import net.minecraft.text.Text
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.imageio.ImageIO

object FacemodClient : ClientModInitializer {

	private var camera: Webcam? = null
	private var webcamNames: List<String> = emptyList()
	private var webcamsInitialized = false
	private var isOpen = false
	private var lastFrame: BufferedImage? = null
	private val cameraExecutor = Executors.newSingleThreadExecutor()
	private var cameraJob: Job? = null

	override fun onInitializeClient() {
		PayloadTypeRegistry.playC2S().register(FabricPluginMessage.CHANNEL_ID, FabricPluginMessage.CODEC);
		PayloadTypeRegistry.playS2C().register(FabricPluginMessage.CHANNEL_ID, FabricPluginMessage.CODEC);

		cameraExecutor.submit {
			webcamNames = try {
				Webcam.getWebcams().map { it.name }
			} catch (_: Exception) {
				emptyList()
			}
			webcamsInitialized = true
		}

		ClientLifecycleEvents.CLIENT_STOPPING.register {
			closeCamera()
			cameraExecutor.shutdown()
		}

		ClientTickEvents.END_CLIENT_TICK.register {
			if (!isOpen) return@register
			val currentFrame = lastFrame ?: return@register
			val bytes = ByteArrayOutputStream().use { baos ->
				ImageIO.write(currentFrame, "png", baos)
				baos.toByteArray()
			}

			val packet = FabricPluginMessage(bytes)
			it.player?.networkHandler?.sendPacket(
				CustomPayloadC2SPacket(packet)
			)
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommandManager.literal("camera")
				.then(
					ClientCommandManager.literal("list")
						.executes {
							if (webcamNames.isEmpty()) {
								it.source.sendFeedback(Text.literal("Nenhuma câmera encontrada"))
							} else {
								it.source.sendFeedback(Text.literal("Câmeras disponíveis: ${webcamNames.joinToString()}"))
							}
							1
						}
				)
				.then(
					ClientCommandManager.literal("stop")
						.executes {
							closeCamera()
							it.source.sendFeedback(Text.literal("Câmera fechada"))
							1
						}
				)
				.then(
					ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.suggests { context, builder ->
							CommandSource.suggestMatching(webcamNames, builder)
						}
						.executes {
							val cameraName = it.getArgument("name", String::class.java)

							it.source.sendFeedback(Text.literal("Abrindo câmera: $cameraName..."))

							cameraExecutor.submit {
								try {
									closeCamera()

									camera = Webcam.getWebcamByName(cameraName)
									camera?.open(true)

									if (camera?.isOpen == true) {
										isOpen = true
										startCameraCapture()

										it.source.sendFeedback(Text.literal("Câmera aberta com sucesso: $cameraName"))
									} else {
										it.source.sendError(Text.of("Falha ao abrir câmera: $cameraName"))
									}
								} catch (e: Exception) {
									it.source.sendError(Text.of("Erro ao abrir câmera: ${e.message}"))
									isOpen = false
								}
							}
							1
						}
				)
			)
		}
	}

	private fun startCameraCapture() {
		cameraJob = CoroutineScope(cameraExecutor.asCoroutineDispatcher()).launch {
			while (isOpen && isActive) {
				try {
					camera?.let { cam ->
						if (cam.isOpen) {
							lastFrame = resize(cam.image, 16 * 5, 9 * 5)
						}
					}
					delay(50)
				} catch (e: Exception) {
					println("Erro na captura: ${e.message}")
				}
			}
		}
	}

	private fun closeCamera() {
		cameraJob?.cancel()
		isOpen = false
		camera?.close()
		camera = null
		lastFrame = null
	}

	private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage {
		val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
		val g = resized.createGraphics()
		g.drawImage(image, 0, 0, width, height, null)
		g.dispose()
		return resized
	}
}