import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report
from scipy.sparse import hstack
import joblib
import os

# 데이터 로드
df = pd.read_csv('processed/merged_labeled.csv')

# 입력(X) / 정답(y)
text_features = df['title'].fillna('') + ' ' + df['place_name'].fillna('')
numeric_features = df[['hour', 'duration_min', 'amount']].fillna(0)
y = df['category']

# TF-IDF 벡터화
vectorizer = TfidfVectorizer(max_features=100)
X_text = vectorizer.fit_transform(text_features)

# 수치형 피처 스케일링
scaler = StandardScaler()
X_num = scaler.fit_transform(numeric_features)

# 합치기
X = hstack([X_text, X_num])

# 학습/검증 분리
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 모델 학습
model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X_train, y_train)

# 평가
y_pred = model.predict(X_test)
print(classification_report(y_test, y_pred))

# 모델 저장
os.makedirs('model', exist_ok=True)
joblib.dump({'model': model, 'vectorizer': vectorizer, 'scaler': scaler}, 'model/calendar_classifier.pkl')

print('✅ 모델 학습 완료 → model/calendar_classifier.pkl')
