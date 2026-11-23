import { useEffect, useState } from 'react'
import {
  changeAdminVehicleTypeActive,
  createAdminVehicleType,
  getAdminVehicleTypes,
  updateAdminVehicleType,
} from '../../api/adminTransportApi'

const EMPTY_FORM = {
  code: '', name: '', maxLoadKg: '', baseFare: '',
  baseDistanceKm: 10, perKmFare: '', nightRate: 0,
  weekendRate: 0, heavyRate: 0, active: true,
}

const FIELD_GROUPS = [
  {
    title: '차량 기준',
    description: '이 차종에 적용할 최대 적재량입니다.',
    fields: [
      { key: 'maxLoadKg', label: '최대 적재량', unit: 'kg', min: 1, step: 100 },
    ],
  },
  {
    title: '기본 · 거리 요금',
    description: '기본 거리를 초과하면 1km당 요금이 추가됩니다.',
    fields: [
      { key: 'baseFare', label: '기본요금', unit: '원', min: 0, step: 1000 },
      { key: 'baseDistanceKm', label: '기본 포함 거리', unit: 'km', min: 0, step: 1 },
      { key: 'perKmFare', label: '초과 1km 요금', unit: '원', min: 0, step: 100 },
    ],
  },
  {
    title: '할증률',
    description: '기본 운임 계산에 적용할 비율입니다.',
    fields: [
      { key: 'nightRate', label: '야간 할증', unit: '%', min: 0, max: 500, step: 5 },
      { key: 'weekendRate', label: '주말 할증', unit: '%', min: 0, max: 500, step: 5 },
      { key: 'heavyRate', label: '중량 할증', unit: '%', min: 0, max: 500, step: 5 },
    ],
  },
]
const NUMBER_FIELDS = FIELD_GROUPS.flatMap((group) => group.fields)

// 선택 목록은 입력 편의를 위한 예시이며, 실제 운임은 관리자가 지정합니다.
const NEW_TYPE_PRESETS = [
  { code: 'TON3_5', name: '3.5톤', maxLoadKg: 3500 },
  { code: 'TON11', name: '11톤', maxLoadKg: 11000 },
  { code: 'TON25', name: '25톤', maxLoadKg: 25000 },
]

const INPUT_CLASS = 'mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm text-gray-900 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 disabled:bg-gray-100'

const NumberField = ({ field, value, onChange }) => {
  const number = value === '' ? null : Number(value)
  const display = number === null || !Number.isFinite(number) ? '' : number.toLocaleString('ko-KR')
  const adjust = (direction) => {
    const initial = number === null || !Number.isFinite(number) ? 0 : number
    const next = Math.max(field.min, Math.min(field.max ?? Number.MAX_SAFE_INTEGER, initial + direction * field.step))
    onChange(next)
  }

  return (
    <label className="block text-xs font-semibold text-gray-700">
      {field.label}
      <div className="mt-1 flex h-10 overflow-hidden rounded-lg border border-gray-200 bg-white focus-within:border-emerald-500 focus-within:ring-1 focus-within:ring-emerald-500">
        <button type="button" onClick={() => adjust(-1)} aria-label={`${field.label} 감소`}
          className="w-9 shrink-0 border-r border-gray-100 bg-gray-50 text-base text-gray-600 hover:bg-emerald-50">−</button>
        <input type="text" inputMode="numeric" value={display}
          onChange={(event) => {
            const digits = event.target.value.replace(/[^0-9]/g, '')
            onChange(digits === '' ? '' : Number(digits))
          }}
          placeholder="직접 입력"
          aria-label={`${field.label} (${field.unit})`}
          className="min-w-0 flex-1 bg-transparent px-2 text-right text-sm font-medium text-gray-900 focus:outline-none" />
        <span className="flex items-center pr-2 text-xs text-gray-500">{field.unit}</span>
        <button type="button" onClick={() => adjust(1)} aria-label={`${field.label} 증가`}
          className="w-9 shrink-0 border-l border-gray-100 bg-gray-50 text-base text-gray-600 hover:bg-emerald-50">+</button>
      </div>
    </label>
  )
}

