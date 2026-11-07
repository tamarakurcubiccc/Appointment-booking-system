import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Reservation } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Badge, Card, Empty, formatDateTime, statusBadge } from "../components/ui";

export function MyBookingsPage() {
  const { user } = useAuth();
  const provider = user?.role === "PROVIDER" || user?.role === "ADMIN";

  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const page = provider ? await api.providerReservations() : await api.myReservations();
      setReservations(page.content ?? []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Greška pri učitavanju");
    } finally {
      setLoading(false);
    }
  }, [provider]);

  useEffect(() => {
    void load();
  }, [load]);

  async function cancel(id: string) {
    setError(null);
    setNotice(null);
    try {
      await api.cancelReservation(id);
      setNotice("Termin je otkazan i vraćen u slobodne termine.");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Otkazivanje nije uspelo");
    }
  }

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">{provider ? "Zakazani termini" : "Moji termini"}</h1>
      <p className="page-subtitle">
        {provider
          ? "Termini koje su klijenti zakazali kod vas."
          : "Termini koje ste rezervisali, najnoviji prvi."}
      </p>

      {error ? <Alert kind="error">{error}</Alert> : null}
      {notice ? <Alert kind="ok">{notice}</Alert> : null}

      <Card>
        {reservations.length === 0 ? (
          <Empty>Nema termina.</Empty>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Usluga</th>
                  <th>Termin</th>
                  <th>Status</th>
                  <th>Nagrada</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {reservations.map((reservation) => (
                  <tr key={reservation.id}>
                    <td>{reservation.serviceName ?? "-"}</td>
                    <td>{formatDateTime(reservation.startTime)}</td>
                    <td>
                      <Badge kind={statusBadge(reservation.status)}>{reservation.status}</Badge>
                    </td>
                    <td className="small muted">{reservation.rewardId ? "iskorišćena" : "-"}</td>
                    <td style={{ textAlign: "right" }}>
                      {reservation.status === "CONFIRMED" ? (
                        <button className="danger" onClick={() => cancel(reservation.id)}>
                          Otkaži
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
