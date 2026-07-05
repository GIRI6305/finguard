import React from 'react'
import { useNavigate } from 'react-router-dom'

export default function Welcome() {
  const navigate = useNavigate()

  return (
    <div style={{ maxWidth: 380, margin: '100px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
      <h1 style={{ marginBottom: 4 }}>FinGuard</h1>
      <p style={{ color: '#666', marginBottom: 32 }}>Real-time fraud detection & risk engine</p>

      <button
        onClick={() => navigate('/login')}
        style={{ width: '100%', padding: 12, marginBottom: 10, background: '#1864ab', color: '#fff', border: 'none', borderRadius: 4, fontSize: 15, cursor: 'pointer' }}
      >
        Log In
      </button>
      <button
        onClick={() => navigate('/signup')}
        style={{ width: '100%', padding: 12, background: '#e9ecef', color: '#333', border: 'none', borderRadius: 4, fontSize: 15, cursor: 'pointer' }}
      >
        Create New Account
      </button>
    </div>
  )
}
