const PKCE_CHARSET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'

export interface PkceParams {
  codeVerifier: string
  codeChallenge: string
  state: string
}

export async function createPkceParams(): Promise<PkceParams> {
  const codeVerifier = randomPkceString(64)
  return {
    codeVerifier,
    codeChallenge: await createCodeChallenge(codeVerifier),
    state: randomPkceString(32),
  }
}

export async function createCodeChallenge(codeVerifier: string): Promise<string> {
  const input = new TextEncoder().encode(codeVerifier)
  const digest = await crypto.subtle.digest('SHA-256', input)
  return base64UrlEncode(new Uint8Array(digest))
}

function randomPkceString(length: number): string {
  const bytes = new Uint8Array(length)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (byte) => PKCE_CHARSET[byte % PKCE_CHARSET.length]).join('')
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return window
    .btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')
}
