package yancey.chelper.network.library.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import yancey.chelper.network.library.service.CommandLabPublicService
import yancey.chelper.network.library.service.CommandLabUserService

class ActivityDataParseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `点赞积分回执兼容 snake case`() {
        val response = json.decodeFromString<CommandLabPublicService.LibraryLikeResponse>(
            """{"like_count":8,"is_liked":true,"daily_first_like_points":1.0,"points_awarded":1.0}"""
        )

        assertEquals(8, response.likeCount)
        assertTrue(response.isLiked == true)
        assertEquals(1.0, response.dailyFirstLikePoints ?: 0.0, 0.0)
        assertEquals(1.0, response.pointsAwarded ?: 0.0, 0.0)
    }

    @Test
    fun `活动摘要和 Tier 时间线能完整解析`() {
        val raw = """
            {
              "points":21.5,
              "total_earned":30.0,
              "total_spent":8.5,
              "total_reversed":0.0,
              "earned_by_type":{"daily_first_like":1.0},
              "tier":{
                "base_tier":0,
                "effective_tier":1,
                "effective_tier_name":"基础认证",
                "tier_summaries":[{"tier":1,"tier_name":"基础认证","status":"active"}],
                "effective_timeline":[{"tier":1,"tier_name":"基础认证","starts_at":"2026-07-26T12:00:00"}],
                "grants":[]
              }
            }
        """.trimIndent()

        val summary = json.decodeFromString<ActivitySummary>(raw)
        assertEquals(21.5, summary.points ?: 0.0, 0.0)
        assertEquals(1, summary.tier?.effectiveTier)
        assertEquals("active", summary.tier?.tierSummaries?.first()?.status)
    }

    @Test
    fun `收藏响应读取服务端状态`() {
        val response = json.decodeFromString<CommandLabUserService.FavoriteLibraryResponse>(
            """{"is_favorited":true,"uuid":"library-uuid"}"""
        )

        assertTrue(response.isFavorited == true)
        assertEquals("library-uuid", response.uuid)
    }
}
