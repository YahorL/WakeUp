// theme.jsx — design tokens + shared minimal UI atoms for the alarm app.
// Monochrome, calm, by-the-book. Exports to window.

const ACCENT_OPTIONS = [
  { name: 'Amber',     value: 'oklch(0.70 0.14 55)' },
  { name: 'Green',     value: 'oklch(0.66 0.15 150)' },
  { name: 'Ocean',     value: 'oklch(0.64 0.15 248)' },
  { name: 'Violet',    value: 'oklch(0.62 0.16 300)' },
  { name: 'Neon blue', value: 'oklch(0.70 0.22 250)' },
];

const FONTS = {
  grotesk: { label: 'Grotesk', ui: "'Hanken Grotesk', system-ui, sans-serif", display: "'Space Grotesk', sans-serif", num: "'Space Grotesk', sans-serif", numWeight: 500 },
  serif:   { label: 'Serif',   ui: "'Hanken Grotesk', system-ui, sans-serif", display: "'Instrument Serif', Georgia, serif", num: "'Instrument Serif', Georgia, serif", numWeight: 400 },
  mono:    { label: 'Mono',    ui: "'Space Grotesk', system-ui, sans-serif", display: "'JetBrains Mono', monospace", num: "'JetBrains Mono', monospace", numWeight: 500 },
};

// Build a token bundle from tweak state.
function makeTheme(t) {
  const dark = !!t.dark;
  const accent = t.accent || 'oklch(0.70 0.15 45)';
  const f = FONTS[(t.fontPairing || '').toLowerCase()] || FONTS.grotesk;
  return {
    dark, accent, fonts: f,
    onAccent: '#ffffff',
    accentSoft: dark ? 'color-mix(in oklch, ' + accent + ' 22%, transparent)'
                     : 'color-mix(in oklch, ' + accent + ' 14%, transparent)',
    bg:      dark ? '#0e0f10' : '#fafaf9',
    bg2:     dark ? '#161718' : '#f1f1ef',
    card:    dark ? '#1a1b1c' : '#ffffff',
    cardSunk:dark ? '#121314' : '#f4f4f2',
    text:    dark ? '#f2f2f0' : '#181a1b',
    textDim: dark ? '#8d9194' : '#74787b',
    textMute:dark ? '#5e6265' : '#a3a7aa',
    hair:    dark ? 'rgba(255,255,255,0.10)' : 'rgba(0,0,0,0.085)',
    hairStrong: dark ? 'rgba(255,255,255,0.18)' : 'rgba(0,0,0,0.16)',
  };
}

// ── Background pattern (dot grid + accent glow, gradient opacity) ──
// Renders two absolutely-positioned layers; place inside a position:relative box.
function PatternBg({ theme, accent, intensity = 1 }) {
  const dot = theme.dark
    ? `color-mix(in oklch, ${accent} 52%, transparent)`
    : `color-mix(in oklch, ${accent} 30%, transparent)`;
  const glow = theme.dark
    ? `color-mix(in oklch, ${accent} 24%, transparent)`
    : `color-mix(in oklch, ${accent} 14%, transparent)`;
  const mask = 'radial-gradient(135% 100% at 50% -12%, #000 0%, rgba(0,0,0,0.5) 36%, transparent 72%)';
  return (
    <React.Fragment>
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 0,
        background: `radial-gradient(130% 95% at 50% -16%, ${glow}, transparent 62%)`,
      }} />
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 0,
        backgroundImage: `radial-gradient(${dot} 1.1px, transparent 1.3px)`,
        backgroundSize: '21px 21px', backgroundPosition: '-2px -2px',
        maskImage: mask, WebkitMaskImage: mask, opacity: intensity,
      }} />
    </React.Fragment>
  );
}

