import api from './api';
import { AuthResponse, User } from '../types';

export const authService = {
  login: async (username: string, password: string):Promise<AuthResponse> => {
    const res = await api.post('/auth/login', { username, password });
    return res.data.data;
  },

  register: async (username: string, email: string, password: string): Promise<AuthResponse> => {
    const res = await api.post('/auth/register', { username, email, password });
    return res.data.data;
  },

  getCurrentUser: async (): Promise<User> => {
    const res = await api.get('/auth/me');
    return res.data.data;
  },
};
