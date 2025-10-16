# document_processor/factory.py

import os
from typing import Type, Dict

from .base_processor import BaseDocumentProcessor
from .docx_processor import DocxProcessor
from .xlsx_processor import XlsxProcessor
from .pdf_processor import PdfProcessor
from .image_processor import ImageProcessor
from .text_processor import TextProcessor

class DocumentProcessorFactory:
    """Factory to get the correct document processor based on file extension."""

    PROCESSORS: Dict[str, Type[BaseDocumentProcessor]] = {
        ".docx": DocxProcessor,
        ".xlsx": XlsxProcessor,
        ".pdf": PdfProcessor,
        ".png": ImageProcessor,
        ".jpg": ImageProcessor,
        ".jpeg": ImageProcessor,
        ".gif": ImageProcessor,
        ".bmp": ImageProcessor,
        ".tiff": ImageProcessor,
        ".tif": ImageProcessor,
        ".txt": TextProcessor,
    }

    @staticmethod
    def get_processor(file_path: str) -> BaseDocumentProcessor:
        """
        Returns an instance of the appropriate document processor for the given file.
        Raises ValueError if no suitable processor is found.
        """
        _, ext = os.path.splitext(file_path)
        processor_class = DocumentProcessorFactory.PROCESSORS.get(ext.lower())
        if not processor_class:
            raise ValueError(f"No processor found for file extension: {ext}")
        return processor_class(file_path)

# # Example Usage
# if __name__ == "__main__":
#     try:
#         docx_proc = DocumentProcessorFactory.get_processor("data/example.docx")
#         print(f"Got processor for docx: {type(docx_proc).__name__}")
#     except ValueError as e:
#         print(e)
    
#     try:
#         xlsx_proc = DocumentProcessorFactory.get_processor("data/example.xlsx")
#         print(f"Got processor for xlsx: {type(xlsx_proc).__name__}")
#     except ValueError as e:
#         print(e)

#     try:
#         pdf_proc = DocumentProcessorFactory.get_processor("data/example.pdf")
#         print(f"Got processor for pdf: {type(pdf_proc).__name__}")
#     except ValueError as e:
#         print(e)
    
#     try:
#         image_proc = DocumentProcessorFactory.get_processor("data/example.png")
#         print(f"Got processor for png: {type(image_proc).__name__}")
#     except ValueError as e:
#         print(e)

#     try:
#         unknown_proc = DocumentProcessorFactory.get_processor("data/example.txt")
#     except ValueError as e:
#         print(f"Correctly handled unknown file type: {e}")