const VehicleTypeFareManagePage = () => {
  const [refreshKey, setRefreshKey] = useState(0)
  const [fetchState, setFetchState] = useState({ key: -1, items: [], error: '' })
  const [mode, setMode] = useState('edit')
  const [editingId, setEditingId] = useState(null)
  const [newPreset, setNewPreset] = useState('')
  const [copyFromId, setCopyFromId] = useState('')
  const [form, setForm] = useState({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const loading = fetchState.key !== refreshKey

  useEffect(() => {
    let alive = true
    getAdminVehicleTypes()
      .then((items) => {
        if (alive) setFetchState({ key: refreshKey, items, error: '' })
      })
      .catch((err) => {
        if (alive) setFetchState((prev) => ({
          ...prev, key: refreshKey,
          error: err.response?.data?.message || '차종·요금 목록을 불러오지 못했습니다.',
        }))
      })
    return () => { alive = false }
  }, [refreshKey])

  const startEdit = (item, scroll = false) => {
    setMode('edit')
    setEditingId(item.id)
    setForm({
      code: item.code, name: item.name, maxLoadKg: item.maxLoadKg,
      baseFare: item.baseFare, baseDistanceKm: item.baseDistanceKm,
      perKmFare: item.perKmFare, nightRate: item.nightRate,
      weekendRate: item.weekendRate, heavyRate: item.heavyRate, active: item.active,
    })
    setError('')
    if (scroll) window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const startCreate = () => {
    setMode('create')
    setEditingId(null)
    setNewPreset('')
    setCopyFromId('')
    setForm({ ...EMPTY_FORM })
    setError('')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setNewPreset('')
    setCopyFromId('')
    setForm({ ...EMPTY_FORM })
    setError('')
  }

  const choosePreset = (value) => {
    setNewPreset(value)
    const selected = NEW_TYPE_PRESETS.find((item) => item.code === value)
    if (selected) {
      setForm((prev) => ({ ...prev, code: selected.code, name: selected.name, maxLoadKg: selected.maxLoadKg }))
    } else {
      setForm((prev) => ({ ...prev, code: '', name: '', maxLoadKg: '' }))
    }
    setError('')
  }

  const copyRates = (value) => {
    setCopyFromId(value)
    const source = fetchState.items.find((item) => String(item.id) === value)
    if (!source) return
    setForm((prev) => ({
      ...prev,
      baseFare: source.baseFare,
      baseDistanceKm: source.baseDistanceKm,
      perKmFare: source.perKmFare,
      nightRate: source.nightRate,
      weekendRate: source.weekendRate,
      heavyRate: source.heavyRate,
    }))
    setError('')
  }

  const submit = async (event) => {
    event.preventDefault()
    if (mode === 'edit' && editingId === null) {
      setError('수정할 차종을 먼저 선택해 주세요.')
      return
    }
    const payload = { ...form, code: form.code.trim().toUpperCase(), name: form.name.trim() }
    for (const field of NUMBER_FIELDS) {
      if (payload[field.key] === '') {
        setError(`${field.label} 값을 입력해 주세요.`)
        return
      }
      const value = Number(payload[field.key])
      if (!Number.isSafeInteger(value) || value < field.min || (field.max !== undefined && value > field.max)) {
        setError(`${field.label} 값을 확인해 주세요.`)
        return
      }
      payload[field.key] = value
    }
    if (!/^[A-Z][A-Z0-9_]{0,29}$/.test(payload.code) || !payload.name || payload.name.length > 60) {
      setError('차종 코드(영문 대문자·숫자·밑줄)와 이름을 확인해 주세요.')
      return
    }
    if (mode === 'create' && fetchState.items.some((item) => item.code === payload.code)) {
      setError('이미 등록된 차종 코드입니다. 기존 차종 수정에서 해당 차종을 선택해 주세요.')
      return
    }
    setSaving(true)
    setError('')
    try {
      if (mode === 'edit') {
        await updateAdminVehicleType(editingId, payload)
      } else {
        await createAdminVehicleType(payload)
      }
      cancelEdit()
      setRefreshKey((value) => value + 1)
    } catch (err) {
      setError(err.response?.data?.message || '차종·요금 저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const toggleActive = async (item) => {
    if (!window.confirm(`${item.name} 차종을 ${item.active ? '비활성화' : '활성화'}할까요?`)) return
    try {
      await changeAdminVehicleTypeActive(item.id, !item.active)
      setRefreshKey((value) => value + 1)
    } catch (err) {
      alert(err.response?.data?.message || '차종 상태 변경에 실패했습니다.')
    }
  }

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">차종·요금 관리</h1>
        <p className="mt-1 text-xs text-gray-500">기본요금, 거리요금, 야간·주말·중량 할증률을 관리합니다.</p>
        <p className="mt-1 text-xs text-amber-700">변경된 요금은 앞으로 계산하는 운임에 적용됩니다. 이미 저장된 배송 운임은 유지됩니다.</p>
      </div>

      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="flex flex-wrap gap-2 border-b border-gray-100 p-4">
          <button type="button" onClick={() => { setMode('edit'); cancelEdit() }}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${mode === 'edit' ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            기존 차종 요금 수정
          </button>
          <button type="button" onClick={startCreate}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${mode === 'create' ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            + 새 차종 등록
          </button>
        </div>

        <form onSubmit={submit} className="space-y-5 p-5">
          {mode === 'edit' ? (
            <div>
              <label className="block max-w-xl text-xs font-semibold text-gray-700">
                수정할 차종 선택
                <select value={editingId ?? ''} disabled={loading || !!fetchState.error}
                  onChange={(event) => {
                    const item = fetchState.items.find((type) => String(type.id) === event.target.value)
                    if (item) startEdit(item)
                    else cancelEdit()
                  }} className={INPUT_CLASS}>
                  <option value="">차종을 선택해 주세요</option>
                  {fetchState.items.map((item) => (
                    <option key={item.id} value={item.id}>{item.name} ({item.code}){item.active ? '' : ' · 비활성'}</option>
                  ))}
                </select>
              </label>
              <p className="mt-2 text-xs text-gray-500">차종을 선택하면 현재 운임과 할증률이 자동으로 채워집니다.</p>
            </div>
          ) : (
            <div className="space-y-4">
              <label className="block max-w-xl text-xs font-semibold text-gray-700">
                차종 선택 (예시)
                <select value={newPreset} onChange={(event) => choosePreset(event.target.value)} className={INPUT_CLASS}>
                  <option value="">직접 입력하기</option>
                  {NEW_TYPE_PRESETS.map((item) => {
                    const exists = fetchState.items.some((type) => type.code === item.code)
                    return <option key={item.code} value={item.code} disabled={exists}>{item.name} ({item.code}){exists ? ' · 이미 등록됨' : ''}</option>
                  })}
                </select>
              </label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label className="text-xs font-semibold text-gray-700">
                  차종 코드 *
                  <input value={form.code} maxLength={30}
                    onChange={(event) => { setNewPreset(''); setForm((prev) => ({ ...prev, code: event.target.value.toUpperCase() })) }}
                    placeholder="예: TON3_5" className={INPUT_CLASS} />
                </label>
                <label className="text-xs font-semibold text-gray-700">
                  차종명 *
                  <input value={form.name} maxLength={60}
                    onChange={(event) => { setNewPreset(''); setForm((prev) => ({ ...prev, name: event.target.value })) }}
                    placeholder="예: 3.5톤" className={INPUT_CLASS} />
                </label>
              </div>
              <p className="text-xs text-gray-500">목록에 없는 신규 차종은 코드·이름을 직접 입력할 수 있어요. 기존 차종을 수정할 때는 다시 입력하지 않아도 됩니다.</p>
              <label className="block max-w-xl text-xs font-semibold text-gray-700">
                기존 차종의 요금 복사 (선택)
                <select value={copyFromId} onChange={(event) => copyRates(event.target.value)} className={INPUT_CLASS}>
                  <option value="">직접 요금 설정하기</option>
                  {fetchState.items.map((item) => <option key={item.id} value={item.id}>{item.name} 요금 가져오기</option>)}
                </select>
              </label>
              <p className="text-xs text-amber-700">복사한 요금은 참고값입니다. 새 차종의 실제 운임이 맞는지 확인한 뒤 등록해 주세요.</p>
            </div>
          )}

          {mode === 'edit' && editingId === null ? (
            <div className="rounded-lg bg-gray-50 px-4 py-8 text-center text-sm text-gray-500">
              위에서 차종을 선택하거나, 아래 목록의 '수정'을 누르면 요금을 바로 변경할 수 있어요.
            </div>
          ) : (
            <>
              {mode === 'edit' && (
                <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-emerald-50 px-4 py-3">
                  <div>
                    <p className="text-sm font-bold text-emerald-900">{form.name}</p>
                    <p className="text-xs text-emerald-700">차종 코드: {form.code} · 기존 차종의 요금을 수정합니다.</p>
                  </div>
                  <button type="button" onClick={cancelEdit} className="text-xs font-semibold text-gray-600 underline hover:text-gray-900">선택 해제</button>
                </div>
              )}
              {FIELD_GROUPS.map((group) => (
                <div key={group.title} className="rounded-xl border border-gray-100 p-4">
                  <div className="mb-3">
                    <h3 className="text-sm font-bold text-gray-800">{group.title}</h3>
                    <p className="mt-1 text-xs text-gray-500">{group.description}</p>
                  </div>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {group.fields.map((field) => (
                      <NumberField key={field.key} field={field} value={form[field.key]}
                        onChange={(value) => setForm((prev) => ({ ...prev, [field.key]: value }))} />
                    ))}
                  </div>
                </div>
              ))}
              <div className="flex flex-wrap items-center justify-between gap-3">
                <label className="flex items-center gap-2 text-xs font-semibold text-gray-700">
                  <input type="checkbox" checked={form.active}
                    onChange={(event) => setForm((prev) => ({ ...prev, active: event.target.checked }))} />
                  고객·기사 화면에 노출 (활성)
                </label>
                <button type="submit" disabled={saving}
                  className="rounded-lg bg-emerald-600 px-5 py-2.5 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50">
                  {saving ? '저장 중...' : mode === 'edit' ? '변경 사항 저장' : '차종 등록'}
                </button>
              </div>
            </>
          )}
          {error && <p role="alert" className="text-xs font-semibold text-red-600">{error}</p>}
        </form>
      </div>

      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        {loading ? <p className="p-8 text-center text-sm text-gray-500">차종 목록을 불러오는 중입니다...</p>
          : fetchState.error ? <p role="alert" className="p-8 text-center text-sm text-red-600">{fetchState.error}</p>
          : <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-3 py-3">차종 (코드)</th>
                  <th className="px-3 py-3 text-right">적재량</th>
                  <th className="px-3 py-3 text-right">기본요금 / 거리</th>
                  <th className="px-3 py-3 text-right">초과 km당</th>
                  <th className="px-3 py-3">할증률 (야간 / 주말 / 중량)</th>
                  <th className="px-3 py-3">상태</th>
                  <th className="px-3 py-3 text-right">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {fetchState.items.map((item) => (
                  <tr key={item.id} className={item.active ? 'hover:bg-gray-50' : 'bg-gray-100 text-gray-500'}>
                    <td className="px-3 py-3"><span className="font-bold">{item.name}</span><div className="mt-0.5 text-gray-500">{item.code}</div></td>
                    <td className="px-3 py-3 text-right">{Number(item.maxLoadKg).toLocaleString()}kg</td>
                    <td className="px-3 py-3 text-right"><span className="font-semibold">{Number(item.baseFare).toLocaleString()}원</span><div className="mt-0.5">{item.baseDistanceKm}km 포함</div></td>
                    <td className="px-3 py-3 text-right">{Number(item.perKmFare).toLocaleString()}원</td>
                    <td className="px-3 py-3">{item.nightRate}% / {item.weekendRate}% / {item.heavyRate}%</td>
                    <td className="px-3 py-3"><span className={`rounded-full px-2 py-1 font-semibold ${item.active ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-200 text-gray-600'}`}>{item.active ? '활성' : '비활성'}</span></td>
                    <td className="whitespace-nowrap px-3 py-3 text-right">
                      <button type="button" onClick={() => startEdit(item, true)} className="rounded border border-gray-200 px-2.5 py-1 font-semibold hover:bg-gray-50">수정</button>
                      <button type="button" onClick={() => toggleActive(item)} className="ml-2 rounded border border-gray-200 px-2.5 py-1 font-semibold hover:bg-gray-50">{item.active ? '비활성화' : '활성화'}</button>
                    </td>
                  </tr>
                ))}
                {fetchState.items.length === 0 && <tr><td colSpan={7} className="p-8 text-center text-gray-500">등록된 차종이 없습니다.</td></tr>}
              </tbody>
            </table>
          </div>}
      </div>
    </section>
  )
}

export default VehicleTypeFareManagePage
