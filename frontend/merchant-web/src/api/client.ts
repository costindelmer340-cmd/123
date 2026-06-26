import axios from 'axios'
import { getToken } from '../utils/auth'

export const api = axios.create({
  baseURL: '/api',
  timeout: 8000
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token && token !== 'demo-token') {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function unwrap<T>(response: { data: { data: T } }) {
  return response.data.data
}
