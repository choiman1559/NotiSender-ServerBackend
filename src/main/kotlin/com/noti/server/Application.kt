package com.noti.server

import com.noti.server.process.Argument
import com.noti.server.module.*
import com.noti.server.process.Service.*

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val argObj: Argument = Argument.buildFrom(args.toList())
    configureServiceInstance(argObj)

    val server = embeddedServer(Netty, port = argObj.port, host = argObj.host, module = Application::module)
        .start(wait = false)
    Runtime.getRuntime().addShutdownHook(Thread {
        onServiceDead()
        server.stop(1, 5, TimeUnit.SECONDS)
    })

    onServiceAlive()
    Thread.currentThread().join()
}

fun Application.module() {
    //configureSockets()

    configureHTTP()
    configureMonitoring()
    configureRouting()
    configureStatus()
}
