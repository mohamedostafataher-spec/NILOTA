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

  /* ---------- World: ground, streets, low-poly buildings, landmarks ---------- */
  const CITY = { size: 400, block: 34, road: 12 };
  const World = {
    group: null, colliders: [], landmarks: [],
    build(scene) {
      const g = new THREE.Group();
      // ground
      const ground = new THREE.Mesh(
        new THREE.PlaneGeometry(CITY.size, CITY.size),
        new THREE.MeshLambertMaterial({ color: 0x14243b }));
      ground.rotation.x = -Math.PI / 2;
      g.add(ground);
      // streets (grid) slightly above ground
      const roadMat = new THREE.MeshLambertMaterial({ color: 0x2a3a52 });
      const half = CITY.size / 2, step = CITY.block + CITY.road;
      for (let p = -half + step / 2; p < half; p += step) {
        const rx = new THREE.Mesh(new THREE.PlaneGeometry(CITY.road, CITY.size), roadMat);
        rx.rotation.x = -Math.PI / 2; rx.position.set(p, 0.02, 0); g.add(rx);
        const rz = new THREE.Mesh(new THREE.PlaneGeometry(CITY.size, CITY.road), roadMat);
        rz.rotation.x = -Math.PI / 2; rz.position.set(0, 0.02, p); g.add(rz);
      }
      // buildings per block (skip landmark blocks)
      const palette = [0x8f6b2f, 0x31435e, 0x4a5f7d, 0x6b5330, 0x22334d, 0x9a7a3d];
      const lm = World.landmarkCells();
      let seed = 42;
      const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
      for (let bx = -half + step / 2; bx < half; bx += step) {
        for (let bz = -half + step / 2; bz < half; bz += step) {
          const key = Math.round(bx) + ',' + Math.round(bz);
          if (lm[key]) continue;
          const n = 2 + Math.floor(rnd() * 3);
          for (let i = 0; i < n; i++) {
            const w = 6 + rnd() * 9, d = 6 + rnd() * 9, h = 6 + rnd() * 26;
            const x = bx + (rnd() - 0.5) * (CITY.block - w - 2);
            const z = bz + (rnd() - 0.5) * (CITY.block - d - 2);
            const m = new THREE.Mesh(
              new THREE.BoxGeometry(w, h, d),
              new THREE.MeshLambertMaterial({ color: palette[Math.floor(rnd() * palette.length)] }));
            m.position.set(x, h / 2, z);
            g.add(m);
            World.colliders.push({ minX: x - w / 2 - 0.6, maxX: x + w / 2 + 0.6, minZ: z - d / 2 - 0.6, maxZ: z + d / 2 + 0.6 });
          }
        }
      }
      // landmark discovery points
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
        g.add(ob);
        const ring = new THREE.Mesh(
          new THREE.RingGeometry(2.4, 3.2, 24),
          new THREE.MeshBasicMaterial({ color: 0xd4af37, side: THREE.DoubleSide, transparent: true, opacity: 0.7 }));
        ring.rotation.x = -Math.PI / 2; ring.position.set(cell.x, 0.05, cell.z);
        g.add(ring);
        World.landmarks.push({ id: 'lm' + i, name, x: cell.x, z: cell.z, mesh: ob, ring, found: false });
      });
      scene.add(g);
      World.group = g;
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

  /* Animated human wrapper: mesh + mixer + simple state machine. */
  function makeHuman(key) {
    const mesh = Assets.clone(key);
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
        Pedestrians.list.push({ h, horizontal, lane, pos, speed });
      }
    },
    update(dt) {
      const lim = CITY.size / 2 + 4;
      Pedestrians.list.forEach((p) => {
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
      for (let i = 0; i < 14; i++) {
        const horizontal = rnd() > 0.5;
        const lane = lanes[Math.floor(rnd() * lanes.length)] + (rnd() > 0.5 ? 2.5 : -2.5);
        const car = new THREE.Mesh(
          new THREE.BoxGeometry(3.4, 1.2, 1.7),
          new THREE.MeshLambertMaterial({ color: colors[Math.floor(rnd() * colors.length)] }));
        car.position.y = 0.6;
        scene.add(car);
        Vehicles.cars.push({
          mesh: car, horizontal, lane,
          pos: (rnd() - 0.5) * CITY.size,
          speed: (8 + rnd() * 7) * (rnd() > 0.5 ? 1 : -1),
        });
      }
    },
    update(dt) {
      const lim = CITY.size / 2 + 6;
      Vehicles.cars.forEach((c) => {
        c.pos += c.speed * dt;
        if (c.pos > lim) c.pos = -lim;
        if (c.pos < -lim) c.pos = lim;
        if (c.horizontal) {
          c.mesh.position.set(c.pos, 0.6, c.lane);
          c.mesh.rotation.y = c.speed > 0 ? Math.PI / 2 : -Math.PI / 2;
        } else {
          c.mesh.position.set(c.lane, 0.6, c.pos);
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
      const cx = Player.x - Math.sin(FollowCam.yaw) * FollowCam.dist * Math.cos(FollowCam.pitch);
      const cz = Player.z - Math.cos(FollowCam.yaw) * FollowCam.dist * Math.cos(FollowCam.pitch);
      const cy = Player.y + 2 + Math.sin(FollowCam.pitch) * FollowCam.dist;
      camera.position.set(cx, Math.max(1.5, cy), cz);
      camera.lookAt(Player.x, Player.y + 2, Player.z);
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
      Game.renderer = renderer;
      const scene = new THREE.Scene();
      scene.background = new THREE.Color(0x0b1420);
      scene.fog = new THREE.Fog(0x0b1420, 60, 220);
      Game.scene = scene;
      const camera = new THREE.PerspectiveCamera(62, 2, 0.1, 500);
      Game.camera = camera;

      scene.add(new THREE.AmbientLight(0x8090b0, 0.55));
      const sun = new THREE.DirectionalLight(0xffe0a3, 0.9);
      sun.position.set(80, 120, 40);
      scene.add(sun);

      World.colliders = []; World.landmarks = [];
      World.build(scene);
      Vehicles.cars = []; Vehicles.build(scene);
      Drive.build(scene);
      HUD.mount(host);
      HUD.toast('⏳ loading city…');
      await Assets.loadAll();
      if (!Assets.lib.player) await Assets.load('player');
      Pedestrians.list = []; Pedestrians.build(scene);
      Player.build(scene);
      Input.init(host);
      FollowCam.init(host);
      Game.resize();
      window.addEventListener('resize', Game.resize);
      document.addEventListener('visibilitychange', Game.onVis);

      await Save.open();
      const saved = await Save.get('save');
      if (saved) {
        Player.x = saved.x; Player.z = saved.z;
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
          renderer.render(scene, camera);
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
      Game.resize();
    },
    resize() {
      if (!Game.renderer) return;
      const w = window.innerWidth, h = window.innerHeight;
      Game.renderer.setSize(w, h, false);
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
      collides: (x, z) => World.hit(x, z),
      tp: (x, z) => { Player.x = x; Player.z = z; } }),
  };
})();
