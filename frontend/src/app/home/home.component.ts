import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { DashboardService, DashboardSummary } from '../dashboard/dashboard.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  summary: DashboardSummary | null = null;
  loading = true;
  errorMessage: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly dashboard: DashboardService
  ) {}

  ngOnInit(): void {
    this.dashboard.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = 'Could not load dashboard data. Try refreshing.';
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  departmentBarWidth(count: number): number {
    if (!this.summary || this.summary.departmentBreakdown.length === 0) {
      return 0;
    }
    const max = Math.max(...this.summary.departmentBreakdown.map((dept) => dept.employeeCount));
    return max === 0 ? 0 : (count / max) * 100;
  }
}
