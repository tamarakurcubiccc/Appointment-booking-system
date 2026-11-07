import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { LoyaltyReward, LoyaltyRule, ServiceOffering, Slot } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Badge, Card, Empty, Field, formatDateTime, formatPrice } from "../components/ui";

export function BrowsePage() {
  const { user } = useAuth();

  const [services, setServices] = useState<ServiceOffering[]>([]);
  const [selected, setSelected] = useState<ServiceOffering | null>(null);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [rule, setRule] = useState<LoyaltyRule | null>(null);
  const [rewards, setRewards] = useState<LoyaltyReward[]>([]);

  const [slotId, setSlotId] = useState<string | null>(null);
  const [rewardId, setRewardId] = useState("");
  const [note, setNote] = useState("");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .services()
      .then(setServices)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Greška pri učitavanju"))
      .finally(() => setLoading(false));
  }, []);

  const openService = useCallback(
    async (service: ServiceOffering) => {
      setSelected(service);
      setSlotId(null);
      setRewardId("");
      setNote("");
      setError(null);
      setSuccess(null);
      setSlots([]);
      setRule(null);
      setRewards([]);

      try {
        const [free, rules] = await Promise.all([
          api.slots(service.id, "AVAILABLE"),
          api.loyaltyRules(service.providerId),
        ]);
        // Past slots are still AVAILABLE in the catalog; booking one is refused, so they
        // are filtered out here rather than offered and then rejected.
        setSlots(free.filter((slot) => new Date(slot.startTime) > new Date()));
        setRule(rules.find((candidate) => candidate.active) ?? null);

        if (user?.role === "CLIENT") {
          const mine = await api.myRewards();
          setRewards(mine.filter((reward) => reward.usable && reward.providerId === service.providerId));
        }
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Greška pri učitavanju termina");
      }
    },
    [user],
  );

  async function handleBook() {
    if (!selected || !slotId) {
      return;
    }
    setError(null);
    setSuccess(null);
    setBusy(true);
    try {
      const reservation = await api.book({
        serviceId: selected.id,
        slotId,
        note: note || undefined,
        rewardId: rewardId || undefined,
      });
      setSuccess(
        `Termin je rezervisan za ${formatDateTime(reservation.startTime)}. Potvrda je poslata na e-mail.`,
      );
      // The slot is gone for everyone now, so the list is refreshed from the server.
      await openService(selected);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Rezervacija nije uspela");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">Ponuda usluga</h1>
      <p className="page-subtitle">
        Pregled je javan. Za rezervaciju je potrebna prijava kao klijent.
      </p>

      {error ? <Alert kind="error">{error}</Alert> : null}
      {success ? <Alert kind="ok">{success}</Alert> : null}

      {services.length === 0 ? (
        <Card>
          <Empty>Trenutno nema aktivnih usluga.</Empty>
        </Card>
      ) : (
        <div className="grid">
          {services.map((service) => (
            <Card key={service.id}>
              <div className="row" style={{ justifyContent: "space-between" }}>
                <strong>{service.name}</strong>
                <Badge>{service.durationMinutes} min</Badge>
              </div>
              <p className="small muted" style={{ margin: "6px 0 10px" }}>
                {service.description || "Bez opisa"}
              </p>
              <div className="row" style={{ justifyContent: "space-between" }}>
                <span>{formatPrice(service.price)}</span>
                <button className="secondary" onClick={() => openService(service)}>
                  {selected?.id === service.id ? "Osveži termine" : "Slobodni termini"}
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {selected ? (
        <Card title={`Slobodni termini - ${selected.name}`}>
          {rule ? (
            <Alert kind="info">
              Program lojalnosti: svaki {rule.requiredAppointments}. termin donosi nagradu
              „{rule.rewardDescription}" (važi {rule.rewardValidityDays} dana).
            </Alert>
          ) : null}

          {slots.length === 0 ? (
            <Empty>Nema slobodnih termina u narednih 90 dana.</Empty>
          ) : (
            <div className="slot-list">
              {slots.map((slot) => (
                <button
                  key={slot.id}
                  className={`slot${slotId === slot.id ? " selected" : ""}`}
                  onClick={() => setSlotId(slot.id)}
                >
                  {formatDateTime(slot.startTime)}
                </button>
              ))}
            </div>
          )}

          {!user ? (
            <p className="small muted" style={{ marginTop: 14 }}>
              <Link to="/login">Prijavite se</Link> da biste rezervisali termin.
            </p>
          ) : user.role !== "CLIENT" ? (
            <p className="small muted" style={{ marginTop: 14 }}>
              Termine rezervišu samo klijenti.
            </p>
          ) : slots.length > 0 ? (
            <div style={{ marginTop: 18 }}>
              <Field label="Napomena (opciono)">
                <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} />
              </Field>

              {rewards.length > 0 ? (
                <Field label="Iskoristi nagradu lojalnosti">
                  <select value={rewardId} onChange={(e) => setRewardId(e.target.value)}>
                    <option value="">Bez nagrade</option>
                    {rewards.map((reward) => (
                      <option key={reward.id} value={reward.id}>
                        {reward.description} (važi do {formatDateTime(reward.expiresAt)})
                      </option>
                    ))}
                  </select>
                </Field>
              ) : null}

              <button onClick={handleBook} disabled={!slotId || busy}>
                {busy ? "Rezervacija…" : "Rezerviši termin"}
              </button>
            </div>
          ) : null}
        </Card>
      ) : null}
    </>
  );
}
