import { NavLink } from 'react-router-dom'

// 메뉴 한 칸: 화면이 있으면(to) 링크, 아직 없으면 '준비 중'으로 흐리게
const MenuLink = ({ menu, className = '' }) => {
  if (!menu.to) {
    return <span className={`${className} cursor-not-allowed text-gray-400`} title="준비 중">{menu.label}</span>
  }
  return (
    <NavLink
      to={menu.to}
      end
      className={({ isActive }) => `${className} ${isActive ? 'font-semibold text-emerald-800' : 'text-gray-700 hover:text-emerald-800'}`}
    >
      <span className="inline-flex items-center gap-2">
        {menu.label}

        {menu.badge > 0 && (
          <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1.5 text-xs font-bold text-white">
            {menu.badge > 99 ? '99+' : menu.badge}
          </span>
        )}
      </span>
    </NavLink>
  )
}

export default MenuLink
