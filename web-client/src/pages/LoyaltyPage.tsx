import { useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { LoyaltyAccount, LoyaltyReward, ServiceOffering } from "../api/types";
import { Alert, Badge, Card, Empty, formatDateTime } from "../components/ui";

const REWARD_BADGE: Record<string, "ok" | "warn" | "danger" | undefined> = {
  AVAILABLE: "ok",
  REDEEMED: undefined,
  EXPIRED: "danger",
};

export function LoyaltyPage() {
  const [accounts, setAccounts] = useState<LoyaltyAccount[]>([]);
  const [rewards, setRewards] = useState<LoyaltyReward[]>([]);
  const [services, setServices] = useState<ServiceOffering[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([api.myLoyaltyAccounts(), api.myRewards(), api.services()])
      .then(([myAccounts, myRewards, allServices]) => {
        setAccounts(myAccounts);
        setRewards(myRewards);
        setServices(allServices);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : "Greška pri učitavanju"))
      .finally(() => setLoading(false));
  }, []);

  function providerLabel(providerId: string): string {
    const offering = services.find((service) => service.providerId === providerId);
    return offering ? offering.name : providerId.slice(0, 8);
  }

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">Lojalnost</h1>
      <p className="page-subtitle">
        Brojači rastu iz događaja o rezervacijama, pa se osvežavaju sekund-dva nakon zakazivanja.
      </p>

      {error ? <Alert kind="error">{error}</Alert> : null}

      <Card title="Napredak po pružaocu usluge">
        {accounts.length === 0 ? (
          <Empty>Još nema zakazanih termina koji se broje.</Empty>
        ) : (
          accounts.map((account) => {
            const required = account.requiredAppointments;
            const percent = required
              ? Math.min(100, Math.round((account.appointmentsTowardsReward / required) * 100))
              : 0;

            return (
              <div key={account.providerId} style={{ marginBottom: 16 }}>
                <div className="row" style={{ justifyContent: "space-between" }}>
                  <strong>{providerLabel(account.providerId)}</strong>
                  <span className="small muted">ukupno termina: {account.totalAppointments}</span>
                </div>
                {required ? (
                  <>
                    <div className="progress">
                      <span style={{ width: `${percent}%` }} />
                    </div>
                    <span className="small muted">
                      {account.appointmentsTowardsReward} / {required} - još{" "}
                      {account.appointmentsUntilReward} do nagrade
                    </span>
                  </>
                ) : (
                  <span className="small muted">
                    Pružalac trenutno nema aktivno pravilo lojalnosti.
                  </span>
                )}
              </div>
            );
          })
        )}
      </Card>

      <Card title="Nagrade">
        {rewards.length === 0 ? (
          <Empty>Još nema dodeljenih nagrada.</Empty>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Nagrada</th>
                  <th>Pružalac</th>
                  <th>Status</th>
                  <th>Važi do</th>
                </tr>
              </thead>
              <tbody>
                {rewards.map((reward) => (
                  <tr key={reward.id}>
                    <td>{reward.description}</td>
                    <td className="small muted">{providerLabel(reward.providerId)}</td>
                    <td>
                      <Badge kind={REWARD_BADGE[reward.status]}>{reward.status}</Badge>
                    </td>
                    <td className="small">{formatDateTime(reward.expiresAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="small muted" style={{ marginTop: 10 }}>
          Slobodnu nagradu iskoristite na stranici „Ponuda", pri rezervaciji termina.
        </p>
      </Card>
    </>
  );
}
