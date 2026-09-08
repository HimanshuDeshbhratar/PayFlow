import { Link } from 'react-router-dom'
import { motion, useScroll, useTransform } from 'framer-motion'
import {
  ArrowRight,
  Shield,
  Globe2,
  Code2,
  BarChart3,
  Lock,
  Zap,
  Layers,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { MiniAreaChart } from '@/components/charts/Charts'

const cities = [
  { city: 'New York', amount: '$1,240', status: 'Payment Successful', ok: true, x: '12%', y: '28%' },
  { city: 'London', amount: '$992', status: 'Payment Successful', ok: true, x: '58%', y: '18%' },
  { city: 'Singapore', amount: '$420', status: 'Fraud Blocked', ok: false, x: '72%', y: '52%' },
  { city: 'Bangalore', amount: '$310', status: 'Payment Successful', ok: true, x: '42%', y: '62%' },
]

const volumeData = [
  { label: 'M', value: 40 },
  { label: 'T', value: 55 },
  { label: 'W', value: 48 },
  { label: 'T', value: 70 },
  { label: 'F', value: 62 },
  { label: 'S', value: 80 },
  { label: 'S', value: 74 },
]

const stats = [
  { value: '99.99%', label: 'Uptime' },
  { value: '<100ms', label: 'Avg. response time' },
  { value: '500M+', label: 'Transactions processed' },
  { value: '180+', label: 'Countries supported' },
]

const companies = ['NovaPay', 'Orbit Retail', 'Lumen Cloud', 'Atlas Bank', 'Pulse Commerce', 'Helix Soft']

const sections = [
  {
    icon: Layers,
    title: 'Infrastructure that scales',
    body: 'Idempotent APIs, ledger-backed settlements, and event-driven processing built for high volume.',
  },
  {
    icon: Zap,
    title: 'Payments that just work',
    body: 'Cards, UPI, net banking, and wallets — one orchestration layer for global checkout.',
  },
  {
    icon: Shield,
    title: 'Fraud protection in real time',
    body: 'Weighted risk rules, device signals, and analyst workflows that block abuse before it settles.',
  },
  {
    icon: Globe2,
    title: 'Global by default',
    body: 'Multi-currency rails with city-level visibility across every corridor you operate in.',
  },
  {
    icon: Code2,
    title: 'Built for developers',
    body: 'API keys, webhooks, sandboxed checkout, and clean REST endpoints ready for production.',
  },
  {
    icon: BarChart3,
    title: 'Analytics you can act on',
    body: 'Volume trends, approval rates, and settlement health in a premium dark workspace.',
  },
  {
    icon: Lock,
    title: 'Security first',
    body: 'JWT auth, role-based access, audit trails, and secrets that never touch the client.',
  },
]

function GlobeVisual() {
  return (
    <div className="relative mx-auto aspect-square w-full max-w-xl">
      <div className="absolute inset-[8%] rounded-full bg-gradient-radial from-primary/40 via-violet-600/10 to-transparent blur-2xl" />
      <svg viewBox="0 0 400 400" className="relative z-10 h-full w-full drop-shadow-[0_0_40px_rgba(124,58,237,0.35)]">
        <defs>
          <radialGradient id="globeGlow" cx="50%" cy="45%" r="55%">
            <stop offset="0%" stopColor="#A855F7" stopOpacity="0.35" />
            <stop offset="55%" stopColor="#3B82F6" stopOpacity="0.12" />
            <stop offset="100%" stopColor="#05050C" stopOpacity="0" />
          </radialGradient>
          <linearGradient id="orbit" x1="0" y1="0" x2="1" y2="1">
            <stop stopColor="#7C3AED" />
            <stop offset="1" stopColor="#22D3EE" />
          </linearGradient>
        </defs>
        <circle cx="200" cy="200" r="150" fill="url(#globeGlow)" stroke="url(#orbit)" strokeWidth="1.5" />
        <ellipse cx="200" cy="200" rx="150" ry="55" fill="none" stroke="rgba(168,85,247,0.35)" strokeWidth="1" />
        <ellipse cx="200" cy="200" rx="110" ry="150" fill="none" stroke="rgba(34,211,238,0.25)" strokeWidth="1" />
        <ellipse cx="200" cy="200" rx="150" ry="95" fill="none" stroke="rgba(124,58,237,0.25)" strokeWidth="1" transform="rotate(35 200 200)" />
        {[
          [120, 140],
          [260, 120],
          [290, 220],
          [160, 260],
          [200, 170],
          [240, 250],
        ].map(([x, y], i) => (
          <g key={i}>
            <circle cx={x} cy={y} r="4" fill="#A855F7" />
            <circle cx={x} cy={y} r="10" fill="#A855F7" opacity="0.2" />
          </g>
        ))}
        <path d="M120 140 C180 100, 240 110, 260 120" stroke="#22D3EE" strokeWidth="1" fill="none" opacity="0.6" />
        <path d="M260 120 C300 160, 300 200, 290 220" stroke="#A855F7" strokeWidth="1" fill="none" opacity="0.55" />
        <path d="M160 260 C200 230, 230 200, 200 170" stroke="#22D3EE" strokeWidth="1" fill="none" opacity="0.45" />
      </svg>

      {cities.map((c, i) => (
        <motion.div
          key={c.city}
          className="absolute z-20 block w-[9.5rem] rounded-2xl border border-border bg-[#0B0D17]/85 p-3 shadow-card backdrop-blur-xl sm:w-40"
          style={{ left: c.x, top: c.y }}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 + i * 0.15, duration: 0.5 }}
        >
          <div className="flex items-center justify-between text-xs text-muted">
            <span>{c.city}</span>
            <span className={c.ok ? 'text-success' : 'text-danger'}>●</span>
          </div>
          <p className="mt-1 text-lg font-bold">{c.amount}</p>
          <p className={`text-xs ${c.ok ? 'text-success' : 'text-danger'}`}>{c.status}</p>
        </motion.div>
      ))}

      <motion.div
        className="absolute -bottom-2 right-0 z-20 w-56 rounded-2xl border border-border bg-[#0B0D17]/9 p-4 shadow-glow backdrop-blur-xl sm:right-4"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.8 }}
      >
        <p className="text-xs text-muted">Global Transaction Volume</p>
        <div className="mt-1 flex items-end gap-2">
          <p className="text-xl font-bold">$2.4B</p>
          <span className="mb-1 text-xs font-semibold text-success">+12.5%</span>
        </div>
        <div className="mt-2 h-14">
          <MiniAreaChart data={volumeData} />
        </div>
      </motion.div>
    </div>
  )
}

