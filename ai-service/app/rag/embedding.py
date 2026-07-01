from sentence_transformers import SentenceTransformer
import numpy as np


class EmbeddingService:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._model = None
        return cls._instance
    
    def load_model(self):
        if self._model is not None:
            return
        
        try:
            self._model = SentenceTransformer('shibing624/text2vec-base-chinese')
        except Exception:
            self._model = SentenceTransformer('all-MiniLM-L6-v2')
    
    def embed_text(self, text: str) -> list[float]:
        if self._model is None:
            self.load_model()
        
        try:
            embedding = self._model.encode(text, convert_to_numpy=True)
            return embedding.tolist()
        except Exception as e:
            print(f"Embedding failed: {e}")
            return []
    
    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        if self._model is None:
            self.load_model()
        
        try:
            embeddings = self._model.encode(texts, convert_to_numpy=True)
            return [emb.tolist() for emb in embeddings]
        except Exception as e:
            print(f"Batch embedding failed: {e}")
            return [[] for _ in texts]


embedding_service = EmbeddingService()