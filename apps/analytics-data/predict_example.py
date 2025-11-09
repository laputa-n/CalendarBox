import joblib
import pandas as pd
from scipy.sparse import hstack

# 모델 불러오기
bundle = joblib.load('model/calendar_classifier.pkl')
model = bundle['model']
vectorizer = bundle['vectorizer']
scaler = bundle['scaler']

# 테스트용 새 일정 데이터
new_data = pd.DataFrame([{
    'title': '주말 공부 모임',
    'place_name': '카페 루프탑',
    'hour': 14,
    'duration_min': 120,
    'amount': 10000
}])

# 전처리
X_text = vectorizer.transform(new_data['title'] + ' ' + new_data['place_name'])
X_num = scaler.transform(new_data[['hour', 'duration_min', 'amount']])
X = hstack([X_text, X_num])

# 예측
pred = model.predict(X)
print(f"예측된 카테고리: {pred[0]}")
