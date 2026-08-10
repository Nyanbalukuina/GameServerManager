export type AuthenticationStatus = {
  configured: boolean
  authenticated: boolean
  username: string | null
  setupAllowed: boolean
}

export type CsrfToken = {
  token: string
  headerName: string
}
