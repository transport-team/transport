import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getActiveDrivers, getVehicleTypes, updateVehicle } from '../../api/vehicleApi'

const VehicleEditPage = () => {
    const { id } = useParams()
    const { state } = useLocation()
    const navigate = useNavigate()

    // 목록에서 전달받은 차량 정보
    const vehicle = state?.vehicle

    // 상태 선언은 effect보다 위에 둔다 (effect 안에서 setError를 쓴다 — react-hooks/immutability)
    const [form, setForm] = useState({
        plateNo: vehicle?.plateNo ?? '',
        vehicleTypeId: vehicle?.vehicleTypeId ?? '',
        status: vehicle?.status ?? 'AVAILABLE',
        driverId: vehicle?.driverId ?? '',
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [drivers, setDrivers] = useState([])
    const [vehicleTypes, setVehicleTypes] = useState([])

    // 수정 화면에서 선택할 활성 기사 목록 조회
    useEffect(() => {
        getActiveDrivers()
            .then((data) => {
                setDrivers(data)
            })
            .catch((err) => {
                console.error('기사 목록 조회 실패:', err)
                setError('기사 목록을 불러오지 못했습니다.')
            })
    }, [])

    // 수정 화면에서 선택할 차종 목록 조회
    useEffect(() => {
        getVehicleTypes()
            .then((data) => {
                // 현재 차량의 차종은 비활성 상태여도 표시
                setVehicleTypes(
                    data.filter(
                        (type) => type.active || type.id === Number(vehicle?.vehicleTypeId)
                    )
                )
            })
            .catch((err) => {
                console.error('차종 목록 조회 실패:', err)
                setError('차종 목록을 불러오지 못했습니다.')
            })
    }, [vehicle?.vehicleTypeId])

    // 입력값 변경
    const handleChange = (e) => {
        const { name, value } = e.target

        setForm({
            ...form,
            [name]: value,
        })
    }

    // 차량 정보 수정
    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)
        setError('')

        try {
            await updateVehicle(id, {
                plateNo: form.plateNo.trim(),
                vehicleTypeId: Number(form.vehicleTypeId),
                status: form.status,
                driverId: form.driverId ? Number(form.driverId) : null,
            })
            // 수정 성공 후 목록으로 이동하며 토스트 메시지 전달
            navigate('/admin/vehicles', {
                state: { toast: '차량 수정이 완료되었습니다.' }
            })
        } catch (err) {
            setError(err.response?.data?.message ?? '차량 수정에 실패했습니다.')
        } finally {
            setLoading(false)
        }
    }

    if (!vehicle) {
        return (
            <section className="space-y-4">
                <h1 className="text-2xl font-bold">차량 수정</h1>
                <p>차량 정보가 없습니다. 목록에서 다시 선택해 주세요.</p>
                <button
                    type="button"
                    onClick={() => navigate('/admin/vehicles')}
                    className="rounded border px-4 py-2"
                >
                    목록으로
                </button>
            </section>
        )
    }

    return (
        <section className="grid max-w-xl gap-6">
            <h1 className="text-2xl font-bold">차량 수정</h1>

            <form
                onSubmit={handleSubmit}
                noValidate
                className="grid gap-5 rounded-lg border border-gray-200 bg-white p-6"
            >
                <label className="grid gap-2 text-sm">
                    <span className="font-medium">차량번호</span>
                    <input
                        name="plateNo"
                        value={form.plateNo}
                        onChange={handleChange}
                        maxLength={20}
                        required
                        className="rounded border border-gray-300 px-3 py-2"
                    />
                </label>

                <label className="grid gap-2 text-sm">
                    <span className="font-medium">차종 선택</span>
                    <select
                        name="vehicleTypeId"
                        value={form.vehicleTypeId}
                        onChange={handleChange}
                        required
                        className="rounded border border-gray-300 px-3 py-2"
                    >
                        <option value="">차종을 선택해 주세요</option>

                        {vehicleTypes.map((type) => (
                            <option key={type.id} value={type.id}>
                                {type.id} ({type.name}){!type.active ? ' - 비활성 차종' : ''}
                            </option>
                        ))}
                    </select>
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
                        <option value="INACTIVE">비활성</option>
                    </select>
                </label>


                <label className="grid gap-2 text-sm">
                    <span className="font-medium">담당 기사 선택 (선택)</span>
                    <select
                        name="driverId"
                        value={form.driverId}
                        onChange={handleChange}
                        className="rounded border border-gray-300 px-3 py-2"
                    >
                        <option value="">담당 기사 미배정</option>

                        {drivers.map((driver) => (
                            <option key={driver.id} value={driver.id}>
                                {driver.id} ({driver.name} · {driver.phone})
                            </option>
                        ))}
                    </select>
                </label>


                {error && <p className="text-sm text-red-600">{error}</p>}

                <div className="flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={() => navigate('/admin/vehicles')}
                        className="rounded border px-4 py-2"
                    >
                        취소
                    </button>

                    <button
                        type="submit"
                        disabled={loading}
                        className="rounded bg-emerald-700 px-4 py-2 text-white disabled:opacity-50"
                    >
                        {loading ? '수정 중...' : '수정 완료'}
                    </button>
                </div>
            </form>
        </section>
    )
}

export default VehicleEditPage
