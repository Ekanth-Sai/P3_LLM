import os
from dotenv import load_dotenv

load_dotenv()

# Embedding Model (Ollama)
EMBEDDING_MODEL_NAME = "nomic-embed-text" # Changed to Ollama embedding model

# ChromaDB Configuration
CHROMA_DB_PATH = "chroma_db"
COLLECTION_NAME = "document_embeddings"

# LLM Configuration (Ollama)
LLM_MODEL = "llama3" # Changed to Llama 3 for Ollama
# No API key needed for Ollama as it runs locally via its client library.
# OPENAI_API_KEY = os.getenv("OPENAI_API_KEY") # No longer needed for Ollama

# OCR Configuration (ensure Tesseract is installed and in your PATH)
TESSERACT_CMD_PATH = "/usr/bin/tesseract"