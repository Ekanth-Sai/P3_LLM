# document_processor/base_processor.py

from abc import ABC, abstractmethod
from typing import List, Dict, Tuple
import os

class BaseDocumentProcessor(ABC):
    """Abstract base class for document processors."""

    def __init__(self, file_path: str):
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")
        self.file_path = file_path
        self.filename = os.path.basename(file_path)

    @abstractmethod
    def extract_text(self) -> str:
        """Extracts text content from the document."""
        pass

    @abstractmethod
    def extract_images(self) -> List[bytes]:
        """Extracts images from the document as byte streams."""
        pass

    def get_metadata(self) -> Dict[str, str]:
        """Returns basic metadata about the file."""
        return {
            "filename": self.filename,
            "file_path": self.file_path,
            "file_extension": os.path.splitext(self.filename)[1].lower()
        }