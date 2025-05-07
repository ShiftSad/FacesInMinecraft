package dev.shiftmc

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id
import net.minecraft.util.Identifier
import org.jetbrains.annotations.NotNull

class FabricPluginMessage : CustomPayload {

    companion object {
        val CODEC: PacketCodec<RegistryByteBuf, FabricPluginMessage> =
            PacketCodec.of(
                { value, buf -> writeBytes(buf, value.data) },
                { buf -> FabricPluginMessage(buf) }
            )

        val CHANNEL_ID: Id<FabricPluginMessage> =
            Id(Identifier.of("sadness", "face"))

        private fun getWrittenBytes(@NotNull buf: PacketByteBuf): ByteArray {
            val bs = ByteArray(buf.readableBytes())
            buf.readBytes(bs)
            return bs
        }

        private fun writeBytes(@NotNull buf: PacketByteBuf, v: ByteArray) {
            buf.writeBytes(v)
        }
    }

    private val data: ByteArray

    constructor(buf: PacketByteBuf) : this(getWrittenBytes(buf))

    constructor(data: ByteArray) {
        this.data = data
    }

    override fun getId(): Id<FabricPluginMessage> = CHANNEL_ID

    fun getData(): ByteArray = data
}
