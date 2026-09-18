import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Department } from '../../employees/employee.service';
import { RiskAssessment, RiskRecommendation, RiskService } from '../risk.service';

type SortKey = 'type' | 'severity' | 'score' | 'impactMinutes';

@Component({
  selector: 'app-risk-assessments',
  templateUrl: './risk-assessments.component.html',
  styleUrls: ['./risk-assessments.component.scss']
})
export class RiskAssessmentsComponent {
  @Input() assessments: RiskAssessment[] = [];
  @Input() departments: Department[] = [];
  @Input() risksByType: Record<string, number> = {};
  @Input() severityFilter = 'ALL';
  @Input() typeFilter = 'ALL';
  @Input() departmentFilter = 'ALL';

  @Output() selectSeverity = new EventEmitter<string>();
  @Output() selectType = new EventEmitter<string>();
  @Output() selectDepartment = new EventEmitter<string>();
  @Output() resetFilters = new EventEmitter<void>();

  search = '';
  sortKey: SortKey = 'severity';
  sortAsc = false;

  private readonly expandedIds = new Set<string>();
  private readonly recommendationsById = new Map<string, RiskRecommendation[]>();
  private readonly loadingById = new Map<string, boolean>();

  constructor(private readonly risk: RiskService) {}

  get rows(): RiskAssessment[] {
    let rows = this.assessments;
    if (this.severityFilter !== 'ALL') {
      rows = rows.filter((row) => row.severity === this.severityFilter);
    }
    if (this.typeFilter !== 'ALL') {
      rows = rows.filter((row) => row.type === this.typeFilter);
    }
    if (this.departmentFilter !== 'ALL') {
      rows = rows.filter((row) => row.departmentId === this.departmentFilter);
    }
    const query = this.search.trim().toLowerCase();
    if (query) {
      rows = rows.filter(
        (row) =>
          row.summary.toLowerCase().includes(query) ||
          this.humanType(row.type).toLowerCase().includes(query) ||
          row.severity.toLowerCase().includes(query)
      );
    }
    const direction = this.sortAsc ? 1 : -1;
    return [...rows].sort((a, b) => {
      const av = this.sortValue(a, this.sortKey);
      const bv = this.sortValue(b, this.sortKey);
      if (av === bv) {
        return 0;
      }
      return av < bv ? -direction : direction;
    });
  }

  get hasActiveFilter(): boolean {
    return (
      this.severityFilter !== 'ALL' ||
      this.typeFilter !== 'ALL' ||
      this.departmentFilter !== 'ALL' ||
      this.search.trim().length > 0
    );
  }

  isExpanded(id: string): boolean {
    return this.expandedIds.has(id);
  }

  loadingRecommendations(id: string): boolean {
    return this.loadingById.get(id) === true;
  }

  recommendations(id: string): RiskRecommendation[] {
    return this.recommendationsById.get(id) ?? [];
  }

  toggleAssessment(id: string): void {
    if (this.expandedIds.has(id)) {
      this.expandedIds.delete(id);
      return;
    }
    this.expandedIds.add(id);
    if (!this.recommendationsById.has(id)) {
      this.loadingById.set(id, true);
      this.risk.getRecommendations(id).subscribe({
        next: (recommendations) => {
          this.recommendationsById.set(id, recommendations);
          this.loadingById.set(id, false);
        },
        error: () => {
          this.loadingById.set(id, false);
        }
      });
    }
  }

  toggleSeverity(severity: string): void {
    this.selectSeverity.emit(severity);
  }

  toggleType(type: string): void {
    this.selectType.emit(type);
  }

  toggleDepartmentSelection(departmentId: string): void {
    this.selectDepartment.emit(departmentId);
  }

  resetAllFilters(): void {
    this.search = '';
    this.resetFilters.emit();
  }

  sortBy(key: SortKey): void {
    if (this.sortKey === key) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortKey = key;
      this.sortAsc = false;
    }
  }

  sortIndicator(key: SortKey): string {
    if (this.sortKey !== key) {
      return '';
    }
    return this.sortAsc ? 'up' : 'down';
  }

  departmentName(departmentId: string | null): string {
    if (!departmentId) {
      return '-';
    }
    return (
      this.departments.find((department) => department.id === departmentId)?.name
      ?? 'Unknown department'
    );
  }

  severityClass(severity: string): string {
    return 'badge badge--' + severity.toLowerCase();
  }

  humanType(key: string): string {
    return key
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (character) => character.toUpperCase());
  }

  private sortValue(row: RiskAssessment, key: SortKey): number | string {
    switch (key) {
      case 'severity':
        return { HIGH: 3, MEDIUM: 2, LOW: 1 }[row.severity] ?? 0;
      case 'score':
        return row.score;
      case 'impactMinutes':
        return row.impactMinutes;
      default:
        return this.humanType(row.type);
    }
  }
}