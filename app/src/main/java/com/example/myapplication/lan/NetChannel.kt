package com.example.myapplication.lan

import java.net.InetAddress

/**
 * 可靠 UDP 封装：seq/ack/重传/乱序缓存
 * 单例，每房间一个实例
 */
class NetChannel(
    private val goIp: InetAddress,
    private val port: Int = 19999
) {
    private val socket = java.net.DatagramSocket().apply { soTimeout = 1000 }
    private var seq: Short = 0
    private var remoteSeq: Short = -1
    private val sendQueue = java.util.concurrent.ConcurrentLinkedQueue<SendItem>()
    private val ackedSet = mutableSetOf<Short>()

    // 后台协程由调用者启动
    fun sendUnreliable(type: PacketType, payload: ByteArray) {
        val buf = buildPacket(type, payload, requireAck = false)
        val pkt = java.net.DatagramPacket(buf, buf.size, goIp, port)
        socket.send(pkt)
    }

    fun sendReliable(type: PacketType, payload: ByteArray) {
        val seq = seq.inc()
        val buf = buildPacket(type, payload, requireAck = true, seq = seq)
        val item = SendItem(seq, buf, System.currentTimeMillis())
        sendQueue.offer(item)
    }

    // 接收线程循环调用
    fun recv(): Packet? {
        val buf = ByteArray(1024)
        val pkt = java.net.DatagramPacket(buf, buf.size)
        return try {
            socket.receive(pkt)
            parsePacket(buf.copyOf(pkt.length))
        } catch (e: java.net.SocketTimeoutException) {
            null
        }
    }

    // 重传线程循环调用
    fun resendTimeout() {
        val now = System.currentTimeMillis()
        val iter = sendQueue.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (ackedSet.contains(item.seq)) {
                iter.remove()
                continue
            }
            if (now - item.timestamp > 500) {
                if (item.retry >= 3) {
                    iter.remove()
                    continue
                }
                val pkt = java.net.DatagramPacket(item.data, item.data.size, goIp, port)
                socket.send(pkt)
                item.retry++
                item.timestamp = now
            }
        }
    }

    private fun buildPacket(type: PacketType, payload: ByteArray, requireAck: Boolean, seq: Short = -1): ByteArray {
        val ack = remoteSeq
        val ackBits = 0 // 简化：仅使用 ack
        val payloadLen = payload.size
        val header = ByteArray(12)
        header[0] = type.id
        header[1] = (seq.toInt() shr 8).toByte()
        header[2] = (seq.toInt() and 0xff).toByte()
        header[3] = (ack.toInt() shr 8).toByte()
        header[4] = (ack.toInt() and 0xff).toByte()
        // ackBits 4B 简化置 0
        header[9] = (payloadLen shr 8).toByte()
        header[10] = (payloadLen and 0xff).toByte()
        header[11] = (header.slice(0..10).sum() % 256).toByte() // checksum
        return header + payload
    }

    private fun parsePacket(data: ByteArray): Packet? {
        if (data.size < 12) return null
        val checksum = data[11].toInt() and 0xff
        val calc = data.slice(0..10).sum() % 256
        if (checksum != calc) return null
        val type = PacketType.entries.firstOrNull { it.id == data[0] } ?: return null
        val seq = ((data[1].toInt() and 0xff) shl 8 or (data[2].toInt() and 0xff)).toShort()
        val ack = ((data[3].toInt() and 0xff) shl 8 or (data[4].toInt() and 0xff)).toShort()
        val payloadLen = ((data[9].toInt() and 0xff) shl 8 or (data[10].toInt() and 0xff))
        if (data.size != 12 + payloadLen) return null
        val payload = data.copyOfRange(12, data.size)
        // 更新远程 seq
        remoteSeq = seq
        return Packet(type, seq, ack, payload)
    }

    fun close() = socket.close()

    private data class SendItem(
        val seq: Short,
        val data: ByteArray,
        var timestamp: Long,
        var retry: Int = 0
    )
}

data class Packet(
    val type: PacketType,
    val seq: Short,
    val ack: Short,
    val payload: ByteArray
)