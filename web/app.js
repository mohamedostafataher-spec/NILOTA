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
    tabPassport: 'الجواز', tabMap: 'الخريطة', tabGlobe: 'العالم ٣D', tabTravel: 'سافر', tabHara: 'الحارة', tabSouk: 'السوق',
    globeTitle: 'العالم ثلاثي الأبعاد', globeHint: 'لفّ الكوكب بإيدك ودوس على أي مدينة — النهار والليل حقيقيين حسب الوقت دلوقتي',
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
    tabHome: 'الرئيسية', tabCards: 'بطاقات',
    homeWelcome: 'أهلاً يا مسافر', homeSub: 'منصة المغامرة والتواصل العالمية — بأصل مصري',
    hubJourney: 'رحلتي', hubJourneySub: 'الجواز والأختام والرتب',
    hubMap: 'الخريطة الحيّة', hubMapSub: 'طقس وزلازل ومواقيت وقصص',
    hubGlobe: 'العالم ٣D', hubGlobeSub: 'الكوكب الحقيقي بنور الشمس دلوقتي',
    hubTravel: 'وضع المسافر', hubTravelSub: 'عملة ووقت وكلمات البلد',
    hubCards: 'بطاقات العالم', hubCardsSub: 'سيب رسالة في مكان… مسافر تاني يلاقيها',
    hubMissions: 'مهام وتحديات', hubMissionsSub: 'نقاط ورتب وجوايز',
    searchPlaceholder: 'دوّر على مدينة أو دولة…',
    cardsTitle: 'بطاقات العالم', cardsSub: 'رسالة قصيرة متعلّقة في مكان على الخريطة — مسافر تاني يعدّي يلاقيها',
    cardNew: 'ابعت بطاقة جديدة', cardWhere: 'اختار المكان', cardMsgPh: 'اكتب رسالتك (١٤٠ حرف)…', cardSend: 'علّقها على الخريطة',
    cardYours: 'بطاقاتك', cardFound: 'بطاقات لقيتها', cardJourney: 'رحلة البطاقة',
    cardReport: 'بلاغ', cardBlock: 'حظر', cardSent: 'اتعلّقت على الخريطة 📍',
    cardEmpty: 'لسه مفيش بطاقات — ابعت أول واحدة',
    cardFrom: (c) => `مسافر من ${c}`,
    memoryTitle: 'ذاكرة نيلورا', memorySub: 'كل خطوة في رحلتك متسجّلة هنا',
    memoryEmpty: 'الذاكرة لسه فاضية — ابدأ اجمع أختام ونقاط',
    memStamp: (n) => `ختم جديد: ${n}`, memMission: (n, p) => `مهمة خلصت: ${n} (+${p})`, memStart: 'بداية الرحلة',
    soukTitle2: 'سوق التبادل الثقافي', soukSub2: 'اعرض حاجة من ثقافتك واطلب حاجة من ثقافة تانية — تبادل حقيقي بين مسافرين',
    swapOffer: 'بتعرض إيه؟', swapWant: 'عايز إيه في المقابل؟', swapCity: 'منين؟', swapBtn: 'انشر العرض',
    swapYours: 'عروضك', swapOpen: 'عروض مفتوحة', swapEmpty: 'لسه مفيش عروض — انشر أول واحد',
    swapDone: 'اتنشر العرض 🎉', swapInterest: 'مهتم',
    savedTitle: 'الأماكن المحفوظة', savedEmpty: 'لسه محفظتش أماكن — احفظ من وضع المسافر',
    savePlace: 'احفظ المكان', savedDone: 'اتحفظ ✅', savedRemove: 'شيل',
    eventsTitle: 'أحداث العالم الجاية', eventsSub: 'الأعياد الرسمية الجاية حوالين العالم',
    aboutTitle: 'عن نيلورا', aboutVersion: 'نسخة الويب v7 — شغالة كلها بدون مفاتيح API',

    missionsTitle: 'مهام النهاردة', points: (p) => `${p} نقطة`, done: 'تم ✅',
    rareTitle: 'ختم نادر', rareBody: 'الجعران الذهبي — اجمع ١٠٠ نقطة',
    shareBtn: 'شارك جوازك', shareDone: 'اتنزّلت الصورة 🎉',
    unlockTitle: 'ختم جديد!',
    cities: (n) => `${n} مدينة`,
  },

  fr: {
    dir: 'ltr', langBtn: 'RU',
    tabHome: 'Accueil', tabCards: 'Cartes',
    homeWelcome: 'Bienvenue, voyageur', homeSub: 'La plateforme mondiale d’aventure et de connexion — origine égyptienne',
    searchPlaceholder: 'Cherche une ville ou un pays…',
    cardsTitle: 'Cartes du monde', cardsSub: 'Un court message épinglé sur la carte — un autre voyageur le trouve',
    cardNew: 'Envoyer une carte', cardWhere: 'Choisis le lieu', cardMsgPh: 'Ton message (140 car.)…', cardSend: 'Épingler sur la carte',
    cardYours: 'Tes cartes', cardFound: 'Cartes trouvées', cardJourney: 'Voyage de la carte',
    cardReport: 'Signaler', cardBlock: 'Bloquer', cardSent: 'Épinglée 📍', cardEmpty: 'Pas encore de cartes — envoie la première',
    memoryTitle: 'Mémoire NILORA', memorySub: 'Chaque étape de ton voyage est enregistrée ici',
    tabPassport: 'Passeport', tabMap: 'Carte', tabGlobe: 'Monde 3D', tabTravel: 'Voyage', tabHara: 'Hara', tabSouk: 'Souk',
    globeTitle: 'Le monde en 3D', globeHint: 'Fais tourner le globe et touche une ville — le jour et la nuit suivent l’heure réelle',
    travelTitle: 'Mode voyageur', chooseDest: 'Choisis ta destination…', currencyLabel: 'Monnaie',
    phrasesTitle: 'Apprends 5 mots locaux', localTimeLabel: 'Heure locale',
    eventTitle: 'Fête du jour dans le monde', eventClaim: 'Gagner +20 pts', eventClaimed: 'Gagné ✅',
    weeklyTitle: 'Défi de la semaine', weeklyBody: 'Collecte 5 tampons de 3 pays différents cette semaine', weeklyReward: 'Récompense : tampon rare + 50 pts',
    boardTitle: 'Classement (local)', youRow: 'Toi', mockNote: 'Le vrai classement en ligne arrive avec le backend',
    noEvent: 'Pas de fête officielle aujourd’hui — reviens demain', highLow: 'Max/Min',
    heroSub: 'Ton passeport du monde — les tampons se gagnent dans des lieux réels',
    stampsOf: (n, t) => `${n} sur ${t} tampons`,
    nextLevel: (name, n) => n > 0 ? `Encore ${n} tampon${n === 1 ? '' : 's'} pour «${name}»` : 'Niveau max atteint 🎉',
    demo: 'Mode démo', demoOn: 'Démo active — touche un tampon verrouillé',
    stampsTitle: 'Tampons', levelsTitle: 'Rangs',
    levelReq: (n) => n === 0 ? 'Début' : `${n}+`,
    earnedOn: (d) => `Tamponné ${d}`,
    mapTitle: 'La carte en direct',
    prayerTitle: 'Heures de prière — Le Caire',
    quakeTitle: 'Séismes proches (24 h)',
    noQuakes: 'Aucun séisme près de l’Égypte ces dernières 24 h',
    weatherNow: 'Météo actuelle',
    legendCities: 'Villes du passeport', legendQuakes: 'Séismes', legendStories: 'Histoires papyrus',
    readMore: 'Lire sur Wikipédia', storiesLoading: 'Recherche d’histoires…', noStories: 'Pas d’histoires ici — zoome ou déplace la carte',
    loading: 'Chargement…', mag: 'Mag',
    mapBody: '', haraTitle: 'Le hara égyptien',
    haraBody: 'Une ruelle virtuelle pour rencontrer les gens : café baladi, sons de tawla, traduction instantanée sur ton appareil.',
    soukTitle: 'Le souk d’échange',
    soukBody: 'Échange une histoire contre une histoire, un artisanat contre un artisanat. Un vrai échange culturel avec des voyageurs du monde entier.',
    comingSoon: 'Bientôt',
    tagline: 'Âme du monde — origine égyptienne',
    missionsTitle: 'Missions du jour', points: (p) => `${p} pts`, done: 'Fait ✅',
    rareTitle: 'Tampon rare', rareBody: 'Le Scarabée d’or — collecte 100 points',
    shareBtn: 'Partage ton passeport', shareDone: 'Image téléchargée 🎉',
    unlockTitle: 'Nouveau tampon !',
    cities: (n) => `${n} villes`,
  },
  ru: {
    dir: 'ltr', langBtn: 'عربي',
    tabHome: 'Домой', tabCards: 'Открытки',
    homeWelcome: 'Привет, путешественник', homeSub: 'Мировая платформа приключений и общения — египетские корни',
    searchPlaceholder: 'Найди город или страну…',
    cardsTitle: 'Открытки мира', cardsSub: 'Короткое сообщение, приколотое к точке на карте — другой путешественник его найдёт',
    cardNew: 'Отправить открытку', cardWhere: 'Выбери место', cardMsgPh: 'Твоё сообщение (140 зн.)…', cardSend: 'Приколоть на карту',
    cardYours: 'Твои открытки', cardFound: 'Найденные открытки', cardJourney: 'Путь открытки',
    cardReport: 'Жалоба', cardBlock: 'Блок', cardSent: 'Приколота 📍', cardEmpty: 'Открыток пока нет — отправь первую',
    memoryTitle: 'Память NILORA', memorySub: 'Каждый шаг твоего пути записан здесь',
    tabPassport: 'Паспорт', tabMap: 'Карта', tabGlobe: 'Мир 3D', tabTravel: 'Путь', tabHara: 'Хара', tabSouk: 'Сук',
    globeTitle: 'Мир в 3D', globeHint: 'Крути глобус и нажимай на города — день и ночь идут по настоящему времени',
    travelTitle: 'Режим путешественника', chooseDest: 'Выбери направление…', currencyLabel: 'Валюта',
    phrasesTitle: 'Выучи 5 местных слов', localTimeLabel: 'Местное время',
    eventTitle: 'Сегодняшний праздник в мире', eventClaim: 'Забрать +20 очков', eventClaimed: 'Забрано ✅',
    weeklyTitle: 'Вызов недели', weeklyBody: 'Собери 5 печатей из 3 разных стран на этой неделе', weeklyReward: 'Награда: редкая печать + 50 очков',
    boardTitle: 'Лидеры (локально)', youRow: 'Ты', mockNote: 'Настоящий онлайн-рейтинг появится с бэкендом',
    noEvent: 'Сегодня официального праздника нет — загляни завтра', highLow: 'Макс/Мин',
    heroSub: 'Твой паспорт мира — печати зарабатываются в реальных местах',
    stampsOf: (n, t) => `${n} из ${t} печатей`,
    nextLevel: (name, n) => n > 0 ? `Ещё ${n} печатей до «${name}»` : 'Максимальный уровень 🎉',
    demo: 'Демо-режим', demoOn: 'Демо включено — нажми на закрытую печать',
    stampsTitle: 'Печати', levelsTitle: 'Ранги',
    levelReq: (n) => n === 0 ? 'Старт' : `${n}+`,
    earnedOn: (d) => `Поставлена ${d}`,
    mapTitle: 'Живая карта',
    prayerTitle: 'Время молитв — Каир',
    quakeTitle: 'Ближние землетрясения (24 ч)',
    noQuakes: 'Землетрясений рядом с Египтом за сутки нет',
    weatherNow: 'Погода сейчас',
    legendCities: 'Города паспорта', legendQuakes: 'Землетрясения', legendStories: 'Истории папируса',
    readMore: 'Читать в Википедии', storiesLoading: 'Ищем истории…', noStories: 'Рядом историй нет — приблизь или подвинь карту',
    loading: 'Загрузка…', mag: 'Маг',
    mapBody: '', haraTitle: 'Египетская хара',
    haraBody: 'Виртуальный переулок, где встречаются люди: кофе балади, звуки тавлы и мгновенный перевод прямо на устройстве.',
    soukTitle: 'Сук обмена',
    soukBody: 'Меняй историю на историю, ремесло на ремесло. Настоящий культурный обмен с путешественниками со всего мира.',
    comingSoon: 'Скоро',
    tagline: 'Душа мира — египетские корни',
    missionsTitle: 'Задания дня', points: (p) => `${p} очков`, done: 'Готово ✅',
    rareTitle: 'Редкая печать', rareBody: 'Золотой скарабей — набери 100 очков',
    shareBtn: 'Поделись паспортом', shareDone: 'Картинка скачана 🎉',
    unlockTitle: 'Новая печать!',
    cities: (n) => `${n} городов`,
  },
  en: {
    dir: 'ltr', langBtn: 'FR',
    tabPassport: 'Passport', tabMap: 'Map', tabGlobe: '3D World', tabTravel: 'Travel', tabHara: 'Hara', tabSouk: 'Souk',
    globeTitle: 'The 3D world', globeHint: 'Spin the globe and tap any city — day and night follow real time',
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
    tabHome: 'Home', tabCards: 'Postcards',
    homeWelcome: 'Welcome, traveler', homeSub: 'The global adventure and connection platform — Egyptian origin',
    hubJourney: 'My journey', hubJourneySub: 'Passport, stamps and ranks',
    hubMap: 'Live map', hubMapSub: 'Weather, quakes, prayer times, stories',
    hubGlobe: '3D world', hubGlobeSub: 'The real planet under live sunlight',
    hubTravel: 'Traveler mode', hubTravelSub: 'Currency, time and local words',
    hubCards: 'World postcards', hubCardsSub: 'Leave a message in a place… another traveler finds it',
    hubMissions: 'Missions and challenges', hubMissionsSub: 'Points, ranks and rewards',
    searchPlaceholder: 'Search a city or country…',
    cardsTitle: 'World postcards', cardsSub: 'A short message pinned to a map point — another traveler passing by finds it',
    cardNew: 'Send a new postcard', cardWhere: 'Choose the place', cardMsgPh: 'Write your message (140 chars)…', cardSend: 'Pin it on the map',
    cardYours: 'Your postcards', cardFound: 'Postcards you found', cardJourney: 'Postcard journey',
    cardReport: 'Report', cardBlock: 'Block', cardSent: 'Pinned on the map 📍',
    cardEmpty: 'No postcards yet — send the first one',
    cardFrom: (c) => `A traveler from ${c}`,
    memoryTitle: 'NILORA memory', memorySub: 'Every step of your journey is recorded here',
    memoryEmpty: 'Memory is still empty — start collecting stamps and points',
    memStamp: (n) => `New stamp: ${n}`, memMission: (n, p) => `Mission done: ${n} (+${p})`, memStart: 'The journey begins',
    soukTitle2: 'The culture-swap souk', soukSub2: 'Offer something from your culture, ask for something from another — a real exchange between travelers',
    swapOffer: 'What do you offer?', swapWant: 'What do you want in return?', swapCity: 'From where?', swapBtn: 'Post the offer',
    swapYours: 'Your offers', swapOpen: 'Open offers', swapEmpty: 'No offers yet — post the first one',
    swapDone: 'Offer posted 🎉', swapInterest: 'Interested',
    savedTitle: 'Saved places', savedEmpty: 'No saved places yet — save from traveler mode',
    savePlace: 'Save this place', savedDone: 'Saved ✅', savedRemove: 'Remove',
    eventsTitle: 'Upcoming world events', eventsSub: 'Official public holidays coming up around the world',
    aboutTitle: 'About NILORA', aboutVersion: 'Web v7 — fully keyless, no API keys anywhere',

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

function t() { return Object.assign({}, I18N.en, I18N[state.lang]); }

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
  const mDef = MISSION_POOL.find(x => x.id === id);
  logMemory('mission', t().memMission ? t().memMission(mDef[state.lang] || mDef.en, pts) : (mDef.en + ' +' + pts));
}

