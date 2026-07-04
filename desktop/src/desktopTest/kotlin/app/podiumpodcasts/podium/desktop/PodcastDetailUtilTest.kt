package app.podiumpodcasts.podium.desktop
import app.podiumpodcasts.podium.stripHtml
import app.podiumpodcasts.podium.formatDate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PodcastDetailUtilTest {

    // ── stripHtml ──

    @Test
    fun testStripHtmlRemovesParagraphTags() {
        val input = "<p>Hello world</p>"
        assertEquals("Hello world", stripHtml(input))
    }

    @Test
    fun testStripHtmlRemovesNestedTags() {
        val input = "Description with <a href=\"link\">a link</a> inside"
        assertEquals("Description with a link inside", stripHtml(input))
    }

    @Test
    fun testStripHtmlRemovesMultipleTags() {
        val input = "<p>Episode 1</p><ul><li>Topic A</li><li>Topic B</li></ul>"
        val result = stripHtml(input)
        assertTrue(result.contains("Episode 1"))
        assertTrue(result.contains("Topic A"))
        assertTrue(result.contains("Topic B"))
        assertTrue(!result.contains("<"))
    }

    @Test
    fun testStripHtmlRemovesBrTags() {
        val input = "Line1<br/>Line2<br />Line3"
        assertEquals("Line1 Line2 Line3", stripHtml(input))
    }

    @Test
    fun testStripHtmlHandlesEmptyString() {
        assertEquals("", stripHtml(""))
    }

    @Test
    fun testStripHtmlHandlesPlainText() {
        val input = "Just some plain text without any HTML"
        assertEquals(input, stripHtml(input))
    }

    @Test
    fun testStripHtmlCollapsesExtraWhitespace() {
        val input = "<p>  Hello   </p><p>  World  </p>"
        assertEquals("Hello World", stripHtml(input))
    }

    // ── formatDate ──

    @Test
    fun testFormatDateReturnsEmptyForZero() {
        assertEquals("", formatDate(0))
    }

    @Test
    fun testFormatDateReturnsEmptyForNegative() {
        assertEquals("", formatDate(-1))
    }

    @Test
    fun testFormatDateFormatsValidTimestamp() {
        // Use a timestamp and verify the format is yyyy-MM-dd (10 chars with dashes)
        val timestamp = 1767052800000L
        val result = formatDate(timestamp)
        assertEquals(10, result.length)
        assertEquals('-', result[4])
        assertEquals('-', result[7])
    }

    @Test
    fun testFormatDateFormatsKnownDate() {
        val timestamp = 1705276800000L
        val result = formatDate(timestamp)
        assertEquals(10, result.length)
        assertEquals('-', result[4])
        assertEquals('-', result[7])
    }

    @Test
    fun testFormatDateHandlesPositiveSmallValue() {
        assertTrue(formatDate(1).isNotEmpty())
    }
}
