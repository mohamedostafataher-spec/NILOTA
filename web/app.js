/* NILORA web — passport, live map, missions. Data in data.js (COUNTRIES, ALL_CITIES, TOTAL_STAMPS). */

const LEVELS = [
  { min: 0,  en: 'Newcomer',   ar: 'وافد جديد',  badge: '🌱' },
  { min: 1,  en: 'Explorer',   ar: 'مستكشف',     badge: '🧭' },
  { min: 5,  en: 'Adventurer', ar: 'مغامر',      badge: '⛰️' },
  { min: 12, en: 'Nile Master',ar: 'سيّد النيل', badge: '👑' },
  { min: 40, en: 'Global Soul',ar: 'روح العالم', badge: '🌍' },
];

const MISSION_POOL = [
  { id: 'open_map',    pts: 10, ar: 'افتح الخريطة الحيّة',              en: 'Open the live map' },
  { id: 'weather',     pts: 10, ar: 'اضغط على مدينة وشوف طقسها دلوقتي',  en: 'Tap a city and check its live weather' },
  { id: 'story',       pts: 15, ar: 'اقرأ قصة من قصص البردي',            en: 'Read a papyrus story' },
  { id: 'prayer',      pts: 10, ar: 'شوف مواقيت الصلاة على الخريطة',     en: 'Check prayer times on the map' },
  { id: 'languages',   pts: 10, ar: 'بدّل بين العربي والإنجليزي',        en: 'Switch between Arabic and English' },
];

const I18N = {
  ar: {
    dir: 'rtl', langBtn: 'EN',
    tabPassport: 'الجواز', tabMap: 'الخريطة', tabTravel: 'سافر', tabHara: 'الحارة', tabSouk: 'السوق',
    travelTitle: 'وضع المسافر', chooseDest: 'اختار وجهتك…', currencyLabel: 'العملة',
    phrasesTitle: 'اتعلم ٥ كلمات من البلد', localTimeLabel: 'الساعة هناك',
    eventTitle: 'احتفال النهاردة في العالم', eventClaim: 'احسبها +٢٠ نقطة', eventClaimed: 'اتحسبت ✅',
    weeklyTitle: 'تحدي الأسبوع', weeklyBody: 'اجمع ٥ أختام من ٣ دول مختلفة الأسبوع ده', weeklyReward: 'الجايزة: ختم نادر + ٥٠ نقطة',
    boardTitle: 'المتصدرين (محلي)', youRow: 'انت', mockNote: 'الترتيب الحقيقي أونلاين ييجي مع الباك إند',
    noEvent: 'مفيش احتفال رسمي النهاردة — بص بكرة', highLow: 'العظمى/الصغرى',
    heroSub: 'جواز سفرك للعالم — الأختام تتجمع من أماكن حقيقية، مش من الشاشة',
    stampsOf: (n, t) => `${n} من ${t} ختم`,
    nextLevel: (name, n) => n > 0 ? `باقي ${n} ${n === 1 ? 'ختم' : 'أختام'} لرتبة «${name}»` : 'وصلت لأعلى رتبة 🎉',
    demo: 'الوضع التجريبي', demoOn: 'التجريبي شغال — دوس على أي ختم مقفول',
    stampsTitle: 'الأختام', levelsTitle: 'الرتب',
    levelReq: (n) => n === 0 ? 'البداية' : `${n}+`,
    earnedOn: (d) => `اتختّم ${d}`,
    mapTitle: 'الخريطة الحيّة',
    prayerTitle: 'أوقات الصلاة — القاهرة',
    quakeTitle: 'زلازل قريبة (آخر ٢٤ ساعة)',
    noQuakes: 'لا زلازل مسجّلة قرب مصر في آخر ٢٤ ساعة',
    weatherNow: 'الطقس الآن',
    legendCities: 'مدن الجواز', legendQuakes: 'زلازل', legendStories: 'قصص بردي',
    readMore: 'اقرأ على ويكيبيديا', storiesLoading: 'بنجيب القصص…', noStories: 'مفيش قصص قريبة هنا — قرّب أو حرّك الخريطة',
    loading: 'بيحمّل…', mag: 'القوة',
    mapBody: '', haraTitle: 'الحارة المصرية',
    haraBody: 'حارة افتراضية تلاقي فيها الناس: قهوة بلدي، صوت الطابية، وترجمة فورية على جهازك من غير ما كلامك يسيب موبايلك.',
    soukTitle: 'سوق التبادل',
    soukBody: 'بدّل قصة بقصة، وصنعة بصنعة. تبادل ثقافي حقيقي مع مسافرين من كل العالم — زي رسايل Bottled بس بالمصري.',
    comingSoon: 'قريبًا',
    tagline: 'روح العالم — بأصل مصري',
    missionsTitle: 'مهام النهاردة', points: (p) => `${p} نقطة`, done: 'تم ✅',
    rareTitle: 'ختم نادر', rareBody: 'الجعران الذهبي — اجمع ١٠٠ نقطة',
    shareBtn: 'شارك جوازك', shareDone: 'اتنزّلت الصورة 🎉',
    unlockTitle: 'ختم جديد!',
    cities: (n) => `${n} مدينة`,
  },
  en: {
    dir: 'ltr', langBtn: 'عربي',
    tabPassport: 'Passport', tabMap: 'Map', tabTravel: 'Travel', tabHara: 'Hara', tabSouk: 'Souk',
    travelTitle: 'Traveler mode', chooseDest: 'Choose your destination…', currencyLabel: 'Currency',
    phrasesTitle: 'Learn 5 local words', localTimeLabel: 'Local time there',
    eventTitle: "Today's world celebration", eventClaim: 'Claim +20 pts', eventClaimed: 'Claimed ✅',
    weeklyTitle: 'Weekly challenge', weeklyBody: 'Collect 5 stamps from 3 different countries this week', weeklyReward: 'Reward: rare stamp + 50 pts',
    boardTitle: 'Leaderboard (local)', youRow: 'You', mockNote: 'Real online ranking comes with the backend',
    noEvent: 'No official holiday today — check back tomorrow', highLow: 'High/Low',
    heroSub: 'Your world passport — stamps are earned in real places, not on a screen',
    stampsOf: (n, t) => `${n} of ${t} stamps`,
    nextLevel: (name, n) => n > 0 ? `${n} more ${n === 1 ? 'stamp' : 'stamps'} to «${name}»` : 'Top level reached 🎉',
    demo: 'Demo mode', demoOn: 'Demo on — tap any locked stamp',
    stampsTitle: 'Stamps', levelsTitle: 'Ranks',
    levelReq: (n) => n === 0 ? 'Start' : `${n}+`,
    earnedOn: (d) => `Stamped ${d}`,
    mapTitle: 'The live map',
    prayerTitle: 'Prayer times — Cairo',
    quakeTitle: 'Nearby earthquakes (last 24h)',
    noQuakes: 'No recorded earthquakes near Egypt in the last 24h',
    weatherNow: 'Weather now',
    legendCities: 'Passport cities', legendQuakes: 'Earthquakes', legendStories: 'Papyrus stories',
    readMore: 'Read on Wikipedia', storiesLoading: 'Finding stories…', noStories: 'No stories nearby — zoom in or move the map',
    loading: 'Loading…', mag: 'Mag',
    mapBody: '', haraTitle: 'The Egyptian hara',
    haraBody: 'A virtual alley to meet people: a baladi café, backgammon sounds, and instant translation on your device — your words never leave your phone.',
    soukTitle: 'The exchange souk',
    soukBody: 'Trade a story for a story, a craft for a craft. Real cultural exchange with travelers worldwide — Bottled letters, Egyptian style.',
    comingSoon: 'Coming soon',
    tagline: 'Global soul — Egyptian origin',
    missionsTitle: "Today's missions", points: (p) => `${p} pts`, done: 'Done ✅',
    rareTitle: 'Rare stamp', rareBody: 'The Golden Scarab — collect 100 points',
    shareBtn: 'Share your passport', shareDone: 'Image downloaded 🎉',
    unlockTitle: 'New stamp!',
    cities: (n) => `${n} cities`,
  },
};

