import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface DepartmentHeadcount {
  departmentName: string;
  employeeCount: number;
}

export interface EmployeeSummary {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  departmentName: string;
  teamName: string;
  active: boolean;
}

export interface DashboardSummary {
  totalEmployees: number;
  activeEmployees: number;
  totalDepartments: number;
  totalTeams: number;
  pendingLeaveRequests: number;
  pendingOvertimeRequests: number;
  departmentBreakdown: DepartmentHeadcount[];
  recentEmployees: EmployeeSummary[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>('/api/v1/dashboard/summary');
  }
}
