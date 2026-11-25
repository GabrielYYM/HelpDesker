import { useEffect, useState } from 'react'
import { Button, Chip, CircularProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material'
import AssignmentOutlined from '@mui/icons-material/AssignmentOutlined'
import CheckCircleOutlined from '@mui/icons-material/CheckCircleOutlined'
import HourglassEmpty from '@mui/icons-material/HourglassEmpty'
import ScheduleOutlined from '@mui/icons-material/ScheduleOutlined'
import { BarChart } from '@mui/x-charts/BarChart'
import { PieChart } from '@mui/x-charts/PieChart'
import { Link } from 'react-router'
import { api } from './api'
import { ErrorNotice } from './Shared'

const status = {
  ABERTO: { label: 'Aberto', color: '#547ccd' },
  EM_ANDAMENTO: { label: 'Em andamento', color: '#8463cc' },
  RESOLVIDO: { label: 'Resolvido', color: '#269181' },
  CANCELADO: { label: 'Cancelado', color: '#c07652' },
}

const number = new Intl.NumberFormat('pt-BR')
const shortDate = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' })
const periodDate = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'UTC' })
const timestamp = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo' })
const dateOnly = (value) => new Date(value + 'T00:00:00Z')

function duration(value) {
  if (value == null) return '—'
  const days = Math.floor(value / 1440)
  const hours = Math.floor((value % 1440) / 60)
  const minutes = value % 60
  return [days && days + ' d', hours && hours + ' h', (minutes || value === 0) && minutes + ' min'].filter(Boolean).join(' ')
}

function Panel({ title, description, action, children }) {
  return (
    <Paper variant="outlined" className="min-w-0 overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-4">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
          <p className="mt-1 text-sm text-neutral-600">{description}</p>
        </div>
        {action}
      </div>
      {children}
    </Paper>
  )
}

function Metrics({ data }) {
  const cards = [
    ['Total de chamados', number.format(data.total), 'Todos os estados', AssignmentOutlined],
    ['Em andamento', number.format(data.porStatus.EM_ANDAMENTO), 'Atendimentos ativos', HourglassEmpty],
    ['Resolvidos', number.format(data.resolvidosNoPeriodo), 'Nos últimos 30 dias', CheckCircleOutlined],
    ['Tempo médio de atendimento', duration(data.tmaMinutos), 'Do início à resolução no período', ScheduleOutlined],
  ]
  return (
    <section aria-label="Indicadores de chamados">
      <p className="mb-4 text-sm text-neutral-600">
        Últimos 30 dias · {periodDate.formatRange(dateOnly(data.inicio), dateOnly(data.fim))}
      </p>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(([label, value, detail, Icon]) => (
          <Paper key={label} variant="outlined" className="p-5">
            <span className="mb-4 grid size-9 place-items-center rounded-lg border border-neutral-200 bg-neutral-50 text-neutral-700">
              <Icon fontSize="small" />
            </span>
            <p className="text-sm font-medium text-neutral-600">{label}</p>
            <p className="mt-1 text-3xl font-semibold tracking-tight">{value}</p>
            <p className="mt-1 text-sm text-neutral-600">{detail}</p>
          </Paper>
        ))}
      </div>
    </section>
  )
}

function Charts({ data }) {
  const hasVolume = data.volume.some((day) => day.abertos || day.resolvidos)
  const slices = Object.entries(status).map(([id, item]) => ({ id, ...item, value: data.porStatus[id] }))
  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)]">
      <Panel title="Volume dos últimos 30 dias" description="Aberturas e resoluções por dia">
        {hasVolume ? (
          <div className="p-4">
            <BarChart
              aria-label="Aberturas e resoluções por dia"
              xAxis={[{ scaleType: 'band', data: data.volume.map((day) => shortDate.format(dateOnly(day.data))),
                tickLabelInterval: (_, index) => index % 7 === 0 || index === data.volume.length - 1 }]}
              yAxis={[{ tickMinStep: 1 }]}
              series={[
                { label: 'Abertos', data: data.volume.map((day) => day.abertos), color: status.ABERTO.color },
                { label: 'Resolvidos', data: data.volume.map((day) => day.resolvidos), color: status.RESOLVIDO.color },
              ]}
              height={250}
              grid={{ horizontal: true }}
            />
          </div>
        ) : <Empty>Nenhuma abertura ou resolução no período.</Empty>}
      </Panel>
      <Panel title="Chamados por estado" description="Distribuição do total de chamados">
        {data.total ? (
          <div className="grid items-center gap-4 p-5 sm:grid-cols-[minmax(130px,1fr)_minmax(130px,1fr)] xl:grid-cols-1 2xl:grid-cols-2">
            <div className="relative h-[160px]">
              <PieChart aria-label="Distribuição dos chamados por estado" height={160} hideLegend margin={0}
                series={[{ data: slices.filter((slice) => slice.value), innerRadius: '69%', outerRadius: '94%', paddingAngle: 3, cornerRadius: 4 }]} />
              <div aria-hidden="true" className="pointer-events-none absolute inset-0 grid place-content-center text-center">
                <span className="text-2xl font-semibold">{number.format(data.total)}</span>
                <span className="text-sm text-neutral-600">Total</span>
              </div>
            </div>
            <ul className="grid gap-2">
              {slices.map(({ id, label, color, value }) => (
                <li key={id} className="flex items-center gap-2 text-sm">
                  <span className="size-2.5 rounded-full" style={{ backgroundColor: color }} aria-hidden="true" />
                  <span className="grow text-neutral-700">{label}</span>
                  <span className="font-semibold">{number.format(value)}</span>
                </li>
              ))}
            </ul>
          </div>
        ) : <Empty>Nenhum chamado cadastrado.</Empty>}
      </Panel>
    </div>
  )
}

