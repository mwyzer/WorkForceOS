import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
}

export interface RegisterTenantPayload {
  organizationName: string;
  timezone?: string;
  adminUsername: string;
  adminPassword: string;
}

export interface RegisterTenantResponse {
  organizationId: string;
  organizationName: string;
  username: string;
  accessToken: string;
  tokenType: string;
}

const TOKEN_KEY = 'workforceos.accessToken';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/v1/auth/login', { username, password })
      .pipe(tap((response) => localStorage.setItem(TOKEN_KEY, response.accessToken)));
  }

  registerTenant(payload: RegisterTenantPayload): Observable<RegisterTenantResponse> {
    return this.http
      .post<RegisterTenantResponse>('/api/v1/auth/register-tenant', payload)
      .pipe(tap((response) => localStorage.setItem(TOKEN_KEY, response.accessToken)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
  }

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.token;
  }
}
