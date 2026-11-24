package de.monsterfinder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val location: Location,
    @SerialName("radius_km") val radiusKm: Double,
    val pfand: Double = 0.25,
    @SerialName("updated_at") val updatedAt: String,
    val sources: Map<String, String> = emptyMap(),
    val results: List<Deal> = emptyList(),
    val cached: Boolean = false,
)

@Serializable
data class Location(val plz: String, val city: String, val lat: Double, val lon: Double)

@Serializable
data class Deal(
    val kind: String,
    val chain: String,
    @SerialName("chain_name") val chainName: String,
    val title: String,
    val description: String = "",
    val price: Double,
    val liters: Double,
    val cans: Int,
    @SerialName("price_per_can") val pricePerCan: Double,
    @SerialName("price_per_can_with_pfand") val pricePerCanWithPfand: Double,
    @SerialName("price_per_liter") val pricePerLiter: Double,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
    val store: Store? = null,
) {
    val isOffer get() = kind == "offer"
}

@Serializable
data class Store(
    val name: String,
    val address: String = "",
    val lat: Double,
    val lon: Double,
    @SerialName("distance_km") val distanceKm: Double,
)
