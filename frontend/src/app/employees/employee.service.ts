import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Employee {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  departmentId: string;
  teamId: string;
  active: boolean;
}

export interface CreateEmployeePayload {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  departmentId: string;
  teamId: string;
}

export interface UpdateEmployeePayload {
  firstName: string;
  lastName: string;
  email: string;
  departmentId: string;
  teamId: string;
}

export interface Department {
  id: string;
  organizationId: string;
  name: string;
  active: boolean;
}

export interface Team {
  id: string;
  departmentId: string;
  name: string;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  constructor(private readonly http: HttpClient) {}

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>('/api/v1/employees');
  }

  createEmployee(payload: CreateEmployeePayload): Observable<Employee> {
    return this.http.post<Employee>('/api/v1/employees', payload);
  }

  updateEmployee(id: string, payload: UpdateEmployeePayload): Observable<Employee> {
    return this.http.put<Employee>(`/api/v1/employees/${id}`, payload);
  }

  deactivateEmployee(id: string): Observable<Employee> {
    return this.http.delete<Employee>(`/api/v1/employees/${id}`);
  }

  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>('/api/v1/departments');
  }

  getTeams(): Observable<Team[]> {
    return this.http.get<Team[]>('/api/v1/teams');
  }
}