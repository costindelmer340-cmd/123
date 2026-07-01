import chromadb
from chromadb.config import Settings
import os


class VectorStore:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def initialize(self):
        if self._initialized:
            return
        
        persist_dir = os.path.join(os.path.dirname(__file__), "..", "..", "chromadb_data")
        os.makedirs(persist_dir, exist_ok=True)
        
        self.client = chromadb.PersistentClient(
            path=persist_dir,
            settings=Settings(
                anonymized_telemetry=False,
                is_persistent=True
            )
        )
        
        self.collection = self.client.get_or_create_collection(name="knowledge_base")
        self._initialized = True
    
    def add_documents(self, documents: list[dict]):
        if not self._initialized:
            self.initialize()
        
        if not documents:
            return
        
        ids = [str(doc["id"]) for doc in documents]
        contents = [doc["content"] for doc in documents]
        metadatas = [doc.get("metadata", {}) for doc in documents]
        
        self.collection.upsert(
            ids=ids,
            documents=contents,
            metadatas=metadatas
        )
    
    def search(self, query: str, top_k: int = 3, filter: dict = None) -> list[dict]:
        if not self._initialized:
            self.initialize()
        
        try:
            results = self.collection.query(
                query_texts=[query],
                n_results=top_k,
                where=filter
            )
            
            docs = []
            for i in range(len(results["ids"][0])):
                docs.append({
                    "id": results["ids"][0][i],
                    "content": results["documents"][0][i],
                    "score": results["distances"][0][i],
                    "metadata": results["metadatas"][0][i] if results["metadatas"][0][i] else {}
                })
            
            return docs
        except Exception as e:
            print(f"Vector search failed: {e}")
            return []
    
    def get_collection_stats(self) -> dict:
        if not self._initialized:
            self.initialize()
        
        try:
            count = self.collection.count()
            return {"document_count": count}
        except Exception as e:
            return {"document_count": 0, "error": str(e)}
    
    def delete_documents(self, ids: list[str]):
        if not self._initialized:
            self.initialize()
        
        self.collection.delete(ids=ids)


vector_store = VectorStore()