import { useEffect, useState } from 'react'
import { getVehicleTypes } from '../api/vehicleApi'

// 차종 번호 → 이름 표 ({ 2: '1톤', … }) — 관리자가 '차종 · 요금 관리'에서 바꾼 이름이 그대로 보인다 (11-26 전체 점검)
// 기사 화면마다 직접 적어 두었던 차종 표를 대신한다. 불러오기 전이나 실패하면 빈 표 → 화면은 '-'로 보인다
const useVehicleTypeNames = () => {
  const [names, setNames] = useState({})

  useEffect(() => {
    let active = true
    getVehicleTypes()
      .then((types) => {
        if (active) setNames(Object.fromEntries((types || []).map((type) => [type.id, type.name])))
      })
      .catch(() => {})
    return () => { active = false }
  }, [])

  return names
}

export default useVehicleTypeNames
