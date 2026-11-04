// 입력값 · 화면 표시 형식 (여러 화면이 같이 쓴다)

// 연락처 형식: 서버 검사(SignupRequestDTO)와 같은 규칙
export const PHONE_PATTERN = /^01[016789]-\d{3,4}-\d{4}$/

// 연락처 입력: 숫자만 남기고 하이픈을 넣는다 (01012345678 → 010-1234-5678, 10자리는 011-123-4567)
export const formatPhone = (value) => {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length < 4) return digits
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  if (digits.length < 11) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}
