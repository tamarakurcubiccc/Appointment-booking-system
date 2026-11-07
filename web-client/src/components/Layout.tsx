import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    signOut();
    navigate("/login");
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            Zakazivanje termina
          </NavLink>

          <nav className="nav">
            <NavLink to="/">Ponuda</NavLink>
            {user ? <NavLink to="/rezervacije">Moji termini</NavLink> : null}
            {user ? <NavLink to="/lojalnost">Lojalnost</NavLink> : null}
            {user ? <NavLink to="/obavestenja">Obaveštenja</NavLink> : null}
            {user?.role === "PROVIDER" || user?.role === "ADMIN" ? (
              <NavLink to="/pruzalac">Moja ponuda</NavLink>
            ) : null}
            {user?.role === "ADMIN" ? <NavLink to="/administracija">Administracija</NavLink> : null}
          </nav>

          <div className="topbar-user">
            {user ? (
              <>
                <span>
                  {user.fullName} · {user.role}
                </span>
                <button className="secondary" onClick={handleSignOut}>
                  Odjava
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login">Prijava</NavLink>
                <NavLink to="/registracija">Registracija</NavLink>
              </>
            )}
          </div>
        </div>
      </header>

      <main>
        <Outlet />
      </main>
    </>
  );
}
