import type { ReactNode } from "react";

export function Card({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <section className="card">
      {title ? <h2>{title}</h2> : null}
      {children}
    </section>
  );
}

export function Alert({ kind, children }: { kind: "error" | "ok" | "info"; children: ReactNode }) {
  return <div className={`alert ${kind}`}>{children}</div>;
}

export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
      {error ? <div className="field-error">{error}</div> : null}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}

export function Badge({ kind, children }: { kind?: "ok" | "warn" | "danger"; children: ReactNode }) {
  return <span className={kind ? `badge ${kind}` : "badge"}>{children}</span>;
}

/** Local time for a UTC instant from the API. */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("sr-RS", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatPrice(value: number): string {
  return `${new Intl.NumberFormat("sr-RS").format(value)} RSD`;
}

const RESERVATION_BADGES: Record<string, "ok" | "warn" | "danger" | undefined> = {
  CONFIRMED: "ok",
  COMPLETED: undefined,
  CANCELLED: "danger",
};

export function statusBadge(status: string) {
  return RESERVATION_BADGES[status];
}
