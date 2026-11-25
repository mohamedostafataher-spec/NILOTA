# NILORA — Status & Deliverables (Master Spec, Part 20)

Honest status labels: **WT** = WORKING AND TESTED · **IT** = IMPLEMENTED BUT NOT FULLY TESTED · **RI** = REQUIRES EXTERNAL INFRASTRUCTURE · **DP** = DEVELOPMENT PREVIEW · **NI** = NOT IMPLEMENTED

## Phase A — Preserve & Audit (done 2026-11-25)
Existing web app (web/, keyless PWA) audited against the master spec. 20 countries / 66 cities confirmed in data.js — preserved, none fabricated. No ads, no accounts, all progress in localStorage.

| Feature | Status |
|---|---|
| Digital passport, country/city stamps, unlock animation + sound | WT |
| Interactive world map (OSM), real weather, quakes, prayer times, city stories | WT |
| Interactive 3D planet with real day/night | WT |
| Traveler Mode: local time, currency, local expressions | WT |
| Daily missions, weekly challenge, points + levels | WT |
| Daily global event | WT |
| World Cards (local) | IT (sample feed clearly labeled) |
| Journey Memory timeline | WT |
| AR/EN/FR/RU interfaces | WT (FR/RU core keys, fallback to EN) |

## Phase B — Core Visual Experience (done 2026-11-25, v9)
- New: skippable intro splash (rotating Earth, START EXPLORING / CONTINUE MY JOURNEY).
- New 5-destination navigation: HOME / EXPLORE / PASSPORT / CHALLENGES / MY JOURNEY. Old routes kept as aliases.
- EXPLORE screen: 3D planet + live map toggle, world pulse panels.
- CHALLENGES screen: daily missions, weekly challenge, leaderboard, events entry.
- MY JOURNEY: memory timeline, saved places, about.
- Home v3: rotating Earth hero, journey progress strip, today's mission, weekly challenge, featured discovery, world pulse.
- Passport: stamp states distinguished — virtual exploration vs 🔖 saved vs ✈️ physically visited (self-declared, manual). No physical-visit stamp is awarded for virtual previews.

## Phase D — Real Data (partial, v9)
- Source + update-time labels added to: weather (Open-Meteo), currency (open.er-api.com), prayer times (Aladhan), earthquakes (USGS), events (Nager.Date).
- No fabricated live data anywhere; failed loads show an unavailable state.

## Phase E — World Cards & Memory (partial, v9)
- Seeded postcard feed, souk offers and leaderboard are clearly badged "Sample content" / "Demo". No fake users are presented as real.

## Data sources (all keyless, free)
OpenStreetMap tiles · Open-Meteo · USGS · Aladhan · Wikipedia geosearch/REST · Nager.Date · open.er-api.com · unpkg/cdnjs CDNs (three.js, three-globe earth texture) · world-atlas TopoJSON.

## Dependencies installed: none (vanilla JS + CDN).

## License review
OSM ODbL (attributed) · Open-Meteo CC BY 4.0 · USGS/Aladhan/Nager/open.er-api public free APIs · Wikipedia CC BY-SA · three.js/three-globe MIT · Natural Earth public domain.

## Tests performed
- JS syntax check every batch; local headless renders of every new screen (mobile 412px).
- Live verification on GitHub Pages after each commit; post-commit content byte-compared via GitHub API.

## Unfinished work / next phases
- Phase C gameplay depth (achievements, badges, quiz missions) — IT/NI.
- Phase D: offline caching of API responses — NI.
- Phase E: real multi-device World Cards — RI (needs backend + consent).
- Phases F–I (accounts, crews, multiplayer, live translation, optimization) — RI/NI, not faked.
- AI assistant, voice translation — NI by design (require API keys, excluded by owner).
- Android native app: Kotlin ports not started; CI known-red on 2 pre-existing UI tests.

## Operating costs: $0 (GitHub Pages + free keyless APIs).

## Stage 4: Visual realism pass (v20) - live
- Golden-hour lighting: warm directional sun + hemisphere sky/ground light, ACES filmic tone mapping, sRGB output.
- Real-time shadows (PCF soft, 2048 map) that follow the player; HQ/LQ toggle turns shadows on/off.
- Procedural canvas textures (free, zero-download): asphalt with lane dashes, pavement blocks, building facades with lit windows.
- Dusk gradient sky dome matched to fog.
- Honesty note: stylized low-poly city + chibi CC0 characters; not photoreal. Textures are procedural, not photos.

## Stage 5: Camera + glow (v21-v22) - live
- Camera collision: the follow-camera boom shortens instead of clipping through buildings (ray-vs-building sampling, per-building heights).
- Emissive lit windows (emissiveMap) + subtle UnrealBloom on HQ (gated, off in LQ); bloom tuned down after first pass washed out.

## Stage 6: Real Cairo streets (v22-v25) - live
- Downtown Cairo road network from OpenStreetMap (ODbL): 391 real ways baked to web/cairo-roads.js; no runtime API key.
- Roads render as textured ribbons along real geometry; cars and pedestrians drive/walk the real streets (arc-length following, lane/sidewalk offsets).
- Buildings placed along streets with distance-to-road clearance (no overlap), player spawn snapped to a major road, save-restore collision guard.
- Poly Haven CC0 photo textures: cobblestone ground; asphalt kept procedural (dashes align with real road direction).
- Honesty note: buildings remain procedural low-poly boxes; only the street LAYOUT is real Cairo data. Grid city stays as fallback if the data file fails to load.
