# vector_db_manager.py

import chromadb
from chromadb.utils import embedding_functions # Still useful for a quick-start, but we'll remove explicit use
from typing import List, Dict, Any, Optional
import uuid

from config import CHROMA_DB_PATH, COLLECTION_NAME # EMBEDDING_MODEL_NAME is not directly used by ChromaDB here

class VectorDBManager:
    """Manages interaction with the ChromaDB vector database."""

    def __init__(self):
        self.client = chromadb.PersistentClient(path=CHROMA_DB_PATH)
        # We will NOT use an explicit embedding_function for the collection
        # because embeddings are generated externally by EmbeddingManager
        self.collection = self.client.get_or_create_collection(
            name=COLLECTION_NAME,
            # embedding_function=self.embedding_function # REMOVED: Embeddings provided directly
        )
        print(f"ChromaDB initialized at: {CHROMA_DB_PATH}")
        print(f"Collection '{COLLECTION_NAME}' ready.")

    def add_documents(self, documents: List[str], embeddings: List[List[float]], metadatas: List[Dict[str, Any]]):
        """
        Adds documents (text chunks), their pre-generated embeddings, and metadata to the collection.
        """
        if not (len(documents) == len(embeddings) == len(metadatas)):
            raise ValueError("Lengths of documents, embeddings, and metadatas must match.")
        
        ids = [str(uuid.uuid4()) for _ in documents] # Generate unique IDs for each chunk

        try:
            self.collection.add(
                documents=documents,
                embeddings=embeddings, # Pass pre-generated embeddings
                metadatas=metadatas,
                ids=ids
            )
            print(f"Added {len(documents)} document chunks to ChromaDB.")
        except Exception as e:
            print(f"Error adding documents to ChromaDB: {e}")

    def query_documents(self, 
                        query_embedding: List[float], 
                        n_results: int = 5, 
                        where_filter: Optional[Dict[str, Any]] = None
                       ) -> List[Dict[str, Any]]:
        """
        Queries the vector database with a query embedding and an optional metadata filter.
        
        Args:
            query_embedding: The pre-generated embedding vector for the query.
            n_results: The number of top results to return.
            where_filter: A dictionary representing the metadata filter.
                          Example: {"department": {"$in": ["HR", "General"]}}
                                   {"sensitivity": "Confidential"}
        Returns:
            A list of dictionaries, where each dict contains 'document', 'metadata', 'distance'.
        """
        print(f"Querying ChromaDB with filter: {where_filter}")
        results = self.collection.query(
            query_embeddings=[query_embedding], # Pass query embedding directly
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

    def reset_collection(self):
        """Deletes and recreates the collection, effectively clearing it."""
        try:
            self.client.delete_collection(name=COLLECTION_NAME)
            self.collection = self.client.get_or_create_collection(
                name=COLLECTION_NAME,
                # embedding_function=self.embedding_function # REMOVED
            )
            print(f"Collection '{COLLECTION_NAME}' reset successfully.")
        except Exception as e:
            print(f"Error resetting collection: {e}")