package project

import kotlinx.serialization.Serializable

@Serializable
data class ConfigResultItem(
    val config: String,
    val delay: Long,
)