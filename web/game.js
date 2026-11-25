/* NILORA City — first playable 3D prototype.
   Sections: Save (IndexedDB) · Input (keyboard + touch joystick) · World · Player ·
   FollowCamera · Discoveries (mission) · HUD · Game orchestrator.
   Uses the THREE r128 global already loaded by index.html. No keys, no accounts. */
(function () {
  'use strict';

  /* ---------- Save: IndexedDB (position, discoveries, mission) ---------- */
  const Save = {
    db: null,
    open() {
      return new Promise((resolve) => {
        try {
          const req = indexedDB.open('nilora-city', 1);
          req.onupgradeneeded = () => req.result.createObjectStore('state');
          req.onsuccess = () => { Save.db = req.result; resolve(true); };
          req.onerror = () => resolve(false);
        } catch (e) { resolve(false); }
      });
    },
    get(key) {
      return new Promise((resolve) => {
        if (!Save.db) return resolve(null);
        try {
          const tx = Save.db.transaction('state').objectStore('state').get(key);
          tx.onsuccess = () => resolve(tx.result || null);
          tx.onerror = () => resolve(null);
        } catch (e) { resolve(null); }
      });
    },
    set(key, val) {
      if (!Save.db) return;
      try { Save.db.transaction('state', 'readwrite').objectStore('state').put(val, key); } catch (e) {}
    },
  };

  /* ---------- Input: WASD/arrows + shift run + space jump + touch joystick ---------- */
  const Input = {
    keys: {}, joy: { active: false, dx: 0, dy: 0, id: null }, jumpQueued: false,
    init(root) {
      window.addEventListener('keydown', (e) => {
        if (!Game.running) return;
        Input.keys[e.code] = true;
        if (e.code === 'Space') { Input.jumpQueued = true; e.preventDefault(); }
        if (e.code === 'KeyE') Drive.toggle();
      });
      window.addEventListener('keyup', (e) => { Input.keys[e.code] = false; });

      const stick = root.querySelector('#gjoy'), knob = root.querySelector('#gjoyKnob');
      const jumpBtn = root.querySelector('#gjump');
      if (stick) {
        const R = 52;
        const setKnob = (dx, dy) => { knob.style.transform = 'translate(' + dx + 'px,' + dy + 'px)'; };
        const handle = (t) => {
          const r = stick.getBoundingClientRect();
          let dx = t.clientX - (r.left + r.width / 2), dy = t.clientY - (r.top + r.height / 2);
          const len = Math.hypot(dx, dy) || 1, cl = Math.min(len, R);
          dx = dx / len * cl; dy = dy / len * cl;
          Input.joy.dx = dx / R; Input.joy.dy = dy / R;
          setKnob(dx, dy);
        };
        stick.addEventListener('pointerdown', (e) => {
          Input.joy.active = true; Input.joy.id = e.pointerId;
          stick.setPointerCapture(e.pointerId); handle(e);
        });
        stick.addEventListener('pointermove', (e) => { if (Input.joy.active && e.pointerId === Input.joy.id) handle(e); });
        const end = (e) => {
          if (e.pointerId !== Input.joy.id) return;
          Input.joy.active = false; Input.joy.dx = 0; Input.joy.dy = 0; setKnob(0, 0);
        };
        stick.addEventListener('pointerup', end);
        stick.addEventListener('pointercancel', end);
      }
      if (jumpBtn) jumpBtn.addEventListener('pointerdown', (e) => { Input.jumpQueued = true; e.preventDefault(); });
    },
    move() { // returns {x, z, run} in camera-relative intent space (x: strafe, z: forward)
      let x = 0, z = 0;
      if (Input.keys.KeyW || Input.keys.ArrowUp) z += 1;
      if (Input.keys.KeyS || Input.keys.ArrowDown) z -= 1;
      if (Input.keys.KeyA || Input.keys.ArrowLeft) x -= 1;
      if (Input.keys.KeyD || Input.keys.ArrowRight) x += 1;
      x += Input.joy.dx; z -= Input.joy.dy;
      const len = Math.hypot(x, z);
      if (len > 1) { x /= len; z /= len; }
      const run = !!(Input.keys.ShiftLeft || Input.keys.ShiftRight) || Math.hypot(Input.joy.dx, Input.joy.dy) > 0.92;
      return { x, z, run };
    },
  };


  /* ---------- Procedural textures (canvas-generated, free, zero-download) ---------- */
  const Tex = {
    make(w, h, fn) {
      const c = document.createElement('canvas'); c.width = w; c.height = h;
      fn(c.getContext('2d'), w, h);
      const t = new THREE.CanvasTexture(c);
      t.wrapS = t.wrapT = THREE.RepeatWrapping;
      t.anisotropy = 4;
      t.encoding = THREE.sRGBEncoding;
      return t;
    },
    noise(ctx, w, h, n, alpha) {
      for (let i = 0; i < n; i++) {
        const v = Math.floor(Math.random() * 40);
        ctx.fillStyle = 'rgba(' + (v + 10) + ',' + (v + 12) + ',' + (v + 18) + ',' + alpha + ')';
        ctx.fillRect(Math.random() * w, Math.random() * h, 2, 2);
      }
    },
    asphalt() {
      return Tex.make(64, 256, (ctx, w, h) => {
        ctx.fillStyle = '#232d3f'; ctx.fillRect(0, 0, w, h);
        Tex.noise(ctx, w, h, 500, 0.25);
        ctx.fillStyle = '#c8b568';
        ctx.fillRect(w / 2 - 2, 20, 4, 60);
        ctx.fillRect(w / 2 - 2, 140, 4, 60);
        ctx.fillStyle = 'rgba(200,181,104,0.85)';
        ctx.fillRect(2, 0, 2, h); ctx.fillRect(w - 4, 0, 2, h);
      });
    },
    asphaltH() {
      return Tex.make(256, 64, (ctx, w, h) => {
        ctx.fillStyle = '#131a28'; ctx.fillRect(0, 0, w, h);
        Tex.noise(ctx, w, h, 500, 0.18);
        ctx.fillStyle = 'rgba(154,134,74,0.8)';
        ctx.fillRect(24, h / 2 - 1, 44, 3);
        ctx.fillRect(148, h / 2 - 1, 44, 3);
      });
    },
    pavement() {
      return Tex.make(128, 128, (ctx, w, h) => {
        ctx.fillStyle = '#1c2942'; ctx.fillRect(0, 0, w, h);
        Tex.noise(ctx, w, h, 350, 0.18);
        ctx.strokeStyle = 'rgba(10,16,28,0.55)'; ctx.lineWidth = 2;
        for (let i = 0; i <= 2; i++) {
          ctx.beginPath(); ctx.moveTo(0, i * h / 2); ctx.lineTo(w, i * h / 2); ctx.stroke();
          ctx.beginPath(); ctx.moveTo(i * w / 2, 0); ctx.lineTo(i * w / 2, h); ctx.stroke();
        }
      });
    },
    facade(base, rows, cols) {
      const grid = [];
      for (let r = 0; r < rows; r++) { grid.push([]); for (let c = 0; c < cols; c++) grid[r].push(Math.random() < 0.28); }
      const map = Tex.make(256, 256, (ctx, w, h) => {
        ctx.fillStyle = base; ctx.fillRect(0, 0, w, h);
        Tex.noise(ctx, w, h, 300, 0.10);
        const cw = w / cols, ch = h / rows;
        for (let r = 0; r < rows; r++) {
          for (let c = 0; c < cols; c++) {
            ctx.fillStyle = grid[r][c] ? '#ffca7a' : '#0d1524';
            ctx.fillRect(c * cw + cw * 0.22, r * ch + ch * 0.22, cw * 0.56, ch * 0.5);
          }
        }
      });
      const glow = Tex.make(256, 256, (ctx, w, h) => {
        ctx.fillStyle = '#000000'; ctx.fillRect(0, 0, w, h);
        const cw = w / cols, ch = h / rows;
        for (let r = 0; r < rows; r++) {
          for (let c = 0; c < cols; c++) {
            if (!grid[r][c]) continue;
            ctx.fillStyle = '#ffca7a';
            ctx.fillRect(c * cw + cw * 0.22, r * ch + ch * 0.22, cw * 0.56, ch * 0.5);
          }
        }
      });
      return { map, glow };
    },
    sky() {
      const t = Tex.make(16, 512, (ctx, w, h) => {
        const gr = ctx.createLinearGradient(0, 0, 0, h);
        gr.addColorStop(0.0, '#060b16');
        gr.addColorStop(0.45, '#0e1a30');
        gr.addColorStop(0.72, '#3a3050');
        gr.addColorStop(0.86, '#8a5a34');
        gr.addColorStop(1.0, '#d4af37');
        ctx.fillStyle = gr; ctx.fillRect(0, 0, w, h);
      });
      t.wrapS = t.wrapT = THREE.ClampToEdgeWrapping;
      return t;
    },
  };

  /* ---------- Photo textures: Poly Haven (CC0, verified) loaded over procedural fallback ---------- */
  const PhotoTex = {
    ASPHALT: 'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/1k/aerial_asphalt_01/aerial_asphalt_01_diff_1k.jpg',
    PAVING: 'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/1k/cobblestone_01/cobblestone_01_diff_1k.jpg',
    apply(materials, url, rx, ry, tint) {
      new THREE.TextureLoader().setCrossOrigin('anonymous').load(url, (t) => {
        t.wrapS = t.wrapT = THREE.RepeatWrapping;
        t.repeat.set(rx, ry);
        t.anisotropy = 4;
        t.encoding = THREE.sRGBEncoding;
        materials.forEach((m) => {
          m.map = t;
          if (tint) m.color = new THREE.Color(tint);
          m.needsUpdate = true;
        });
      }, undefined, () => { /* keep procedural fallback */ });
    },
  };


  /* ---------- Cairo: real OSM street network (ODbL, baked in cairo-roads.js) ---------- */
  const Cairo = {
    paths: [],
    build() {
      Cairo.paths = [];
      const data = window.CAIRO_ROADS;
      if (!data || !data.ways) return false;
      const cLat = data.center[0], cLon = data.center[1];
      const kx = 111320 * Math.cos(cLat * Math.PI / 180), kz = 110540;
      const raw = data.ways.map((w) => ({
        r: w.r, n: w.n, e: w.e,
        pts: w.g.map((p) => [(p[1] - cLon) * kx, -(p[0] - cLat) * kz]),
      }));
      let maxE = 1;
      raw.forEach((w) => w.pts.forEach((p) => { maxE = Math.max(maxE, Math.abs(p[0]), Math.abs(p[1])); }));
      const s = 185 / maxE;
      raw.forEach((w) => {
        const pts = w.pts.map((p) => [p[0] * s, p[1] * s]);
        const cum = [0];
        for (let i = 1; i < pts.length; i++) {
          cum.push(cum[i - 1] + Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]));
        }
        Cairo.paths.push({ r: w.r, n: w.n, e: w.e, pts, cum, len: cum[cum.length - 1], width: [4, 6, 8, 10][w.r] });
      });
      Cairo.paths = Cairo.paths.filter((p) => p.len > 6);
      return Cairo.paths.length > 10;
    },
    at(path, s) {
      const c = path.cum, pts = path.pts;
      let i = 1;
      while (i < c.length - 1 && c[i] < s) i++;
      const t = c[i] === c[i - 1] ? 0 : (s - c[i - 1]) / (c[i] - c[i - 1]);
      const x = pts[i - 1][0] + (pts[i][0] - pts[i - 1][0]) * t;
      const z = pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * t;
      const dx = pts[i][0] - pts[i - 1][0], dz = pts[i][1] - pts[i - 1][1];
      const l = Math.hypot(dx, dz) || 1;
      return { x, z, dx: dx / l, dz: dz / l };
    },
  };

  /* ---------- World: ground, streets, low-poly buildings, landmarks ---------- */
  const CITY = { size: 400, block: 34, road: 12 };
  const World = {
    group: null, colliders: [], landmarks: [],
    build(scene) {
      const g = new THREE.Group();
      // ground
      const paveTex = Tex.pavement(); paveTex.repeat.set(CITY.size / 10, CITY.size / 10);
      const ground = new THREE.Mesh(
        new THREE.PlaneGeometry(CITY.size, CITY.size),
        new THREE.MeshLambertMaterial({ map: paveTex }));
      ground.rotation.x = -Math.PI / 2;
      ground.receiveShadow = true;
      g.add(ground);
      // facade materials shared by both layouts
      const palette = ['#8f6b2f', '#31435e', '#4a5f7d', '#6b5330', '#22334d', '#9a7a3d'];
      const facades = palette.map((col) => {
        const f = Tex.facade(col, 6, 4);
        const side = new THREE.MeshLambertMaterial({ map: f.map, emissive: 0xffb763, emissiveIntensity: 0.55, emissiveMap: f.glow });
        const top = new THREE.MeshLambertMaterial({ color: new THREE.Color(col).multiplyScalar(0.55) });
        return [side, side, top, top, side, side];
      });
      let seed = 42;
      const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
      const overlaps = (minX, maxX, minZ, maxZ) => World.colliders.some((c) => minX < c.maxX && maxX > c.minX && minZ < c.maxZ && maxZ > c.minZ);
      const addBuilding = (x, z, w, d, h) => {
        const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), facades[Math.floor(rnd() * facades.length)]);
        m.position.set(x, h / 2, z);
        m.castShadow = true; m.receiveShadow = true;
        g.add(m);
        World.colliders.push({ minX: x - w / 2 - 0.6, maxX: x + w / 2 + 0.6, minZ: z - d / 2 - 0.6, maxZ: z + d / 2 + 0.6, h: h });
      };

      const cairoOK = Cairo.build();
      if (cairoOK) {
        // real Cairo streets: textured ribbons + junction discs, buildings along them
        const asp = Tex.asphaltH(); asp.repeat.set(1, 1);
        const roadMat = new THREE.MeshLambertMaterial({ map: asp });
        Cairo.paths.forEach((p) => {
          for (let i = 1; i < p.pts.length; i++) {
            const [x0, z0] = p.pts[i - 1], [x1, z1] = p.pts[i];
            const len = Math.hypot(x1 - x0, z1 - z0);
            if (len < 0.5) continue;
            const geo = new THREE.PlaneGeometry(len + p.width * 0.6, p.width);
            const uv = geo.attributes.uv;
            for (let u = 0; u < uv.count; u++) uv.setX(u, uv.getX(u) * (len + p.width * 0.6) / 8);
            geo.rotateX(-Math.PI / 2);
            const m = new THREE.Mesh(geo, roadMat);
            m.position.set((x0 + x1) / 2, 0.02, (z0 + z1) / 2);
            m.rotation.y = -Math.atan2(z1 - z0, x1 - x0);
            m.receiveShadow = true;
            g.add(m);
          }
        });
        // road clearance: distance-to-segment, no building may touch any street
        const segs = [];
        Cairo.paths.forEach((p) => {
          for (let i = 1; i < p.pts.length; i++) {
            segs.push({ x0: p.pts[i - 1][0], z0: p.pts[i - 1][1], x1: p.pts[i][0], z1: p.pts[i][1], hw: p.width / 2 });
          }
        });
        const roadDist = (x, z) => {
          let best = 1e9;
          for (const s of segs) {
            const dx = s.x1 - s.x0, dz = s.z1 - s.z0;
            const l2 = dx * dx + dz * dz;
            let t = l2 ? ((x - s.x0) * dx + (z - s.z0) * dz) / l2 : 0;
            t = Math.max(0, Math.min(1, t));
            const px = s.x0 + dx * t, pz = s.z0 + dz * t;
            const d = Math.hypot(x - px, z - pz) - s.hw;
            if (d < best) best = d;
          }
          return best;
        };
        // buildings lining the streets
        let placed = 0;
        Cairo.paths.forEach((p) => {
          for (let s = 10; s < p.len - 6 && placed < 240; s += 17 + rnd() * 11) {
            for (const side of [-1, 1]) {
              if (rnd() < 0.5 || placed >= 240) continue;
              const at = Cairo.at(p, s);
              const w = 6 + rnd() * 9, d = 6 + rnd() * 9, h = 7 + rnd() * 24;
              const half = Math.max(w, d) / 2;
              const off = p.width / 2 + 2 + half + rnd() * 6;
              const x = at.x + -at.dz * off * side;
              const z = at.z + at.dx * off * side;
              if (Math.abs(x) > 190 || Math.abs(z) > 190) continue;
              const minX = x - w / 2 - 0.6, maxX = x + w / 2 + 0.6, minZ = z - d / 2 - 0.6, maxZ = z + d / 2 + 0.6;
              if (overlaps(minX, maxX, minZ, maxZ) || roadDist(x, z) < half + 1.2) continue;
              const bp = Buildings.place(w, d);
              if (bp) {
                bp.mesh.position.x += x; bp.mesh.position.z += z;
                if (rnd() > 0.5) { bp.mesh.rotation.y = Math.PI; }
                g.add(bp.mesh);
                World.colliders.push({ minX: x - bp.w / 2 - 0.6, maxX: x + bp.w / 2 + 0.6, minZ: z - bp.d / 2 - 0.6, maxZ: z + bp.d / 2 + 0.6, h: bp.h });
              } else {
                addBuilding(x, z, w, d, h);
              }
              placed++;
            }
          }
        });
        // landmarks on major roads
        const majors = Cairo.paths.filter((p) => p.r >= 2 && p.len > 30).sort((a, b) => b.len - a.len);
        const picks = majors.slice(0, 5);
        picks.forEach((p, i) => {
          const at = Cairo.at(p, p.len / 2);
          const nm = p.e || p.n;
          const name = { en: nm ? String(nm).slice(0, 40) : 'Cairo Street ' + (i + 1), ar: p.n ? String(p.n).slice(0, 40) : 'شارع القاهرة ' + (i + 1) };
          const ob = new THREE.Mesh(
            new THREE.ConeGeometry(1.6, 7, 6),
            new THREE.MeshLambertMaterial({ color: 0xd4af37, emissive: 0x6b5410 }));
          ob.position.set(at.x, 3.5, at.z);
          ob.castShadow = true;
          g.add(ob);
          const ring = new THREE.Mesh(
            new THREE.RingGeometry(2.4, 3.2, 24),
            new THREE.MeshBasicMaterial({ color: 0xd4af37, side: THREE.DoubleSide, transparent: true, opacity: 0.7 }));
          ring.rotation.x = -Math.PI / 2; ring.position.set(at.x, 0.05, at.z);
          g.add(ring);
          World.landmarks.push({ id: 'lm' + i, name, x: at.x, z: at.z, mesh: ob, ring, found: false });
        });
      } else {
        // fallback: procedural grid city
        const aspX = Tex.asphalt(); aspX.repeat.set(1, CITY.size / 24);
        const aspZ = Tex.asphalt(); aspZ.rotation = Math.PI / 2; aspZ.center.set(0.5, 0.5); aspZ.repeat.set(1, CITY.size / 24);
        const roadMatX = new THREE.MeshLambertMaterial({ map: aspX });
        const roadMatZ = new THREE.MeshLambertMaterial({ map: aspZ });
        const half = CITY.size / 2, step = CITY.block + CITY.road;
        for (let p = -half + step / 2; p < half; p += step) {
          const rx = new THREE.Mesh(new THREE.PlaneGeometry(CITY.road, CITY.size), roadMatX);
          rx.rotation.x = -Math.PI / 2; rx.position.set(p, 0.02, 0); rx.receiveShadow = true; g.add(rx);
          const rz = new THREE.Mesh(new THREE.PlaneGeometry(CITY.size, CITY.road), roadMatZ);
          rz.rotation.x = -Math.PI / 2; rz.position.set(0, 0.02, p); rz.receiveShadow = true; g.add(rz);
        }
        const lm = World.landmarkCells();
        for (let bx = -half + step / 2; bx < half; bx += step) {
          for (let bz = -half + step / 2; bz < half; bz += step) {
            const key = Math.round(bx) + ',' + Math.round(bz);
            if (lm[key]) continue;
            const n = 2 + Math.floor(rnd() * 3);
            for (let i = 0; i < n; i++) {
              const w = 6 + rnd() * 9, d = 6 + rnd() * 9, h = 6 + rnd() * 26;
              const x = bx + (rnd() - 0.5) * (CITY.block - w - 2);
              const z = bz + (rnd() - 0.5) * (CITY.block - d - 2);
              addBuilding(x, z, w, d, h);
            }
          }
        }
        const NAMES = [
          { en: 'Cairo Tower', ar: 'برج القاهرة' }, { en: 'Khan el-Khalili Gate', ar: 'بوابة خان الخليلي' },
          { en: 'Nile Corniche', ar: 'كورنيش النيل' }, { en: 'Citadel View', ar: 'إطلالة القلعة' },
          { en: 'Pyramids Road', ar: 'طريق الأهرام' }];
        NAMES.forEach((name, i) => {
          const cell = World.lmCells[i];
          const ob = new THREE.Mesh(
            new THREE.ConeGeometry(1.6, 7, 6),
            new THREE.MeshLambertMaterial({ color: 0xd4af37, emissive: 0x6b5410 }));
          ob.position.set(cell.x, 3.5, cell.z);
          ob.castShadow = true;
          g.add(ob);
          const ring = new THREE.Mesh(
            new THREE.RingGeometry(2.4, 3.2, 24),
            new THREE.MeshBasicMaterial({ color: 0xd4af37, side: THREE.DoubleSide, transparent: true, opacity: 0.7 }));
          ring.rotation.x = -Math.PI / 2; ring.position.set(cell.x, 0.05, cell.z);
          g.add(ring);
          World.landmarks.push({ id: 'lm' + i, name, x: cell.x, z: cell.z, mesh: ob, ring, found: false });
        });
      }
      scene.add(g);
      World.group = g;
      PhotoTex.apply([ground.material], PhotoTex.PAVING, CITY.size / 22, CITY.size / 22, 0x6b7790);
    },
    lmCells: [{ x: -69, z: -69 }, { x: 69, z: -69 }, { x: 0, z: 0 }, { x: -69, z: 69 }, { x: 69, z: 69 }],
    landmarkCells() {
      const map = {};
      World.lmCells.forEach((c) => { map[Math.round(c.x) + ',' + Math.round(c.z)] = true; });
      return map;
    },
    hit(x, z) {
      for (const c of World.colliders) {
        if (x > c.minX && x < c.maxX && z > c.minZ && z < c.maxZ) return true;
      }
      return false;
    },
  };

  /* ---------- Assets: rigged human characters (KayKit Adventurers, CC0) ---------- */
  const Assets = {
    lib: {}, // key -> {scene, animations}
    CDN: 'https://cdn.jsdelivr.net/gh/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0/addons/kaykit_character_pack_adventures/Characters/gltf/',
    FILES: { player: 'Rogue_Hooded.glb', ped_a: 'Knight.glb', ped_b: 'Mage.glb' },
    load(key) {
      return new Promise((resolve) => {
        const urls = ['./game/assets/' + key + '.glb', Assets.CDN + Assets.FILES[key], Assets.CDN + Assets.FILES[key]];
        const tryNext = (i) => {
          if (i >= urls.length) return resolve(false);
          try {
            new THREE.GLTFLoader().load(urls[i],
              (g) => { Assets.lib[key] = g; resolve(true); },
              undefined,
              () => setTimeout(() => tryNext(i + 1), 800));
          } catch (e) { tryNext(i + 1); }
        };
        tryNext(0);
      });
    },
    async loadAll() {
      const out = [];
      for (const k of ['player', 'ped_a', 'ped_b']) out.push(await Assets.load(k));
      return out;
    },
    clone(key) {
      const src = Assets.lib[key];
      return src ? THREE.SkeletonUtils.clone(src.scene) : null;
    },
    anims(key) { return Assets.lib[key] ? Assets.lib[key].animations : []; },
  };


  /* ---------- Real CC0 building models (Quaternius packs, CC0-1.0, via jsDelivr) ---------- */
  const Buildings = {
    lib: [],
    CDN: 'https://cdn.jsdelivr.net/gh/trebeljahr/quaternius-showcase@main/public/glb/',
    FILES: [
      'buildings_pack_2/Building2_Large.glb',
      'buildings_pack_2/Building2_Small.glb',
      'buildings_pack_2/Building3_Small.glb',
      'buildings_pack_2/Building4.glb',
      'buildings_pack_3/2Story_GableRoof_Mat.glb',
      'buildings_pack_3/2Story_Balcony_Mat.glb',
      'buildings_pack_3/2Story_Center_Mat.glb',
      'buildings_pack_3/3Story_Balcony_Mat.glb',
      'buildings_pack_3/2Story_Wide_Mat.glb',
      'buildings_pack_3/1Story_Sign_Mat.glb',
    ],
    loadOne(url) {
      return new Promise((resolve) => {
        try {
          new THREE.GLTFLoader().load(url, (g) => resolve(g.scene), undefined, () => resolve(null));
        } catch (e) { resolve(null); }
      });
    },
    async loadAll() {
      const jobs = Buildings.FILES.map((f) => Buildings.loadOne(Buildings.CDN + f));
      const out = await Promise.all(jobs);
      Buildings.lib = out.filter(Boolean).map((scene) => {
        scene.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } });
        const box = new THREE.Box3().setFromObject(scene);
        const size = new THREE.Vector3(); box.getSize(size);
        return { scene, size };
      });
      return Buildings.lib.length;
    },
    place(targetW, targetD) {
      if (!Buildings.lib.length) return null;
      const b = Buildings.lib[Math.floor(Math.random() * Buildings.lib.length)];
      const mesh = b.scene.clone(true);
      const s = Math.min(targetW / Math.max(0.1, b.size.x), targetD / Math.max(0.1, b.size.z));
      mesh.scale.setScalar(s);
      const box = new THREE.Box3().setFromObject(mesh);
      mesh.position.y = -box.min.y;
      return { mesh, w: b.size.x * s, d: b.size.z * s, h: b.size.y * s };
    },
  };


  /* ---------- Street props: lights, palms, signs (Quaternius CC0, jsDelivr) ---------- */
  const Props = {
    lib: {},
    CDN: 'https://cdn.jsdelivr.net/gh/trebeljahr/quaternius-showcase@main/public/glb/',
    FILES: {
      palm1: 'nature_pack/PalmTree_1.glb',
      palm2: 'nature_pack/PalmTree_2.glb',
      palm3: 'nature_pack/PalmTree_3.glb',
      palm4: 'nature_pack/PalmTree_4.glb',
      lamp1: 'street_pack/Streetlight_Single.glb',
      lamp2: 'street_pack/Streetlight_Double.glb',
      traffic: 'street_pack/TrafficLight.glb',
      stopsign: 'street_pack/Sign_Stop.glb',
    },
    loadOne(url) {
      return new Promise((resolve) => {
        try {
          new THREE.GLTFLoader().load(url, (g) => resolve(g.scene), undefined, () => resolve(null));
        } catch (e) { resolve(null); }
      });
    },
    async loadAll() {
      const keys = Object.keys(Props.FILES);
      const out = await Promise.all(keys.map((k) => Props.loadOne(Props.CDN + Props.FILES[k])));
      keys.forEach((k, i) => {
        if (!out[i]) return;
        out[i].traverse((o) => { if (o.isMesh) { o.castShadow = true; } });
        const box = new THREE.Box3().setFromObject(out[i]);
        const size = new THREE.Vector3(); box.getSize(size);
        Props.lib[k] = { scene: out[i], size };
      });
      return Object.keys(Props.lib).length;
    },
    place(key, targetH) {
      const b = Props.lib[key];
      if (!b) return null;
      const mesh = b.scene.clone(true);
      const s = targetH / Math.max(0.1, b.size.y);
      mesh.scale.setScalar(s);
      const box = new THREE.Box3().setFromObject(mesh);
      mesh.position.y = -box.min.y;
      mesh.position.x = -(box.min.x + box.max.x) / 2;
      mesh.position.z = -(box.min.z + box.max.z) / 2;
      const wrap = new THREE.Group();
      wrap.add(mesh);
      return wrap;
    },
    roadClear(x, z, margin) {
      // true when x,z is at least margin away from every road's asphalt edge
      for (const p of Cairo.paths) {
        const hw = p.width / 2;
        for (let i = 1; i < p.pts.length; i++) {
          const x0 = p.pts[i - 1][0], z0 = p.pts[i - 1][1], x1 = p.pts[i][0], z1 = p.pts[i][1];
          const dx = x1 - x0, dz = z1 - z0;
          const L2 = dx * dx + dz * dz;
          let t = L2 ? ((x - x0) * dx + (z - z0) * dz) / L2 : 0;
          t = Math.max(0, Math.min(1, t));
          const d = Math.hypot(x - (x0 + dx * t), z - (z0 + dz * t));
          if (d - hw < margin) return false;
        }
      }
      return true;
    },
    blocked(x, z, pad) {
      for (const c of World.colliders) {
        if (x > c.minX - pad && x < c.maxX + pad && z > c.minZ - pad && z < c.maxZ + pad) return true;
      }
      return false;
    },
    pole(x, z, half) {
      World.colliders.push({ minX: x - half, maxX: x + half, minZ: z - half, maxZ: z + half, h: 4 });
    },
    stats: { placed: 0, lamps: 0, palms: 0, signs: 0, samples: [] },
    build(scene) {
      if (!Cairo.paths.length) return 0;
      const g = new THREE.Group();
      let placed = 0, lamps = 0, palms = 0, signs = 0;
      const jitter = (a) => (Math.random() - 0.5) * a;
      // streetlights + palms along every road
      Cairo.paths.forEach((p) => {
        const off = p.width / 2 + 1.0;
        for (let s = 8; s < p.len - 6; s += 22) {
          if (lamps >= 100) break;
          const side = (Math.floor(s / 22) % 2) ? 1 : -1;
          const at = Cairo.at(p, s);
          const x = at.x - at.dz * off * side, z = at.z + at.dx * off * side;
          if (Props.blocked(x, z, 0.8) || !Props.roadClear(x, z, 0.35)) continue;
          const key = (p.r >= 2 && Math.random() > 0.5) ? 'lamp2' : 'lamp1';
          const lamp = Props.place(key, 5.2);
          if (!lamp) break;
          lamp.position.x += x; lamp.position.z += z;
          lamp.rotation.y = Math.atan2(at.dx, at.dz) + (side > 0 ? Math.PI : 0);
          g.add(lamp); Props.pole(x, z, 0.3); lamps++; placed++;
          if (Props.stats.samples.length < 6) Props.stats.samples.push([Math.round(x), Math.round(z), key]);
        }
        for (let s = 16; s < p.len - 6; s += 34) {
          if (palms >= 110) break;
          const side = (Math.floor(s / 34) % 2) ? -1 : 1;
          const at = Cairo.at(p, s + jitter(6));
          const x = at.x - at.dz * (off + 1.6) * side + jitter(1), z = at.z + at.dx * (off + 1.6) * side + jitter(1);
          if (Props.blocked(x, z, 0.8) || !Props.roadClear(x, z, 0.35)) continue;
          const palm = Props.place('palm' + (1 + Math.floor(Math.random() * 4)), 5.5 + Math.random() * 2.5);
          if (!palm) break;
          palm.position.x += x; palm.position.z += z;
          palm.rotation.y = Math.random() * Math.PI * 2;
          g.add(palm); Props.pole(x, z, 0.35); palms++; placed++;
        }
      });
      // intersections: endpoints shared by >=2 paths get a traffic light or stop sign
      const ends = [];
      Cairo.paths.forEach((p) => {
        ends.push({ x: p.pts[0][0], z: p.pts[0][1], p });
        ends.push({ x: p.pts[p.pts.length - 1][0], z: p.pts[p.pts.length - 1][1], p });
      });
      const used = [];
      for (let i = 0; i < ends.length && signs < 22; i++) {
        const e = ends[i];
        if (used.some((u) => Math.hypot(u.x - e.x, u.z - e.z) < 6)) continue;
        const near = ends.filter((o) => Math.hypot(o.x - e.x, o.z - e.z) < 4);
        if (near.length < 2) continue;
        // search outward from the junction for a corner spot off the asphalt
        let x = 0, z = 0, ok = false;
        for (let r = 2; r <= 7 && !ok; r += 1) {
          for (let a = 0; a < 8 && !ok; a++) {
            const ang = a * Math.PI / 4 + jitter(0.4);
            const cx = e.x + Math.cos(ang) * r, cz = e.z + Math.sin(ang) * r;
            if (!Props.blocked(cx, cz, 1.0) && Props.roadClear(cx, cz, 0.3)) { x = cx; z = cz; ok = true; }
          }
        }
        if (!ok) continue;
        const key = Math.random() > 0.45 ? 'traffic' : 'stopsign';
        const sign = Props.place(key, key === 'traffic' ? 4.4 : 2.7);
        if (!sign) break;
        sign.position.x += x; sign.position.z += z;
        sign.rotation.y = Math.random() * Math.PI * 2;
        g.add(sign); Props.pole(x, z, 0.3); signs++; placed++;
        used.push(e);
      }
      scene.add(g);
      Props.stats.placed = placed; Props.stats.lamps = lamps; Props.stats.palms = palms; Props.stats.signs = signs;
      return placed;
    },
  };


  /* ---------- Real CC0 car models (Kenney Car Kit 3.1, CC0, via jsDelivr) ---------- */
  const CarLib = {
    lib: {},
    CDN: 'https://cdn.jsdelivr.net/gh/Arslan12216775/kenney_car-kit@HEAD/Models/GLB%20format/',
    FILES: ['taxi', 'sedan', 'suv', 'van', 'delivery', 'truck', 'police', 'ambulance'],
    loadOne(name) {
      return new Promise((resolve) => {
        try {
          new THREE.GLTFLoader().load(CarLib.CDN + name + '.glb', (g) => resolve(g.scene), undefined, () => resolve(null));
        } catch (e) { resolve(null); }
      });
    },
    async loadAll() {
      const out = await Promise.all(CarLib.FILES.map((f) => CarLib.loadOne(f)));
      CarLib.FILES.forEach((f, i) => {
        if (!out[i]) return;
        out[i].traverse((o) => { if (o.isMesh) { o.castShadow = true; } });
        const box = new THREE.Box3().setFromObject(out[i]);
        const size = new THREE.Vector3(); box.getSize(size);
        CarLib.lib[f] = { scene: out[i], size };
      });
      return Object.keys(CarLib.lib).length;
    },
    // clone scaled to targetLen on the long horizontal axis; front turned to face frontAxis ('+X' or '+Z')
    place(name, targetLen, frontAxis) {
      const b = CarLib.lib[name];
      if (!b) return null;
      const model = b.scene.clone(true);
      const longX = b.size.x >= b.size.z;
      const s = targetLen / Math.max(0.1, longX ? b.size.x : b.size.z);
      model.scale.setScalar(s);
      const box = new THREE.Box3().setFromObject(model);
      model.position.y = -box.min.y;
      model.position.x = -(box.min.x + box.max.x) / 2;
      model.position.z = -(box.min.z + box.max.z) / 2;
      const inner = new THREE.Group();
      inner.add(model);
      // glTF convention: front is +Z. Rotate so the front faces frontAxis.
      if (longX) inner.rotation.y = Math.PI / 2; // model was X-long; make it Z-long first
      if (frontAxis === '+X') inner.rotation.y += Math.PI / 2; // then front (+Z) to +X
      const wrap = new THREE.Group();
      wrap.add(inner);
      const wb = new THREE.Box3().setFromObject(wrap);
      return { mesh: wrap, w: wb.max.x - wb.min.x, d: wb.max.z - wb.min.z };
    },
  };


  /* ---------- Minimap: prerendered road layer + live player/landmark dots ---------- */
  const Minimap = {
    S: 148, cv: null, ctx: null, base: null, t: 0, scale: 0.33, basePaths: 0,
    init(root) {
      const cv = document.createElement('canvas');
      cv.id = 'gmap';
      cv.width = Minimap.S * 2; cv.height = Minimap.S * 2;
      root.appendChild(cv);
      Minimap.cv = cv;
      Minimap.ctx = cv.getContext('2d');
      Minimap.buildBase();
    },
    buildBase() {
      const S = Minimap.S * 2;
      const off = document.createElement('canvas');
      off.width = S; off.height = S;
      const c = off.getContext('2d');
      c.fillStyle = 'rgba(16,26,44,0.9)';
      c.beginPath(); c.arc(S / 2, S / 2, S / 2 - 2, 0, Math.PI * 2); c.fill();
      let maxE = 185;
      Cairo.paths.forEach((p) => p.pts.forEach((pt) => { maxE = Math.max(maxE, Math.abs(pt[0]), Math.abs(pt[1])); }));
      Minimap.scale = (S / 2 - 8) / maxE;
      const px = (x, z) => [S / 2 + x * Minimap.scale, S / 2 + z * Minimap.scale];
      c.save();
      c.beginPath(); c.arc(S / 2, S / 2, S / 2 - 2, 0, Math.PI * 2); c.clip();
      const cols = ['#6f83ad', '#8fa3cc', '#b4c6e8', '#dbe6fb'];
      Cairo.paths.forEach((p) => {
        c.strokeStyle = cols[p.r] || cols[0];
        c.lineWidth = [1.4, 2, 3, 4][p.r] || 1.4;
        c.beginPath();
        p.pts.forEach((pt, i) => {
          const [mx, my] = px(pt[0], pt[1]);
          if (i === 0) c.moveTo(mx, my); else c.lineTo(mx, my);
        });
        c.stroke();
      });
      c.restore();
      c.strokeStyle = '#d4af37';
      c.lineWidth = 2;
      c.beginPath(); c.arc(S / 2, S / 2, S / 2 - 2, 0, Math.PI * 2); c.stroke();
      Minimap.base = off;
      Minimap.basePaths = Cairo.paths.length;
    },
    update(dt) {
      if (!Minimap.ctx || !Minimap.base) return;
      if (Cairo.paths.length && Minimap.basePaths !== Cairo.paths.length) Minimap.buildBase();
      Minimap.t += dt;
      if (Minimap.t < 0.25) return;
      Minimap.t = 0;
      const S = Minimap.S * 2, c = Minimap.ctx;
      c.clearRect(0, 0, S, S);
      c.drawImage(Minimap.base, 0, 0);
      const px = (x, z) => [S / 2 + x * Minimap.scale, S / 2 + z * Minimap.scale];
      // landmarks: gold for unfound, dim green for found
      World.landmarks.forEach((l) => {
        const [mx, my] = px(l.x, l.z);
        c.fillStyle = l.found ? 'rgba(90,160,110,0.8)' : '#ffd257';
        c.beginPath(); c.arc(mx, my, 3.4, 0, Math.PI * 2); c.fill();
      });
      // player / car
      const x = Drive.active ? Drive.x : Player.x, z = Drive.active ? Drive.z : Player.z;
      const [mx, my] = px(x, z);
      const yaw = Drive.active ? Drive.heading : FollowCam.yaw + Math.PI;
      c.fillStyle = Drive.active ? '#ff7b6b' : '#7CFFB2';
      c.save();
      c.translate(mx, my);
      c.rotate(-yaw);
      c.beginPath();
      c.moveTo(0, -7); c.lineTo(4.6, 5); c.lineTo(-4.6, 5);
      c.closePath(); c.fill();
      c.restore();
    },
  };

  /* Animated human wrapper: mesh + mixer + simple state machine. */
  function makeHuman(key) {
    const mesh = Assets.clone(key);
    if (mesh) mesh.traverse((o) => { if (o.isMesh || o.isSkinnedMesh) { o.castShadow = true; o.receiveShadow = false; } });
    if (!mesh) return null;
    const mixer = new THREE.AnimationMixer(mesh);
    const clips = {};
    Assets.anims(key).forEach((c) => { clips[c.name] = c; });
    let current = null;
    return {
      mesh, mixer,
      play(name, fade) {
        if (current === name || !clips[name]) return;
        const next = mixer.clipAction(clips[name]);
        next.reset().setLoop(THREE.LoopRepeat).fadeIn(fade || 0.15).play();
        if (current && clips[current]) mixer.clipAction(clips[current]).fadeOut(fade || 0.15);
        current = name;
      },
      currentName: () => current,
    };
  }

  /* ---------- Pedestrians: ambient walkers on the sidewalks ---------- */
  const Pedestrians = {
    list: [],
    build(scene) {
      const half = CITY.size / 2, step = CITY.block + CITY.road;
      const lanes = [];
      for (let p = -half + step / 2; p < half; p += step) lanes.push(p);
      let seed = 13;
      const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
      for (let i = 0; i < 10; i++) {
        const key = i % 2 === 0 ? 'ped_a' : 'ped_b';
        const h = makeHuman(key);
        if (!h) break;
        const horizontal = rnd() > 0.5;
        const lane = lanes[Math.floor(rnd() * lanes.length)] + (rnd() > 0.5 ? CITY.road / 2 - 1.5 : -(CITY.road / 2 - 1.5));
        const speed = (2 + rnd() * 2) * (rnd() > 0.5 ? 1 : -1);
        const pos = (rnd() - 0.5) * CITY.size;
        h.mesh.position.y = 0;
        scene.add(h.mesh);
        h.play('Walking_A', 0);
        h.mixer.update(rnd() * 2); // desync strides
        const roads = Cairo.paths.filter((p) => p.r >= 0);
        if (roads.length) {
          const path = roads[Math.floor(rnd() * roads.length)];
          Pedestrians.list.push({ h, path, pos: rnd() * path.len, dir: rnd() > 0.5 ? 1 : -1, speed: 1.6 + rnd() * 1.4 });
        } else {
          Pedestrians.list.push({ h, horizontal, lane, pos, speed });
        }
      }
    },
    update(dt) {
      const lim = CITY.size / 2 + 4;
      Pedestrians.list.forEach((p) => {
        if (p.path) {
          p.pos += p.dir * p.speed * dt;
          if (p.pos > p.path.len || p.pos < 0) { p.dir *= -1; p.pos = Math.max(0, Math.min(p.path.len, p.pos)); }
          const at = Cairo.at(p.path, p.pos);
          const off = p.path.width / 2 - 1.1;
          p.h.mesh.position.set(at.x + -at.dz * off * p.dir, 0, at.z + at.dx * off * p.dir);
          p.h.mesh.rotation.y = Math.atan2(at.dx * p.dir, at.dz * p.dir);
          p.h.mixer.update(dt);
          return;
        }
        p.pos += p.speed * dt;
        if (p.pos > lim) p.pos = -lim;
        if (p.pos < -lim) p.pos = lim;
        if (p.horizontal) {
          p.h.mesh.position.set(p.pos, 0, p.lane);
          p.h.mesh.rotation.y = p.speed > 0 ? Math.PI / 2 : -Math.PI / 2;
        } else {
          p.h.mesh.position.set(p.lane, 0, p.pos);
          p.h.mesh.rotation.y = p.speed > 0 ? 0 : Math.PI;
        }
        p.h.mixer.update(dt);
      });
    },
  };

  /* ---------- Vehicles: ambient street traffic (non-drivable) ---------- */
  const Vehicles = {
    cars: [],
    build(scene) {
      const half = CITY.size / 2, step = CITY.block + CITY.road;
      const lanes = [];
      for (let p = -half + step / 2; p < half; p += step) lanes.push(p);
      const colors = [0xb03a2e, 0x2e5fa3, 0xd4af37, 0x6e7b8a, 0x3d8b6e];
      let seed = 7;
      const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
      const mix = ['taxi', 'taxi', 'taxi', 'taxi', 'sedan', 'sedan', 'sedan', 'suv', 'suv', 'van', 'van', 'delivery', 'truck', 'police'];
      for (let i = 0; i < 14; i++) {
        const horizontal = rnd() > 0.5;
        const lane = lanes[Math.floor(rnd() * lanes.length)] + (rnd() > 0.5 ? 2.5 : -2.5);
        let car = null;
        const want = mix[i % mix.length];
        const big = (want === 'truck' || want === 'delivery');
        const placed = CarLib.place(want, big ? 5.6 : 4.4, '+X');
        if (placed) {
          car = placed.mesh;
        } else {
          car = new THREE.Mesh(
            new THREE.BoxGeometry(3.4, 1.2, 1.7),
            new THREE.MeshLambertMaterial({ color: colors[Math.floor(rnd() * colors.length)] }));
          car.castShadow = true;
        }
        car.userData.baseY = placed ? 0 : 0.6;
        scene.add(car);
        const roads = Cairo.paths.filter((p) => p.r >= 1);
        if (roads.length) {
          const path = roads[Math.floor(rnd() * roads.length)];
          Vehicles.cars.push({
            mesh: car, path, baseY: car.userData.baseY,
            pos: rnd() * path.len,
            dir: rnd() > 0.5 ? 1 : -1,
            speed: 8 + rnd() * 7,
          });
        } else {
          Vehicles.cars.push({
            mesh: car, horizontal, lane, baseY: car.userData.baseY,
            pos: (rnd() - 0.5) * CITY.size,
            speed: (8 + rnd() * 7) * (rnd() > 0.5 ? 1 : -1),
          });
        }
      }
    },
    update(dt) {
      const lim = CITY.size / 2 + 6;
      Vehicles.cars.forEach((c) => {
        if (c.path) {
          c.pos += c.dir * Math.abs(c.speed) * dt;
          if (c.pos > c.path.len) c.pos = 0;
          const at = Cairo.at(c.path, c.pos);
          const off = c.path.width / 4;
          c.mesh.position.set(at.x + -at.dz * off * c.dir, c.baseY, at.z + at.dx * off * c.dir);
          c.mesh.rotation.y = -Math.atan2(at.dz * c.dir, at.dx * c.dir);
          return;
        }
        c.pos += c.speed * dt;
        if (c.pos > lim) c.pos = -lim;
        if (c.pos < -lim) c.pos = lim;
        if (c.horizontal) {
          c.mesh.position.set(c.pos, c.baseY, c.lane);
          c.mesh.rotation.y = c.speed > 0 ? Math.PI / 2 : -Math.PI / 2;
        } else {
          c.mesh.position.set(c.lane, c.baseY, c.pos);
          c.mesh.rotation.y = c.speed > 0 ? 0 : Math.PI;
        }
      });
    },
  };

  /* ---------- Drive: one drivable car, arcade physics ---------- */
  const Drive = {
    active: false, mesh: null, x: 4, z: 34, heading: Math.PI, speed: 0,
    ACCEL: 14, BRAKE: 22, MAX: 22, MAXREV: 7, TURN: 2.2,
    build(scene) {
      const taxi = CarLib.place('taxi', 4.6, '+Z');
      if (taxi) {
        const g = taxi.mesh;
        g.position.set(Drive.x, 0, Drive.z);
        g.rotation.y = Drive.heading;
        scene.add(g);
        Drive.mesh = g;
        return;
      }
      const g = new THREE.Group();
      const body = new THREE.Mesh(
        new THREE.BoxGeometry(2, 0.8, 4.2),
        new THREE.MeshLambertMaterial({ color: 0xd4af37 }));
      body.position.y = 0.7;
      const cabin = new THREE.Mesh(
        new THREE.BoxGeometry(1.7, 0.7, 2),
        new THREE.MeshLambertMaterial({ color: 0x22334d }));
      cabin.position.set(0, 1.3, -0.2);
      g.add(body); g.add(cabin);
      const wm = new THREE.MeshLambertMaterial({ color: 0x111111 });
      [[-0.95, 1.4], [0.95, 1.4], [-0.95, -1.4], [0.95, -1.4]].forEach(([x, z]) => {
        const w = new THREE.Mesh(new THREE.CylinderGeometry(0.38, 0.38, 0.3, 10), wm);
        w.rotation.z = Math.PI / 2; w.position.set(x, 0.38, z);
        g.add(w);
      });
      g.traverse((o) => { if (o.isMesh) o.castShadow = true; });
      g.position.set(Drive.x, 0, Drive.z);
      g.rotation.y = Drive.heading;
      scene.add(g);
      Drive.mesh = g;
    },
    near() {
      return Math.hypot(Player.x - Drive.x, Player.z - Drive.z) < 4;
    },
    toggle() {
      if (Drive.active) {
        Drive.active = false;
        Player.x = Drive.x + Math.sin(Drive.heading + Math.PI / 2) * 2.6;
        Player.z = Drive.z + Math.cos(Drive.heading + Math.PI / 2) * 2.6;
        Player.mesh.visible = true;
        HUD.driveBtn(false);
      } else if (Drive.near()) {
        Drive.active = true;
        Player.mesh.visible = false;
        HUD.driveBtn(true);
        HUD.toast('🚗');
      }
    },
    update(dt) {
      if (!Drive.active) return;
      const mv = Input.move();
      const throttle = mv.z, steer = mv.x;
      if (throttle > 0) Drive.speed += Drive.ACCEL * throttle * dt;
      else if (throttle < 0) Drive.speed += (Drive.speed > 0.5 ? -Drive.BRAKE : Drive.ACCEL * 0.6 * throttle) * dt;
      else Drive.speed -= Math.sign(Drive.speed) * Math.min(Math.abs(Drive.speed), 8 * dt);
      Drive.speed = Math.max(-Drive.MAXREV, Math.min(Drive.MAX, Drive.speed));
      if (Math.abs(Drive.speed) > 0.3) {
        Drive.heading -= steer * Drive.TURN * dt * Math.min(1, Math.abs(Drive.speed) / 8) * Math.sign(Drive.speed);
      }
      const nx = Drive.x + Math.sin(Drive.heading) * Drive.speed * dt;
      const nz = Drive.z + Math.cos(Drive.heading) * Drive.speed * dt;
      const bound = CITY.size / 2 - 2;
      if (!World.hit(nx, Drive.z)) Drive.x = Math.max(-bound, Math.min(bound, nx)); else Drive.speed = 0;
      if (!World.hit(Drive.x, nz)) Drive.z = Math.max(-bound, Math.min(bound, nz)); else Drive.speed = 0;
      Drive.mesh.position.set(Drive.x, 0, Drive.z);
      Drive.mesh.rotation.y = Drive.heading;
      // keep player glued (hidden) so camera + missions keep working
      Player.x = Drive.x; Player.z = Drive.z;
    },
  };

  /* ---------- Audio: tiny WebAudio ambience, off until toggled ---------- */
  const Audio8 = {
    ctx: null, on: false, nodes: null,
    toggle(btn) {
      Audio8.on = !Audio8.on;
      btn.textContent = Audio8.on ? '🔊' : '🔇';
      if (Audio8.on && !Audio8.ctx) {
        try {
          const ctx = new (window.AudioContext || window.webkitAudioContext)();
          const osc = ctx.createOscillator(); osc.type = 'sine'; osc.frequency.value = 82;
          const osc2 = ctx.createOscillator(); osc2.type = 'triangle'; osc2.frequency.value = 123.5;
          const g = ctx.createGain(); g.gain.value = 0.035;
          osc.connect(g); osc2.connect(g); g.connect(ctx.destination);
          osc.start(); osc2.start();
          Audio8.ctx = ctx; Audio8.nodes = { g };
        } catch (e) { Audio8.on = false; btn.textContent = '🔇'; }
      }
      if (Audio8.nodes) Audio8.nodes.g.gain.value = Audio8.on ? 0.035 : 0;
    },
  };

  /* ---------- Player: capsule, gravity, jump, collision ---------- */
  const Player = {
    mesh: null, x: 0, z: 30, y: 0, vy: 0, heading: 0, grounded: true,
    SPEED_WALK: 7, SPEED_RUN: 13, JUMP: 9, GRAV: 24,
    build(scene) {
      const g = new THREE.Group();
      Player.human = makeHuman('player');
      if (Player.human) {
        Player.human.mesh.position.y = 0;
        g.add(Player.human.mesh);
        Player.human.play('Idle', 0);
      } else {
        // fallback capsule if the GLB failed to load
        const body = new THREE.Mesh(
          new THREE.CylinderGeometry(0.55, 0.55, 1.5, 10),
          new THREE.MeshLambertMaterial({ color: 0xd4af37 }));
        body.position.y = 1.05;
        g.add(body);
      }
      scene.add(g);
      Player.mesh = g;
    },
    update(dt, camYaw) {
      const mv = Input.move();
      const mag = Math.hypot(mv.x, mv.z);
      if (mag > 0.05) {
        const ang = Math.atan2(mv.x, mv.z) + camYaw;
        Player.heading = ang;
        const speed = mv.run ? Player.SPEED_RUN : Player.SPEED_WALK;
        const nx = Player.x + Math.sin(ang) * speed * mag * dt;
        const nz = Player.z + Math.cos(ang) * speed * mag * dt;
        const bound = CITY.size / 2 - 2;
        if (!World.hit(nx, Player.z)) Player.x = Math.max(-bound, Math.min(bound, nx));
        if (!World.hit(Player.x, nz)) Player.z = Math.max(-bound, Math.min(bound, nz));
      }
      if (Input.jumpQueued) {
        if (Player.grounded) { Player.vy = Player.JUMP; Player.grounded = false; }
        Input.jumpQueued = false;
      }
      if (Player.human) {
        if (!Player.grounded) Player.human.play('Jump_Idle');
        else if (mag > 0.05) Player.human.play(mv.run ? 'Running_A' : 'Walking_A');
        else Player.human.play('Idle');
        Player.human.mixer.update(dt);
      }
      Player.vy -= Player.GRAV * dt;
      Player.y += Player.vy * dt;
      if (Player.y <= 0) { Player.y = 0; Player.vy = 0; Player.grounded = true; }
      Player.mesh.position.set(Player.x, Player.y, Player.z);
      Player.mesh.rotation.y = Player.heading;
      return mag;
    },
  };

  /* ---------- FollowCamera: third-person orbit behind player ---------- */
  const FollowCam = {
    yaw: Math.PI, pitch: 0.42, dist: 11,
    init(root) {
      const cv = root.querySelector('#gcity');
      let dragging = false, px = 0, py = 0;
      cv.addEventListener('pointerdown', (e) => {
        if (e.target !== cv) return;
        dragging = true; px = e.clientX; py = e.clientY; cv.setPointerCapture(e.pointerId);
      });
      cv.addEventListener('pointermove', (e) => {
        if (!dragging) return;
        FollowCam.yaw -= (e.clientX - px) * 0.005;
        FollowCam.pitch = Math.max(0.1, Math.min(1.2, FollowCam.pitch + (e.clientY - py) * 0.004));
        px = e.clientX; py = e.clientY;
      });
      cv.addEventListener('pointerup', () => { dragging = false; });
      cv.addEventListener('pointercancel', () => { dragging = false; });
    },
    update(camera) {
      const tx = Player.x, ty = Player.y + 2, tz = Player.z;
      const dx = -Math.sin(FollowCam.yaw) * Math.cos(FollowCam.pitch);
      const dz = -Math.cos(FollowCam.yaw) * Math.cos(FollowCam.pitch);
      const dy = Math.sin(FollowCam.pitch);
      let dist = FollowCam.dist;
      // camera collision: shorten the boom until the sampled ray is clear of buildings
      const steps = 16;
      for (let i = 2; i <= steps; i++) {
        const t = (FollowCam.dist * i) / steps;
        const sx = tx + dx * t, sy = ty + dy * t, sz = tz + dz * t;
        let blocked = false;
        for (const c of World.colliders) {
          if (sx > c.minX && sx < c.maxX && sz > c.minZ && sz < c.maxZ && sy < (c.h || 40)) { blocked = true; break; }
        }
        if (blocked) { dist = Math.max(2.5, t - FollowCam.dist / steps - 0.6); break; }
      }
      const cx = tx + dx * dist;
      const cz = tz + dz * dist;
      const cy = ty + dy * dist;
      camera.position.set(cx, Math.max(1.5, cy), cz);
      camera.lookAt(tx, ty, tz);
    },
  };

  /* ---------- Discoveries: proximity mission ---------- */
  const Mission = {
    GOAL: 3,
    foundCount() { return World.landmarks.filter((l) => l.found).length; },
    update() {
      World.landmarks.forEach((l) => {
        if (l.found) return;
        const d = Math.hypot(Player.x - l.x, Player.z - l.z);
        l.mesh.rotation.y += 0.02;
        if (d < 4) {
          l.found = true;
          l.mesh.material.color.set(0x7CFFB2);
          l.mesh.material.emissive.set(0x1d5c38);
          l.ring.material.color.set(0x7CFFB2);
          Mission.award(l);
        }
      });
    },
    award(l) {
      const lang = (typeof state !== 'undefined' && state.lang) || 'en';
      const name = lang === 'ar' ? l.name.ar : l.name.en;
      HUD.toast('🏆 ' + name + ' (+15)');
      try {
        if (typeof state !== 'undefined') {
          state.points += 15;
          localStorage.setItem('nilora.points', String(state.points));
          if (typeof updateHud === 'function') updateHud();
        }
      } catch (e) {}
      HUD.mission();
      Game.persist();
    },
  };

  /* ---------- HUD ---------- */
  const HUD = {
    el: {},
    mount(root) {
      const wrap = document.createElement('div');
      wrap.id = 'ghud';
      wrap.innerHTML =
        '<div id="gmission" class="gpanel"></div>' +
        '<div id="gfps" class="gpanel gfps">…</div>' +
        '<button id="gquality" class="gpanel gbtn" type="button">HQ</button>' +
        '<button id="gsound" class="gpanel gbtn" type="button" style="top:calc(52px + env(safe-area-inset-top));right:64px">🔇</button>' +
        '<button id="gdrive" class="gpanel gbtn" type="button" style="top:calc(94px + env(safe-area-inset-top));right:64px">🚗</button>' +
        '<button id="gexit" class="gpanel gbtn" type="button">✕</button>' +
        '<div id="gtoast"></div>' +
        '<div id="gjoy"><div id="gjoyKnob"></div></div>' +
        '<button id="gjump" type="button">⤒</button>';
      root.appendChild(wrap);
      HUD.el = {
        mission: wrap.querySelector('#gmission'), fps: wrap.querySelector('#gfps'),
        quality: wrap.querySelector('#gquality'), exit: wrap.querySelector('#gexit'),
        toast: wrap.querySelector('#gtoast'),
      };
      HUD.el.exit.addEventListener('click', () => { location.hash = '#/explore'; });
      wrap.querySelector('#gsound').addEventListener('click', (e) => Audio8.toggle(e.currentTarget));
      wrap.querySelector('#gdrive').addEventListener('click', () => Drive.toggle());
      HUD.el.quality.addEventListener('click', () => {
        Game.hq = !Game.hq;
        HUD.el.quality.textContent = Game.hq ? 'HQ' : 'LQ';
        Game.applyQuality();
      });
      HUD.mission();
    },
    mission() {
      const n = Mission.foundCount();
      const done = n >= Mission.GOAL;
      const lang = (typeof state !== 'undefined' && state.lang) || 'en';
      HUD.el.mission.innerHTML = lang === 'ar'
        ? '🧭 مهمة: اكتشف ' + Mission.GOAL + ' معالم — ' + n + '/' + Mission.GOAL + (done ? ' ✅' : '')
        : '🧭 Mission: discover ' + Mission.GOAL + ' landmarks — ' + n + '/' + Mission.GOAL + (done ? ' ✅' : '');
    },
    toast(msg) {
      HUD.el.toast.textContent = msg;
      HUD.el.toast.classList.add('show');
      clearTimeout(HUD._t);
      HUD._t = setTimeout(() => HUD.el.toast.classList.remove('show'), 2400);
    },
    fps(v) { HUD.el.fps.textContent = v + ' fps'; },
    driveBtn(on) {
      const b = document.getElementById('gdrive');
      if (b) b.style.background = on ? '#d4af37' : 'rgba(11,20,32,.82)';
    },
  };

  const CSS =
    '#gcityHost{position:fixed;inset:0;z-index:60;background:#0b1420}' +
    '#gcity{width:100%;height:100%;display:block;touch-action:none}' +
    '.gpanel{position:fixed;z-index:61;background:rgba(11,20,32,.82);color:#e8dcc0;border:1px solid #d4af37;' +
    'border-radius:10px;padding:8px 12px;font:600 13px/1.3 system-ui}' +
    '#gmission{top:calc(10px + env(safe-area-inset-top));left:10px;max-width:70vw}' +
    '#gmap{position:fixed;top:calc(62px + env(safe-area-inset-top));left:10px;width:148px;height:148px;z-index:61;border-radius:50%}' +
    '.gfps{top:calc(10px + env(safe-area-inset-top));right:120px}' +
    '#gquality{top:calc(10px + env(safe-area-inset-top));right:64px;cursor:pointer}' +
    '#gexit{top:calc(10px + env(safe-area-inset-top));right:10px;cursor:pointer}' +
    '#gtoast{position:fixed;bottom:120px;left:50%;transform:translateX(-50%);z-index:62;background:#d4af37;color:#0b1420;' +
    'font:700 15px system-ui;padding:10px 18px;border-radius:999px;opacity:0;transition:opacity .25s;pointer-events:none}' +
    '#gtoast.show{opacity:1}' +
    '#gjoy{position:fixed;bottom:calc(24px + env(safe-area-inset-bottom));left:24px;width:120px;height:120px;border-radius:50%;' +
    'border:2px solid rgba(212,175,55,.55);background:rgba(11,20,32,.4);z-index:61;touch-action:none}' +
    '#gjoyKnob{position:absolute;left:35px;top:35px;width:50px;height:50px;border-radius:50%;background:#d4af37;opacity:.9}' +
    '#gjump{position:fixed;bottom:calc(40px + env(safe-area-inset-bottom));right:28px;width:72px;height:72px;border-radius:50%;' +
    'border:2px solid #d4af37;background:rgba(212,175,55,.25);color:#e8dcc0;font-size:30px;z-index:61;touch-action:none}' +
    '@media (pointer:fine){#gjoy,#gjump{opacity:.35}}';

  /* ---------- Game orchestrator ---------- */
  const Game = {
    running: false, hq: true, raf: 0, renderer: null, scene: null, camera: null,
    frames: 0, fpsT: 0, last: 0, host: null,
    async start(appEl) {
      if (Game.running) return;
      Game.running = true;
      const intro = document.getElementById('intro');
      if (intro) intro.remove();
      const host = document.createElement('div');
      host.id = 'gcityHost';
      host.innerHTML = '<canvas id="gcity"></canvas>';
      appEl.appendChild(host);
      Game.host = host;
      const style = document.createElement('style');
      style.id = 'gcityCSS'; style.textContent = CSS;
      host.appendChild(style);

      const canvas = host.querySelector('#gcity');
      const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
      renderer.shadowMap.enabled = true;
      renderer.shadowMap.type = THREE.PCFSoftShadowMap;
      renderer.outputEncoding = THREE.sRGBEncoding;
      renderer.toneMapping = THREE.ACESFilmicToneMapping;
      renderer.toneMappingExposure = 1.15;
      Game.renderer = renderer;
      try {
        const gl = renderer.getContext();
        const ext = gl.getExtension('WEBGL_debug_renderer_info');
        const rn = ext ? String(gl.getParameter(ext.UNMASKED_RENDERER_WEBGL)) : '';
        if (/swiftshader|llvmpipe|software|angle \(google/i.test(rn)) Game.hq = false;
      } catch (e) { /* keep HQ default */ }
      const scene = new THREE.Scene();
      scene.background = new THREE.Color(0x0b1420);
      scene.fog = new THREE.Fog(0x1a2233, 60, 260);
      Game.scene = scene;
      const camera = new THREE.PerspectiveCamera(62, 2, 0.1, 900);
      Game.camera = camera;

      const skyDome = new THREE.Mesh(
        new THREE.SphereGeometry(430, 24, 16),
        new THREE.MeshBasicMaterial({ map: Tex.sky(), side: THREE.BackSide, fog: false, depthWrite: false }));
      skyDome.renderOrder = -1;
      scene.add(skyDome);
      Game.skyDome = skyDome;

      scene.add(new THREE.HemisphereLight(0x4a5f8d, 0x2a2018, 0.75));
      scene.add(new THREE.AmbientLight(0x8090b0, 0.25));
      const sun = new THREE.DirectionalLight(0xffc37a, 1.25);
      sun.position.set(70, 95, 35);
      sun.castShadow = true;
      sun.shadow.mapSize.set(2048, 2048);
      sun.shadow.camera.left = -80; sun.shadow.camera.right = 80;
      sun.shadow.camera.top = 80; sun.shadow.camera.bottom = -80;
      sun.shadow.camera.near = 1; sun.shadow.camera.far = 320;
      sun.shadow.bias = -0.0015;
      scene.add(sun); scene.add(sun.target);
      Game.sun = sun;

      if (THREE.EffectComposer && THREE.RenderPass && THREE.UnrealBloomPass) {
        const composer = new THREE.EffectComposer(renderer);
        composer.addPass(new THREE.RenderPass(scene, camera));
        const bloom = new THREE.UnrealBloomPass(new THREE.Vector2(1, 1), 0.22, 0.45, 0.92);
        composer.addPass(bloom);
        Game.composer = composer;
      }

      HUD.mount(host);
      Minimap.init(host);
      HUD.toast('⏳ loading city…');
      await Buildings.loadAll();
      await Props.loadAll();
      await CarLib.loadAll();
      World.colliders = []; World.landmarks = [];
      World.build(scene);
      Props.build(scene);
      Vehicles.cars = []; Vehicles.build(scene);
      Drive.build(scene);
      if (Cairo.paths.length) {
        const majors = Cairo.paths.filter((p) => p.r >= 2).sort((a, b) => b.len - a.len);
        const p0 = majors[0] || Cairo.paths[0];
        const at = Cairo.at(p0, p0.len / 2);
        Player.x = at.x; Player.z = at.z;
        Drive.x = at.x + 4; Drive.z = at.z + 4;
        if (Drive.mesh) { Drive.mesh.position.set(Drive.x, 0, Drive.z); Drive.heading = Math.atan2(at.dx, at.dz); Drive.mesh.rotation.y = Drive.heading; }
      }
      await Assets.loadAll();
      if (!Assets.lib.player) await Assets.load('player');
      Pedestrians.list = []; Pedestrians.build(scene);
      Player.build(scene);
      Input.init(host);
      FollowCam.init(host);
      HUD.el.quality.textContent = Game.hq ? 'HQ' : 'LQ';
      Game.applyQuality();
      Game.resize();
      window.addEventListener('resize', Game.resize);
      document.addEventListener('visibilitychange', Game.onVis);

      await Save.open();
      const saved = await Save.get('save');
      if (saved) {
        Player.x = saved.x; Player.z = saved.z;
        if (World.hit(Player.x, Player.z) && Cairo.paths.length) {
          let best = null, bd = 1e9;
          Cairo.paths.forEach((p) => {
            for (let s = 0; s <= p.len; s += 8) {
              const at = Cairo.at(p, s);
              const dd = (at.x - Player.x) * (at.x - Player.x) + (at.z - Player.z) * (at.z - Player.z);
              if (dd < bd) { bd = dd; best = at; }
            }
          });
          if (best) { Player.x = best.x; Player.z = best.z; }
        }
        (saved.found || []).forEach((id) => {
          const l = World.landmarks.find((m) => m.id === id);
          if (l) { l.found = true; l.mesh.material.color.set(0x7CFFB2); l.mesh.material.emissive.set(0x1d5c38); l.ring.material.color.set(0x7CFFB2); }
        });
        HUD.mission();
        HUD.toast('💾 save loaded');
      }

      Game.last = performance.now();
      Game.fpsT = Game.last; Game.frames = 0;
      const loop = (now) => {
        if (!Game.running) return;
        Game.raf = requestAnimationFrame(loop);
        const dt = Math.min(0.05, (now - Game.last) / 1000);
        Game.last = now;
        if (!document.hidden) {
          if (Drive.active) Drive.update(dt); else Player.update(dt, FollowCam.yaw);
          Vehicles.update(dt);
          Pedestrians.update(dt);
          FollowCam.update(camera);
          Mission.update();
          Minimap.update(dt);
          const px = Drive.active ? Drive.x : Player.x, pz = Drive.active ? Drive.z : Player.z;
          if (Game.sun) {
            Game.sun.position.set(px + 70, 95, pz + 35);
            Game.sun.target.position.set(px, 0, pz);
            Game.sun.target.updateMatrixWorld();
          }
          if (Game.skyDome) Game.skyDome.position.set(px, 0, pz);
          if (Game.composer && Game.hq) Game.composer.render();
          else renderer.render(scene, camera);
          Game.frames++;
          if (now - Game.fpsT >= 1000) {
            HUD.fps(Math.round(Game.frames * 1000 / (now - Game.fpsT)));
            Game.frames = 0; Game.fpsT = now;
          }
        }
      };
      Game.raf = requestAnimationFrame(loop);
      // autosave every 5s
      Game.saveTimer = setInterval(() => Game.persist(), 5000);
    },
    applyQuality() {
      const pr = Game.hq ? Math.min(window.devicePixelRatio || 1, 2) : 1;
      Game.renderer.setPixelRatio(pr);
      Game.renderer.toneMapping = Game.hq ? THREE.ACESFilmicToneMapping : THREE.NoToneMapping;
      Game.renderer.outputEncoding = Game.hq ? THREE.sRGBEncoding : THREE.LinearEncoding;
      if (Game.sun) Game.sun.castShadow = Game.hq;
      if (Game.scene) Game.scene.traverse((o) => { if (o.material) { const ms = Array.isArray(o.material) ? o.material : [o.material]; ms.forEach((m) => { m.needsUpdate = true; }); } });
      Game.resize();
    },
    resize() {
      if (!Game.renderer) return;
      const w = window.innerWidth, h = window.innerHeight;
      Game.renderer.setSize(w, h, false);
      if (Game.composer) Game.composer.setSize(w, h);
      Game.camera.aspect = w / h;
      Game.camera.updateProjectionMatrix();
    },
    onVis() { /* render skipped while hidden via document.hidden check in loop */ },
    persist() {
      Save.set('save', {
        x: Player.x, z: Player.z,
        found: World.landmarks.filter((l) => l.found).map((l) => l.id),
      });
    },
    stop() {
      if (!Game.running) return;
      Game.running = false;
      cancelAnimationFrame(Game.raf);
      clearInterval(Game.saveTimer);
      window.removeEventListener('resize', Game.resize);
      document.removeEventListener('visibilitychange', Game.onVis);
      Game.persist();
      if (Game.renderer) { Game.renderer.dispose(); Game.renderer = null; }
      if (Game.host && Game.host.parentNode) Game.host.parentNode.removeChild(Game.host);
      Game.host = null;
    },
  };

  window.NiloraGame = {
    start: Game.start.bind(Game), stop: Game.stop.bind(Game),
    debug: () => ({ x: Player.x, y: Player.y, z: Player.z, grounded: Player.grounded,
      found: Mission.foundCount(), cars: Vehicles.cars.length,
      peds: Pedestrians.list.length, driving: Drive.active, carSpeed: Math.round(Drive.speed * 10) / 10, anim: Player.human ? Player.human.currentName() : 'capsule',
      assets: Object.keys(Assets.lib),
      props: Props.stats,
      carSample: Vehicles.cars.slice(0, 6).map((c) => [Math.round(c.mesh.position.x), Math.round(c.mesh.position.z), c.mesh.type]),
      collides: (x, z) => World.hit(x, z),
      tp: (x, z) => { Player.x = x; Player.z = z; } }),
  };
})();
