/* NILORA web — passport first. Data mirrors the Android models
   (app/src/main/java/com/example/ui/passport/PassportModels.kt). */

const CITIES = [
  { id: 'cairo', en: 'Cairo', ar: 'القاهرة', glyph: '🕌', lat: 30.0444, lon: 31.2357 },
  { id: 'giza', en: 'Giza', ar: 'الجيزة', glyph: '🔺', lat: 30.0131, lon: 31.2089 },
  { id: 'alexandria', en: 'Alexandria', ar: 'الإسكندرية', glyph: '⚓', lat: 31.2001, lon: 29.9187 },
  { id: 'sharm_el_sheikh', en: 'Sharm El Sheikh', ar: 'شرم الشيخ', glyph: '🤿', lat: 27.9158, lon: 34.3299 },
  { id: 'hurghada', en: 'Hurghada', ar: 'الغردقة', glyph: '🐠', lat: 27.2579, lon: 33.8116 },
  { id: 'dahab', en: 'Dahab', ar: 'دهب', glyph: '🐪', lat: 28.5092, lon: 34.5136 },
  { id: 'marsa_alam', en: 'Marsa Alam', ar: 'مرسى علم', glyph: '🐬', lat: 25.0676, lon: 34.879 },
  { id: 'luxor', en: 'Luxor', ar: 'الأقصر', glyph: '🏛️', lat: 25.6872, lon: 32.6396 },
  { id: 'aswan', en: 'Aswan', ar: 'أسوان', glyph: '⛵', lat: 24.0889, lon: 32.8998 },
  { id: 'el_gouna', en: 'El Gouna', ar: 'الجونة', glyph: '🌅', lat: 27.3942, lon: 33.6782 },
  { id: 'siwa', en: 'Siwa', ar: 'سيوة', glyph: '🌴', lat: 29.2032, lon: 25.5196 },
  { id: 'north_coast', en: 'North Coast', ar: 'الساحل الشمالي', glyph: '🏖️', lat: 30.93, lon: 28.95 },
];

const LEVELS = [
  { min: 0,  en: 'Newcomer',   ar: 'وافد جديد',  badge: '🌱' },
  { min: 1,  en: 'Explorer',   ar: 'مستكشف',     badge: '🧭' },
  { min: 4,  en: 'Adventurer', ar: 'مغامر',      badge: '⛰️' },
  { min: 8,  en: 'Nile Master',ar: 'سيّد النيل', badge: '👑' },
  { min: 12, en: 'Global Soul',ar: 'روح العالم', badge: '🌍' },
];

