import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Alert, Card, Field } from "../components/ui";

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    setBusy(true);
    try {
      await signIn(email, password);
      navigate("/");
    } catch (err) {
      // The service distinguishes "not activated" from "disabled" from "wrong password";
      // its message is more useful than anything invented here.
      setError(err instanceof ApiError ? err.message : "Prijava nije uspela");
    } finally {
      setBusy(false);
    }
  }

  async function handleForgotPassword() {
    if (!email) {
      setError("Unesite e-mail adresu, pa zatražite novu lozinku");
      return;
    }
    setError(null);
    try {
      await api.requestPasswordReset(email);
      setNotice("Ako nalog postoji, link za promenu lozinke je poslat na e-mail.");
    } catch {
      setNotice("Ako nalog postoji, link za promenu lozinke je poslat na e-mail.");
    }
  }

  return (
    <div className="auth-shell">
      <h1 className="page-title">Prijava</h1>
      <p className="page-subtitle">Ulogujte se da biste zakazivali termine.</p>

      <Card>
        {error ? <Alert kind="error">{error}</Alert> : null}
        {notice ? <Alert kind="info">{notice}</Alert> : null}

        <form onSubmit={handleSubmit}>
          <Field label="E-mail">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </Field>
          <Field label="Lozinka">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </Field>

          <div className="row">
            <button type="submit" disabled={busy}>
              {busy ? "Prijavljivanje…" : "Prijavi se"}
            </button>
            <button type="button" className="link" onClick={handleForgotPassword}>
              Zaboravljena lozinka?
            </button>
          </div>
        </form>
      </Card>

      <p className="small muted">
        Nemate nalog? <Link to="/registracija">Registrujte se</Link>.
      </p>
    </div>
  );
}