const state = {
  lang: localStorage.getItem('nilora.lang') || 'ar',
  demo: localStorage.getItem('nilora.demo') === '1',
  demoStamps: JSON.parse(localStorage.getItem('nilora.demoStamps') || '[]'),
  points: parseInt(localStorage.getItem('nilora.points') || '0', 10),
};

function t() { return I18N[state.lang]; }

function todayKey() { return new Date().toISOString().slice(0, 10); }

function missionState() {
  const raw = JSON.parse(localStorage.getItem('nilora.missions') || '{}');
  return raw.day === todayKey() ? raw : { day: todayKey(), done: [] };
}
function saveMissions(m) { localStorage.setItem('nilora.missions', JSON.stringify(m)); }

// Deterministic daily pick of 3 missions from the pool.
function dailyMissions() {
  let seed = 0;
  for (const ch of todayKey()) seed = (seed * 31 + ch.charCodeAt(0)) >>> 0;
  const pool = [...MISSION_POOL];
  const picked = [];
  while (picked.length < 3) {
    seed = (seed * 1103515245 + 12345) >>> 0;
    picked.push(pool.splice(seed % pool.length, 1)[0]);
  }
  return picked;
}

function completeMission(id) {
  const m = missionState();
  if (m.done.includes(id)) return;
  if (!dailyMissions().some(x => x.id === id)) return;
  m.done.push(id);
  saveMissions(m);
  const pts = MISSION_POOL.find(x => x.id === id).pts;
  state.points += pts;
  localStorage.setItem('nilora.points', String(state.points));
}

