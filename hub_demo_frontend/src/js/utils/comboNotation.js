const controllerBase = '/street-fighter-6/controller'

const notationMap = {
  '63214': {
    label: 'HALF CIRCLE BACK',
    images: [`${controllerBase}/key-r.png`, `${controllerBase}/key-dr.png`, `${controllerBase}/key-d.png`, `${controllerBase}/key-dl.png`, `${controllerBase}/key-l.png`],
  },
  '41236': {
    label: 'HALF CIRCLE FORWARD',
    images: [`${controllerBase}/key-l.png`, `${controllerBase}/key-dl.png`, `${controllerBase}/key-d.png`, `${controllerBase}/key-dr.png`, `${controllerBase}/key-r.png`],
  },
  '66': {
    label: 'DASH FORWARD',
    images: [`${controllerBase}/key-r.png`, `${controllerBase}/key-r.png`],
  },
  '44': {
    label: 'DASH BACK',
    images: [`${controllerBase}/key-l.png`, `${controllerBase}/key-l.png`],
  },
  '236': {
    label: 'QUARTER CIRCLE FORWARD',
    images: [`${controllerBase}/key-d.png`, `${controllerBase}/key-dr.png`, `${controllerBase}/key-r.png`],
  },
  '214': {
    label: 'QUARTER CIRCLE BACK',
    images: [`${controllerBase}/key-d.png`, `${controllerBase}/key-dl.png`, `${controllerBase}/key-l.png`],
  },
  '623': {
    label: 'DRAGON PUNCH',
    images: [`${controllerBase}/key-r.png`, `${controllerBase}/key-d.png`, `${controllerBase}/key-dr.png`],
  },
  '28': {
    label: 'DOWN UP',
    images: [`${controllerBase}/key-dc.png`, `${controllerBase}/key-u.png`],
  },
  '46': {
    label: 'BACK FORWARD',
    images: [`${controllerBase}/key-lc.png`, `${controllerBase}/key-r.png`],
  },
  '1': { label: 'DOWN-BACK', images: [`${controllerBase}/key-dl.png`] },
  '2': { label: 'DOWN', images: [`${controllerBase}/key-d.png`] },
  '3': { label: 'DOWN-FORWARD', images: [`${controllerBase}/key-dr.png`] },
  '4': { label: 'BACK', images: [`${controllerBase}/key-l.png`] },
  '5': { label: 'NEUTRAL', images: [`${controllerBase}/key-nutral.png`] },
  '6': { label: 'FORWARD', images: [`${controllerBase}/key-r.png`] },
  '7': { label: 'UP-BACK', images: [`${controllerBase}/key-ul.png`] },
  '8': { label: 'UP', images: [`${controllerBase}/key-u.png`] },
  '9': { label: 'UP-FORWARD', images: [`${controllerBase}/key-ur.png`] },
  'LPMPHP': { label: 'DOUBLE PUNCH', images: [`${controllerBase}/icon_punch.png`, `${controllerBase}/icon_punch.png`] },
  'LKMKHK': { label: 'DOUBLE KICK', images: [`${controllerBase}/icon_kick.png`, `${controllerBase}/icon_kick.png`] },
  'PP': { label: 'DOUBLE PUNCH', images: [`${controllerBase}/icon_punch.png`, `${controllerBase}/icon_punch.png`] },
  'KK': { label: 'DOUBLE KICK', images: [`${controllerBase}/icon_kick.png`, `${controllerBase}/icon_kick.png`] },
  'LP': { label: 'LIGHT PUNCH', images: [`${controllerBase}/icon_punch_l.png`] },
  'MP': { label: 'MEDIUM PUNCH', images: [`${controllerBase}/icon_punch_m.png`] },
  'HP': { label: 'HEAVY PUNCH', images: [`${controllerBase}/icon_punch_h.png`] },
  'LK': { label: 'LIGHT KICK', images: [`${controllerBase}/icon_kick_l.png`] },
  'MK': { label: 'MEDIUM KICK', images: [`${controllerBase}/icon_kick_m.png`] },
  'HK': { label: 'HEAVY KICK', images: [`${controllerBase}/icon_kick_h.png`] },
  'P': { label: 'PUNCH', images: [`${controllerBase}/icon_punch.png`] },
  'K': { label: 'KICK', images: [`${controllerBase}/icon_kick.png`] },
  'ML': { label: 'MODERN LIGHT', images: [`${controllerBase}/modern_l.png`] },
  'MM': { label: 'MODERN MEDIUM', images: [`${controllerBase}/modern_m.png`] },
  'MH': { label: 'MODERN HEAVY', images: [`${controllerBase}/modern_h.png`] },
  'SP': { label: 'MODERN SPECIAL', images: [`${controllerBase}/modern_sp.png`] },
  'DI': { label: 'DRIVE IMPACT', images: [`${controllerBase}/modern_dl.png`] },
  'DP': { label: 'DRIVE PARRY', images: [`${controllerBase}/modern_dp.png`] },
  'AUTO': { label: 'AUTO', images: [`${controllerBase}/modern_auto.png`] },
  'THROW': { label: 'THROW', images: [`${controllerBase}/icon_throw.png`] },
  'CHARGE2': { label: 'HOLD DOWN', images: [`${controllerBase}/key-dc.png`] },
  'CHARGE4': { label: 'HOLD BACK', images: [`${controllerBase}/key-lc.png`] },
  'CHARGE6': { label: 'HOLD FORWARD', images: [`${controllerBase}/key-rc.png`] },
  'ATK': { label: 'ATTACK', images: [`${controllerBase}/key-all.png`] },
  'HD': { label: 'BUTTON MASH', images: [`${controllerBase}/key-barrage.png`] },
  '360': { label: '360', images: [`${controllerBase}/key-circle.png`] },
  '720': { label: '720', images: [`${controllerBase}/key-circle.png`, `${controllerBase}/key-circle.png`] },
  'PLUS': { label: 'PLUS', images: [`${controllerBase}/key-plus.png`] },
  'OR': { label: 'OR', images: [`${controllerBase}/key-or.png`] },
}

