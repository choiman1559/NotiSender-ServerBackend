package com.noti.server.module

import com.noti.server.process.PacketProcess
import com.noti.server.process.Service
import com.noti.server.process.packet.Packet
import com.noti.server.process.packet.PacketConst

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

suspend fun doProcessPacket(call: ApplicationCall) {
    if (call.parameters["version"] == "v1") {
        val serviceType: String = call.parameters["service_type"].toString()
        PacketProcess.processRequest(call, serviceType, call.receiveText())
    } else {
        Service.replyPacket(call, Packet.makeErrorPacket(PacketConst.ERROR_ILLEGAL_ARGUMENT))
    }
}

fun Application.configureRouting() {
    val service: Service = Service.getInstance()
    service.mOnPacketProcessReplyReceiver = Service.onPacketProcessReplyReceiver { call, code, data ->
        runBlocking {
            call.respond(code, data)
            if(service.argument.isDebug) {
                println(data)
            }
        }
    }

    routing {
        post(PacketConst.API_ROUTE_SCHEMA) {
            doProcessPacket(call)
        }

        get(PacketConst.API_ROUTE_SCHEMA) {
            doProcessPacket(call)
        }

        delete(PacketConst.API_ROUTE_SCHEMA) {
            doProcessPacket(call)
        }

        put(PacketConst.API_ROUTE_SCHEMA) {
            doProcessPacket(call)
        }
    }
}
