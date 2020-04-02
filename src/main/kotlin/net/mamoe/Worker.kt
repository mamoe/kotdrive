package net.mamoe

import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val sdf = SimpleDateFormat("[MM-dd]HH:mm:ss: ")

open class Worker(
        private val logPrefix:String = "Worker",
        private val enableLog:Boolean = true,
        _coroutineContext:CoroutineContext = EmptyCoroutineContext
): CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + _coroutineContext

    fun log(any:Any?){
        if(enableLog)
        println(sdf.format(Date()) + "[" + logPrefix + "]" + (any ?: "null"))
    }
}

open class HttpWorker @KtorExperimentalAPI constructor(
        logPrefix:String = "Http",
        enableLog: Boolean = true,
        _coroutineContext:CoroutineContext = EmptyCoroutineContext,
        val client:HttpClient = HttpClient(CIO)
        ):Worker(logPrefix,enableLog,_coroutineContext)


@KtorExperimentalAPI
class OneDriveWorker @KtorExperimentalAPI constructor(
        logPrefix:String = "OneDrive",
        enableLog: Boolean = true,
        _coroutineContext:CoroutineContext = EmptyCoroutineContext,
        client:HttpClient = HttpClient(CIO),
        private val authProvider: AuthProvider
):HttpWorker(logPrefix,enableLog,_coroutineContext,client){

    class DataDSLBuilder{
        val data = mutableMapOf<String,Any>()

        infix fun String.to(another: Any){
            data[this] = another
        }
    }

    suspend fun connect(
            _method:HttpMethod,
            path:String,
            data: Map<String,Any>
    ):JsonObject{
        val accessToken = authProvider.getAccessToken()

        client.request<String> {
            url.takeFrom(authProvider.getBaseUrl() + path)
            method = _method
            body = MultiPartFormDataContent(formData {
                data.forEach {
                    this.append(it.key,it.value.toString())
                }
            })
            headers {
                accept(ContentType.Application.Json)
                header("Authorization","${accessToken.type} ${accessToken.token}")
            }
        }

        return JsonObject()
    }

    suspend inline fun connect(
            method:HttpMethod,
            path:String,
            dataBuilder:DataDSLBuilder.() -> Unit
    ):JsonObject{
        return connect(method,path, DataDSLBuilder().apply { dataBuilder.invoke(this) }.data)
    }

    suspend fun post(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Post,path, data)

    suspend inline fun post(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Post,path, dataBuilder)

    suspend fun get(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Get,path, data)

    suspend inline fun get(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Get,path, dataBuilder)

    suspend fun put(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Put,path, data)

    suspend inline fun put(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Put,path, dataBuilder)

    suspend fun delete(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Delete,path, data)

    suspend inline fun delete(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Delete,path, dataBuilder)

    suspend fun patch(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Patch,path, data)

    suspend inline fun patch(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Patch,path, dataBuilder)

    suspend fun option(path:String, data: Map<String,Any>):JsonObject = connect(HttpMethod.Options,path, data)

    suspend inline fun option(path:String, dataBuilder:DataDSLBuilder.() -> Unit):JsonObject = connect(HttpMethod.Options,path, dataBuilder)
}


@KtorExperimentalAPI
fun oneDriveWorker(logPrefix:String = "Http", enableLog: Boolean = true, _coroutineContext:CoroutineContext = EmptyCoroutineContext, client:HttpClient = HttpClient(CIO), block:() -> AuthProvider):OneDriveWorker{
    return OneDriveWorker(
            logPrefix,enableLog,_coroutineContext,client,block.invoke()
    )
}