import { api } from "@/lib/api"

// ---- Tipos (espejo de los records del backend) ----

export interface UserSummary {
  id: string
  username: string
  displayName: string
  email: string | null
  state: "ACTIVO" | "INACTIVO" | string
  createdAt: string
}

export interface AssignmentView {
  id: string
  roleId: string
  roleCode: string
  roleName: string
  scopeKind: "GLOBAL" | "CENTROS" | string
  centerIds: string[]
  validFrom: string
  validUntil: string | null
  state: string
  grantedBy: string | null
}

export interface MatrixMembershipView {
  matrixId: string
  code: string
  name: string
  centerId: string | null
  actType: string
  memberRole: "FIRMANTE" | "APROBADOR" | string
}

export interface UserDetail {
  summary: UserSummary
  assignments: AssignmentView[]
  matrices: MatrixMembershipView[]
}

export interface RoleView {
  id: string
  code: string
  name: string
  description: string | null
  permissions: string[]
  activeAssignments: number
  createdAt: string
}

export interface Permission {
  code: string
  name: string
  description: string | null
}

export interface MemberView {
  id: string
  userId: string
  username: string
  displayName: string
  memberRole: "FIRMANTE" | "APROBADOR" | string
  signerPosition: number | null
}

export interface MatrixView {
  id: string
  code: string
  name: string
  centerId: string | null
  centerCode: string | null
  actType: string
  matrixState: "ACTIVO" | "INACTIVO" | string
  description: string | null
  members: MemberView[]
}

export interface AuditEventView {
  id: string
  occurredAt: string
  action: string
  entityType: string
  entityId: string | null
  outcome: string
  actorUserId: string | null
  actorName: string | null
  centerId: string | null
  changedFields: string | null
}

export interface AuditPage {
  items: AuditEventView[]
  nextCursor: string | null
}

// ---- Comandos ----

export interface CreateUserInput {
  username: string
  displayName: string
  email?: string
  temporaryPassword: string
}

export interface UpdateUserInput {
  displayName: string
  email?: string
}

export interface CreateRoleInput {
  code: string
  name: string
  description?: string
}

export interface CreateMatrixInput {
  code: string
  name: string
  centerId?: string | null
  actType: string
  description?: string
}

// ---- Llamadas ----

export const accessApi = {
  users: {
    list: (params: { state?: string; q?: string; limit?: number; offset?: number }) =>
      api<UserSummary[]>("/users", { params }),
    detail: (id: string) => api<UserDetail>(`/users/${id}`),
    create: (input: CreateUserInput) => api<UserSummary>("/users", { method: "POST", body: input }),
    update: (id: string, input: UpdateUserInput) =>
      api<UserSummary>(`/users/${id}`, { method: "PATCH", body: input }),
    activate: (id: string) => api<UserSummary>(`/users/${id}/activate`, { method: "POST" }),
    suspend: (id: string, reason?: string) =>
      api<UserSummary>(`/users/${id}/suspend`, { method: "POST", body: reason ? { reason } : {} }),
    sync: (id: string) => api<UserDetail>(`/users/${id}/sync`, { method: "POST" }),
    assignRole: (id: string, input: { roleId: string; scopeKind: string; centerIds?: string[]; validUntil?: string; reason?: string }) =>
      api<AssignmentView>(`/users/${id}/role-assignments`, { method: "POST", body: input }),
    revokeAssignment: (id: string, assignmentId: string, reason?: string) =>
      api<void>(`/users/${id}/role-assignments/${assignmentId}/revoke`, {
        method: "POST",
        body: reason ? { reason } : {},
      }),
  },
  roles: {
    list: () => api<RoleView[]>("/roles"),
    detail: (id: string) => api<RoleView>(`/roles/${id}`),
    create: (input: CreateRoleInput) => api<RoleView>("/roles", { method: "POST", body: input }),
    update: (id: string, input: { name: string; description?: string }) =>
      api<RoleView>(`/roles/${id}`, { method: "PATCH", body: input }),
    replacePermissions: (id: string, permissionCodes: string[]) =>
      api<RoleView>(`/roles/${id}/permissions`, { method: "PUT", body: { permissionCodes } }),
  },
  permissions: () => api<Permission[]>("/permissions"),
  matrices: {
    list: (includeInactive = false) => api<MatrixView[]>("/signature-matrices", { params: { includeInactive } }),
    detail: (id: string) => api<MatrixView>(`/signature-matrices/${id}`),
    create: (input: CreateMatrixInput) => api<MatrixView>("/signature-matrices", { method: "POST", body: input }),
    update: (id: string, input: { name?: string; description?: string; actType?: string; matrixState?: string }) =>
      api<MatrixView>(`/signature-matrices/${id}`, { method: "PATCH", body: input }),
    addMember: (id: string, input: { userId: string; memberRole: string; signerPosition?: number }) =>
      api<MemberView>(`/signature-matrices/${id}/members`, { method: "POST", body: input }),
    removeMember: (id: string, memberId: string) =>
      api<void>(`/signature-matrices/${id}/members/${memberId}`, { method: "DELETE" }),
    mine: () => api<MatrixView[]>("/me/matrices"),
  },
  audit: {
    list: (params: {
      entityType?: string
      entityId?: string
      action?: string
      outcome?: string
      actorUserId?: string
      occurredFrom?: string
      occurredTo?: string
      cursor?: string
      limit?: number
    }) => api<AuditPage>("/audit-events", { params }),
  },
}