function Section({
  children,
  className = '',
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <motion.section
      className={`mx-auto max-w-6xl px-4 py-20 md:px-6 ${className}`}
      initial={{ opacity: 0, y: 28 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.55 }}
    >
      {children}
    </motion.section>
  )
}

export function LandingPage() {
  const { scrollY } = useScroll()
  const heroY = useTransform(scrollY, [0, 400], [0, 60])

  return (
    <div className="overflow-x-hidden">
      <header className="absolute inset-x-0 top-0 z-40">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-5 md:px-8">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-primary font-bold shadow-glow-sm">
              P
            </div>
            <span className="text-lg font-bold tracking-tight">PayFlow</span>
          </Link>
          <nav className="hidden items-center gap-7 text-sm text-muted lg:flex">
            {['Product', 'Solutions', 'Developers', 'Pricing', 'Company'].map((item) => (
              <a key={item} href="#platform" className="transition hover:text-foreground">
                {item}
              </a>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <Link to="/login">
              <Button variant="ghost">Sign in</Button>
            </Link>
            <Link to="/register">
              <Button>
                Get started <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </header>

      <section className="relative mx-auto grid min-h-[88vh] max-w-7xl items-center gap-8 px-4 pb-10 pt-28 md:grid-cols-2 md:gap-6 md:px-8 md:pt-24 lg:gap-12">
        <motion.div style={{ y: heroY }} className="relative z-10 flex flex-col justify-center">
          <h1 className="text-4xl font-extrabold leading-[1.02] tracking-tight sm:text-5xl lg:text-[3.5rem] xl:text-6xl">
            Payments without borders.
            <br />
            <span className="text-gradient">Built for what&apos;s next.</span>
          </h1>
          <p className="mt-5 max-w-lg text-base text-muted md:text-lg">
            Modern payment infrastructure with real-time fraud protection — designed for merchants who move money at scale.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/register">
              <Button size="lg">
                Get started <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
            <Button size="lg" variant="outline">
              Talk to sales
            </Button>
          </div>
        </motion.div>
        <div className="relative z-0 scale-95 sm:scale-100 lg:scale-110 lg:translate-x-4">
          <GlobeVisual />
        </div>
      </section>

      <Section className="!py-10">
        <div className="grid grid-cols-2 gap-6 md:grid-cols-4">
          {stats.map((s) => (
            <div key={s.label} className="text-center md:text-left">
              <p className="text-3xl font-bold tracking-tight md:text-4xl">{s.value}</p>
              <p className="mt-1 text-sm text-muted">{s.label}</p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="!py-12">
        <p className="mb-6 text-center text-xs font-semibold uppercase tracking-[0.2em] text-muted">
          Trusted by innovative companies
        </p>
        <div className="flex flex-wrap items-center justify-center gap-x-10 gap-y-4 opacity-60">
          {companies.map((c) => (
            <span key={c} className="text-sm font-semibold tracking-wide text-foreground/80">
              {c}
            </span>
          ))}
        </div>
      </Section>

      <Section>
        <div id="platform" className="mb-10 max-w-2xl scroll-mt-24">
          <h2 className="text-3xl font-bold tracking-tight md:text-4xl">One platform. Entire payment stack.</h2>
          <p className="mt-3 text-muted">
            From checkout to settlement to fraud review — PayFlow keeps every layer cohesive and observable.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {sections.map(({ icon: Icon, title, body }) => (
            <Card key={title} className="transition hover:border-primary/30 hover:shadow-glow-sm">
              <div className="mb-4 inline-flex rounded-xl bg-primary/15 p-2.5 text-primary-soft">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="text-lg font-semibold">{title}</h3>
              <p className="mt-2 text-sm text-muted">{body}</p>
            </Card>
          ))}
        </div>
      </Section>

      <Section>
        <Card glow className="relative overflow-hidden px-6 py-12 text-center md:px-12">
          <div className="absolute inset-0 bg-gradient-to-br from-primary/20 via-transparent to-accent/10" />
          <div className="relative">
            <h2 className="text-3xl font-bold tracking-tight md:text-4xl">Ready to move money smarter?</h2>
            <p className="mx-auto mt-3 max-w-xl text-muted">
              Spin up a demo merchant in minutes. Explore risk scoring, checkout, and analytics with seeded data.
            </p>
            <div className="mt-8 flex flex-wrap justify-center gap-3">
              <Link to="/register">
                <Button size="lg">
                  Create account <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
              <Link to="/login">
                <Button size="lg" variant="secondary">
                  Sign in to demo
                </Button>
              </Link>
            </div>
            <ul className="mx-auto mt-8 flex max-w-lg flex-col gap-2 text-left text-sm text-muted sm:flex-row sm:justify-center sm:gap-6">
              {['No real money movement', 'Role-based demo accounts', 'Docker-ready stack'].map((t) => (
                <li key={t} className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-success" />
                  {t}
                </li>
              ))}
            </ul>
          </div>
        </Card>
      </Section>

      <footer className="border-t border-border py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 text-sm text-muted md:flex-row md:items-center md:justify-between md:px-6">
          <div className="flex items-center gap-2 text-foreground">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-primary text-xs font-bold">P</div>
            <span className="font-semibold">PayFlow</span>
          </div>
          <p>© {new Date().getFullYear()} PayFlow. Simulated payments — portfolio demo.</p>
          <div className="flex gap-4">
            <Link to="/login" className="hover:text-foreground">
              Login
            </Link>
            <Link to="/register" className="hover:text-foreground">
              Register
            </Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
