from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()

model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")


class EmbedRequest(BaseModel):
    texts: list[str]


class EmbedResponse(BaseModel):
    vectors: list[list[float]]


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    # texts 리스트를 임베딩해서 2차원 리스트로 반환
    embeddings = model.encode(
        req.texts,
        normalize_embeddings=True  # 코사인 유사도용 정규화
    ).tolist()

    return EmbedResponse(vectors=embeddings)
