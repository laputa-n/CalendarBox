import os
import pandas as pd
from sqlalchemy import create_engine
from dotenv import load_dotenv

# .env 불러오기
load_dotenv()
db_url = os.getenv("DB_URL")

if not db_url:
    raise RuntimeError("❌ DB_URL not found. .env 파일을 확인하세요.")

engine = create_engine(db_url)

print("⏳ PostgreSQL에서 데이터 추출 중...")

query = """
SELECT s.schedule_id, s.title, s.start_at, s.end_at,
       p.title AS place_name, e.amount, e.paid_at
FROM schedule s
LEFT JOIN schedule_place sp ON sp.schedule_id = s.schedule_id
LEFT JOIN place p ON p.place_id = sp.place_id
LEFT JOIN expense e ON e.schedule_id = s.schedule_id
WHERE s.created_by = 1;
"""

df = pd.read_sql(query, engine)
print(f"📦 {len(df)} rows loaded from database.")

# 전처리
df["duration_min"] = (df["end_at"] - df["start_at"]).dt.total_seconds() / 60
df["place_name"].fillna("미정", inplace=True)
df["amount"].fillna(0, inplace=True)

# 저장
os.makedirs("export", exist_ok=True)
output_path = "export/schedule_expense.csv"
df.to_csv(output_path, index=False, encoding="utf-8-sig")

print(f"✅ Export 완료 → {output_path}")
