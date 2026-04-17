package eu.kanade.tachiyomi.extension.es.kumanga

import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Calendar

class KuManga : ParsedHttpSource() {
    override val name = "KuManga"
    override val baseUrl = "https://www.kumanga.com"
    override val lang = "es"
    override val supportsLatest = true

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/mangalist?page=$page&sort=views", headers)
    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/mangalist?page=$page&sort=updated", headers)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/mangalist?page=$page&search=$query", headers)

    override fun popularMangaSelector() = ".col-manga.manga-item"
    override fun searchMangaSelector() = popularMangaSelector()
    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        element.select("a.manga-name").first()?.let {
            setUrlWithoutDomain(it.attr("href"))
            title = it.text()
        }
        thumbnail_url = element.select("img").first()?.attr("abs:src")
    }

    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)
    override fun popularMangaNextPageSelector() = ".pagination.next:not(.disabled)"
    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        title = document.select("h1.manga-title").text()
        author = document.select(".manga-info span:contains(Autor) + *").text()
        artist = document.select(".manga-info span:contains(Artista) + *").text()
        status = when (document.select(".manga-info span:contains(Estado) + *").text()) {
            "Publicándose" -> SManga.ONGOING
            "Finalizado" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        description = document.select(".manga-description").text()
        genre = document.select(".manga-genres a").joinToString { it.text() }
        thumbnail_url = document.select(".manga-cover img").attr("abs:src")
    }

    override fun chapterListSelector() = ".chapter-list.chapter-item"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        element.select("a").first()?.let {
            setUrlWithoutDomain(it.attr("href"))
            name = it.select(".chapter-title").text().ifEmpty { it.text() }
            date_upload = parseDate(it.select(".chapter-date").text())
        }
    }

    private fun parseDate(date: String): Long {
        return try {
            if (date.contains("/")) {
                val p = date.split("/")
                Calendar.getInstance().apply { set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt()) }.timeInMillis
            } else 0L
        } catch (e: Exception) { 0L }
    }

    override fun pageListParse(document: Document): List<Page> {
        return document.select("#chapter-images img,.chapter-images img").mapIndexed { i, img ->
            Page(i, imageUrl = img.attr("abs:data-src").ifEmpty { img.attr("abs:src") })
        }
    }
    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException()
}

class KuMangaFactory : eu.kanade.tachiyomi.source.SourceFactory {
    override fun createSources() = listOf(KuManga())
}
