/* NILORA 3D globe: Three.js + Natural Earth borders (world-atlas TopoJSON via unpkg).
   Real day/night from UTC time, tappable city markers, drag to spin. */

let globeState = null;

function latLonToVec3(lat, lon, r) {
  const phi = (90 - lat) * Math.PI / 180;
  const theta = (lon + 180) * Math.PI / 180;
  return new THREE.Vector3(
    -r * Math.sin(phi) * Math.cos(theta),
    r * Math.cos(phi),
    r * Math.sin(phi) * Math.sin(theta)
  );
}

// Approximate subsolar point for the current UTC time.
function sunDirection() {
  const now = new Date();
  const start = new Date(Date.UTC(now.getUTCFullYear(), 0, 0));
  const day = Math.floor((now - start) / 86400000);
  const decl = -23.44 * Math.cos((360 / 365) * (day + 10) * Math.PI / 180);
  const utcH = now.getUTCHours() + now.getUTCMinutes() / 60;
  const lon = -(utcH - 12) * 15;
  return latLonToVec3(decl, lon, 10);
}

function drawEarthTexture() {
  const W = 2048, H = 1024;
  const cv = document.createElement('canvas');
  cv.width = W; cv.height = H;
  const x = cv.getContext('2d');
  x.fillStyle = '#0B1B33';
  x.fillRect(0, 0, W, H);
  // lat/lon grid
  x.strokeStyle = 'rgba(25,182,176,0.18)';
  x.lineWidth = 1;
  for (let lat = -60; lat <= 60; lat += 30) {
    const y = (90 - lat) / 180 * H;
    x.beginPath(); x.moveTo(0, y); x.lineTo(W, y); x.stroke();
  }
  for (let lon = -150; lon <= 180; lon += 30) {
    const px = (lon + 180) / 360 * W;
    x.beginPath(); x.moveTo(px, 0); x.lineTo(px, H); x.stroke();
  }
  return { canvas: cv, ctx: x, W, H };
}

function drawBorders(x, W, H, geo) {
  x.fillStyle = '#1B2F50';
  x.strokeStyle = '#C6A15B';
  x.lineWidth = 1.2;
  const project = (lon, lat) => [(lon + 180) / 360 * W, (90 - lat) / 180 * H];
  const drawPoly = (rings) => {
    rings.forEach(ring => {
      x.beginPath();
      ring.forEach(([lon, lat], i) => {
        const [px, py] = project(lon, lat);
        if (i === 0) x.moveTo(px, py); else x.lineTo(px, py);
      });
      x.closePath();
      x.fill(); x.stroke();
    });
  };
  geo.features.forEach(f => {
    if (!f.geometry) return;
    if (f.geometry.type === 'Polygon') drawPoly(f.geometry.coordinates);
    else if (f.geometry.type === 'MultiPolygon') f.geometry.coordinates.forEach(drawPoly);
  });
}

