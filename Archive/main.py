# main.py

import os
from pathlib import Path
from typing import List, Dict, Any

from document_processor.factory import DocumentProcessorFactory
from embedding_manager import EmbeddingManager
from vector_db_manager import VectorDBManager
from llm_qa_manager import LLM_QAManager
from auth import AccessManager

PROCESSED_FILES_LOG = os.path.join("chroma_db", "processed_files.log")

class DocQASystem:
    def __init__(self):
        self.access_manager = AccessManager()
        self.embedding_manager = EmbeddingManager()
        self.vector_db_manager = VectorDBManager()
        self.llm_qa_manager = LLM_QAManager()

    def process_and_add_document(self, file_path: str, department: str, sensitivity: str):
        """
        Processes a single document, extracts text/images, embeds them,
        and adds to the vector database with associated metadata.
        """
        print(f"\nProcessing file: {file_path}")
        try:
            processor = DocumentProcessorFactory.get_processor(file_path)
            
            # 1. Extract Text
            text_content = processor.extract_text()
            
            # 2. Extract Images and OCR them
            ocr_text_from_images = []
            image_bytes_list = processor.extract_images()
            for i, img_bytes in enumerate(image_bytes_list):
                try:
                    # Save image to temp file for OCR, then delete
                    temp_img_path = Path(f"temp_img_{os.path.basename(file_path)}_{i}.png")
                    with open(temp_img_path, "wb") as f:
                        f.write(img_bytes)
                    
                    img_processor = DocumentProcessorFactory.get_processor(str(temp_img_path)) # Use ImageProcessor
                    ocr_text = img_processor.extract_text()
                    if ocr_text.strip():
                        ocr_text_from_images.append(f"Text from image in {processor.filename} (image {i+1}): {ocr_text}")
                    os.remove(temp_img_path)
                except Exception as e:
                    print(f"Warning: Could not OCR image {i+1} from {processor.filename}: {e}")

            full_document_content = text_content + "\n\n" + "\n".join(ocr_text_from_images)
            
            if not full_document_content.strip():
                print(f"Warning: No text content extracted from {file_path}. Skipping embedding.")
                return

            # Simple chunking (for real apps, use a more sophisticated chunker)
            chunks = [chunk.strip() for chunk in full_document_content.split('\n\n') if chunk.strip()]
            
            # If no good chunks, just use the whole content
            if not chunks:
                chunks = [full_document_content.strip()]
            
            # Ensure chunks are not too large or too small
            processed_chunks = []
            for chunk in chunks:
                if len(chunk) > 1000: # Arbitrary limit for demo
                    processed_chunks.append(" ".join(chunk.split()[:200]) + "...")
                elif len(chunk) < 50: # Arbitrary min for demo
                    continue # Skip very small chunks
                else:
                    processed_chunks.append(chunk)

            if not processed_chunks:
                print(f"No valid chunks to embed from {file_path}.")
                return

            # Generate embeddings for the processed chunks
            chunk_embeddings = self.embedding_manager.generate_embeddings(processed_chunks)

            # Ensure embeddings were generated successfully
            if not chunk_embeddings or not all(chunk_embeddings): # Check if any embedding failed
                print(f"Failed to generate embeddings for all chunks in {file_path}. Skipping add.")
                return

            metadatas = []
            for i, chunk in enumerate(processed_chunks):
                metadata = processor.get_metadata()
                metadata["department"] = department
                metadata["sensitivity"] = sensitivity
                metadata["chunk_id"] = i
                metadata["source"] = f"{processor.filename}_chunk_{i}"
                metadatas.append(metadata)

            # Add documents with pre-generated embeddings
            self.vector_db_manager.add_documents(processed_chunks, chunk_embeddings, metadatas)
            print(f"Successfully processed and added chunks from {file_path} to DB.")

            # Log the processed file
            with open(PROCESSED_FILES_LOG, "a") as f:
                f.write(f"{file_path}\n")

        except FileNotFoundError as e:
            print(f"Error: {e}")
        except ValueError as e:
            print(f"Error processing file type: {e}")
        except Exception as e:
            print(f"An unexpected error occurred while processing {file_path}: {e}")

    def ingest_data_from_folder(self, folder_path: str):
        """Ingests all supported documents from a given folder."""
        print(f"\n--- Ingesting data from: {folder_path} ---")
        for root, _, files in os.walk(folder_path):
            for file_name in files:
                file_path = os.path.join(root, file_name)
                try:
                    # Infer department/sensitivity based on filename for demo purposes
                    department = "General"
                    sensitivity = "Public"
                    
                    if "hr_" in file_name.lower():
                        department = "HR"
                        sensitivity = "Internal"
                    elif "accounting_" in file_name.lower() or "budget" in file_name.lower() or "financial" in file_name.lower():
                        department = "Accounting"
                        sensitivity = "Confidential"
                    elif "sales_" in file_name.lower():
                        department = "Sales"
                        sensitivity = "Internal"
                    elif "legal_" in file_name.lower():
                        department = "Legal"
                        sensitivity = "Confidential"
                    elif "sensitive" in file_name.lower():
                        sensitivity = "Highly Confidential"
                    
                    self.process_and_add_document(file_path, department, sensitivity)
                except ValueError as e:
                    print(f"Skipping {file_name}: {e}")
                except Exception as e:
                    print(f"Failed to ingest {file_name}: {e}")


    def query_system(self, username: str, query: str) -> str:
        """
        Handles a user query, applying role-based access control.
        """
        print(f"\n--- User '{username}' Query: '{query}' ---")
        
        # 1. Get allowed metadata filters for the user
        allowed_filters = self.access_manager.get_allowed_metadata_filters(username)
        print(f"User '{username}' allowed filters: {allowed_filters}")

        # 2. Convert allowed filters into ChromaDB's 'where' clause format
        chroma_filter = {}
        if "department" in allowed_filters:
            if len(allowed_filters["department"]) == 1:
                chroma_filter["department"] = allowed_filters["department"][0]
            elif len(allowed_filters["department"]) > 1:
                chroma_filter["department"] = {"$in": allowed_filters["department"]}

        if "sensitivity" in allowed_filters:
            if len(allowed_filters["sensitivity"]) == 1:
                chroma_filter["sensitivity"] = allowed_filters["sensitivity"][0]
            elif len(allowed_filters["sensitivity"]) > 1:
                chroma_filter["sensitivity"] = {"$in": allowed_filters["sensitivity"]}
        
        # Combine filters with an OR if both department and sensitivity are present
        final_filter = {}
        if "department" in chroma_filter and "sensitivity" in chroma_filter:
            final_filter["$or"] = [
                {"department": chroma_filter["department"]},
                {"sensitivity": chroma_filter["sensitivity"]}
            ]
        elif "department" in chroma_filter:
            final_filter = {"department": chroma_filter["department"]}
        elif "sensitivity" in chroma_filter:
            final_filter = {"sensitivity": chroma_filter["sensitivity"]}
        
        print(f"ChromaDB filter applied: {final_filter if final_filter else 'No filter'}")

        # Generate embedding for the user's query
        query_embedding = self.embedding_manager.generate_embedding(query)
        if not query_embedding:
            return "Failed to generate query embedding. Please try again."

        # 3. Query vector database with the user's query embedding and the applied filters
        retrieved_results = self.vector_db_manager.query_documents(
            query_embedding=query_embedding, # Pass the generated query embedding
            n_results=5, # Get top 5 relevant chunks
            where_filter=final_filter if final_filter else None
        )

        # 4. Extract relevant document chunks to form the context for the LLM
        context_chunks = [r['document'] for r in retrieved_results]
        
        # Optional: Print what context was retrieved
        print("\n--- Retrieved Context Chunks (based on roles): ---")
        if context_chunks:
            for i, chunk in enumerate(context_chunks):
                print(f"Chunk {i+1} (Source: {retrieved_results[i]['metadata'].get('source', 'N/A')}): {chunk[:100]}...")
        else:
            print("No relevant context found for this query under current user's permissions.")
            return "I couldn't find any relevant information that you are authorized to access."

        # 5. Generate LLM response using the authorized context
        response = self.llm_qa_manager.generate_response(query, context_chunks)
        return response