function logMemory(type, text) {
  const log = JSON.parse(localStorage.getItem('nilora.memory') || '[]');
  log.push({ t: Date.now(), type, text });
  localStorage.setItem('nilora.memory', JSON.stringify(log.slice(-200)));
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
  const name = (city[state.lang] || city.en);
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
    <div class="unlock-city">${(city[state.lang] || city.en)} ${city.country.flag}</div>`;
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
  const name = (city[state.lang] || city.en);
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
  // your avatar on the map (home: Cairo)
  L.marker([30.0444, 31.2357], {
    icon: L.divIcon({ className: '', html: '<div class="avatar-marker">𓂀</div>', iconSize: [38, 38] }),
    zIndexOffset: 900,
  }).addTo(mapObj).bindTooltip(state.lang === 'ar' ? 'انت هنا' : 'You are here');
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
    const cname = country ? (country[state.lang] || country.en) : h.countryCode;
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
        ${COUNTRIES.map(cn => `<optgroup label="${cn.flag} ${(cn[state.lang] || cn.en)}">
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
  const isSaved = savedPlaces().includes(cityId);
  el.innerHTML = weatherHtml + timeHtml + currHtml + wordsHtml +
    `<button class="btn-gold" id="savePlaceBtn" style="margin-top:10px">${isSaved ? t().savedDone : t().savePlace}</button>`;
  const sp = document.getElementById('savePlaceBtn');
  if (sp && !isSaved) sp.addEventListener('click', () => {
    const s = savedPlaces();
    s.push(cityId);
    localStorage.setItem('nilora.saved', JSON.stringify(s));
    state.points += 5;
    localStorage.setItem('nilora.points', String(state.points));
    logMemory('stamp', city[state.lang] || city.en);
    sp.textContent = t().savedDone;
    sp.disabled = true;
    updateHud();
  });
}

