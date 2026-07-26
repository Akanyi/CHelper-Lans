package yancey.chelper.ui.library

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import yancey.chelper.network.library.data.AuthorInfo
import yancey.chelper.network.library.data.LibraryFunction

class LocalLibrarySupportTest {
    private fun library(
        id: String,
        name: String,
        note: String = "",
        tags: List<String> = emptyList(),
        content: String = "say hello"
    ) = LibraryFunction(
        localEntryId = id,
        name = name,
        note = note,
        tags = tags,
        content = content,
        author = AuthorInfo(name = "Tester")
    )

    @Test
    fun `全文搜索保留原始条目身份并支持多关键词`() {
        val libraries = listOf(
            library("a", "传送大厅", tags = listOf("utility"), content = "tp @s 0 80 0"),
            library("b", "红石工具", note = "自动清理", content = "kill @e[type=item]")
        )

        val result = filterAndSortLocalLibraries(libraries, "自动 item", LocalLibrarySort.DEFAULT)

        assertEquals(1, result.size)
        assertEquals("b", result.single().localEntryId)
        assertEquals(1, result.single().storageIndex)
    }

    @Test
    fun `名称排序不会修改条目身份`() {
        val result = filterAndSortLocalLibraries(
            listOf(library("z", "Zulu"), library("a", "alpha")),
            "",
            LocalLibrarySort.NAME_ASC
        )

        assertEquals(listOf("a", "z"), result.map { it.localEntryId })
    }

    @Test
    fun `导入同时兼容单对象和数组`() {
        val single = library("a", "单库")
        val array = listOf(single, library("b", "第二个"))

        assertEquals(listOf("单库"), decodeLocalLibraryImport(Json.encodeToString(single)).map { it.name })
        assertEquals(listOf("单库", "第二个"), decodeLocalLibraryImport(Json.encodeToString(array)).map { it.name })
    }

    @Test
    fun `复制副本清理云端绑定和本地身份`() {
        val duplicate = library("local", "原库").copy(
            id = 7,
            uuid = "cloud",
            autoSync = true,
            localUnsynced = true
        ).toLocalDuplicate()

        assertEquals("原库 - 副本", duplicate.name)
        assertNull(duplicate.id)
        assertNull(duplicate.uuid)
        assertNull(duplicate.localEntryId)
        assertFalse(duplicate.autoSync == true)
        assertFalse(duplicate.localUnsynced)
    }

    @Test
    fun `外链导入会清理云端身份并规范化为本地正文`() {
        val imported = library(
            "remote",
            "外链库",
            content = """
                @name=外链库
                @version=1.0
                @mcd_version=2
                @uuid=cloud-uuid

                ###Function###
                > C
                say imported
                ###End###
            """.trimIndent()
        ).copy(id = 42, uuid = "cloud-uuid").toLocalImportedCopy()

        assertNull(imported.id)
        assertNull(imported.uuid)
        assertNull(imported.localEntryId)
        assertEquals("> C\nsay imported", imported.content)
        assertTrue(imported.localIsV2 == true)
    }

    @Test
    fun `外链导入去重兼容完整源码和仅正文存储`() {
        val remote = library(
            "remote",
            "同一个库",
            content = """
                @name=同一个库
                @version=1.0
                ###Function###
                say hello
                ###End###
            """.trimIndent()
        )
        val local = library("local", "同一个库", content = "say hello")

        assertTrue(remote.hasSameLocalContent(local))
    }

    @Test
    fun `完整 MCD 不会重复套壳且正文可提取`() {
        val source = """
            @name=示例
            @version=1.0
            @mcd_version=2

            ###Function###
            > I
            say hello
            ###End###
        """.trimIndent()
        val library = library("a", "示例", content = source)

        assertEquals(source, library.toFullLocalMcd())
        assertEquals("> I\nsay hello", library.localBody())
        assertTrue(library.usesLocalMcdV2())
        assertEquals(1, Regex("###Function###").findAll(library.toFullLocalMcd()).count())
    }

    @Test
    fun `仅正文 V2 会生成规范完整源码`() {
        val source = library("a", "示例", content = "> R!t20\nsay loop")
            .copy(localIsV2 = true)
            .toFullLocalMcd()

        assertTrue(source.contains("@mcd_version=2"))
        assertTrue(source.contains("> R!t20\nsay loop"))
    }
}
