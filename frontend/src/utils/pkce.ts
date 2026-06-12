import { sha256Bytes } from './sha256'

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
  const digest = await sha256Bytes(codeVerifier)
  return base64UrlEncode(digest)
}

function randomPkceString(length: number): string {
  const bytes = new Uint8Array(length)
  const cryptoApi = globalThis.crypto
  if (!cryptoApi?.getRandomValues) {
    throw new Error('当前浏览器不支持安全随机数，无法发起登录')
  }
  cryptoApi.getRandomValues(bytes)
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
