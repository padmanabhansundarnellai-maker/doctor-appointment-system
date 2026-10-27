const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

async function req(path, method = "GET", body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.error || "Request failed");
  return data;
}

export const api = {
  get: (p) => req(p),
  post: (p, b) => req(p, "POST", b),
  put: (p, b) => req(p, "PUT", b),
  del: (p) => req(p, "DELETE"),
};
