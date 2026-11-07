import { useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Notification } from "../api/types";
import { Alert, Badge, Card, Empty, formatDateTime } from "../components/ui";

const STATUS_BADGE: Record<string, "ok" | "warn" | "danger" | undefined> = {
  SENT: "ok",
  PENDING: "warn",
  FAILED: "danger",
};

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [open, setOpen] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .myNotifications()
      .then((page) => setNotifications(page.content ?? []))
      .catch((err) => setError(err instanceof ApiError ? err.message : "Greška pri učitavanju"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">Obaveštenja</h1>
      <p className="page-subtitle">
        Sve što vam je sistem poslao, sa tekstom kakav je zaista otišao na e-mail.
      </p>

      {error ? <Alert kind="error">{error}</Alert> : null}

      <Card>
        {notifications.length === 0 ? (
          <Empty>Nema obaveštenja.</Empty>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              style={{ borderBottom: "1px solid var(--border)", padding: "10px 0" }}
            >
              <div className="row" style={{ justifyContent: "space-between" }}>
                <strong>{notification.subject}</strong>
                <Badge kind={STATUS_BADGE[notification.status]}>{notification.status}</Badge>
              </div>
              <div className="row small muted" style={{ marginTop: 2 }}>
                <span>{formatDateTime(notification.createdAt)}</span>
                <span>·</span>
                <span>{notification.templateCode}</span>
                <button className="link" onClick={() => setOpen(open === notification.id ? null : notification.id)}>
                  {open === notification.id ? "Sakrij" : "Prikaži tekst"}
                </button>
              </div>
              {open === notification.id ? <pre>{notification.body}</pre> : null}
            </div>
          ))
        )}
      </Card>
    </>
  );
}
