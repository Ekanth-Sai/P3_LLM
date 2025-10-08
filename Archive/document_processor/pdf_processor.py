# document_processor/pdf_processor.py

import PyPDF2
# For better image extraction, consider pymupdf (fitz) or pdfplumber
# import fitz # pip install PyMuPDF

import io
from PIL import Image
import pytesseract
from typing import List
from config import TESSERACT_CMD_PATH

# Set Tesseract command path (important for Windows)
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD_PATH

from .base_processor import BaseDocumentProcessor

class PdfProcessor(BaseDocumentProcessor):
    """Processes .pdf files."""

    def extract_text(self) -> str:
        full_text = []
        try:
            with open(self.file_path, 'rb') as f:
                reader = PyPDF2.PdfReader(f)
                for page in reader.pages:
                    text = page.extract_text()
                    if text:
                        full_text.append(text)
        except Exception as e:
            print(f"Error extracting text from PDF {self.filename}: {e}")
        return "\n".join(full_text)

    def extract_images(self) -> List[bytes]:
        image_bytes_list = []
        try:
            with open(self.file_path, 'rb') as f:
                reader = PyPDF2.PdfReader(f)
                for page_num in range(len(reader.pages)):
                    page = reader.pages[page_num]
                    if '/XObject' in page['/Resources']:
                        xObjects = page['/Resources']['/XObject']
                        for obj in xObjects:
                            if xObjects[obj]['/Subtype'] == '/Image':
                                size = xObjects[obj]['/Width'], xObjects[obj]['/Height']
                                data = xObjects[obj].get_object().get_data()
                                mode = ""
                                if '/ColorSpace' in xObjects[obj] and xObjects[obj]['/ColorSpace'] == '/DeviceRGB':
                                    mode = "RGB"
                                elif '/ColorSpace' in xObjects[obj] and xObjects[obj]['/ColorSpace'] == '/DeviceCMYK':
                                    mode = "CMYK"
                                else:
                                    # Fallback for other color spaces, often grayscale or indexed
                                    mode = "L" # Grayscale
                                    
                                if '/Filter' in xObjects[obj]:
                                    filter_type = xObjects[obj]['/Filter']
                                    if filter_type == '/FlateDecode':
                                        # Data is compressed
                                        try:
                                            img_data = io.BytesIO(data)
                                            # PyPDF2 internal decompression:
                                            # img_data = io.BytesIO(xObjects[obj]._data) 
                                            # This is tricky with PyPDF2, often better to use PIL or external tools
                                            # For simplicity, we'll assume most PDFs images can be handled by PIL
                                            # directly if not heavily filtered/encoded in exotic ways.
                                            # Or use pymupdf for more robust handling.
                                            
                                            # A common hack for simple FlateDecode images that PIL can handle
                                            # is to just try opening them.
                                            img = Image.open(img_data)
                                            with io.BytesIO() as output:
                                                img.save(output, format="PNG") # Save as PNG for consistency
                                                image_bytes_list.append(output.getvalue())
                                        except Exception as img_err:
                                            print(f"Could not decompress image from PDF: {img_err}")
                                            # Fallback: if PIL fails, try to just append the raw data
                                            # image_bytes_list.append(data)
                                    else:
                                        print(f"Unsupported image filter type: {filter_type}")
                                        image_bytes_list.append(data) # Append raw data, might not be viewable directly
                                else:
                                    if mode:
                                        try:
                                            img = Image.frombytes(mode, size, data)
                                            with io.BytesIO() as output:
                                                img.save(output, format="PNG")
                                                image_bytes_list.append(output.getvalue())
                                        except Exception as img_err:
                                            print(f"Could not create image from raw data: {img_err}")
                                            image_bytes_list.append(data) # Append raw data
                                    else:
                                        image_bytes_list.append(data) # Append raw data
        except Exception as e:
            print(f"Error extracting images from PDF {self.filename}: {e}")
        return image_bytes_list