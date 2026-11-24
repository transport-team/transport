// 공통 확인 창 — hooks/useConfirm과 함께 쓴다 (브라우저 기본 window.confirm 대신, #143)
// title · message(\n 줄바꿈 그대로) · confirmLabel(기본 '확인') · cancelLabel(기본 '취소')
// danger: 취소 · 거절 · 삭제처럼 되돌리기 어려운 동작 → 빨간 확인 버튼, 처음 포커스는 '취소'에 (Enter 실수 방지)
import Modal from './Modal'

const ConfirmModal = ({
  open,
  title = '확인',
  message,
  confirmLabel = '확인',
  cancelLabel = '취소',
  danger = false,
  onConfirm,
  onCancel,
}) => (
  <Modal open={open} onClose={onCancel} title={title} size="sm">
    <p className="whitespace-pre-line text-sm leading-6 text-gray-700">{message}</p>
    <div className="mt-6 flex justify-end gap-2">
      <button
        type="button"
        onClick={onCancel}
        autoFocus={danger}
        className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50"
      >
        {cancelLabel}
      </button>
      <button
        type="button"
        onClick={onConfirm}
        autoFocus={!danger}
        className={`rounded px-4 py-2 text-sm font-semibold text-white ${danger ? 'bg-red-600 hover:bg-red-700' : 'bg-primary hover:bg-primary-hover'}`}
      >
        {confirmLabel}
      </button>
    </div>
  </Modal>
)

export default ConfirmModal
