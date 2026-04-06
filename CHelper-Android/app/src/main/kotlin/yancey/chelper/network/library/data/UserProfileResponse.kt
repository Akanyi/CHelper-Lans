package yancey.chelper.network.library.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileData(
    var id: Int? = null,
    var nickname: String? = null,
    @SerialName("avatar_url") var avatarUrl: String? = null,
    var tier: Int? = null,
    var signature: String? = null,
    var homepage: String? = null,
    @SerialName("total_public_functions") var totalPublicFunctions: Int? = null,
    @SerialName("total_likes") var totalLikes: Int? = null,
    @SerialName("recent_functions") var recentFunctions: List<LibraryFunction>? = null,
    var email: String? = null,
    @SerialName("created_at") var createdAt: String? = null,
    @SerialName("tier_expires_at") var tierExpiresAt: String? = null
)

@Serializable
data class UpdateProfileRequest(
    var nickname: String? = null,
    @SerialName("avatar_url") var avatarUrl: String? = null,
    var homepage: String? = null,
    var signature: String? = null
)
