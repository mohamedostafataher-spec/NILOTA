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

## Rules this repo follows

1. No source may require an account, signup, or API key.
2. Fake/sample content is always labeled "Sample/Demo" in the UI.
3. Every API response panel shows source name + fetch timestamp (implemented in the live panels).
4. If a source is unreachable, the UI degrades to a labeled cached/demo state, never a silent blank.
