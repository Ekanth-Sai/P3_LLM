import chromadb
from typing import List, Dict, Any, Optional
import uuid

from config import CHROMA_DB_PATH

class VectorDBManager:
    def __init__(self):
        self.client = chromadb.PersistentClient(path=CHROMA_DB_PATH)
        self.collections_cache = {}
        print(f"ChromaDB initialized at: {CHROMA_DB_PATH}")

    def get_or_create_department_collection(self, department: str):
        if department is None:
            department = 'General'
        dept_lower = department.lower().replace(" ", "_")
        collection_name = f"dept_{dept_lower}"

        if collection_name not in self.collections_cache:
            self.collections_cache[collection_name] = self.client.get_or_create_collection(name=collection_name)
            print(f"Collection '{collection_name}' ready")
        
        return self.collections_cache[collection_name]

    def add_documents(self, documents: List[str], embeddings: List[List[float]], 
                     metadatas: List[Dict[str, Any]], department: str, project_name: str):
        if not (len(documents) == len(embeddings) == len(metadatas)):
            raise ValueError("Lengths of documents, embeddings, and metadatas must match.")
        
        collection = self.get_or_create_department_collection(department)

        for metadata in metadatas:
            metadata["project"] = project_name
            metadata["department"] = department

            if "sensitivity" not in metadata:
                metadata["sensitivity"] = "Internal"
            
            # Ensure allowed_roles is a string (ChromaDB requirement)
            if "allowed_roles" in metadata:
                if isinstance(metadata["allowed_roles"], list):
                    metadata["allowed_roles"] = ",".join(metadata["allowed_roles"])
                elif metadata["allowed_roles"] is None:
                    metadata["allowed_roles"] = "CEO"

        ids = [str(uuid.uuid4()) for _ in documents] 

        collection.add(
            documents=documents, 
            embeddings=embeddings, 
            metadatas=metadatas, 
            ids=ids
        )
        print(f"✅ Added {len(documents)} chunks to {department}/{project_name} knowledge base")

    def query_documents(self, query_embedding: List[float], department: str, 
                       allowed_projects: List[str], allowed_sensitivity: List[str],
                       user_roles: List[str], n_results: int = 5) -> List[Dict]:
        collection = self.get_or_create_department_collection(department)

        # Build base filter for department, project, and sensitivity
        where_filter = {
            "$and": [
                {"project": {"$in": allowed_projects}},
                {"sensitivity": {"$in": allowed_sensitivity}}
            ]
        }

        print(f"Querying {department} collection")
        print(f"Projects: {allowed_projects}")
        print(f"Sensitivity: {allowed_sensitivity}")
        print(f"User Roles: {user_roles}")

        try:
            # Query without role filter first (we'll filter in Python)
            results = collection.query(
                query_embeddings=[query_embedding], 
                n_results=n_results * 3,  # Get more results to filter
                where=where_filter,
                include=['documents', 'metadatas', 'distances']
            )

            processed_results = []
            if results and results.get('documents') and len(results['documents'][0]) > 0:
                for i in range(len(results['documents'][0])):
                    metadata = results['metadatas'][0][i]
                    
                    # Check role access
                    # allowed_roles in metadata is a comma-separated string
                    doc_allowed_roles = metadata.get('allowed_roles', 'CEO')
                    if isinstance(doc_allowed_roles, str):
                        doc_roles_list = [r.strip() for r in doc_allowed_roles.split(',')]
                    else:
                        doc_roles_list = ['CEO']
                    
                    # Check if user has any of the required roles
                    has_access = any(user_role in doc_roles_list for user_role in user_roles)
                    
                    if has_access:
                        processed_results.append({
                            "document": results['documents'][0][i],
                            "metadata": metadata,
                            "distance": results['distances'][0][i]
                        })
                    
                    # Stop if we have enough results
                    if len(processed_results) >= n_results:
                        break
                
                print(f"✅ Found {len(processed_results)} accessible chunks (filtered from {len(results['documents'][0])} total)")
            else:
                print("⚠️ No results found with current filters")
            
            return processed_results
            
        except Exception as e:
            print(f"❌ Query error: {e}")
            import traceback
            traceback.print_exc()
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
                results = collection.get(where={"filename": filename})
                
                if results and results.get("ids"):
                    collection.delete(ids=results["ids"])
                    deleted_count += len(results["ids"])
            
            except Exception as e:
                print(f"Error deleting from collection: {e}")
        
        return deleted_count