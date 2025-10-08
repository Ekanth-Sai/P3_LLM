# document_processor/image_processor.py

import io
from PIL import Image
import pytesseract
from typing import List
from config import TESSERACT_CMD_PATH

# Set Tesseract command path (important for Windows)
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD_PATH

from .base_processor import BaseDocumentProcessor

class ImageProcessor(BaseDocumentProcessor):
    """Processes image files (PNG, JPG, etc.) using OCR."""

    def extract_text(self) -> str:
        try:
            image = Image.open(self.file_path)
            text = pytesseract.image_to_string(image)
            return text
        except Exception as e:
            print(f"Error performing OCR on image {self.filename}: {e}")
            return ""

    def extract_images(self) -> List[bytes]:
        # For a standalone image, the image itself is the "extracted image"
        try:
            with open(self.file_path, 'rb') as f:
                return [f.read()]
        except Exception as e:
            print(f"Error reading image bytes for {self.filename}: {e}")
            return []