import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createVehicle } from '../../api/vehicleApi'

const initForm = {
    plateNo: '',
    vehicleTypeId: '',
    status: 'AVAILABLE',
    driverId: '',
}

const VehicleRegisterPage = () => {
    const [form, setForm] = useState(initForm)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const navigate = useNavigate()

    // 입력값 변경 시 form 상태 업데이트
    const handleChange = (e) => {
        const { name, value } = e.target

        setForm({
            ...form,
            [name]: value,
        })
    }

    // 차량 등록 API 호출
    const handleSubmit = async (e) => {
        e.preventDefault()

        setLoading(true)
        setError('')

        try {
            await createVehicle({
                plateNo: form.plateNo.trim(),
                vehicleTypeId: Number(form.vehicleTypeId),
                status: form.status,
                driverId: form.driverId ? Number(form.driverId) : null,
            })

            navigate('/admin/vehicles')
        } catch (err) {
            setError(
                err.response?.data?.message ?? '차량 등록에 실패했습니다.'
            )
        } finally {
            setLoading(false)
        }
    }

    return (
        <section className="grid max-w-xl gap-6">
            <div>
                <h1 className="text-2xl font-bold">차량 등록</h1>
                <p className="mt-2 text-sm text-gray-600">
                    새로운 차량 정보를 입력해 주세요.
                </p>
            </div>

            <form
                onSubmit={handleSubmit}
                className="grid gap-5 rounded-lg border border-gray-200 bg-white p-6"
            >
                <label className="grid gap-2 text-sm">
                    <span className="font-medium">차량번호</span>
                    <input
                        type="text"
                        name="plateNo"
                        value={form.plateNo}
                        onChange={handleChange}
                        placeholder="예: 12가3456"
                        maxLength={20}
                        required
                        className="rounded border border-gray-300 px-3 py-2"
                    />
                </label>

                <label className="grid gap-2 text-sm">
                    <span className="font-medium">차종 ID</span>
                    <input
                        type="number"
                        name="vehicleTypeId"
                        value={form.vehicleTypeId}
                        onChange={handleChange}
                        placeholder="등록된 차종 ID 입력"
                        min="1"
                        required
                        className="rounded border border-gray-300 px-3 py-2"
                    />
                </label>

                <label className="grid gap-2 text-sm">
                    <span className="font-medium">차량 상태</span>
                    <select
                        name="status"
                        value={form.status}
                        onChange={handleChange}
                        className="rounded border border-gray-300 px-3 py-2"
                    >
                        <option value="AVAILABLE">운행 가능</option>
                        <option value="MAINTENANCE">정비 중</option>
                    </select>
                </label>

                <label className="grid gap-2 text-sm">
                    <span className="font-medium">담당 기사 ID (선택)</span>
                    <input
                        type="number"
                        name="driverId"
                        value={form.driverId}
                        onChange={handleChange}
                        placeholder="미배정 시 비워두세요"
                        min="1"
                        className="rounded border border-gray-300 px-3 py-2"
                    />
                </label>

                {error && (
                    <p role="alert" className="text-sm text-red-600">
                        {error}
                    </p>
                )}

                <div className="flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={() => navigate('/admin/vehicles')}
                        className="rounded border border-gray-300 px-4 py-2"
                    >
                        취소
                    </button>

                    <button
                        type="submit"
                        disabled={loading}
                        className="rounded bg-emerald-700 px-4 py-2 text-white disabled:opacity-50"
                    >
                        {loading ? '등록 중...' : '차량 등록'}
                    </button>
                </div>
            </form>
        </section>
    )
}

export default VehicleRegisterPage
