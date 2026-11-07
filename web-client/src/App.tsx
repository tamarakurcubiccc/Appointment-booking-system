import { Navigate, Route, Routes } from "react-router-dom";
import type { ReactNode } from "react";
import { Layout } from "./components/Layout";
import { useAuth } from "./auth/AuthContext";
import type { Role } from "./api/types";
import { ActivatePage } from "./pages/ActivatePage";
import { AdminPage } from "./pages/AdminPage";
import { BrowsePage } from "./pages/BrowsePage";
import { LoginPage } from "./pages/LoginPage";
import { LoyaltyPage } from "./pages/LoyaltyPage";
import { MyBookingsPage } from "./pages/MyBookingsPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { ProviderPage } from "./pages/ProviderPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage";

function Protected({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="muted">Učitavanje…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<BrowsePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registracija" element={<RegisterPage />} />
        <Route path="/activate" element={<ActivatePage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route
          path="/rezervacije"
          element={
            <Protected>
              <MyBookingsPage />
            </Protected>
          }
        />
        <Route
          path="/lojalnost"
          element={
            <Protected>
              <LoyaltyPage />
            </Protected>
          }
        />
        <Route
          path="/obavestenja"
          element={
            <Protected>
              <NotificationsPage />
            </Protected>
          }
        />
        <Route
          path="/pruzalac"
          element={
            <Protected roles={["PROVIDER", "ADMIN"]}>
              <ProviderPage />
            </Protected>
          }
        />
        <Route
          path="/administracija"
          element={
            <Protected roles={["ADMIN"]}>
              <AdminPage />
            </Protected>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
