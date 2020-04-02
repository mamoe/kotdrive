package net.mamoe.protocol

import io.ktor.util.KtorExperimentalAPI
import net.mamoe.OneDriveWorker


data class Driver(
        val id:String, val type:String
)

@KtorExperimentalAPI
suspend fun OneDriveWorker.getDrive():Driver {
    return with(this.get("/drive")) {
        Driver(this.get("id").asString, this.get("driveType").asString)
    }
}