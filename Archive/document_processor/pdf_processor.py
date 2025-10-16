# document_processor/pdf_processor.py

import fitz  # PyMuPDF
import io
from PIL import Image
import pytesseract
from typing import List
from config import TESSERACT_CMD_PATH

# Set Tesseract command path
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD_PATH

from .base_processor import BaseDocumentProcessor

class PdfProcessor(BaseDocumentProcessor):
    """Processes .pdf files using PyMuPDF and falls back to OCR if needed."""

    def extract_text(self) -> str:
        full_text = []
        try:
            doc = fitz.open(self.file_path)
            for page_num in range(len(doc)):
                page = doc.load_page(page_num)
                
                # First, try to extract text directly
                text = page.get_text()
                
                # If no text is found, perform OCR on the page image
                if not text.strip():
                    try:
                        # Render page to an image (pixmap)
                        pix = page.get_pixmap(dpi=300) # Higher DPI for better OCR
                        img_bytes = pix.tobytes("png")
                        img = Image.open(io.BytesIO(img_bytes))
                        
                        # Perform OCR
                        ocr_text = pytesseract.image_to_string(img)
                        if ocr_text.strip():
                            full_text.append(f"--- OCR Text from Page {page_num + 1} ---\n{ocr_text}")
                    except Exception as ocr_err:
                        print(f"Could not perform OCR on page {page_num + 1} of {self.filename}: {ocr_err}")
                else:
                    full_text.append(text)
            doc.close()
        except Exception as e:
            print(f"Error processing PDF {self.filename} with PyMuPDF: {e}")
        return "\n".join(full_text)

    def extract_images(self) -> List[bytes]:
        image_bytes_list = []
        try:
            doc = fitz.open(self.file_path)
            for page_num in range(len(doc)):
                image_list = doc.get_page_images(page_num)
                for img_info in image_list:
                    xref = img_info[0]
                    base_image = doc.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_bytes_list.append(image_bytes)
            doc.close()
        except Exception as e:
            print(f"Error extracting images from PDF {self.filename} with PyMuPDF: {e}")
        return image_bytes_list