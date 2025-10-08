# embedding_manager.py

import ollama
from typing import List, Dict, Any
import numpy as np

from config import EMBEDDING_MODEL_NAME

class EmbeddingManager:
    """Handles generating embeddings for text chunks using Ollama."""

    def __init__(self):
        # No model loading needed here, the ollama client handles it.
        # Ensure your Ollama server is running and the model is pulled.
        print(f"Ollama Embedding Manager initialized for model: {EMBEDDING_MODEL_NAME}")

    def generate_embedding(self, text: str) -> List[float]:
        """Generates an embedding vector for the given text using Ollama."""
        
        response = ollama.embed(model=EMBEDDING_MODEL_NAME, input=text)
        #print (response)
        return response['embeddings']
        # except Exception as e:
        #     print(f"Error generating embedding with Ollama for text: '{text[:50]}...' Error: {e}")
        #     return [] # Return empty list on error

    def generate_embeddings(self, texts: List[str]) -> List[List[float]]:
        """Generates embeddings for a list of texts using Ollama."""
        embeddings_list = []
        for text in texts:
            embeddings_list.append(self.generate_embedding(text))
        return embeddings_list