function earnedIds() { return state.demo ? state.demoStamps : []; }
function earnedCount() { return earnedIds().length; }
function levelFor(n) { return LEVELS.filter(l => n >= l.min).pop(); }
function nextLevel(l) { const i = LEVELS.indexOf(l); return LEVELS[i + 1] || null; }

function cartoucheSVG() {
  return `
  <svg class="cartouche" viewBox="0 0 200 88" role="img" aria-label="cartouche">
    <path d="M100 6 C 40 6 14 24 14 44 C 14 64 40 82 100 82 C 160 82 186 64 186 44 C 186 24 160 6 100 6 Z"
      fill="none" stroke="#C6A15B" stroke-width="3"/>
    <path d="M100 12 C 46 12 20 27 20 44 C 20 61 46 76 100 76 C 154 76 180 61 180 44 C 180 27 154 12 100 12 Z"
      fill="none" stroke="#C6A15B" stroke-width="1.4" stroke-dasharray="5 4" opacity="0.7"/>
    <rect x="14" y="40" width="7" height="8" fill="#C6A15B"/>
    <text x="100" y="40" text-anchor="middle" font-family="Cairo, sans-serif" font-weight="900"
      font-size="17" fill="#C6A15B" letter-spacing="4">NILORA</text>
    <text x="100" y="62" text-anchor="middle" font-size="16" fill="#C6A15B" opacity="0.9">☥ ☥ ☥</text>
  </svg>`;
}

function stampHTML(city, earned) {
  const name = city[state.lang];
  const date = earned ? `<div class="stamp-date">${t().earnedOn('٢٠٢٦/١١')}</div>` : '';
  const glyph = earned ? city.glyph : '🔒';
  return `
    <div class="stamp ${earned ? 'earned' : 'locked'}" data-stamp="${city.id}">
      <div class="stamp-ring"><span class="stamp-glyph">${glyph}</span></div>
      <div class="stamp-name">${name}</div>
      ${date}
    </div>`;
}

function weekKey() {
  const d = new Date();
  const onejan = new Date(d.getFullYear(), 0, 1);
  const week = Math.ceil((((d - onejan) / 86400000) + onejan.getDay() + 1) / 7);
  return d.getFullYear() + '-W' + week;
}

function weeklyChallengeHTML(n) {
  const raw = JSON.parse(localStorage.getItem('nilora.weekly') || '{}');
  const done = raw.week === weekKey() && raw.done;
  const ids = earnedIds();
  const countries = new Set(ALL_CITIES.filter(c => ids.includes(c.id)).map(c => c.country.code));
  const goalStamps = Math.min(n, 5), goalCountries = Math.min(countries.size, 3);
  return `<div class="live-panel weekly ${done ? 'done' : ''}">
    <h3>🏆 ${t().weeklyTitle}</h3>
    <div class="rows">${t().weeklyBody}</div>
    <div class="rows" style="margin-top:6px"><b>${goalStamps}/5</b> · <b>${goalCountries}/3</b> ${state.lang === 'ar' ? 'دول' : 'countries'}</div>
    <div class="rows weekly-reward">${t().weeklyReward}</div>
    ${done ? `<div class="rows">✅</div>` : ''}
  </div>`;
}

const MOCK_NAMES = [['كابتن سارة', 340], ['GlobeTrotter_88', 285], ['مسافر_النيل', 220], ['Layla.roams', 150], ['WanderAsh', 90]];

function leaderboardHTML(pts) {
  const rows = [...MOCK_NAMES.map(([nm, p]) => ({ nm, p, you: false })), { nm: t().youRow, p: pts, you: true }];
  rows.sort((a, b) => b.p - a.p);
  return `<div class="live-panel">
    <h3>📊 ${t().boardTitle}</h3>
    ${rows.map((r, i) => `<div class="board-row ${r.you ? 'you' : ''}">
      <span class="board-rank">${['🥇','🥈','🥉'][i] || (i + 1)}</span>
      <span class="board-name">${r.nm}${r.you ? ' 🧭' : ''}</span>
      <span class="board-pts">${t().points(r.p)}</span>
    </div>`).join('')}
    <div class="rows" style="margin-top:8px;font-size:11.5px">${t().mockNote}</div>
  </div>`;
}

