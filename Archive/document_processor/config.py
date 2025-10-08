import os
from dotenv import load_dotenv

load_dotenv()

# Embedding Model (Ollama)
EMBEDDING_MODEL_NAME = "nomic-embed-text" 

# ChromaDB Configuration
CHROMA_DB_PATH = "chroma_db"
COLLECTION_NAME = "document_embeddings"

# LLM Configuration (Ollama)
LLM_MODEL = "llama3" 


# OCR Configuration 
TESSERACT_CMD_PATH = "/usr/bin/tesseract"