import { useEffect, useState } from 'react'
import {
  changeAdminCargoTypeActive,
  createAdminCargoType,
  getAdminCargoTypes,
  updateAdminCargoType,
} from '../../api/adminTransportApi'

const INITIAL_FORM = { name: '', extraFee: 0, handlingNote: '', active: true }

const CargoTypeManagePage = () => {
  const [refreshCount, setRefreshCount] = useState(0)
  const [fetchState, setFetchState] = useState({ resolvedKey: -1, items: [], error: '' })
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(INITIAL_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  const loading = fetchState.resolvedKey !== refreshCount

  useEffect(() => {
    let active = true

    getAdminCargoTypes()
      .then((list) => {
        if (!active) return
        setFetchState({ resolvedKey: refreshCount, items: list, error: '' })
      })
      .catch((err) => {
        if (!active) return
        setFetchState((prev) => ({
          ...prev,
          resolvedKey: refreshCount,
          error: err.response?.data?.message || '화물 유형 목록을 불러오지 못했습니다.',
        }))
      })

    return () => { active = false }
  }, [refreshCount])

  const handleStartEdit = (item) => {
    setEditingId(item.id)
    setFormError('')
    setForm({
      name: item.name,
      extraFee: item.extraFee,
      handlingNote: item.handlingNote || '',
      active: item.active,
    })
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setForm(INITIAL_FORM)
    setFormError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.name.trim()) {
      setFormError('화물 유형 이름을 입력해 주세요.')
      return
    }

    setSubmitting(true)
    setFormError('')
    try {
      const payload = {
        name: form.name.trim(),
        extraFee: Number(form.extraFee) || 0,
        handlingNote: form.handlingNote.trim(),
        active: form.active,
      }
      if (editingId) {
        await updateAdminCargoType(editingId, payload)
      } else {
        await createAdminCargoType(payload)
      }
      handleCancelEdit()
      setRefreshCount((c) => c + 1)
    } catch (err) {
      setFormError(err.response?.data?.message || '저장에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggleActive = async (item) => {
    try {
      await changeAdminCargoTypeActive(item.id, !item.active)
      setRefreshCount((c) => c + 1)
    } catch (err) {
      alert(err.response?.data?.message || '상태 변경에 실패했습니다.')
    }
  }

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">화물 종류 관리</h1>
        <p className="mt-0.5 text-xs text-gray-500">배송 신청 시 선택 가능한 화물 유형과 추가 요금, 취급 안내를 등록·관리합니다.</p>
      </div>

      {/* 등록 / 수정 폼 */}
      <form onSubmit={handleSubmit} className="rounded-xl border border-gray-200 bg-white p-5 shadow-xs">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-bold text-gray-800">
            {editingId ? `화물 유형 수정 (#${editingId})` : '새 화물 유형 등록'}
          </h2>
          {editingId && (
            <button
              type="button"
              onClick={handleCancelEdit}
              className="text-xs font-semibold text-gray-500 hover:text-gray-800"
            >
              수정 취소
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <div>
            <label className="mb-1 block text-xs font-semibold text-gray-600">유형 이름 *</label>
            <input
              type="text"
              maxLength={30}
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              placeholder="예: 냉장/냉동 화물"
              className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-gray-600">추가 요금 (원)</label>
            <input
              type="number"
              min={0}
              step={1000}
              value={form.extraFee}
              onChange={(e) => setForm((prev) => ({ ...prev, extraFee: e.target.value }))}
              className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>

          <div className="md:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-gray-600">기사 취급 안내</label>
            <input
              type="text"
              maxLength={255}
              value={form.handlingNote}
              onChange={(e) => setForm((prev) => ({ ...prev, handlingNote: e.target.value }))}
              placeholder="예: 상하차 시 충격 주의 및 적재함 고정 필수"
              className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>

        {formError && (
          <p className="mt-2.5 text-xs font-semibold text-red-600" role="alert">{formError}</p>
        )}

        <div className="mt-4 flex items-center justify-between">
          <label className="flex items-center gap-2 text-xs font-semibold text-gray-700">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))}
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            고객 신청 화면에 즉시 노출 (활성)
          </label>

          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-blue-600 px-5 py-2 text-xs font-bold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? '저장 중...' : editingId ? '수정 완료' : '화물 유형 등록'}
          </button>
        </div>
      </form>

      {/* 목록 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xs">
        {loading ? (
          <div className="p-10 text-center text-sm text-gray-500">화물 유형 목록을 불러오는 중입니다...</div>
        ) : fetchState.error ? (
          <div className="p-8 text-center text-sm font-semibold text-red-600">{fetchState.error}</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-semibold">ID</th>
                  <th className="px-4 py-3 font-semibold">화물 유형명</th>
                  <th className="px-4 py-3 text-right font-semibold">추가 요금</th>
                  <th className="px-4 py-3 font-semibold">취급 안내</th>
                  <th className="px-4 py-3 font-semibold">상태</th>
                  <th className="px-4 py-3 text-right font-semibold">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {fetchState.items.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50/70">
                    <td className="px-4 py-3 font-semibold text-gray-500">{item.id}</td>
                    <td className="px-4 py-3 font-bold text-gray-900">{item.name}</td>
                    <td className="px-4 py-3 text-right font-semibold text-gray-800">
                      +{Number(item.extraFee).toLocaleString()}원
                    </td>
                    <td className="px-4 py-3 text-gray-600">{item.handlingNote || '-'}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block rounded-full border px-2.5 py-0.5 font-bold ${
                          item.active
                            ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                            : 'border-gray-200 bg-gray-100 text-gray-500'
                        }`}
                      >
                        {item.active ? '활성' : '비활성'}
                      </span>
                    </td>
                    <td className="space-x-2 px-4 py-3 text-right whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => handleStartEdit(item)}
                        className="rounded border border-gray-200 px-2.5 py-1 font-semibold text-gray-700 hover:bg-gray-50"
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        onClick={() => handleToggleActive(item)}
                        className="rounded border border-gray-200 px-2.5 py-1 font-semibold text-gray-700 hover:bg-gray-50"
                      >
                        {item.active ? '비활성화' : '활성화'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}

export default CargoTypeManagePage