function renderGlobe() {
  const i18n = t();
  return `
    <h2 class="section-title">${i18n.globeTitle}</h2>
    <div id="globe" class="globe-wrap"></div>
    <div class="map-legend"><span>${i18n.globeHint}</span></div>
    <div id="globeCityCard" class="live-panel" style="display:none"></div>
    <div class="soon-card" style="text-align:center"><p style="color:var(--gold-dark);font-weight:700">${i18n.tagline}</p></div>`;
}


/* ---- home hub (AI Studio style) ---- */
function renderHome() {
  const i18n = t();
  const lvl = levelFor(earnedCount());
  return `
    <div class="home-hero">
      <div class="home-hello">${i18n.homeWelcome} 𓂀</div>
      <div class="home-sub">${i18n.homeSub}</div>
      <input id="homeSearch" class="home-search" placeholder="${i18n.searchPlaceholder}" />
      <div id="searchResults" class="search-results"></div>
    </div>
    <div class="hub-grid">
      <a class="hub-card" href="#/passport"><span class="hub-ico">🪪</span><b>${i18n.hubJourney}</b><small>${i18n.hubJourneySub}</small></a>
      <a class="hub-card" href="#/map"><span class="hub-ico">🗺️</span><b>${i18n.hubMap}</b><small>${i18n.hubMapSub}</small></a>
      <a class="hub-card" href="#/globe"><span class="hub-ico">🌍</span><b>${i18n.hubGlobe}</b><small>${i18n.hubGlobeSub}</small></a>
      <a class="hub-card" href="#/travel"><span class="hub-ico">✈️</span><b>${i18n.hubTravel}</b><small>${i18n.hubTravelSub}</small></a>
      <a class="hub-card" href="#/cards"><span class="hub-ico">📮</span><b>${i18n.hubCards}</b><small>${i18n.hubCardsSub}</small></a>
      <a class="hub-card" href="#/memory"><span class="hub-ico">📜</span><b>${i18n.memoryTitle}</b><small>${i18n.memorySub}</small></a>
      <a class="hub-card" href="#/souk"><span class="hub-ico">🏺</span><b>${i18n.soukTitle2}</b><small>${i18n.soukSub2}</small></a>
      <a class="hub-card" href="#/events"><span class="hub-ico">🎉</span><b>${i18n.eventsTitle}</b><small>${i18n.eventsSub}</small></a>
      <a class="hub-card" href="#/saved"><span class="hub-ico">⭐</span><b>${i18n.savedTitle}</b><small>${i18n.savedEmpty}</small></a>
      <a class="hub-card" href="#/about"><span class="hub-ico">☥</span><b>${i18n.aboutTitle}</b><small>${i18n.aboutVersion}</small></a>
    </div>
    <div class="soon-card" style="text-align:center"><p style="color:var(--gold);font-weight:700">${i18n.tagline}</p></div>`;
}

