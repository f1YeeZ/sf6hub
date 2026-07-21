import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative } from 'node:path'

const root = new URL('../', import.meta.url)
const includedExtensions = new Set([
  '.cjs', '.css', '.env', '.example', '.html', '.java', '.js', '.json', '.md',
  '.mjs', '.properties', '.sql', '.vue', '.xml', '.yml', '.yaml',
])
const excludedDirectories = new Set([
  '.git', '.idea', 'dist', 'node_modules', 'output', 'target', 'uploads',
])
const decoder = new TextDecoder('utf-8', { fatal: true })
const errors = []

await visit(root)

if (errors.length) {
  errors.forEach(error => console.error(`FAIL ${error}`))
  process.exitCode = 1
} else {
  console.log('OK UTF-8 source checks')
}

async function visit(directoryUrl) {
  const entries = await readdir(directoryUrl, { withFileTypes: true })
  for (const entry of entries) {
    if (entry.isDirectory() && excludedDirectories.has(entry.name)) continue
    const entryUrl = new URL(`${entry.name}${entry.isDirectory() ? '/' : ''}`, directoryUrl)
    if (entry.isDirectory()) {
      await visit(entryUrl)
      continue
    }
    if (!entry.isFile() || !shouldCheck(entry.name)) continue
    await checkFile(entryUrl)
  }
}

async function checkFile(fileUrl) {
  const bytes = await readFile(fileUrl)
  const path = relative(new URL('.', root).pathname, fileUrl.pathname).replaceAll('\\', '/')
  if (bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf) {
    errors.push(`${path} contains a UTF-8 BOM`)
  }
  try {
    const text = decoder.decode(bytes)
    if (text.includes('\uFFFD')) errors.push(`${path} contains replacement characters`)
  } catch {
    errors.push(`${path} is not valid UTF-8`)
  }
}

function shouldCheck(name) {
  if (name === '.editorconfig' || name === '.gitignore') return true
  return includedExtensions.has(extname(name).toLowerCase())
}
