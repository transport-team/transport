import { useEffect, useState } from 'react'
import { getMyDriverProfile, updateMyDriverProfile } from '../../api/driverProfileApi'
import DriverReviewModal from '../review/DriverReviewModal'

const LICENSE_TYPES = ['1종 대형', '1종 보통', '1종 특수', '2종 보통']
const INPUT_CLASS = 'w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-600 focus:outline-none'
const STATUS_LABEL = { ACTIVE: '승인됨', PENDING: '승인 대기', INACTIVE: '중지' }

const toForm = (profile) => ({
  licenseType: profile.licenseType || '',
  careerYears: String(profile.careerYears ?? 0),
  certifications: profile.certifications || '',
})

const DriverProfileSection = () => {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [editing, setEditing] = useState(false)
  const [showReviews, setShowReviews] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    getMyDriverProfile()
      .then((data) => {
        if (!active) return
        setProfile(data)
        setForm(toForm(data))
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message ?? '기사 정보를 불러오지 못했습니다.')
      })
    return () => { active = false }
  }, [])

  const startEditing = () => {
    setForm(toForm(profile))
    setError('')
    setNotice('')
    setEditing(true)
  }

  const cancelEditing = () => {
    setEditing(false)
    setError('')
    setForm(toForm(profile))
  }

  const save = async (event) => {
    event.preventDefault()
    if (saving || !profile) return
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const data = await updateMyDriverProfile({
        licenseType: form.licenseType,
        careerYears: Number(form.careerYears),
        certifications: form.certifications.trim(),
      })
      setProfile(data)
      setForm(toForm(data))
      setEditing(false)
      setNotice('기사 정보가 저장되었습니다.')
    } catch (err) {
      setError(err.response?.data?.message ?? '기사 정보를 저장하지 못했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-semibold text-gray-900">기사 정보</h2>
          <p className="mt-1 text-xs text-gray-500">면허, 경력, 보유 자격증명을 확인하고 수정할 수 있어요.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => setShowReviews(true)} disabled={!profile}
            className="rounded-lg border border-blue-600 px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-50 disabled:opacity-50">
            ★ 내 리뷰 보기
          </button>
          {profile && !editing && (
            <button type="button" onClick={startEditing}
              className="rounded-lg bg-blue-700 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-800">
              정보 수정
            </button>
          )}
        </div>
      </div>
      {!profile && !error && <p className="mt-4 text-gray-500">기사 정보를 불러오는 중...</p>}
      {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-red-700">{error}</p>}
      {notice && <p role="status" className="mt-3 rounded-lg bg-emerald-50 p-3 text-emerald-800">{notice}</p>}
      {profile && !editing && (
        <dl className="mt-4 grid grid-cols-[auto_minmax(0,1fr)] gap-x-5 gap-y-3 border-t border-gray-100 pt-4">
          <dt className="text-gray-500">면허 종류</dt><dd className="font-medium text-gray-900">{profile.licenseType || '-'}</dd>
          <dt className="text-gray-500">경력</dt><dd className="font-medium text-gray-900">{profile.careerYears}년</dd>
          <dt className="text-gray-500">보유 자격증</dt><dd className="whitespace-pre-wrap break-words font-medium text-gray-900">{profile.certifications || '미등록'}</dd>
          <dt className="text-gray-500">기사 상태</dt><dd className="font-medium text-gray-900">{STATUS_LABEL[profile.status] || profile.status}</dd>
        </dl>
      )}
      {profile && editing && form && (
        <form onSubmit={save} className="mt-4 grid gap-4 border-t border-gray-100 pt-4">
          <label className="grid gap-1 font-medium text-gray-700">
            면허 종류
            <select required className={INPUT_CLASS} value={form.licenseType}
              onChange={(event) => setForm({ ...form, licenseType: event.target.value })}>
              <option value="">면허를 선택해 주세요</option>
              {!LICENSE_TYPES.includes(form.licenseType) && form.licenseType && (
                <option value={form.licenseType}>{form.licenseType}</option>
              )}
              {LICENSE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
            </select>
          </label>
          <label className="grid gap-1 font-medium text-gray-700">
            경력 (년)
            <input required type="number" inputMode="numeric" min="0" max="60" step="1"
              className={INPUT_CLASS} value={form.careerYears}
              onChange={(event) => setForm({ ...form, careerYears: event.target.value })} />
          </label>
          <label className="grid gap-1 font-medium text-gray-700">
            보유 자격증 (선택)
            <textarea rows={3} maxLength={200} className={INPUT_CLASS}
              placeholder="보유한 자격증명을 입력해 주세요 (최대 200자)"
              value={form.certifications}
              onChange={(event) => setForm({ ...form, certifications: event.target.value })} />
            <span className="text-xs font-normal text-gray-500">본인 입력 정보이며, 별도 검증이나 자격 승인 처리는 이루어지지 않습니다.</span>
          </label>
          <div className="flex justify-end gap-2">
            <button type="button" disabled={saving} onClick={cancelEditing}
              className="rounded-lg border border-gray-300 px-4 py-2 font-medium hover:bg-gray-50 disabled:opacity-50">취소</button>
            <button type="submit" disabled={saving}
              className="rounded-lg bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 disabled:opacity-50">
              {saving ? '저장 중...' : '저장하기'}
            </button>
          </div>
        </form>
      )}
      {showReviews && profile && (
        <DriverReviewModal driverId={profile.id} driverName={profile.name} onClose={() => setShowReviews(false)} />
      )}
    </section>
  )
}

export default DriverProfileSection
