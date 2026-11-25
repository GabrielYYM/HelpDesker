import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, GlobalStyles } from '@mui/material'
import { createTheme, StyledEngineProvider, ThemeProvider } from '@mui/material/styles'
import { BrowserRouter } from 'react-router'
import './index.css'
import App from './App.jsx'

const neutral = { main: '#303030' }
const theme = createTheme({
  palette: {
    primary: neutral,
    secondary: neutral,
    error: neutral,
    warning: neutral,
    info: neutral,
    success: neutral,
    background: { default: '#f8f8f8', paper: '#ffffff' },
    text: { primary: '#191919', secondary: '#626262' },
    divider: '#e6e6e6',
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: 'Inter, system-ui, sans-serif',
    button: { textTransform: 'none', fontWeight: 650 },
  },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiPaper: { defaultProps: { elevation: 0 } },
    MuiTableCell: { styleOverrides: { root: { borderColor: '#ededed' } } },
  },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <StyledEngineProvider enableCssLayer>
      <GlobalStyles styles="@layer theme, base, mui, components, utilities;" />
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ThemeProvider>
    </StyledEngineProvider>
  </StrictMode>,
)
