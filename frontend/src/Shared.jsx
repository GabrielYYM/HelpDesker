import { Button } from '@mui/material'
import HeadsetMicOutlined from '@mui/icons-material/HeadsetMicOutlined'
import LockOutlined from '@mui/icons-material/LockOutlined'
import ReportProblemOutlined from '@mui/icons-material/ReportProblemOutlined'
import SearchOffOutlined from '@mui/icons-material/SearchOffOutlined'
import { Link } from 'react-router'

export function Brand() {
  return (
    <div className="flex items-center gap-2.5">
      <span className="grid size-9 place-items-center rounded-lg bg-neutral-900 text-white">
        <HeadsetMicOutlined fontSize="small" />
      </span>
      <span className="text-lg font-bold tracking-tight">HelpDesker</span>
    </div>
  )
}

export function ErrorNotice({ status, onRetry }) {
  const errors = {
    403: [LockOutlined, 'Acesso negado', 'Seu perfil não tem permissão para acessar esta área.'],
    404: [SearchOffOutlined, 'Página não encontrada', 'Confira o endereço ou volte para o início.'],
    500: [ReportProblemOutlined, 'Não foi possível carregar', 'O serviço está indisponível no momento. Tente novamente.'],
  }
  const code = Object.hasOwn(errors, status) ? status : 500
  const [Icon, title, description] = errors[code]
  return (
    <section className="mx-auto flex min-h-[60vh] max-w-lg flex-col items-center justify-center px-6 text-center">
      <span className="mb-5 grid size-14 place-items-center rounded-xl border border-neutral-200 bg-white"><Icon /></span>
      <div className="mb-2 text-sm font-semibold text-neutral-500">{code}</div>
      <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
      <p className="mt-3 text-neutral-600">{description}</p>
      <div className="mt-6 flex flex-wrap items-center justify-center gap-2">
        {onRetry && <Button variant="contained" onClick={onRetry}>Tentar novamente</Button>}
        <Button variant="outlined" component={Link} to="/">Voltar ao início</Button>
      </div>
    </section>
  )
}
