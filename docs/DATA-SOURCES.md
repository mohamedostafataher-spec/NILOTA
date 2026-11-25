# NILORA — External Data Sources, Licenses & Attribution

Every external source the web app uses. All are free and require **no API key and no account**.
Last audited: 2026-11-25 (against `web/app.js`, `web/globe.js`, `web/index.html`).

## Live data APIs (called at runtime)

| Source | Used for | Endpoint | Cost | Key? | License / terms | Attribution needed |
|---|---|---|---|---|---|---|
| Open-Meteo | City weather cards | `api.open-meteo.com/v1/forecast` | Free (non-commercial) | No | CC BY 4.0 | "Weather data by Open-Meteo.com" |
| Aladhan | Prayer times per city | `api.aladhan.com/v1/timingsByCity` | Free | No | Free to use (aladhan.com terms) | "Prayer times by Aladhan.com" |
| USGS | Earthquake feed | `earthquake.usgs.gov/.../2.5_day.geojson` | Free | No | Public domain (US Gov) | "USGS Earthquake Hazards Program" |
| Nager.Date | Public holidays | `date.nager.at/api/v3/...` | Free | No | Open data, free use | "Nager.Date" |
| open.er-api.com | EGP exchange rates | `open.er-api.com/v6/latest/EGP` | Free | No | Free tier, no key | "ExchangeRate-API (open access)" |
| Wikipedia REST | City summary snippets | `*.wikipedia.org` (via API, `origin=*`) | Free | No | CC BY-SA 3.0 text | "Wikipedia" + link to article |

## Map & globe assets

| Source | Used for | Cost | Key? | License | Attribution needed |
|---|---|---|---|---|---|
| OpenStreetMap tiles | Leaflet live map | Free (light use per tile policy) | No | ODbL | "© OpenStreetMap contributors" (already on map) |
| world-atlas (unpkg) | Globe country outlines (TopoJSON) | Free | No | ISC code; Natural Earth data is public domain | "Natural Earth" |
| Leaflet (unpkg) | Map library | Free | No | BSD-2-Clause | — |
| Three.js r128 (cdnjs) | Globe + 3D city engine | Free | No | MIT | — |
| topojson-client (unpkg) | TopoJSON parsing | Free | No | ISC | — |
| Google Fonts (Cairo, Tajawal) | Typography | Free | No | SIL OFL 1.1 | — |

## 3D models (NILORA City characters)

| Source | Used for | Cost | Key? | License | Attribution |
|---|---|---|---|---|---|
| KayKit Character Pack: Adventurers 1.0 (Kay Lousberg) — via GitHub + jsDelivr CDN | Player (Rogue_Hooded.glb) + pedestrians (Knight.glb, Mage.glb), rigged GLB with 76 embedded animations (Idle, Walking_A, Running_A, Jump_* used) | Free | No | **CC0 1.0** (verified in pack LICENSE.txt: "Creative Commons Zero... free to use in personal, educational and commercial projects", credit not mandatory) | Credit line in app (not legally required): "Characters: KayKit Adventurers by Kay Lousberg (kaylousberg.com), CC0" |
| Three.js r128 examples (GLTFLoader, SkeletonUtils via unpkg) | Loading/cloning rigged GLBs | Free | No | MIT | — |

Note: characters are **stylized low-poly cartoon humans** (chibi proportions), not photorealistic. No CC0 photorealistic rigged+animated human exists; this is the best free keyless option. Models are served full-size from jsDelivr (~3.6 MB each) until the repo's binary upload path is fixed; quantized 455 KB versions are prepared locally.

## Parked / not currently wired

- Natural Earth shapefiles (`ne_10m_admin_0_countries`), geoBoundaries EGY ADM1/ADM2: downloaded for a different project, **not used by NILORA** and not in this repo. If ever added: Natural Earth = public domain; geoBoundaries = CC BY 4.0.

## Added in v20-v22 (visual stages)

| Source | License | Used for | How NILORA fetches it |
| --- | --- | --- | --- |
| Poly Haven (polyhaven.com), textures `aerial_asphalt_01`, `checkered_pavement_tiles` (1k JPG) | CC0 1.0 (verified on site) | Photo ground/pavement textures in the 3D city | Direct CDN image URLs (dl.polyhaven.org, CORS `*`), procedural canvas texture stays as fallback if the CDN fails |
| OpenStreetMap via Overpass API (overpass.kumi.systems) | ODbL 1.0, "(c) OpenStreetMap contributors" | Real Downtown Cairo road network in the 3D city (391 ways baked to `web/cairo-roads.js`) | Baked offline to a static JS file; the game needs no API key at runtime |
| Quaternius Universal Animation Library mirror (github.com/J-Ponzo/gltf-universal-animation-library) | CC0 1.0 (LICENSE in repo; Quaternius packs are CC0) | RESEARCHED, not used: 46 quality animations but Rigify `DEF-*` bone names do not match available character rigs (0/53 overlap), so in-app retargeting was rejected | n/a |
| Quaternius Ultimate Modular Men/Women (github.com/trebeljahr/quaternius-showcase mirror) | CC0 1.0 (LICENSES.md per-pack table) | RESEARCHED, not used yet: adult-proportioned humans but rigged WITHOUT animations; pairing them with UAL clips needs offline retargeting | n/a |

Honesty note: the player/pedestrian characters remain KayKit Adventurers (CC0, chibi proportions). No keyless, no-signup, CC0 source of REALISTIC rigged-and-animated humans exists on a fetchable CDN as of 2026-11-25; that is why characters stay stylized.

## Rules this repo follows

1. No source may require an account, signup, or API key.
2. Fake/sample content is always labeled "Sample/Demo" in the UI.
3. Every API response panel shows source name + fetch timestamp (implemented in the live panels).
4. If a source is unreachable, the UI degrades to a labeled cached/demo state, never a silent blank.
