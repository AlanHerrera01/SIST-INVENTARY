import { api } from "@/lib/api"

export interface CityView { id: string; code: string; name: string; state: string }
export interface SiteView { id: string; cityId: string; cityName: string; code: string; name: string; kind: string; state: string }
export interface CenterView { id: string; code: string; name: string; kind: string; state: string; cityId: string | null; cityName: string | null; siteId: string | null; siteName: string | null }
export interface LocationView { id: string; centerId: string; name: string; type: string; parentLocationId: string | null }
export interface MatrixView { id: string; code: string; name: string; state: string; description: string | null; cityId: string | null; cityName: string | null; siteId: string | null; centerIds: string[] }
export interface StructureView { cities: CityView[]; sites: SiteView[]; centers: CenterView[]; locations: LocationView[]; matrices: MatrixView[] }

export interface ResponsibleView {
  id: string; centerId: string; centerName: string; userId: string | null; username: string | null
  role: string; email: string; validFrom: string | null; validUntil: string | null; state: string; assignedBy: string | null
}
export interface HistoryView {
  id: string; action: string; centerId: string | null; userId: string | null; role: string | null
  email: string | null; validFrom: string | null; validUntil: string | null
  changedBy: string | null; changedAt: string; reason: string | null
}

export const orgApi = {
  structure: () => api<StructureView>("/org/structure"),
  createCity: (input: { code: string; name: string }) => api<CityView>("/org/cities", { method: "POST", body: input }),
  updateCity: (id: string, input: { name?: string; state?: string }) => api<CityView>(`/org/cities/${id}`, { method: "PATCH", body: input }),
  deleteCity: (id: string) => api<void>(`/org/cities/${id}`, { method: "DELETE" }),
  createSite: (input: { cityId: string; code: string; name: string; kind: string }) => api<SiteView>("/org/sites", { method: "POST", body: input }),
  updateSite: (id: string, input: { name?: string; kind?: string; state?: string }) => api<SiteView>(`/org/sites/${id}`, { method: "PATCH", body: input }),
  deleteSite: (id: string) => api<void>(`/org/sites/${id}`, { method: "DELETE" }),
  createCenter: (input: { code: string; name: string; cityId?: string | null; siteId?: string | null; kind: string }) =>
    api<CenterView>("/org/centers", { method: "POST", body: input }),
  updateCenter: (id: string, input: { name?: string; cityId?: string | null; siteId?: string | null; kind?: string; state?: string }) =>
    api<CenterView>(`/org/centers/${id}`, { method: "PATCH", body: input }),
  deleteCenter: (id: string) => api<void>(`/org/centers/${id}`, { method: "DELETE" }),
  createLocation: (input: { centerId: string; name: string; type: string; parentLocationId?: string | null }) =>
    api<LocationView>("/org/locations", { method: "POST", body: input }),
  createMatrix: (input: { code: string; name: string; cityId?: string | null; siteId?: string | null; description?: string }) =>
    api<MatrixView>("/org/receiving-matrices", { method: "POST", body: input }),
  updateMatrix: (id: string, input: { name?: string; cityId?: string | null; siteId?: string | null; state?: string; description?: string }) =>
    api<MatrixView>(`/org/receiving-matrices/${id}`, { method: "PATCH", body: input }),
  deleteMatrix: (id: string) => api<void>(`/org/receiving-matrices/${id}`, { method: "DELETE" }),
  linkCenter: (matrixId: string, centerId: string) =>
    api<void>(`/org/receiving-matrices/${matrixId}/centers/${centerId}`, { method: "POST" }),
  unlinkCenter: (matrixId: string, centerId: string) =>
    api<void>(`/org/receiving-matrices/${matrixId}/centers/${centerId}`, { method: "DELETE" }),
  responsibles: {
    list: (centerId?: string) => api<ResponsibleView[]>("/org/responsibles", { params: { centerId } }),
    create: (input: { centerId: string; userId?: string | null; role: string; email: string; validUntil?: string; reason?: string }) =>
      api<ResponsibleView>("/org/responsibles", { method: "POST", body: input }),
    end: (id: string, reason?: string) =>
      api<ResponsibleView>(`/org/responsibles/${id}/end`, { method: "POST", body: reason ? { reason } : {} }),
    history: (id: string) => api<HistoryView[]>(`/org/responsibles/${id}/history`),
  },
}
