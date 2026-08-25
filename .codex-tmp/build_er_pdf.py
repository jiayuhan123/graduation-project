from pathlib import Path

from PIL import Image
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader


ROOT = Path(r"E:\graduation-project")
SOURCE_DIR = ROOT / "er-diagrams"
OUTPUT_DIR = ROOT / "output" / "pdf"
OUTPUT_FILE = OUTPUT_DIR / "primary-math-platform-conceptual-er-diagrams.pdf"

IMAGE_FILES = [
    SOURCE_DIR / "01-user-school-organization-chen.png",
    SOURCE_DIR / "02-course-knowledge-chen.png",
    SOURCE_DIR / "03-question-bank-rules-chen.png",
    SOURCE_DIR / "04-practice-grading-chen.png",
    SOURCE_DIR / "05-diagnosis-mastery-recommendation-chen.png",
    SOURCE_DIR / "06-assignment-report-chen.png",
    SOURCE_DIR / "07-gamification-chen.png",
    SOURCE_DIR / "08-pk-battle-chen.png",
    SOURCE_DIR / "09-open-platform-audit-chen.png",
]


def main() -> None:
    missing = [str(path) for path in IMAGE_FILES if not path.exists()]
    if missing:
        raise FileNotFoundError("Missing ER diagram images:\n" + "\n".join(missing))

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    page_width = A4[0]
    margin = 18.0
    content_width = page_width - 2 * margin

    pdf = canvas.Canvas(str(OUTPUT_FILE), pagesize=A4, pageCompression=1)
    pdf.setTitle("小学数学智能学习与游戏平台概念实体联系图")
    pdf.setAuthor("毕业设计")
    pdf.setSubject("概念实体联系图 1-9")

    for image_path in IMAGE_FILES:
        with Image.open(image_path) as image:
            pixel_width, pixel_height = image.size

        content_height = content_width * pixel_height / pixel_width
        page_height = content_height + 2 * margin
        pdf.setPageSize((page_width, page_height))
        pdf.setFillColorRGB(1, 1, 1)
        pdf.rect(0, 0, page_width, page_height, fill=1, stroke=0)
        pdf.drawImage(
            ImageReader(str(image_path)),
            margin,
            margin,
            width=content_width,
            height=content_height,
            preserveAspectRatio=True,
            anchor="c",
            mask="auto",
        )
        pdf.showPage()

    pdf.save()
    print(OUTPUT_FILE)


if __name__ == "__main__":
    main()
