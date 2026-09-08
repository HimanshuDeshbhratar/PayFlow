import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { CreditCard, Landmark, Smartphone, Wallet, ShieldCheck, Lock, CheckCircle2, XCircle, ArrowRight } from 'lucide-react'
import { paymentsApi } from '@/api/payments'
import { Button, Card, Input, Tabs, Select, SkeletonCard, useToast } from '@/components/ui'
import { formatCurrency } from '@/lib/utils'
import { getErrorMessage } from '@/lib/api'
import type { PaymentMethodType } from '@/types'

type Phase = 'form' | 'processing' | 'success' | 'failure'

export function CheckoutPage() {
  const { paymentId = '' } = useParams()
  const { toast } = useToast()
  const [method, setMethod] = useState<PaymentMethodType>('CARD')
  const [phase, setPhase] = useState<Phase>('form')
  const [card, setCard] = useState({ number: '4242 4242 4242 4242', expiry: '12/30', cvv: '123', holder: 'Alex Sharma', save: true })
  const [upi, setUpi] = useState('alex@upi')
  const [bank, setBank] = useState('HDFC')
  const [wallet, setWallet] = useState('Paytm')

  const session = useQuery({
    queryKey: ['checkout', paymentId],
    queryFn: () => paymentsApi.checkout(paymentId),
    enabled: !!paymentId,
    retry: 1,
  })

  const pay = useMutation({
    mutationFn: () =>
      paymentsApi.processCheckout(paymentId, {
        paymentMethodType: method,
        ...(method === 'CARD'
          ? {
              cardNumber: card.number.replace(/\s/g, ''),
              cardExpiry: card.expiry,
              cardCvv: card.cvv,
              cardHolder: card.holder,
              saveMethod: card.save,
            }
          : {}),
        ...(method === 'UPI' ? { upiId: upi } : {}),
        ...(method === 'NET_BANKING' ? { bankCode: bank } : {}),
        ...(method === 'WALLET' ? { walletProvider: wallet } : {}),
      }),
    onMutate: () => setPhase('processing'),
    onSuccess: (res) => {
      const blocked = res.status === 'BLOCKED' || res.status === 'FAILED'
      setPhase(blocked ? 'failure' : 'success')
    },
    onError: (e) => {
      setPhase('failure')
      toast({ type: 'error', title: 'Payment failed', description: getErrorMessage(e) })
    },
  })

  const amountLabel = useMemo(() => {
    const s = session.data
    return s ? formatCurrency(s.amountCents, s.currency) : '—'
  }, [session.data])

  if (session.isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-16">
        <SkeletonCard className="h-96" />
      </div>
    )
  }

  return (
    <div className="relative min-h-screen overflow-hidden px-4 py-10 md:py-16">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_center,_rgba(124,58,237,0.22),_transparent_55%),linear-gradient(180deg,#05050C_0%,#0B0D17_100%)]" />
      <div className="relative z-10 mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1fr_minmax(0,28rem)_1fr]">
        <div className="hidden flex-col justify-center lg:flex">
          <ol className="space-y-4 text-sm">
            {['Payment', 'Review', 'Complete'].map((step, i) => (
              <li key={step} className="flex items-center gap-3">
                <span className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold ${i === 0 ? 'bg-gradient-primary' : 'border border-border text-muted'}`}>
                  {i + 1}
                </span>
                <span className={i === 0 ? 'font-semibold' : 'text-muted'}>{step}</span>
              </li>
            ))}
          </ol>
          <h2 className="mt-10 text-3xl font-bold tracking-tight">Secure payments for a global world</h2>
          <p className="mt-3 text-muted">Test cards only. No real card data is stored or charged.</p>
        </div>

        <Card className="relative overflow-hidden p-6 md:p-8" glow>
          <AnimatePresence mode="wait">
            {phase === 'success' || phase === 'failure' ? (
              <motion.div
                key={phase}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center py-10 text-center"
              >
                {phase === 'success' ? (
                  <CheckCircle2 className="mb-4 h-16 w-16 text-success" />
                ) : (
                  <XCircle className="mb-4 h-16 w-16 text-danger" />
                )}
                <h2 className="text-2xl font-bold">{phase === 'success' ? 'Payment successful' : 'Payment blocked'}</h2>
                <p className="mt-2 text-muted">{amountLabel} · {session.data?.merchantName || 'Merchant'}</p>
                <Button className="mt-8" variant="secondary" onClick={() => setPhase('form')}>
                  Try again
                </Button>
              </motion.div>
            ) : (
              <motion.div key="form" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <div className="mb-6 flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm text-muted">{session.data?.merchantName || 'PayFlow Merchant'}</p>
                    <p className="text-3xl font-bold tracking-tight">{amountLabel}</p>
                    {session.data?.description && <p className="mt-1 text-sm text-muted">{session.data.description}</p>}
                  </div>
                  <div className="rounded-xl bg-gradient-primary px-3 py-2 text-xs font-bold">PayFlow</div>
                </div>

                <Tabs
                  value={method}
                  onChange={(id) => setMethod(id as PaymentMethodType)}
                  tabs={[
                    { id: 'CARD', label: 'Card', icon: <CreditCard className="h-4 w-4" /> },
                    { id: 'UPI', label: 'UPI', icon: <Smartphone className="h-4 w-4" /> },
                    { id: 'NET_BANKING', label: 'NetBanking', icon: <Landmark className="h-4 w-4" /> },
                    { id: 'WALLET', label: 'Wallet', icon: <Wallet className="h-4 w-4" /> },
                  ]}
                />

                <div className="mt-5 space-y-3">
                  {method === 'CARD' && (
                    <>
                      <Input label="Card number" value={card.number} onChange={(e) => setCard({ ...card, number: e.target.value })} hint="Test: 4242 4242 4242 4242" />
                      <div className="grid grid-cols-2 gap-3">
                        <Input label="Expiry" value={card.expiry} onChange={(e) => setCard({ ...card, expiry: e.target.value })} />
                        <Input label="CVV" value={card.cvv} onChange={(e) => setCard({ ...card, cvv: e.target.value })} />
                      </div>
                      <Input label="Cardholder" value={card.holder} onChange={(e) => setCard({ ...card, holder: e.target.value })} />
                      <label className="flex items-center gap-2 text-sm text-muted">
                        <input type="checkbox" checked={card.save} onChange={(e) => setCard({ ...card, save: e.target.checked })} />
                        Save this card for future checkouts (tokenized demo)
                      </label>
                    </>
                  )}
                  {method === 'UPI' && <Input label="UPI ID" value={upi} onChange={(e) => setUpi(e.target.value)} />}
                  {method === 'NET_BANKING' && (
                    <Select
                      label="Bank"
                      value={bank}
                      onChange={(e) => setBank(e.target.value)}
                      options={[
                        { value: 'HDFC', label: 'HDFC Bank' },
                        { value: 'ICICI', label: 'ICICI Bank' },
                        { value: 'SBI', label: 'State Bank of India' },
                      ]}
                    />
                  )}
                  {method === 'WALLET' && (
                    <Select
                      label="Wallet"
                      value={wallet}
                      onChange={(e) => setWallet(e.target.value)}
                      options={[
                        { value: 'Paytm', label: 'Paytm' },
                        { value: 'PhonePe', label: 'PhonePe' },
                        { value: 'AmazonPay', label: 'Amazon Pay' },
                      ]}
                    />
                  )}
                </div>

                <div className="mt-4 flex flex-wrap gap-3 text-xs text-muted">
                  <span className="inline-flex items-center gap-1"><Lock className="h-3.5 w-3.5 text-accent" /> TLS encrypted</span>
                  <span className="inline-flex items-center gap-1"><ShieldCheck className="h-3.5 w-3.5 text-success" /> Fraud screened</span>
                </div>

                <Button className="mt-6 w-full" size="lg" loading={phase === 'processing' || pay.isPending} onClick={() => pay.mutate()} disabled={session.isError}>
                  Pay {amountLabel} <ArrowRight className="h-4 w-4" />
                </Button>
                {session.isError && (
                  <p className="mt-3 text-center text-xs text-danger">Checkout session unavailable. Create a payment from the dashboard first.</p>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </Card>

        <div className="hidden flex-col justify-center gap-4 lg:flex">
          <Card className="p-4 text-sm text-muted">
            “PayFlow checkout feels like a real product — fast, clear, and trustworthy.”
            <p className="mt-2 font-semibold text-foreground">— Demo merchant</p>
          </Card>
        </div>
      </div>
    </div>
  )
}
