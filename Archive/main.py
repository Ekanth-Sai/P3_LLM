import os
from pathlib import Path
from typing import List, Dict, Any
import psycopg2
from psycopg2.extras import RealDictCursor

from document_processor.factory import DocumentProcessorFactory
from embedding_manager import EmbeddingManager
from vector_db_manager import VectorDBManager
from llm_qa_manager import LLM_QAManager

PROCESSED_FILES_LOG = os.path.join("chroma_db", "processed_files.log")

DB_CONFIG = {
    'host': 'localhost',
    'database': 'p3_db',
    'user': 'test',
    'password': 'admin',
    'port': 5432
}

def fetch_user_from_db(username: str) -> Dict[str, Any]:
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        
        query = """
            SELECT email, department, project, role, allowed_projects, allowed_sensitivity FROM users WHERE email = %s AND status = 'ACTIVE'
        """
        cursor.execute(query, (username,))
        user = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        if user:
            return dict(user)
        else:
            print(f"User {username} not found or not active")
            return None
            
    except Exception as e:
        print(f"DB Error: {e}")
        return None

class DocQASystem:
    def __init__(self):
        self.embedding_manager = EmbeddingManager()
        self.vector_db_manager = VectorDBManager()
        self.llm_qa_manager = LLM_QAManager()

    def process_and_add_document(self, file_path: str, department: str, sensitivity: str, project_name: str, allowed_roles: List[str] = None):
        print(f"\nProcessing: {file_path}")
        print(f"Dept: {department} | Project: {project_name} | Sensitivity: {sensitivity}")
        
        try:
            processor = DocumentProcessorFactory.get_processor(file_path)
    
            text_content = processor.extract_text()
            
            ocr_text_from_images = []
            image_bytes_list = processor.extract_images()
            for i, img_bytes in enumerate(image_bytes_list):
                try:
                    temp_img_path = Path(f"temp_img_{os.path.basename(file_path)}_{i}.png")
                    with open(temp_img_path, "wb") as f:
                        f.write(img_bytes)
                    
                    img_processor = DocumentProcessorFactory.get_processor(str(temp_img_path))
                    ocr_text = img_processor.extract_text()
                    if ocr_text.strip():
                        ocr_text_from_images.append(f"Image {i+1}: {ocr_text}")
                    os.remove(temp_img_path)
                except Exception as e:
                    print(f"OCR failed for image {i+1}: {e}")

            full_document_content = text_content + "\n\n" + "\n".join(ocr_text_from_images)
            
            if not full_document_content.strip():
                print("No text extracted")
                return

            chunks = [c.strip() for c in full_document_content.split('\n\n') if c.strip()]
            if not chunks:
                chunks = [full_document_content.strip()]
            
            processed_chunks = []
            for chunk in chunks:
                if 50 <= len(chunk) <= 1000:
                    processed_chunks.append(chunk)
                elif len(chunk) > 1000:
                    processed_chunks.append(" ".join(chunk.split()[:200]) + "...")

            if not processed_chunks:
                print("No valid chunks")
                return

            chunk_embeddings = self.embedding_manager.generate_embeddings(processed_chunks)

            if not chunk_embeddings or not all(chunk_embeddings):
                print("Embedding generation failed")
                return

            metadatas = []
            for i, chunk in enumerate(processed_chunks):
                metadata = processor.get_metadata()
                metadata["department"] = department
                metadata["sensitivity"] = sensitivity
                metadata["project"] = project_name
                metadata["chunk_id"] = i
                metadata["source"] = f"{processor.filename}_chunk_{i}"
                # metadatas.append(metadata)
                
                if allowed_roles:
                    metadata["allowed_roles"] = allowed_roles
                else:
                    metadata["allowed_roles"] = ["CEO"]
                
                metadatas.append(metadata)

            self.vector_db_manager.add_documents(processed_chunks, chunk_embeddings, metadatas, department=department, project_name=project_name)

            with open(PROCESSED_FILES_LOG, "a") as f:
                f.write(f"{file_path}\n")

            print(f"Successfully processed {file_path}")

        except Exception as e:
            print(f"Error processing {file_path}: {e}")

    def query_system(self, username: str, query: str, filters: Dict = None) -> str:
        print(f"\nQuery from {username}: {query}")
        
        user = fetch_user_from_db(username)
        
        user_roles = user.get('inherited_roles', [user.get('role', 'USER')])
        
        if not user:
            return "User not found or unauthorized"

        if filters:
            department = filters.get('department', user.get('department', 'General'))
            allowed_projects = filters.get('projects', [user.get('project', 'General')])
            allowed_sensitivity = filters.get('sensitivity', ['Public', 'Internal'])
        else:
            department = user.get('department', 'General')
            allowed_projects = user.get('allowed_projects', [user.get('project', 'General')])
            allowed_sensitivity = user.get('allowed_sensitivity', ['Public', 'Internal'])

        print(f"Dept: {department}")
        print(f"Projects: {allowed_projects}")
        print(f"Sensitivity: {allowed_sensitivity}")

        query_embedding = self.embedding_manager.generate_embedding(query)
        if not query_embedding:
            return "Failed to generate query embedding"

        retrieved_results = self.vector_db_manager.query_documents(
            query_embedding=query_embedding,
            department=department,
            allowed_projects=allowed_projects,
            allowed_sensitivity=allowed_sensitivity,
            allowed_roles = user_roles,
            n_results=5
        )

        if not retrieved_results:
            return "No relevant information found with your access permissions."

        context_chunks = [r['document'] for r in retrieved_results]
        
        print(f"\nRetrieved {len(context_chunks)} chunks:")
        for i, r in enumerate(retrieved_results):
            print(f"[{i+1}] {r['metadata'].get('source', 'N/A')} (score: {r['distance']:.3f})")

        response = self.llm_qa_manager.generate_response(query, context_chunks)
        return response