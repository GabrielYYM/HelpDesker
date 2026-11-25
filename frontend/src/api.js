const configuredApiUrl = import.meta.env.VITE_API_URL?.replace(/\/+$/, '')
const apiBaseUrl = configuredApiUrl
  ? (configuredApiUrl.endsWith('/api') ? configuredApiUrl : `${configuredApiUrl}/api`)
  : '/api'
const isCrossOriginApi = /^https?:\/\//i.test(apiBaseUrl)

export async function api(path, { body, method = 'GET', signal } = {}) {
  const headers = { Accept: 'application/json' }

  if (method !== 'GET') {
    const csrf = await api('/auth/csrf', { signal })
    headers[csrf.headerName] = csrf.token
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response
  try {
    response = await fetch(apiBaseUrl + path, {
      credentials: isCrossOriginApi ? 'include' : 'same-origin',
      headers,
      method,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError(503, 'Não foi possível conectar ao servidor.')
  }

  const isJson = response.headers.get('content-type')?.includes('application/json')
  if ([502, 503, 504].includes(response.status) || (response.ok && response.status !== 204 && !isJson)) {
    throw new ApiError(503, 'Serviço indisponível no momento. Tente novamente em instantes.')
  }
  const data = isJson ? await response.json() : null
  if (!response.ok) {
    throw new ApiError(response.status, data?.message || 'Não foi possível concluir a solicitação.', data?.fields)
  }
  return data
}

class ApiError extends Error {
  constructor(status, message, fields) {
    super(message)
    this.status = status
    this.fields = fields
  }
}
