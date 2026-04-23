# SmarTown Frontend

Vue-Frontend fuer das SmarTown-Dashboard.

## Lokale Entwicklung

```sh
npm ci
npm run dev
```

Der lokale Dev-Server laeuft standardmaessig unter:

```text
http://localhost:5173
```

## Docker

Im Docker-Workflow ist das Frontend ein eigener Container. Das gebaute Vue-Frontend wird von Nginx ausgeliefert.

Port-Konfiguration:

```env
FRONTEND_PORT=8081
```

Compose mappt `FRONTEND_PORT` auf den internen Nginx-Port `80`.

## Build

```sh
npm run build
```
