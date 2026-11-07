import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { Alert, Card, Field } from "../components/ui";

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  const [token, setToken] = useState(params.get("token") ?? "");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.resetPassword(token, password);
      setDone(true);
      setTimeout(() => navigate("/login"), 1800);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Promena lozinke nije uspela");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-shell">
      <h1 className="page-title">Nova lozinka</h1>
      <Card>
        {done ? (
          <Alert kind="ok">Lozinka je promenjena. Vodimo vas na prijavu…</Alert>
        ) : (
          <>
            {error ? <Alert kind="error">{error}</Alert> : null}
            <form onSubmit={handleSubmit}>
              <Field label="Token iz e-maila">
                <input value={token} onChange={(e) => setToken(e.target.value)} required />
              </Field>
              <Field label="Nova lozinka (najmanje 8 karaktera)">
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </Field>
              <button type="submit" disabled={busy}>
                {busy ? "Čuvanje…" : "Postavi lozinku"}
              </button>
            </form>
            <p className="small muted" style={{ marginTop: 12 }}>
              Link važi jedan sat i može se iskoristiti samo jednom.
            </p>
          </>
        )}
      </Card>
      <p className="small muted">
        <Link to="/login">Nazad na prijavu</Link>
      </p>
    </div>
  );
}
