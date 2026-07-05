import React, { useState } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Welcome from './pages/Welcome.jsx'
import Login from './pages/Login.jsx'
import Signup from './pages/Signup.jsx'
import Dashboard from './pages/Dashboard.jsx'

export default function App() {
  // Real React state, not a plain function call read at render time.
  // This guarantees the UI actually re-renders the instant login/logout happens,
  // instead of depending on React Router deciding to re-render App on its own.
  const [loggedIn, setLoggedIn] = useState(!!localStorage.getItem('finguard_token'))

  return (
    <Routes>
      <Route
        path="/"
        element={
          loggedIn ? <Dashboard onLogout={() => setLoggedIn(false)} /> : <Welcome />
        }
      />
      <Route
        path="/login"
        element={
          loggedIn ? (
            <Navigate to="/" replace />
          ) : (
            <Login onLoginSuccess={() => setLoggedIn(true)} />
          )
        }
      />
      <Route
        path="/signup"
        element={
          loggedIn ? (
            <Navigate to="/" replace />
          ) : (
            <Signup onSignupSuccess={() => setLoggedIn(true)} />
          )
        }
      />
    </Routes>
  )
}
