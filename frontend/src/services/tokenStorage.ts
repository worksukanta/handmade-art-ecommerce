const accessTokenKey = 'handmade-art.access-token'

export const tokenStorage = {
  get(): string | null {
    return window.localStorage.getItem(accessTokenKey)
  },

  set(token: string): void {
    window.localStorage.setItem(accessTokenKey, token)
  },

  clear(): void {
    window.localStorage.removeItem(accessTokenKey)
  },
}
