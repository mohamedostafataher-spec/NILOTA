/* NILORA world data: 20 flagship countries, 61 landmark cities.
   Egypt first (full 12), then 2-3 landmark cities per country. */

const COUNTRIES = [
  { code: 'EG', flag: '🇪🇬', en: 'Egypt', ar: 'مصر', cities: [
    { id: 'cairo', en: 'Cairo', ar: 'القاهرة', glyph: '🕌', lat: 30.0444, lon: 31.2357 },
    { id: 'giza', en: 'Giza', ar: 'الجيزة', glyph: '🔺', lat: 30.0131, lon: 31.2089 },
    { id: 'alexandria', en: 'Alexandria', ar: 'الإسكندرية', glyph: '⚓', lat: 31.2001, lon: 29.9187 },
    { id: 'sharm_el_sheikh', en: 'Sharm El Sheikh', ar: 'شرم الشيخ', glyph: '🤿', lat: 27.9158, lon: 34.3299 },
    { id: 'hurghada', en: 'Hurghada', ar: 'الغردقة', glyph: '🐠', lat: 27.2579, lon: 33.8116 },
    { id: 'dahab', en: 'Dahab', ar: 'دهب', glyph: '🐪', lat: 28.5092, lon: 34.5136 },
    { id: 'marsa_alam', en: 'Marsa Alam', ar: 'مرسى علم', glyph: '🐬', lat: 25.0676, lon: 34.8790 },
    { id: 'luxor', en: 'Luxor', ar: 'الأقصر', glyph: '🏛️', lat: 25.6872, lon: 32.6396 },
    { id: 'aswan', en: 'Aswan', ar: 'أسوان', glyph: '⛵', lat: 24.0889, lon: 32.8998 },
    { id: 'el_gouna', en: 'El Gouna', ar: 'الجونة', glyph: '🌅', lat: 27.3942, lon: 33.6782 },
    { id: 'siwa', en: 'Siwa', ar: 'سيوة', glyph: '🌴', lat: 29.2032, lon: 25.5196 },
    { id: 'north_coast', en: 'North Coast', ar: 'الساحل الشمالي', glyph: '🏖️', lat: 30.9300, lon: 28.9500 },
  ]},
  { code: 'SA', flag: '🇸🇦', en: 'Saudi Arabia', ar: 'السعودية', cities: [
    { id: 'riyadh', en: 'Riyadh', ar: 'الرياض', glyph: '🏙️', lat: 24.7136, lon: 46.6753 },
    { id: 'jeddah', en: 'Jeddah', ar: 'جدة', glyph: '🌊', lat: 21.4858, lon: 39.1925 },
    { id: 'alula', en: 'AlUla', ar: 'العلا', glyph: '🏜️', lat: 26.6085, lon: 37.9232 },
  ]},
  { code: 'AE', flag: '🇦🇪', en: 'UAE', ar: 'الإمارات', cities: [
    { id: 'dubai', en: 'Dubai', ar: 'دبي', glyph: '🌆', lat: 25.2048, lon: 55.2708 },
    { id: 'abu_dhabi', en: 'Abu Dhabi', ar: 'أبوظبي', glyph: '🕌', lat: 24.4539, lon: 54.3773 },
    { id: 'sharjah', en: 'Sharjah', ar: 'الشارقة', glyph: '📚', lat: 25.3463, lon: 55.4209 },
  ]},
  { code: 'TR', flag: '🇹🇷', en: 'Turkey', ar: 'تركيا', cities: [
    { id: 'istanbul', en: 'Istanbul', ar: 'إسطنبول', glyph: '🌉', lat: 41.0082, lon: 28.9784 },
    { id: 'cappadocia', en: 'Cappadocia', ar: 'كابادوكيا', glyph: '🎈', lat: 38.6431, lon: 34.8289 },
    { id: 'antalya', en: 'Antalya', ar: 'أنطاليا', glyph: '🏖️', lat: 36.8969, lon: 30.7133 },
  ]},
  { code: 'FR', flag: '🇫🇷', en: 'France', ar: 'فرنسا', cities: [
    { id: 'paris', en: 'Paris', ar: 'باريس', glyph: '🗼', lat: 48.8566, lon: 2.3522 },
    { id: 'nice', en: 'Nice', ar: 'نيس', glyph: '🌴', lat: 43.7102, lon: 7.2620 },
    { id: 'lyon', en: 'Lyon', ar: 'ليون', glyph: '🍷', lat: 45.7640, lon: 4.8357 },
  ]},
  { code: 'IT', flag: '🇮🇹', en: 'Italy', ar: 'إيطاليا', cities: [
    { id: 'rome', en: 'Rome', ar: 'روما', glyph: '🏛️', lat: 41.9028, lon: 12.4964 },
    { id: 'venice', en: 'Venice', ar: 'البندقية', glyph: '🛶', lat: 45.4408, lon: 12.3155 },
    { id: 'milan', en: 'Milan', ar: 'ميلانو', glyph: '👗', lat: 45.4642, lon: 9.1900 },
  ]},
  { code: 'GB', flag: '🇬🇧', en: 'United Kingdom', ar: 'بريطانيا', cities: [
    { id: 'london', en: 'London', ar: 'لندن', glyph: '🎡', lat: 51.5074, lon: -0.1278 },
    { id: 'edinburgh', en: 'Edinburgh', ar: 'إدنبرة', glyph: '🏰', lat: 55.9533, lon: -3.1883 },
    { id: 'manchester', en: 'Manchester', ar: 'مانشستر', glyph: '⚽', lat: 53.4808, lon: -2.2426 },
  ]},
  { code: 'US', flag: '🇺🇸', en: 'United States', ar: 'أمريكا', cities: [
    { id: 'new_york', en: 'New York', ar: 'نيويورك', glyph: '🗽', lat: 40.7128, lon: -74.0060 },
    { id: 'los_angeles', en: 'Los Angeles', ar: 'لوس أنجلوس', glyph: '🎬', lat: 34.0522, lon: -118.2437 },
    { id: 'las_vegas', en: 'Las Vegas', ar: 'لاس فيغاس', glyph: '🎰', lat: 36.1699, lon: -115.1398 },
  ]},
  { code: 'JP', flag: '🇯🇵', en: 'Japan', ar: 'اليابان', cities: [
    { id: 'tokyo', en: 'Tokyo', ar: 'طوكيو', glyph: '🗾', lat: 35.6762, lon: 139.6503 },
    { id: 'kyoto', en: 'Kyoto', ar: 'كيوتو', glyph: '⛩️', lat: 35.0116, lon: 135.7681 },
    { id: 'osaka', en: 'Osaka', ar: 'أوساكا', glyph: '🏯', lat: 34.6937, lon: 135.5023 },
  ]},
  { code: 'BR', flag: '🇧🇷', en: 'Brazil', ar: 'البرازيل', cities: [
    { id: 'rio', en: 'Rio de Janeiro', ar: 'ريو دي جانيرو', glyph: '🏖️', lat: -22.9068, lon: -43.1729 },
    { id: 'sao_paulo', en: 'São Paulo', ar: 'ساو باولو', glyph: '🌃', lat: -23.5505, lon: -46.6333 },
  ]},
  { code: 'IN', flag: '🇮🇳', en: 'India', ar: 'الهند', cities: [
    { id: 'new_delhi', en: 'New Delhi', ar: 'نيودلهي', glyph: '🕌', lat: 28.6139, lon: 77.2090 },
    { id: 'mumbai', en: 'Mumbai', ar: 'مومباي', glyph: '🎥', lat: 19.0760, lon: 72.8777 },
    { id: 'agra', en: 'Agra', ar: 'أغرا', glyph: '🌙', lat: 27.1767, lon: 78.0081 },
  ]},
  { code: 'CN', flag: '🇨🇳', en: 'China', ar: 'الصين', cities: [
    { id: 'beijing', en: 'Beijing', ar: 'بكين', glyph: '🐉', lat: 39.9042, lon: 116.4074 },
    { id: 'shanghai', en: 'Shanghai', ar: 'شنغهاي', glyph: '🌆', lat: 31.2304, lon: 121.4737 },
    { id: 'xian', en: "Xi'an", ar: 'شيان', glyph: '🗿', lat: 34.3416, lon: 108.9398 },
  ]},
  { code: 'MA', flag: '🇲🇦', en: 'Morocco', ar: 'المغرب', cities: [
    { id: 'marrakech', en: 'Marrakech', ar: 'مراكش', glyph: '🐍', lat: 31.6295, lon: -7.9811 },
    { id: 'casablanca', en: 'Casablanca', ar: 'الدار البيضاء', glyph: '🌊', lat: 33.5731, lon: -7.5898 },
    { id: 'chefchaouen', en: 'Chefchaouen', ar: 'شفشاون', glyph: '🔵', lat: 35.1688, lon: -5.2636 },
  ]},
  { code: 'GR', flag: '🇬🇷', en: 'Greece', ar: 'اليونان', cities: [
    { id: 'athens', en: 'Athens', ar: 'أثينا', glyph: '🏛️', lat: 37.9838, lon: 23.7275 },
    { id: 'santorini', en: 'Santorini', ar: 'سانتوريني', glyph: '⛪', lat: 36.4610, lon: 25.3760 },
    { id: 'thessaloniki', en: 'Thessaloniki', ar: 'سالونيك', glyph: '🌅', lat: 40.6401, lon: 22.9444 },
  ]},
  { code: 'ES', flag: '🇪🇸', en: 'Spain', ar: 'إسبانيا', cities: [
    { id: 'madrid', en: 'Madrid', ar: 'مدريد', glyph: '👑', lat: 40.4168, lon: -3.7038 },
    { id: 'barcelona', en: 'Barcelona', ar: 'برشلونة', glyph: '🦎', lat: 41.3874, lon: 2.1686 },
    { id: 'seville', en: 'Seville', ar: 'إشبيلية', glyph: '💃', lat: 37.3891, lon: -5.9845 },
  ]},
  { code: 'DE', flag: '🇩🇪', en: 'Germany', ar: 'ألمانيا', cities: [
    { id: 'berlin', en: 'Berlin', ar: 'برلين', glyph: '🐻', lat: 52.5200, lon: 13.4050 },
    { id: 'munich', en: 'Munich', ar: 'ميونخ', glyph: '🍺', lat: 48.1351, lon: 11.5820 },
    { id: 'hamburg', en: 'Hamburg', ar: 'هامبورغ', glyph: '⚓', lat: 53.5511, lon: 9.9937 },
  ]},
  { code: 'AU', flag: '🇦🇺', en: 'Australia', ar: 'أستراليا', cities: [
    { id: 'sydney', en: 'Sydney', ar: 'سيدني', glyph: '🎭', lat: -33.8688, lon: 151.2093 },
    { id: 'melbourne', en: 'Melbourne', ar: 'ملبورن', glyph: '☕', lat: -37.8136, lon: 144.9631 },
  ]},
  { code: 'ZA', flag: '🇿🇦', en: 'South Africa', ar: 'جنوب أفريقيا', cities: [
    { id: 'cape_town', en: 'Cape Town', ar: 'كيب تاون', glyph: '⛰️', lat: -33.9249, lon: 18.4241 },
    { id: 'johannesburg', en: 'Johannesburg', ar: 'جوهانسبرغ', glyph: '🦁', lat: -26.2041, lon: 28.0473 },
  ]},
  { code: 'CA', flag: '🇨🇦', en: 'Canada', ar: 'كندا', cities: [
    { id: 'toronto', en: 'Toronto', ar: 'تورونتو', glyph: '🗼', lat: 43.6532, lon: -79.3832 },
    { id: 'vancouver', en: 'Vancouver', ar: 'فانكوفر', glyph: '🌲', lat: 49.2827, lon: -123.1207 },
    { id: 'montreal', en: 'Montreal', ar: 'مونتريال', glyph: '🍁', lat: 45.5017, lon: -73.5673 },
  ]},
  { code: 'MX', flag: '🇲🇽', en: 'Mexico', ar: 'المكسيك', cities: [
    { id: 'mexico_city', en: 'Mexico City', ar: 'مكسيكو سيتي', glyph: '🦅', lat: 19.4326, lon: -99.1332 },
    { id: 'cancun', en: 'Cancún', ar: 'كانكون', glyph: '🏝️', lat: 21.1619, lon: -86.8515 },
    { id: 'guadalajara', en: 'Guadalajara', ar: 'غوادالاخارا', glyph: '🎺', lat: 20.6597, lon: -103.3496 },
  ]},
];

