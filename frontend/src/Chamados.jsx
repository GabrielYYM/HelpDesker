import { useEffect, useState } from 'react'
import {
  Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  MenuItem, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField,
} from '@mui/material'
import Add from '@mui/icons-material/Add'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { api } from './api'
import { ErrorNotice } from './Shared'

const statusLabels = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  RESOLVIDO: 'Resolvido',
  CANCELADO: 'Cancelado',
}

const eventLabels = {
  ABERTURA: 'Abriu o chamado',
  EDICAO: 'Editou o chamado',
  COMENTARIO: 'Adicionou um comentário',
  INICIO_ATENDIMENTO: 'Assumiu o atendimento',
  RESOLUCAO: 'Resolveu o chamado',
  CANCELAMENTO: 'Cancelou o chamado',
}

const actionDetails = {
  ABERTURA: { title: 'Novo chamado', button: 'Abrir chamado' },
  EDICAO: { title: 'Editar chamado', button: 'Salvar alterações' },
  INICIO_ATENDIMENTO: { title: 'Assumir atendimento', button: 'Assumir', status: 'EM_ANDAMENTO', label: 'Como você vai iniciar o atendimento?' },
  RESOLUCAO: { title: 'Resolver chamado', button: 'Resolver', status: 'RESOLVIDO', label: 'O que resolveu o problema?' },
  CANCELAMENTO: { title: 'Cancelar chamado', button: 'Confirmar cancelamento', status: 'CANCELADO', label: 'Motivo do cancelamento' },
  COMENTARIO: { title: 'Adicionar comentário', button: 'Publicar comentário', label: 'Comentário' },
}

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

function Status({ value }) {
  return <Chip size="small" variant={value === 'EM_ANDAMENTO' ? 'filled' : 'outlined'} label={statusLabels[value]} />
}

function useChamadoRequest(path, revision, onSignOut) {
  const [result, setResult] = useState({})
  const requestKey = `${path}:${revision}`

  useEffect(() => {
    const controller = new AbortController()
    api(path, { signal: controller.signal })
      .then((data) => setResult({ requestKey, data }))
      .catch((error) => {
        if (error.name === 'AbortError') return
        if (error.status === 401) onSignOut()
        else setResult({ requestKey, error })
      })
    return () => controller.abort()
  }, [path, requestKey, onSignOut])

  return result.requestKey === requestKey ? result : {}
}

function RequestState({ error, onRetry }) {
  if (error) return <ErrorNotice status={error.status} onRetry={error.status >= 500 ? onRetry : undefined} />
  return <div className="grid min-h-44 place-items-center"><CircularProgress aria-label="Carregando chamados" /></div>
}

function ActionDialog({ action, ticket, onClose, onSaved, onSignOut, onReload }) {
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState(null)
  const { title, button, status, label } = actionDetails[action]
  const writesDetails = action === 'ABERTURA' || action === 'EDICAO'

  async function submit(event) {
    event.preventDefault()
    const fields = Object.fromEntries(Array.from(new FormData(event.currentTarget), ([name, value]) => [name, value.trim()]))
    if (Object.values(fields).some((value) => !value)) {
      setFailure({ message: 'Preencha todos os campos.' })
      return
    }

    const path = ticket ? '/chamados/' + ticket.resumo.protocolo : '/chamados'
    let saved
    setPending(true)
    setFailure(null)
    try {
      if (action === 'ABERTURA') {
        saved = await api(path, { method: 'POST', body: fields })
      } else if (action === 'EDICAO') {
        saved = await api(path, { method: 'PATCH', body: { ...fields, versao: ticket.versao } })
      } else if (status) {
        saved = await api(path + '/transicoes', { method: 'POST', body: { ...fields, status } })
      } else {
        saved = await api(path + '/comentarios', { method: 'POST', body: fields })
      }
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setFailure(error)
    } finally {
      setPending(false)
    }
    if (saved) onSaved(saved)
  }

  return (
    <Dialog open fullWidth maxWidth="sm" onClose={pending ? undefined : onClose} aria-labelledby="ticket-action-title">
      <DialogTitle id="ticket-action-title">{title}</DialogTitle>
      <form onSubmit={submit}>
        <DialogContent dividers className="grid gap-4">
          {failure && <p role="alert" className="rounded-lg bg-neutral-100 p-3 text-sm">{failure.message}</p>}
          {writesDetails ? <>
            <TextField autoFocus required label="Título" name="titulo" defaultValue={ticket?.resumo.titulo}
              slotProps={{ htmlInput: { maxLength: 120 } }} helperText={failure?.fields?.titulo} />
            <TextField required multiline minRows={4} label="Descrição" name="descricao" defaultValue={ticket?.descricao}
              slotProps={{ htmlInput: { maxLength: 4000 } }} helperText={failure?.fields?.descricao} />
          </> : <TextField autoFocus required multiline minRows={4} label={label} name="comentario"
            slotProps={{ htmlInput: { maxLength: 4000 } }} helperText={failure?.fields?.comentario} />}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={pending}>Fechar</Button>
          {failure?.status === 409 && onReload
            ? <Button onClick={onReload} variant="contained">Recarregar chamado</Button>
            : <Button type="submit" variant="contained" disabled={pending}>{pending ? 'Salvando…' : button}</Button>}
        </DialogActions>
      </form>
    </Dialog>
  )
}

