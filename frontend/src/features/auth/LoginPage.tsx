import { useState } from "react"
import { useSearchParams } from "react-router-dom"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import {
  ArrowRight,
  CircleAlert,
  Eye,
  EyeOff,
  FileSignature,
  Headset,
  KeyRound,
  Landmark,
  Loader2,
  Lock,
  MonitorSmartphone,
  ShieldCheck,
  Stethoscope,
  User,
} from "lucide-react"
import { authApi, ApiError } from "@/lib/api"

const schema = z.object({
  username: z.string().min(1, "Ingrese su usuario"),
  password: z.string().min(1, "Ingrese su contraseña"),
})
type FormValues = z.infer<typeof schema>

function LogoMark() {
  return (
    <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-brand-800 text-white shadow-md">
      <Stethoscope className="h-6 w-6" aria-hidden="true" />
    </div>
  )
}

function HeroCard({ icon: Icon, title, subtitle, aside }: {
  icon: typeof MonitorSmartphone
  title: string
  subtitle: string
  aside?: React.ReactNode
}) {
  return (
    <div className="flex items-center justify-between rounded-xl bg-white/10 p-4 backdrop-blur-md transition-colors hover:bg-white/15">
      <div className="flex items-center gap-3.5">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-300/25 text-cyan-200">
          <Icon className="h-5 w-5" aria-hidden="true" />
        </div>
        <div>
          <p className="text-sm font-semibold text-white">{title}</p>
          <p className="text-xs text-brand-100">{subtitle}</p>
        </div>
      </div>
      {aside}
    </div>
  )
}

