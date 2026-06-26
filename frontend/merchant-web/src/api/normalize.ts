import { isDemoMode } from '../utils/auth'

export function listFromApi<T>(value: unknown, fallback: T[]): T[] {
  if (Array.isArray(value)) {
    return value as T[]
  }
  if (value && typeof value === 'object' && Array.isArray((value as { records?: unknown }).records)) {
    return (value as { records: T[] }).records
  }
  return fallback
}

export async function loadListWithFallback<T>(
  loader: () => Promise<unknown>,
  fallback: T[]
): Promise<T[]> {
  if (isDemoMode()) {
    return fallback
  }
  try {
    return listFromApi(await loader(), fallback)
  } catch {
    return fallback
  }
}
