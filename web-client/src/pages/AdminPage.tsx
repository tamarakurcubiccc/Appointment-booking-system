import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { NotificationTemplate, User } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Badge, Card, Empty, Field } from "../components/ui";

export function AdminPage() {
  const { user } = useAuth();

  const [users, setUsers] = useState<User[]>([]);
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [editing, setEditing] = useState<NotificationTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [allUsers, allTemplates] = await Promise.all([api.users(), api.templates()]);
      setUsers(allUsers);
      setTemplates(allTemplates);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Greška pri učitavanju");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function toggleEnabled(target: User) {
    setError(null);
    setNotice(null);
    try {
      await api.setUserEnabled(target.id, !target.enabled);
      setNotice(`Pristup za ${target.email} je ${target.enabled ? "ukinut" : "vraćen"}.`);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Izmena nije uspela");
    }
  }

  async function saveTemplate(event: React.FormEvent) {
    event.preventDefault();
    if (!editing) {
      return;
    }
    setError(null);
    setNotice(null);
    try {
      await api.updateTemplate(editing.code, {
        description: editing.description ?? undefined,
        subject: editing.subject,
        body: editing.body,
        active: editing.active,
      });
      setNotice(`Šablon ${editing.code} je sačuvan.`);
      setEditing(null);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Čuvanje nije uspelo");
    }
  }

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">Administracija</h1>
      <p className="page-subtitle">Pristup korisnika i tekst obaveštenja koja sistem šalje.</p>

      {error ? <Alert kind="error">{error}</Alert> : null}
      {notice ? <Alert kind="ok">{notice}</Alert> : null}

      <Card title="Korisnici">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>E-mail</th>
                <th>Ime</th>
                <th>Uloga</th>
                <th>Stanje</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((row) => (
                <tr key={row.id}>
                  <td>{row.email}</td>
                  <td>{row.fullName}</td>
                  <td>
                    <Badge>{row.role}</Badge>
                  </td>
                  <td>
                    {!row.activated ? (
                      <Badge kind="warn">nije aktiviran</Badge>
                    ) : row.enabled ? (
                      <Badge kind="ok">aktivan</Badge>
                    ) : (
                      <Badge kind="danger">deaktiviran</Badge>
                    )}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {/* An admin cannot disable their own account - the service refuses it,
                        and offering the button would only invite a 400. */}
                    {row.id === user?.id ? (
                      <span className="small muted">vi</span>
                    ) : (
                      <button
                        className={row.enabled ? "danger" : "secondary"}
                        onClick={() => toggleEnabled(row)}
                      >
                        {row.enabled ? "Ukini pristup" : "Vrati pristup"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card title="Šabloni obaveštenja">
        {templates.length === 0 ? (
          <Empty>Nema šablona.</Empty>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Šifra</th>
                  <th>Naslov</th>
                  <th>Parametri</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {templates.map((template) => (
                  <tr key={template.id}>
                    <td className="small">{template.code}</td>
                    <td className="small">{template.subject}</td>
                    <td className="small muted">{template.parameters.join(", ") || "-"}</td>
                    <td>
                      <Badge kind={template.active ? "ok" : undefined}>
                        {template.active ? "aktivan" : "isključen"}
                      </Badge>
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <button className="secondary" onClick={() => setEditing(template)}>
                        Izmeni
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {editing ? (
        <Card title={`Izmena šablona - ${editing.code}`}>
          <form onSubmit={saveTemplate}>
            <Field label="Naslov">
              <input
                value={editing.subject}
                onChange={(e) => setEditing({ ...editing, subject: e.target.value })}
                required
                maxLength={200}
              />
            </Field>
            <Field label="Tekst">
              <textarea
                value={editing.body}
                onChange={(e) => setEditing({ ...editing, body: e.target.value })}
                required
                maxLength={4000}
                rows={10}
              />
            </Field>
            <Field label="Aktivan">
              <select
                value={editing.active ? "yes" : "no"}
                onChange={(e) => setEditing({ ...editing, active: e.target.value === "yes" })}
              >
                <option value="yes">Da - obaveštenje se šalje</option>
                <option value="no">Ne - obaveštenje se ne šalje</option>
              </select>
            </Field>
            <div className="row">
              <button type="submit">Sačuvaj</button>
              <button type="button" className="secondary" onClick={() => setEditing(null)}>
                Odustani
              </button>
            </div>
            <p className="small muted" style={{ marginTop: 10 }}>
              Parametri se pišu kao ime unutar dvostrukih vitičastih zagrada i popunjavaju se
              u trenutku slanja. Lista prepoznatih parametara se osvežava pri čuvanju.
            </p>
          </form>
        </Card>
      ) : null}
    </>
  );
}
