from app.rag import build_chinese_splitter


def test_chunker_keeps_content_and_applies_overlap() -> None:
    content = "第一段" * 180 + "\n\n" + "第二段" * 180
    chunks = build_chinese_splitter(chunk_size=300, chunk_overlap=40).split_text(content)

    assert len(chunks) >= 3
    assert all(20 <= len(chunk) <= 500 for chunk in chunks)
    assert "第一段" in chunks[0]
