import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api.js'

const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).+$/

export default function Signup({ onSignupSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (username.trim().length < 3) {
      setError('Username must be at least 3 characters')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }
    if (!PASSWORD_RULE.test(password)) {
      setError('Password must include an uppercase letter, a lowercase letter, a number, and a special character')
      return
    }

    setLoading(true)
    try {
      const res = await api.post('/auth/register', { username, password })
      localStorage.setItem('finguard_token', res.data.token)
      localStorage.setItem('finguard_role', res.data.role)
      localStorage.setItem('finguard_username', res.data.username)
      onSignupSuccess()
      navigate('/')
    } catch (err) {
      if (err.response?.status === 409) {
        setError(`Username "${username}" is already taken — try a different one.`)
      } else if (err.response?.data?.error) {
        setError(err.response.data.error)
      } else if (err.response?.data && typeof err.response.data === 'object') {
        setError(Object.values(err.response.data).join(' — '))
      } else {
        setError('Could not create account. Check that the backend is running.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 380, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h2>Create your FinGuard account</h2>
      <form onSubmit={handleSubmit}>
        <input
          style={{ display: 'block', width: '100%', marginBottom: 10, padding: 8 }}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Choose a username"
        />
        <input
          style={{ display: 'block', width: '100%', marginBottom: 6, padding: 8 }}
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Choose a password"
        />
        <p style={{ fontSize: 11, color: '#888', marginTop: 0, marginBottom: 10 }}>
          Min 8 characters, with uppercase, lowercase, a number, and a special character (e.g. Fraud@2026)
        </p>
        <button style={{ padding: '8px 16px' }} type="submit" disabled={loading}>
          {loading ? 'Creating account...' : 'Sign up'}
        </button>
      </form>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <p style={{ fontSize: 13, marginTop: 16 }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
      <p style={{ fontSize: 12, color: '#888' }}>
        New accounts are created with ANALYST access. The seeded admin/admin123 account
        remains the only ADMIN user.
      </p>
    </div>
  )
}