function Empty({ children }) {
  return <p className="px-5 py-12 text-center text-sm text-neutral-600">{children}</p>
}

function DashboardTable({ title, headers, minimumWidth, children }) {
  return <TableContainer>
    <Table size="small" aria-label={title} sx={{ minWidth: minimumWidth }}>
      <TableHead className="bg-neutral-50">
        <TableRow>{headers.map((label) => <TableCell key={label} className="px-5 py-3 font-semibold">{label}</TableCell>)}</TableRow>
      </TableHead>
      <TableBody>{children}</TableBody>
    </Table>
  </TableContainer>
}

function Workload({ technicians }) {
  return (
    <Panel title="Carga por técnico" description="Em andamento agora, com resoluções e tempo médio no período">
      {technicians.length ? (
        <DashboardTable title="Carga por técnico" headers={['Técnico', 'Em andamento', 'Resolvidos', 'Tempo médio']} minimumWidth={500}>
          {technicians.map((tech) => (
            <TableRow key={tech.codigo} hover>
              <TableCell component="th" scope="row" className="px-5 py-3 font-semibold">{tech.nome}</TableCell>
              <TableCell className="px-5 tabular-nums">{number.format(tech.emAndamento)}</TableCell>
              <TableCell className="px-5 tabular-nums">{number.format(tech.resolvidosNoPeriodo)}</TableCell>
              <TableCell className="px-5 whitespace-nowrap">{duration(tech.tmaMinutos)}</TableCell>
            </TableRow>
          ))}
        </DashboardTable>
      ) : <Empty>Nenhum técnico disponível para exibir.</Empty>}
    </Panel>
  )
}

function Recent({ tickets }) {
  return (
    <Panel title="Chamados recentes" description="Últimas solicitações abertas"
      action={<Button component={Link} to="/chamados" variant="outlined" size="small">Ver fila</Button>}>
      {tickets.length ? (
        <DashboardTable title="Chamados recentes" headers={['Chamado', 'Status', 'Abertura']} minimumWidth={460}>
          {tickets.map((ticket) => (
            <TableRow key={ticket.protocolo} hover>
              <TableCell className="px-5 py-3">
                <Link to={'/chamados/' + ticket.protocolo} className="block font-semibold underline-offset-4 hover:underline">{ticket.titulo}</Link>
                <span className="mt-1 block font-mono text-xs text-neutral-600">{ticket.protocolo}</span>
              </TableCell>
              <TableCell className="px-5"><Chip size="small" variant="outlined" label={status[ticket.status].label} /></TableCell>
              <TableCell className="px-5 whitespace-nowrap">{timestamp.format(new Date(ticket.criadoEm))}</TableCell>
            </TableRow>
          ))}
        </DashboardTable>
      ) : <Empty>Nenhum chamado cadastrado.</Empty>}
    </Panel>
  )
}

export default function Dashboard({ onSignOut }) {
  const [result, setResult] = useState({})
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api('/dashboard', { signal: controller.signal })
      .then((data) => setResult({ data }))
      .catch((error) => {
        if (error.name === 'AbortError') return
        if (error.status === 401) onSignOut()
        else setResult({ error })
      })
    return () => controller.abort()
  }, [attempt, onSignOut])

  if (result.error) return <ErrorNotice status={result.error.status} onRetry={result.error.status >= 500 ? () => {
    setResult({})
    setAttempt(attempt + 1)
  } : undefined} />

  if (!result.data) return <div className="grid min-h-44 place-items-center"><CircularProgress aria-label="Carregando indicadores" /></div>

  return <div className="grid gap-5">
    <Metrics data={result.data} />
    <Charts data={result.data} />
    <div className="grid items-start gap-5 xl:grid-cols-2">
      <Workload technicians={result.data.tecnicos} />
      <Recent tickets={result.data.recentes} />
    </div>
  </div>
}
