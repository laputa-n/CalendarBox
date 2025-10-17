// SignupCompletePage.js
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { LoadingSpinner } from "../common/LoadingSpinner";
import { ApiService } from "../../services/apiService";

export const SignupCompletePage = () => {
  const navigate = useNavigate();
  const { completeSignup, loading } = useAuth();

  const [formData, setFormData] = useState({
    name: "",
    phoneNumber: "",
    nickname: "",
  });

  const [errors, setErrors] = useState({});
  const [email, setEmail] = useState("");
useEffect(() => {
  const fetchNext = async () => {
    try {
       const data = await ApiService.getNextAction();
      console.log("[SignupCompletePage] /auth/kakao/next 응답:", data);
      setEmail(data.email || "(카카오 이메일)");
    } catch (err) {
      console.error("이메일 불러오기 실패:", err);
      setEmail("(카카오 이메일)");
    }
  };
  fetchNext();
}, []);
  const validateForm = () => {
    const newErrors = {};
    if (!formData.name.trim()) newErrors.name = "이름을 입력해주세요.";
    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = "전화번호를 입력해주세요.";
    } else if (!/^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$/.test(formData.phoneNumber)) {
      newErrors.phoneNumber = "올바른 전화번호 형식이 아닙니다.";
    }
    if (!formData.nickname.trim()) {
      newErrors.nickname = "닉네임을 입력해주세요.";
    } else if (formData.nickname.length < 2 || formData.nickname.length > 20) {
      newErrors.nickname = "닉네임은 2-20자 사이여야 합니다.";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));
  };

  const handleSubmit = async (e) => {
  e.preventDefault();
  if (!validateForm()) return;

  try {
    // 👉 AuthContext에서 가져온 completeSignup 호출
    await completeSignup({ ...formData, email });

    // 회원가입 성공 시 대시보드 이동
    navigate("/dashboard", { replace: true });
  } catch (error) {
    console.error("회원가입 완료 실패:", error);
    setErrors({
      submit: error.message || "회원가입에 실패했습니다. 다시 시도해주세요.",
    });
  }
};

  if (loading) {
    return (
      <div style={styles.container}>
        <LoadingSpinner size="3rem" text="처리 중..." />
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>신규회원 정보 입력</h1>
        <p style={styles.subtitle}>정보를 입력해주세요</p>

        <div style={styles.emailInfo}>
          <label style={styles.label}>이메일</label>
          <div style={styles.emailDisplay}>{email ? email : "이메일을 불러오는 중..."}
</div>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>
          {/* 이름 */}
          <div style={styles.formGroup}>
            <label htmlFor="name" style={styles.label}>
              이름 <span style={styles.required}>*</span>
            </label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              style={{ ...styles.input, ...(errors.name ? styles.inputError : {}) }}
              placeholder="홍길동"
            />
            {errors.name && <span style={styles.errorText}>{errors.name}</span>}
          </div>

          {/* 전화번호 */}
          <div style={styles.formGroup}>
            <label htmlFor="phoneNumber" style={styles.label}>
              전화번호 <span style={styles.required}>*</span>
            </label>
            <input
              type="tel"
              id="phoneNumber"
              name="phoneNumber"
              value={formData.phoneNumber}
              onChange={handleChange}
              style={{ ...styles.input, ...(errors.phoneNumber ? styles.inputError : {}) }}
              placeholder="010-1234-5678"
            />
            {errors.phoneNumber && <span style={styles.errorText}>{errors.phoneNumber}</span>}
          </div>

          {/* 닉네임 */}
          <div style={styles.formGroup}>
            <label htmlFor="nickname" style={styles.label}>
              닉네임 <span style={styles.required}>*</span>
            </label>
            <input
              type="text"
              id="nickname"
              name="nickname"
              value={formData.nickname}
              onChange={handleChange}
              style={{ ...styles.input, ...(errors.nickname ? styles.inputError : {}) }}
              placeholder="닉네임 (2-20자)"
            />
            {errors.nickname && <span style={styles.errorText}>{errors.nickname}</span>}
          </div>

          {/* 제출 에러 */}
          {errors.submit && <div style={styles.submitError}>{errors.submit}</div>}

          <button type="submit" style={styles.submitButton} disabled={loading}>
            {loading ? "처리 중..." : "회원가입 완료"}
          </button>
        </form>
      </div>
    </div>
  );
};

const styles = {
  container: { minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", backgroundColor: "#f9fafb", padding: "2rem" },
  card: { backgroundColor: "white", borderRadius: "12px", boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)", padding: "3rem", width: "100%", maxWidth: "500px" },
  title: { fontSize: "2rem", fontWeight: "bold", color: "#1f2937", marginBottom: "0.5rem", textAlign: "center" },
  subtitle: { fontSize: "1rem", color: "#6b7280", marginBottom: "2rem", textAlign: "center" },
  emailInfo: { marginBottom: "1.5rem", padding: "1rem", backgroundColor: "#f3f4f6", borderRadius: "8px" },
  emailDisplay: { fontSize: "1rem", color: "#374151", fontWeight: "500" },
  form: { display: "flex", flexDirection: "column", gap: "1.5rem" },
  formGroup: { display: "flex", flexDirection: "column", gap: "0.5rem" },
  label: { fontSize: "0.875rem", fontWeight: "600", color: "#374151" },
  required: { color: "#ef4444" },
  input: { padding: "0.75rem", fontSize: "1rem", border: "1px solid #d1d5db", borderRadius: "8px", outline: "none", transition: "border-color 0.2s" },
  inputError: { borderColor: "#ef4444" },
  errorText: { fontSize: "0.875rem", color: "#ef4444", marginTop: "0.25rem" },
  submitError: { padding: "0.75rem", backgroundColor: "#fee2e2", borderRadius: "8px", color: "#dc2626", fontSize: "0.875rem", textAlign: "center" },
  submitButton: { padding: "0.875rem", fontSize: "1rem", fontWeight: "600", color: "white", backgroundColor: "#3b82f6", border: "none", borderRadius: "8px", cursor: "pointer", transition: "background-color 0.2s", marginTop: "1rem" },
};
