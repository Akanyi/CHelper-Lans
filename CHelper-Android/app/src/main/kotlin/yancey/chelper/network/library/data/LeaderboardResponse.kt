package yancey.chelper.network.library.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardUser(
    var id: Int? = null,
    @SerialName("avatar_url") var avatarUrl: String? = null,
    var nickname: String? = null,
    var tier: Int? = null,
    @SerialName("total_likes") var totalLikes: Int? = null,
    @SerialName("total_functions") var totalFunctions: Int? = null
)

@Serializable
data class LeaderboardData(
    var leaderboard: List<LeaderboardUser>? = null
)
