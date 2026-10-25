package com.tobiso.tobisoappnative.components

import com.tobiso.tobisoappnative.model.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFormatterTest {

    private fun parse(md: String, posts: List<Post> = emptyList()) =
        parseContentToElements(md, isOffline = false, posts = posts)

    private fun post(id: Int, path: String) = Post(id = id, title = "t$id", filePath = path)

    // ── Block elements ────────────────────────────────────────────────────

    @Test
    fun `blank content yields no elements`() {
        assertTrue(parse("").isEmpty())
        assertTrue(parse("   \n\n ").isEmpty())
    }

    @Test
    fun `null characters are stripped`() {
        val el = parse("Ahoj\u0000 svete").single() as ContentElement.Paragraph
        assertEquals("Ahoj svete", inlinePartsToText(el.parts))
    }

    @Test
    fun `headings keep their level`() {
        val els = parse("# Jedna\n## Dva\n### Tri")
        assertEquals(listOf(1, 2, 3), els.map { (it as ContentElement.Heading).level })
        assertEquals("Dva", inlinePartsToText((els[1] as ContentElement.Heading).parts))
    }

    @Test
    fun `plain line becomes a paragraph`() {
        val el = parse("Obycejny text").single() as ContentElement.Paragraph
        assertEquals(listOf<InlinePart>(InlinePart.Text("Obycejny text")), el.parts)
    }

    @Test
    fun `consecutive bullets are grouped into one list`() {
        val el = parse("- a\n- b\n- c").single() as ContentElement.BulletList
        assertEquals(1, el.level)
        assertEquals(listOf("a", "b", "c"), el.items.map { inlinePartsToText(it) })
    }

    @Test
    fun `indented bullets map to deeper levels`() {
        val els = parse("- a\n  - b\n    - c")
        assertEquals(listOf(1, 2, 3), els.map { (it as ContentElement.BulletList).level })
    }

    @Test
    fun `code block captures lines between fences`() {
        val el = parse("```\nx = 1\ny = 2\n```").single() as ContentElement.CodeBlock
        assertEquals("x = 1\ny = 2", el.code)
    }

    @Test
    fun `block quote joins consecutive quote lines`() {
        val el = parse("> prvni\n> druha").single() as ContentElement.BlockQuote
        assertEquals("prvni\ndruha", el.text)
    }

    @Test
    fun `table is parsed with header and padded rows`() {
        val el = parse("| A | B |\n|---|:-:|\n| 1 | 2 |\n| 3 |").single() as ContentElement.Table
        assertEquals(listOf("A", "B"), el.header)
        assertEquals(listOf(listOf("1", "2"), listOf("3", "")), el.rows)
    }

    @Test
    fun `pipe line without separator is not a table`() {
        val el = parse("a | b\nc | d")
        assertTrue(el.none { it is ContentElement.Table })
    }

    @Test
    fun `single line image`() {
        val el = parse("![obr](http://x/y.png)").single() as ContentElement.Image
        assertEquals("obr", el.alt)
        assertEquals("http://x/y.png", el.url)
    }

    @Test
    fun `image followed by caption and source becomes ImageWithMeta`() {
        val md = "![obr](http://x/y.png)\n\nPopisek obrazku\n\nZdroj: Wikipedia"
        val el = parse(md).single() as ContentElement.ImageWithMeta
        assertEquals("http://x/y.png", el.url)
        assertEquals("Popisek obrazku", el.caption)
        assertEquals("Zdroj: Wikipedia", el.source)
        assertEquals(false, el.isTobiso)
    }

    @Test
    fun `image source mentioning Tobiso is flagged`() {
        val md = "![o](u.png)\n\nAutor: Tobiso"
        val el = parse(md).single() as ContentElement.ImageWithMeta
        assertEquals(true, el.isTobiso)
        assertEquals(null, el.caption)
    }

    @Test
    fun `video tag is extracted and its body skipped`() {
        val md = "<video src=\"http://v/a.mp4\" controls>\nfallback\n</video>\nDal"
        val els = parse(md)
        assertEquals(ContentElement.VideoPlayer("http://v/a.mp4", null), els[0])
        assertEquals("Dal", inlinePartsToText((els[1] as ContentElement.Paragraph).parts))
    }

    @Test
    fun `addendum reference on its own line`() {
        assertEquals(ContentElement.AddendumReference(7), parse("(--DOD-7--)").single())
    }

    @Test
    fun `intra text is taken from between the first two ellipses`() {
        val els = parse("Uvod ... shrnuti ... telo")
        assertEquals(ContentElement.Intra("shrnuti"), els[0])
    }

    @Test
    fun `standalone link resolves post id from file path`() {
        val posts = listOf(post(42, "/matematika/zlomky.md"))
        val el = parse("[Zlomky](ml-matematika/zlomky.html)", posts).single() as ContentElement.ClickableLink
        assertEquals(42, el.postId)
        assertEquals("Zlomky", el.text)
    }

    @Test
    fun `standalone link to unknown post has null id`() {
        val el = parse("[X](neco.html)").single() as ContentElement.ClickableLink
        assertEquals(null, el.postId)
    }

    @Test
    fun `numbered lines are grouped into one numbered list`() {
        val el = parse("1. prvni\n2. druha\n10. desata").single() as ContentElement.NumberedList
        assertEquals(listOf("prvni", "druha", "desata"), el.items)
    }

    @Test
    fun `number without dot-space is still a paragraph`() {
        assertTrue(parse("2024 byl rok").single() is ContentElement.Paragraph)
    }

    // ── Inline parsing ────────────────────────────────────────────────────

    @Test
    fun `bold italic and bold-italic`() {
        assertEquals(listOf<InlinePart>(InlinePart.Bold("a")), parseInlineParts("**a**"))
        assertEquals(listOf<InlinePart>(InlinePart.Italic("a")), parseInlineParts("*a*"))
        assertEquals(listOf<InlinePart>(InlinePart.BoldItalic("a")), parseInlineParts("***a***"))
    }

    @Test
    fun `unclosed markers fall back to literal text`() {
        assertEquals("**abc", inlinePartsToText(parseInlineParts("**abc")))
        assertEquals("*abc", inlinePartsToText(parseInlineParts("*abc")))
    }

    @Test
    fun `mixed inline text keeps order`() {
        val parts = parseInlineParts("a **b** c")
        assertEquals(listOf("a ", "b", " c"), parts.map { inlinePartsToText(listOf(it)) })
        assertTrue(parts[1] is InlinePart.Bold)
    }

    @Test
    fun `inline addendum`() {
        val parts = parseInlineParts("text (--DOD-3--) dal")
        assertTrue(parts.contains(InlinePart.Addendum(3)))
    }

    @Test
    fun `invalid addendum id stays text`() {
        val parts = parseInlineParts("(--DOD-x--)")
        assertTrue(parts.none { it is InlinePart.Addendum })
    }

    @Test
    fun `inline link resolves post`() {
        val posts = listOf(post(5, "/cesky/slovesa.md"))
        val link = parseInlineParts("viz [slovesa](cesky/slovesa.html)", posts)
            .filterIsInstance<InlinePart.Link>().single()
        assertEquals("slovesa", link.text)
        assertEquals(5, link.postId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Test
    fun `extractToc returns headings with element indexes`() {
        val els = parse("Uvod\n# A\ntext\n## B")
        val toc = extractToc(els)
        assertEquals(listOf(TocEntry("A", 1, 1), TocEntry("B", 2, 3)), toc)
    }

    @Test
    fun `plain words with slash are not fractions`() {
        val parts = parseInlineParts("proč/kde")
        assertTrue(parts.none { it is InlinePart.Fraction })
        assertEquals("proč/kde", inlinePartsToText(parts))
    }

    @Test
    fun `numeric slash is a fraction`() {
        val parts = parseInlineParts("1/2")
        assertTrue(parts.single() is InlinePart.Fraction)
    }

    @Test
    fun `explicit word fraction via braces`() {
        val parts = parseInlineParts("{proč}/{kde}")
        assertTrue(parts.single() is InlinePart.Fraction)
    }

    @Test
    fun `inlinePartsToText renders fraction`() {
        val f = InlinePart.Fraction(listOf(InlinePart.Text("1")), listOf(InlinePart.Text("2")))
        assertEquals("1/2", inlinePartsToText(listOf(f)))
    }

    @Test
    fun `person mentions are injected into paragraphs`() {
        val parts = injectPersonMentions(listOf(InlinePart.Text("Karel IV. vladl")), listOf("Karel IV."))
        assertTrue(parts.any { it is InlinePart.PersonMention && it.name == "Karel IV." })
    }

    @Test
    fun `sub and sup tags become styled parts`() {
        val parts = parseInlineParts("A'<sub>R</sub> a x<sup>2</sup>")
        assertTrue(parts.contains(InlinePart.Subscript("R")))
        assertTrue(parts.contains(InlinePart.Superscript("2")))
        assertEquals("A'R a x2", inlinePartsToText(parts))
    }

    @Test
    fun `unclosed sub tag stays literal`() {
        assertEquals("<sub>x", inlinePartsToText(parseInlineParts("<sub>x")))
    }
}
