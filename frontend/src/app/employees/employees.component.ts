import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import {
  Department,
  Employee,
  EmployeeService,
  Team,
  UpdateEmployeePayload,
  CreateEmployeePayload
} from './employee.service';

interface EmployeeRow {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  departmentId: string;
  departmentName: string;
  teamId: string;
  teamName: string;
  active: boolean;
}

@Component({
  selector: 'app-employees',
  templateUrl: './employees.component.html',
  styleUrls: ['./employees.component.scss']
})
export class EmployeesComponent implements OnInit {
  employees: EmployeeRow[] = [];
  departments: Department[] = [];
  teams: Team[] = [];
  loading = true;
  saving = false;
  errorMessage: string | null = null;
  search = '';
  modalOpen = false;
  editing: Employee | null = null;
  form: FormGroup;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly fb: FormBuilder,
    private readonly employeesService: EmployeeService
  ) {
    this.form = this.fb.group({
      employeeNumber: ['', Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      departmentId: ['', Validators.required],
      teamId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadAll();
  }

  get filteredEmployees(): EmployeeRow[] {
    const term = this.search.trim().toLowerCase();
    if (!term) {
      return this.employees;
    }
    return this.employees.filter((employee) =>
      [
        employee.employeeNumber,
        employee.firstName,
        employee.lastName,
        employee.email,
        employee.departmentName,
        employee.teamName
      ]
        .join(' ')
        .toLowerCase()
        .includes(term)
    );
  }

  get teamsForDepartment(): Team[] {
    const departmentId = this.form.get('departmentId')?.value;
    return this.teams.filter((team) => team.departmentId === departmentId);
  }

  loadAll(): void {
    this.loading = true;
    this.errorMessage = null;
    this.employeesService.getDepartments().subscribe({
      next: (departments) => {
        this.departments = departments;
        this.employeesService.getTeams().subscribe({
          next: (teams) => {
            this.teams = teams;
            this.loadEmployees();
          },
          error: () => {
            this.errorMessage = 'Could not load teams. Try again.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Could not load departments. Try again.';
        this.loading = false;
      }
    });
  }

  loadEmployees(): void {
    this.employeesService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees.map((employee) => {
          const department = this.departments.find((candidate) => candidate.id === employee.departmentId);
          const team = this.teams.find((candidate) => candidate.id === employee.teamId);
          return {
            ...employee,
            departmentName: department ? department.name : '—',
            teamName: team ? team.name : '—'
          };
        });
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load employees. Try again.';
        this.loading = false;
      }
    });
  }

  openCreate(): void {
    this.editing = null;
    this.form.reset();
    if (this.departments.length > 0) {
      this.form.patchValue({ departmentId: this.departments[0].id });
    }
    this.modalOpen = true;
  }

  openEdit(employee: EmployeeRow): void {
    this.editing = {
      id: employee.id,
      employeeNumber: employee.employeeNumber,
      firstName: employee.firstName,
      lastName: employee.lastName,
      email: employee.email,
      departmentId: employee.departmentId,
      teamId: employee.teamId,
      active: employee.active
    };
    this.form.patchValue({
      employeeNumber: employee.employeeNumber,
      firstName: employee.firstName,
      lastName: employee.lastName,
      email: employee.email,
      departmentId: employee.departmentId,
      teamId: employee.teamId
    });
    this.modalOpen = true;
  }

  closeModal(): void {
    if (!this.saving) {
      this.modalOpen = false;
      this.editing = null;
    }
  }

  onDepartmentChange(): void {
    this.form.patchValue({ teamId: '' });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.errorMessage = null;
    const value = this.form.value;

    if (this.editing) {
      const payload: UpdateEmployeePayload = {
        firstName: value.firstName,
        lastName: value.lastName,
        email: value.email,
        departmentId: value.departmentId,
        teamId: value.teamId
      };
      this.employeesService.updateEmployee(this.editing.id, payload).subscribe({
        next: () => {
          this.saving = false;
          this.modalOpen = false;
          this.editing = null;
          this.loadEmployees();
        },
        error: (error: HttpErrorResponse) => {
          this.saving = false;
          this.errorMessage = error.error?.message || 'Could not save the employee.';
        }
      });
      return;
    }

    const payload: CreateEmployeePayload = {
      employeeNumber: value.employeeNumber,
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      departmentId: value.departmentId,
      teamId: value.teamId
    };
    this.employeesService.createEmployee(payload).subscribe({
      next: () => {
        this.saving = false;
        this.modalOpen = false;
        this.loadEmployees();
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.errorMessage = error.error?.message || 'Could not create the employee.';
      }
    });
  }

  deactivate(employee: EmployeeRow): void {
    const confirmed = window.confirm(
      `Deactivate ${employee.firstName} ${employee.lastName}? They will be removed from active staffing.`
    );
    if (!confirmed) {
      return;
    }
    this.employeesService.deactivateEmployee(employee.id).subscribe({
      next: (deactivated) => {
        const index = this.employees.findIndex((candidate) => candidate.id === deactivated.id);
        if (index >= 0) {
          this.employees[index] = { ...this.employees[index], active: false };
        }
      },
      error: () => {
        this.errorMessage = 'Could not deactivate the employee.';
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}