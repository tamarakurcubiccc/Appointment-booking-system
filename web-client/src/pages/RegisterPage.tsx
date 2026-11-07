import { useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { Alert, Card, Field } from "../components/ui";
import type { Role } from "../api/types";

export function RegisterPage() {
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
    phone: "",
    role: "CLIENT" as Role,
  });
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [registered, setRegistered] = useState(false);
  const [busy, setBusy] = useState(false);

  function update(key: keyof typeof form, value: string) {
    setForm((previous) => ({ ...previous, [key]: value }));
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setBusy(true);
    try {
      await api.register({
        email: form.email,
        password: form.password,
        fullName: form.fullName,
        phone: form.phone || undefined,
        role: form.role,
      });
      setRegistered(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
        setFieldErrors(err.fieldErrors ?? {});
      } else {
        setError("Registracija nije uspela");
      }
    } finally {
      setBusy(false);
    }
  }

  if (registered) {
    return (
      <div className="auth-shell">
        <h1 className="page-title">Proverite e-mail</h1>
        <Card>
          <Alert kind="ok">
            Poslali smo aktivacioni link na <strong>{form.email}</strong>. Nalog je potrebno
            aktivirati pre prve prijave.
          </Alert>
          <p className="small muted">
            U lokalnom okruženju sva pošta završava u Mailpit-u, na{" "}
            <a href="http://localhost:8025" target="_blank" rel="noreferrer">
              localhost:8025
            </a>
            .
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="auth-shell">
      <h1 className="page-title">Registracija</h1>
      <p className="page-subtitle">Nalog klijenta ili pružaoca usluge.</p>

      <Card>
        {error ? <Alert kind="error">{error}</Alert> : null}

        <form onSubmit={handleSubmit}>
          <Field label="Ime i prezime" error={fieldErrors.fullName}>
            <input value={form.fullName} onChange={(e) => update("fullName", e.target.value)} required />
          </Field>
          <Field label="E-mail" error={fieldErrors.email}>
            <input
              type="email"
              value={form.email}
              onChange={(e) => update("email", e.target.value)}
              required
              autoComplete="email"
            />
          </Field>
          <Field label="Lozinka (najmanje 8 karaktera)" error={fieldErrors.password}>
            <input
              type="password"
              value={form.password}
              onChange={(e) => update("password", e.target.value)}
              required
              minLength={8}
              autoComplete="new-password"
            />
          </Field>
          <Field label="Telefon (opciono)" error={fieldErrors.phone}>
            <input value={form.phone} onChange={(e) => update("phone", e.target.value)} />
          </Field>
          <Field label="Tip naloga">
            <select value={form.role} onChange={(e) => update("role", e.target.value)}>
              <option value="CLIENT">Klijent - zakazujem termine</option>
              <option value="PROVIDER">Pružalac usluge - nudim termine</option>
            </select>
          </Field>

          <button type="submit" disabled={busy}>
            {busy ? "Slanje…" : "Registruj se"}
          </button>
        </form>
      </Card>

      <p className="small muted">
        Već imate nalog? <Link to="/login">Prijavite se</Link>.
      </p>
    </div>
  );
}
