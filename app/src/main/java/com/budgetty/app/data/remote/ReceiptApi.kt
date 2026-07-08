package com.budgetty.app.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Base URL of the deployed `extractReceipt` Cloud Function (gen2, europe-west1). */
const val RECEIPT_API_BASE_URL = "https://extractreceipt-5izrhecgza-ew.a.run.app/"

interface ReceiptApi {
    @POST("extractReceipt")
    suspend fun extract(
        @Header("Authorization") authorization: String,
        @Body request: ExtractRequest,
    ): ExtractResponse
}