function homeSearch(q) {
  const box = document.getElementById('searchResults');
  if (!box) return;
  q = q.trim().toLowerCase();
  if (q.length < 2) { box.innerHTML = ''; return; }
  const hits = ALL_CITIES.filter(city =>
    city.en.toLowerCase().includes(q) || (city.ar || '').includes(q) ||
    city.country.en.toLowerCase().includes(q) || (city.country.ar || '').includes(q)
  ).slice(0, 8);
  box.innerHTML = hits.map(city =>
    `<a class="search-hit" href="#/travel" data-city="${city.id}">
       <span>${city.country.flag} <b>${city[state.lang] || city.en}</b></span><small>${city.country[state.lang] || city.country.en}</small>
     </a>`).join('') || `<div class="search-hit"><small>…</small></div>`;
  box.querySelectorAll('.search-hit[data-city]').forEach(a => a.addEventListener('click', () => {
    sessionStorage.setItem('nilora.dest', a.dataset.city);
  }));
}

/* ---- world postcards (message to a traveler) ---- */
const CARD_SEED_SENDERS = ['Italy', 'Japan', 'Brazil', 'France', 'Turkey', 'Spain'];

function myCards() { return JSON.parse(localStorage.getItem('nilora.cards') || '[]'); }
function foundCards() { return JSON.parse(localStorage.getItem('nilora.cardsFound') || '[]'); }

