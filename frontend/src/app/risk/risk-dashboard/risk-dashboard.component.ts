import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { Department, EmployeeService } from '../../employees/employee.service';
import {
  RiskAlert,
  RiskAssessment,
  RiskService,
  RiskTrendPoint,
  WorkforceRiskSummary
} from '../risk.service';

@Component({
  selector: 'app-risk-dashboard',
  templateUrl: './risk-dashboard.component.html',
  styleUrls: ['./risk-dashboard.component.scss']
})
export class RiskDashboardComponent implements OnInit {
  summary: WorkforceRiskSummary | null = null;
  alerts: RiskAlert[] = [];
  assessments: RiskAssessment[] = [];
  departments: Department[] = [];
  trend: RiskTrendPoint[] = [];

  severityFilter = 'ALL';
  typeFilter = 'ALL';
  departmentFilter = 'ALL';

  loading = true;
  analyzing = false;
  errorMessage: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly risk: RiskService,
    private readonly employees: EmployeeService
  ) {}

  ngOnInit(): void {
    this.loadSummary();
    this.loadReferenceData();
  }

  loadSummary(): void {
    this.loading = true;
    this.errorMessage = null;
    this.risk.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loadAssessments();
        this.loadAlerts();
      },
      error: () => {
        this.errorMessage = 'Could not load risk data. Try again.';
        this.loading = false;
      }
    });
  }

  loadReferenceData(): void {
    this.risk.getTrend().subscribe({
      next: (trend) => {
        this.trend = trend;
      },
      error: () => undefined
    });
    this.employees.getDepartments().subscribe({
      next: (departments) => {
        this.departments = departments;
      },
      error: () => undefined
    });
  }

  runAnalysis(): void {
    this.analyzing = true;
    this.errorMessage = null;
    this.risk.analyze().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.analyzing = false;
        this.loadAssessments();
        this.loadAlerts();
        this.loadReferenceData();
      },
      error: () => {
        this.analyzing = false;
        this.errorMessage = 'Analysis failed. Check that the backend is running.';
      }
    });
  }

  loadAssessments(): void {
    this.risk.getAssessments().subscribe({
      next: (assessments) => {
        this.assessments = assessments;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadAlerts(): void {
    this.risk.getAlerts().subscribe({
      next: (alerts) => {
        this.alerts = alerts;
      },
      error: () => {
        this.errorMessage = 'Could not load risk alerts.';
      }
    });
  }

  resolveAlert(alert: RiskAlert): void {
    this.risk.resolveAlert(alert.id).subscribe({
      next: (resolved) => {
        const index = this.alerts.findIndex((candidate) => candidate.id === resolved.id);
        if (index >= 0) {
          this.alerts[index] = resolved;
        }
      },
      error: () => {
        this.errorMessage = 'Could not resolve the alert.';
      }
    });
  }

  toggleSeverity(severity: string): void {
    this.severityFilter = this.severityFilter === severity ? 'ALL' : severity;
  }

  toggleType(type: string): void {
    this.typeFilter = this.typeFilter === type ? 'ALL' : type;
  }

  toggleDepartment(departmentId: string): void {
    this.departmentFilter = this.departmentFilter === departmentId ? 'ALL' : departmentId;
  }

  resetFilters(): void {
    this.severityFilter = 'ALL';
    this.typeFilter = 'ALL';
    this.departmentFilter = 'ALL';
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

  gaugeOffset(riskIndex: number): number {
    const circumference = Math.PI * 50;
    const fraction = Math.min(100, Math.max(0, riskIndex)) / 100;
    return circumference * (1 - fraction);
  }

  severityClass(severity: string): string {
    return 'badge badge--' + severity.toLowerCase();
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}