function renderPassport() {
  const n = earnedCount(), lvl = levelFor(n), nxt = nextLevel(lvl);
  const lang = state.lang;
  const pct = Math.round((n / TOTAL_STAMPS) * 100);
  const need = nxt ? nxt.min - n : 0;
  const ids = earnedIds();
  const missions = dailyMissions();
  const mState = missionState();
  const pts = state.points;
  return `
    <section class="hero">
      ${cartoucheSVG()}
      <div class="hero-level">${lvl.badge} ${lvl[lang]}</div>
      <div class="hero-sub">${t().heroSub}</div>
      <div class="progress-wrap"><div class="progress-bar" style="width:${pct}%"></div></div>
      <div class="progress-label">${t().stampsOf(n, TOTAL_STAMPS)} · ${pct}% · ${t().points(pts)}</div>
      <div class="next-level">${t().nextLevel(nxt ? nxt[lang] : '', need)}</div>
      <div class="hero-actions">
        <button id="shareBtn" class="share-btn" type="button">📤 ${t().shareBtn}</button>
        <button id="demoToggle" class="demo-toggle ${state.demo ? 'on' : ''}" type="button">
          ${state.demo ? t().demoOn : t().demo}
        </button>
      </div>
    </section>

    <h2 class="section-title">${t().missionsTitle}</h2>
    <div class="missions">
      ${missions.map(m => {
        const done = mState.done.includes(m.id);
        return `<div class="mission-row ${done ? 'done' : ''}">
          <span class="mission-pts">+${m.pts}</span>
          <span class="mission-text">${m[lang]}</span>
          <span class="mission-state">${done ? t().done : ''}</span>
        </div>`;
      }).join('')}
      <div class="mission-row rare ${pts >= 100 ? 'done' : ''}">
        <span class="mission-pts">🪲</span>
        <span class="mission-text"><b>${t().rareTitle}:</b> ${t().rareBody}</span>
        <span class="mission-state">${pts >= 100 ? t().done : `${Math.min(pts,100)}/100`}</span>
      </div>
    </div>

    <h2 class="section-title">${t().weeklyTitle}</h2>
    ${weeklyChallengeHTML(n)}

    <h2 class="section-title">${t().boardTitle}</h2>
    ${leaderboardHTML(pts)}

    <h2 class="section-title">${t().stampsTitle}</h2>
    ${COUNTRIES.map(cn => `
      <div class="country-block">
        <div class="country-head">${cn.flag} ${cn[lang]} <span class="country-count">${cn.cities.filter(c => ids.includes(c.id)).length}/${cn.cities.length}</span></div>
        <div class="stamp-grid">
          ${cn.cities.map(c => stampHTML(c, ids.includes(c.id))).join('')}
        </div>
      </div>`).join('')}

    <h2 class="section-title">${t().levelsTitle}</h2>
    <div class="levels">
      ${LEVELS.map(l => {
        const cls = l === lvl ? 'current' : (n >= l.min ? 'reached' : '');
        return `<div class="level-row ${cls}">
          <span class="level-badge">${l.badge}</span>
          <span class="level-name">${l[lang]}</span>
          <span class="level-req">${t().levelReq(l.min)}</span>
        </div>`;
      }).join('')}
    </div>`;
}

/* ---- unlock animation + sound ---- */

function playUnlockSound() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const notes = [523.25, 659.25, 783.99, 1046.5];
    notes.forEach((f, i) => {
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = 'triangle';
      o.frequency.value = f;
      g.gain.setValueAtTime(0, ctx.currentTime + i * 0.13);
      g.gain.linearRampToValueAtTime(0.25, ctx.currentTime + i * 0.13 + 0.02);
      g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.13 + 0.5);
      o.connect(g).connect(ctx.destination);
      o.start(ctx.currentTime + i * 0.13);
      o.stop(ctx.currentTime + i * 0.13 + 0.55);
    });
    setTimeout(() => ctx.close(), 1200);
  } catch (e) {}
}

function showUnlock(city) {
  playUnlockSound();
  const ov = document.createElement('div');
  ov.className = 'unlock-overlay';
  ov.innerHTML = `
    <div class="unlock-rings"><span></span><span></span><span></span></div>
    <div class="unlock-stamp">${city.glyph}</div>
    <div class="unlock-title">${t().unlockTitle}</div>
    <div class="unlock-city">${city[state.lang]} ${city.country.flag}</div>`;
  document.body.appendChild(ov);
  setTimeout(() => ov.classList.add('out'), 1900);
  setTimeout(() => ov.remove(), 2400);
}

/* ---- shareable passport card ---- */

