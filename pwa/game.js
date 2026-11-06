// Yörünge Koşusu — PWA engine; course data comes from the Android source via ORBIT_DATA
(() => {
  const DATA = window.ORBIT_DATA;
  const LEVELS = DATA.levels.length;
  const FLOOR = 0, HOLE = 1, RED = 2, WALL = 3, BOOST = 4;
  const MENU = 0, RUN = 1, PAUSE = 2, FALL = 3, WIN = 4, CUSTOMIZE = 5, ENDING = 6;

  // Install offer: the browser's own prompt where it exists, short written steps where it does not.
  let installEvent = null, installPanel = false;
  const installed = () => navigator.standalone === true || ['standalone', 'fullscreen', 'minimal-ui'].some(m => window.matchMedia(`(display-mode: ${m})`).matches);
  const ua = navigator.userAgent;
  const isIos = /iPad|iPhone|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const isMacSafari = !isIos && /Macintosh/.test(ua) && /Safari/.test(ua) && !/Chrome|Chromium|Edg|OPR|Firefox/.test(ua);
  const isAndroidFirefox = /Android/.test(ua) && /Firefox/.test(ua);
  const INSTALL_STEPS = isIos
    ? { title: 'Ana ekrana ekle', steps: ['Tarayıcının Paylaş düğmesine dokun', 'Ana Ekrana Ekle seçeneğini seç', 'Sağ üstten Ekle düğmesine dokun'] }
    : isMacSafari
      ? { title: "Dock'a ekle", steps: ['Safari menüsünde Dosya bölümünü aç', "Dock'a Ekle seçeneğini seç", 'Açılan pencerede Ekle düğmesine bas'] }
      : isAndroidFirefox
        ? { title: 'Ana ekrana ekle', steps: ['Firefox menüsünü aç', 'Yükle veya Ana Ekrana Ekle seçeneğini seç', 'Açılan pencerede onayla'] }
        : null;
  const canOfferInstall = () => !installed() && (installEvent !== null || INSTALL_STEPS !== null);
  window.addEventListener('beforeinstallprompt', e => { e.preventDefault(); installEvent = e; });
  function offerInstall() {
    if (!installEvent) { installPanel = true; return; }
    const offer = installEvent;
    installEvent = null;
    offer.prompt();
  }

  const canvas = document.getElementById('game');
  const ctx = canvas.getContext('2d');

  // Persistent storage matching Android SharedPreferences("orbit")
  const store = {
    getInt: (k, d) => { const v = localStorage.getItem('orbit_' + k); return v === null ? d : parseInt(v, 10); },
    setInt: (k, v) => localStorage.setItem('orbit_' + k, String(v | 0)),
    getBool: (k, d) => { const v = localStorage.getItem('orbit_' + k); return v === null ? d : v === 'true'; },
    setBool: (k, v) => localStorage.setItem('orbit_' + k, v ? 'true' : 'false')
  };

  // Web Audio API sound & music system (needed for iOS Safari low-latency multi-channel audio)
  const MUSIC_FILES = ['orbit.wav','ion.wav','drift.wav','turkish_march.wav','ode.wav','waltz.wav','pulsar.wav','nebula.wav','elise.wav'];
  const MUSIC_NAMES = ['Yörünge','İyon','Gece Uçuşu','Türk Marşı','Neşeye Övgü','Yıldız Valsi','Süpernova','Kristal','Für Elise'];
  const SFX_FILES = ['jump.wav','turn.wav','hit.wav','finish.wav','coin.wav','frenzy.wav'];

  const audio = {
    ctx: null,
    musicGain: null,
    musicSource: null,
    musicBuffers: {},
    sfxBuffers: [],
    musicOn: store.getBool('music', true),
    effectsOn: store.getBool('effects', true),
    musicIndex: Math.max(0, Math.min(MUSIC_FILES.length - 1, store.getInt('musicIndex', 0))),
    duckTimeout: null,
    unlocked: false,
    init() {
      if (this.ctx) return;
      const AC = window.AudioContext || window.webkitAudioContext;
      this.ctx = new AC();
      this.musicGain = this.ctx.createGain();
      this.musicGain.gain.value = 0.32;
      this.musicGain.connect(this.ctx.destination);
      SFX_FILES.forEach((f, i) => {
        fetch('assets/' + f).then(r => r.arrayBuffer()).then(b => this.ctx.decodeAudioData(b)).then(buf => { this.sfxBuffers[i] = buf; }).catch(() => {});
      });
      this.loadTrack(this.musicIndex);
    },
    unlock() {
      this.init();
      if (this.ctx && this.ctx.state === 'suspended') this.ctx.resume();
      if (!this.unlocked) {
        this.unlocked = true;
        this.syncMusic();
      }
    },
    loadTrack(idx) {
      this.musicIndex = idx;
      if (this.musicBuffers[idx]) {
        this.syncMusic();
        return;
      }
      fetch('assets/' + MUSIC_FILES[idx]).then(r => r.arrayBuffer()).then(b => this.ctx.decodeAudioData(b)).then(buf => {
        this.musicBuffers[idx] = buf;
        if (this.musicIndex === idx) this.syncMusic();
      }).catch(() => {});
    },
    syncMusic() {
      if (!this.ctx) return;
      if (this.musicSource) {
        try { this.musicSource.stop(); } catch (e) {}
        this.musicSource.disconnect();
        this.musicSource = null;
      }
      if (this.musicOn && this.musicBuffers[this.musicIndex] && !document.hidden) {
        const src = this.ctx.createBufferSource();
        src.buffer = this.musicBuffers[this.musicIndex];
        src.loop = true;
        src.connect(this.musicGain);
        src.start(0);
        this.musicSource = src;
      }
    },
    nextMusic() {
      this.init();
      this.musicIndex = (this.musicIndex + 1) % MUSIC_FILES.length;
      store.setInt('musicIndex', this.musicIndex);
      this.loadTrack(this.musicIndex);
    },
    toggleMusic() {
      this.init();
      this.musicOn = !this.musicOn;
      store.setBool('music', this.musicOn);
      this.syncMusic();
    },
    toggleEffects() {
      this.init();
      this.effectsOn = !this.effectsOn;
      store.setBool('effects', this.effectsOn);
    },
    play(id) {
      if (!this.effectsOn || !this.ctx || !this.sfxBuffers[id]) return;
      if (this.ctx.state === 'suspended') this.ctx.resume();
      const src = this.ctx.createBufferSource();
      const g = this.ctx.createGain();
      src.buffer = this.sfxBuffers[id];
      g.gain.value = id === 2 ? 0.64 : 0.55;
      src.connect(g);
      g.connect(this.ctx.destination);
      src.start(0);
      if (id === 2 && this.musicGain) {
        this.musicGain.gain.value = 0.14;
        clearTimeout(this.duckTimeout);
        this.duckTimeout = setTimeout(() => { if (this.musicGain) this.musicGain.gain.value = 0.32; }, 850);
      }
    },
    musicLabel() {
      return `Parça: ${MUSIC_NAMES[this.musicIndex]} · ${this.musicIndex + 1}/${MUSIC_FILES.length}`;
    }
  };

  document.addEventListener('visibilitychange', () => audio.syncMusic());

  const wrap = lane => ((lane % 8) + 8) % 8;

  // Runner physics matching Runner.java
  const characterColors = ['#ffcd77', '#9bdeef', '#d7a8ed'];
  const characterNames = ['Keşif', 'Nova', 'Atlas', 'Özel'];
  let customColorIndex = store.getInt('customColor', 3);
  let customHeadType = store.getInt('customHead', 3);
  let customTrait = store.getInt('customTrait', 3);

  const Runner = {
    effectiveTrait: c => c === 3 ? customTrait : c,
    color: c => c === 3 ? DATA.palette[((customColorIndex % DATA.palette.length) + DATA.palette.length) % DATA.palette.length] : characterColors[Math.max(0, Math.min(2, c))],
    headType: c => c === 3 ? customHeadType : c,
    speed: (c, lvl) => {
      const t = Runner.effectiveTrait(c);
      return DATA.levels[lvl].speed * (t === 2 ? 1.10 : 1);
    },
    jumpSeconds: c => {
      const t = Runner.effectiveTrait(c);
      return DATA.jumpSeconds * (t === 1 ? 1.10 : t === 3 ? 1.15 : 1);
    },
    height: (c, air) => {
      if (air <= 0) return 0;
      const t = Runner.effectiveTrait(c);
      const mult = t === 1 ? 1.25 : 1;
      return Math.sin(Math.max(0, 1 - air / Runner.jumpSeconds(c)) * Math.PI) * mult;
    },
    lethal: (c, tile, air) => tile === WALL ? Runner.height(c, air) < 0.25 : (tile === HOLE || tile === RED) && air <= 0,
    description: c => {
      const t = Runner.effectiveTrait(c);
      return t === 1 ? '%25 yüksek zıplama' : t === 2 ? '%10 hızlı koşu' : t === 3 ? '%15 uzun süzülme' : 'Dengeli koşu ve zıplama';
    }
  };

  // Collapse wave matching Collapse.java
  class Collapse {
    constructor(lvl) {
      this.level = lvl;
      const rows = DATA.levels[lvl].length;
      this.tiles = DATA.levels[lvl].tiles;
      this.times = Array.from({ length: rows }, () => Array(8).fill(Infinity));
    }
    red(r, l) { return r >= 0 && r < this.tiles.length && this.tiles[r][wrap(l)] === RED; }
    trigger(row, lane, now) {
      lane = wrap(lane);
      if (!this.red(row, lane) || this.times[row][lane] <= now) return false;
      this.times[row][lane] = now;
      const q = [{ row, lane, time: now }];
      while (q.length) {
        q.sort((a, b) => a.time - b.time);
        const n = q.shift();
        if (n.time > this.times[n.row][n.lane]) continue;
        const dirs = [[n.row - 1, n.lane], [n.row + 1, n.lane], [n.row, n.lane - 1], [n.row, n.lane + 1]];
        for (const [r, rawL] of dirs) {
          const l = wrap(rawL);
          if (!this.red(r, l)) continue;
          const arrival = n.time + 0.11 + ((r * 17 + l * 13 + Math.max(this.level, 0)) % 7) * 0.01;
          if (arrival < this.times[r][l]) {
            this.times[r][l] = arrival;
            q.push({ row: r, lane: l, time: arrival });
          }
        }
      }
      return true;
    }
    age(r, l, now) {
      if (!this.red(r, l)) return -1;
      const t = now - this.times[r][wrap(l)];
      return t < 0 ? -1 : t;
    }
    canEscape(r, l, now) {
      const a = this.age(r, l, now);
      return a >= 0 && a < DATA.escapeSeconds;
    }
    static drop(age) { return age <= 0 ? 0 : age * age * 600; }
  }

  // Endless mode matching Endless.java
  class Endless {
    constructor() {
      this.WINDOW = 160;
      this.tiles = Array.from({ length: 160 }, () => Array(8).fill(FLOOR));
      this.collapseTimes = Array.from({ length: 160 }, () => Array(8).fill(Infinity));
      this.shieldRow = Array(160).fill(false);
      this.shieldLane = Array(160).fill(0);
      this.crystalLane = Array(160).fill(-1);
      this.patrolRow = Array(160).fill(false);
      this.patrolBase = Array(160).fill(0);
      this.generatedUpTo = -1;
      this.cursor = 0;
      this.currentRoute = 0;
      this.encounterCount = 0;
      this.PALETTES = [[37,94,115],[89,75,131],[52,91,153],[134,83,50],[50,108,86],[102,94,135],[120,97,65],[45,106,110]];
      this.ensure(60);
    }
    clearRow(r) {
      const idx = r % this.WINDOW;
      this.tiles[idx].fill(FLOOR);
      this.collapseTimes[idx].fill(Infinity);
      this.shieldRow[idx] = false;
      this.crystalLane[idx] = -1;
      this.patrolRow[idx] = false;
    }
    ensure(target) {
      while (this.generatedUpTo < target) {
        if (this.cursor === 0) {
          for (let r = 0; r < 6; r++) {
            this.clearRow(r);
            if (r === 2) this.tiles[r].fill(WALL);
          }
          this.generatedUpTo = 5;
          this.cursor = 6;
        } else {
          this.generateEncounter();
        }
      }
    }
    generateEncounter() {
      this.encounterCount++;
      const start = this.cursor;
      let ring, dangerStart, dangerEnd;
      if (Math.random() < 0.5) {
        ring = this.cursor + 2 + ((Math.random() * 4) | 0);
        dangerStart = ring + 1;
        dangerEnd = ring + 4 + ((Math.random() * 4) | 0);
      } else {
        dangerStart = this.cursor + 3 + ((Math.random() * 3) | 0);
        dangerEnd = dangerStart + 3 + ((Math.random() * 4) | 0);
        ring = dangerEnd + 2 + ((Math.random() * 2) | 0);
      }
      const squeeze = Math.max(ring, dangerEnd);
      const end = squeeze + 5 + ((Math.random() * 4) | 0);
      const prev = this.currentRoute;
      const move = 1 + ((Math.random() * 3) | 0);
      this.currentRoute = wrap(this.currentRoute + (Math.random() < 0.5 ? move : -move));
      const width = this.encounterCount >= 12 && Math.random() < 0.66 ? 1 : 2;
      for (let r = start; r < end; r++) this.clearRow(r);
      for (let l = 0; l < 8; l++) this.tiles[ring % this.WINDOW][l] = Math.random() < 0.33 ? RED : HOLE;
      for (let r = dangerStart; r <= dangerEnd; r++) {
        const idx = r % this.WINDOW;
        for (let l = 0; l < 8; l++) if (wrap(l - this.currentRoute) >= width) this.tiles[idx][l] = Math.random() < 0.5 ? HOLE : RED;
      }
      for (let r = start; r < ring - 1; r++) {
        const idx = r % this.WINDOW;
        for (let l = 0; l < 8; l++) {
          if (wrap(l - this.currentRoute) > 2 && wrap(this.currentRoute - l) > 2 && wrap(l - prev) > 2 && wrap(prev - l) > 2 && Math.random() < 0.33) {
            this.tiles[idx][l] = Math.random() < 0.5 ? HOLE : RED;
          }
        }
      }
      if (ring - 2 > start && Math.random() < 0.6) {
        const bLane = wrap(this.currentRoute + (Math.random() < 0.5 ? 1 : -1));
        if (this.tiles[(ring - 2) % this.WINDOW][bLane] === FLOOR) this.tiles[(ring - 2) % this.WINDOW][bLane] = BOOST;
      }
      for (let r = dangerStart + 1; r <= Math.min(dangerEnd, dangerStart + 3); r++) {
        if (this.tiles[r % this.WINDOW][this.currentRoute] === FLOOR) this.crystalLane[r % this.WINDOW] = this.currentRoute;
      }
      if (this.encounterCount % 4 === 2 && squeeze + 2 < end - 1) {
        const sRow = squeeze + 2, sLane = wrap(this.currentRoute + 1);
        if (this.tiles[sRow % this.WINDOW][sLane] === FLOOR) {
          this.shieldRow[sRow % this.WINDOW] = true;
          this.shieldLane[sRow % this.WINDOW] = sLane;
        }
      }
      if (this.encounterCount >= 4 && Math.random() < 0.33 && squeeze + 3 < end - 1) {
        const pRow = squeeze + 2;
        this.patrolRow[pRow % this.WINDOW] = true;
        this.patrolBase[pRow % this.WINDOW] = this.currentRoute;
      }
      this.generatedUpTo = end - 1;
      this.cursor = end;
    }
    tile(r, l) { if (r < 0) return FLOOR; this.ensure(r + 32); return this.tiles[r % this.WINDOW][wrap(l)]; }
    rgb(r) { const s = Math.max(0, (r / 28) | 0); return this.PALETTES[(s * 3 + (s >> 1)) % this.PALETTES.length]; }
    speed(dist) { return 9.0 + Math.min(60, dist / 45.0) * 0.32; }
    hasShield(r, l) { if (r < 0) return false; this.ensure(r + 8); const idx = r % this.WINDOW; return this.shieldRow[idx] && this.shieldLane[idx] === wrap(l); }
    consumeShield(r) { if (r >= 0) this.shieldRow[r % this.WINDOW] = false; }
    hasCrystal(r, l) { if (r < 0) return false; this.ensure(r + 8); return this.crystalLane[r % this.WINDOW] === wrap(l); }
    consumeCrystal(r) { if (r >= 0) this.crystalLane[r % this.WINDOW] = -1; }
    hasPatrol(r) { if (r < 0) return false; this.ensure(r + 8); return this.patrolRow[r % this.WINDOW]; }
    patrolLane(r, elapsed) { return this.patrolBase[r % this.WINDOW] + Math.sin(elapsed * 2.4 + r * 0.3) * 1.35; }
    triggerCollapse(row, lane, now) {
      if (row < 0) return false;
      this.ensure(row + 12);
      lane = wrap(lane);
      if (this.tile(row, lane) !== RED || this.collapseTimes[row % this.WINDOW][lane] <= now) return false;
      this.collapseTimes[row % this.WINDOW][lane] = now;
      for (let dr = -2; dr <= 2; dr++) for (let dl = -2; dl <= 2; dl++) {
        const dist = Math.abs(dr) + Math.abs(dl);
        if (dist === 0 || dist > 2) continue;
        const r = row + dr, l = wrap(lane + dl);
        if (r >= 0 && this.tile(r, l) === RED) {
          const arr = now + dist * 0.13 + ((r * 17 + l * 13) % 5) * 0.01;
          if (arr < this.collapseTimes[r % this.WINDOW][l]) this.collapseTimes[r % this.WINDOW][l] = arr;
        }
      }
      return true;
    }
    collapseAge(r, l, now) {
      if (r < 0 || this.tile(r, l) !== RED) return -1;
      const t = now - this.collapseTimes[r % this.WINDOW][wrap(l)];
      return t < 0 ? -1 : t;
    }
    canEscape(r, l, now) { const a = this.collapseAge(r, l, now); return a >= 0 && a < DATA.escapeSeconds; }
  }

  // Game state
  let unlocked = Math.max(0, Math.min(LEVELS - 1, store.getInt('unlocked', 0)));
  let level = unlocked;
  let character = Math.max(0, Math.min(3, store.getInt('character', 0)));
  let bestEndlessMeters = store.getInt('bestEndless', 0);
  let totalCrystals = store.getInt('totalCrystals', 0);
  let flawlessSet = new Set((localStorage.getItem('orbit_flawless') || '').split(',').filter(Boolean).map(Number));
  let completedSet = new Set((localStorage.getItem('orbit_completed') || '').split(',').filter(Boolean).map(Number));
  for (let i = 0; i < unlocked; i++) completedSet.add(i);
  let signalsSet = new Set((localStorage.getItem('orbit_signals') || '').split(',').filter(Boolean).map(Number));

  let state = MENU, endlessMode = false, newRecord = false, shieldActive = false, shieldTaken = false, flawlessRun = true;
  let currentMeters = 0, comboMultiplier = 1, frenzyCharge = 0;
  let boostTimer = 0, dustTime = 0, shieldBreakTime = 0, frenzyTimer = 0, calloutTime = 0, prevAir = 0, headTurn = 0;
  let calloutText = '', calloutColor = '#ffcd77', dustX = 0, dustY = 0, fallCamera = 0, jumpFromDrop = 0;
  // Visual-only animation state; never read by physics or collisions.
  const POSE_RUN = 0, POSE_IDLE = 1, POSE_WAVE = 2, POSE_FALL = 3, POSE_CHEER = 4;
  const DUST_COLORS = ['#bfe7dd', '#8fc9bd'], CONFETTI_COLORS = ['#ffcd77', '#67dfcc', '#42e8ff', '#ff7a66', '#d7a8ed'];
  let animClock = 0, stride = 0, squash = 0, squashV = 0, airPose = 0, leanAnim = 0, antenna = 0, antennaV = 0;
  let selectPulse = 0, dizzyTime = 0, shakeTime = 0, hitFlash = 0, robotX = 0, robotY = 0, wasAirborne = false, animPrevState = MENU;
  const particles = [];
  // Final scene: the robot walks into the light, signals, and the light answers.
  const ENDING_SECONDS = 3.6, ENDING_WALK = 1.05;
  let endingTime = 0, signalGlow = 0, whiteOut = 0;
  // A newly activated build reloads the game the next time the player is on the main menu, never during a run.
  let updateReady = false;
  if ('serviceWorker' in navigator) navigator.serviceWorker.addEventListener('controllerchange', () => { updateReady = true; });
  let levelScroll = 0, dragStart = 0, scrollStart = 0, stripDragging = false, stripMoved = false;
  let menuHint = 'Bölümleri sağa / sola kaydır', failure = 'Boşluğa düştün. Zıpla ya da duvar değiştir.';
  let restartNotice = 0, completionNotice = 0, signalTime = 0, signalText = '', completedLevel = -1;
  let distance = 0, lane = 0, cameraLane = 0, targetLane = 0, air = 0, fall = 0, elapsed = 0, repeat = 0, last = 0;
  let collapse = new Collapse(level), endless = null, crystalTaken = new Set();
  const touches = new Map();
  let keyLeft = false, keyRight = false, keyJump = false, overlayChoice = 0;

  let w = 0, h = 0, u = 1, cx = 0, cy = 0, focal = 0;
  const rect = () => ({ left: 0, top: 0, right: 0, bottom: 0, set(l, t, r, b) { this.left = l; this.top = t; this.right = r; this.bottom = b; }, width() { return this.right - this.left; }, height() { return this.bottom - this.top; }, centerX() { return (this.left + this.right) / 2; }, centerY() { return (this.top + this.bottom) / 2; }, contains(x, y) { return x >= this.left && x <= this.right && y >= this.top && y <= this.bottom; } });
  const leftBtn = rect(), rightBtn = rect(), jumpBtn = rect(), pauseBtn = rect(), primaryBtn = rect(), secondaryBtn = rect();
  const inGameMusic = rect(), trackButton = rect(), musicButton = rect(), effectsButton = rect(), menuStart = rect(), endlessButton = rect(), levelStrip = rect();
  const characterButtons = [rect(), rect(), rect(), rect()];
  const customColorBtns = Array.from({ length: 6 }, rect);
  const customHeadBtns = Array.from({ length: 4 }, rect);
  const customTraitBtns = Array.from({ length: 4 }, rect);
  const customSaveBtn = rect();
  const installBtn = rect();

  function maxScroll() { return Math.max(0, LEVELS * 88 * u - 12 * u - levelStrip.width()); }
  function centerSelection() { levelScroll = Math.max(0, Math.min(maxScroll(), level * 88 * u - levelStrip.width() / 2 + 38 * u)); }

  function resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    w = window.innerWidth * dpr;
    h = window.innerHeight * dpr;
    canvas.width = w;
    canvas.height = h;
    u = Math.min(w / 1100, h / 650);
    cx = w * 0.5;
    cy = h * 0.40;
    focal = h * 0.88;
    const s = 98 * u, m = 32 * u;
    leftBtn.set(m, h - m - s, m + s, h - m);
    rightBtn.set(m + s + 18 * u, h - m - s, m + 2 * s + 18 * u, h - m);
    jumpBtn.set(w - m - 180 * u, h - m - s, w - m, h - m);
    pauseBtn.set(w - m - 64 * u, m, w - m, m + 58 * u);
    primaryBtn.set(cx - 175 * u, h * 0.61, cx + 175 * u, h * 0.61 + 66 * u);
    secondaryBtn.set(cx - 175 * u, h * 0.61 + 82 * u, cx + 175 * u, h * 0.61 + 140 * u);
    trackButton.set(28 * u, 20 * u, 350 * u, 72 * u);
    inGameMusic.set(w - 280 * u, 32 * u, w - 114 * u, 90 * u);
    musicButton.set(w - 208 * u, 20 * u, w - 28 * u, 72 * u);
    effectsButton.set(w - 400 * u, 20 * u, w - 220 * u, 72 * u);
    menuStart.set(cx - 310 * u, h * 0.51, cx - 10 * u, h * 0.51 + 62 * u);
    endlessButton.set(cx + 10 * u, h * 0.51, cx + 310 * u, h * 0.51 + 62 * u);
    for (let i = 0; i < 4; i++) { const x = cx + (i - 1.5) * 175 * u; characterButtons[i].set(x - 80 * u, h * 0.31, x + 80 * u, h * 0.31 + 114 * u); }
    for (let i = 0; i < 6; i++) { const x = cx - 95 * u + i * 66 * u; customColorBtns[i].set(x - 26 * u, h * 0.27, x + 26 * u, h * 0.27 + 52 * u); }
    for (let i = 0; i < 4; i++) { const x = cx - 95 * u + i * 112 * u; customHeadBtns[i].set(x - 50 * u, h * 0.44, x + 50 * u, h * 0.44 + 52 * u); }
    for (let i = 0; i < 4; i++) { const x = cx - 95 * u + i * 112 * u; customTraitBtns[i].set(x - 50 * u, h * 0.61, x + 50 * u, h * 0.61 + 52 * u); }
    customSaveBtn.set(cx - 160 * u, h * 0.79, cx + 160 * u, h * 0.79 + 62 * u);
    installBtn.set(cx - 75 * u, 20 * u, cx + 75 * u, 72 * u);
    levelStrip.set(32 * u, h - 136 * u, w - 32 * u, h - 54 * u);
    centerSelection();
  }
  window.addEventListener('resize', resize);
  window.addEventListener('appinstalled', () => { installEvent = null; installPanel = false; menuHint = 'Oyun yüklendi'; });
  resize();

  function start() {
    endlessMode = false;
    level = Math.max(0, Math.min(unlocked, level));
    distance = 0; lane = 0; cameraLane = 0; targetLane = 0;
    collapse = new Collapse(level);
    fallCamera = 0; jumpFromDrop = 0; air = Runner.jumpSeconds(character); prevAir = air; fall = 0;
    elapsed = 0; repeat = 0;
    boostTimer = 0; dustTime = 0; shieldBreakTime = 0; frenzyTimer = 0; calloutTime = 0;
    comboMultiplier = 1; frenzyCharge = 0; crystalTaken.clear();
    shieldActive = false; shieldTaken = false;
    touches.clear(); keyLeft = keyRight = keyJump = false;
    state = RUN;
  }

  function startEndless() {
    endlessMode = true; newRecord = false; currentMeters = 0;
    endless = new Endless();
    distance = 0; lane = 0; cameraLane = 0; targetLane = 0;
    fallCamera = 0; jumpFromDrop = 0; air = Runner.jumpSeconds(character); prevAir = air; fall = 0;
    elapsed = 0; repeat = 0;
    boostTimer = 0; dustTime = 0; shieldBreakTime = 0; frenzyTimer = 0; calloutTime = 0;
    comboMultiplier = 1; frenzyCharge = 0;
    shieldActive = true; shieldTaken = false;
    touches.clear(); keyLeft = keyRight = keyJump = false;
    state = RUN;
  }

  function held(action) {
    for (const v of touches.values()) if (v === action) return true;
    return action === 1 ? keyLeft : action === 2 ? keyRight : keyJump;
  }
  function turn(dir) { targetLane += dir; repeat = 0.22; audio.play(1); }
  function supportDrop() {
    if (state === FALL) return Collapse.drop(fall);
    if (air > 0) return jumpFromDrop * Math.max(0, 1 - (Runner.jumpSeconds(character) - air) / 0.16);
    const r = ((distance + 5) / 4) | 0, l = Math.round(lane);
    if (endlessMode) return endless ? Collapse.drop(endless.collapseAge(r, l, elapsed)) : 0;
    return collapse ? Collapse.drop(collapse.age(r, l, elapsed)) : 0;
  }
  function leap() {
    if (state === RUN && air <= 0) {
      jumpFromDrop = supportDrop();
      air = Runner.jumpSeconds(character);
      audio.play(0);
    }
  }
  function currentSpeed() {
    const t = Runner.effectiveTrait(character);
    const mult = t === 2 ? 1.10 : 1;
    const base = endlessMode ? endless.speed(distance) * mult : Runner.speed(character, level);
    if (frenzyTimer > 0) return base * 1.28;
    return boostTimer > 0 ? base * 1.22 : base;
  }
  function showCallout(msg, col) { calloutText = msg; calloutColor = col; calloutTime = 1.15; }
  function triggerDust() {
    const angle = (lane - cameraLane) * Math.PI / 4;
    dustX = cx + Math.sin(angle) * 3 * focal / 8;
    dustY = cy + Math.cos(angle) * 3 * focal / 8 * 0.924;
    dustTime = 0.28;
  }
  function consumeShieldRescue() {
    shieldActive = false; shieldBreakTime = 0.55;
    jumpFromDrop = 0; air = Runner.jumpSeconds(character) * 1.12;
    audio.play(0); triggerDust();
  }
  function collectCrystal() {
    totalCrystals += comboMultiplier;
    store.setInt('totalCrystals', totalCrystals);
    audio.play(4);
    if (frenzyTimer <= 0) {
      frenzyCharge++;
      if (frenzyCharge % 5 === 0 && comboMultiplier < 5) {
        comboMultiplier++;
        showCallout(`ÇARPAN x${comboMultiplier}!`, '#ffcd77');
      }
      if (frenzyCharge >= 12) {
        frenzyCharge = 0; frenzyTimer = 4.2; shieldActive = true;
        audio.play(5); showCallout('YÖRÜNGE ÇILGINLIĞI!', '#ff7a66');
      }
    }
  }

  function enemyPos(e, time) {
    const phase = (time / e.period + e.section * 0.31) % 1;
    const prog = phase < 0.5 ? phase * 2 : 2 - phase * 2;
    return (e.first + prog * (e.last - e.first)) * 4;
  }

  function burst(x, y, count, speed, colors) {
    for (let i = 0; i < count; i++) {
      const a = Math.random() * Math.PI * 2, v = speed * (0.35 + Math.random() * 0.65), life = 0.45 + Math.random() * 0.45;
      particles.push({ x, y, vx: Math.cos(a) * v, vy: Math.sin(a) * v - speed * 0.35, life, max: life, size: (2.5 + Math.random() * 3.5) * u, color: colors[i % colors.length] });
    }
    if (particles.length > 64) particles.splice(0, particles.length - 64);
  }
  function crash() {
    shakeTime = 0.42; hitFlash = 0.28;
    const body = Runner.color(character);
    burst(robotX, robotY - 40 * u, 18, 320 * u, [body, shade(body, 0.72), '#e9f6f3', '#163342']);
  }
  function updateAnim(dt) {
    if (state === PAUSE) return;
    animClock += dt;
    const airborne = (state === RUN || state === ENDING) && air > 0;
    if (state === ENDING) stride += dt * 11 * Math.max(0, 1 - endingTime / ENDING_WALK);
    if (state === RUN) {
      const trait = Runner.effectiveTrait(character), before = stride;
      if (!airborne) stride += dt * Math.min(15, 7 + currentSpeed() * 0.3) * (trait === 2 ? 0.85 : trait === 1 ? 0.92 : trait === 3 ? 0.8 : 1);
      if (trait === 2 && Math.floor(stride / Math.PI) !== Math.floor(before / Math.PI)) burst(robotX, robotY, 2, 46 * u, DUST_COLORS);
      if (airborne && !wasAirborne) { squashV -= 2.6; antennaV += Math.sin(animClock * 7) > 0 ? 34 : -34; }
    }
    if ((state === RUN || state === ENDING) && !airborne && wasAirborne) { squashV += 3.4; antennaV += Math.sin(animClock * 7) > 0 ? 46 : -46; }
    if (state === FALL && animPrevState === RUN) shakeTime = Math.max(shakeTime, 0.22);
    animPrevState = state; wasAirborne = airborne;
    squashV += (-squash * 180 - squashV * 14) * dt; squash = Math.max(-0.32, Math.min(0.32, squash + squashV * dt));
    airPose += ((airborne ? 1 : 0) - airPose) * Math.min(1, dt * 14);
    leanAnim += ((targetLane - lane) - leanAnim) * Math.min(1, dt * 10);
    antennaV += ((-leanAnim * 9 - antenna) * 140 - antennaV * 9) * dt; antenna = Math.max(-9, Math.min(9, antenna + antennaV * dt));
    selectPulse = Math.max(0, selectPulse - dt); dizzyTime = Math.max(0, dizzyTime - dt);
    shakeTime = Math.max(0, shakeTime - dt); hitFlash = Math.max(0, hitFlash - dt); whiteOut = Math.max(0, whiteOut - dt);
    for (let i = particles.length - 1; i >= 0; i--) {
      const q = particles[i];
      q.life -= dt; q.vy += 620 * u * dt; q.x += q.vx * dt; q.y += q.vy * dt;
      if (q.life <= 0) particles.splice(i, 1);
    }
  }
  function drawParticles() {
    for (const q of particles) {
      ctx.globalAlpha = Math.min(1, q.life / q.max * 1.6);
      ctx.fillStyle = q.color;
      ctx.fillRect(q.x - q.size, q.y - q.size * 0.7, q.size * 2, q.size * 1.4);
    }
    ctx.globalAlpha = 1;
  }

  function update(dt) {
    if (state === RUN || state === FALL || state === CUSTOMIZE || state === ENDING) elapsed += dt;
    restartNotice = Math.max(0, restartNotice - dt);
    completionNotice = Math.max(0, completionNotice - dt);
    signalTime = Math.max(0, signalTime - dt);
    boostTimer = Math.max(0, boostTimer - dt);
    dustTime = Math.max(0, dustTime - dt);
    shieldBreakTime = Math.max(0, shieldBreakTime - dt);
    frenzyTimer = Math.max(0, frenzyTimer - dt);
    calloutTime = Math.max(0, calloutTime - dt);
    const cycle = elapsed % 7.2;
    const wantLook = state === ENDING ? endingTime > 2.4 : state === CUSTOMIZE || state === FALL || dizzyTime > 0 || completionNotice > 1.1 || calloutTime > 0 || frenzyTimer > 0 || (cycle > 4.5 && cycle < 6.1);
    const speedTurn = wantLook ? 5.5 : 4.2;
    if (wantLook) headTurn = Math.min(1, headTurn + dt * speedTurn);
    else headTurn = Math.max(0, headTurn - dt * speedTurn);

    if (state === RUN) {
      repeat -= dt;
      const dir = (held(2) ? 1 : 0) - (held(1) ? 1 : 0);
      if (dir !== 0 && repeat <= 0) turn(dir);
      lane += (targetLane - lane) * Math.min(1, dt * 16);
      cameraLane += (lane - cameraLane) * Math.min(1, dt * 8);
      prevAir = air;
      if (air > 0) {
        air = Math.max(0, air - dt);
        if (air === 0 && prevAir > 0) triggerDust();
      }
      distance += dt * currentSpeed();
      const row = ((distance + 5) / 4) | 0;
      const curLane = wrap(Math.round(lane));

      if (endlessMode) {
        currentMeters = (distance * 1.2) | 0;
        if (currentMeters > bestEndlessMeters) {
          bestEndlessMeters = currentMeters; newRecord = true;
          store.setInt('bestEndless', bestEndlessMeters);
        }
        if (!shieldActive && endless.hasShield(row, curLane) && Runner.height(character, air) < 0.65) {
          shieldActive = true; endless.consumeShield(row); audio.play(0);
        }
        if (endless.hasCrystal(row, curLane) && Runner.height(character, air) < 0.75) {
          endless.consumeCrystal(row); collectCrystal();
        }
        if (endless.hasPatrol(row)) {
          const pLane = endless.patrolLane(row, elapsed);
          let diff = Math.abs(lane - pLane) % 8; diff = Math.min(diff, 8 - diff);
          if (diff > 0.42 && diff < 1.25 && Math.abs(row * 4 + 2 - (distance + 5)) < 0.45 && calloutTime <= 0) {
            totalCrystals += 2 * comboMultiplier; store.setInt('totalCrystals', totalCrystals);
            showCallout(`TEĞET GEÇİŞ! +${2 * comboMultiplier}`, '#67dfcc');
          }
          if (diff < 0.42 && Math.abs(row * 4 + 2 - (distance + 5)) < 1.3 && Runner.height(character, air) < 0.65) {
            if (frenzyTimer > 0 || shieldActive) {
              if (frenzyTimer <= 0) consumeShieldRescue(); else { jumpFromDrop = 0; air = Runner.jumpSeconds(character); audio.play(0); }
              return;
            }
            failure = `Uzaylı devriyesine çarptın! (${currentMeters} m)`;
            audio.play(2); state = FALL; fall = 0; touches.clear(); crash(); return;
          }
        }
        const tile = endless.tile(row, curLane);
        if (tile === BOOST && air <= 0.08) {
          boostTimer = 0.65; jumpFromDrop = 0; air = Runner.jumpSeconds(character) * 1.20;
          audio.play(0); triggerDust(); showCallout('SÜPER SIÇRAMA!', '#42e8ff'); return;
        }
        let fatal = Runner.lethal(character, tile, air);
        if (tile === RED && air <= 0) {
          if (endless.triggerCollapse(row, curLane, elapsed)) audio.play(1);
          fatal = !endless.canEscape(row, curLane, elapsed);
        }
        if (fatal) {
          if (frenzyTimer > 0) { jumpFromDrop = 0; air = Runner.jumpSeconds(character) * 1.05; audio.play(0); triggerDust(); return; }
          if (shieldActive) { consumeShieldRescue(); return; }
          failure = `${tile === RED ? 'Kırmızı plaka çöktü! ' : 'Boşluğa düştün! '}Mesafe: ${currentMeters} m`;
          audio.play(2); state = FALL; overlayChoice = 0; keyLeft = keyRight = keyJump = false; fall = tile === RED ? Math.min(0.45, Math.max(0, endless.collapseAge(row, curLane, elapsed))) : 0; touches.clear();
        }
        return;
      }

      const lvlData = DATA.levels[level];
      if (lvlData.markRow >= 0 && row >= lvlData.markRow && !signalsSet.has(level)) {
        signalText = lvlData.fragment; signalTime = 3; signalsSet.add(level);
        localStorage.setItem('orbit_signals', Array.from(signalsSet).join(','));
      }
      if (!shieldTaken && !shieldActive && row === lvlData.shieldRow && curLane === lvlData.shieldLane && Runner.height(character, air) < 0.65) {
        shieldActive = true; shieldTaken = true; audio.play(0);
      }
      if (row >= 0 && row < lvlData.length && !crystalTaken.has(row) && lvlData.crystals[row] === curLane && Runner.height(character, air) < 0.75) {
        crystalTaken.add(row); collectCrystal();
      }
      if (row >= lvlData.length) {
        audio.play(3); completedLevel = level; completionNotice = 2;
        if (level < LEVELS - 1) burst(robotX, robotY - 50 * u, 26, 340 * u, CONFETTI_COLORS);
        completedSet.add(level);
        if (flawlessRun) flawlessSet.add(level);
        if (level === unlocked && unlocked < LEVELS - 1) unlocked++;
        store.setInt('unlocked', unlocked);
        localStorage.setItem('orbit_completed', Array.from(completedSet).join(','));
        localStorage.setItem('orbit_flawless', Array.from(flawlessSet).join(','));
        touches.clear();
        if (level === LEVELS - 1) { state = ENDING; endingTime = 0; } else { level++; flawlessRun = true; start(); }
      } else {
        for (const e of lvlData.enemies) {
          const ez = enemyPos(e, elapsed) - (distance + 5);
          let diff = Math.abs(lane - e.lane) % 8; diff = Math.min(diff, 8 - diff);
          if (Math.abs(ez) < 0.5 && diff >= 0.38 && diff <= 1.2 && calloutTime <= 0) {
            totalCrystals += 2 * comboMultiplier; store.setInt('totalCrystals', totalCrystals);
            showCallout(`TEĞET GEÇİŞ! +${2 * comboMultiplier}`, '#67dfcc');
          }
          if (diff < 0.38 && Math.abs(ez) < 1.1 && Runner.height(character, air) < 0.65) {
            if (frenzyTimer > 0) { jumpFromDrop = 0; air = Runner.jumpSeconds(character); audio.play(0); return; }
            if (shieldActive) { consumeShieldRescue(); return; }
            flawlessRun = false; audio.play(2); start(); restartNotice = 1.6; crash(); dizzyTime = 1.5; return;
          }
        }
        const tile = row < 0 || row >= lvlData.length ? FLOOR : lvlData.tiles[row][curLane];
        if (tile === BOOST && air <= 0.08) {
          boostTimer = 0.65; jumpFromDrop = 0; air = Runner.jumpSeconds(character) * 1.20;
          audio.play(0); triggerDust(); showCallout('SÜPER SIÇRAMA!', '#42e8ff'); return;
        }
        let fatal = Runner.lethal(character, tile, air);
        if (tile === RED && air <= 0) {
          if (collapse.trigger(row, curLane, elapsed)) audio.play(1);
          fatal = !collapse.canEscape(row, curLane, elapsed);
        }
        if (fatal) {
          if (frenzyTimer > 0) { jumpFromDrop = 0; air = Runner.jumpSeconds(character) * 1.05; audio.play(0); triggerDust(); return; }
          if (shieldActive) { consumeShieldRescue(); return; }
          flawlessRun = false;
          failure = tile === RED ? 'Kırmızı plaka çöktü! Üzerinden zıpla.' : tile === WALL ? 'Duvarı aşmak için daha yüksekte olmalısın.' : 'Boşluğa düştün. Zıpla ya da duvar değiştir.';
          if (tile === WALL) crash();
          audio.play(2); state = FALL; overlayChoice = 0; keyLeft = keyRight = keyJump = false; fall = tile === RED ? Math.min(0.45, Math.max(0, collapse.age(row, curLane, elapsed))) : 0; touches.clear();
        }
      }
    } else if (state === FALL) {
      fall = Math.min(1.3, fall + dt);
    } else if (state === ENDING) {
      endingTime += dt; air = Math.max(0, air - dt);
      lane += (targetLane - lane) * Math.min(1, dt * 16);
      cameraLane += (lane - cameraLane) * Math.min(1, dt * 8);
      if (endingTime >= ENDING_SECONDS) { state = WIN; whiteOut = 0.7; }
    }
    if (state === RUN || state === FALL) {
      const target = supportDrop() * u * 0.65;
      fallCamera += (target - fallCamera) * Math.min(1, dt * 12);
    }
  }

  // Drawing helpers matching GameView.java
  function roundRect(x, y, rw, rh, rad, fill, stroke, sw) {
    ctx.beginPath();
    ctx.roundRect(x, y, rw, rh, rad);
    if (fill) { ctx.fillStyle = fill; ctx.fill(); }
    if (stroke) { ctx.strokeStyle = stroke; ctx.lineWidth = sw || 2 * u; ctx.stroke(); }
  }
  function text(s, x, y, size, color, bold, align = 'center') {
    ctx.fillStyle = color;
    ctx.font = `${bold ? 'bold ' : ''}${(size * u) | 0}px -apple-system, BlinkMacSystemFont, sans-serif`;
    ctx.textAlign = align;
    ctx.fillText(s, x, y);
  }
  const px = (a, z) => cx + Math.sin(a) * 3 * focal / (z + 3);
  const py = (a, z) => cy + Math.cos(a) * 3 * focal / (z + 3);

  function drawIcon(icon, x, y, size, color, off) {
    ctx.save();
    ctx.translate(x - size / 2, y - size / 2);
    ctx.scale(size / 24, size / 24);
    ctx.strokeStyle = color; ctx.fillStyle = color;
    ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath();
    if (icon === 1 || icon === 9) {
      ctx.moveTo(7, 4); ctx.lineTo(19, 12); ctx.lineTo(7, 20); ctx.closePath(); ctx.stroke();
      if (icon === 9) { ctx.beginPath(); ctx.moveTo(22, 5); ctx.lineTo(22, 19); ctx.stroke(); }
    } else if (icon === 2) {
      ctx.arc(12, 12, 8, 205 * Math.PI / 180, 505 * Math.PI / 180); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(4.75, 3); ctx.lineTo(4.75, 8.62); ctx.lineTo(10.4, 8.62); ctx.stroke();
    } else if (icon === 3) {
      ctx.moveTo(2, 11); ctx.lineTo(12, 3); ctx.lineTo(22, 11);
      ctx.moveTo(5, 9); ctx.lineTo(5, 21); ctx.lineTo(10, 21); ctx.lineTo(10, 15); ctx.lineTo(14, 15); ctx.lineTo(14, 21); ctx.lineTo(19, 21); ctx.lineTo(19, 9); ctx.stroke();
    } else if (icon === 4) {
      ctx.moveTo(12, 18); ctx.lineTo(12, 4); ctx.moveTo(12, 4); ctx.lineTo(6, 10); ctx.moveTo(12, 4); ctx.lineTo(18, 10); ctx.moveTo(4, 22); ctx.lineTo(20, 22); ctx.stroke();
    } else if (icon === 5) {
      ctx.moveTo(9, 17); ctx.lineTo(9, 5); ctx.lineTo(20, 3); ctx.lineTo(20, 15); ctx.stroke();
      ctx.beginPath(); ctx.arc(6, 18, 3, 0, Math.PI * 2); ctx.arc(17, 16, 3, 0, Math.PI * 2); ctx.stroke();
    } else if (icon === 6) {
      ctx.moveTo(3, 9); ctx.lineTo(7, 9); ctx.lineTo(13, 4); ctx.lineTo(13, 20); ctx.lineTo(7, 15); ctx.lineTo(3, 15); ctx.closePath(); ctx.stroke();
      if (!off) { ctx.beginPath(); ctx.arc(16, 12, 4, -1.1, 1.1); ctx.stroke(); ctx.beginPath(); ctx.arc(17, 12, 7, -0.95, 0.95); ctx.stroke(); }
    } else if (icon === 7) {
      ctx.roundRect(5, 10, 14, 12, 2); ctx.stroke();
      ctx.beginPath(); ctx.arc(12, 9, 4, Math.PI, 0); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(8, 8); ctx.lineTo(8, 10); ctx.moveTo(16, 8); ctx.lineTo(16, 10); ctx.moveTo(12, 15); ctx.lineTo(12, 18); ctx.stroke();
    } else if (icon === 8) {
      ctx.moveTo(4, 12); ctx.lineTo(10, 18); ctx.lineTo(21, 5); ctx.stroke();
    } else if (icon === 10) {
      for (let i = 0; i < 10; i++) {
        const a = i * Math.PI / 5 - Math.PI / 2, r = i % 2 === 0 ? 10 : 4.2;
        if (i === 0) ctx.moveTo(12 + Math.cos(a) * r, 12 + Math.sin(a) * r);
        else ctx.lineTo(12 + Math.cos(a) * r, 12 + Math.sin(a) * r);
      }
      ctx.closePath(); ctx.fill();
    } else if (icon === 12) {
      ctx.moveTo(12, 12); ctx.bezierCurveTo(9, 7, 3, 7, 3, 12); ctx.bezierCurveTo(3, 17, 9, 17, 12, 12);
      ctx.bezierCurveTo(15, 7, 21, 7, 21, 12); ctx.bezierCurveTo(21, 17, 15, 17, 12, 12); ctx.stroke();
    } else if (icon === 14) {
      ctx.moveTo(12, 3); ctx.lineTo(12, 15); ctx.moveTo(7, 10); ctx.lineTo(12, 15); ctx.lineTo(17, 10);
      ctx.moveTo(4, 17); ctx.lineTo(4, 21); ctx.lineTo(20, 21); ctx.lineTo(20, 17); ctx.stroke();
    } else if (icon === 13) {
      ctx.arc(12, 12, 4, 0, Math.PI * 2); ctx.stroke();
      for (let i = 0; i < 8; i++) {
        const a = i * Math.PI / 4;
        ctx.beginPath(); ctx.moveTo(12 + Math.cos(a) * 6.5, 12 + Math.sin(a) * 6.5); ctx.lineTo(12 + Math.cos(a) * 9.5, 12 + Math.sin(a) * 9.5); ctx.stroke();
      }
    }
    if (off) { ctx.beginPath(); ctx.moveTo(3, 3); ctx.lineTo(21, 21); ctx.stroke(); }
    ctx.restore();
  }

  function button(r, s, accent, icon = 0, off = false) {
    roundRect(r.left, r.top, r.width(), r.height(), 14 * u, accent ? '#ffcd77' : 'rgba(23,53,67,0.93)');
    let font = 20;
    const iconSpace = icon === 0 ? 0 : 34 * u;
    ctx.font = `bold ${(font * u) | 0}px -apple-system, sans-serif`;
    const avail = r.width() - 32 * u - iconSpace;
    const mw = ctx.measureText(s).width;
    if (mw > avail && mw > 0) font = Math.max(13, font * avail / mw);
    ctx.font = `bold ${(font * u) | 0}px -apple-system, sans-serif`;
    const total = ctx.measureText(s).width + iconSpace;
    const startX = r.centerX() - total / 2;
    const fg = accent ? '#081722' : '#e9f6f3';
    if (icon !== 0) drawIcon(icon, startX + 12 * u, r.centerY(), 24 * u, fg, off);
    text(s, startX + iconSpace, r.centerY() + font * u * 0.35, font, fg, true, 'left');
  }

  const hexRgb = c => [parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16)];
  const toHex = (r, g, b) => '#' + [r, g, b].map(v => Math.max(0, Math.min(255, v | 0)).toString(16).padStart(2, '0')).join('');
  const shade = (c, k) => { const [r, g, b] = hexRgb(c); return toHex(r * k, g * k, b * k); };
  const mixColor = (a, b, t) => { const x = hexRgb(a), y = hexRgb(b); return toHex(x[0] + (y[0] - x[0]) * t, x[1] + (y[1] - x[1]) * t, x[2] + (y[2] - x[2]) * t); };
  const mix = (a, b, t) => a + (b - a) * t;
  const circle = (x, y, r, color) => { ctx.fillStyle = color; ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill(); };
  const line = (x0, y0, x1, y1) => { ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke(); };
  function limb(x0, y0, x1, y1, x2, y2, width, color) {
    ctx.strokeStyle = color; ctx.lineWidth = width * u; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath(); ctx.moveTo(x0 * u, y0 * u); ctx.lineTo(x1 * u, y1 * u); ctx.lineTo(x2 * u, y2 * u); ctx.stroke();
  }
  function drawHand(head, x, y, color) {
    if (head === 1) circle(x * u, y * u, 6.5 * u, color);
    else if (head === 2) roundRect((x - 5) * u, (y - 7) * u, 10 * u, 14 * u, 3 * u, color);
    else circle(x * u, y * u, 5 * u, color);
  }

  function drawRobotBody(charIdx, phase, air01, airProg, fastTrail, pose) {
    const bodyColor = Runner.color(charIdx);
    const bodyDark = shade(bodyColor, 0.72);
    const head = Runner.headType(charIdx);
    const trait = Runner.effectiveTrait(charIdx);
    const striding = pose === POSE_RUN || pose === POSE_CHEER;
    const idle = pose === POSE_IDLE || pose === POSE_WAVE;
    const runAmt = striding ? 1 - air01 : 0, airAmt = striding ? air01 : 0;
    const sinP = Math.sin(phase), cosP = Math.cos(phase), clock = animClock;
    // Each ability gets its own gait: Nova bounces, Atlas stomps, gliders skim.
    const stepLift = trait === 2 ? 8 : trait === 1 ? 6 : trait === 3 ? 3.5 : 6.5;
    const bobAmp = trait === 2 ? 2.2 : trait === 1 ? 5 : trait === 3 ? 1.6 : 3.4;
    const rollAmp = trait === 2 ? 3.6 : trait === 1 ? 1.5 : trait === 3 ? 0.8 : 2.2;
    const stance = trait === 2 ? 13 : 10.5;
    const breath = idle ? Math.sin(clock * 2.4 + charIdx) * 1.1 : 0;
    const bob = runAmt * Math.abs(sinP) * bobAmp;
    const tuck = Math.sin(airProg * Math.PI);
    let footL = -stepLift * Math.max(0, sinP) * runAmt + airAmt * (-3 * tuck + 4 * (1 - tuck));
    let footR = -stepLift * Math.max(0, -sinP) * runAmt + airAmt * (1 * tuck + 4 * (1 - tuck));
    let splay = 0;
    if (pose === POSE_FALL) { const kick = Math.sin(clock * 22) * 6; footL = 1 + kick; footR = 1 - kick; splay = 5; }

    if (striding && (fastTrail || (trait === 2 && runAmt > 0.5))) {
      const len = (fastTrail ? 36 : 18) * (0.8 + 0.2 * Math.sin(clock * 38));
      ctx.fillStyle = frenzyTimer > 0 || !fastTrail ? 'rgba(255,155,84,0.8)' : 'rgba(66,232,255,0.8)';
      for (let side = -1; side <= 1; side += 2) {
        const fx = side * (stance - 1);
        ctx.beginPath(); ctx.moveTo((fx - 6) * u, -6 * u);
        ctx.quadraticCurveTo((fx - 5) * u, len * 0.5 * u, fx * u, (len + 6) * u);
        ctx.quadraticCurveTo((fx + 5) * u, len * 0.5 * u, (fx + 6) * u, -6 * u);
        ctx.closePath(); ctx.fill();
      }
    }
    // Jointed legs; the knee bends outward as the foot lifts.
    const legColor = '#2c4a58';
    limb(-stance, -10, -stance + footL * 0.45, (-10 + footL) / 2, -stance - splay, footL, 5, legColor);
    limb(stance, -10, stance - footR * 0.45, (-10 + footR) / 2, stance + splay, footR, 5, legColor);
    const wL = 7 * (1 + footL * 0.012), wR = 7 * (1 + footR * 0.012);
    roundRect((-stance - splay - wL) * u, (footL - 4) * u, wL * 2 * u, 11 * u, 4 * u, '#e9f6f3');
    roundRect((stance + splay - wR) * u, (footR - 4) * u, wR * 2 * u, 11 * u, 4 * u, '#e9f6f3');

    const roll = runAmt * sinP * rollAmp;
    ctx.save();
    ctx.translate(0, (-bob + breath - 5) * u);
    ctx.rotate(roll * Math.PI / 180);
    const forcedFace = state === CUSTOMIZE || idle;
    const turn = forcedFace ? 1 : headTurn;

    if (head === 1) {
      // Nova's fins beat quickly in the air and rest on the ground.
      const reach = 0.55 + 0.45 * air01, flap = Math.sin(clock * (air01 > 0.3 ? 18 : 5)) * 6;
      ctx.fillStyle = shade(bodyColor, 0.82);
      for (let side = -1; side <= 1; side += 2) {
        ctx.beginPath(); ctx.moveTo(side * 22 * u, -30 * u); ctx.lineTo(side * (22 + 24 * reach) * u, (-42 + flap) * u); ctx.lineTo(side * 24 * u, -16 * u); ctx.closePath(); ctx.fill();
      }
    }
    // 1. Lower Torso / Suit (always facing forward into the tunnel, with back pack)
    roundRect(-23 * u, -26 * u, 46 * u, 28 * u, 9 * u, bodyDark);
    roundRect(-22 * u, -32 * u, 44 * u, 26 * u, 8 * u, bodyColor);
    roundRect(-13 * u, -29 * u, 26 * u, 19 * u, 4 * u, '#163342');
    circle(0, -19.5 * u, (4.5 + Math.sin(clock * 5) * 0.5) * u, fastTrail ? '#42e8ff' : '#67dfcc');
    circle(0, -19.5 * u, 2 * u, '#e9f6f3');
    if (signalGlow > 0) circle(0, -19.5 * u, (5 + 6 * signalGlow) * u, `rgba(255,224,160,${0.92 * signalGlow})`);

    // Arms swing against the legs, then rise and spread through the jump.
    const swing = cosP * 5.5 * runAmt;
    const settle = airProg * airProg * (3 - 2 * airProg);
    const airX = trait === 3 ? 40 : mix(30, 34, settle), airY = trait === 3 ? -30 + Math.sin(clock * 6) * 2 : mix(-40, -26, settle);
    let handLX = -mix(27, airX, airAmt), handLY = mix(-16 + swing, airY, airAmt);
    let handRX = mix(27, airX, airAmt), handRY = mix(-16 - swing, airY, airAmt);
    if (idle) { handLX = -26; handLY = -12 + breath; handRX = 26; handRY = -12 + breath; }
    if (pose === POSE_WAVE) { handRX = 31 + Math.sin(clock * 11) * 5; handRY = -46; }
    if (pose === POSE_CHEER) { handLX = -31; handLY = -48 + Math.sin(clock * 14) * 5; handRX = 31; handRY = -48 - Math.sin(clock * 14) * 5; }
    if (pose === POSE_FALL) {
      handLX = -31 - Math.sin(clock * 19) * 3; handLY = -40 + Math.sin(clock * 26) * 9;
      handRX = 31 + Math.sin(clock * 19) * 3; handRY = -40 - Math.sin(clock * 26) * 9;
    }
    limb(-22, -27, (-22 + handLX) / 2 - 3, (-27 + handLY) / 2, handLX, handLY, 4.5, bodyDark);
    limb(22, -27, (22 + handRX) / 2 + 3, (-27 + handRY) / 2, handRX, handRY, 4.5, bodyDark);
    drawHand(head, handLX, handLY, bodyColor);
    drawHand(head, handRX, handRY, bodyColor);

    // Neck ring separating torso and rotating head
    roundRect(-12 * u, -35 * u, 24 * u, 5 * u, 2 * u, '#102a38');

    // 2. Separate Head (rotates smoothly over the left shoulder to look back and smile)
    ctx.save();
    const s = turn * turn * (3 - 2 * turn);
    const lean = striding ? leanAnim : 0;
    const headTilt = (forcedFace ? 0 : Math.sin(s * Math.PI) * -6) - roll * 0.5 - lean * 4;
    const headShift = (forcedFace ? 0 : s * -3.5 * u) + lean * 5 * u;
    ctx.translate(headShift, (-46 + runAmt * Math.abs(cosP) * 1.4) * u);
    ctx.rotate(headTilt * Math.PI / 180);
    roundRect(-21 * u, -15 * u, 42 * u, 29 * u, (head === 2 ? 5 : 12) * u, bodyColor);

    ctx.fillStyle = bodyColor; ctx.strokeStyle = bodyColor; ctx.lineWidth = 3 * u; ctx.lineCap = 'round';
    if (head === 2) {
      ctx.fillRect(-18 * u, -23 * u, 8 * u, 10 * u); ctx.fillRect(10 * u, -23 * u, 8 * u, 10 * u);
    } else if (head === 3) {
      ctx.beginPath(); ctx.moveTo(-14 * u, -14 * u); ctx.lineTo(-10 * u, -26 * u); ctx.lineTo(-4 * u, -18 * u);
      ctx.lineTo(0, -29 * u); ctx.lineTo(4 * u, -18 * u); ctx.lineTo(10 * u, -26 * u); ctx.lineTo(14 * u, -14 * u); ctx.closePath(); ctx.fill();
    } else if (head === 0) {
      const sway = (Math.sin(phase * 0.7) * 3.2 + (striding ? antenna : 0)) * u;
      line(0, -15 * u, sway, -25 * u);
      circle(sway, -28 * u, 4 * u, bodyColor);
      if (signalGlow > 0) circle(sway, -28 * u, (1.4 + signalGlow * 4) * u, '#ffe0a0');
      else circle(sway - 1.1 * u, -29.1 * u, 1.4 * u, '#eff9ff');
    }

    // Back of the head plates (slides right and shrinks smoothly using 3D cosine projection)
    if (s < 0.65) {
      const backProg = s / 0.65;
      const bx = Math.sin(backProg * Math.PI * 0.5) * 16 * u;
      const bw = Math.cos(backProg * Math.PI * 0.5) * 6 * u;
      // Same three-stroke maintenance mark as the tunnel panels and late patrols.
      ctx.strokeStyle = '#5f7476'; ctx.lineWidth = 1.8 * u; ctx.lineCap = 'butt';
      for (let mark = -1; mark <= 1; mark++) line(bx + mark * bw * 0.7, -7 * u, bx + mark * bw * 0.7, 1 * u);
      circle(bx, 5 * u, 1.5 * u, '#5f7476');
    }

    // Face visor & expression (continuous 3D cylindrical wrap from left profile to front)
    const dizzy = dizzyTime > 0 && striding;
    if (s > 0.18) {
      const t = (s - 0.18) / 0.82;
      const rot = (1 - t) * Math.PI * 0.48;
      const fx = -Math.sin(rot) * 14 * u;
      const fw = Math.max(4 * u, Math.cos(rot) * 16 * u);
      roundRect(fx - fw, -10 * u, fw * 2, 19 * u, 5 * u, '#102a38');
      ctx.fillStyle = '#67dfcc'; ctx.strokeStyle = '#67dfcc'; ctx.lineCap = 'round';
      const eyeSep = fw * 0.46;
      const leftEyeScale = Math.max(0, (t - 0.22) / 0.78);
      const happy = t > 0.75 && (state === CUSTOMIZE || state === ENDING || pose === POSE_WAVE || pose === POSE_CHEER || calloutTime > 0 || frenzyTimer > 0 || (((elapsed * 1.8) | 0) % 3 !== 0));
      const blink = (animClock + charIdx * 0.7) % 3.4 < 0.11;
      if (pose === POSE_FALL) {
        if (leftEyeScale > 0.15) circle(fx - eyeSep, -3 * u, 3.7 * u * leftEyeScale, '#67dfcc');
        circle(fx + eyeSep, -3 * u, 3.7 * u, '#67dfcc');
        circle(fx, 4.2 * u, 2.4 * u, '#67dfcc');
      } else if (dizzy) {
        ctx.lineWidth = 1.9 * u;
        for (let side = -1; side <= 1; side += 2) {
          const ex = fx + side * eyeSep, ey = -2.5 * u, r = 2.6 * u;
          line(ex - r, ey - r, ex + r, ey + r); line(ex - r, ey + r, ex + r, ey - r);
        }
        const wave = Math.sin(clock * 12) * 1.2 * u;
        line(fx - fw * 0.3, 4 * u + wave, fx, 4 * u - wave); line(fx, 4 * u - wave, fx + fw * 0.3, 4 * u + wave);
      } else if (blink && !happy) {
        ctx.lineWidth = 2 * u;
        if (leftEyeScale > 0.15) line(fx - eyeSep - 2.6 * u, -2.5 * u, fx - eyeSep + 2.6 * u, -2.5 * u);
        line(fx + eyeSep - 2.6 * u, -2.5 * u, fx + eyeSep + 2.6 * u, -2.5 * u);
      } else if (happy) {
        ctx.lineWidth = 2.3 * u;
        ctx.beginPath(); ctx.arc(fx - eyeSep, -3 * u, 3 * u, 1.08 * Math.PI, 1.92 * Math.PI); ctx.stroke();
        ctx.beginPath(); ctx.arc(fx + eyeSep, -3 * u, 3 * u, 1.08 * Math.PI, 1.92 * Math.PI); ctx.stroke();
        ctx.beginPath(); ctx.arc(fx, 1.5 * u, fw * 0.44, 0.12 * Math.PI, 0.88 * Math.PI); ctx.stroke();
      } else {
        if (leftEyeScale > 0.15) circle(fx - eyeSep, -2.5 * u, 2.8 * u * leftEyeScale, '#67dfcc');
        circle(fx + eyeSep, -2.5 * u, 2.8 * u, '#67dfcc');
        if (t > 0.45) {
          const smileAlpha = (t - 0.45) / 0.55;
          ctx.lineWidth = 2 * u * smileAlpha;
          ctx.beginPath(); ctx.arc(fx, 1.5 * u, fw * 0.34 * smileAlpha, 0.15 * Math.PI, 0.85 * Math.PI); ctx.stroke();
        }
      }
    }
    if (dizzy) {
      ctx.fillStyle = '#ffcd77';
      for (let i = 0; i < 3; i++) {
        const a = clock * 5 + i * 2.09, sx = Math.cos(a) * 20 * u, sy = (-24 + Math.sin(a) * 5) * u, r = 3.4 * u;
        ctx.beginPath(); ctx.moveTo(sx, sy - r); ctx.lineTo(sx + r, sy); ctx.lineTo(sx, sy + r); ctx.lineTo(sx - r, sy); ctx.closePath(); ctx.fill();
      }
    }
    ctx.restore();
    ctx.restore();

    if (striding && (air01 > 0.3 || fastTrail)) {
      const jetColor = fastTrail ? 'rgba(66,232,255,0.8)' : trait === 1 ? 'rgba(155,222,239,0.8)' : 'rgba(188,238,240,0.6)';
      const jets = trait === 1 ? 4 : 3;
      for (let i = 0; i < jets; i++) circle((i - (jets - 1) / 2) * 11 * u, (12 + i * 6) * u, (3.6 - i * 0.5) * (0.85 + 0.15 * Math.sin(clock * 40 + i)) * u, jetColor);
    }
  }

  function alienDanger(alienLane, z) {
    let diff = Math.abs(lane - alienLane) % 8; diff = Math.min(diff, 8 - diff);
    return state === RUN && diff < 0.6 && z > 4 && z < 21;
  }
  function drawAlienSprite(angle, z, markings, seed, danger) {
    const x = cx + Math.sin(angle) * 2.7 * focal / (z + 3);
    const t = animClock * 4.2 + seed * 1.7;
    let size = 0.34 * focal / (z + 3);
    const y = cy + Math.cos(angle) * 2.7 * focal / (z + 3) + Math.sin(t * 0.5) * size * 0.1;
    const wob = Math.sin(t) * 0.07;
    const pulse = danger ? 0.5 + 0.5 * Math.sin(animClock * 14) : 0;
    if (danger) size *= 1.06;
    const body = danger ? mixColor('#e0d45c', '#ff7a66', pulse) : '#b9e86d';
    const detailed = size >= 6;
    if (detailed) {
      // Skirt lobes ripple around the body as it hovers.
      const skirt = shade(body, 0.7);
      for (let i = 0; i < 4; i++) circle(x + (i - 1.5) * size * 0.46, y + size * (0.58 + Math.sin(t * 1.6 + i * 1.5) * 0.1), size * 0.22, skirt);
    }
    ctx.fillStyle = body;
    ctx.beginPath(); ctx.ellipse(x, y - size * (0.175 - wob / 2), size * (1 + wob), size * (0.825 - wob / 2), 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#233e27';
    if ((animClock + seed * 0.9) % 2.8 < 0.1) {
      ctx.fillRect(x - size * 0.62, y - size * 0.11, size * 0.44, size * 0.08); ctx.fillRect(x + size * 0.18, y - size * 0.11, size * 0.44, size * 0.08);
    } else {
      ctx.beginPath(); ctx.ellipse(x - size * 0.405, y - size * 0.075, size * 0.255, size * 0.175, 0, 0, Math.PI * 2); ctx.ellipse(x + size * 0.405, y - size * 0.075, size * 0.255, size * 0.175, 0, 0, Math.PI * 2); ctx.fill();
      if (detailed) {
        const dx = robotX - x, dy = robotY - 40 * u - y, len = Math.max(1, Math.hypot(dx, dy));
        const ox = dx / len * size * 0.08, oy = dy / len * size * 0.05, white = danger ? '#ffe9c4' : '#eaffc4';
        circle(x - size * 0.405 + ox, y - size * 0.075 + oy, size * 0.095, white);
        circle(x + size * 0.405 + ox, y - size * 0.075 + oy, size * 0.095, white);
      }
    }
    if (markings) {
      ctx.strokeStyle = '#547146'; ctx.lineWidth = Math.max(1, size * 0.07);
      for (let mark = -1; mark <= 1; mark++) line(x + mark * size * 0.22, y + size * 0.25, x + mark * size * 0.22, y + size * 0.45);
    }
    const tipL = detailed ? Math.sin(t * 1.3) * size * 0.12 : 0, tipR = detailed ? Math.sin(t * 1.3 + 1.4) * size * 0.12 : 0;
    ctx.strokeStyle = '#eaffc4'; ctx.lineWidth = Math.max(1, size * 0.1); ctx.lineCap = 'butt';
    line(x - size * 0.5, y - size * 0.6, x - size * 0.85 + tipL, y - size * 1.4);
    line(x + size * 0.5, y - size * 0.6, x + size * 0.85 + tipR, y - size * 1.4);
    if (detailed) { circle(x - size * 0.85 + tipL, y - size * 1.42, size * 0.09, '#eaffc4'); circle(x + size * 0.85 + tipR, y - size * 1.42, size * 0.09, '#eaffc4'); }
    if (danger) {
      ctx.strokeStyle = `rgba(255,122,102,${(110 + pulse * 130) / 255})`; ctx.lineWidth = Math.max(1.5, size * 0.09);
      ctx.beginPath(); ctx.arc(x, y - size * 0.12, size * (1.3 + pulse * 0.14), 0, Math.PI * 2); ctx.stroke();
    }
  }

  const pulseAt = (time, center) => Math.max(0, 1 - Math.abs(time - center) / 0.13);
  // The light past the last tunnel row of the final level; the spill pass tints the walls in front of it.
  function drawEndLight(spill) {
    if (endlessMode || level !== LEVELS - 1 || state === MENU || state === WIN) return;
    const z = DATA.levels[level].length * 4 - distance;
    if (z > 108) return;
    const reply = state === ENDING ? Math.max(pulseAt(endingTime, 1.95), pulseAt(endingTime, 2.3)) : 0;
    const r = 3 * focal / (z + 3) * (1.02 + reply * 0.05);
    if (spill) {
      const alpha = Math.min(1, 90 * (1 - z / 108) * (1 + reply * 0.9) / 255), reach = r * 2.1;
      const glow = ctx.createRadialGradient(cx, cy, 0, cx, cy, reach);
      glow.addColorStop(0, `rgba(255,224,160,${alpha})`);
      glow.addColorStop(0.45, `rgba(255,224,160,${alpha})`);
      glow.addColorStop(1, 'rgba(255,224,160,0)');
      circle(cx, cy, reach, glow);
    } else {
      const core = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
      core.addColorStop(0, '#ffffff');
      core.addColorStop(0.45 + reply * 0.2, '#fff1cf');
      core.addColorStop(1, mixColor('#ffd98a', '#fff1cf', reply));
      circle(cx, cy, r, core);
    }
  }
  function drawSpeedLines() {
    if (currentSpeed() < 9.8 && boostTimer <= 0) return;
    const count = boostTimer > 0 ? 18 : 10;
    ctx.strokeStyle = boostTimer > 0 ? 'rgba(66,232,255,0.53)' : 'rgba(168,204,217,0.27)';
    ctx.lineWidth = (boostTimer > 0 ? 2.2 : 1.4) * u; ctx.lineCap = 'butt';
    for (let i = 0; i < count; i++) {
      const a = i * Math.PI * 2 / count + elapsed * 0.4;
      const z = ((i * 19 - distance * 2.8) % 85 + 85) % 85 + 6;
      line(cx + Math.sin(a) * 3.4 * focal / (z + 3), cy + Math.cos(a) * 3.4 * focal / (z + 3), cx + Math.sin(a) * 3.4 * focal / (z + 13), cy + Math.cos(a) * 3.4 * focal / (z + 13));
    }
  }
  function drawFallingPlates() {
    const first = Math.max(0, (distance / 4) | 0);
    const lastRow = endlessMode ? first + 27 : Math.min(DATA.levels[level].length - 1, first + 27);
    if (!endlessMode && !collapse) return;
    for (let row = lastRow; row >= first; row--) for (let side = 0; side < 8; side++) {
      const age = endlessMode ? endless.collapseAge(row, side, elapsed) : collapse.age(row, side, elapsed);
      if (age < 0 || age > 1.3) continue;
      const a = side * Math.PI / 4 - Math.PI / 8 - cameraLane * Math.PI / 4, b = a + Math.PI / 4;
      const mid = (a + b) / 2, z0 = Math.max(0.15, row * 4 - distance), z1 = (row + 1) * 4 - distance;
      if (z1 <= 0.15) continue;
      const drop = Collapse.drop(age) * u, zc = (z0 + z1) / 2;
      ctx.save();
      ctx.translate(Math.sin(mid) * drop, Math.cos(mid) * drop);
      ctx.beginPath(); ctx.moveTo(px(a, z0), py(a, z0)); ctx.lineTo(px(b, z0), py(b, z0)); ctx.lineTo(px(b, z1), py(b, z1)); ctx.lineTo(px(a, z1), py(a, z1)); ctx.closePath();
      ctx.fillStyle = '#c6353b'; ctx.fill();
      ctx.strokeStyle = '#ffb2a0'; ctx.lineWidth = 2 * u; ctx.stroke();
      line(px(a + 0.1, zc), py(a + 0.1, zc), px(mid, zc - 0.5), py(mid, zc - 0.5));
      line(px(mid, zc - 0.5), py(mid, zc - 0.5), px(b - 0.1, zc + 0.3), py(b - 0.1, zc + 0.3));
      ctx.restore();
    }
  }

  function render(now) {
    const dt = last === 0 ? 0 : Math.min(0.035, (now - last) / 1000);
    last = now;
    update(dt);
    updateAnim(dt);

    ctx.fillStyle = '#081722';
    ctx.fillRect(0, 0, w, h);

    const shake = shakeTime / 0.42 * 9 * u;
    ctx.save();
    ctx.translate(Math.sin(animClock * 61) * shake, -fallCamera + Math.cos(animClock * 47) * shake);

    // Stars & Nebula
    const rgb = endlessMode ? (endless ? endless.rgb((distance / 4) | 0) : [37, 94, 115]) : DATA.levels[level].rgb;
    ctx.fillStyle = `rgba(${rgb[0]},${rgb[1]},${rgb[2]},0.15)`;
    ctx.beginPath(); ctx.ellipse(cx, cy, 340 * u, 190 * u, 0, 0, Math.PI * 2); ctx.fill();
    for (let i = 0; i < 125; i++) {
      let seed = (Math.imul(i + 1, 1103515245) + 12345) | 0;
      const sx = ((seed >>> 8) & 65535) / 65535 * w;
      seed = (Math.imul(seed, 1664525) + 1013904223) | 0;
      const sy = ((seed >>> 8) & 65535) / 65535 * h;
      const bright = i % 7 === 0;
      ctx.fillStyle = bright ? '#eff9ff' : i % 3 === 0 ? '#a8ccd9' : '#6d98ae';
      ctx.beginPath(); ctx.arc(sx, sy, (bright ? 2.6 : i % 3 === 0 ? 1.6 : 1.0) * u, 0, Math.PI * 2); ctx.fill();
    }

    drawSpeedLines();
    drawEndLight(false);

    // Tunnel
    const first = Math.max(0, (distance / 4) | 0);
    const boundary = endlessMode ? first + 1000 : DATA.levels[level].length;
    const lastRow = endlessMode ? first + 27 : Math.min(boundary + (level < LEVELS - 1 ? 26 : -1), first + 27);
    const rotation = cameraLane * Math.PI / 4;

    for (let row = lastRow; row >= first; row--) {
      const z0 = Math.max(0.15, row * 4 - distance), z1 = (row + 1) * 4 - distance;
      if (z1 <= 0.15) continue;
      const drawLevel = row >= boundary ? level + 1 : level;
      const localRow = row >= boundary ? row - boundary : row;
      for (let side = 0; side < 8; side++) {
        const tile = endlessMode ? endless.tile(row, side) : (localRow < 0 || localRow >= DATA.levels[drawLevel].length ? FLOOR : DATA.levels[drawLevel].tiles[localRow][side]);
        const age = endlessMode ? endless.collapseAge(row, side, elapsed) : (drawLevel === level && collapse ? collapse.age(row, side, elapsed) : -1);
        if (age >= 0 || tile === HOLE) continue;

        const a = side * Math.PI / 4 - Math.PI / 8 - rotation, b = a + Math.PI / 4;
        const mid = (a + b) / 2, zc = (z0 + z1) / 2;
        ctx.save();
        ctx.beginPath();
        ctx.moveTo(px(a, z0), py(a, z0)); ctx.lineTo(px(b, z0), py(b, z0));
        ctx.lineTo(px(b, z1), py(b, z1)); ctx.lineTo(px(a, z1), py(a, z1));
        ctx.closePath();
        const fade = Math.max(0.18, 1 - z0 / 115);
        const pal = endlessMode ? endless.rgb(row) : DATA.levels[drawLevel].rgb;
        const shade = side % 2 === 0 ? 1 : 0.82;
        ctx.fillStyle = tile === RED ? `rgb(${(198 * fade) | 0},${(53 * fade) | 0},${(59 * fade) | 0})`
          : tile === BOOST ? `rgb(${(40 * fade) | 0},${(195 * fade) | 0},${(230 * fade) | 0})`
          : `rgb(${(pal[0] * fade * shade) | 0},${(pal[1] * fade * shade) | 0},${(pal[2] * fade * shade) | 0})`;
        ctx.fill();
        ctx.strokeStyle = `rgba(103,223,204,${0.49 * fade})`;
        ctx.lineWidth = Math.max(0.7, 1.6 * u);
        ctx.stroke();

        if (tile === BOOST) {
          ctx.strokeStyle = '#d6fbff'; ctx.lineWidth = 2.4 * u;
          ctx.beginPath(); ctx.moveTo(px(a + 0.18, zc - 0.5), py(a + 0.18, zc - 0.5)); ctx.lineTo(px(mid, zc + 0.6), py(mid, zc + 0.6)); ctx.lineTo(px(b - 0.18, zc - 0.5), py(b - 0.18, zc - 0.5)); ctx.stroke();
        }
        const hasOrb = endlessMode ? endless.hasShield(row, side) : (!shieldTaken && drawLevel === level && localRow === DATA.levels[drawLevel].shieldRow && side === DATA.levels[drawLevel].shieldLane);
        if (hasOrb) {
          const ox = cx + Math.sin(mid) * 2.65 * focal / (zc + 3), oy = cy + Math.cos(mid) * 2.65 * focal / (zc + 3), rad = 0.26 * focal / (zc + 3);
          ctx.fillStyle = '#42e8ff'; ctx.beginPath(); ctx.arc(ox, oy, rad, 0, Math.PI * 2); ctx.fill();
        }
        const hasCrystal = endlessMode ? endless.hasCrystal(row, side) : (drawLevel === level && !crystalTaken.has(localRow) && DATA.levels[drawLevel].crystals[localRow] === side);
        if (hasCrystal) {
          const ox = cx + Math.sin(mid) * 2.68 * focal / (zc + 3), oy = cy + Math.cos(mid) * 2.68 * focal / (zc + 3), rad = 0.21 * focal / (zc + 3);
          ctx.fillStyle = '#ffcd77'; ctx.beginPath(); ctx.moveTo(ox, oy - rad * 1.25); ctx.lineTo(ox + rad, oy); ctx.lineTo(ox, oy + rad * 1.25); ctx.lineTo(ox - rad, oy); ctx.closePath(); ctx.fill();
        }
        if (!endlessMode && localRow === DATA.levels[drawLevel].markRow && side === wrap(drawLevel + 2) && z0 < 60) {
          const mx = px(mid, zc), my = py(mid, zc), size = 0.23 * focal / (zc + 3);
          ctx.strokeStyle = '#b3c6b9'; ctx.lineWidth = Math.max(1, size * 0.13); ctx.lineCap = 'butt';
          for (let mark = 0; mark < 3; mark++) line(mx + (mark - 1) * size * 0.65, my - size * 0.6, mx + (mark - 1) * size * 0.65, my + size * 0.3);
          ctx.beginPath(); ctx.arc(mx, my + size * 0.7, size * 0.22, 0, Math.PI * 2); ctx.stroke();
        }
        if (tile === WALL) {
          const ax = px(a, z0), ay = py(a, z0), bx = px(b, z0), by = py(b, z0);
          ctx.beginPath(); ctx.moveTo(ax, ay); ctx.lineTo(bx, by);
          ctx.lineTo(cx + (bx - cx) * 0.79, cy + (by - cy) * 0.79);
          ctx.lineTo(cx + (ax - cx) * 0.79, cy + (ay - cy) * 0.79);
          ctx.closePath();
          ctx.fillStyle = '#d9984e'; ctx.fill(); ctx.strokeStyle = '#ffcd77'; ctx.lineWidth = 2 * u; ctx.stroke();
        }
        ctx.restore();
      }
    }

    drawEndLight(true);

    // Enemies
    if (endlessMode) {
      for (let row = first + 26; row >= first; row--) {
        if (!endless.hasPatrol(row)) continue;
        const z = row * 4 + 2 - distance;
        if (z >= 0.1 && z <= 100) { const pl = endless.patrolLane(row, elapsed); drawAlienSprite((pl - cameraLane) * Math.PI / 4, z, row > 80, row, alienDanger(pl, z)); }
      }
    } else {
      for (const e of DATA.levels[level].enemies) {
        const z = enemyPos(e, elapsed) - distance;
        if (z >= 0.1 && z <= 100) drawAlienSprite((e.lane - cameraLane) * Math.PI / 4, z, level >= 21, e.section, alienDanger(e.lane, z));
      }
    }

    drawFallingPlates();

    // Robot
    if (state !== FALL || fall < 1.1) {
      const angle = (lane - cameraLane) * Math.PI / 4;
      const walk = state === ENDING ? Math.min(1, endingTime / ENDING_WALK) : 0;
      const robotZ = 5 + 12 * (1 - (1 - walk) * (1 - walk)), depth = 8 / (robotZ + 3);
      const ground = cy + Math.cos(angle) * 3 * focal / (robotZ + 3) * 0.924;
      const lift = Runner.height(character, air) * 100 * u * depth;
      const ry = ground - lift + supportDrop() * u;
      const rx = cx + Math.sin(angle) * 3 * focal / (robotZ + 3);
      const scale = (state === FALL ? 1 - fall * 0.6 : 1) * depth;
      signalGlow = state === ENDING ? Math.max(pulseAt(endingTime, 1.2), pulseAt(endingTime, 1.55)) : 0;
      robotX = rx; robotY = ry;
      if (dustTime > 0) {
        const prog = 1 - dustTime / 0.28, dustColor = `rgba(103,223,204,${(1 - prog) * 0.82})`;
        for (let i = -2; i <= 2; i++) if (i !== 0) circle(dustX + i * 14 * u * prog, dustY - 6 * u * prog, (4.5 - prog * 3) * u, dustColor);
      }
      const shadowScale = Math.max(0.45, 1 - Runner.height(character, air) * 0.45) * (1 + squash * 0.6) * depth;
      ctx.fillStyle = 'rgba(0,0,0,0.4)';
      ctx.beginPath(); ctx.ellipse(rx, ground - 0.5 * u * shadowScale, 27 * u * shadowScale, 7.5 * u * shadowScale, 0, 0, Math.PI * 2); ctx.fill();
      const tumble = state === FALL ? Math.sin(fall * 9) * Math.min(1, fall * 3) * 22 : 0;
      ctx.save();
      ctx.translate(rx, ry);
      ctx.rotate((leanAnim * -16 + tumble) * Math.PI / 180);
      ctx.scale(scale * (1 + squash * 0.7), scale * (1 - squash));
      if (shieldActive || frenzyTimer > 0) {
        ctx.strokeStyle = frenzyTimer > 0 ? '#ff9b54' : '#42e8ff';
        ctx.lineWidth = 3 * u;
        ctx.beginPath(); ctx.arc(0, -34 * u, (44 + Math.sin(animClock * 6) * 1.6) * u, 0, Math.PI * 2); ctx.stroke();
      }
      const pose = state === FALL ? POSE_FALL : state === MENU ? POSE_IDLE : state === RUN && completionNotice > 1.1 ? POSE_CHEER : POSE_RUN;
      const airProg = air > 0 ? Math.max(0, Math.min(1, 1 - air / Runner.jumpSeconds(character))) : 0;
      drawRobotBody(character, stride, airPose, airProg, boostTimer > 0 || frenzyTimer > 0, pose);
      ctx.restore();
      signalGlow = 0;
    }
    drawParticles();
    ctx.restore();
    if (hitFlash > 0) { ctx.fillStyle = `rgba(255,70,60,${hitFlash / 0.28 * 0.35})`; ctx.fillRect(0, 0, w, h); }
    if (state === ENDING) { ctx.fillStyle = `rgba(255,241,207,${Math.max(0, Math.min(1, (endingTime - 2.75) / 0.85))})`; ctx.fillRect(0, 0, w, h); }

    // HUD & Controls
    if (state === RUN || state === PAUSE || (state === FALL && fall <= 1.1)) {
      ctx.fillStyle = 'rgba(8,23,34,0.9)';
      ctx.fillRect(0, 0, w, 112 * u);
      if (endlessMode) {
        text('Sonsuz Yörünge', 32 * u, 46 * u, 22, '#ffcd77', true, 'left');
        text(`En İyi: ${bestEndlessMeters} m`, 32 * u, 77 * u, 16, '#a7c3cb', false, 'left');
        text(`${currentMeters} m`, cx, 54 * u, 28, '#e9f6f3', true, 'center');
        text(`◆ ${totalCrystals} · Çarpan x${comboMultiplier}`, cx, 84 * u, 15, '#ffcd77', true, 'center');
      } else {
        text(`Bölüm ${level + 1} / ${LEVELS}`, 32 * u, 46 * u, 22, '#e9f6f3', true, 'left');
        text(DATA.levels[level].name + (shieldActive ? ' · Kalkan Aktif' : ''), 32 * u, 77 * u, 16, shieldActive ? '#42e8ff' : '#a7c3cb', false, 'left');
        const prog = Math.min(1, (distance + 5) / (DATA.levels[level].length * 4));
        roundRect(cx - 130 * u, 38 * u, 260 * u, 7 * u, 4 * u, '#274551');
        roundRect(cx - 130 * u, 38 * u, Math.max(8 * u, 260 * u * prog), 7 * u, 4 * u, '#67dfcc');
        text(`%${(prog * 100) | 0} · ◆ ${totalCrystals} (x${comboMultiplier})`, cx, 78 * u, 16, '#e9f6f3', false, 'center');
      }
      button(inGameMusic, audio.musicOn ? 'Müzik: Açık' : 'Müzik: Kapalı', false, 5, !audio.musicOn);
      button(pauseBtn, '||', false);
      button(leftBtn, '◀', held(1));
      button(rightBtn, '▶', held(2));
      button(jumpBtn, 'Zıpla', true, 4, false);
    }

    if (state === RUN && calloutTime > 0) text(calloutText, cx, 142 * u, 22, calloutColor, true, 'center');
    if (state === RUN && restartNotice > 0) text('Uzaylıya çarptın! Bölüm yeniden başladı.', cx, 151 * u, 19, '#ffcd77', true, 'center');
    if (state === RUN && completionNotice > 0) text(`Bölüm ${completedLevel + 1} bitti! Şimdi Bölüm ${level + 1}`, cx, 158 * u, 24, '#ffcd77', true, 'center');
    if (state === RUN && shieldBreakTime > 0) {
      roundRect(cx - 195 * u, 120 * u, 390 * u, 42 * u, 10 * u, 'rgba(24,74,92,0.82)');
      text('Enerji kalkanı seni kurtardı!', cx, 147 * u, 18, '#42e8ff', true, 'center');
    }
    if (state === RUN && signalTime > 0) {
      ctx.font = `${(19 * u) | 0}px -apple-system, BlinkMacSystemFont, sans-serif`;
      const half = ctx.measureText(signalText).width / 2 + 18 * u;
      roundRect(cx - half, h - 182 * u, half * 2, 36 * u, 10 * u, 'rgba(8,23,34,0.77)');
      text(signalText, cx, h - 157 * u, 19, '#e4eef0', false, 'center');
    }

    if (state === MENU) {
      ctx.fillStyle = 'rgba(8,23,34,0.94)'; ctx.fillRect(0, 0, w, h);
      button(trackButton, audio.musicLabel(), false, 9, false);
      button(musicButton, audio.musicOn ? 'Müzik: Açık' : 'Müzik: Kapalı', false, 5, !audio.musicOn);
      button(effectsButton, audio.effectsOn ? 'Efekt: Açık' : 'Efekt: Kapalı', false, 6, !audio.effectsOn);
      text('Yörünge Koşusu', cx, h * 0.20, 44, '#e9f6f3', true, 'center');
      text('Zıpla. Yön değiştir. Uzaylılardan kaç.', cx, h * 0.265, 20, '#b9d1d5', false, 'center');
      for (let i = 0; i < 4; i++) {
        const sel = i === character, col = Runner.color(i), head = Runner.headType(i), r = characterButtons[i];
        roundRect(r.left, r.top, r.width(), r.height(), 12 * u, sel ? '#345766' : '#173543', sel ? col : null, 3 * u);
        if (sel) {
          ctx.fillStyle = col; ctx.beginPath(); ctx.arc(r.right - 18 * u, r.top + 18 * u, 11 * u, 0, Math.PI * 2); ctx.fill();
          drawIcon(8, r.right - 18 * u, r.top + 18 * u, 16 * u, '#081722', false);
        }
        if (i === 3) drawIcon(13, r.left + 18 * u, r.top + 18 * u, 16 * u, '#b9d1d5', false);
        const x = r.centerX();
        const hop = sel ? Math.sin(Math.max(0, selectPulse / 0.45) * Math.PI) * 12 * u : 0;
        ctx.save(); ctx.translate(x, r.top + 56 * u - hop); ctx.scale(0.62, 0.62);
        drawRobotBody(i, animClock * 2.2 + i * 1.3, 0, 0, false, sel && animClock % 4.2 < 1.3 ? POSE_WAVE : POSE_IDLE);
        ctx.restore();
        text(i === 3 ? 'Özel (Tasarım)' : characterNames[i], x, r.bottom - 38 * u, 17, sel ? col : '#e9f6f3', sel, 'center');
        text(Runner.description(i), x, r.bottom - 14 * u, 13, '#e9f6f3', false, 'center');
      }
      button(menuStart, `Başla · Bölüm ${level + 1}`, true, 1, false);
      button(endlessButton, bestEndlessMeters > 0 ? `Sonsuz · ${bestEndlessMeters} m` : 'Sonsuz Yörünge', false, 12, false);
      text(DATA.levels[level].name, cx, menuStart.bottom + 27 * u, 18, '#b9d1d5', false, 'center');
      const starCount = flawlessSet.size + completedSet.size * 2;
      text('Bölümler', levelStrip.left, levelStrip.top - 18 * u, 22, '#e9f6f3', true, 'left');
      text(`${unlocked + 1} / 50 açık · ★ ${starCount} · ◆ ${totalCrystals}`, levelStrip.right, levelStrip.top - 18 * u, 17, '#ffcd77', false, 'right');

      ctx.save();
      ctx.beginPath(); ctx.rect(levelStrip.left, levelStrip.top, levelStrip.width(), levelStrip.height()); ctx.clip();
      for (let i = 0; i < LEVELS; i++) {
        const x = levelStrip.left + i * 88 * u - levelScroll;
        if (x + 76 * u < levelStrip.left || x > levelStrip.right) continue;
        roundRect(x, levelStrip.top, 76 * u, levelStrip.height(), 12 * u, i === level ? '#ffcd77' : i <= unlocked ? '#244a58' : '#142d39');
        if (i > unlocked) drawIcon(7, x + 62 * u, levelStrip.top + 16 * u, 15 * u, '#b9d1d5', false);
        else if (i === level) drawIcon(8, x + 62 * u, levelStrip.top + 16 * u, 14 * u, '#081722', false);
        text(String(i + 1), x + 38 * u, levelStrip.top + 34 * u, 24, i === level ? '#081722' : '#e9f6f3', true, 'center');
        const stars = flawlessSet.has(i) ? 3 : (completedSet.has(i) || i < unlocked) ? 2 : 0;
        if (stars > 0) {
          for (let s = 0; s < 3; s++) {
            const sc = s < stars ? (i === level ? '#081722' : '#ffcd77') : (i === level ? 'rgba(8,23,34,0.33)' : 'rgba(142,172,183,0.33)');
            drawIcon(10, x + 22 * u + s * 16 * u, levelStrip.bottom - 16 * u, 12 * u, sc, false);
          }
        } else {
          text(i > unlocked ? 'Kilitli' : i === level ? 'Seçili' : 'Açık', x + 38 * u, levelStrip.bottom - 13 * u, 13, i === level ? '#081722' : '#b9d1d5', false, 'center');
        }
      }
      ctx.restore();
      text(menuHint, cx, h - 20 * u, 16, '#b9d1d5', false, 'center');
      if (canOfferInstall()) button(installBtn, 'Yükle', false, 14, false);
      if (installPanel && INSTALL_STEPS) {
        ctx.fillStyle = 'rgba(8,23,34,0.82)'; ctx.fillRect(0, 0, w, h);
        const panelW = 560 * u, panelH = 300 * u, left = cx - panelW / 2, top = h / 2 - panelH / 2;
        roundRect(left, top, panelW, panelH, 16 * u, '#173543', '#ffcd77', 2.5 * u);
        text(INSTALL_STEPS.title, cx, top + 56 * u, 28, '#e9f6f3', true, 'center');
        INSTALL_STEPS.steps.forEach((step, i) => {
          const rowY = top + (108 + i * 50) * u;
          circle(left + 48 * u, rowY - 7 * u, 16 * u, '#ffcd77');
          text(String(i + 1), left + 48 * u, rowY, 19, '#081722', true, 'center');
          text(step, left + 78 * u, rowY, 19, '#e9f6f3', false, 'left');
        });
        text('Kapatmak için dokun', cx, top + panelH - 22 * u, 15, '#b9d1d5', false, 'center');
      }
    }

    if (state === CUSTOMIZE) {
      ctx.fillStyle = 'rgba(8,23,34,0.96)'; ctx.fillRect(0, 0, w, h);
      text('Kendi Karakterini Oluştur', cx, h * 0.14, 36, '#e9f6f3', true, 'center');
      roundRect(cx - 390 * u, h * 0.22, 220 * u, h * 0.50, 16 * u, '#142d39', Runner.color(3), 2.5 * u);
      // The preview runs with the chosen gait and jumps every few seconds at the ability's real airtime and height.
      const jumpTime = elapsed % 3.2, jumpSeconds = Runner.jumpSeconds(3), jumping = jumpTime < jumpSeconds;
      const jumpBlend = jumping ? Math.min(1, Math.min(jumpTime, jumpSeconds - jumpTime) / 0.08) : 0;
      const jumpLift = jumping ? Runner.height(3, jumpSeconds - jumpTime) * 60 * u : 0;
      const feetY = h * 0.47 + 50 * u, previewShadow = 1 - jumpLift / (150 * u);
      ctx.fillStyle = 'rgba(0,0,0,0.4)';
      ctx.beginPath(); ctx.ellipse(cx - 280 * u, feetY + 4 * u, 38 * u * previewShadow, 9 * u, 0, 0, Math.PI * 2); ctx.fill();
      ctx.save(); ctx.translate(cx - 280 * u, feetY - jumpLift); ctx.scale(1.45, 1.45);
      drawRobotBody(3, elapsed * 9.6 * (customTrait === 2 ? 0.85 : customTrait === 1 ? 0.92 : customTrait === 3 ? 0.8 : 1), jumpBlend, jumping ? jumpTime / jumpSeconds : 0, false, POSE_RUN);
      ctx.restore();
      text('Özel Robot', cx - 280 * u, h * 0.72 - 42 * u, 20, Runner.color(3), true, 'center');
      text(Runner.description(3), cx - 280 * u, h * 0.72 - 18 * u, 14, '#e9f6f3', false, 'center');
      text('Renk:', cx - 140 * u, h * 0.24, 18, '#b9d1d5', true, 'left');
      for (let i = 0; i < 6; i++) {
        const r = customColorBtns[i];
        roundRect(r.left, r.top, r.width(), r.height(), 10 * u, DATA.palette[i], customColorIndex === i ? '#e9f6f3' : null, 3.5 * u);
        if (customColorIndex === i) drawIcon(8, r.centerX(), r.centerY(), 18 * u, '#081722', false);
      }
      text('Başlık / Gövde Tipi:', cx - 140 * u, h * 0.41, 18, '#b9d1d5', true, 'left');
      for (let i = 0; i < 4; i++) {
        const r = customHeadBtns[i], sel = customHeadType === i;
        roundRect(r.left, r.top, r.width(), r.height(), 10 * u, sel ? '#345766' : '#173543', sel ? '#ffcd77' : null, 2.5 * u);
        text(DATA.headNames[i], r.centerX(), r.centerY() + 6 * u, 15, sel ? '#ffcd77' : '#e9f6f3', sel, 'center');
      }
      text('Yetenek Gücü:', cx - 140 * u, h * 0.58, 18, '#b9d1d5', true, 'left');
      for (let i = 0; i < 4; i++) {
        const r = customTraitBtns[i], sel = customTrait === i;
        roundRect(r.left, r.top, r.width(), r.height(), 10 * u, sel ? '#345766' : '#173543', sel ? '#ffcd77' : null, 2.5 * u);
        text(DATA.traitNames[i], r.centerX(), r.centerY() + 6 * u, 14, sel ? '#ffcd77' : '#e9f6f3', sel, 'center');
      }
      button(customSaveBtn, 'Tamam', true, 8, false);
    }

    if (state === PAUSE || (state === FALL && fall > 1.1) || state === WIN) {
      ctx.fillStyle = 'rgba(8,23,34,0.92)'; ctx.fillRect(0, 0, w, h);
      if (state === PAUSE) {
        button(trackButton, audio.musicLabel(), false, 9, false);
        button(musicButton, audio.musicOn ? 'Müzik: Açık' : 'Müzik: Kapalı', false, 5, !audio.musicOn);
        button(effectsButton, audio.effectsOn ? 'Efekt: Açık' : 'Efekt: Kapalı', false, 6, !audio.effectsOn);
      }
      const title = state === PAUSE ? 'Kısa bir mola' : state === WIN ? 'Bölüm 50 bitti!' : endlessMode && newRecord ? `Yeni Rekor: ${currentMeters} m!` : 'Bir daha deneyelim';
      const detail = state === PAUSE ? (endlessMode ? `Sonsuz Yörünge · ${currentMeters} m` : 'Hazır olduğunda yörüngene dön.') : state === WIN ? 'Uzakta bir ışık daha yandı.' : failure;
      text(title, cx, h * 0.32, 48, '#e9f6f3', true, 'center');
      text(detail, cx, h * 0.41, 22, '#b9d1d5', false, 'center');
      button(primaryBtn, state === PAUSE ? 'Devam et' : state === WIN ? 'İlk bölümden oyna' : 'Yeniden dene', overlayChoice === 0, state === PAUSE ? 1 : 2, false);
      button(secondaryBtn, 'Ana menü', overlayChoice === 1, 3, false);
    }

    if (whiteOut > 0) { ctx.fillStyle = `rgba(255,241,207,${whiteOut / 0.7})`; ctx.fillRect(0, 0, w, h); }

    if (updateReady && state === MENU && !stripDragging && !installPanel) { location.reload(); return; }
    requestAnimationFrame(render);
  }
  requestAnimationFrame(render);

  // Unified pointer/touch input for iOS Safari & desktop
  function pos(t) {
    const r = canvas.getBoundingClientRect();
    const dpr = canvas.width / r.width;
    return { x: (t.clientX - r.left) * dpr, y: (t.clientY - r.top) * dpr };
  }

  function handleDown(id, x, y) {
    audio.unlock();
    if (state === CUSTOMIZE) return;
    if (state === MENU) {
      if (installPanel) return;
      if (levelStrip.contains(x, y)) { stripDragging = true; stripMoved = false; dragStart = x; scrollStart = levelScroll; }
      return;
    }
    if (state === RUN) {
      if (inGameMusic.contains(x, y)) audio.toggleMusic();
      else if (pauseBtn.contains(x, y)) { state = PAUSE; overlayChoice = 0; keyLeft = keyRight = keyJump = false; touches.clear(); }
      else if (leftBtn.contains(x, y)) { touches.set(id, 1); turn(-1); }
      else if (rightBtn.contains(x, y)) { touches.set(id, 2); turn(1); }
      else if (jumpBtn.contains(x, y)) { touches.set(id, 3); leap(); }
    } else if (state !== ENDING && (state !== FALL || fall > 1.1)) {
      if (state === PAUSE && trackButton.contains(x, y)) audio.nextMusic();
      else if (state === PAUSE && musicButton.contains(x, y)) audio.toggleMusic();
      else if (state === PAUSE && effectsButton.contains(x, y)) audio.toggleEffects();
      else if (primaryBtn.contains(x, y)) {
        if (state === PAUSE) { state = RUN; last = 0; }
        else if (endlessMode) startEndless();
        else { if (state === WIN) level = 0; start(); }
      } else if (secondaryBtn.contains(x, y)) { state = MENU; endlessMode = false; centerSelection(); }
    }
  }

  function handleMove(id, x, y) {
    if (state === MENU && stripDragging) {
      if (Math.abs(x - dragStart) > 8 * u) stripMoved = true;
      levelScroll = Math.max(0, Math.min(maxScroll(), scrollStart + dragStart - x));
      return;
    }
    const v = touches.get(id);
    if (v) {
      const r = v === 1 ? leftBtn : v === 2 ? rightBtn : jumpBtn;
      if (!r.contains(x, y)) touches.delete(id);
    }
  }

  function handleUp(id, x, y) {
    touches.delete(id);
    if (state === CUSTOMIZE) {
      for (let i = 0; i < 6; i++) if (customColorBtns[i].contains(x, y)) { customColorIndex = i; store.setInt('customColor', i); audio.play(1); return; }
      for (let i = 0; i < 4; i++) if (customHeadBtns[i].contains(x, y)) { customHeadType = i; store.setInt('customHead', i); audio.play(1); return; }
      for (let i = 0; i < 4; i++) if (customTraitBtns[i].contains(x, y)) { customTrait = i; store.setInt('customTrait', i); audio.play(1); return; }
      if (customSaveBtn.contains(x, y)) { character = 3; store.setInt('character', 3); audio.play(0); state = MENU; }
      return;
    }
    if (state === MENU) {
      if (installPanel) { installPanel = false; return; }
      if (canOfferInstall() && !stripDragging && installBtn.contains(x, y)) { audio.play(1); offerInstall(); return; }
      if (stripDragging) {
        if (!stripMoved && levelStrip.contains(x, y)) {
          const rel = x - levelStrip.left + levelScroll, sel = (rel / (88 * u)) | 0;
          if (sel >= 0 && sel < LEVELS && (rel % (88 * u)) < 76 * u) {
            if (sel <= unlocked) { level = sel; menuHint = `Bölüm ${level + 1} seçildi`; audio.play(1); }
            else menuHint = `Önce Bölüm ${unlocked + 1} tamamlanmalı`;
          }
        }
        stripDragging = false;
        return;
      }
      if (trackButton.contains(x, y)) audio.nextMusic();
      else if (musicButton.contains(x, y)) audio.toggleMusic();
      else if (effectsButton.contains(x, y)) audio.toggleEffects();
      else if (menuStart.contains(x, y)) { audio.play(0); flawlessRun = true; start(); }
      else if (endlessButton.contains(x, y)) { audio.play(0); startEndless(); }
      else for (let i = 0; i < 4; i++) if (characterButtons[i].contains(x, y)) {
        character = i; store.setInt('character', i); audio.play(1); selectPulse = 0.45;
        if (i === 3) state = CUSTOMIZE;
      }
    }
  }

  canvas.addEventListener('touchstart', e => { e.preventDefault(); for (const t of e.changedTouches) { const p = pos(t); handleDown(t.identifier, p.x, p.y); } }, { passive: false });
  canvas.addEventListener('touchmove', e => { e.preventDefault(); for (const t of e.changedTouches) { const p = pos(t); handleMove(t.identifier, p.x, p.y); } }, { passive: false });
  canvas.addEventListener('touchend', e => { e.preventDefault(); for (const t of e.changedTouches) { const p = pos(t); handleUp(t.identifier, p.x, p.y); } }, { passive: false });
  canvas.addEventListener('touchcancel', e => { e.preventDefault(); touches.clear(); stripDragging = false; }, { passive: false });

  let mouseDown = false;
  canvas.addEventListener('mousedown', e => { mouseDown = true; const p = pos(e); handleDown('m', p.x, p.y); });
  canvas.addEventListener('mousemove', e => { if (mouseDown) { const p = pos(e); handleMove('m', p.x, p.y); } });
  window.addEventListener('mouseup', e => { if (mouseDown) { mouseDown = false; const p = pos(e); handleUp('m', p.x, p.y); } });

  window.addEventListener('keydown', e => {
    audio.unlock();
    const isConfirm = e.code === 'Space' || e.code === 'Enter' || e.code === 'NumpadEnter';
    const isLeft = e.code === 'ArrowLeft' || e.code === 'KeyA';
    const isRight = e.code === 'ArrowRight' || e.code === 'KeyD';
    const isUp = e.code === 'ArrowUp' || e.code === 'KeyW';
    const isDown = e.code === 'ArrowDown' || e.code === 'KeyS';

    if (state === RUN) {
      if ((e.code === 'Space' || isUp) && !e.repeat) { leap(); e.preventDefault(); }
      if (isLeft) { keyLeft = true; e.preventDefault(); }
      if (isRight) { keyRight = true; e.preventDefault(); }
      if (e.code === 'Escape') { state = PAUSE; overlayChoice = 0; keyLeft = keyRight = keyJump = false; touches.clear(); e.preventDefault(); }
    } else if (state === MENU) {
      if (installPanel) { installPanel = false; e.preventDefault(); return; }
      if (isLeft && level > 0) { level--; centerSelection(); menuHint = `Bölüm ${level + 1} seçildi`; audio.play(1); e.preventDefault(); }
      if (isRight && level < unlocked) { level++; centerSelection(); menuHint = `Bölüm ${level + 1} seçildi`; audio.play(1); e.preventDefault(); }
      if (isConfirm && !e.repeat) { audio.play(0); flawlessRun = true; start(); e.preventDefault(); }
    } else if (state === CUSTOMIZE) {
      if (isConfirm || e.code === 'Escape') { character = 3; store.setInt('character', 3); audio.play(0); state = MENU; e.preventDefault(); }
    } else if (state === PAUSE || (state === FALL && fall > 1.1) || state === WIN) {
      if (isUp || isDown || isLeft || isRight) {
        overlayChoice = 1 - overlayChoice;
        audio.play(1);
        e.preventDefault();
      } else if (isConfirm && !e.repeat) {
        if (overlayChoice === 0) {
          if (state === PAUSE) { state = RUN; last = 0; }
          else if (endlessMode) startEndless();
          else { if (state === WIN) level = 0; start(); }
        } else {
          state = MENU; endlessMode = false; centerSelection();
        }
        e.preventDefault();
      }
    }
  });
  window.addEventListener('keyup', e => {
    if (e.code === 'ArrowLeft' || e.code === 'KeyA') keyLeft = false;
    if (e.code === 'ArrowRight' || e.code === 'KeyD') keyRight = false;
  });
})();
