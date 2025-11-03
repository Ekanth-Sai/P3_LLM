import chromadb
from chromadb.utils import embedding_functions # Still useful for a quick-start, but we'll remove explicit use
from typing import List, Dict, Any, Optional
import uuid

from config import CHROMA_DB_PATH, COLLECTION_NAME # EMBEDDING_MODEL_NAME is not directly used by ChromaDB here

class VectorDBManager:
    def __init__(self):
        self.client = chromadb.PersistentClient(path=CHROMA_DB_PATH)
        self.collections = {}
        # self.collection = self.client.get_or_create_collection(
        #     name=COLLECTION_NAME,
        #     # embedding_function=self.embedding_function # REMOVED: Embeddings provided directly
        # )
        # print(f"ChromaDB initialized at: {CHROMA_DB_PATH}")
        # print(f"Collection '{COLLECTION_NAME}' ready.")

    def get_or_create_project_collection(self, project_name: str):
        if project_name not in self.collections:
            collection_name = f"project_{project_name.lower().replace(' ', '_')}"
            self.collections[project_name] = self.client.get_or_create_collection(name = collection_name)

        return self.collections[project_name]

    def add_documents(self, documents: List[str], embeddings: List[List[float]], metadatas: List[Dict[str, Any]], project_name: str):
        # if not (len(documents) == len(embeddings) == len(metadatas)):
        #     raise ValueError("Lengths of documents, embeddings, and metadatas must match.")
        
        collection = self.get_or_create_project_collection(project_name)

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
        print(f"Added {len(documents)} chunks to {project_name} knowledge base")

    def query_documents(self, query_embedding: List[float], project_name: str, n_results: int = 5, where_filter: Optional[Dict] = None):
        collection = self.get_or_create_project_collection(project_name)
        # print(f"Querying ChromaDB with filter: {where_filter}")
        results = collection.query(
            query_embeddings=[query_embedding], 
            n_results=n_results,
            where=where_filter,
            include=['documents', 'metadatas', 'distances']
        )
        
        processed_results = []
        if results and results.get('documents'):
            for i in range(len(results['documents'][0])):
                processed_results.append({
                    "document": results['documents'][0][i],
                    "metadata": results['metadatas'][0][i],
                    "distance": results['distances'][0][i]
                })
        return processed_results

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