async function sharePassport() {
  const n = earnedCount(), lvl = levelFor(n);
  const lang = state.lang;
  const canvas = document.createElement('canvas');
  canvas.width = 1080; canvas.height = 1080;
  const x = canvas.getContext('2d');
  const grad = x.createLinearGradient(0, 0, 0, 1080);
  grad.addColorStop(0, '#101D32'); grad.addColorStop(1, '#1B2F50');
  x.fillStyle = grad; x.fillRect(0, 0, 1080, 1080);
  // cartouche
  x.strokeStyle = '#C6A15B'; x.lineWidth = 8;
  x.beginPath(); x.ellipse(540, 220, 300, 90, 0, 0, Math.PI * 2); x.stroke();
  x.setLineDash([12, 9]); x.lineWidth = 3;
  x.beginPath(); x.ellipse(540, 220, 278, 72, 0, 0, Math.PI * 2); x.stroke();
  x.setLineDash([]);
  x.fillStyle = '#C6A15B';
  x.textAlign = 'center';
  x.font = '900 64px Cairo, sans-serif';
  x.fillText('NILORA', 540, 240);
  x.font = '90px serif';
  x.fillText(lvl.badge, 540, 430);
  x.font = '900 72px Cairo, sans-serif';
  x.fillText(lvl[lang], 540, 540);
  x.fillStyle = '#FAF8F2';
  x.font = '700 44px Cairo, sans-serif';
  x.fillText(t().stampsOf(n, TOTAL_STAMPS), 540, 630);
  x.fillText(t().points(state.points), 540, 700);
  // earned glyphs
  const ids = earnedIds();
  const glyphs = ALL_CITIES.filter(c => ids.includes(c.id)).map(c => c.glyph).slice(0, 12);
  x.font = '72px serif';
  glyphs.forEach((g, i) => x.fillText(g, 240 + (i % 6) * 120, 830 + Math.floor(i / 6) * 110));
  x.fillStyle = '#E9D9BD';
  x.font = '600 34px Cairo, sans-serif';
  x.fillText('mohamedostafataher-spec.github.io/NILOTA/web', 540, 1000);

  const blob = await new Promise(r => canvas.toBlob(r, 'image/png'));
  const file = new File([blob], 'nilora-passport.png', { type: 'image/png' });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try { await navigator.share({ files: [file], title: 'NILORA Passport' }); return; } catch (e) {}
  }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'nilora-passport.png';
  a.click();
  const btn = document.getElementById('shareBtn');
  if (btn) btn.textContent = t().shareDone;
}

/* ---- live map ---- */

let mapObj = null;
const WEATHER_CODES = {
  0: ['صافي', 'Clear'], 1: ['صافي غالبًا', 'Mostly clear'], 2: ['غيوم جزئية', 'Partly cloudy'],
  3: ['غائم', 'Overcast'], 45: ['ضباب', 'Fog'], 48: ['ضباب', 'Fog'],
  51: ['رذاذ خفيف', 'Light drizzle'], 53: ['رذاذ', 'Drizzle'], 55: ['رذاذ كثيف', 'Dense drizzle'],
  61: ['مطر خفيف', 'Light rain'], 63: ['مطر', 'Rain'], 65: ['مطر غزير', 'Heavy rain'],
  71: ['ثلج خفيف', 'Light snow'], 73: ['ثلج', 'Snow'], 75: ['ثلج كثيف', 'Heavy snow'],
  80: ['زخات مطر', 'Rain showers'], 81: ['زخات مطر', 'Rain showers'], 82: ['زخات غزيرة', 'Heavy showers'],
  95: ['عاصفة رعدية', 'Thunderstorm'], 96: ['عاصفة رعدية مع بَرَد', 'Thunderstorm, hail'], 99: ['عاصفة رعدية مع بَرَد', 'Thunderstorm, hail'],
};
function weatherLabel(code) {
  const w = WEATHER_CODES[code] || ['—', '—'];
  return state.lang === 'ar' ? w[0] : w[1];
}

async function cityPopup(marker, city) {
  const name = city[state.lang];
  marker.bindPopup(`<b>${city.country.flag} ${name}</b><br>${t().loading}`).openPopup();
  try {
    const url = 'https://api.open-meteo.com/v1/forecast?latitude=' + city.lat + '&longitude=' + city.lon +
      '&current=temperature_2m,weather_code,wind_speed_10m&timezone=auto';
    const r = await fetch(url);
    const j = await r.json();
    const cur = j.current;
    completeMission('weather');
    const weatherHtml = `<b>${city.country.flag} ${name}</b><br>${t().weatherNow}: ${weatherLabel(cur.weather_code)} · ` +
      `${Math.round(cur.temperature_2m)}°C · 💨 ${Math.round(cur.wind_speed_10m)} km/h`;
    marker.setPopupContent(weatherHtml + '<div class="guide-text">' + t().loading + '</div>');
    // free city guide: short Wikipedia extract, no key
    try {
      const title = encodeURIComponent(state.lang === 'ar' ? city.ar : city.en);
      const gr = await fetch('https://' + wikiHost() + '/api/rest_v1/page/summary/' + title);
      const gj = await gr.json();
      const guide = (gj.extract || '').slice(0, 320);
      marker.setPopupContent(weatherHtml + (guide ? '<div class="guide-text">📖 ' + guide + '…</div>' : ''));
    } catch (e2) {
      marker.setPopupContent(weatherHtml);
    }
  } catch (e) {
    marker.setPopupContent(`<b>${city.country.flag} ${name}</b>`);
  }
}