function ListaChamados({ canManage, onSignOut }) {
  const [params, setParams] = useSearchParams()
  const status = Object.hasOwn(statusLabels, params.get('status')) ? params.get('status') : ''
  const rawPage = Number(params.get('pagina'))
  const pagina = Number.isInteger(rawPage) && rawPage > 0 && rawPage <= 2147483647 ? rawPage : 0
  const [creating, setCreating] = useState(false)
  const [revision, setRevision] = useState(0)
  const navigate = useNavigate()
  const path = '/chamados?' + new URLSearchParams({ pagina, ...(status && { status }) })
  const { data, error } = useChamadoRequest(path, revision, onSignOut)

  function filtrar(nextStatus, nextPage = 0) {
    setParams({ ...(nextStatus && { status: nextStatus }), ...(nextPage > 0 && { pagina: nextPage }) })
  }

  return (
    <>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Chamados</h1>
          <p className="mt-2 text-neutral-600">{canManage ? 'Acompanhe a fila e assuma chamados abertos.' : 'Solicite suporte e acompanhe seus chamados.'}</p>
        </div>
        <Button variant="contained" startIcon={<Add />} onClick={() => setCreating(true)}>Novo chamado</Button>
      </div>
      <Paper variant="outlined" className="overflow-hidden">
        <div className="border-b border-neutral-200 p-4">
          <TextField select size="small" label="Status" value={status} onChange={(event) => filtrar(event.target.value)}
            className="w-full sm:w-56" slotProps={{ inputLabel: { shrink: true }, select: { displayEmpty: true } }}>
            <MenuItem value="">Todos os estados</MenuItem>
            {Object.entries(statusLabels).map(([id, label]) => <MenuItem key={id} value={id}>{label}</MenuItem>)}
          </TextField>
        </div>
        {data ? <>
          {data.chamados.length ? (
            <TableContainer>
              <Table size="small" aria-label="Chamados" className="min-w-[660px]">
                <TableHead className="bg-neutral-50">
                  <TableRow>{['Chamado', 'Status', 'Solicitante', 'Responsável', 'Última atualização'].map((label) => (
                    <TableCell key={label} className="px-5 py-3 font-semibold">{label}</TableCell>
                  ))}</TableRow>
                </TableHead>
                <TableBody>{data.chamados.map((ticket) => (
                  <TableRow key={ticket.protocolo} hover>
                    <TableCell className="px-5 py-3">
                      <Link className="block font-semibold underline-offset-4 hover:underline" to={'/chamados/' + ticket.protocolo}>{ticket.titulo}</Link>
                      <span className="mt-1 block font-mono text-xs text-neutral-600">{ticket.protocolo}</span>
                    </TableCell>
                    <TableCell className="whitespace-nowrap"><Status value={ticket.status} /></TableCell>
                    <TableCell>{ticket.solicitante.nome}</TableCell>
                    <TableCell>{ticket.tecnico?.nome || 'Sem responsável'}</TableCell>
                    <TableCell className="whitespace-nowrap">{dateFormat.format(new Date(ticket.atualizadoEm))}</TableCell>
                  </TableRow>
                ))}</TableBody>
              </Table>
            </TableContainer>
          ) : <p role="status" className="px-5 py-10 text-center text-neutral-600">Nenhum chamado encontrado.</p>}
          {(pagina > 0 || data.temProximaPagina) && (
            <div className="flex items-center justify-end gap-3 border-t border-neutral-200 p-3">
              <Button disabled={pagina === 0} onClick={() => filtrar(status, pagina - 1)}>Anterior</Button>
              <span className="text-sm text-neutral-600">Página {pagina + 1}</span>
              <Button disabled={!data.temProximaPagina} onClick={() => filtrar(status, pagina + 1)}>Próxima</Button>
            </div>
          )}
        </> : <RequestState error={error} onRetry={() => setRevision(revision + 1)} />}
      </Paper>
      {creating && <ActionDialog action="ABERTURA" onSignOut={onSignOut} onClose={() => setCreating(false)}
        onSaved={(saved) => navigate('/chamados/' + saved.resumo.protocolo)} />}
    </>
  )
}

