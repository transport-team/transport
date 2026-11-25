import { useEffect, useState } from 'react'
import {
  createSavedAddress,
  deleteSavedAddress,
  getSavedAddresses,
  updateSavedAddress,
} from '../../api/addressApi'
import { Button, ConfirmModal, Input } from '../common'

const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
const emptyAddress = { label: '', address: '', detail: '' }

// 기존 마이페이지의 DefaultAddressForm 자리에서 동작하는 새 다중 배송지 주소록.
// 출발·도착 세트를 저장하지 않고 주소 하나씩 저장한다.
const DefaultAddressForm = () => {
  const [addresses, setAddresses] = useState([])
  const [form, setForm] = useState(emptyAddress)
  const [editingId, setEditingId] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    getSavedAddresses()
      .then((items) => {
        if (active) setAddresses(Array.isArray(items) ? items : [])
      })
      .catch(() => {
        if (active) setError('저장한 배송지를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    if (!window.daum?.Postcode && !document.getElementById(POSTCODE_SCRIPT_ID)) {
      const script = document.createElement('script')
      script.id = POSTCODE_SCRIPT_ID
      script.src = POSTCODE_SCRIPT_URL
      script.async = true
      document.head.appendChild(script)
    }
    return () => { active = false }
  }, [])

  const closeForm = () => {
    setFormOpen(false)
    setEditingId(null)
    setForm(emptyAddress)
    setError('')
  }

  const startCreate = () => {
    setEditingId(null)
    setForm(emptyAddress)
    setError('')
    setNotice('')
    setFormOpen(true)
  }

  const startEdit = (address) => {
    setEditingId(address.id)
    setForm({ label: address.label || '', address: address.address || '', detail: address.detail || '' })
    setError('')
    setNotice('')
    setFormOpen(true)
  }

  const openPostcode = () => {
    if (!window.daum?.Postcode) {
      setError('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.')
      return
    }
    new window.daum.Postcode({
      oncomplete: (data) => {
        setForm((current) => ({ ...current, address: data.roadAddress || data.jibunAddress }))
        setError('')
      },
    }).open()
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!form.label.trim() || !form.address.trim()) {
      setError('배송지 별명과 주소를 입력해 주세요.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const payload = {
        label: form.label.trim(),
        address: form.address.trim(),
        detail: form.detail.trim(),
      }
      const saved = editingId
        ? await updateSavedAddress(editingId, payload)
        : await createSavedAddress(payload)
      setAddresses((current) => editingId
        ? current.map((item) => item.id === editingId ? saved : item)
        : [saved, ...current])
      closeForm()
      setNotice(editingId ? '배송지를 수정했습니다.' : '새 배송지를 저장했습니다.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || '배송지를 저장하지 못했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const confirmDelete = async () => {
    if (!deleting) return
    setSaving(true)
    setError('')
    try {
      await deleteSavedAddress(deleting.id)
      setAddresses((current) => current.filter((item) => item.id !== deleting.id))
      if (editingId === deleting.id) closeForm()
      setNotice('배송지를 삭제했습니다.')
      setDeleting(null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || '배송지를 삭제하지 못했습니다.')
      setDeleting(null)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4" aria-labelledby="saved-address-title">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 id="saved-address-title" className="font-semibold">배송지 저장</h2>
          <p className="mt-1 text-sm text-gray-500">집, 회사처럼 별명을 붙여 주소 한 곳씩 저장하세요. 배송 신청할 때 출발지 또는 도착지로 선택할 수 있어요.</p>
        </div>
        {!formOpen && <Button size="sm" disabled={loading} onClick={startCreate}>+ 배송지 추가</Button>}
      </div>

      {loading ? (
        <p className="text-sm text-gray-500">배송지 불러오는 중...</p>
      ) : addresses.length === 0 ? (
        <p className="rounded-lg bg-gray-50 p-3 text-sm text-gray-500">저장한 배송지가 없습니다.</p>
      ) : (
        <ul className="grid gap-2">
          {addresses.map((item) => (
            <li key={item.id} className="flex flex-wrap items-start justify-between gap-3 rounded-lg border border-gray-200 p-3">
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-gray-900">{item.label}</p>
                <p className="mt-1 break-words text-sm text-gray-600">{item.address}</p>
                {item.detail && <p className="break-words text-xs text-gray-500">{item.detail}</p>}
              </div>
              <div className="flex shrink-0 gap-2">
                <button type="button" onClick={() => startEdit(item)} disabled={saving} className="rounded-lg border border-gray-200 px-2.5 py-1.5 text-xs text-gray-700 hover:bg-gray-50 disabled:opacity-50">수정</button>
                <button type="button" onClick={() => setDeleting(item)} disabled={saving} className="rounded-lg border border-red-200 px-2.5 py-1.5 text-xs text-red-600 hover:bg-red-50 disabled:opacity-50">삭제</button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {formOpen && (
        <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-blue-100 bg-blue-50/40 p-3">
          <h3 className="text-sm font-semibold">{editingId ? '배송지 수정' : '새 배송지 등록'}</h3>
          <Input id="savedAddressLabel" label="배송지 별명" value={form.label} onChange={(event) => setForm((current) => ({ ...current, label: event.target.value }))} maxLength={50} placeholder="예: 우리 집, 회사 창고" required />
          <div>
            <label htmlFor="savedAddressAddress" className="mb-1 block text-sm font-medium">주소</label>
            <div className="flex gap-2">
              <input id="savedAddressAddress" readOnly required value={form.address} placeholder="주소 검색으로 선택하세요" className="min-w-0 flex-1 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm" />
              <Button type="button" size="sm" variant="outline" onClick={openPostcode}>주소 검색</Button>
            </div>
          </div>
          <Input id="savedAddressDetail" label="상세주소 (선택)" value={form.detail} onChange={(event) => setForm((current) => ({ ...current, detail: event.target.value }))} maxLength={200} placeholder="동·호수, 층 등" />
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={saving}>{saving ? '저장 중...' : editingId ? '수정 저장' : '배송지 저장'}</Button>
            <Button type="button" size="sm" variant="ghost" onClick={closeForm} disabled={saving}>취소</Button>
          </div>
        </form>
      )}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="text-sm text-emerald-700">{notice}</p>}
      <ConfirmModal
        open={Boolean(deleting)}
        title="배송지 삭제"
        message={deleting ? `‘${deleting.label}’ 배송지를 삭제할까요?` : ''}
        confirmLabel="삭제"
        danger
        onConfirm={confirmDelete}
        onCancel={() => { if (!saving) setDeleting(null) }}
      />
    </section>
  )
}

export default DefaultAddressForm