async function loadPrayerTimes() {
  const el = document.getElementById('prayerRows');
  if (!el) return;
  try {
    const r = await fetch('https://api.aladhan.com/v1/timingsByCity?city=Cairo&country=Egypt&method=5');
    const j = await r.json();
    const tt = j.data.timings;
    const names = state.lang === 'ar'
      ? [['الفجر','Fajr'],['الشروق','Sunrise'],['الظهر','Dhuhr'],['العصر','Asr'],['المغرب','Maghrib'],['العشاء','Isha']]
      : [['Fajr','Fajr'],['Sunrise','Sunrise'],['Dhuhr','Dhuhr'],['Asr','Asr'],['Maghrib','Maghrib'],['Isha','Isha']];
    el.innerHTML = names.map(([label, key]) =>
      `<div><b>${label}</b> · ${tt[key]}</div>`).join('');
    completeMission('prayer');
  } catch (e) { el.textContent = '—'; }
}

async function loadQuakes(map) {
  const el = document.getElementById('quakeRows');
  try {
    const r = await fetch('https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson');
    const j = await r.json();
    const near = j.features.filter(f => {
      const [lon, lat] = f.geometry.coordinates;
      return lat >= 19 && lat <= 34 && lon >= 22 && lon <= 42;
    });
    near.forEach(f => {
      const [lon, lat] = f.geometry.coordinates;
      if (map) L.circleMarker([lat, lon], {
        radius: 4 + f.properties.mag, color: '#B83924', weight: 1.5, fillOpacity: 0.35,
      }).addTo(map).bindPopup(`${t().mag} ${f.properties.mag} · ${f.properties.place}`);
    });
    if (el) el.innerHTML = near.length
      ? near.map(f => `<div><b>${t().mag} ${f.properties.mag}</b> · ${f.properties.place}</div>`).join('')
      : t().noQuakes;
  } catch (e) { if (el) el.textContent = '—'; }
}

/* ---- papyrus stories ---- */

let storyLayer = null;
let storyTimer = null;

function wikiHost() { return (state.lang === 'ar' ? 'ar' : 'en') + '.wikipedia.org'; }

async function fetchStories(lat, lon, radius, limit) {
  const url = 'https://' + wikiHost() + '/w/api.php?action=query&list=geosearch' +
    '&gscoord=' + lat + '|' + lon + '&gsradius=' + radius + '&gslimit=' + limit +
    '&format=json&origin=*';
  const r = await fetch(url);
  const j = await r.json();
  return (j.query && j.query.geosearch) || [];
}

async function fetchExtracts(ids) {
  if (!ids.length) return {};
  const url = 'https://' + wikiHost() + '/w/api.php?action=query&prop=extracts' +
    '&exintro&explaintext&exchars=420&pageids=' + ids.join('|') + '&format=json&origin=*';
  const r = await fetch(url);
  const j = await r.json();
  return (j.query && j.query.pages) || {};
}

function storyPopupHTML(page, extract) {
  const link = 'https://' + wikiHost() + '/?curid=' + page.pageid;
  const text = extract ? extract.replace(/\n+/g, ' ').slice(0, 380) : '';
  return '<div class="papyrus-popup"><div class="papyrus-title">📜 ' + page.title + '</div>' +
    (text ? '<div class="papyrus-text">' + text + '…</div>' : '') +
    '<a href="' + link + '" target="_blank" rel="noopener">' + t().readMore + ' ←</a></div>';
}

async function loadStories(lat, lon) {
  if (!mapObj) return;
  try {
    const zoom = mapObj.getZoom();
    const radius = zoom >= 12 ? 10000 : zoom >= 9 ? 40000 : 100000;
    const pages = await fetchStories(lat, lon, radius, 20);
    if (!mapObj) return;
    if (storyLayer) { storyLayer.clearLayers(); } else { storyLayer = L.layerGroup().addTo(mapObj); }
    if (!pages.length) return;
    const extracts = await fetchExtracts(pages.map(p => p.pageid));
    pages.forEach(p => {
      const ex = extracts[p.pageid] ? extracts[p.pageid].extract : '';
      const m = L.marker([p.lat, p.lon], {
        icon: L.divIcon({ className: 'story-marker', html: '📜', iconSize: [28, 28] }),
      }).addTo(storyLayer).bindPopup(storyPopupHTML(p, ex), { maxWidth: 280 });
      m.on('popupopen', () => completeMission('story'));
    });
  } catch (e) { /* stories are best-effort */ }
}

function watchStories() {
  if (!mapObj) return;
  const c0 = mapObj.getCenter();
  loadStories(c0.lat, c0.lng);
  mapObj.on('moveend', () => {
    clearTimeout(storyTimer);
    storyTimer = setTimeout(() => {
      if (!mapObj) return;
      const c1 = mapObj.getCenter();
      loadStories(c1.lat, c1.lng);
    }, 800);
  });
}

