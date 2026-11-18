import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import {
  Organization,
  SubscriptionPlan,
  TenantAdminService,
  TenantSubscription
} from './tenant-admin.service';

@Component({
  selector: 'app-tenant-admin',
  templateUrl: './tenant-admin.component.html',
  styleUrls: ['./tenant-admin.component.scss']
})
export class TenantAdminComponent implements OnInit {
  currentOrg: Organization | null = null;
  organizations: Organization[] = [];
  plans: SubscriptionPlan[] = [];
  subscription: TenantSubscription | null = null;

  newOrgName: string = '';
  newOrgTimezone: string = 'UTC';

  acctUsername: string = '';
  acctPassword: string = '';
  acctRole: string = 'MANAGER';

  statusMessage: string = '';
  isError: boolean = false;

  constructor(
    private readonly adminService: TenantAdminService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.adminService.getCurrentOrganization().subscribe({
      next: (org) => (this.currentOrg = org)
    });

    this.adminService.getPlans().subscribe({
      next: (plans) => (this.plans = plans)
    });

    this.adminService.getCurrentSubscription().subscribe({
      next: (sub) => (this.subscription = sub)
    });

    this.adminService.getOrganizations().subscribe({
      next: (orgs) => (this.organizations = orgs),
      error: () => (this.organizations = [])
    });
  }

  selectPlan(planCode: string): void {
    if (!this.currentOrg) return;
    this.adminService.updateSubscription(this.currentOrg.id, planCode, 'ACTIVE').subscribe({
      next: (sub) => {
        this.subscription = sub;
        this.notify(`Switched plan to ${sub.planName}`, false);
      },
      error: (err) => this.notify(err?.error?.message || 'Failed to update subscription', true)
    });
  }

  createOrg(): void {
    const name = this.newOrgName.trim();
    if (!name) return;
    this.adminService.createOrganization(name, this.newOrgTimezone).subscribe({
      next: (org) => {
        this.organizations.push(org);
        this.newOrgName = '';
        this.notify(`Created organization "${org.name}"`, false);
      },
      error: (err) => this.notify(err?.error?.message || 'Failed to create organization', true)
    });
  }

  provisionUser(): void {
    if (!this.currentOrg || !this.acctUsername.trim() || !this.acctPassword.trim()) return;
    this.adminService
      .provisionAccount({
        username: this.acctUsername.trim(),
        password: this.acctPassword.trim(),
        organizationId: this.currentOrg.id,
        roles: [this.acctRole]
      })
      .subscribe({
        next: () => {
          this.notify(`Provisioned account "${this.acctUsername}" (${this.acctRole})`, false);
          this.acctUsername = '';
          this.acctPassword = '';
        },
        error: (err) => this.notify(err?.error?.message || 'Failed to provision account', true)
      });
  }

  private notify(msg: string, isError: boolean): void {
    this.statusMessage = msg;
    this.isError = isError;
    setTimeout(() => (this.statusMessage = ''), 5000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
