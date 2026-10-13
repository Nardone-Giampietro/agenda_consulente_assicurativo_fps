let csrf, pendingCsrf;
let generation = 0;
let requests = new AbortController();
export function resetSession() {
  generation++;
  requests.abort();
  requests = new AbortController();
  csrf = pendingCsrf = null;
}
async function csrfToken(signal) {
  const version = generation;
  if (csrf) return csrf;
  if (!pendingCsrf) {
    const pending = fetch("/api/auth/csrf", { credentials: "same-origin", signal })
      .then(async (response) => {
        if (!response.ok) throw new Error("Accesso non disponibile. Riprova.");
        return response.json();
      });
    pendingCsrf = pending;
    try {
      const token = await pending;
      if (version === generation) csrf = token;
    }
    finally { if (pendingCsrf === pending) pendingCsrf = null; }
  }
  return csrf || pendingCsrf;
}
export async function api(path, options = {}) {
  const version = generation;
  const signal = requests.signal;
  const method = options.method || "GET";
  const token = ["GET", "HEAD", "OPTIONS"].includes(method) ? null : await csrfToken(signal);
  if (version !== generation) throw new DOMException("Sessione cambiata", "AbortError");
  const multipart = options.body instanceof FormData;
  const response = await fetch(`/api/${path}`, {
    ...options,
    method,
    credentials: "same-origin",
    signal,
    headers: {
      Accept: "application/json",
      ...(options.body && !multipart ? { "Content-Type": "application/json" } : {}),
      ...(token ? { [token.headerName]: token.token } : {}),
      ...options.headers,
    },
    body: multipart ? options.body : options.body ? JSON.stringify(options.body) : undefined,
  });
  const data = response.status === 204 ? null : options.responseType === "blob" && response.ok ? await response.blob() : await response.json().catch(() => ({}));
  if (version !== generation) throw new DOMException("Sessione cambiata", "AbortError");
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith("auth/"))
      window.dispatchEvent(new Event("auth-expired"));
    const error = new Error(data?.detail || "Non è stato possibile completare l’operazione. Riprova.");
    error.status = response.status;
    throw error;
  }
  return data;
}
export function query(values) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== "" && value != null) params.set(key, value);
  });
  return params.toString();
}
export async function all(path, filters = {}) {
  const version = generation;
  const items = [];
  for (let page = 0; ; page++) {
    if (version !== generation) throw new DOMException("Sessione cambiata", "AbortError");
    const result = await api(`${path}?${query({ ...filters, size: 100, page })}`);
    items.push(...result.items);
    if (items.length >= result.total || !result.items.length) return items;
  }
}
