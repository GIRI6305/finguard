import axios from 'axios'

const api = axios.create({
  baseURL: 'https://finguard-6t63.onrender.com/api',
  timeout: 8000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('finguard_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// If the token is missing/expired/invalid, the backend returns 401/403.
// Instead of failing silently forever, clear the stale session and force
// a clean re-login -- this is what prevents the UI from looking "stuck".
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('finguard_token')
      localStorage.removeItem('finguard_role')
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

export default api