function seedFoundCards() {
  if (localStorage.getItem('nilora.cardsFound')) return;
  const seeds = [
    { id: 's1', cityId: 'rome', text: 'Rome at sunrise is worth every early alarm. Leave something kind here too.', from: 'Italy', journey: ['rome', 'athens', 'cairo'] },
    { id: 's2', cityId: 'tokyo', text: 'Found this spot by getting lost on purpose. 10/10 recommend.', from: 'Japan', journey: ['tokyo', 'kyoto', 'beijing'] },
    { id: 's3', cityId: 'rio', text: 'The ocean answers every question if you listen long enough.', from: 'Brazil', journey: ['rio', 'cancun'] },
  ];
  localStorage.setItem('nilora.cardsFound', JSON.stringify(seeds));
}

function cardJourneyHTML(card) {
  const names = (card.journey || [card.cityId]).map(id => {
    const city = ALL_CITIES.find(x => x.id === id);
    return city ? (city[state.lang] || city.en) : id;
  });
  return names.map(n => `<span class="journey-hop">${n}</span>`).join('<span class="journey-arrow">←</span>');
}

function renderCards() {
  const i18n = t();
  seedFoundCards();
  const mine = myCards();
  const found = foundCards();
  const cardRow = (card, own) => {
    const city = ALL_CITIES.find(x => x.id === card.cityId);
    const cname = city ? (city[state.lang] || city.en) : card.cityId;
    const flag = city ? city.country.flag : '📍';
    return `
      <div class="postcard">
        <div class="postcard-head">
          <span>${flag} <b>${cname}</b></span>
          <small>${own ? '' : i18n.cardFrom(card.from)}</small>
        </div>
        <p class="postcard-text">${card.text.replace(/</g, '&lt;')}</p>
        <div class="postcard-journey">${i18n.cardJourney}: ${cardJourneyHTML(card)}</div>
        ${own ? '' : `<div class="postcard-actions"><button class="pc-act" data-report="${card.id}">🚩 ${i18n.cardReport}</button><button class="pc-act" data-block="${card.id}">⛔ ${i18n.cardBlock}</button></div>`}
      </div>`;
  };
  return `
    <h2 class="section-title">${i18n.cardsTitle}</h2>
    <p class="cards-sub">${i18n.cardsSub}</p>
    <div class="live-panel">
      <h3>${i18n.cardNew}</h3>
      <select id="cardCity"><option value="">${i18n.cardWhere}…</option>
        ${ALL_CITIES.map(ct => `<option value="${ct.id}">${ct.country.flag} ${ct[state.lang] || ct.en}</option>`).join('')}
      </select>
      <textarea id="cardText" maxlength="140" rows="2" placeholder="${i18n.cardMsgPh}"></textarea>
      <button id="cardSend" class="btn-gold">${i18n.cardSend}</button>
    </div>
    <h2 class="section-title">${i18n.cardYours}</h2>
    ${mine.length ? mine.map(cd => cardRow(cd, true)).join('') : `<p class="cards-empty">${i18n.cardEmpty}</p>`}
    <h2 class="section-title">${i18n.cardFound}</h2>
    ${found.map(cd => cardRow(cd, false)).join('')}`;
}

