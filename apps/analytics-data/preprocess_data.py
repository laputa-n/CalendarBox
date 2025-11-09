import pandas as pd
import numpy as np
import os

# 1. 파일 경로 지정
schedule_path = 'export/schedule_expense.csv'
labels_path = 'labels/category_labels.csv'

# 2. 데이터 불러오기
df = pd.read_csv(schedule_path)
df_label = pd.read_csv(labels_path)

# 3. 라벨 병합
merged = df.merge(df_label, on='schedule_id', how='left')

# 4. 시간 기반 feature 생성
merged['start_at'] = pd.to_datetime(merged['start_at'])
merged['hour'] = merged['start_at'].dt.hour
merged['weekday'] = merged['start_at'].dt.dayofweek
merged['is_weekend'] = merged['weekday'].isin([5,6]).astype(int)

# 5. 수치형 feature 생성
merged['log_amount'] = np.log1p(merged['amount'])
merged['duration_bucket'] = pd.cut(
    merged['duration_min'],
    bins=[0, 30, 60, 120, 240, 1440],
    labels=['<=30min', '30~60', '60~120', '120~240', '>240']
)

# 6. 결측치 처리
merged['category'].fillna('미분류', inplace=True)

# 7. 저장
os.makedirs('processed', exist_ok=True)
merged.to_csv('processed/merged_labeled.csv', index=False, encoding='utf-8-sig')

print(f"✅ 라벨 병합 및 전처리 완료 → processed/merged_labeled.csv ({len(merged)} rows)")