function initMap() {
  if (typeof L === 'undefined') return;
  if (mapObj) { mapObj.remove(); mapObj = null; }
  mapObj = L.map('map', { scrollWheelZoom: false, worldCopyJump: true }).setView([27.5, 30.5], 5);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18, attribution: '&copy; OpenStreetMap',
  }).addTo(mapObj);
  ALL_CITIES.forEach(city => {
    const m = L.marker([city.lat, city.lon], {
      icon: L.divIcon({ className: 'city-marker', html: city.glyph, iconSize: [30, 30] }),
    }).addTo(mapObj);
    m.on('click', () => cityPopup(m, city));
  });
  loadPrayerTimes();
  loadQuakes(mapObj);
  watchStories();
  completeMission('open_map');
}

/* ---- world event of the day (Nager.Date, keyless) ---- */

function eventState() {
  const raw = JSON.parse(localStorage.getItem('nilora.event') || '{}');
  return raw.day === todayKey() ? raw : { day: todayKey(), claimed: false };
}

async function loadWorldEvent() {
  const el = document.getElementById('eventRows');
  if (!el) return;
  try {
    const r = await fetch('https://date.nager.at/api/v3/NextPublicHolidaysWorldwide');
    const list = await r.json();
    const today = todayKey();
    const todays = list.filter(h => h.date === today);
    const st = eventState();
    if (!todays.length) { el.textContent = t().noEvent; return; }
    const h = todays[0];
    const country = COUNTRIES.find(cn => cn.code === h.countryCode);
    const flag = country ? country.flag : '🌍';
    const cname = country ? country[state.lang] : h.countryCode;
    el.innerHTML = `<div><b>${flag} ${cname}</b> · ${h.name}</div>` +
      (st.claimed
        ? `<div>${t().eventClaimed}</div>`
        : `<button id="eventClaimBtn" class="claim-btn" type="button">${t().eventClaim}</button>`);
    const btn = document.getElementById('eventClaimBtn');
    if (btn) btn.addEventListener('click', () => {
      const s2 = eventState();
      if (s2.claimed) return;
      s2.claimed = true;
      localStorage.setItem('nilora.event', JSON.stringify(s2));
      state.points += 20;
      localStorage.setItem('nilora.points', String(state.points));
      btn.replaceWith(document.createTextNode(t().eventClaimed));
    });
  } catch (e) { el.textContent = '—'; }
}

/* ---- traveler mode ---- */

function renderTravel() {
  const i18n = t();
  return `
    <h2 class="section-title">${i18n.travelTitle}</h2>
    <div class="live-panel">
      <select id="destSelect" class="dest-select">
        <option value="">${i18n.chooseDest}</option>
        ${COUNTRIES.map(cn => `<optgroup label="${cn.flag} ${cn[state.lang]}">
          ${cn.cities.map(c => `<option value="${c.id}">${c[state.lang]}</option>`).join('')}
        </optgroup>`).join('')}
      </select>
      <div id="destInfo" class="rows" style="margin-top:10px"></div>
    </div>`;
}

async function loadDestination(cityId) {
  const el = document.getElementById('destInfo');
  if (!el) return;
  const city = ALL_CITIES.find(c => c.id === cityId);
  if (!city) { el.innerHTML = ''; return; }
  const cur = CURRENCY[city.country.code] || 'USD';
  el.innerHTML = `<span class="live-loading">${t().loading}</span>`;
  let weatherHtml = '', tz = '';
  try {
    const r = await fetch('https://api.open-meteo.com/v1/forecast?latitude=' + city.lat + '&longitude=' + city.lon +
      '&current=temperature_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto');
    const j = await r.json();
    tz = j.timezone;
    weatherHtml = `<div>🌡️ <b>${Math.round(j.current.temperature_2m)}°C</b> · ${weatherLabel(j.current.weather_code)} · ` +
      `${t().highLow}: ${Math.round(j.daily.temperature_2m_max[0])}°/${Math.round(j.daily.temperature_2m_min[0])}° · 💨 ${Math.round(j.current.wind_speed_10m)} km/h</div>`;
  } catch (e) { weatherHtml = ''; }
  let currHtml = `<div>💱 <b>${t().currencyLabel}:</b> ${cur}</div>`;
  try {
    const r2 = await fetch('https://open.er-api.com/v6/latest/EGP');
    const j2 = await r2.json();
    const rate = j2.rates[cur];
    if (rate) currHtml = `<div>💱 1 EGP = <b>${rate.toFixed(rate < 0.01 ? 4 : 2)}</b> ${cur} · 100 EGP ≈ <b>${(rate * 100).toFixed(2)}</b> ${cur}</div>`;
  } catch (e) {}
  let timeHtml = '';
  try {
    if (tz) timeHtml = `<div>🕐 ${t().localTimeLabel}: <b>${new Date().toLocaleTimeString(state.lang === 'ar' ? 'ar-EG' : 'en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit' })}</b></div>`;
  } catch (e) {}
  const words = PHRASES[city.country.code] || [];
  const wordsHtml = words.length
    ? `<div class="phrases-title">${t().phrasesTitle}</div>` + words.map(w =>
        `<div class="phrase-row"><b>${w[0]}</b> <span class="phrase-tr">(${w[1]})</span> — ${w[2]}</div>`).join('')
    : '';
  el.innerHTML = weatherHtml + timeHtml + currHtml + wordsHtml;
}