function sendCard() {
  const cityId = document.getElementById('cardCity').value;
  const text = document.getElementById('cardText').value.trim();
  if (!cityId || !text) return;
  const mine = myCards();
  mine.unshift({ id: 'c' + Date.now(), cityId, text, journey: [cityId], ts: Date.now() });
  localStorage.setItem('nilora.cards', JSON.stringify(mine));
  state.points += 15;
  localStorage.setItem('nilora.points', String(state.points));
  const city = ALL_CITIES.find(x => x.id === cityId);
  logMemory('card', (city ? (city[state.lang] || city.en) : cityId) + ': ' + text.slice(0, 40));
  render();
}

/* ---- NILORA memory ---- */
function renderMemory() {
  const i18n = t();
  const log = JSON.parse(localStorage.getItem('nilora.memory') || '[]');
  const ico = { stamp: '🔖', mission: '✅', card: '📮' };
  const items = log.slice().reverse().map(e => `
    <div class="memory-row">
      <span class="memory-ico">${ico[e.type] || '•'}</span>
      <div><div class="memory-text">${String(e.text).replace(/</g, '&lt;')}</div>
      <small class="memory-time">${new Date(e.t).toLocaleString(state.lang === 'ar' ? 'ar-EG' : state.lang)}</small></div>
    </div>`).join('');
  return `
    <h2 class="section-title">${i18n.memoryTitle}</h2>
    <p class="cards-sub">${i18n.memorySub}</p>
    ${items || `<p class="cards-empty">${i18n.memoryEmpty}</p>`}
    <div class="memory-row"><span class="memory-ico">☥</span><div><div class="memory-text">${i18n.memStart}</div></div></div>`;
}


/* ---- culture-swap souk ---- */
function mySwaps() { return JSON.parse(localStorage.getItem('nilora.swaps') || '[]'); }

function seedSwaps() {
  if (localStorage.getItem('nilora.swapsSeeded')) return;
  localStorage.setItem('nilora.swapsSeeded', '1');
  const seeds = [
    { id: 'w1', offer: 'A hand-written Arabic calligraphy of your name', want: 'A local recipe from your hometown', from: 'Egypt', cityId: 'cairo', own: false },
    { id: 'w2', offer: 'A playlist of 20 underground Cairo tracks', want: 'A voice note teaching me your favorite tongue twister', from: 'Egypt', cityId: 'alexandria', own: false },
    { id: 'w3', offer: 'Origami crane + folding tutorial', want: 'A photo of your city at dawn', from: 'Japan', cityId: 'kyoto', own: false },
  ];
  localStorage.setItem('nilora.swapsFeed', JSON.stringify(seeds));
}

function swapRow(s, own) {
  const i18n = t();
  const city = ALL_CITIES.find(x => x.id === s.cityId);
  const cname = city ? (city[state.lang] || city.en) : '';
  const flag = city ? city.country.flag : '🌍';
  return `
    <div class="postcard">
      <div class="postcard-head"><span>${flag} <b>${cname}</b></span><small>${own ? '' : s.from}</small></div>
      <p class="postcard-text"><b>${i18n.swapOffer}</b> ${s.offer.replace(/</g,'&lt;')}<br/><b>${i18n.swapWant}</b> ${s.want.replace(/</g,'&lt;')}</p>
      ${own ? '' : `<div class="postcard-actions"><button class="pc-act" data-interest="${s.id}">🤝 ${i18n.swapInterest}</button><button class="pc-act" data-report="${s.id}">🚩 ${i18n.cardReport}</button></div>`}
    </div>`;
}

function renderSouk() {
  const i18n = t();
  seedSwaps();
  const mine = mySwaps();
  const feed = JSON.parse(localStorage.getItem('nilora.swapsFeed') || '[]');
  return `
    <h2 class="section-title">${i18n.soukTitle2}</h2>
    <p class="cards-sub">${i18n.soukSub2}</p>
    <div class="live-panel">
      <input id="swapOffer" placeholder="${i18n.swapOffer}" />
      <input id="swapWant" placeholder="${i18n.swapWant}" />
      <select id="swapCity"><option value="">${i18n.swapCity}…</option>
        ${ALL_CITIES.map(ct => `<option value="${ct.id}">${ct.country.flag} ${ct[state.lang] || ct.en}</option>`).join('')}
      </select>
      <button id="swapSend" class="btn-gold">${i18n.swapBtn}</button>
    </div>
    <h2 class="section-title">${i18n.swapYours}</h2>
    ${mine.length ? mine.map(s => swapRow(s, true)).join('') : `<p class="cards-empty">${i18n.swapEmpty}</p>`}
    <h2 class="section-title">${i18n.swapOpen}</h2>
    ${feed.map(s => swapRow(s, false)).join('')}`;
}

