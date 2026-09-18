import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Department } from '../../employees/employee.service';
import { RiskAssessment, RiskTrendPoint, WorkforceRiskSummary } from '../risk.service';

interface SeverityBar {
  key: string;
  label: string;
  count: number;
  pct: number;
  cls: string;
}

interface TypeBar {
  key: string;
  label: string;
  count: number;
  pct: number;
}

interface DepartmentBar {
  departmentId: string;
  name: string;
  count: number;
  pct: number;
}

interface TrendBar {
  date: string;
  count: number;
  high: number;
  medium: number;
  low: number;
  pct: number;
}

@Component({
  selector: 'app-risk-charts',
  templateUrl: './risk-charts.component.html',
  styleUrls: ['./risk-charts.component.scss']
})
export class RiskChartsComponent {
  @Input() summary: WorkforceRiskSummary | null = null;
  @Input() assessments: RiskAssessment[] = [];
  @Input() departments: Department[] = [];
  @Input() trend: RiskTrendPoint[] = [];
  @Input() activeType: string | null = null;
  @Input() activeSeverity: string | null = null;
  @Input() activeDepartment: string | null = null;

  @Output() selectType = new EventEmitter<string>();
  @Output() selectSeverity = new EventEmitter<string>();
  @Output() selectDepartment = new EventEmitter<string>();

  severityBars(): SeverityBar[] {
    if (!this.summary) {
      return [];
    }
    const total = Math.max(
      1,
      this.summary.highCount + this.summary.mediumCount + this.summary.lowCount
    );
    return [
      { key: 'HIGH', label: 'High', count: this.summary.highCount, cls: 'bar--high' },
      { key: 'MEDIUM', label: 'Medium', count: this.summary.mediumCount, cls: 'bar--medium' },
      { key: 'LOW', label: 'Low', count: this.summary.lowCount, cls: 'bar--low' }
    ]
      .filter((bar) => bar.count > 0)
      .map((bar) => ({ ...bar, pct: (bar.count / total) * 100 }));
  }

  typeBars(): TypeBar[] {
    const map = this.summary?.risksByType ?? {};
    const entries = Object.entries(map).map(([key, count]) => ({
      key,
      label: this.humanType(key),
      count
    }));
    const max = Math.max(1, ...entries.map((entry) => entry.count));
    return entries
      .sort((a, b) => b.count - a.count)
      .map((entry) => ({ ...entry, pct: (entry.count / max) * 100 }));
  }

  departmentBars(): DepartmentBar[] {
    const byId = new Map<string, number>();
    for (const assessment of this.assessments) {
      if (!assessment.departmentId) {
        continue;
      }
      byId.set(assessment.departmentId, (byId.get(assessment.departmentId) ?? 0) + 1);
    }
    const entries = [...byId.entries()].map(([departmentId, count]) => ({
      departmentId,
      name: this.departments.find((department) => department.id === departmentId)?.name
        ?? 'Unknown department',
      count
    }));
    const max = Math.max(1, ...entries.map((entry) => entry.count));
    return entries
      .sort((a, b) => b.count - a.count)
      .map((entry) => ({ ...entry, pct: (entry.count / max) * 100 }));
  }

  trendBars(): TrendBar[] {
    const points = [...this.trend].slice(-14);
    const max = Math.max(1, ...points.map((point) => point.newAssessments));
    return points.map((point) => ({
      date: point.date,
      count: point.newAssessments,
      high: point.high,
      medium: point.medium,
      low: point.low,
      pct: (point.newAssessments / max) * 100
    }));
  }

  humanType(key: string): string {
    return key
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (character) => character.toUpperCase());
  }

  shortDate(isoDate: string): string {
    const [, month, day] = isoDate.split('-');
    return `${month}-${day}`;
  }
}