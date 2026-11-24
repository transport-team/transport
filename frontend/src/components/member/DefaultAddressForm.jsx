import { useEffect, useState } from 'react'
import { getDefaultAddress, saveDefaultAddress } from '../../api/addressApi'
import { Button, Input } from '../common'

const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

const emptyAddress = {
  label: '',
  originAddress: '',
  originDetail: '',
  destAddress: '',
  destDetail: '',
}

const DefaultAddressForm = () => {
  const [address, setAddress] = useState(emptyAddress)
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getDefaultAddress()
      .then((savedAddress) => {
        if (savedAddress) setAddress(savedAddress)
      })
      .catch(() => setError('기본 배송지를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))

    if (!window.daum?.Postcode) {
      const script = document.createElement('script')
      script.id = POSTCODE_SCRIPT_ID
      script.src = POSTCODE_SCRIPT_URL
      script.async = true
      document.head.appendChild(script)
    }
  }, [])

  const handleChange = ({ target }) => {
    setAddress((current) => ({ ...current, [target.name]: target.value }))
    setSaved(false)
    setError('')
  }

  const openPostcode = (field) => {
    if (!window.daum?.Postcode) {
      setError('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.')
      return
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        setAddress((current) => ({ ...current, [field]: data.roadAddress || data.jibunAddress }))
        setSaved(false)
        setError('')
      },
    }).open()
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!address.label.trim() || !address.originAddress || !address.destAddress) {
      setError('배송지 이름, 출발지 주소, 도착지 주소를 입력해 주세요.')
      return
    }
    saveDefaultAddress({ ...address, label: address.label.trim() })
      .then((savedAddress) => {
        setAddress(savedAddress)
        setSaved(true)
        setError('')
      })
      .catch((requestError) => {
        setError(requestError.response?.data?.message || '기본 배송지 저장에 실패했습니다.')
      })
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
      <div>
        <h2 className="font-semibold">기본 배송지</h2>
        <p className="mt-1 text-sm text-gray-500">배송 신청 화면에서 선택하면 주소가 자동으로 입력됩니다.</p>
      </div>
      <Input
        id="defaultAddressLabel"
        label="배송지 이름"
        name="label"
        value={address.label}
        onChange={handleChange}
        placeholder="예: 우리 집"
        required
      />
      {['origin', 'dest'].map((field) => (
        <div key={field} className="grid gap-2">
          <label className="text-sm font-medium" htmlFor={`default-${field}-address`}>
            {field === 'origin' ? '출발지 주소' : '도착지 주소'}
          </label>
          <div className="flex gap-2">
            <input
              id={`default-${field}-address`}
              name={`${field}Address`}
              value={address[`${field}Address`]}
              readOnly
              required
              placeholder="주소 검색을 눌러 선택하세요"
              className="min-w-0 flex-1 rounded border border-gray-300 bg-white px-3 py-2"
            />
            <Button type="button" variant="outline" onClick={() => openPostcode(`${field}Address`)}>
              주소 검색
            </Button>
          </div>
          <Input
            id={`default-${field}-detail`}
            name={`${field}Detail`}
            value={address[`${field}Detail`]}
            onChange={handleChange}
            placeholder="상세주소"
          />
        </div>
      ))}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {saved && <p role="status" className="text-sm text-emerald-700">기본 배송지를 저장했습니다.</p>}
      <Button type="submit" className="justify-self-start" disabled={loading}>
        {loading ? '불러오는 중...' : '기본 배송지 저장'}
      </Button>
    </form>
  )
}

export default DefaultAddressForm
