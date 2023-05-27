package com.superwall.sdk.models.postback

import com.superwall.sdk.models.SerializableEntity
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class PostBackResponse(val status: String): SerializableEntity

@Serializable
data class PostbackRequest(val products: List<PostbackProductIdentifier>, val delay: Int? = null) {

    val postbackDelay: Double
        get() = delay?.let { it.toDouble() / 1000 } ?: Random.nextDouble(2.0, 10.0)

    val productsToPostBack: List<PostbackProductIdentifier>
        get() = products.filter { it.isiOS }

    companion object {
        fun stub() = PostbackRequest(
            products = listOf(
                PostbackProductIdentifier(
                    identifier = "123",
                    platform = "ios"
                )
            )
        )
    }
}

@Serializable
data class PostbackProductIdentifier(val identifier: String, val platform: String) {
    val isiOS: Boolean
        get() = platform.lowercase() == "ios"
}