function sendSwap() {
  const offer = document.getElementById('swapOffer').value.trim();
  const want = document.getElementById('swapWant').value.trim();
  const cityId = document.getElementById('swapCity').value;
  if (!offer || !want || !cityId) return;
  const mine = mySwaps();
  mine.unshift({ id: 'w' + Date.now(), offer, want, cityId, ts: Date.now() });
  localStorage.setItem('nilora.swaps', JSON.stringify(mine));
  state.points += 10;
  localStorage.setItem('nilora.points', String(state.points));
  logMemory('card', offer.slice(0, 50));
  render();
}

/* ---- saved places ---- */
function savedPlaces() { return JSON.parse(localStorage.getItem('nilora.saved') || '[]'); }

function renderSaved() {
  const i18n = t();
  const saved = savedPlaces().map(id => ALL_CITIES.find(x => x.id === id)).filter(Boolean);
  return `
    <h2 class="section-title">${i18n.savedTitle}</h2>
    ${saved.length ? saved.map(city => `
      <div class="postcard">
        <div class="postcard-head"><span>${city.country.flag} <b>${city[state.lang] || city.en}</b></span>
        <button class="pc-act" data-unsave="${city.id}">${i18n.savedRemove}</button></div>
        <p class="postcard-text">${city.country[state.lang] || city.country.en}</p>
      </div>`).join('') : `<p class="cards-empty">${i18n.savedEmpty}</p>`}`;
}

/* ---- upcoming world events ---- */
function renderEvents() {
  const i18n = t();
  return `
    <h2 class="section-title">${i18n.eventsTitle}</h2>
    <p class="cards-sub">${i18n.eventsSub}</p>
    <div class="live-panel"><div id="eventsList" class="rows"><span class="live-loading">${i18n.loading}</span></div></div>`;
}

async function loadEvents() {
  const el = document.getElementById('eventsList');
  if (!el) return;
  try {
    const r = await fetch('https://date.nager.at/api/v3/NextPublicHolidaysWorldwide');
    const data = await r.json();
    el.innerHTML = data.slice(0, 15).map(e => {
      const cc = COUNTRIES.find(x => x.code === e.countryCode);
      const flag = cc ? cc.flag : '🌍';
      const cname = cc ? (cc[state.lang] || cc.en) : e.countryCode;
      return `<div class="row"><span>${flag} <b>${e.localName || e.name}</b></span><small>${cname} · ${e.date}</small></div>`;
    }).join('');
  } catch (e) { el.innerHTML = `<span class="live-loading">${t().noEvent}</span>`; }
}