async function initGlobe() {
  const container = document.getElementById('globe');
  if (!container || typeof THREE === 'undefined') return;
  if (globeState) { globeState.dispose(); globeState = null; }

  const width = container.clientWidth;
  const height = Math.max(320, Math.min(520, Math.round(window.innerHeight * 0.55)));
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
  camera.position.set(0, 0, 3.1);
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  container.appendChild(renderer.domElement);

  const world = new THREE.Group();
  scene.add(world);

  // earth with drawn texture
  const { canvas, ctx, W, H } = drawEarthTexture();
  const tex = new THREE.CanvasTexture(canvas);
  const sphere = new THREE.Mesh(
    new THREE.SphereGeometry(1, 48, 48),
    new THREE.MeshStandardMaterial({ map: tex, roughness: 0.9, metalness: 0.05 })
  );
  world.add(sphere);

  // atmosphere glow
  const atmo = new THREE.Mesh(
    new THREE.SphereGeometry(1.06, 32, 32),
    new THREE.MeshBasicMaterial({ color: 0x19b6b0, transparent: true, opacity: 0.08, side: THREE.BackSide })
  );
  world.add(atmo);

  // lights: sun (real position) + dim ambient
  const sun = new THREE.DirectionalLight(0xfff3d6, 1.25);
  sun.position.copy(sunDirection());
  scene.add(sun);
  scene.add(new THREE.AmbientLight(0x223355, 0.85));

  // city markers
  const markerGeo = new THREE.SphereGeometry(0.012, 8, 8);
  const markerMat = new THREE.MeshBasicMaterial({ color: 0xC6A15B });
  const earnedMat = new THREE.MeshBasicMaterial({ color: 0x19B6B0 });
  const ids = earnedIds();
  const markers = [];
  ALL_CITIES.forEach(city => {
    const m = new THREE.Mesh(markerGeo, ids.includes(city.id) ? earnedMat : markerMat);
    m.position.copy(latLonToVec3(city.lat, city.lon, 1.012));
    m.userData.city = city;
    world.add(m);
    markers.push(m);
  });

  // fetch + draw country borders
  try {
    const r = await fetch('https://unpkg.com/world-atlas@2/countries-110m.json');
    const topo = await r.json();
    const geo = topojson.feature(topo, topo.objects.countries);
    drawBorders(ctx, W, H, geo);
    tex.needsUpdate = true;
  } catch (e) { /* grid-only globe still works */ }

  // interaction: drag to spin, tap a marker
  const ray = new THREE.Raycaster();
  ray.params.Points = { threshold: 0.05 };
  const pointer = new THREE.Vector2();
  let dragging = false, moved = 0, lastX = 0, lastY = 0;
  let velY = 0.0016, targetRX = 0.35;
  world.rotation.x = targetRX;

  const el = renderer.domElement;
  el.style.touchAction = 'pan-y';
  el.addEventListener('pointerdown', (e) => { dragging = true; moved = 0; lastX = e.clientX; lastY = e.clientY; });
  window.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    const dx = e.clientX - lastX, dy = e.clientY - lastY;
    moved += Math.abs(dx) + Math.abs(dy);
    world.rotation.y += dx * 0.005;
    targetRX = Math.max(-1.1, Math.min(1.1, targetRX + dy * 0.003));
    lastX = e.clientX; lastY = e.clientY;
  });
  window.addEventListener('pointerup', (e) => {
    if (!dragging) return;
    dragging = false;
    if (moved > 8) return;
    const rect = el.getBoundingClientRect();
    pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
    ray.setFromCamera(pointer, camera);
    const hits = ray.intersectObjects(markers);
    if (hits.length) showGlobeCity(hits[0].object.userData.city);
  });

  function showGlobeCity(city) {
    const card = document.getElementById('globeCityCard');
    if (!card) return;
    card.style.display = 'block';
    card.innerHTML = `<b>${city.country.flag} ${city[state.lang]}</b><br><span class="live-loading">${t().loading}</span>`;
    fetch('https://api.open-meteo.com/v1/forecast?latitude=' + city.lat + '&longitude=' + city.lon +
      '&current=temperature_2m,weather_code,wind_speed_10m&timezone=auto')
      .then(r => r.json())
      .then(j => {
        completeMission('weather');
        card.innerHTML = `<b>${city.country.flag} ${city[state.lang]}</b><br>` +
          `${t().weatherNow}: ${weatherLabel(j.current.weather_code)} · ${Math.round(j.current.temperature_2m)}°C · 💨 ${Math.round(j.current.wind_speed_10m)} km/h`;
      })
      .catch(() => { card.innerHTML = `<b>${city.country.flag} ${city[state.lang]}</b>`; });
  }

  let raf = 0;
  function animate() {
    raf = requestAnimationFrame(animate);
    if (!dragging) world.rotation.y += velY;
    world.rotation.x += (targetRX - world.rotation.x) * 0.08;
    sun.position.copy(sunDirection());
    renderer.render(scene, camera);
  }
  animate();

  globeState = {
    dispose() {
      cancelAnimationFrame(raf);
      renderer.dispose();
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement);
      globeState = null;
    },
  };
}
