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

## UI simplification pass (v38) - live
- Mohamed's feedback (Nov 25): simpler UI, clean up clutter/formatting.
- Game HUD: removed the on-screen fps counter; right-side buttons (exit / HQ / sound / car) now form one clean aligned column on the right edge.
- Home: removed the duplicate points figure from the progress strip (level + points already live in the header).
- styles.css and app.js are now version-tagged like game.js, so future updates bypass the 10-minute HTTP cache immediately.

## Bottled-style bottom navigation (v39) - live
- Mohamed's direction (Nov 25): make the UI like Bottled (message-in-a-bottle app), especially the bottom menus.
- Bottom bar is now icons-only (labels kept in the DOM for accessibility/localization, visually hidden), evenly spaced, RTL-correct.
- Center: prominent raised gold circular button with the NILORA ankh, Bottled's signature center-action pattern; opens the 3D game directly.
- Tabs: home, explore, PLAY (center), passport, journey. Challenges left the bar (still reachable from the home cards).

## Roadmap backlog - Mohamed's differentiating features (Nov 25, priority order TBD by Mohamed)
1. Voice calls with instant translation (honest note: needs external infra - WebRTC signaling + translation API; a free in-browser Web Speech API demo is the keyless fallback).
2. REJECTED by Mohamed (too close to Bottled): message-in-a-bottle on the real map. APPROVED replacement (Mohamed, Nov 25): the Traveling Postcard - a card passed player to player around the world, each adding their country's stamp until it returns home (real cross-player handoff needs a backend - keyless version can be local/sample only).
3. Egyptian passport with a Golden Scarab stamp per discovered city.
4. Egyptian-Arabic-first identity across the product.
5. Weekly player challenges on the Egypt map.
6. Friend-making (ta'aruf) - APPROVED by Mohamed Nov 25: 'Traveler of the Day' (daily match with one player from another country, add as friend if both like) plus introducing players visiting the same city (real matching needs a backend - keyless version can be local/sample only).

## Egypt-wide vision (Nov 25)
- All of Egypt, realistic, city by city (Alexandria first, then Luxor, Aswan, more), each city with real OSM streets and landmarks, shipped as its own stage after Cairo polish - same keyless pipeline as Cairo.

## Minor roads thinner (v40) - live
- Minor/residential roads in the Cairo game now render thinner (widths 2.6/4.6/7/10 by road class instead of 4/6/8/10) so the dense downtown web reads cleaner at street level and on the minimap.

## Ta'aruf + Traveling Postcards, keyless web version (v41) - live
- Mohamed confirmed both mechanics (Nov 25): this is the local-first web version of the approved direction.
- New "Travelers & connections" screen (#/friends): Traveler of the Day gold card (rotates daily), 6 sample travelers (3 Egyptian: Nadia/Cairo, Yara/Alexandria, Omar/Luxor; plus Claire/Paris, Kenji/Kyoto, Marco/Rome), languages + intro note per traveler, Follow buttons persisted in localStorage, "your city" badge when a traveler matches a stamped/saved/visited city, Sample badge + honest note that real online matching needs accounts.
- Traveling postcards: a sent postcard now gains one journey hop per day (deterministic city path, max 4) and shows "traveling - last carried by <sample traveler>"; found sample postcards already had journeys.
- Linked from the home hub cards; grouped under the Journey tab in the bottom nav. All 4 languages (AR/EN/FR/RU).
- Incident (fixed same hour): the first v41 app.js commit duplicated file content due to a GitHub web-editor fill quirk; the file was replaced with the correct content ~10 minutes later. Live site impact window was brief.

## Adult-proportions pass on human characters (v42 + v43) - live
- Player and pedestrian rigs (KayKit Adventurers, chibi-proportioned) now get a runtime bone-scale pass at load: head smaller (0.74x), legs longer (upper 1.38x, lower 1.28x), spine slightly taller (1.08x); the rig root auto-lifts so feet still plant on the road.
- Done in game.js rather than binary GLB surgery: same visual result, instantly tunable/reversible.
- Verified live with screenshots: characters walk, animate and stand correctly.
- Honesty note: this softens the childish proportions but the KayKit art style stays stylized. Truly adult-looking humans need a different character pack (e.g. realistic-proportioned rigged CC0 models) - possible future stage, needs Mohamed's call.

## Characters and steering (v44 - v54) - live

- v45: tried swapping to Quaternius Ultimate Animated characters. It failed visually and was rolled back in v46. The v44 state was restored first.
- v47/v48: characters now use the Quaternius Universal Animation Library mannequin (CC0, loaded from the jsDelivr Papyszoo CDN), tinted per palette with vertex colors. A custom material on a SkinnedMesh needs skinning:true on r128 (that was the bug).
- v49: steering. Left/right input swings the camera and the heading is smoothed. Drag still orbits.
- v52: hair painted as vertex colors on the head.
- v53: eye placement attempt. Eyes showed up as stray dots at the neck.
- v54: eyes and nose removed. Hair is a dark cap, faces are blank. This is a stopgap, not a final look.
- Not verified: run and jump animations. No FPS figures have been measured.

## NILORA Lite (separate, web/lite/) - live

- Simple translator: EN, Egyptian Arabic, FR, RU. Free MyMemory API, offline phrasebook, history, theme.
- Call page: text call over an ntfy.sh topic with an 8-letter code. Optional WebRTC voice beta (not verified end to end, needs a TURN server on some networks).
- Situations tab: offline cards (taxi, market, restaurant, metro, help) in four languages, full-screen with Play.
- Egyptian layer: about 440 words and 50 patterns that swap formal Arabic output to Egyptian. It can sound stiff and is labelled in the UI.
- Phrasebook: about 90 conversational phrases, partial-phrase matching, and a guard against one-word garbage from MyMemory.
- Mic works on the user's phone (his screenshot). Speech voice depends on the phone and is usually not ar-EG.
