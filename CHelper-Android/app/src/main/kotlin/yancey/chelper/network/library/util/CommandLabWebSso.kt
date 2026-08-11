package yancey.chelper.network.library.util

import java.net.URI

object CommandLabWebSso {
    private val invitePaths = setOf(
        "/invite",
        "/invite/title",
        "/invite/quota",
        "/invite/points",
    )

    /**
     * 只把受支持的 CommandLab 邀请链接转换为后端 authorize 接口需要的站内 next。
     */
    fun inviteNext(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        if (uri.host?.lowercase() !in setOf("abyssous.site", "www.abyssous.site")) return null

        val path = uri.rawPath ?: return null
        if (path !in invitePaths) return null

        val query = uri.rawQuery?.takeIf { it.isNotBlank() } ?: return null
        val hasInviteCode = query.split('&').any { parameter ->
            parameter.substringBefore('=') == "code" &&
                    parameter.substringAfter('=', missingDelimiterValue = "").isNotBlank()
        }
        if (!hasInviteCode) return null

        return "$path?$query"
    }
}