# if __name__ == "__main__":
#     system = DocQASystem()

#     # --- Setup Data Directory ---
#     data_dir = "data"
#     os.makedirs(data_dir, exist_ok=True)

#     # --- Create Dummy Files for Testing (ensure these are created before ingestion) ---
#     print("\n--- Creating dummy files for demonstration ---")
#     # This block remains the same as before.
#     # It includes creating a dummy HR document, Accounting spreadsheet, Sales PDF,
#     # General text file, Legal PDF (with simulated image text), and HR Org Chart image.
#     from reportlab.lib.pagesizes import letter
#     from reportlab.pdfgen import canvas
#     from docx import Document
#     from openpyxl import Workbook
#     from PIL import Image, ImageDraw, ImageFont

#     try:
#         # HR Document
#         doc = Document()
#         doc.add_heading('HR Employee Handbook Excerpt', level=1)
#         doc.add_paragraph('This section outlines the company\'s leave policy including sick leave, vacation, and parental leave. All employees are entitled to 20 days of vacation annually.')
#         doc.add_paragraph('Performance reviews are conducted annually by managers. Employee feedback is crucial for development.')
#         doc.save(os.path.join(data_dir, "hr_employee_handbook.docx"))
        
#         # Accounting Document
#         wb = Workbook()
#         ws = wb.active
#         ws.title = "Q4_2024_Financials"
#         ws['A1'] = 'Revenue Q4 2024'
#         ws['B1'] = 1500000
#         ws['A2'] = 'Expenses Q4 2024'
#         ws['B2'] = 800000
#         ws['A3'] = 'Net Profit'
#         ws['B3'] = '=B1-B2'
#         wb.save(os.path.join(data_dir, "accounting_q4_financials.xlsx"))