/* ---- screens ---- */

function renderMap() {
  const i18n = t();
  return `
    <h2 class="section-title">${i18n.mapTitle}</h2>
    <div id="map"></div>
    <div class="map-legend">
      <span><i class="dot dot-city"></i>${i18n.legendCities}</span>
      <span><i class="dot dot-quake"></i>${i18n.legendQuakes}</span>
      <span><i class="dot dot-story"></i>${i18n.legendStories}</span>
    </div>
    <div class="live-panel" id="eventPanel"><h3>🎉 ${i18n.eventTitle}</h3><div id="eventRows" class="rows"><span class="live-loading">${i18n.loading}</span></div></div>
    <div class="live-panel"><h3>🕌 ${i18n.prayerTitle}</h3><div id="prayerRows" class="rows"><span class="live-loading">${i18n.loading}</span></div></div>
    <div class="live-panel"><h3>🌋 ${i18n.quakeTitle}</h3><div id="quakeRows" class="rows"><span class="live-loading">${i18n.loading}</span></div></div>
    <div class="soon-card" style="text-align:center"><p style="color:var(--gold-dark);font-weight:700">${i18n.tagline}</p></div>`;
}

function renderSoon(kind) {
  if (kind === 'map') return renderMap();
  const i18n = t();
  const cards = {
    hara: [{ icon: '🏘️', title: i18n.haraTitle, body: i18n.haraBody }],
    souk: [{ icon: '🏺', title: i18n.soukTitle, body: i18n.soukBody }],
  }[kind] || [];
  return cards.map(c => `
    <div class="soon-card">
      <h3>${c.icon} ${c.title}</h3>
      <p>${c.body}</p>
      <span class="soon-badge">${i18n.comingSoon}</span>
    </div>`).join('') +
    `<div class="soon-card" style="text-align:center"><p style="color:var(--gold-dark);font-weight:700">${i18n.tagline}</p></div>`;
}

function render() {
  const hashParts = location.hash.replace('#/', '').split('?');
  const route = hashParts[0] || 'passport';
  if ((hashParts[1] || '').includes('demo=1') && !state.demo) {
    state.demo = true;
    localStorage.setItem('nilora.demo', '1');
  }
  const app = document.getElementById('app');
  document.documentElement.lang = state.lang;
  document.documentElement.dir = t().dir;
  document.getElementById('langToggle').textContent = t().langBtn;
  document.querySelectorAll('.tab').forEach(tab =>
    tab.classList.toggle('active', tab.dataset.route === route));
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const k = el.dataset.i18n; if (t()[k]) el.textContent = t()[k];
  });

  if (mapObj && route !== 'map') { mapObj.remove(); mapObj = null; }
  if (route === 'passport') app.innerHTML = renderPassport();
  else if (route === 'travel') app.innerHTML = renderTravel();
  else app.innerHTML = renderSoon(route);
  if (route === 'map') { initMap(); loadWorldEvent(); }

  const destSelect = document.getElementById('destSelect');
  if (destSelect) destSelect.addEventListener('change', () => loadDestination(destSelect.value));

  const demoBtn = document.getElementById('demoToggle');
  if (demoBtn) demoBtn.addEventListener('click', () => {
    state.demo = !state.demo;
    localStorage.setItem('nilora.demo', state.demo ? '1' : '0');
    render();
  });

  const shareBtn = document.getElementById('shareBtn');
  if (shareBtn) shareBtn.addEventListener('click', sharePassport);

  // demo sandbox: tapping a locked stamp earns it with the unlock animation
  document.querySelectorAll('.stamp.locked').forEach(el => {
    el.addEventListener('click', () => {
      if (!state.demo) return;
      const id = el.dataset.stamp;
      if (state.demoStamps.includes(id)) return;
      state.demoStamps.push(id);
      localStorage.setItem('nilora.demoStamps', JSON.stringify(state.demoStamps));
      const city = ALL_CITIES.find(c => c.id === id);
      showUnlock(city);
      setTimeout(render, 2000);
    });
  });
}

document.getElementById('langToggle').addEventListener('click', () => {
  state.lang = state.lang === 'ar' ? 'en' : 'ar';
  localStorage.setItem('nilora.lang', state.lang);
  completeMission('languages');
  render();
});

window.addEventListener('hashchange', render);
render();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
