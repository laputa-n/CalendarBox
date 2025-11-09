from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
from scipy.sparse import hstack

# FastAPI 앱 생성
app = FastAPI(title="CalendarBox AI Model Server")

# 모델 로드
bundle = joblib.load("model/calendar_classifier.pkl")
model = bundle["model"]
vectorizer = bundle["vectorizer"]
scaler = bundle["scaler"]

# 요청 데이터 모델 정의
class Schedule(BaseModel):
    schedule_id: int
    title: str
    place_name: str
    hour: int
    duration_min: float
    amount: float

# 예측 엔드포인트
@app.post("/predict")
def predict(data: list[Schedule]):
    df = pd.DataFrame([d.dict() for d in data])

    # 텍스트 피처 (title + place_name)
    X_text = vectorizer.transform(df["title"] + " " + df["place_name"])

    # 수치형 피처 (hour, duration, amount)
    X_num = scaler.transform(df[["hour", "duration_min", "amount"]])

    # 결합
    X = hstack([X_text, X_num])

    # 예측
    preds = model.predict(X)

    # schedule_id → category 매핑
    return {int(df.iloc[i]["schedule_id"]): preds[i] for i in range(len(preds))}
