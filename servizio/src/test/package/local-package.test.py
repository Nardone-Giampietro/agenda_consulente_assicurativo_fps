"""HTTP verification inside an offline Debian container; credentials arrive on stdin.

Invoke with phase 'initial' or 'restart'. Never run against the developer's agenda.
The caller owns the container and removes its entire isolated data directory.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import sys
import time
import urllib.error
import urllib.request

settings = json.load(sys.stdin)
data = Path(os.environ["AGENDA_DATA_DIR"])
assert data.name.startswith("agenda_wa_test_")
assert str(data).startswith("/home/agenda/")
assert not Path("/usr/bin/java").exists()
base = "http://127.0.0.1:18080"
cookies = http.cookiejar.CookieJar()
http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))


def request(path, method="GET", body=None, expected=200, content_type=None):
    headers = {}
    if body is not None:
        if content_type is None:
            body = json.dumps(body).encode()
            content_type = "application/json"
        headers["Content-Type"] = content_type
    if method != "GET":
        token = request("/api/auth/csrf")
        headers[token["headerName"]] = token["token"]
    req = urllib.request.Request(base + path, data=body, method=method, headers=headers)
    try:
        with http.open(req, timeout=20) as response:
            code, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        code, raw = error.code, error.read()
    assert code == expected, (method, path, code, expected)
    return json.loads(raw) if raw else None


for attempt in range(120):
    try:
        http.open(base, timeout=1).close()
        break
    except (OSError, urllib.error.URLError):
        time.sleep(0.5)
else:
    raise AssertionError("The bundled launcher did not start the HTTP server")

request("/api/clienti", expected=401)
request("/api/auth/login", "POST", {"password": settings["password"]})
request("/api/auth/me")
clients = request("/api/clienti")
assert clients["total"] == settings["customers"]
policies = request("/api/cataloghi/polizze")
phase = sys.argv[1]
if phase == "initial":
    assert len(policies) == settings["policies"]
    assert request("/api/appuntamenti")["total"] == 0
    assert request("/api/callback")["total"] == 0
    outcomes = request("/api/cataloghi/esiti")
    appointment = request("/api/appuntamenti", "POST", {
        "data": "2026-10-13", "clientId": clients["items"][0]["id"],
        "esitoId": outcomes[0]["id"], "polizzaId": policies[0]["id"],
        "premio": 123.45, "note": "Verifica isolata pacchetto locale",
        "creaCallback": True, "dataCallback": "2026-10-14"
    }, expected=201)
    backup = request("/api/backup")
    assert len(backup["dati"]["appuntamenti"]) == 1
    assert len(backup["dati"]["callback"]) == 1
    request("/api/appuntamenti/" + appointment["id"], "DELETE", expected=204)
    assert request("/api/callback")["total"] == 1
    boundary = "agenda-wa-package-verification"
    body = ("--" + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="backup.json"\r\n'
            "Content-Type: application/json\r\n\r\n").encode()
    body += json.dumps(backup, ensure_ascii=False).encode() + ("\r\n--" + boundary + "--\r\n").encode()
    request("/api/backup/ripristino", "POST", body, content_type="multipart/form-data; boundary=" + boundary)
    request("/api/auth/me", expected=401)
    request("/api/auth/login", "POST", {"password": settings["password"]})
    request("/api/cataloghi/polizze/" + policies[-1]["id"], "DELETE", expected=204)
elif phase == "restart":
    assert len(policies) == settings["policies"] - 1
else:
    raise AssertionError("Unknown phase")
assert request("/api/appuntamenti")["total"] == 1
assert request("/api/callback")["total"] == 1
assert request("/api/appuntamenti/riepilogo")["premi"] == 123.45
assert (data / "agenda.mv.db").exists()
assert (data / "security/jwt.key").exists()
for attempt in range(30):
    status = request("/api/backup/stato")
    if status["copieAutomatiche"] > 0:
        break
    time.sleep(0.2)
assert status["copieAutomatiche"] > 0
assert list((data / "backups").glob("automatico-*.json"))
print("PASS:", phase, "offline login, seed, persistence, transactions, backup, restore, token revocation")