/** Login del frontend: credenciales validadas por Keycloak vía backend. */
export function LoginPage() {
  const [params] = useSearchParams()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", password: "" },
  })
  const authFailed = params.get("auth") === "failed"

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      await authApi<{ ok: boolean }>("/api/auth/login", {
        method: "POST",
        body: { username: values.username, password: values.password },
      })
      window.location.assign("/")
    } catch (error) {
      setServerError(
        error instanceof ApiError && error.status === 401
          ? "Usuario o contraseña incorrectos."
          : error instanceof ApiError
            ? error.message
            : "No se pudo iniciar sesión. Inténtelo de nuevo.",
      )
    }
  })

  return (
    <div className="flex min-h-svh flex-col bg-slate-50">
      <main className="flex flex-1 items-center justify-center p-4 sm:p-8">
        <div className="flex w-full max-w-7xl flex-col overflow-hidden rounded-xl bg-white shadow-xl lg:flex-row">
          {/* Columna izquierda: acceso */}
          <div className="flex w-full flex-col justify-between bg-white p-6 sm:p-10 lg:w-7/12 lg:p-12">
            <div>
              <div className="mb-8 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <LogoMark />
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xl font-bold tracking-tight text-brand-800">veris</span>
                      <span className="text-lg font-bold text-cyan-brand">•</span>
                      <span className="text-xs font-semibold uppercase tracking-wider text-cyan-brand">ParaMí</span>
                    </div>
                    <p className="text-xs font-medium text-ink-soft">Gestión de Inventario TI • Centrales Médicas</p>
                  </div>
                </div>
                <span className="hidden items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-cyan-700 sm:inline-flex">
                  <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-500" aria-hidden="true" />
                  Directorio Activo
                </span>
              </div>

              <div className="mb-6 space-y-1.5">
                <h1 className="text-2xl font-bold tracking-tight text-ink">Acceso Seguro al Sistema</h1>
                <p className="text-sm text-ink-soft">
                  Ingresa con tus credenciales corporativas. La validación se realiza contra el directorio Keycloak.
                </p>
              </div>

              {(authFailed || serverError) && (
                <div role="alert" className="mb-4 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                  <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                  <span>{serverError ?? "No se pudo completar el inicio de sesión. Inténtelo de nuevo."}</span>
                </div>
              )}

              <form className="space-y-4" onSubmit={onSubmit} noValidate>
                <div className="flex items-center gap-2 rounded-lg border border-cyan-200 bg-cyan-50 px-3 py-2 text-xs font-medium text-cyan-800">
                  <ShieldCheck className="h-4 w-4 text-cyan-brand" aria-hidden="true" />
                  <span>Detección automática de sede y rol mediante Keycloak Directory</span>
                </div>

                <div>
                  <label htmlFor="username" className="mb-1.5 block text-sm font-medium text-ink">Usuario institucional</label>
                  <div className="relative flex items-center">
                    <User className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-400" aria-hidden="true" />
                    <input
                      id="username"
                      type="text"
                      autoComplete="username"
                      placeholder="usuario@veris.com.ec"
                      className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-4 text-sm shadow-sm focus:border-cyan-brand focus:outline-none focus:ring-2 focus:ring-cyan-brand/30"
                      {...register("username")}
                    />
                  </div>
                  {errors.username && <p className="mt-1 text-xs text-red-600">{errors.username.message}</p>}
                </div>

                <div>
                  <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-ink">Contraseña</label>
                  <div className="relative flex items-center">
                    <Lock className="pointer-events-none absolute left-3.5 h-4 w-4 text-slate-400" aria-hidden="true" />
                    <input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      placeholder="••••••••••••"
                      className="h-11 w-full rounded-lg border border-slate-300 bg-white pl-10 pr-11 text-sm shadow-sm focus:border-cyan-brand focus:outline-none focus:ring-2 focus:ring-cyan-brand/30"
                      {...register("password")}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-3 p-1 text-slate-400 hover:text-brand-800 focus:outline-none"
                      aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="mt-2 flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-brand-800 text-sm font-semibold text-white shadow-md transition-all hover:bg-brand-950 hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-cyan-brand focus:ring-offset-2 disabled:opacity-60"
                >
                  {isSubmitting ? (
                    <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <KeyRound className="h-4 w-4" aria-hidden="true" />
                  )}
                  <span>{isSubmitting ? "Verificando…" : "Iniciar Sesión en Veris TI"}</span>
                  {!isSubmitting && <ArrowRight className="h-4 w-4" aria-hidden="true" />}
                </button>
              </form>
            </div>

            <div className="mt-8 flex items-start gap-3 rounded-b-xl border-t border-slate-100 bg-slate-50/60 px-6 py-5 sm:px-10 lg:px-12">
              <Lock className="mt-0.5 h-5 w-5 shrink-0 text-cyan-brand" aria-hidden="true" />
              <p className="text-xs leading-relaxed text-ink-soft">
                Acceso exclusivo para personal autorizado de <strong className="font-semibold text-brand-800">Veris Centrales Médicas</strong>.
                Conexión cifrada y control de acceso basado en roles (RBAC) por centro.
              </p>
            </div>
          </div>

          {/* Columna derecha: hero institucional */}
          <div className="relative flex w-full flex-col justify-between overflow-hidden bg-brand-800 p-6 text-white sm:p-10 lg:w-5/12 lg:p-12">
            <div className="absolute inset-0 bg-gradient-to-br from-brand-950 via-brand-800 to-[#30277b] opacity-95" aria-hidden="true" />
            <div className="absolute -right-24 -top-24 h-80 w-80 rounded-full bg-cyan-400/20 blur-3xl" aria-hidden="true" />
            <div className="absolute -bottom-24 -left-24 h-80 w-80 rounded-full bg-indigo-300/15 blur-3xl" aria-hidden="true" />

            <div className="relative z-10 space-y-6">
              <div className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3.5 py-1.5 text-xs font-medium tracking-wide text-cyan-200 backdrop-blur-md">
                <Landmark className="h-3.5 w-3.5" aria-hidden="true" />
                PLATAFORMA DE ACTIVOS CLÍNICOS • TI
              </div>
              <div className="space-y-3">
                <h2 className="text-xl font-bold leading-snug tracking-tight">
                  Gestión integral, trazabilidad y control de equipamiento tecnológico hospitalario.
                </h2>
                <p className="text-sm leading-relaxed text-brand-100">
                  Consola centralizada para la administración de hardware, equipos diagnósticos,
                  periféricos de consultorio y actas digitales de entrega y custodia.
                </p>
              </div>

              <div className="space-y-3 pt-3">
                <HeroCard
                  icon={MonitorSmartphone}
                  title="Inventario multicentro"
                  subtitle="Activos, bodegas TI y ubicaciones por sede"
                  aside={<span className="rounded bg-emerald-400/20 px-2 py-0.5 text-xs font-semibold text-emerald-300">Quito · Gye · CUE</span>}
                />
                <HeroCard
                  icon={FileSignature}
                  title="Actas y firmas digitales"
                  subtitle="Matrices de firma y custodia auditada"
                  aside={<ShieldCheck className="h-4 w-4 text-cyan-200" aria-hidden="true" />}
                />
                <HeroCard
                  icon={ShieldCheck}
                  title="Seguridad y auditoría"
                  subtitle="RBAC por centro, sesiones y bitácora de cambios"
                />
              </div>
            </div>

            <div className="relative z-10 mt-8 space-y-2 border-t border-white/10 pt-6">
              <div className="flex items-center justify-between text-xs text-brand-100">
                <span className="flex items-center gap-1.5">
                  <Headset className="h-3.5 w-3.5 text-cyan-200" aria-hidden="true" />
                  Mesa de Ayuda TI
                </span>
                <span className="text-cyan-200">soporte@veris.com.ec</span>
              </div>
              <div className="flex items-center justify-between text-[11px] text-brand-100/80">
                <span>Veris Ecuador S.A. • Todos los derechos reservados</span>
                <span>Sistema de Inventario Tecnológico</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="w-full bg-white/80 py-4 shadow-[0_1px_8px_rgba(0,0,0,0.04)] backdrop-blur-md">
        <div className="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-2 px-4 text-center text-xs text-ink-soft md:flex-row md:px-8 md:text-left">
          <span className="flex items-center gap-1.5">
            <ShieldCheck className="h-4 w-4 text-cyan-brand" aria-hidden="true" />
            Portal Seguro Veris IT • Conexión Cifrada TLS
          </span>
          <span className="flex items-center gap-1.5">
            <Lock className="h-3.5 w-3.5 text-cyan-brand" aria-hidden="true" />
            Control de acceso por roles y centros autorizados
          </span>
        </div>
      </footer>
    </div>
  )
}
