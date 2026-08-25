from pathlib import Path

from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn


DOCX_PATH = Path(r"E:\graduation-project\primary-math-platform-feature-plan-updated.docx")


def set_table_indent(table, dxa: int = 120) -> None:
    table_properties = table._tbl.tblPr
    indent = table_properties.find(qn("w:tblInd"))
    if indent is None:
        indent = OxmlElement("w:tblInd")
        table_properties.append(indent)
    indent.set(qn("w:w"), str(dxa))
    indent.set(qn("w:type"), "dxa")


def mark_first_row_as_header(table) -> None:
    row_properties = table.rows[0]._tr.get_or_add_trPr()
    header = row_properties.find(qn("w:tblHeader"))
    if header is None:
        header = OxmlElement("w:tblHeader")
        row_properties.append(header)
    header.set(qn("w:val"), "true")


def main() -> None:
    document = Document(DOCX_PATH)
    for table in document.tables:
        set_table_indent(table)
        mark_first_row_as_header(table)
    document.save(DOCX_PATH)
    print(DOCX_PATH)


if __name__ == "__main__":
    main()
