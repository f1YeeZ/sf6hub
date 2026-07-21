import { readFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..')
const backendDir = resolve(repoRoot, 'hub_demo_backend')
const outputFile = resolve(backendDir, 'target', 'dependency-tree.json')
const allowlistFile = resolve(repoRoot, 'scripts', 'osv-allowlist.json')
const result = spawnSync('mvn', [
  '--batch-mode',
  'dependency:tree',
  '-DoutputType=json',
  '-DoutputFile=target/dependency-tree.json',
], { cwd: backendDir, stdio: 'inherit', shell: process.platform === 'win32' })

if (result.error) throw result.error
if (result.status !== 0) process.exit(result.status || 1)

const tree = JSON.parse(await readFile(outputFile, 'utf8'))
const allowlist = JSON.parse(await readFile(allowlistFile, 'utf8'))
const components = new Map()
walk(tree)

const packages = [...components.values()]
const response = await fetch('https://api.osv.dev/v1/querybatch', {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({
    queries: packages.map(item => ({
      package: { ecosystem: 'Maven', name: item.name },
      version: item.version,
    })),
  }),
})

if (!response.ok) throw new Error(`OSV request failed: HTTP ${response.status}`)
const report = await response.json()
const findings = []
for (let index = 0; index < packages.length; index += 1) {
  for (const vulnerability of report.results[index]?.vulns || []) {
    const allowed = allowlist.find(item => item.id === vulnerability.id && item.package === packages[index].name)
    if (allowed && new Date(`${allowed.expires}T00:00:00Z`) > new Date()) {
      console.warn(`WARN temporarily allowed ${packages[index].name}: ${vulnerability.id} until ${allowed.expires}`)
      continue
    }
    findings.push(`${packages[index].name}@${packages[index].version}: ${vulnerability.id}`)
  }
}

if (findings.length) {
  console.error('FAIL vulnerable Maven dependencies detected:')
  findings.forEach(item => console.error(`- ${item}`))
  process.exit(1)
}

console.log(`OK OSV checked ${packages.length} Maven runtime dependencies`)

function walk(node) {
  if (node.groupId && node.artifactId && node.version && !['test', 'provided'].includes(node.scope)) {
    const name = `${node.groupId}:${node.artifactId}`
    components.set(`${name}@${node.version}`, { name, version: node.version })
  }
  for (const child of node.children || []) walk(child)
}
