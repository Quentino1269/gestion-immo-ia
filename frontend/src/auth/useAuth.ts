import { useContext } from 'react';
import { AuthContext, type AuthContextValeur } from './AuthContext';

export function useAuth(): AuthContextValeur {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth doit être appelé sous un <AuthProvider>.');
  }
  return ctx;
}