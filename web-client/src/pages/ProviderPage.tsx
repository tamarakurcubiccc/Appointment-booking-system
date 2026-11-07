import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { LoyaltyAccount, LoyaltyRule, ServiceOffering } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Alert, Badge, Card, Empty, Field, formatPrice } from "../components/ui";

function today(offsetDays = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

export function ProviderPage() {
  const { user } = useAuth();

  const [services, setServices] = useState<ServiceOffering[]>([]);
  const [rules, setRules] = useState<LoyaltyRule[]>([]);
  const [accounts, setAccounts] = useState<LoyaltyAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [service, setService] = useState({ name: "", description: "", durationMinutes: 30, price: 1500 });
  const [generate, setGenerate] = useState({
    serviceId: "",
    fromDate: today(1),
    toDate: today(5),
    workDayStart: "09:00:00",
    workDayEnd: "17:00:00",
    skipWeekend: true,
  });
  const [rule, setRule] = useState({ requiredAppointments: 5, rewardDescription: "Besplatan termin", rewardValidityDays: 90 });

  const load = useCallback(async () => {
    if (!user) {
      return;
    }
    setError(null);
    try {
      const [mine, myRules, myAccounts] = await Promise.all([
        api.services(user.id),
        api.loyaltyRules(user.id),
        api.providerLoyaltyAccounts(),
      ]);
      setServices(mine);
      setRules(myRules);
      setAccounts(myAccounts);
      setGenerate((previous) => ({
        ...previous,
        serviceId: previous.serviceId || mine[0]?.id || "",
      }));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Greška pri učitavanju");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    void load();
  }, [load]);

  async function createService(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      await api.createService({
        name: service.name,
        description: service.description || undefined,
        durationMinutes: Number(service.durationMinutes),
        price: Number(service.price),
      });
      setNotice("Usluga je kreirana.");
      setService({ name: "", description: "", durationMinutes: 30, price: 1500 });
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Kreiranje nije uspelo");
    }
  }

  async function generateSlots(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      const created = await api.generateSlots(generate.serviceId, {
        fromDate: generate.fromDate,
        toDate: generate.toDate,
        workDayStart: generate.workDayStart,
        workDayEnd: generate.workDayEnd,
        skipWeekend: generate.skipWeekend,
        zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      // Generation is idempotent, so a second run over the same range creates nothing.
      setNotice(
        created.length === 0
          ? "Nije kreiran nijedan nov termin - svi iz tog intervala već postoje."
          : `Kreirano ${created.length} termina.`,
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Generisanje nije uspelo");
    }
  }

  async function saveRule(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      await api.createLoyaltyRule({
        requiredAppointments: Number(rule.requiredAppointments),
        rewardDescription: rule.rewardDescription,
        rewardValidityDays: Number(rule.rewardValidityDays),
      });
      setNotice("Pravilo lojalnosti je aktivno.");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Čuvanje pravila nije uspelo");
    }
  }

  async function deactivateRule(id: string) {
    setError(null);
    try {
      await api.deactivateLoyaltyRule(id);
      setNotice("Pravilo je deaktivirano.");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Deaktiviranje nije uspelo");
    }
  }

  const activeRule = rules.find((candidate) => candidate.active) ?? null;

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }

  return (
    <>
      <h1 className="page-title">Moja ponuda</h1>
      <p className="page-subtitle">Usluge, slobodni termini i program lojalnosti.</p>

      {error ? <Alert kind="error">{error}</Alert> : null}
      {notice ? <Alert kind="ok">{notice}</Alert> : null}

      <Card title="Moje usluge">
        {services.length === 0 ? (
          <Empty>Još nemate kreiranih usluga.</Empty>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Naziv</th>
                  <th>Trajanje</th>
                  <th>Cena</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {services.map((offering) => (
                  <tr key={offering.id}>
                    <td>{offering.name}</td>
                    <td>{offering.durationMinutes} min</td>
                    <td>{formatPrice(offering.price)}</td>
                    <td>
                      <Badge kind={offering.active ? "ok" : undefined}>
                        {offering.active ? "aktivna" : "neaktivna"}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid">
        <Card title="Nova usluga">
          <form onSubmit={createService}>
            <Field label="Naziv">
              <input
                value={service.name}
                onChange={(e) => setService({ ...service, name: e.target.value })}
                required
                maxLength={140}
              />
            </Field>
            <Field label="Opis (opciono)">
              <input
                value={service.description}
                onChange={(e) => setService({ ...service, description: e.target.value })}
                maxLength={1000}
              />
            </Field>
            <div className="row">
              <Field label="Trajanje (min)">
                <input
                  type="number"
                  min={5}
                  max={600}
                  value={service.durationMinutes}
                  onChange={(e) => setService({ ...service, durationMinutes: Number(e.target.value) })}
                  required
                />
              </Field>
              <Field label="Cena (RSD)">
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={service.price}
                  onChange={(e) => setService({ ...service, price: Number(e.target.value) })}
                  required
                />
              </Field>
            </div>
            <button type="submit">Kreiraj uslugu</button>
          </form>
        </Card>

        <Card title="Generisanje termina">
          {services.length === 0 ? (
            <Empty>Prvo kreirajte uslugu.</Empty>
          ) : (
            <form onSubmit={generateSlots}>
              <Field label="Usluga">
                <select
                  value={generate.serviceId}
                  onChange={(e) => setGenerate({ ...generate, serviceId: e.target.value })}
                  required
                >
                  {services.map((offering) => (
                    <option key={offering.id} value={offering.id}>
                      {offering.name}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="row">
                <Field label="Od datuma">
                  <input
                    type="date"
                    value={generate.fromDate}
                    onChange={(e) => setGenerate({ ...generate, fromDate: e.target.value })}
                    required
                  />
                </Field>
                <Field label="Do datuma">
                  <input
                    type="date"
                    value={generate.toDate}
                    onChange={(e) => setGenerate({ ...generate, toDate: e.target.value })}
                    required
                  />
                </Field>
              </div>
              <div className="row">
                <Field label="Radno vreme od">
                  <input
                    type="time"
                    step={1}
                    value={generate.workDayStart.slice(0, 5)}
                    onChange={(e) => setGenerate({ ...generate, workDayStart: `${e.target.value}:00` })}
                    required
                  />
                </Field>
                <Field label="Radno vreme do">
                  <input
                    type="time"
                    step={1}
                    value={generate.workDayEnd.slice(0, 5)}
                    onChange={(e) => setGenerate({ ...generate, workDayEnd: `${e.target.value}:00` })}
                    required
                  />
                </Field>
              </div>
              <Field label="Preskoči vikend">
                <select
                  value={generate.skipWeekend ? "yes" : "no"}
                  onChange={(e) => setGenerate({ ...generate, skipWeekend: e.target.value === "yes" })}
                >
                  <option value="yes">Da</option>
                  <option value="no">Ne</option>
                </select>
              </Field>
              <button type="submit">Generiši termine</button>
            </form>
          )}
        </Card>
      </div>

      <Card title="Program lojalnosti">
        {activeRule ? (
          <>
            <Alert kind="info">
              Aktivno pravilo: svaki {activeRule.requiredAppointments}. termin donosi
              „{activeRule.rewardDescription}", nagrada važi {activeRule.rewardValidityDays} dana.
            </Alert>
            <button className="danger" onClick={() => deactivateRule(activeRule.id)}>
              Deaktiviraj pravilo
            </button>
            <p className="small muted" style={{ marginTop: 10 }}>
              U jednom trenutku može biti aktivno samo jedno pravilo - deaktivirajte ovo da
              biste postavili drugačiji prag.
            </p>
          </>
        ) : (
          <form onSubmit={saveRule}>
            <div className="row">
              <Field label="Svaki koji termin se nagrađuje">
                <input
                  type="number"
                  min={2}
                  max={100}
                  value={rule.requiredAppointments}
                  onChange={(e) => setRule({ ...rule, requiredAppointments: Number(e.target.value) })}
                  required
                />
              </Field>
              <Field label="Nagrada važi (dana)">
                <input
                  type="number"
                  min={1}
                  max={730}
                  value={rule.rewardValidityDays}
                  onChange={(e) => setRule({ ...rule, rewardValidityDays: Number(e.target.value) })}
                  required
                />
              </Field>
            </div>
            <Field label="Opis nagrade">
              <input
                value={rule.rewardDescription}
                onChange={(e) => setRule({ ...rule, rewardDescription: e.target.value })}
                required
                maxLength={200}
              />
            </Field>
            <button type="submit">Aktiviraj pravilo</button>
          </form>
        )}
      </Card>

      <Card title="Verni klijenti">
        {accounts.length === 0 ? (
          <Empty>Još nema klijenata sa zakazanim terminima.</Empty>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Klijent</th>
                  <th>Ukupno termina</th>
                  <th>Ka nagradi</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((account) => (
                  <tr key={account.clientId}>
                    <td className="small muted">{account.clientId.slice(0, 8)}…</td>
                    <td>{account.totalAppointments}</td>
                    <td>
                      {account.requiredAppointments
                        ? `${account.appointmentsTowardsReward} / ${account.requiredAppointments}`
                        : "-"}
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
