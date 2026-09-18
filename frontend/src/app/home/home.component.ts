import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { DashboardService, DashboardSummary } from '../dashboard/dashboard.service';
import { RiskService, WorkforceRiskSummary } from '../risk/risk.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  summary: DashboardSummary | null = null;
  riskSummary: WorkforceRiskSummary | null = null;
  loading = true;
  errorMessage: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly dashboard: DashboardService,
    private readonly risk: RiskService
  ) {}

  ngOnInit(): void {
    this.dashboard.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load dashboard data. Try refreshing.';
        this.loading = false;
      }
    });
    this.risk.getSummary().subscribe({
      next: (riskSummary) => {
        this.riskSummary = riskSummary;
      },
      error: () => undefined
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  goToRisk(): void {
    this.router.navigateByUrl('/risk');
  }

  goToEmployees(): void {
    this.router.navigateByUrl('/employees');
  }

  departmentBarWidth(count: number): number {
    if (!this.summary || this.summary.departmentBreakdown.length === 0) {
      return 0;
    }
    const max = Math.max(...this.summary.departmentBreakdown.map((dept) => dept.employeeCount));
    return max === 0 ? 0 : (count / max) * 100;
  }

  departmentShare(count: number): number {
    if (!this.summary || this.summary.totalEmployees === 0) {
      return 0;
    }
    return Math.round((count / this.summary.totalEmployees) * 100);
  }

  riskTimer(riskIndex: number): string {
    if (riskIndex >= 80) {
      return 'danger';
    }
    if (riskIndex >= 50) {
      return 'amber';
    }
    return 'safe';
  }

  riskValueColor(riskIndex: number): string {
    switch (this.riskTimer(riskIndex)) {
      case 'danger':
        return '#d9463f';
      case 'amber':
        return '#c97f0b';
      default:
        return '#2f7d4f';
    }
  }
}