/* ---- about / settings ---- */
function renderAbout() {
  const i18n = t();
  return `
    <h2 class="section-title">${i18n.aboutTitle}</h2>
    <div class="live-panel" style="text-align:center">
      <p style="color:var(--gold);font-weight:900;letter-spacing:2px;line-height:2">NILORA — BUILT TO BE INDEPENDENT.<br/>CONNECTED TO THE REAL WORLD.<br/>THE WORLD CONNECTS THROUGH NILORA.</p>
      <p class="cards-sub">Created &amp; Founded by Mohamed Mostafa Taher.</p>
      <p class="cards-sub">${i18n.aboutVersion}</p>
      <p class="cards-sub">OpenStreetMap · Open-Meteo · USGS · Aladhan · Wikipedia · Nager.Date · open.er-api.com · Natural Earth</p>
    </div>`;
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
  const route = hashParts[0] || 'home';
  if ((hashParts[1] || '').includes('demo=1') && !state.demo) {
    state.demo = true;
    localStorage.setItem('nilora.demo', '1');
  }
  const app = document.getElementById('app');
  document.documentElement.lang = state.lang;
  document.documentElement.dir = t().dir;
  document.getElementById('langToggle').textContent = t().langBtn;
  updateHud();
  document.querySelectorAll('.tab').forEach(tab =>
    tab.classList.toggle('active', tab.dataset.route === route));
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const k = el.dataset.i18n; if (t()[k]) el.textContent = t()[k];
  });

  if (mapObj && route !== 'map') { mapObj.remove(); mapObj = null; }
  if (globeState && route !== 'globe') { globeState.dispose(); globeState = null; }
  if (route === 'passport') app.innerHTML = renderPassport();
  else if (route === 'travel') app.innerHTML = renderTravel();
  else if (route === 'globe') app.innerHTML = renderGlobe();
  else if (route === 'cards') app.innerHTML = renderCards();
  else if (route === 'memory') app.innerHTML = renderMemory();
  else if (route === 'home') app.innerHTML = renderHome();
  else if (route === 'souk') app.innerHTML = renderSouk();
  else if (route === 'saved') app.innerHTML = renderSaved();
  else if (route === 'events') app.innerHTML = renderEvents();
  else if (route === 'about') app.innerHTML = renderAbout();
  else app.innerHTML = renderSoon(route);
  if (route === 'map') { initMap(); loadWorldEvent(); }
  if (route === 'events') loadEvents();
  if (route === 'globe') initGlobe();

  const destSelect = document.getElementById('destSelect');
  if (destSelect) destSelect.addEventListener('change', () => loadDestination(destSelect.value));
  const preDest = sessionStorage.getItem('nilora.dest');
  if (preDest && destSelect) {
    sessionStorage.removeItem('nilora.dest');
    destSelect.value = preDest;
    loadDestination(preDest);
  }

  const demoBtn = document.getElementById('demoToggle');
  if (demoBtn) demoBtn.addEventListener('click', () => {
    state.demo = !state.demo;
    localStorage.setItem('nilora.demo', state.demo ? '1' : '0');
    render();
  });

  const swapSendBtn = document.getElementById('swapSend');
  if (swapSendBtn) swapSendBtn.addEventListener('click', sendSwap);
  document.querySelectorAll('[data-interest]').forEach(b => b.addEventListener('click', () => { b.textContent = '✅'; b.disabled = true; }));
  document.querySelectorAll('[data-unsave]').forEach(b => b.addEventListener('click', () => {
    const left = savedPlaces().filter(id => id !== b.dataset.unsave);
    localStorage.setItem('nilora.saved', JSON.stringify(left));
    render();
  }));

  const cardSendBtn = document.getElementById('cardSend');
  if (cardSendBtn) cardSendBtn.addEventListener('click', sendCard);
  document.querySelectorAll('[data-report]').forEach(b => b.addEventListener('click', () => { b.textContent = '✅'; b.disabled = true; }));
  document.querySelectorAll('[data-block]').forEach(b => b.addEventListener('click', () => { b.textContent = '✅'; b.disabled = true; }));
  const hs = document.getElementById('homeSearch');
  if (hs) hs.addEventListener('input', () => homeSearch(hs.value));

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
      logMemory('stamp', t().memStamp ? t().memStamp(city[state.lang] || city.en) : city.en);
      showUnlock(city);
      setTimeout(render, 2000);
    });
  });
}

const LANG_ORDER = ['ar', 'en', 'fr', 'ru'];
const ONBOARD_LANGS = [
  { code: 'en', name: 'English', desc: 'English • LTR' },
  { code: 'ar', name: 'العربية (المصرية)', desc: 'Arabic (incl. Egyptian) • RTL' },
  { code: 'fr', name: 'Français', desc: 'French • LTR' },
  { code: 'ru', name: 'Русский', desc: 'Russian • LTR' },
];

function showOnboarding() {
  const el = document.createElement('div');
  el.className = 'onboard';
  el.id = 'onboard';
  el.innerHTML = `
    <div style="color:var(--gold);letter-spacing:4px;font-size:12px;font-weight:700">NILORA</div>
    <div class="onboard-title">Select Your Language</div>
    <div class="onboard-sub">Arabic supports full Right-to-Left (RTL) layout. English, French, and Russian use Left-to-Right (LTR).</div>
    ${ONBOARD_LANGS.map((l, i) => `
      <button class="onboard-lang${i === 0 ? ' sel' : ''}" data-lang="${l.code}">
        <span class="ol-name">${l.name}</span><span class="ol-desc">${l.desc}</span>
      </button>`).join('')}
    <div class="onboard-foot">NILORA • GLOBAL SOUL — EGYPTIAN ORIGIN<br/>ONE APP, ONE WORLD, NO LANGUAGE BARRIERS</div>
    <button class="onboard-next" id="onboardNext">Next</button>`;
  document.body.appendChild(el);
  el.querySelectorAll('.onboard-lang').forEach(b => b.addEventListener('click', () => {
    el.querySelectorAll('.onboard-lang').forEach(x => x.classList.remove('sel'));
    b.classList.add('sel');
  }));
  el.querySelector('#onboardNext').addEventListener('click', () => {
    state.lang = el.querySelector('.onboard-lang.sel').dataset.lang;
    localStorage.setItem('nilora.lang', state.lang);
    el.remove();
    completeMission('languages');
    render();
  });
}

function updateHud() {
  const lv = Math.floor(state.points / 100) + 1;
  const el = document.getElementById('hudLevel');
  const pp = document.getElementById('hudPoints');
  if (el) el.textContent = 'Lv ' + lv;
  if (pp) pp.textContent = state.points + ' pts';
}

document.getElementById('langToggle').addEventListener('click', () => {
  state.lang = LANG_ORDER[(LANG_ORDER.indexOf(state.lang) + 1) % LANG_ORDER.length];
  localStorage.setItem('nilora.lang', state.lang);
  completeMission('languages');
  render();
});

if (!localStorage.getItem('nilora.lang')) showOnboarding();

window.addEventListener('hashchange', render);
render();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
