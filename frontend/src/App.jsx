import { lazy, Suspense, useEffect, useState } from 'react'
import { Button, CircularProgress, Paper, TextField } from '@mui/material'
import { Brand } from './Shared'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router'
import { api } from './api'

const Management = lazy(() => import('./Management'))

function LoadingSession() {
  return <div className="grid min-h-dvh place-items-center"><CircularProgress aria-label="Carregando sessão" /></div>
}

function AuthForm({ firstAccess, onSignIn, onSignOut, sessionError }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [failure, setFailure] = useState(null)
  const [pending, setPending] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setPending(true)
    setFailure(null)
    const data = Object.fromEntries(new FormData(event.currentTarget))
    if (!firstAccess) data.code = data.code.trim()
    try {
      const signedIn = await api(firstAccess ? '/auth/first-access' : '/auth/login', { method: 'POST', body: data })
      onSignIn(signedIn)
      const from = location.state?.from
      navigate(signedIn.firstAccess ? '/primeiro-acesso' : from || '/', { replace: true, state: { from } })
    } catch (error) {
      setFailure(error)
    } finally {
      setPending(false)
    }
  }

  async function cancelFirstAccess() {
    setPending(true)
    setFailure(null)
    try {
      await api('/logout', { method: 'POST' })
      onSignOut()
      navigate('/login', { replace: true })
    } catch (error) {
      setFailure(error)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="grid min-h-dvh grid-rows-[auto_1fr] bg-neutral-50">
      <header className="border-b border-neutral-200 bg-white px-6 py-4 sm:px-10"><Brand /></header>
      <main className="grid place-items-center px-5 py-10">
        <Paper variant="outlined" className="w-full max-w-[440px] p-6 sm:p-9">
          <h1 className="text-3xl font-bold tracking-tight">{firstAccess ? 'Primeiro acesso' : 'Entrar'}</h1>
          <p className="mt-2 text-neutral-600">{firstAccess ? 'Troque sua senha temporária para acessar o HelpDesker.' : 'Use seu código e sua senha. No primeiro acesso, use a senha temporária recebida.'}</p>
          <form className="mt-7 grid gap-4" onSubmit={submit}>
            {sessionError && !failure && <p role="status" className="rounded-lg border border-neutral-300 bg-neutral-100 p-3 text-sm">{sessionError.message}</p>}
            {failure && <p role="alert" className="rounded-lg border border-neutral-300 bg-neutral-100 p-3 text-sm">{failure.message}</p>}
            {firstAccess ? (
              <>
                <TextField name="newPassword" label="Nova senha" type="password" required autoComplete="new-password"
                  slotProps={{ htmlInput: { minLength: 6, maxLength: 128 } }} helperText={failure?.fields?.newPassword || 'De 6 a 128 caracteres.'} />
                <TextField name="confirmPassword" label="Confirmar senha" type="password" required autoComplete="new-password"
                  slotProps={{ htmlInput: { maxLength: 128 } }} helperText={failure?.fields?.confirmPassword} />
              </>
            ) : <>
              <TextField name="code" label="Código de acesso" required autoComplete="username"
                helperText={failure?.fields?.code} />
              <TextField name="password" label="Senha" type="password" required autoComplete="current-password"
                helperText={failure?.fields?.password} />
            </>}
            <Button type="submit" variant="contained" size="large" disabled={pending}>
              {pending ? 'Aguarde…' : firstAccess ? 'Definir senha' : 'Entrar'}
            </Button>
          </form>
          {firstAccess && <div className="mt-6 border-t border-neutral-200 pt-4 text-center">
            <Button onClick={cancelFirstAccess} disabled={pending}>Sair e voltar ao login</Button>
          </div>}
        </Paper>
      </main>
    </div>
  )
}

export default function App() {
  const [session, setSession] = useState({ loading: true })
  const location = useLocation()

  useEffect(() => {
    const controller = new AbortController()
    api('/me', { signal: controller.signal })
      .then((user) => setSession({ user }))
      .catch((error) => {
        if (error.name !== 'AbortError') setSession(error.status === 401 ? { user: null } : { error })
      })
    return () => controller.abort()
  }, [])

  const signIn = (user) => setSession({ user })
  const signOut = () => setSession({ user: null })
  const canManage = ['ADM', 'IT'].includes(session.user?.role)

  if (session.loading) return <LoadingSession />
  if (session.user?.firstAccess) {
    return <Routes>
      <Route path="/primeiro-acesso" element={<AuthForm key="first-access" firstAccess onSignIn={signIn} onSignOut={signOut} />} />
      <Route path="*" element={<Navigate to="/primeiro-acesso" replace state={{ from: location.state?.from || location.pathname }} />} />
    </Routes>
  }

  return (
    <Routes>
      <Route path="/login" element={session.user ? <Navigate to="/" replace /> : <AuthForm key="login" onSignIn={signIn} sessionError={session.error} />} />
      <Route path="/primeiro-acesso" element={<Navigate to={session.user ? '/' : '/login'} replace />} />
      <Route path="*" element={session.user
        ? <Suspense fallback={<LoadingSession />}><Management user={session.user} canManage={canManage} onSignOut={signOut} /></Suspense>
        : <Navigate to="/login" replace state={{ from: location.pathname }} />} />
    </Routes>
  )
}
