# Grand Lake Alert — v0.2.0

Android starter for a public-information dashboard focused on Grand Lake, New Brunswick, with a design that can expand across Atlantic Canada.

## What's new in v0.2.0
- Live current weather for the Grand Lake area using Open-Meteo.
- Official-source buttons for police, fire, ambulance/health, schools, buses, roads and fuel information.
- NB 511 is linked as the primary road/incident source. NB 511 also publishes a developer REST API for road conditions, cameras, events and advisories; a developer key is required for direct API integration.
- Existing public-information-only design retained.

## Planned next steps
1. Add a searchable Atlantic Canada area selector.
2. Add NB 511 API integration for road conditions, incidents and cameras when a developer key is supplied.
3. Add official weather warnings and alerts.
4. Add school closure/bus feeds by district where public feeds are available.
5. Add fuel-price data with source and timestamp.
6. Add optional push notifications for selected areas/categories.
7. Add source + last-updated timestamps to every card.

## Important
This app does not intercept, decode or rebroadcast encrypted emergency radio traffic, and it does not expose private 911 information. It is intended to aggregate information that is publicly available from official or otherwise authorized sources.

## Build
The GitHub Actions workflow builds a debug APK and publishes it as a workflow artifact.

Package: `ca.grandlake.alert`
Version: `0.2.0`
Min SDK: 26
Target SDK: 35
