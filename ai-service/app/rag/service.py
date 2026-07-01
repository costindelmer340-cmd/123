from .vector_store import vector_store
from .embedding import embedding_service


class RAGService:
    def __init__(self):
        self.vector_store = vector_store
        self.embedding_service = embedding_service
    
    def initialize(self):
        self.vector_store.initialize()
    
    def add_knowledge(self, knowledge_items: list[dict]):
        documents = []
        for item in knowledge_items:
            content = item.get("content", "")
            if not content:
                continue
            
            documents.append({
                "id": str(item["id"]),
                "content": content,
                "metadata": {
                    "type": item.get("type", "article"),
                    "category": item.get("category", ""),
                    "title": item.get("title", ""),
                    "merchant_id": item.get("merchant_id", ""),
                    "created_at": item.get("created_at", "")
                }
            })
        
        if documents:
            self.vector_store.add_documents(documents)
    
    def search_knowledge(self, query: str, top_k: int = 3, merchant_id: str = None) -> list[dict]:
        filter_dict = {}
        if merchant_id:
            filter_dict["merchant_id"] = merchant_id
        
        results = self.vector_store.search(query, top_k=top_k, filter=filter_dict)
        
        return results
    
    def get_knowledge_context(self, query: str, top_k: int = 3, merchant_id: str = None) -> str:
        results = self.search_knowledge(query, top_k=top_k, merchant_id=merchant_id)
        
        if not results:
            return ""
        
        context_parts = []
        for idx, result in enumerate(results):
            score = 1 - result.get("score", 0)
            if score < 0.3:
                continue
            
            content = result.get("content", "")[:200]
            title = result.get("metadata", {}).get("title", "")
            
            if title:
                context_parts.append(f"知识{idx+1}【{title}】：{content}")
            else:
                context_parts.append(f"知识{idx+1}：{content}")
        
        return "\n\n".join(context_parts)
    
    def get_stats(self) -> dict:
        return self.vector_store.get_collection_stats()


rag_service = RAGService()