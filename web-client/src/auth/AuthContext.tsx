import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api, storeToken, storedToken } from "../api/client";
import type { AuthResponse, Role, User } from "../api/types";

interface AuthState {
  user: User | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signInWith: (response: AuthResponse) => void;
  signOut: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // A token in localStorage survives a reload, but it may have expired meanwhile, so
  // the profile is re-fetched instead of trusting a cached copy of the user.
  useEffect(() => {
    if (!storedToken()) {
      setLoading(false);
      return;
    }
    api
      .me()
      .then(setUser)
      .catch(() => {
        storeToken(null);
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const response = await api.login(email, password);
    storeToken(response.accessToken);
    setUser(response.user);
  }, []);

  const signInWith = useCallback((response: AuthResponse) => {
    storeToken(response.accessToken);
    setUser(response.user);
  }, []);

  const signOut = useCallback(() => {
    storeToken(null);
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...roles: Role[]) => (user ? roles.includes(user.role) : false),
    [user],
  );

  const value = useMemo(
    () => ({ user, loading, signIn, signInWith, signOut, hasRole }),
    [user, loading, signIn, signInWith, signOut, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
