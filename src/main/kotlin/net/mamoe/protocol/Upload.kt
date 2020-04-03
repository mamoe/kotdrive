package net.mamoe.protocol

import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import net.mamoe.*
import org.jsoup.Jsoup
import java.io.File
import kotlin.contracts.ExperimentalContracts


enum class ConflictBehavior(val id:String){
    FAIL("fail"),
    RENAME("rename"),
    REPLACE("replace")
}


open class UploadException(message:String):Exception(message)
class UploadSessionCreatedFailedException():UploadException("No Upload URL Found")
class UploadNoResponse():UploadException("No Response Found")
class UploadUnknownException(message:String):UploadException(message)

@ExperimentalContracts
@KtorExperimentalAPI
suspend fun OneDriveWorker.upload(
        conflictBehavior:ConflictBehavior,
        _path:String,
        file: File
){

    val path = _path.removePrefix("/")

    val size = file.length()

    suspend fun uploadBig() {
        val resp = post("/me/drive/root:/${path}:/createUploadSession") {
            "item" to buildMap {
                this["@microsoft.graph.conflictBehavior"] = conflictBehavior.id
            }
        }

        if (!resp.has("uploadUrl")) {
            log(resp.toString())
            throw UploadSessionCreatedFailedException()
        }

        val uploadUrl = resp.get("uploadUrl").asString
        log("Big File Upload URL $uploadUrl")

        val blockSize = 327680 * 15
        var offset = 0L

        var lastResp:String? = null

        try{
            file.forEachBlockSuspending(blockSize) { buffer, read ->
                tryNTimes(3) {//// 指数策略？
                    val end = offset + read - 1
                    val accessToken = authProvider.getAccessToken()
                    lastResp = client.put<String>(uploadUrl) {
                        headers {
                            accept(ContentType.Application.Json)
                            header("Authorization", "${accessToken.type} ${accessToken.token}")
                            header("Content-Range", "bytes $offset-$end/$size")
                            log("Uploading Big File $offset-$end/$size")
                        }

                        body = if (read == blockSize) {
                            ByteArrayContent(buffer)
                        } else {
                            ByteArrayContent(buffer.copyOfRange(0, read))
                        }
                    }

                    //after success
                    offset += read
                }
            }
        }catch (e:Exception){
            //delete temp file
            val accessToken = authProvider.getAccessToken()
            client.delete<String>(uploadUrl){
                headers {
                    accept(ContentType.Application.Json)
                    header("Authorization", "${accessToken.type} ${accessToken.token}")
                }
            }
            throw e
        }

        if(lastResp == null){
            throw UploadNoResponse()
        }
        val createdInfo = lastResp!!.decodeJson()
        if(createdInfo.has("size") && createdInfo.get("size").asLong == size){
            return
        }
        throw UploadUnknownException(lastResp!!)
    }
    suspend fun uploadSmall(){
        val accessToken = authProvider.getAccessToken()
        val urlBase = authProvider.getBaseUrl()


        suspend fun uploadAsNew(){
            log("Uploading Small File directly")
            val result = client.put<String>((urlBase + "me/drive/root:/$path:/content").also {
                log(it)
            }){
                header("Authorization", "${accessToken.type} ${accessToken.token}")
                body = ByteArrayContent(file.readBytes())
            }.decodeJson()
            if(result.has("id") && result.has("name")){
                log("Upload Small File success")
                return
            }
            throw UploadUnknownException(result.toString())
        }

        uploadAsNew()
    }

    if(size > 3145728){//3MB
        uploadBig()
    }else{
        uploadSmall()
    }
}

public suspend inline fun File.forEachBlockSuspending(blockSize: Int, action: (buffer: ByteArray, bytesRead: Int) -> Unit): Unit {
    val arr = ByteArray(blockSize.coerceAtLeast(512))

    inputStream().use { input ->
        do {
            val size = input.read(arr)
            if (size <= 0) {
                break
            } else {
                action(arr, size)
            }
        } while (true)
    }
}


