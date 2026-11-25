import { useEffect, useState } from 'react'
import {
  Avatar, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, InputAdornment, MenuItem, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TablePagination, TableRow, TextField,
} from '@mui/material'
import Add from '@mui/icons-material/Add'
import AdminPanelSettingsOutlined from '@mui/icons-material/AdminPanelSettingsOutlined'
import DashboardOutlined from '@mui/icons-material/DashboardOutlined'
import GroupOutlined from '@mui/icons-material/GroupOutlined'
import HourglassEmpty from '@mui/icons-material/HourglassEmpty'
import Logout from '@mui/icons-material/Logout'
import PersonOutlineOutlined from '@mui/icons-material/PersonOutlineOutlined'
import Search from '@mui/icons-material/Search'
import VerifiedUserOutlined from '@mui/icons-material/VerifiedUserOutlined'
import { PieChart } from '@mui/x-charts/PieChart'
import { Link, NavLink, useLocation } from 'react-router'
import { api } from './api'
import { Brand, ErrorNotice } from './Shared'

const roles = {
  ADM: { label: 'Administrador', color: '#7058ce' },
  USER: { label: 'Usuário', color: '#4487d3' },
  IT: { label: 'TI', color: '#169c89' },
}
const number = new Intl.NumberFormat('pt-BR')
const initials = (name) => name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()

function UserAvatar({ name }) {
  return <Avatar aria-hidden="true" className="size-9 bg-neutral-100 text-sm font-semibold text-neutral-700">{initials(name)}</Avatar>
}

