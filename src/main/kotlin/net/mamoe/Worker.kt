package net.mamoe

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.mamoe.protocol.upload
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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

@ExperimentalContracts
inline fun <T:Worker> T.tryNTimes(n:Int = 2, builder: T.() -> Unit):Exception? {
    contract {
        callsInPlace(builder, InvocationKind.AT_LEAST_ONCE)
    }
    var lastException: Exception? = null
    repeat(n) {
        try {
            builder.invoke(this)
            return null
        } catch (e: Exception) {
            lastException = e
        }
    }
    return lastException
}

fun buildDefaultHttpClient():HttpClient{
    return HttpClient(CIO){
        expectSuccess = false
        engine {
            requestTimeout = 600_000
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 600_000
            requestTimeoutMillis = 600_000
            connectTimeoutMillis = 600_000
        }
    }
}

open class HttpWorker @KtorExperimentalAPI constructor(
        logPrefix:String = "Http",
        enableLog: Boolean = true,
        _coroutineContext:CoroutineContext = EmptyCoroutineContext,
        val client:HttpClient = buildDefaultHttpClient()
        ):Worker(logPrefix,enableLog,_coroutineContext)


@KtorExperimentalAPI
class OneDriveWorker @KtorExperimentalAPI constructor(
        logPrefix:String = "OneDrive",
        enableLog: Boolean = true,
        _coroutineContext:CoroutineContext = EmptyCoroutineContext,
        client:HttpClient = buildDefaultHttpClient(),
        internal val authProvider: AuthProvider
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

        val result = client.request<String> {
            url.takeFrom((authProvider.getBaseUrl() + path.removePrefix("/")).also {
                log("Sending a " + _method.value + " to " + it )
            })
            method = _method
            if(data.isNotEmpty()) {
                body = data.toJson().also {
                    log("With body of $it")
                }
                contentType(ContentType.Application.Json)
            }
            headers {
                accept(ContentType.Application.Json)
                header("Authorization","${accessToken.type} ${accessToken.token}")
            }
        }

        return result.decodeJson()
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

    suspend fun get(path:String):JsonObject = get(path, mapOf())

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
fun oneDriveWorker(logPrefix:String = "Http", enableLog: Boolean = true, _coroutineContext:CoroutineContext = EmptyCoroutineContext, client:HttpClient = buildDefaultHttpClient(), block:() -> AuthProvider):OneDriveWorker{
    return OneDriveWorker(logPrefix,enableLog,_coroutineContext,client,block.invoke())
}


fun Collection<*>.toJson():String = Gson().toJson(this)
fun Map<String,Any>.toJson():String{
    return Gson().toJson(this)
}

fun String.decodeJsonList():List<String> {
    if(this.isBlank() || this == "{}"){
        return listOf()
    }
    return Gson().fromJson(this,object : TypeToken<List<String>>(){}.type)
}
fun String.decodeJson(): JsonObject {
    return  JsonParser.parseString(this).asJsonObject
}

fun json(
        vararg pair: Pair<String,Any>
):String{
    return pair.toMap().toJson()
}

fun buildJson(
        block: MutableMap<String,Any>.() -> Unit
):String{
    return with(mutableMapOf<String,Any>()){
        block.invoke(this)
        this.toJson()
    }
}

fun buildMap(
        block: MutableMap<String,Any>.() -> Unit
):Map<String,Any>{
    return with(mutableMapOf<String,Any>()){
        block.invoke(this)
        this
    }
}