const I18N = {
  ar: {
    dir: 'rtl', langBtn: 'EN',
    tabPassport: 'الجواز', tabMap: 'الخريطة', tabHara: 'الحارة', tabSouk: 'السوق',
    heroSub: 'جواز سفرك المصري — الأختام تتجمع من أماكن حقيقية، مش من الشاشة',
    stampsOf: (n, t) => `${n} من ${t} ختم`,
    nextLevel: (name, n) => n > 0 ? `باقي ${n} ${n === 1 ? 'ختم' : 'أختام'} لرتبة «${name}»` : 'وصلت لأعلى رتبة 🎉',
    demo: 'عرض تجريبي للشكل', demoOn: 'العرض التجريبي شغال',
    stampsTitle: 'أختام مصر', levelsTitle: 'الرتب',
    levelReq: (n) => n === 0 ? 'البداية' : `${n} ${n === 1 ? 'ختم' : 'أختام'}`,
    earnedOn: (d) => `اتختّم ${d}`,
    mapTitle: 'الخريطة الحيّة',
    prayerTitle: 'أوقات الصلاة — القاهرة',
    quakeTitle: 'زلازل قريبة (آخر ٢٤ ساعة)',
    noQuakes: 'لا زلازل مسجّلة قرب مصر في آخر ٢٤ ساعة',
    weatherNow: 'الطقس الآن',
    legendCities: 'مدن الجواز', legendQuakes: 'زلازل', legendStories: 'قصص بردي',
    readMore: 'اقرأ على ويكيبيديا', storiesLoading: 'بنجيب القصص…', noStories: 'مفيش قصص قريبة هنا — قرّب أو حرّك الخريطة',
    loading: 'بيحمّل…', mag: 'القوة',
    mapBody: 'خريطة العالم ببيانات حيّة مجانية: الطقس، الزلازل، أوقات الصلاة، الأعياد، وقصص البردي المثبّتة على الأماكن. المراكب الشراعية وغرف الصوت على النيل جاية هنا كمان.',
    haraTitle: 'الحارة المصرية',
    haraBody: 'حارة افتراضية تلاقي فيها الناس: قهوة بلدي، صوت الطابية، وترجمة فورية على جهازك من غير ما كلامك يسيب موبايلك.',
    soukTitle: 'سوق التبادل',
    soukBody: 'بدّل قصة بقصة، وصنعة بصنعة. تبادل ثقافي حقيقي مع مسافرين من كل العالم — زي رسايل Bottled بس بالمصري.',
    comingSoon: 'قريبًا',
    tagline: 'روح العالم — بأصل مصري',
  },
  en: {
    dir: 'ltr', langBtn: 'عربي',
    tabPassport: 'Passport', tabMap: 'Map', tabHara: 'Hara', tabSouk: 'Souk',
    heroSub: 'Your Egyptian passport — stamps are earned in real places, not on a screen',
    stampsOf: (n, t) => `${n} of ${t} stamps`,
    nextLevel: (name, n) => n > 0 ? `${n} more ${n === 1 ? 'stamp' : 'stamps'} to «${name}»` : 'Top level reached 🎉',
    demo: 'Demo preview', demoOn: 'Demo preview on',
    stampsTitle: 'Egypt stamps', levelsTitle: 'Ranks',
    levelReq: (n) => n === 0 ? 'Start' : `${n} ${n === 1 ? 'stamp' : 'stamps'}`,
    earnedOn: (d) => `Stamped ${d}`,
    mapTitle: 'The live map',
    prayerTitle: 'Prayer times — Cairo',
    quakeTitle: 'Nearby earthquakes (last 24h)',
    noQuakes: 'No recorded earthquakes near Egypt in the last 24h',
    weatherNow: 'Weather now',
    legendCities: 'Passport cities', legendQuakes: 'Earthquakes', legendStories: 'Papyrus stories',
    readMore: 'Read on Wikipedia', storiesLoading: 'Finding stories…', noStories: 'No stories nearby — zoom in or move the map',
    loading: 'Loading…', mag: 'Mag',
    mapBody: 'A world map fed by free live data: weather, earthquakes, prayer times, holidays, and papyrus stories pinned to real places. Nile sailboats and voice rooms land here too.',
    haraTitle: 'The Egyptian hara',
    haraBody: 'A virtual alley to meet people: a baladi café, backgammon sounds, and instant translation on your device — your words never leave your phone.',
    soukTitle: 'The exchange souk',
    soukBody: 'Trade a story for a story, a craft for a craft. Real cultural exchange with travelers worldwide — Bottled letters, Egyptian style.',
    comingSoon: 'Coming soon',
    tagline: 'Global soul — Egyptian origin',
  },
};

const state = {
  lang: localStorage.getItem('nilora.lang') || 'ar',
  demo: localStorage.getItem('nilora.demo') === '1',
};

// Demo mode lights up the first three stamps so the earned look is visible.
function earnedCount() { return state.demo ? 3 : 0; }
function levelFor(n) { return LEVELS.filter(l => n >= l.min).pop(); }
function nextLevel(l) { const i = LEVELS.indexOf(l); return LEVELS[i + 1] || null; }

function t() { return I18N[state.lang]; }

function cartoucheSVG() {
  const label = 'NILORA';
  return `
  <svg class="cartouche" viewBox="0 0 200 88" role="img" aria-label="cartouche">
    <path d="M100 6 C 40 6 14 24 14 44 C 14 64 40 82 100 82 C 160 82 186 64 186 44 C 186 24 160 6 100 6 Z"
      fill="none" stroke="#C6A15B" stroke-width="3"/>
    <path d="M100 12 C 46 12 20 27 20 44 C 20 61 46 76 100 76 C 154 76 180 61 180 44 C 180 27 154 12 100 12 Z"
      fill="none" stroke="#C6A15B" stroke-width="1.4" stroke-dasharray="5 4" opacity="0.7"/>
    <rect x="14" y="40" width="7" height="8" fill="#C6A15B"/>
    <text x="100" y="40" text-anchor="middle" font-family="Cairo, sans-serif" font-weight="900"
      font-size="17" fill="#C6A15B" letter-spacing="4">${label}</text>
    <text x="100" y="62" text-anchor="middle" font-size="16" fill="#C6A15B" opacity="0.9">☥ ☥ ☥</text>
  </svg>`;
}

