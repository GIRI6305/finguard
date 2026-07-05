import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api.js'
import useWebSocket from '../hooks/useWebSocket.js'
import AlertCard from '../components/AlertCard.jsx'
import TransactionsTable from '../components/TransactionsTable.jsx'

export default function Dashboard({ onLogout }) {
  const [alerts, setAlerts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [tab, setTab] = useState('alerts')
  const [form, setForm] = useState({ cardNumber: '4111111111111111', amount: 100, merchant: 'Amazon', location: 'IN' })
  const [errorMsg, setErrorMsg] = useState('')
  const [lastUpdated, setLastUpdated] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [initialLoad, setInitialLoad] = useState(true)
  const liveAlerts = useWebSocket('wss://finguard-6t63.onrender.com/ws/alerts')
  const navigate = useNavigate()

  const username = localStorage.getItem('finguard_username') || ''
  const role = localStorage.getItem('finguard_role')
  const isAdmin = role === 'ADMIN'

  const loadAlerts = async () => {
    try {
      const res = await api.get('/alerts')
      setAlerts(res.data)
      setErrorMsg('')
      setLastUpdated(new Date())
    } catch (err) {
      if (err.code === 'ECONNABORTED' || err.message === 'Network Error') {
        setErrorMsg('Cannot reach the backend at localhost:8080 — is it still running?')
      }
    }
  }

  const loadTransactions = async () => {
    try {
      const res = await api.get('/transactions')
      setTransactions(res.data)
      setErrorMsg('')
      setLastUpdated(new Date())
    } catch (err) {
      if (err.code === 'ECONNABORTED' || err.message === 'Network Error') {
        setErrorMsg('Cannot reach the backend at localhost:8080 — is it still running?')
      }
    }
  }

  // Initial load
  useEffect(() => {
    Promise.all([loadAlerts(), loadTransactions()]).finally(() => setInitialLoad(false))
  }, [])

  // Refresh instantly whenever a live WebSocket alert arrives
  useEffect(() => {
    if (liveAlerts.length > 0) {
      loadAlerts()
      loadTransactions()
    }
  }, [liveAlerts.length])

  // Backup polling every 3 seconds -- guarantees data changes always show up
  // on screen even if the WebSocket connection drops for any reason.
  useEffect(() => {
    const interval = setInterval(() => {
      loadAlerts()
      loadTransactions()
    }, 3000)
    return () => clearInterval(interval)
  }, [])

  const submitTransaction = async (e) => {
    e.preventDefault()
    setErrorMsg('')
    setSubmitting(true)
    try {
      await api.post('/transactions', {
        ...form,
        amount: Number(form.amount)
      })
      setTimeout(() => {
        loadAlerts()
        loadTransactions()
      }, 600)
    } catch (err) {
      const fieldErrors = err.response?.data
      if (fieldErrors && typeof fieldErrors === 'object' && !fieldErrors.error) {
        setErrorMsg(Object.values(fieldErrors).join(' — '))
      } else {
        setErrorMsg(fieldErrors?.error || 'Failed to submit transaction')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const reviewAlert = async (id, status) => {
    const label = status === 'DISMISSED' ? 'dismiss' : 'mark reviewed'
    if (!window.confirm(`Are you sure you want to ${label} this alert?`)) {
      return
    }
    try {
      await api.put(`/alerts/${id}/review`, { status })
      loadAlerts()
    } catch (err) {
      setErrorMsg(err.response?.data?.error || 'Only ADMIN can review alerts.')
    }
  }

  const logout = () => {
    if (!window.confirm('Are you sure you want to log out?')) {
      return
    }
    localStorage.removeItem('finguard_token')
    localStorage.removeItem('finguard_role')
    localStorage.removeItem('finguard_username')
    onLogout()
    navigate('/')
  }

  const openAlerts = alerts.filter((a) => a.status === 'OPEN').length
  const reviewedAlerts = alerts.filter((a) => a.status !== 'OPEN').length
  const successRate = transactions.length === 0
    ? '—'
    : `${Math.round((transactions.filter((t) => t.status === 'APPROVED').length / transactions.length) * 100)}%`

  const statCardStyle = { flex: 1, minWidth: 120, background: '#f8f9fa', border: '1px solid #eee', borderRadius: 6, padding: 12, textAlign: 'center' }

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 850, margin: '40px auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <h1>FinGuard Dashboard</h1>
        <div>
          <span style={{ marginRight: 12, fontSize: 13, color: '#666' }}>
            {username && <>{username} — </>}<strong>{role}</strong>
          </span>
          <button onClick={logout}>Log out</button>
        </div>
      </div>

      {errorMsg && (
        <div style={{ background: '#ffe3e3', border: '1px solid #e03131', padding: 10, borderRadius: 4, marginBottom: 16, fontSize: 13 }}>
          ⚠ {errorMsg}
        </div>
      )}

      {initialLoad ? (
        <p style={{ color: '#888' }}>Loading dashboard...</p>
      ) : (
        <>
          <div style={{ display: 'flex', gap: 10, marginBottom: 20, flexWrap: 'wrap' }}>
            <div style={statCardStyle}>
              <div style={{ fontSize: 22, fontWeight: 'bold' }}>{transactions.length}</div>
              <div style={{ fontSize: 12, color: '#666' }}>Total Transactions</div>
            </div>
            <div style={statCardStyle}>
              <div style={{ fontSize: 22, fontWeight: 'bold' }}>{alerts.length}</div>
              <div style={{ fontSize: 12, color: '#666' }}>Fraud Alerts</div>
            </div>
            <div style={statCardStyle}>
              <div style={{ fontSize: 22, fontWeight: 'bold', color: '#e03131' }}>{openAlerts}</div>
              <div style={{ fontSize: 12, color: '#666' }}>Pending Review</div>
            </div>
            <div style={statCardStyle}>
              <div style={{ fontSize: 22, fontWeight: 'bold', color: '#2f9e44' }}>{reviewedAlerts}</div>
              <div style={{ fontSize: 12, color: '#666' }}>Reviewed</div>
            </div>
            <div style={statCardStyle}>
              <div style={{ fontSize: 22, fontWeight: 'bold' }}>{successRate}</div>
              <div style={{ fontSize: 12, color: '#666' }}>Approval Rate</div>
            </div>
          </div>

          <h3>Simulate a transaction</h3>
          <form onSubmit={submitTransaction} style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
            <input
              value={form.cardNumber}
              onChange={(e) => setForm({ ...form, cardNumber: e.target.value })}
              placeholder="Card number"
            />
            <input
              type="number"
              value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              placeholder="Amount"
            />
            <input
              value={form.merchant}
              onChange={(e) => setForm({ ...form, merchant: e.target.value })}
              placeholder="Merchant"
            />
            <input
              value={form.location}
              onChange={(e) => setForm({ ...form, location: e.target.value })}
              placeholder="Location"
            />
            <button type="submit" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Send transaction'}
            </button>
          </form>

          <p style={{ fontSize: 11, color: '#999', marginTop: 0, marginBottom: 20 }}>
            {lastUpdated ? `Last refreshed: ${lastUpdated.toLocaleTimeString()} (auto-refreshes every 3s)` : 'Loading...'}
          </p>

          <div style={{ display: 'flex', gap: 8, marginBottom: 16, borderBottom: '1px solid #ddd' }}>
            <button
              onClick={() => setTab('alerts')}
              style={{ padding: '8px 16px', fontWeight: tab === 'alerts' ? 'bold' : 'normal', border: 'none', background: 'none', borderBottom: tab === 'alerts' ? '2px solid #333' : 'none' }}
            >
              Fraud Alerts ({alerts.length})
            </button>
            <button
              onClick={() => setTab('transactions')}
              style={{ padding: '8px 16px', fontWeight: tab === 'transactions' ? 'bold' : 'normal', border: 'none', background: 'none', borderBottom: tab === 'transactions' ? '2px solid #333' : 'none' }}
            >
              All Transactions ({transactions.length})
            </button>
          </div>

          {tab === 'alerts' && (
            <div>
              {alerts.length === 0 && (
                <p style={{ color: '#888' }}>No alerts yet. Submit your first transaction above to test the fraud engine.</p>
              )}
              {alerts.map((a) => (
                <AlertCard key={a.id} alert={a} isAdmin={isAdmin} onReview={reviewAlert} />
              ))}
            </div>
          )}

          {tab === 'transactions' && <TransactionsTable transactions={transactions} />}
        </>
      )}
    </div>
  )
}