function UserTable({ users, compact = false }) {
  return (
    <TableContainer>
      <Table size="small" aria-label={compact ? 'Usuários recentes' : 'Usuários cadastrados'} className="min-w-[550px]">
        <TableHead className="bg-neutral-50">
          <TableRow>
            {['Nome', 'Código', 'Perfil', 'Status'].map((label) => (
              <TableCell key={label} className="px-5 py-3 font-semibold text-neutral-600">{label}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((user) => (
            <TableRow key={user.id || user.code} hover>
              <TableCell className="px-5 py-3">
                <div className="flex items-center gap-3 whitespace-nowrap font-semibold"><UserAvatar name={user.name} />{user.name}</div>
              </TableCell>
              <TableCell className="px-5 text-neutral-600">{user.code}</TableCell>
              <TableCell className="px-5 whitespace-nowrap">{roles[user.role]?.label || user.role}</TableCell>
              <TableCell className="px-5">
                <Chip size="small" variant={user.active ? 'filled' : 'outlined'} label={user.active ? 'Ativo' : 'Inativo'}
                  className="font-medium" />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function Overview({ users }) {
  const active = users.filter((user) => user.active).length
  const privileged = users.filter((user) => ['ADM', 'IT'].includes(user.role)).length
  const pending = users.filter((user) => user.firstAccess).length
  const chart = Object.entries(roles).map(([id, role]) => ({
    id,
    ...role,
    value: users.filter((user) => user.role === id).length,
  }))
  const metrics = [
    ['Usuários cadastrados', users.length, GroupOutlined],
    ['Usuários ativos', active, VerifiedUserOutlined],
    ['Perfis de gestão', privileged, AdminPanelSettingsOutlined],
    ['Primeiro acesso pendente', pending, HourglassEmpty],
  ]
  // O código de usuário é sequencial no backend.
  const recent = [...users].sort((a, b) => b.code.localeCompare(a.code, 'pt-BR', { numeric: true })).slice(0, 5)

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metrics.map(([label, value, Icon]) => (
          <Paper key={label} variant="outlined" className="p-5">
            <span className="mb-4 grid size-9 place-items-center rounded-lg border border-neutral-200 bg-neutral-50 text-neutral-700">
              <Icon fontSize="small" />
            </span>
            <div className="text-sm font-medium text-neutral-600">{label}</div>
            <div className="mt-1 text-3xl font-semibold tracking-tight">{number.format(value)}</div>
          </Paper>
        ))}
      </div>
      <div className="mt-5 grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.75fr)]">
        <Paper variant="outlined" className="min-w-0">
          <div className="border-b border-neutral-200 px-5 py-4">
            <h2 className="text-lg font-semibold tracking-tight">Usuários por perfil</h2>
            <p className="mt-1 text-sm text-neutral-600">Distribuição dos acessos cadastrados</p>
          </div>
          {users.length ? (
            <div className="p-5">
              <div className="relative mx-auto h-[210px] max-w-[320px]">
                <PieChart
                  aria-label="Distribuição dos usuários por perfil"
                  series={[{ data: chart.filter((item) => item.value > 0), innerRadius: '71%', outerRadius: '96%', paddingAngle: 3, cornerRadius: 4 }]}
                  height={210}
                  hideLegend
                  margin={0}
                />
                <div aria-hidden="true" className="pointer-events-none absolute inset-0 grid place-content-center text-center">
                  <span className="text-3xl font-semibold tracking-tight">{number.format(users.length)}</span>
                  <span className="text-sm text-neutral-600">Total</span>
                </div>
              </div>
              <ul className="mt-5 grid gap-2.5">
                {chart.map(({ id, label, color, value }) => (
                  <li key={id} className="flex items-center gap-2 text-sm">
                    <span className="size-2.5 rounded-full" style={{ backgroundColor: color }} />
                    <span className="grow text-neutral-700">{label}</span>
                    <span className="font-semibold tabular-nums">{number.format(value)}</span>
                  </li>
                ))}
              </ul>
            </div>
          ) : <p className="px-5 py-12 text-center text-sm text-neutral-600">A distribuição aparece após o primeiro cadastro.</p>}
        </Paper>
        <Paper variant="outlined" className="min-w-0 overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-4">
            <div>
              <h2 className="text-lg font-semibold tracking-tight">Usuários recentes</h2>
              <p className="mt-1 text-sm text-neutral-600">Últimos cadastros no sistema</p>
            </div>
            <Button component={Link} to="/usuarios" variant="outlined" size="small">Ver todos</Button>
          </div>
          {recent.length ? <UserTable users={recent} compact /> : <p className="px-5 py-12 text-center text-sm text-neutral-600">Nenhum usuário cadastrado.</p>}
        </Paper>
      </div>
    </>
  )
}

function Users({ users }) {
  const [query, setQuery] = useState('')
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const search = query.trim().toLocaleLowerCase('pt-BR')
  const filtered = users.filter((user) => (!role || user.role === role)
    && [user.name, user.code].some((value) => value.toLocaleLowerCase('pt-BR').includes(search)))
  const currentPage = Math.min(page, Math.max(0, Math.ceil(filtered.length / pageSize) - 1))

  return (
    <Paper variant="outlined" className="min-w-0 overflow-hidden">
      <div className="grid gap-3 border-b border-neutral-200 p-4 sm:grid-cols-[minmax(0,360px)_180px]">
        <TextField size="small" label="Buscar por nome ou código" value={query}
          onChange={(event) => { setQuery(event.target.value); setPage(0) }}
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><Search fontSize="small" /></InputAdornment> } }} />
        <TextField size="small" select label="Perfil" value={role}
          onChange={(event) => { setRole(event.target.value); setPage(0) }}
          slotProps={{ inputLabel: { shrink: true }, select: { displayEmpty: true } }}>
          <MenuItem value="">Todos os perfis</MenuItem>
          {Object.entries(roles).map(([id, { label }]) => <MenuItem key={id} value={id}>{label}</MenuItem>)}
        </TextField>
      </div>
      {filtered.length ? <UserTable users={filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize)} />
        : <p role="status" className="px-5 py-12 text-center text-neutral-600">Nenhum usuário encontrado.</p>}
      <TablePagination component="div" count={filtered.length} page={currentPage} rowsPerPage={pageSize}
        rowsPerPageOptions={[10, 25, 50]} onPageChange={(_, nextPage) => setPage(nextPage)}
        onRowsPerPageChange={(event) => { setPageSize(Number(event.target.value)); setPage(0) }}
        labelRowsPerPage="Linhas por página" labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
        getItemAriaLabel={(type) => type === 'next' ? 'Próxima página' : 'Página anterior'} />
    </Paper>
  )
}

function CreateUserDialog({ onClose, onCreated, onSignOut, currentRole }) {
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState(null)
  const [created, setCreated] = useState(null)

  async function submit(event) {
    event.preventDefault()
    const data = Object.fromEntries(new FormData(event.currentTarget))
    data.name = data.name.trim()
    if (!data.name) {
      setFailure({ message: 'Informe o nome do usuário.' })
      return
    }
    setPending(true)
    setFailure(null)
    try {
      const newUser = await api('/users', { method: 'POST', body: data })
      onCreated(newUser)
      setCreated(newUser)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setFailure(error)
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog open onClose={pending ? undefined : onClose} fullWidth maxWidth="xs"
      aria-labelledby="create-user-title">
      <DialogTitle id="create-user-title">{created ? 'Usuário cadastrado' : 'Novo usuário'}</DialogTitle>
      {created ? (
        <>
          <DialogContent dividers>
            <p className="text-neutral-700">Compartilhe este código com {created.name} para o primeiro acesso.</p>
            <p className="mt-4 rounded-lg bg-neutral-100 px-4 py-3 text-center font-mono text-xl font-semibold">{created.code}</p>
          </DialogContent>
          <DialogActions><Button variant="contained" onClick={onClose}>Concluir</Button></DialogActions>
        </>
      ) : (
        <form onSubmit={submit}>
          <DialogContent dividers className="grid gap-4">
            {failure && <p role="alert" className="rounded-lg bg-neutral-100 p-3 text-sm">{failure.message}</p>}
            <TextField autoFocus required label="Nome completo" name="name" helperText={failure?.fields?.name} />
            <TextField select required label="Perfil" name="role" defaultValue="USER">
              {Object.entries(roles).filter(([id]) => id !== 'ADM' || currentRole === 'ADM')
                .map(([id, { label }]) => <MenuItem key={id} value={id}>{label}</MenuItem>)}
            </TextField>
          </DialogContent>
          <DialogActions>
            <Button onClick={onClose} disabled={pending}>Cancelar</Button>
            <Button type="submit" variant="contained" disabled={pending}>{pending ? 'Salvando…' : 'Cadastrar'}</Button>
          </DialogActions>
        </form>
      )}
    </Dialog>
  )
}

export default function Management({ user, canManage, onSignOut }) {
  const { pathname } = useLocation()
  const route = pathname.replace(/\/$/, '') || '/'
  const [result, setResult] = useState({})
  const [attempt, setAttempt] = useState(0)
  const [creating, setCreating] = useState(false)
  const [leaving, setLeaving] = useState(false)
  const [logoutError, setLogoutError] = useState(null)
  const showsUsers = route === '/usuarios'
  const showsData = canManage && (route === '/' || showsUsers)

  useEffect(() => {
    if (!canManage) return
    const controller = new AbortController()
    api('/users', { signal: controller.signal })
      .then((users) => setResult({ users }))
      .catch((error) => {
        if (error.name !== 'AbortError') setResult({ error })
      })
    return () => controller.abort()
  }, [canManage, attempt])

  useEffect(() => {
    if (result.error?.status === 401) onSignOut()
  }, [result.error, onSignOut])

  function retry() {
    setResult({})
    setAttempt(attempt + 1)
  }

  async function signOut() {
    setLeaving(true)
    setLogoutError(null)
    try {
      await api('/logout', { method: 'POST' })
      onSignOut()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLogoutError(error.message)
    } finally {
      setLeaving(false)
    }
  }

  const title = showsUsers ? 'Usuários' : canManage ? 'Visão geral' : 'Minha conta'
  const nav = [
    { to: '/', label: canManage ? 'Visão geral' : 'Minha conta', Icon: canManage ? DashboardOutlined : PersonOutlineOutlined },
    ...(canManage ? [{ to: '/usuarios', label: 'Usuários', Icon: GroupOutlined }] : []),
  ]
  const errorStatus = { '/403': 403, '/500': 500 }[route] || (showsUsers && !canManage ? 403 : 404)

  return (
    <div className="min-h-dvh md:grid md:grid-cols-[240px_minmax(0,1fr)]">
      <aside className="flex flex-col border-b border-neutral-200 bg-white md:sticky md:top-0 md:h-dvh md:border-r md:border-b-0">
        <Link to="/" className="px-5 py-5 md:px-6"><Brand /></Link>
        <nav aria-label="Navegação principal" className="flex gap-1 px-4 pb-3 md:mt-7 md:flex-col">
          {canManage && <div className="mb-1 hidden px-3 text-xs font-semibold tracking-wider text-neutral-500 uppercase md:block">Gestão</div>}
          {nav.map(({ to, label, Icon }) => (
            <NavLink key={to} to={to} end className={({ isActive }) =>
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-semibold ' +
              (isActive ? 'bg-neutral-100 text-neutral-950' : 'text-neutral-600 hover:bg-neutral-50')}>
              <Icon fontSize="small" />{label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto hidden items-center gap-3 border-t border-neutral-200 px-5 py-4 md:flex">
          <UserAvatar name={user.name} />
          <div className="min-w-0 grow">
            <p className="truncate text-sm font-semibold">{user.name}</p>
            <p className="text-xs text-neutral-600">{roles[user.role]?.label || user.role}</p>
          </div>
        </div>
      </aside>
      <div className="min-w-0">
        <header className="flex min-h-[73px] items-center justify-between gap-3 border-b border-neutral-200 bg-white px-5 sm:px-8">
          <div className="flex items-center gap-2 text-sm">
            <span className="text-neutral-600">{canManage ? 'Gestão' : 'HelpDesker'}</span>
            {(showsData || route === '/') && <><span aria-hidden="true" className="text-neutral-400">/</span><span className="font-semibold">{title}</span></>}
          </div>
          <Button size="small" color="inherit" startIcon={<Logout />} onClick={signOut} disabled={leaving}>Sair</Button>
        </header>
        <main className="mx-auto max-w-[1500px] px-5 py-7 sm:px-8 sm:py-9">
          {logoutError && <p role="alert" className="mb-5 rounded-lg border border-neutral-300 bg-white p-3">{logoutError}</p>}
          {showsData ? (
            <>
              <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
                <div>
                  <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{title}</h1>
                  <p className="mt-2 text-neutral-600">{showsUsers ? 'Consulte e cadastre usuários do HelpDesker.' : 'Acompanhe os usuários e perfis de acesso do HelpDesker.'}</p>
                </div>
                {result.users && <Button variant="contained" startIcon={<Add />} onClick={() => setCreating(true)}>Novo usuário</Button>}
              </div>
              {result.error ? <ErrorNotice status={result.error.status} onRetry={retry} />
                : result.users ? showsUsers ? <Users users={result.users} /> : <Overview users={result.users} />
                  : <div className="grid min-h-44 place-items-center"><CircularProgress aria-label="Carregando usuários" /></div>}
            </>
          ) : route === '/' ? (
            <Paper variant="outlined" className="max-w-xl p-6">
              <PersonOutlineOutlined />
              <h1 className="mt-3 text-2xl font-bold tracking-tight">Minha conta</h1>
              <dl className="mt-5 grid grid-cols-[auto_1fr] gap-x-5 gap-y-3">
                <dt className="text-neutral-600">Nome</dt><dd>{user.name}</dd>
                <dt className="text-neutral-600">Código</dt><dd>{user.code}</dd>
                <dt className="text-neutral-600">Perfil</dt><dd>{roles[user.role]?.label || user.role}</dd>
              </dl>
            </Paper>
          ) : <ErrorNotice status={errorStatus} />}
        </main>
      </div>
      {creating && <CreateUserDialog currentRole={user.role} onClose={() => setCreating(false)} onSignOut={onSignOut}
        onCreated={(newUser) => setResult(({ users }) => ({ users: [...users, newUser] }))} />}
    </div>
  )
}
