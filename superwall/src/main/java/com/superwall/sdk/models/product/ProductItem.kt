package com.superwall.sdk.models.product

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class Store {
    @SerialName("PLAY_STORE")
    PLAY_STORE;

    companion object {
        fun fromValue(value: String): Store {
            return when(value) {
                "PLAY_STORE" -> PLAY_STORE
                else -> throw SerializationException("Store must be PLAY_STORE, found: $value")
            }
        }
    }
}

sealed class Offer {
    @Serializable
    data class Automatic(val type: String = "AUTOMATIC") : Offer()

    @Serializable
    data class Specified(val type: String = "SPECIFIED", val offerIdentifier: String) : Offer()
}

@Serializable(with = PlayStoreProductSerializer::class)
data class PlayStoreProduct(
    val store: Store = Store.PLAY_STORE,

    val productIdentifier: String,

    val basePlanIdentifier: String,

    val offer: Offer
)

object PlayStoreProductSerializer : KSerializer<PlayStoreProduct> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlayStoreProduct")

    override fun serialize(encoder: Encoder, value: PlayStoreProduct) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
        val jsonObj = buildJsonObject {
            put("store", JsonPrimitive(value.store.name))
            put("product_identifier", JsonPrimitive(value.productIdentifier))
            put("base_plan_identifier", JsonPrimitive(value.basePlanIdentifier))
            val offer = when (val offer = value.offer) {
                is Offer.Automatic -> JsonObject(mapOf("type" to JsonPrimitive(offer.type)))
                is Offer.Specified -> JsonObject(
                    mapOf(
                        "type" to JsonPrimitive(offer.type),
                        "offer_identifier" to JsonPrimitive(offer.offerIdentifier)
                    )
                )
            }
            put("offer", offer)
        }
        jsonEncoder.encodeJsonElement(jsonObj)
    }

    override fun deserialize(decoder: Decoder): PlayStoreProduct {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonDecoder.decodeJsonElement() as JsonObject

        val store = Store.fromValue(jsonObject["store"]?.jsonPrimitive?.content ?: throw SerializationException("Store is missing"))
        val productIdentifier = jsonObject["product_identifier"]?.jsonPrimitive?.content ?: throw SerializationException("product_identifier is missing")
        val basePlanIdentifier = jsonObject["base_plan_identifier"]?.jsonPrimitive?.content ?: throw SerializationException("base_plan_identifier is missing")
        val offerJsonObject = jsonObject["offer"] as? JsonObject ?: throw SerializationException("Offer is missing")
        val type = offerJsonObject["type"]?.jsonPrimitive?.content ?: throw SerializationException("Offer type is missing")

        val offer = when (type) {
            "AUTOMATIC" -> Offer.Automatic()
            "SPECIFIED" -> {
                val offerIdentifier = offerJsonObject["offerIdentifier"]?.jsonPrimitive?.content ?: throw SerializationException("OfferIdentifier is missing")
                Offer.Specified(offerIdentifier = offerIdentifier)
            }
            else -> throw SerializationException("Unknown offer type")
        }

        return PlayStoreProduct(store, productIdentifier, basePlanIdentifier, offer)
    }
}

@Serializable(with = ProductItemSerializer::class)
data class ProductItem(
    @SerialName("reference_name")
    val name: String,

    val type: StoreProductType
) {
    sealed class StoreProductType {
        data class PlayStore(val product: PlayStoreProduct) : StoreProductType()
    }

    val id: String
        get() = when (type) {
            is StoreProductType.PlayStore -> type.product.productIdentifier
        }
}

@Serializer(forClass = ProductItem::class)
object ProductItemSerializer : KSerializer<ProductItem> {
    override fun serialize(encoder: Encoder, value: ProductItem) {
        // Create a JSON object with custom field names for serialization
        val jsonOutput = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be saved only by Json")
        val jsonObject = buildJsonObject {
            put("product", JsonPrimitive(value.name))
            put("productId", JsonPrimitive(value.id))
        }
        // Encode the JSON object
        jsonOutput.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): ProductItem {
        // Decode the JSON object
        val jsonInput = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be loaded only by Json")
        val jsonObject = jsonInput.decodeJsonElement().jsonObject

        // Extract fields using the expected names during deserialization
        val name = jsonObject["reference_name"]?.jsonPrimitive?.content ?:  throw SerializationException("Missing reference_name")
        val storeProductJsonObject = jsonObject["store_product"]?.jsonObject
            ?: throw SerializationException("Missing store_product")

        // Deserialize 'storeProduct' JSON object into the expected Kotlin data class
        val storeProduct = Json.decodeFromJsonElement<PlayStoreProduct>(storeProductJsonObject)

        return ProductItem(
            name = name,
            type = ProductItem.StoreProductType.PlayStore(storeProduct)
        )
    }
}

object ProductItemsDeserializer : KSerializer<List<ProductItem>> {
    override val descriptor: SerialDescriptor = listSerialDescriptor(ProductItem.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: List<ProductItem>) {
        encoder.encodeSerializableValue(ListSerializer(ProductItem.serializer()), value)
    }

    override fun deserialize(decoder: Decoder): List<ProductItem> {
        // Ensure we're working with JsonDecoder and thus can navigate the JSON structure
        require(decoder is JsonDecoder) // This line ensures we have a JsonDecoder

        // Decode the entire document as JsonElement
        val productsV2Element = decoder.decodeJsonElement().jsonArray

        // Use a mutable list to collect valid ProductItem instances
        val validProducts = mutableListOf<ProductItem>()

        // Process each product in the array
        for (productElement in productsV2Element) {
            try {
                val product = Json.decodeFromJsonElement<ProductItem>(productElement)
                // Check the store type and add to the list if it matches the criteria
                if (product.type is ProductItem.StoreProductType.PlayStore) {
                    validProducts.add(product)
                }
                // If the type is APP_STORE or anything else, it will simply skip adding it
            } catch (e: SerializationException) {
                // Catch and ignore items that cannot be deserialized due to unknown store types or other issues
                // Log the error or handle it as needed
            }
        }

        return validProducts
    }
}