// Flat lookup used by the map and passport.
const ALL_CITIES = COUNTRIES.flatMap(cn => cn.cities.map(c => ({ ...c, country: cn })));
const TOTAL_STAMPS = ALL_CITIES.length;

/* Traveler mode: currency + 5 starter words per country (local script + Arabic transliteration). */
const CURRENCY = { EG:'EGP', SA:'SAR', AE:'AED', TR:'TRY', FR:'EUR', IT:'EUR', GB:'GBP', US:'USD', JP:'JPY', BR:'BRL', IN:'INR', CN:'CNY', MA:'MAD', GR:'EUR', ES:'EUR', DE:'EUR', AU:'AUD', ZA:'ZAR', CA:'CAD', MX:'MXN' };

const PHRASES = {
  EG: [['أهلين','Ahleen','مرحبًا'],['ماشي','Mashi','حسنًا'],['يا سلام','Ya salam','جميل جدًا'],['إزيك','Ezzayyak','كيف حالك'],['تحيا مصر','Tahya Masr','فخر وطني']],
  SA: [['هلا','Hala','مرحبًا'],['مشكور','Mashkoor','شكرًا'],['عساك بخير','Asak bkhair','أتمنى لك الخير'],['يلا','Yalla','هيّا'],['تو الناس','Taww annas','حديثًا']],
  AE: [['هلا والله','Hala wallah','مرحبًا حيّاك'],['مشكورين','Mashkoorin','شكرًا للجميع'],['خذ راحتك','Khuth rahatak','ارتاح'],['على عيني','Ala einy','بكل سرور'],['دوم','Dawm','دمت']],
  TR: [['Merhaba','مرحبا','مرحبًا'],['Teşekkürler','تشكورلار','شكرًا'],['Lütfen','لوتفن','من فضلك'],['Evet','أوت','نعم'],['Hayır','هايِر','لا']],
  FR: [['Bonjour','بونجور','مرحبًا'],['Merci','ميرسي','شكرًا'],['S’il vous plaît','سيل فو بليه','من فضلك'],['Oui','وي','نعم'],['Non','نو','لا']],
  IT: [['Ciao','تشاو','مرحبًا'],['Grazie','غراتسييه','شكرًا'],['Per favore','بير فافوريه','من فضلك'],['Sì','سي','نعم'],['No','نو','لا']],
  GB: [['Hello','هالو','مرحبًا'],['Cheers','تشيرز','شكرًا'],['Please','بليز','من فضلك'],['Yes','يس','نعم'],['No','نو','لا']],
  US: [['Hey','هَي','مرحبًا'],['Thanks','ثانكس','شكرًا'],['Please','بليز','من فضلك'],['Yeah','ييه','نعم'],['Nope','نوب','لا']],
  JP: [['こんにちは','كونيتشيوا','مرحبًا'],['ありがとう','أريغاتو','شكرًا'],['おねがいします','أونيغاي شيماس','من فضلك'],['はい','هاي','نعم'],['いいえ','إييه','لا']],
  BR: [['Olá','أولا','مرحبًا'],['Obrigado','أوبريغادو','شكرًا'],['Por favor','بور فافور','من فضلك'],['Sim','سيم','نعم'],['Não','ناو','لا']],
  IN: [['नमस्ते','ناماستيه','مرحبًا'],['धन्यवाद','دهانيَفاد','شكرًا'],['कृपया','كريبَيا','من فضلك'],['हाँ','هان','نعم'],['नहीं','نهين','لا']],
  CN: [['你好','ني هاو','مرحبًا'],['谢谢','شييه شييه','شكرًا'],['请','تشينغ','من فضلك'],['是','شِ','نعم'],['不是','بو شِ','لا']],
  MA: [['السلام','Salam','مرحبًا'],['شكرًا بزاف','Shukran bezzaf','شكرًا كثيرًا'],['عافاك','Afak','من فضلك'],['إيه','Ih','نعم'],['لا','La','لا']],
  GR: [['Γειά','يا','مرحبًا'],['Ευχαριστώ','إفخاريستو','شكرًا'],['Παρακαλώ','باراكالو','من فضلك'],['Ναι','نيه','نعم'],['Όχι','أوخي','لا']],
  ES: [['Hola','أولا','مرحبًا'],['Gracias','غراسياس','شكرًا'],['Por favor','بور فافور','من فضلك'],['Sí','سي','نعم'],['No','نو','لا']],
  DE: [['Hallo','هالو','مرحبًا'],['Danke','دانكه','شكرًا'],['Bitte','بيته','من فضلك'],['Ja','يا','نعم'],['Nein','ناين','لا']],
  AU: [['G’day','غداي','مرحبًا'],['Ta','تا','شكرًا'],['Please','بليز','من فضلك'],['Yeah','ييه','نعم'],['Nah','ناه','لا']],
  ZA: [['Hello','هالو','مرحبًا'],['Thank you','ثانك يو','شكرًا'],['Please','بليز','من فضلك'],['Yes','يس','نعم'],['No','نو','لا']],
  CA: [['Hi','هاي','مرحبًا'],['Thanks','ثانكس','شكرًا'],['Please','بليز','من فضلك'],['Yeah','ييه','نعم'],['No','نو','لا']],
  MX: [['Órale','أوراليه','هيّا/رائع'],['Gracias','غراسياس','شكرًا'],['Por favor','بور فافور','من فضلك'],['Sí','سي','نعم'],['No','نو','لا']],
};
