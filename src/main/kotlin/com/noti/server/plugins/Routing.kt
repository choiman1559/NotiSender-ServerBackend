package com.noti.server.plugins

import io.ktor.client.engine.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        var isProcessed = false

        get("/api/{version}/{type}") {
            isProcessed = true
            if (call.parameters["version"] == "v1") {
                call.respondText("Hello World!")
            } else {
                call.respondText("Error!")
            }
        }

        get("/") {
            isProcessed = true
            call.respondRedirect("https://cuj1559.asuscomm.com")
        }
    }
}
