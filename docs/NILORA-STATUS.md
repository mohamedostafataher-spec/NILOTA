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

## Stage 7: Real CC0 building models (v27) - live
- 10 Quaternius building GLBs (buildings_pack_2/3, CC0-1.0, via jsDelivr) replace procedural boxes in Cairo mode; measured with Box3 at load (node-scaled geometry), scaled to street slot, grounded.
- Colliders from measured footprints; verified live (wall pin + slide-along-wall + alley pass-through).
- Procedural boxes remain as automatic fallback if GLBs fail to load.
- Honesty note: stylized low-poly with flat colors (no textures); not photoreal.

## Stage 8: Street props (v28) - live
- Streetlights (single/double), palm trees (4 variants), traffic lights and stop signs from Quaternius street_pack/nature_pack (CC0-1.0).
- Placed along the real OSM Cairo road network: lights every ~22 m alternating sides, palms every ~34 m, signs/lights at detected intersections. Small pole colliders; placement skips building footprints.
- Caps for mobile: 100 lights, 110 palms, 22 intersection props.
- Honesty note: prop placement is algorithmic along road edges, not surveyed real-world positions.

## Stage 9: Real CC0 vehicles + drivable taxi (v33) - live
- Kenney Car Kit 3.1 (CC0) GLBs via jsDelivr: taxi, sedan, SUV, van, delivery, truck, police, ambulance.
- Weighted traffic mix of 14 vehicles driving the real OSM Cairo street network; peds and cars share roads.
- Player can enter/exit the yellow-white taxi and drive it (WASD/arrows + touch buttons); drive-facing verified live.
- Box-geometry fallback if a GLB fails to load.
- Honesty note: stylized low-poly vehicles; no traffic signals obeyed, no physics engine (arcade driving).

## Stage 10: Minimap (v34) - live
- Circular minimap (148 px) under the mission pill, prerendered from the real OSM road data (cairo-roads.js): 391 ways, width/color by road class, gold ring border in the NILORA palette.
- Live markers (redrawn ~4 Hz): player as a green heading arrow (red when driving the taxi), discovery landmarks as gold dots (dim green once found).
- Zero network cost: map is drawn once to an offscreen canvas at boot from local road data.
- Honesty note: map covers the loaded downtown block; it does not stream new tiles as you leave the area.

## Minimap readability tweak (v35) - live
- Brightened minimap road palette and widened road strokes so the street web reads against the night scene; lighter map disc background.

## Minimap road-render fix (v36) - live
- Fixed: the minimap base was prerendered before the OSM road paths finished building, so the street web never drew (v34/v35 showed only the player arrow and landmark dots). The base now rebuilds automatically once road paths are ready.

## Stage 11: Day/night cycle (v37) - live
- Full 5-minute day cycle: sun orbits the player (sunrise -> noon -> sunset -> night), cool moonlight at night.
- Sky gradient, fog, scene background, hemisphere/ambient light lerp through 4 palettes (sunrise/day/sunset/midnight); sky texture repaints at 2 Hz.
- Building window glow (emissive) fades up at night and down by day.
- Starts just after sunset (the classic NILORA look); free-running, not tied to real Cairo time.
- Honesty note: stylized cycle, 5 minutes long, not real-time-of-day; stars/moon sprite not included.
