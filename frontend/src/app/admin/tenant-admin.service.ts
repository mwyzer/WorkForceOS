import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Organization {
  id: string;
  name: string;
  timezone: string;
  active: boolean;
  createdAt: string;
}

export interface SubscriptionPlan {
  code: string;
  name: string;
  maxSeats: number;
  monthlyPriceUsd: number;
  features: string[];
}

export interface TenantSubscription {
  organizationId: string;
  planCode: string;
  planName: string;
  billingStatus: string;
  seatLimit: number;
  seatsUsed: number;
  compliant: boolean;
  enabledFeatures: string[];
}

export interface ProvisionAccountPayload {
  username: string;
  password: string;
  organizationId: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class TenantAdminService {
  constructor(private readonly http: HttpClient) {}

  getCurrentOrganization(): Observable<Organization> {
    return this.http.get<Organization>('/api/v1/organizations/current');
  }

  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>('/api/v1/organizations');
  }

  createOrganization(name: string, timezone: string): Observable<Organization> {
    return this.http.post<Organization>('/api/v1/organizations', { name, timezone });
  }

  getPlans(): Observable<SubscriptionPlan[]> {
    return this.http.get<SubscriptionPlan[]>('/api/v1/organizations/plans');
  }

  getCurrentSubscription(): Observable<TenantSubscription> {
    return this.http.get<TenantSubscription>('/api/v1/organizations/current/subscription');
  }

  updateSubscription(orgId: string, planCode: string, billingStatus: string): Observable<TenantSubscription> {
    return this.http.put<TenantSubscription>(`/api/v1/organizations/${orgId}/subscription`, {
      planCode,
      billingStatus
    });
  }

  provisionAccount(payload: ProvisionAccountPayload): Observable<any> {
    return this.http.post('/api/v1/admin/accounts', payload);
  }
}
