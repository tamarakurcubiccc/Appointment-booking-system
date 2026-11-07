import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Alert, Card } from "../components/ui";

/**
 * Target of the activation link in the e-mail. The token is single-use, so the request
 * is fired exactly once even though React's development mode mounts twice.
 */
export function ActivatePage() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const { signInWith } = useAuth();
  const navigate = useNavigate();

  const attempted = useRef(false);
  const [state, setState] = useState<"working" | "done" | "failed">("working");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setState("failed");
      setError("Link nije valjan - nema tokena.");
      return;
    }
    if (attempted.current) {
      return;
    }
    attempted.current = true;

    api
      .activate(token)
      .then((response) => {
        signInWith(response);
        setState("done");
        setTimeout(() => navigate("/"), 1500);
      })
      .catch((err) => {
        setState("failed");
        setError(err instanceof ApiError ? err.message : "Aktivacija nije uspela");
      });
  }, [token, signInWith, navigate]);

  return (
    <div className="auth-shell">
      <h1 className="page-title">Aktivacija naloga</h1>
      <Card>
        {state === "working" ? <p className="muted">Aktiviramo nalog…</p> : null}
        {state === "done" ? (
          <Alert kind="ok">Nalog je aktiviran i prijavljeni ste. Vodimo vas na ponudu…</Alert>
        ) : null}
        {state === "failed" ? (
          <>
            <Alert kind="error">{error}</Alert>
            <p className="small muted">
              Aktivacioni link važi 24 sata i može se iskoristiti samo jednom. Ako je istekao,
              registrujte se ponovo ili se obratite administratoru.
            </p>
            <Link to="/login">Nazad na prijavu</Link>
          </>
        ) : null}
      </Card>
    </div>
  );
}