const textMap = {
  PF: '完美',
  AGAINST_WALL: '板边',

  HOLD_OK: '长按',
  STOCK_0: '无资源',
  STOCK_1: '1印记',
  STOCK_2: '2印记',
  NEAR: '近距离',
  LOW_LIFE: '低血量',
  GUARD_OR_PARRY: '防御/招架中',
  ON_WAKEUP: '起身时',
  PARRY: '招架中',
  FOLLOWUP: '派生后',
  BEFORE_ATTACK: '攻击前',
  NO_INPUT: '无需输入',
  DURING_MOVE: '招式中',
  FAR: '远距离',
  DRUNK_1: '醉意1级',
  DRUNK_2: '醉意2级',
  DRUNK_3: '醉意3级',
  DRUNK_4: '醉意4级',
  ON_STRIKE: '受击触发',
  GUARD_DIR: '防御方向',
  J: '跳跃中',
  OD: 'OD',
  DR: '绿冲',
  DRC: '绿冲取消',
  PC: 'PUNISH COUNTER',
  CH: 'COUNTER HIT',
  JC: 'JUMP CANCEL',
  XX: 'SPECIAL CANCEL',
  SA1: 'SA1',
  SA2: 'SA2',
  SA3: 'SA3',
}

const separatorMap = {
  '~': '~',
  ',': ',',
  '.': '.',
}

const separatorIconMap = {
  '+': notationMap.PLUS,
  '/': notationMap.OR,
}

const greedyTokens = [
  ...Object.keys(textMap),
  ...Object.keys(notationMap),
].sort((left, right) => right.length - left.length)

