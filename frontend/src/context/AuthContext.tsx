import React, { createContext, useContext, useState } from 'react';
import type { Merchant } from '../types';

interface AuthContextType {
  token: string | null;
  merchant: Merchant | null;
  login: (token: string, merchant: Merchant) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('payflow_token'));
  const [merchant, setMerchant] = useState<Merchant | null>(() => {
    const saved = localStorage.getItem('payflow_merchant');
    return saved ? JSON.parse(saved) : null;
  });

  const login = (newToken: string, newMerchant: Merchant) => {
    localStorage.setItem('payflow_token', newToken);
    localStorage.setItem('payflow_merchant', JSON.stringify(newMerchant));
    setToken(newToken);
    setMerchant(newMerchant);
  };

  const logout = () => {
    localStorage.removeItem('payflow_token');
    localStorage.removeItem('payflow_merchant');
    setToken(null);
    setMerchant(null);
  };

  return (
    <AuthContext.Provider value={{ token, merchant, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