function stampHTML(city, earned, i) {
  const name = city[state.lang];
  const date = earned ? `<div class="stamp-date">${t().earnedOn('٢٠٢٦/١١')}</div>` : '';
  const glyph = earned ? city.glyph : '🔒';
  return `
    <div class="stamp ${earned ? 'earned' : 'locked'}">
      <div class="stamp-ring"><span class="stamp-glyph">${glyph}</span></div>
      <div class="stamp-name">${name}</div>
      ${date}
    </div>`;
}

function renderPassport() {
  const n = earnedCount(), lvl = levelFor(n), nxt = nextLevel(lvl);
  const lang = state.lang;
  const pct = Math.round((n / CITIES.length) * 100);
  const need = nxt ? nxt.min - n : 0;
  const nextName = nxt ? nxt[lang] : '';
  return `
    <section class="hero">
      ${cartoucheSVG()}
      <div class="hero-level">${lvl.badge} ${lvl[lang]}</div>
      <div class="hero-sub">${t().heroSub}</div>
      <div class="progress-wrap"><div class="progress-bar" style="width:${pct}%"></div></div>
      <div class="progress-label">${t().stampsOf(n, CITIES.length)} · ${pct}%</div>
      <div class="next-level">${t().nextLevel(nextName, need)}</div>
      <button id="demoToggle" class="demo-toggle ${state.demo ? 'on' : ''}" type="button">
        ${state.demo ? t().demoOn : t().demo}
      </button>
    </section>

    <h2 class="section-title">${t().stampsTitle}</h2>
    <div class="stamp-grid">
      ${CITIES.map((c, i) => stampHTML(c, i < n, i)).join('')}
    </div>

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

/* ---- live map (OpenStreetMap + Open-Meteo + USGS + Aladhan, all keyless) ---- */

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
  marker.bindPopup(`<b>${name}</b><br>${t().loading}`).openPopup();
  try {
    const url = 'https://api.open-meteo.com/v1/forecast?latitude=' + city.lat + '&longitude=' + city.lon +
      '&current=temperature_2m,weather_code,wind_speed_10m&timezone=Africa%2FCairo';
    const r = await fetch(url);
    const j = await r.json();
    const cur = j.current;
    marker.setPopupContent(
      `<b>${name}</b><br>${t().weatherNow}: ${weatherLabel(cur.weather_code)} · ` +
      `${Math.round(cur.temperature_2m)}°C · 💨 ${Math.round(cur.wind_speed_10m)} km/h`);
  } catch (e) {
    marker.setPopupContent(`<b>${name}</b>`);
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

/* ---- papyrus stories: Wikipedia geosearch around the visible map ---- */

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
      L.marker([p.lat, p.lon], {
        icon: L.divIcon({ className: 'story-marker', html: '📜', iconSize: [28, 28] }),
      }).addTo(storyLayer).bindPopup(storyPopupHTML(p, ex), { maxWidth: 280 });
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
  mapObj = L.map('map', { scrollWheelZoom: false }).setView([27.5, 30.5], 5);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18, attribution: '&copy; OpenStreetMap',
  }).addTo(mapObj);
  CITIES.forEach(city => {
    const m = L.marker([city.lat, city.lon], {
      icon: L.divIcon({ className: 'city-marker', html: city.glyph, iconSize: [30, 30] }),
    }).addTo(mapObj);
    m.on('click', () => cityPopup(m, city));
  });
  loadPrayerTimes();
  loadQuakes(mapObj);
  watchStories();
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
  else app.innerHTML = renderSoon(route);
  if (route === 'map') initMap();

  const demoBtn = document.getElementById('demoToggle');
  if (demoBtn) demoBtn.addEventListener('click', () => {
    state.demo = !state.demo;
    localStorage.setItem('nilora.demo', state.demo ? '1' : '0');
    render();
  });
}

document.getElementById('langToggle').addEventListener('click', () => {
  state.lang = state.lang === 'ar' ? 'en' : 'ar';
  localStorage.setItem('nilora.lang', state.lang);
  render();
});

window.addEventListener('hashchange', render);
render();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