// ── Icons (geometric only) ───────────────────────────────────
function Icon({ name, size = 24, color = 'currentColor', stroke = 2 }) {
  const p = { fill: 'none', stroke: color, strokeWidth: stroke, strokeLinecap: 'round', strokeLinejoin: 'round' };
  const paths = {
    plus: <g {...p}><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></g>,
    check: <polyline {...p} points="20 6 9 17 4 12"/>,
    clock: <g {...p}><circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 16 14"/></g>,
    chevL: <polyline {...p} points="15 18 9 12 15 6"/>,
    chevR: <polyline {...p} points="9 18 15 12 9 6"/>,
    chevD: <polyline {...p} points="6 9 12 15 18 9"/>,
    x: <g {...p}><line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/></g>,
    settings: <g {...p}><circle cx="12" cy="12" r="3"/><circle cx="12" cy="12" r="9"/></g>,
    repeat: <g {...p}><polyline points="17 2 21 6 17 10"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 22 3 18 7 14"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></g>,
    trash: <g {...p}><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></g>,
    bolt: <polygon {...p} points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>,
    sun: <g {...p}><circle cx="12" cy="12" r="4"/><line x1="12" y1="2" x2="12" y2="4"/><line x1="12" y1="20" x2="12" y2="22"/><line x1="4.2" y1="4.2" x2="5.6" y2="5.6"/><line x1="18.4" y1="18.4" x2="19.8" y2="19.8"/><line x1="2" y1="12" x2="4" y2="12"/><line x1="20" y1="12" x2="22" y2="12"/><line x1="4.2" y1="19.8" x2="5.6" y2="18.4"/><line x1="18.4" y1="5.6" x2="19.8" y2="4.2"/></g>,
    arrowDown: <g {...p}><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></g>,
    arrowUp: <g {...p}><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></g>,
    backspace: <g {...p}><path d="M21 4H8L1 12l7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z"/><line x1="18" y1="9" x2="12" y2="15"/><line x1="12" y1="9" x2="18" y2="15"/></g>,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" style={{ display: 'block', flexShrink: 0 }}>
      {paths[name] || null}
    </svg>
  );
}

// ── Switch ───────────────────────────────────────────────────
function Switch({ on, onChange, accent, theme, size = 1 }) {
  const w = 50 * size, h = 30 * size, k = (h - 8) * 1;
  return (
    <button onClick={() => onChange && onChange(!on)} style={{
      width: w, height: h, borderRadius: h, border: 'none', cursor: 'pointer', padding: 0,
      background: on ? accent : (theme.dark ? '#2c2e30' : '#d9d9d6'),
      position: 'relative', transition: 'background .22s ease', flexShrink: 0,
    }}>
      <span style={{
        position: 'absolute', top: 4, left: on ? w - k - 4 : 4, width: k, height: k,
        borderRadius: '50%', background: '#fff', transition: 'left .22s cubic-bezier(.3,1.3,.5,1)',
        boxShadow: '0 1px 3px rgba(0,0,0,.3)',
      }} />
    </button>
  );
}

// ── Stepper (− n +) ──────────────────────────────────────────
function Stepper({ value, min = 0, max = 99, step = 1, onChange, theme, suffix }) {
  const btn = (label, fn, disabled) => (
    <button onClick={() => !disabled && onChange(fn)} disabled={disabled} style={{
      width: 40, height: 40, borderRadius: '50%', border: `1.5px solid ${theme.hair}`,
      background: 'transparent', color: disabled ? theme.textMute : theme.text,
      display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: disabled ? 'default' : 'pointer',
      fontSize: 20, opacity: disabled ? 0.5 : 1,
    }}>{label}</button>
  );
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
      {btn('–', value - step, value <= min)}
      <div style={{ minWidth: 56, textAlign: 'center', fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight,
        fontSize: 26, color: theme.text, fontVariantNumeric: 'tabular-nums' }}>
        {value}{suffix ? <span style={{ fontSize: 14, color: theme.textDim, marginLeft: 2 }}>{suffix}</span> : null}
      </div>
      {btn('+', value + step, value >= max)}
    </div>
  );
}

// ── Number keypad ────────────────────────────────────────────
function NumPad({ onKey, theme, accent }) {
  const keys = ['1','2','3','4','5','6','7','8','9','clear','0','back'];
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10 }}>
      {keys.map(k => {
        const isNum = /\d/.test(k);
        return (
          <button key={k} onClick={() => onKey(k)} style={{
            height: 58, borderRadius: 16, border: 'none', cursor: 'pointer',
            background: isNum ? (theme.dark ? '#1f2122' : '#fff') : 'transparent',
            border: isNum ? `1px solid ${theme.hair}` : `1px solid transparent`,
            color: theme.text, fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight,
            fontSize: 26, display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontVariantNumeric: 'tabular-nums',
          }}>
            {k === 'back' ? <Icon name="backspace" size={24} color={theme.textDim} />
              : k === 'clear' ? <span style={{ fontSize: 14, color: theme.textDim, letterSpacing: 1 }}>CLR</span>
              : k}
          </button>
        );
      })}
    </div>
  );
}

// ── Small pill / badge ───────────────────────────────────────
function Badge({ children, theme, accent, solid }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      padding: '3px 9px', borderRadius: 100, fontSize: 12, letterSpacing: 0.2, whiteSpace: 'nowrap',
      fontFamily: theme.fonts.ui, fontWeight: 500,
      background: solid ? theme.accentSoft : (theme.dark ? '#202223' : '#f1f1ef'),
      color: solid ? accent : theme.textDim,
      border: `1px solid ${solid ? 'transparent' : theme.hair}`,
    }}>{children}</span>
  );
}

Object.assign(window, { FONTS, ACCENT_OPTIONS, makeTheme, PatternBg, Icon, Switch, Stepper, NumPad, Badge });
