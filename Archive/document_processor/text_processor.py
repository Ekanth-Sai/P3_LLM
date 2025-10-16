# document_processor/text_processor.py

from typing import List

from .base_processor import BaseDocumentProcessor

class TextProcessor(BaseDocumentProcessor):
    """Processes .txt files."""

    def extract_text(self) -> str:
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception as e:
            print(f"Error reading text file {self.filename}: {e}")
            return ""

    def extract_images(self) -> List[bytes]:
        # Text files do not contain images
        return []