function parseToken(token) {
  const normalized = String(token || '').trim().toUpperCase()
  if (!normalized) return []

  const parts = []
  let rest = normalized

  while (rest.length) {
    if (rest.startsWith('PF.')) {
      parts.push({ type: 'text', key: 'PF', value: textMap.PF, tone: 'perfect' })
      rest = rest.slice(3)
      continue
    }

    if (rest.startsWith('[')) {
      const chargeEnd = rest.indexOf(']')
      if (chargeEnd > 1) {
        parts.push(...parseToken(rest.slice(1, chargeEnd)))
        parts.push({ type: 'text', key: 'CHARGE', value: '蓄力', accent: true })
        rest = rest.slice(chargeEnd + 1)
        const chargedDirections = rest.match(/^[1-9]+/)?.[0] || ''
        if (chargedDirections) {
          parts.push(...chargedDirections.split('').map(direction => ({
            type: 'icons',
            key: direction,
            ...notationMap[direction],
          })))
          rest = rest.slice(chargedDirections.length)
        }
        continue
      }
    }

    if (rest.startsWith('J.')) {
      parts.push({ type: 'text', key: 'J', value: textMap.J, accent: true })
      rest = rest.slice(2)
      continue
    }

    if (rest.startsWith('J') && rest.length > 1 && /^(?:[1-9]|L|M|H|P|K)/.test(rest.slice(1))) {
      parts.push({ type: 'text', key: 'J', value: textMap.J, accent: true })
      rest = rest.slice(1)
      continue
    }

    if (rest[0] === '-') {
      parts.push({ type: 'arrow', key: '-' })
      rest = rest.slice(1)
      continue
    }

    if (separatorMap[rest[0]]) {
      parts.push({ type: 'text', value: separatorMap[rest[0]] })
      rest = rest.slice(1)
      continue
    }

    if (separatorIconMap[rest[0]]) {
      parts.push({ type: 'icons', key: rest[0], ...separatorIconMap[rest[0]] })
      rest = rest.slice(1)
      continue
    }

    const tokenKey = greedyTokens.find(key => rest.startsWith(key))
    if (tokenKey && notationMap[tokenKey]) {
      parts.push({ type: 'icons', key: tokenKey, ...notationMap[tokenKey] })
      rest = rest.slice(tokenKey.length)
      continue
    }

    if (tokenKey) {
      parts.push({
        type: 'text',
        key: tokenKey,
        value: textMap[tokenKey],
        accent: tokenKey.startsWith('SA') || tokenKey.startsWith('DR') || tokenKey === 'DI',
        tone: tokenKey === 'PF' ? 'perfect' : ['360', '720'].includes(tokenKey) ? 'spin' : undefined,
      })
      rest = rest.slice(tokenKey.length)
      continue
    }

    const unknown = rest.match(/^[A-Z0-9]+/)?.[0] || rest[0]
    parts.push({ type: 'text', value: unknown })
    rest = rest.slice(unknown.length)
  }

  return parts
}

export function parseComboNotation(route) {
  return String(route || '')
    .split(/\s*>\s*/)
    .map(move => ({
      raw: move.trim(),
      parts: String(move || '')
        .replace(/4\s*溜\s*め\s*(?:[+＋]\s*)?6/g, ' CHARGE4 + 6 ')
        .replace(/2\s*溜\s*め\s*(?:[+＋]\s*)?8/g, ' CHARGE2 + 8 ')
        .replace(/\(\s*AGAINST\s+WALL\s*\)/gi, ' AGAINST_WALL ')
        .replace(/[（(]\s*HOLD\s+OK\s*[）)]/gi, ' HOLD_OK ')
        .replace(/[（(]\s*0\s*STOCK\s*[）)]/gi, ' STOCK_0 ')
        .replace(/[（(]\s*1\s*STOCK\s*[）)]/gi, ' STOCK_1 ')
        .replace(/[（(]\s*2\s*STOCK\s*[）)]/gi, ' STOCK_2 ')
        .trim()
        .split(/\s+/)
        .flatMap(parseToken),
    }))
    .filter(move => move.raw)
}
