package yancey.chelper.ui.library

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import yancey.chelper.network.library.data.LibraryFunction

enum class LocalLibrarySort(val label: String) {
    DEFAULT("默认顺序"),
    NEWEST("最近添加"),
    OLDEST("最早添加"),
    NAME_ASC("名称 A-Z"),
    NAME_DESC("名称 Z-A")
}

data class LocalLibraryEntry(
    val storageIndex: Int,
    val library: LibraryFunction
) {
    val localEntryId: String get() = requireNotNull(library.localEntryId)
}

private val localLibraryJson = Json { ignoreUnknownKeys = true }
private val stateLineRegex = Regex(
    """^>\s*([ICRH_])?([?_])?([!_])?(?:t(\d+|_))?\s*$""",
    RegexOption.IGNORE_CASE
)

fun filterAndSortLocalLibraries(
    libraries: List<LibraryFunction>,
    query: String,
    sort: LocalLibrarySort
): List<LocalLibraryEntry> {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    val entries = libraries.mapIndexed { index, library -> LocalLibraryEntry(index, library) }
        .filter { entry -> terms.all { term -> entry.library.matchesLocalQuery(term) } }

    return when (sort) {
        LocalLibrarySort.DEFAULT -> entries
        LocalLibrarySort.NEWEST -> entries.sortedByDescending { it.storageIndex }
        LocalLibrarySort.OLDEST -> entries.sortedBy { it.storageIndex }
        LocalLibrarySort.NAME_ASC -> entries.sortedWith(localLibraryNameComparator())
        LocalLibrarySort.NAME_DESC -> entries.sortedWith(localLibraryNameComparator().reversed())
    }
}

private fun LibraryFunction.matchesLocalQuery(term: String): Boolean =
    sequenceOf(
        name,
        note,
        version,
        author?.name,
        content
    ).filterNotNull().any { it.contains(term, ignoreCase = true) } ||
            tags.orEmpty().any { it.contains(term, ignoreCase = true) }

private fun localLibraryNameComparator(): Comparator<LocalLibraryEntry> =
    Comparator { first, second ->
        val nameOrder = String.CASE_INSENSITIVE_ORDER.compare(
            first.library.name.orEmpty(),
            second.library.name.orEmpty()
        )
        if (nameOrder != 0) {
            nameOrder
        } else {
            compareValuesBy(first, second, { it.storageIndex }, { it.localEntryId })
        }
    }

fun decodeLocalLibraryImport(raw: String): List<LibraryFunction> {
    return when (val root = localLibraryJson.parseToJsonElement(raw)) {
        is JsonArray -> root.map { localLibraryJson.decodeFromJsonElement<LibraryFunction>(it) }
        is JsonObject -> listOf(localLibraryJson.decodeFromJsonElement<LibraryFunction>(root))
        else -> error("命令库备份必须是 JSON 对象或数组")
    }
}

fun LibraryFunction.toLocalDuplicate(): LibraryFunction = copy(
    id = null,
    uuid = null,
    name = "${name?.takeIf(String::isNotBlank) ?: "未命名"} - 副本",
    createdAt = null,
    preview = null,
    likeCount = null,
    isLiked = null,
    isFavorited = null,
    hasPublicVersion = null,
    isPublish = null,
    isOwner = null,
    chainData = null,
    autoSync = false,
    hasUnsyncedChanges = null,
    localUnsynced = false,
    localEntryId = null
)

fun LibraryFunction.toLocalImportedCopy(): LibraryFunction = copy(
    id = null,
    uuid = null,
    content = localBody(),
    createdAt = null,
    preview = null,
    likeCount = null,
    isLiked = null,
    isFavorited = null,
    hasPublicVersion = null,
    isPublish = null,
    isOwner = null,
    chainData = null,
    autoSync = false,
    hasUnsyncedChanges = null,
    localUnsynced = false,
    localEntryId = null,
    localIsV2 = usesLocalMcdV2()
)

fun LibraryFunction.hasSameLocalContent(other: LibraryFunction): Boolean =
    name.orEmpty().trim() == other.name.orEmpty().trim() &&
            localBody().trim() == other.localBody().trim()

fun LibraryFunction.localBody(): String {
    val source = content.orEmpty()
    val lines = source.lines()
    val start = lines.indexOfFirst { it.trim() == "###Function###" }
    if (start < 0) {
        val metadataKeys = listOf(
            "@name=", "@version=", "@tags=", "@note=", "@mcd_version=", "@uuid="
        )
        return lines.filterNot { line ->
            val trimmed = line.trim()
            metadataKeys.any { trimmed.startsWith(it, ignoreCase = true) }
        }.dropWhile(String::isBlank).joinToString("\n")
    }
    val end = lines.withIndex()
        .firstOrNull { (index, line) -> index > start && line.trim() == "###End###" }
        ?.index ?: -1
    if (end < 0) return source
    return lines.subList(start + 1, end).joinToString("\n").trim('\n', '\r')
}

fun LibraryFunction.usesLocalMcdV2(): Boolean {
    localIsV2?.let { return it }
    val source = content.orEmpty()
    if (source.lineSequence().any { it.trim().matches(Regex("""@mcd_version\s*=\s*2""")) }) {
        return true
    }
    return localBody().lineSequence().any { stateLineRegex.matches(it.trim()) }
}

fun LibraryFunction.toFullLocalMcd(): String {
    val source = content.orEmpty()
    val lines = source.lines()
    val start = lines.indexOfFirst { it.trim() == "###Function###" }
    val end = lines.withIndex()
        .firstOrNull { (index, line) -> index > start && line.trim() == "###End###" }
        ?.index ?: -1
    if (start >= 0 && end > start) return source

    return buildString {
        append("@name=${name.orEmpty()}\n")
        append("@version=${version?.ifBlank { "1.0.0" } ?: "1.0.0"}\n")
        if (!tags.isNullOrEmpty()) append("@tags=${tags!!.joinToString(",")}\n")
        append("@note=${note.orEmpty().replace('\n', ' ').replace('\r', ' ')}\n")
        if (usesLocalMcdV2()) append("@mcd_version=2\n")
        if (!uuid.isNullOrBlank()) append("@uuid=$uuid\n")
        append("\n###Function###\n")
        append(localBody())
        append("\n###End###")
    }
}
