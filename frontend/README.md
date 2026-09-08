# PayFlow Frontend

Premium dark fintech UI for the PayFlow payment platform (React + Vite + TypeScript + Tailwind).

## Local development

```bash
cd frontend
npm install
npm run dev
```

App: [http://localhost:5173](http://localhost:5173)

API base URL defaults to `http://localhost:8080/api`. Override with:

```bash
cp .env.example .env
# edit VITE_API_BASE_URL
```

## Scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Vite dev server |
| `npm run build` | Typecheck + production build |
| `npm run preview` | Preview production build |
| `npm run lint` | TypeScript check (`tsc --noEmit`) |

## Demo login

Use the helper chips on `/login`, or:

- `admin@payflow.demo` / `PayFlowAdmin!2026`
- `merchant@payflow.demo` / `PayFlowMerchant!2026`
- `risk@payflow.demo` / `PayFlowRisk!2026`

## Docker

Built via the root `docker-compose` `frontend` service (nginx multi-stage). Standalone:

```bash
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080/api -t payflow-frontend .
docker run --rm -p 3000:80 payflow-frontend
```

## Structure

```
src/
  api/           REST modules (auth, dashboard, payments, …)
  components/ui  Design system (glass cards, charts wrappers, …)
  components/layout  Sidebar, Topbar, protected shell
  pages/         Landing, auth, checkout, dashboard domains
  lib/api.ts     Axios client + JWT refresh interceptor
  styles/        CSS variables + Tailwind layers
```

If the backend is offline, pages show empty/error states instead of crashing.