#         # Sales Document
#         c = canvas.Canvas(os.path.join(data_dir, "sales_strategy_2025.pdf"), pagesize=letter)
#         c.drawString(100, 750, "Sales Strategy 2025: Focus on APAC region expansion.")
#         c.drawString(100, 730, "Key initiatives include partner recruitment and targeted marketing campaigns. Customer segmentation analysis showed strong growth in tech sector.")
#         c.save()

#         # General/Public Document
#         with open(os.path.join(data_dir, "general_company_announcement.txt"), "w") as f:
#             f.write("Important announcement: The office will be closed on December 25th for the holiday season.")
#             f.write("\nAll employees are requested to check the internal portal for updates.")
        
#         # Sensitive Legal Document (PDF with simulated image)
#         c = canvas.Canvas(os.path.join(data_dir, "legal_nda_template.pdf"), pagesize=letter)
#         c.drawString(100, 750, "Non-Disclosure Agreement Template. This document contains highly sensitive legal clauses.")
#         c.drawString(100, 730, "No party shall disclose confidential information as defined herein.")
#         try:
#             # Simulate an image with OCRable text (requires dummy_pdf_image.png from pdf_processor.py)
#             # Create a simple dummy image if it doesn't exist
#             if not os.path.exists("data/dummy_pdf_image.png"):
#                 img_for_pdf = Image.new('RGB', (100, 50), color = 'blue')
#                 img_for_pdf.save("data/dummy_pdf_image.png")
#             c.drawImage("data/dummy_pdf_image.png", 200, 500, width=100, height=50)
#             c.drawString(200, 480, "Image says: Confidential Legal Clause.")
#         except Exception as e:
#             print(f"Could not add dummy image to legal PDF: {e}")
#         c.save()

