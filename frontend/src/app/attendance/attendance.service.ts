import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AttendanceSession {
  id: string;
  organizationId: string;
  employeeId: string;
  rosterId: string;
  shiftTemplateId: string;
  clockInAt: string;
  clockOutAt?: string;
  active: boolean;
}

export interface AttendanceCommandPayload {
  employeeId: string;
  occurredAt: string;
}

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  constructor(private readonly http: HttpClient) {}

  clockIn(payload: AttendanceCommandPayload): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>('/api/v1/attendance/clock-in', payload);
  }

  clockOut(payload: AttendanceCommandPayload): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>('/api/v1/attendance/clock-out', payload);
  }

  breakStart(payload: AttendanceCommandPayload): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>('/api/v1/attendance/break-start', payload);
  }

  breakEnd(payload: AttendanceCommandPayload): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>('/api/v1/attendance/break-end', payload);
  }

  getMyAttendance(employeeId: string): Observable<AttendanceSession[]> {
    return this.http.get<AttendanceSession[]>(`/api/v1/attendance/me?employeeId=${employeeId}`);
  }
}
