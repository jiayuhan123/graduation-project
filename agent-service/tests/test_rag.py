from app.rag import TextChunker


def test_chunker_keeps_content_and_applies_overlap() -> None:
    content = "第一段" * 180 + "\n\n" + "第二段" * 180
    chunks = TextChunker(chunk_chars=300, overlap_chars=40).split(content)

    assert len(chunks) >= 3
    assert all(20 <= len(chunk) <= 500 for chunk in chunks)
    assert "第一段" in chunks[0]