#         # Image with Text
#         img_path = os.path.join(data_dir, "hr_org_chart.png")
#         img = Image.new('RGB', (400, 200), color = 'white')
#         d = ImageDraw.Draw(img)
#         try:
#             fnt = ImageFont.truetype("arial.ttf", 25)
#         except IOError:
#             fnt = ImageFont.load_default()
#         d.text((10,10), "Human Resources Department", fill=(0,0,0), font=fnt)
#         d.text((10,50), "Director: Jane Doe", fill=(0,0,0), font=fnt)
#         d.text((10,80), "Manager: John Smith", fill=(0,0,0), font=fnt)
#         img.save(img_path)

#         print("Dummy files created successfully.")
#     except Exception as e:
#         print(f"Error creating dummy files: {e}")
#         print("Please ensure `docx`, `openpyxl`, `reportlab`, `Pillow` are installed and Tesseract is configured.")
#         # Exit if we can't even create test data
#         exit()


#     # --- Reset DB before ingestion ---
#     system.vector_db_manager.reset_collection()

#     # --- Ingest Data ---
#     system.ingest_data_from_folder(data_dir)

#     # --- Perform Queries as Different Users ---

#     # Alice (HR Manager)
#     print("\n##### ALICE (HR Manager) QUERIES #####")
#     alice_query1 = "What is the company's leave policy?"
#     print(f"Alice's response: {system.query_system('alice', alice_query1)}")

#     alice_query2 = "Summarize the Q4 2024 financial report."
#     print(f"Alice's response: {system.query_system('alice', alice_query2)}") # Should NOT get full financial details

#     alice_query3 = "Who is the HR Director?"
#     print(f"Alice's response: {system.query_system('alice', alice_query3)}") # From OCR'd image

#     # Bob (Accounting Staff)
#     print("\n##### BOB (Accounting Staff) QUERIES #####")
#     bob_query1 = "What was the net profit in Q4 2024?"
#     print(f"Bob's response: {system.query_system('bob', bob_query1)}")

#     bob_query2 = "Tell me about the employee performance review process."
#     print(f"Bob's response: {system.query_system('bob', bob_query2)}") # Should NOT get HR details

#     # Charlie (Sales Rep)
#     print("\n##### CHARLIE (Sales Rep) QUERIES #####")
#     charlie_query1 = "What are the key sales initiatives for 2025?"
#     print(f"Charlie's response: {system.query_system('charlie', charlie_query1)}")
    
#     charlie_query2 = "What are the company's sick leave policies?"
#     print(f"Charlie's response: {system.query_system('charlie', charlie_query2)}") # Should NOT get HR details

#     # David (Executive)
#     print("\n##### DAVID (Executive) QUERIES #####")
#     david_query1 = "What are the key financial figures for Q4 2024 and HR's latest policy updates?"
#     print(f"David's response: {system.query_system('david', david_query1)}") # Should get details from both

#     david_query2 = "Summarize the legal NDA template."
#     print(f"David's response: {system.query_system('david', david_query2)}")

#     # Eve (General Employee)
#     print("\n##### EVE (General Employee) QUERIES #####")
#     eve_query1 = "Is the office closed on December 25th?"
#     print(f"Eve's response: {system.query_system('eve', eve_query1)}")

#     eve_query2 = "What are the Q4 2024 revenues?"
#     print(f"Eve's response: {system.query_system('eve', eve_query2)}") # Should NOT get confidential financial data

#     # Frank (HR Manager & Executive) - demonstrating multiple roles
#     print("\n##### FRANK (HR Manager & Executive) QUERIES #####")
#     frank_query1 = "What's our latest sales strategy and who is the HR Director?"
#     print(f"Frank's response: {system.query_system('frank', frank_query1)}") # Should get both

#     # --- Clean up dummy files ---
#     print("\n--- Cleaning up dummy files ---")
#     for file_name in os.listdir(data_dir):
#         file_path = os.path.join(data_dir, file_name)
#         if os.path.isfile(file_path):
#             os.remove(file_path)
#     print("Cleanup complete.")