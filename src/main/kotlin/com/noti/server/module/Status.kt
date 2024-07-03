package com.noti.server.module

import com.noti.server.process.Service
import com.noti.server.process.packet.Packet
import com.noti.server.process.packet.PacketConst

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*

import org.apache.http.auth.AuthenticationException

fun Application.configureStatus() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, _ ->
            Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_NOT_FOUND))
        }

        exception<Throwable> { call, cause ->
            var causeMessage: String = PacketConst.ERROR_INTERNAL_ERROR
            var responseCode: HttpStatusCode? = null

            if(cause is AuthenticationException) {
                causeMessage = PacketConst.ERROR_FORBIDDEN
                responseCode = HttpStatusCode.Forbidden
            }

            val packet: Packet? = if(Service.getInstance().argument.isDebug) {
                if(responseCode != null) {
                    Packet.makeErrorPacket(causeMessage, cause.message, responseCode)
                } else {
                    Packet.makeErrorPacket(causeMessage, cause.message)
                }
            } else {
                if(responseCode != null) {
                    Packet.makeErrorPacket(causeMessage, responseCode)
                } else {
                    Packet.makeErrorPacket(causeMessage)
                }
            }

            if(packet != null) {
                Service.replyPacket(call, packet)
            }
        }
    }
}