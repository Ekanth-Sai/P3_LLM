# llm_qa_manager.py

import ollama
from typing import List, Dict, Any
# from config import LLM_MODEL, OPENAI_API_KEY # OPENAI_API_KEY no longer needed
from config import LLM_MODEL

class LLM_QAManager:
    """Manages interaction with the Large Language Model for Q&A using Ollama."""

    def __init__(self):
        # No API key or explicit client initialization needed for Ollama client
        # It assumes Ollama server is running on localhost:11434 by default
        print(f"Ollama LLM QA Manager initialized for model: {LLM_MODEL}")

    def generate_response(self, query: str, context_chunks: List[str]) -> str:
        """
        Generates an LLM response based on the query and provided context chunks using Ollama.
        """
        if not context_chunks:
            return "I couldn't find any relevant information based on your query and my available knowledge."

        # Construct a detailed prompt for the LLM
        context_str = "\n\n".join([f"Document Chunk {i+1}:\n{chunk}" for i, chunk in enumerate(context_chunks)])

        # Using Ollama's chat endpoint, which is more robust for conversational turns
        # We simulate a system message by pre-pending it to the user's content
        # Or you can pass it directly if the model supports a 'system' role
        # Llama 3 generally supports a 'system' role well.

        messages = [
            {"role": "system", "content": (
                "You are a helpful assistant specialized in providing answers based *only* on the "
                "context provided below. If the answer is not in the context, state that you "
                "cannot find the information in the provided context. Do not invent information."
            )},
            {"role": "user", "content": f"{context_str}\n\nUser Query: {query}"}
        ]

        try:
            response = ollama.chat(
                model=LLM_MODEL,
                messages=messages,
                options={
                    "temperature": 0.7,
                    # Add other Ollama options here if desired, e.g., "top_p", "num_predict"
                }
            )
            return response['message']['content'].strip()
        except ollama.ResponseError as e:
            print(f"Ollama API Error: {e}")
            return "An error occurred while trying to generate a response from the AI (Ollama API Error)."
        except Exception as e:
            print(f"An unexpected error occurred: {e}")
            return "An unexpected error occurred."

# Example Usage (requires a running vector DB and embeddings to be added first)
if __name__ == "__main__":
    from vector_db_manager import VectorDBManager
    from embedding_manager import EmbeddingManager
    
    # Initialize DB and Embedding Manager (assuming it has data from vector_db_manager.py's example)
    db_manager = VectorDBManager()
    embed_manager = EmbeddingManager()
    
    llm_manager = LLM_QAManager()

    print("--- Testing LLM with a query and context from HR data ---")
    hr_filter = {"department": "HR"}
    
    query_text_hr = "What are the main points of the employee performance review guidelines?"
    query_embedding_hr = embed_manager.generate_embedding(query_text_hr)

    if query_embedding_hr:
        hr_results = db_manager.query_documents(query_embedding=query_embedding_hr, n_results=2, where_filter=hr_filter)
        hr_contexts = [r['document'] for r in hr_results]
        
        if hr_contexts:
            hr_response = llm_manager.generate_response(
                query=query_text_hr,
                context_chunks=hr_contexts
            )
            print(f"\nHR Query Response:\n{hr_response}")
        else:
            print("\nNo HR context found for LLM test.")
    else:
        print(f"Failed to generate query embedding for '{query_text_hr}'")


    print("\n--- Testing LLM with a query and context from Accounting data ---")
    acc_filter = {"department": "Accounting"}
    
    query_text_acc = "Can you summarize the Q1 2024 financial report's revenue figures?"
    query_embedding_acc = embed_manager.generate_embedding(query_text_acc)

    if query_embedding_acc:
        acc_results = db_manager.query_documents(query_embedding=query_embedding_acc, n_results=2, where_filter=acc_filter)
        acc_contexts = [r['document'] for r in acc_results]

        if acc_contexts:
            acc_response = llm_manager.generate_response(
                query=query_text_acc,
                context_chunks=acc_contexts
            )
            print(f"\nAccounting Query Response:\n{acc_response}")
        else:
            print("\nNo Accounting context found for LLM test.")
    else:
        print(f"Failed to generate query embedding for '{query_text_acc}'")

    print("\n--- Testing LLM with no relevant context ---")
    query_text_no_context = "Explain the principles of quantum entanglement."
    query_embedding_no_context = embed_manager.generate_embedding(query_text_no_context)
    
    if query_embedding_no_context:
        # Query for something that wouldn't typically be in the dummy data
        no_context_results = db_manager.query_documents(query_embedding=query_embedding_no_context, n_results=2)
        no_context_chunks = [r['document'] for r in no_context_results] # These will be irrelevant
        
        no_context_response = llm_manager.generate_response(
            query=query_text_no_context,
            context_chunks=no_context_chunks # Even if some are returned, they won't be about quantum physics
        )
        print(f"\nNo Relevant Context Response:\n{no_context_response}")
    else:
        print(f"Failed to generate query embedding for '{query_text_no_context}'")