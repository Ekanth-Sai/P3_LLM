import chromadb
from chromadb.utils import embedding_functions # Still useful for a quick-start, but we'll remove explicit use
from typing import List, Dict, Any, Optional
import uuid

from config import CHROMA_DB_PATH, COLLECTION_NAME # EMBEDDING_MODEL_NAME is not directly used by ChromaDB here

class VectorDBManager:
    def __init__(self):
        self.client = chromadb.PersistentClient(path=CHROMA_DB_PATH)
        self.collections_cache = {}
        print(f"ChromaDB initialized at: {CHROMA_DB_PATH}")
        # self.collection = self.client.get_or_create_collection(
        #     name=COLLECTION_NAME,
        #     # embedding_function=self.embedding_function # REMOVED: Embeddings provided directly
        # )
        # print(f"ChromaDB initialized at: {CHROMA_DB_PATH}")
        # print(f"Collection '{COLLECTION_NAME}' ready.")

    # def get_or_create_project_collection(self, project_name: str):
    #     if project_name not in self.collections:
    #         collection_name = f"project_{project_name.lower().replace(' ', '_')}"
    #         self.collections[project_name] = self.client.get_or_create_collection(name = collection_name)

    #     return self.collections[project_name]

    def get_or_create_department_collection(self, department: str):
        if department is None:
            department = 'General'
        dept_lower = department.lower().replace(" ", "_")
        collection_name = f"dept_{dept_lower}"

        if collection_name not in self.collections_cache:
            self.collections_cache[collection_name] = self.client.get_or_create_collection(name = collection_name)

            print(f"Collection '{collection_name}' ready")
        
        return self.collections_cache[collection_name]

    def add_documents(self, documents: List[str], embeddings: List[List[float]], metadatas: List[Dict[str, Any]], department: str, project_name: str):
        if not (len(documents) == len(embeddings) == len(metadatas)):
            raise ValueError("Lengths of documents, embeddings, and metadatas must match.")
        
        collection = self.get_or_create_department_collection(department)

        for metadata in metadatas:
            metadata["project"] = project_name
            metadata["department"] = department

            if "sensitivity" not in metadata:
                metadata["sensitivity"] = "Internal"

        ids = [str(uuid.uuid4()) for _ in documents] 

        # try:
        #     self.collection.add(
        #         documents=documents,
        #         embeddings=embeddings, 
        #         metadatas=metadatas,
        #         ids=ids
        #     )
        #     print(f"Added {len(documents)} document chunks to ChromaDB.")
        # except Exception as e:
        #     print(f"Error adding documents to ChromaDB: {e}")

        collection.add(documents=documents, embeddings=embeddings, metadatas=metadatas, ids=ids)
        print(f"Added {len(documents)} chunks to {department}/{project_name} knowledge base")

    def query_documents(self, query_embedding: List[float], department: str, allowed_projects: List[str], allowed_sensitivity: List[str], n_results: int = 5) -> List[Dict]:
        collection = self.get_or_create_department_collection(department)

        where_filter = {
            "$and": [
                {"project": {"$in": allowed_projects}},
                {"sensitivity": {"$in": allowed_sensitivity}}
            ]
        }

        print(f"Querying {department} collection")
        print(f"Projects: {allowed_projects}")
        print(f"Sensitivity: {allowed_sensitivity}")

        try:
            results = collection.query(
            query_embeddings=[query_embedding], 
            n_results=n_results,
            where=where_filter,
            include=['documents', 'metadatas', 'distances']
            )   

        # print(f"Querying ChromaDB with filter: {where_filter}")
        
            processed_results = []
            if results and results.get('documents') and len(results['documents'][0]) > 0:
                for i in range(len(results['documents'][0])):
                    processed_results.append({
                        "document": results['documents'][0][i],
                        "metadata": results['metadatas'][0][i],
                        "distance": results['distances'][0][i]
                    })
                print(f"Found {len(processed_results)} relevant chunks")
            else:
                print("No results found with current filters")
            return processed_results
        except Exception as e:
            print("Query error: {e}")
            return []
    
    def list_collections(self) -> List[str]:
        collections = self.client.list_collections()
        return [c.name for c in collections]

    def delete_document_by_filename(self, filename: str, department: str = None) -> int:
        deleted_count = 0

        if department:
            collections_to_check = [self.get_or_create_department_collection(department)]
        else:
            all_collections = self.list_collections()
            collections_to_check = [self.client.get_collection(name) for name in all_collections]
        
        for collection in collections_to_check:
            try:
                results = collection.get(where = {"filename": filename})
                
                if results and results.get("ids"):
                    collection.delete(ids = results["ids"])
                    deleted_count += len(results["ids"])
            
            except Exception as e:
                print(f"Error deleting from collections: {e}")
        
        return deleted_count
    

    # def reset_collection(self):
    #     try:
    #         self.client.delete_collection(name=COLLECTION_NAME)
    #         self.collection = self.client.get_or_create_collection(
    #             name=COLLECTION_NAME,
    #             # embedding_function=self.embedding_function # REMOVED
    #         )
    #         print(f"Collection '{COLLECTION_NAME}' reset successfully.")
    #     except Exception as e:
    #         print(f"Error resetting collection: {e}")

