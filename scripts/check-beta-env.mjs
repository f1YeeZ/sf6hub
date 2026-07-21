import { resolve, relative, isAbsolute } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..')
const env = process.env

const required = [
  'APP_ENV',
  'DB_URL',
  'DB_USERNAME',
  'DB_PASSWORD',
  'JWT_SECRET',
  'QQ_MAIL_USERNAME',
  'QQ_MAIL_AUTH_CODE',
  'PUBLIC_BASE_URL',
  'CORS_ALLOWED_ORIGIN_PATTERNS',
  'VITE_API_BASE_URL',
]

const errors = []
const warnings = []

for (const name of required) {
  if (!hasValue(env[name])) {
    errors.push(`${name} is required`)
  }
}

if (hasValue(env.APP_ENV) && !['beta', 'production', 'prod'].includes(env.APP_ENV.toLowerCase())) {
  errors.push('APP_ENV must be beta or production for beta checks')
}

if (hasValue(env.JWT_SECRET) && env.JWT_SECRET.length < 32) {
  errors.push('JWT_SECRET must be at least 32 characters')
}

if (hasValue(env.PUBLIC_BASE_URL)) {
  validateUrl('PUBLIC_BASE_URL', env.PUBLIC_BASE_URL, { requireApiSuffix: true })
  requireHttps('PUBLIC_BASE_URL', env.PUBLIC_BASE_URL)
}

if (hasValue(env.VITE_API_BASE_URL)) {
  validateUrl('VITE_API_BASE_URL', env.VITE_API_BASE_URL, { requireApiSuffix: true })
  requireHttps('VITE_API_BASE_URL', env.VITE_API_BASE_URL)
}

if (hasValue(env.PUBLIC_BASE_URL) && hasValue(env.VITE_API_BASE_URL) && env.PUBLIC_BASE_URL !== env.VITE_API_BASE_URL) {
  warnings.push('PUBLIC_BASE_URL and VITE_API_BASE_URL differ; confirm this is intentional')
}

if (hasValue(env.CORS_ALLOWED_ORIGIN_PATTERNS)) {
  const origins = env.CORS_ALLOWED_ORIGIN_PATTERNS.split(',').map(value => value.trim()).filter(Boolean)
  if (!origins.length) {
    errors.push('CORS_ALLOWED_ORIGIN_PATTERNS must include at least one origin')
  }
  for (const origin of origins) {
    validateUrl('CORS_ALLOWED_ORIGIN_PATTERNS entry', origin)
    requireHttps('CORS_ALLOWED_ORIGIN_PATTERNS entry', origin)
  }
}

const storageProvider = String(env.STORAGE_PROVIDER || 'local').trim().toLowerCase()
if (!['local', 'oss'].includes(storageProvider)) {
  errors.push('STORAGE_PROVIDER must be local or oss')
}
if (storageProvider === 'oss') {
  for (const name of ['OSS_ENDPOINT', 'OSS_BUCKET', 'OSS_ACCESS_KEY_ID', 'OSS_ACCESS_KEY_SECRET', 'OSS_PUBLIC_BASE_URL']) {
    if (!hasValue(env[name])) errors.push(`${name} is required when STORAGE_PROVIDER=oss`)
  }
  if (hasValue(env.OSS_ENDPOINT)) {
    validateUrl('OSS_ENDPOINT', env.OSS_ENDPOINT)
    requireHttps('OSS_ENDPOINT', env.OSS_ENDPOINT)
  }
  if (hasValue(env.OSS_PUBLIC_BASE_URL)) {
    validateUrl('OSS_PUBLIC_BASE_URL', env.OSS_PUBLIC_BASE_URL)
    requireHttps('OSS_PUBLIC_BASE_URL', env.OSS_PUBLIC_BASE_URL)
  }
  if (hasValue(env.OSS_BUCKET) && !/^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$/.test(env.OSS_BUCKET)) {
    errors.push('OSS_BUCKET must be a valid lowercase OSS bucket name')
  }
} else {
  const uploadDir = env.UPLOAD_DIR || ''
  if (!hasValue(uploadDir)) {
    warnings.push('UPLOAD_DIR is not set; backend will use local uploads/ and this is not suitable for production')
  } else {
    const resolvedUploadDir = resolve(uploadDir)
    const relativeUploadDir = relative(repoRoot, resolvedUploadDir)
    if (!isAbsolute(uploadDir)) {
      warnings.push('UPLOAD_DIR is relative; use an absolute persistent storage path in beta')
    }
    if (relativeUploadDir && !relativeUploadDir.startsWith('..') && !isAbsolute(relativeUploadDir)) {
      errors.push('UPLOAD_DIR must be outside the Git workspace for beta')
    }
  }
}

if ((env.FLYWAY_ENABLED || '').toLowerCase() === 'false') {
  warnings.push('FLYWAY_ENABLED=false; beta deploys should normally run Flyway')
}

if ((env.SQL_INIT_MODE || '').toLowerCase() && (env.SQL_INIT_MODE || '').toLowerCase() !== 'never') {
  errors.push('SQL_INIT_MODE should be never for beta')
}

if ((env.OFFICIAL_FRAME_SYNC_ENABLED || '').toLowerCase() === 'true') {
  warnings.push('OFFICIAL_FRAME_SYNC_ENABLED=true; review upstream sync load before beta')
}

for (const warning of warnings) {
  console.warn(`WARN ${warning}`)
}

if (errors.length) {
  for (const error of errors) {
    console.error(`FAIL ${error}`)
  }
  process.exitCode = 1
} else {
  console.log('OK beta environment checks')
}

function hasValue(value) {
  return typeof value === 'string' && value.trim().length > 0
}

function validateUrl(name, value, options = {}) {
  if (options.allowWildcard && value.includes('*')) {
    if (!value.startsWith('http://') && !value.startsWith('https://')) {
      errors.push(`${name} wildcard URL must start with http:// or https://`)
    }
    return
  }
  let parsed
  try {
    parsed = new URL(value)
  } catch (_) {
    errors.push(`${name} must be a valid URL`)
    return
  }
  if (!['http:', 'https:'].includes(parsed.protocol)) {
    errors.push(`${name} must use http or https`)
  }
  if (options.requireApiSuffix && !parsed.pathname.replace(/\/+$/, '').endsWith('/api')) {
    errors.push(`${name} must include the /api path`)
  }
}

function requireHttps(name, value) {
  try {
    if (new URL(value).protocol !== 'https:') errors.push(`${name} must use HTTPS for beta`)
  } catch (_) {
    // URL format errors are reported by validateUrl.
  }
}
