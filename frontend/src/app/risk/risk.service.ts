import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type RiskSeverity = 'HIGH' | 'MEDIUM' | 'LOW';
export type RiskType =
  | 'COVERAGE_SHORTFALL'
  | 'STAFFING_LIQUIDITY'
  | 'ATTENDANCE_TREND'
  | 'OVERTIME_DEPENDENCY'
  | 'SINGLE_POINT_OF_FAILURE';
export type RiskAlertStatus = 'OPEN' | 'RESOLVED';

export interface RiskAssessment {
  id: string;
  type: RiskType;
  severity: RiskSeverity;
  score: number;
  entityType: string;
  entityId: string;
  teamId: string | null;
  departmentId: string | null;
  windowStart: string;
  windowEnd: string;
  impactMinutes: number;
  summary: string;
  evidence: string[];
}

export interface RiskRecommendation {
  riskType: string;
  title: string;
  description: string;
  actionType: string;
  actionEndpoint: string;
}

export interface RiskAlert {
  id: string;
  assessmentId: string;
  severity: RiskSeverity;
  status: RiskAlertStatus;
  summary: string;
  createdAt: string;
  resolvedAt: string | null;
}

export interface RiskTrendPoint {
  date: string;
  newAssessments: number;
  high: number;
  medium: number;
  low: number;
}

export interface WorkforceRiskSummary {
  riskIndex: number;
  coveragePercentage: number;
  totalAssessments: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  openAlerts: number;
  latestAnalysis: string;
  advisorMode: string;
  risksByType: Record<string, number>;
  topAssessments: RiskAssessment[];
}

@Injectable({ providedIn: 'root' })
export class RiskService {
  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<WorkforceRiskSummary> {
    return this.http.get<WorkforceRiskSummary>('/api/v1/risk/summary');
  }

  analyze(): Observable<WorkforceRiskSummary> {
    return this.http.post<WorkforceRiskSummary>('/api/v1/risk/analyze', null);
  }

  getAssessments(): Observable<RiskAssessment[]> {
    return this.http.get<RiskAssessment[]>('/api/v1/risk/assessments');
  }

  getRecommendations(assessmentId: string): Observable<RiskRecommendation[]> {
    return this.http.get<RiskRecommendation[]>(`/api/v1/risk/assessments/${assessmentId}/recommendations`);
  }

  getAlerts(): Observable<RiskAlert[]> {
    return this.http.get<RiskAlert[]>('/api/v1/risk/alerts');
  }

  getTrend(): Observable<RiskTrendPoint[]> {
    return this.http.get<RiskTrendPoint[]>('/api/v1/risk/trend');
  }

  resolveAlert(alertId: string): Observable<RiskAlert> {
    return this.http.post<RiskAlert>(`/api/v1/risk/alerts/${alertId}/resolve`, null);
  }
}