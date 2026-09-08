import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { Drawer } from '@/components/ui/Drawer'

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen">
      <div className="hidden lg:block">
        <div className="sticky top-0 h-screen">
          <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((v) => !v)} />
        </div>
      </div>

      <Drawer open={mobileOpen} onClose={() => setMobileOpen(false)} title="PayFlow">
        <Sidebar
          collapsed={false}
          onToggle={() => undefined}
          onNavigate={() => setMobileOpen(false)}
          className="border-0 bg-transparent shadow-none backdrop-blur-none"
        />
      </Drawer>

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar
          onMenuClick={() => setMobileOpen(true)}
          onSearch={(q) => {
            if (q.length > 1) navigate(`/transactions?search=${encodeURIComponent(q)}`)
          }}
        />
        <motion.main
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35 }}
          className="flex-1 px-4 py-6 md:px-6 lg:px-8"
        >
          <Outlet />
        </motion.main>
      </div>
    </div>
  )
}