function TicketHistory({ events }) {
  return (
    <Paper variant="outlined" className="min-w-0">
      <h2 className="border-b border-neutral-200 px-5 py-4 text-lg font-semibold">Histórico e comentários</h2>
      <ol className="divide-y divide-neutral-200 px-5">
        {[...events].reverse().map((event, index) => (
          <li key={events.length - index} className="py-4">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
              <p className="text-sm"><strong>{event.autor.nome}</strong> · {eventLabels[event.acao]}</p>
              <time dateTime={event.data} className="text-xs text-neutral-600">{dateFormat.format(new Date(event.data))}</time>
            </div>
            <p className="my-2 text-sm text-neutral-600">
              {event.statusAnterior && event.statusAnterior !== event.status
                ? `${statusLabels[event.statusAnterior]} → ${statusLabels[event.status]}`
                : statusLabels[event.status]}
            </p>
            {event.comentario && <p className="wrap-break-word whitespace-pre-wrap rounded-lg bg-neutral-50 p-3">{event.comentario}</p>}
            {event.alteracoes.length > 0 && <ul className="mt-2 grid gap-2">
              {event.alteracoes.map(({ campo, anterior, atual }) => (
                <li key={campo} className="wrap-break-word rounded-lg border border-neutral-200 p-3 text-sm">
                  <strong>{campo}</strong>
                  <p className="mt-1 whitespace-pre-wrap text-neutral-600">Antes: {anterior}</p>
                  <p className="mt-1 whitespace-pre-wrap">Depois: {atual}</p>
                </li>
              ))}
            </ul>}
          </li>
        ))}
      </ol>
    </Paper>
  )
}

function TicketDetails({ protocolo, canManage, onSignOut }) {
  const [revision, setRevision] = useState(0)
  const [action, setAction] = useState(null)
  const [updated, setUpdated] = useState(null)
  const { data, error } = useChamadoRequest('/chamados/' + encodeURIComponent(protocolo), revision, onSignOut)
  const ticket = updated || data

  function reload() {
    setAction(null)
    setUpdated(null)
    setRevision(revision + 1)
  }

  if (!ticket) return <RequestState error={error} onRetry={reload} />

  const { resumo, descricao, historico, acoes } = ticket
  const primary = acoes.includes('INICIO_ATENDIMENTO') ? 'INICIO_ATENDIMENTO' : acoes.includes('RESOLUCAO') ? 'RESOLUCAO' : null

  return (
    <>
      <Button component={Link} to="/chamados" size="small" className="mb-3">Voltar para chamados</Button>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div className="min-w-0">
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm text-neutral-600">{resumo.protocolo}</span>
            <Status value={resumo.status} />
          </div>
          <h1 className="wrap-break-word text-3xl font-bold tracking-tight sm:text-4xl">{resumo.titulo}</h1>
        </div>
        {primary && <Button variant="contained" onClick={() => setAction(primary)}>{actionDetails[primary].title}</Button>}
      </div>
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.35fr)]">
        <div className="grid min-w-0 gap-5">
          <Paper variant="outlined" className="p-5">
            <h2 className="text-lg font-semibold">Detalhes</h2>
            <p className="mt-3 wrap-break-word whitespace-pre-wrap">{descricao}</p>
            <dl className="mt-5 grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-2 border-t border-neutral-200 pt-4 text-sm">
              <dt className="text-neutral-600">Solicitante</dt><dd className="wrap-break-word">{resumo.solicitante.nome}</dd>
              <dt className="text-neutral-600">Responsável</dt><dd className="wrap-break-word">{resumo.tecnico?.nome || 'Sem responsável'}</dd>
              <dt className="text-neutral-600">Abertura</dt><dd>{dateFormat.format(new Date(resumo.criadoEm))}</dd>
            </dl>
            {acoes.some((id) => ['EDICAO', 'CANCELAMENTO', 'COMENTARIO'].includes(id)) && (
              <div className="mt-5 flex flex-wrap gap-2 border-t border-neutral-200 pt-4">
                {['EDICAO', 'COMENTARIO', 'CANCELAMENTO'].filter((id) => acoes.includes(id)).map((id) => (
                  <Button key={id} variant="outlined" size="small" onClick={() => setAction(id)}>{actionDetails[id].title}</Button>
                ))}
              </div>
            )}
          </Paper>
          {canManage && ['ABERTO', 'EM_ANDAMENTO'].includes(resumo.status) && (
            <Paper variant="outlined" className="p-5">
              <h2 className="text-lg font-semibold">Fluxo de atendimento</h2>
              <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm text-neutral-700">
                <li>Assuma o chamado e descreva como vai iniciar o atendimento.</li>
                <li>Registre o diagnóstico e as atualizações em comentários.</li>
                <li>Resolva o chamado com um resumo da solução ou informe o motivo do cancelamento.</li>
              </ol>
            </Paper>
          )}
        </div>
        <TicketHistory events={historico} />
      </div>
      {action && <ActionDialog action={action} ticket={ticket} onSignOut={onSignOut} onClose={() => setAction(null)}
        onReload={reload} onSaved={(saved) => { setUpdated(saved); setAction(null) }} />}
    </>
  )
}

export default function Chamados({ protocolo, canManage, onSignOut }) {
  return protocolo
    ? <TicketDetails key={protocolo} protocolo={protocolo} canManage={canManage} onSignOut={onSignOut} />
    : <ListaChamados canManage={canManage} onSignOut={onSignOut} />
}
