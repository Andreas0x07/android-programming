package com.example.bugsgame.widget

import okhttp3.ResponseBody
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Streaming

interface CbrApiService {
    @Headers(
        "User-Agent: Mozilla/5.0",
        "Accept: application/xml"
    )
    @Streaming
    @GET("scripts/xml_metall.asp")
    suspend fun getDailyRates(
        @Query("date_req1") date_req1: String,
        @Query("date_req2") date_req2: String
    ): ResponseBody
}

@Root(name = "Metall", strict = false)
data class Metals(
    @field:ElementList(inline = true, name = "Record", required = false)
    var metallList: MutableList<MetallRecord> = mutableListOf()
)

@Root(name = "Record", strict = false)
data class MetallRecord(
    @field:Attribute(name = "Date", required = false)
    var date: String? = null,

    @field:Attribute(name = "Code")
    var code: String = "",

    @field:Element(name = "Sell")
    var price: String = ""
)

object RetrofitClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val instance: CbrApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .build()
            .create(CbrApiService::class.java)
    }
}