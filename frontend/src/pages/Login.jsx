import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api.js'

export default function Login({ onLoginSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.post('/auth/login', { username, password })
      localStorage.setItem('finguard_token', res.data.token)
      localStorage.setItem('finguard_role', res.data.role)
      localStorage.setItem('finguard_username', res.data.username)
      onLoginSuccess()
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid username or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h2>Log in to FinGuard</h2>
      <form onSubmit={handleSubmit}>
        <input
          style={{ display: 'block', width: '100%', marginBottom: 10, padding: 8 }}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
        />
        <input
          style={{ display: 'block', width: '100%', marginBottom: 10, padding: 8 }}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
        />
        <button style={{ padding: '8px 16px' }} type="submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Log in'}
        </button>
      </form>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <p style={{ fontSize: 13, marginTop: 16 }}>
        Don't have an account? <Link to="/signup">Sign up</Link>
      </p>
      <p style={{ fontSize: 12, color: '#666' }}>
        Seeded demo accounts: admin/admin123 (ADMIN), analyst/analyst123 (ANALYST) —
        or create your own via Sign up.
      </p>
    </div>
  )
}
