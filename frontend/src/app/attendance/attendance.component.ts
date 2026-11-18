import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Employee, EmployeeService } from '../employees/employee.service';
import { AttendanceService, AttendanceSession } from './attendance.service';

@Component({
  selector: 'app-attendance',
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.scss']
})
export class AttendanceComponent implements OnInit, OnDestroy {
  currentTime: Date = new Date();
  private timer: any;

  employees: Employee[] = [];
  selectedEmployeeId: string = '';
  activeSession: AttendanceSession | null = null;
  history: AttendanceSession[] = [];

  isOnBreak: boolean = false;
  loading: boolean = false;
  statusMessage: string = '';
  isError: boolean = false;

  constructor(
    private readonly attendanceService: AttendanceService,
    private readonly employeeService: EmployeeService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.timer = setInterval(() => {
      this.currentTime = new Date();
    }, 1000);

    this.employeeService.getEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        if (data.length > 0) {
          this.selectedEmployeeId = data[0].id;
          this.loadHistory();
        }
      },
      error: () => {
        this.showFeedback('Failed to load employees', true);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  onEmployeeChange(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    if (!this.selectedEmployeeId) return;
    this.attendanceService.getMyAttendance(this.selectedEmployeeId).subscribe({
      next: (sessions) => {
        this.history = sessions;
        this.activeSession = sessions.find((s) => s.active) || null;
      },
      error: () => {
        this.history = [];
        this.activeSession = null;
      }
    });
  }

  clockIn(): void {
    this.executeCommand((payload) => this.attendanceService.clockIn(payload), 'Clocked in successfully');
  }

  breakStart(): void {
    this.executeCommand((payload) => this.attendanceService.breakStart(payload), 'Break started');
    this.isOnBreak = true;
  }

  breakEnd(): void {
    this.executeCommand((payload) => this.attendanceService.breakEnd(payload), 'Break ended');
    this.isOnBreak = false;
  }

  clockOut(): void {
    this.executeCommand((payload) => this.attendanceService.clockOut(payload), 'Clocked out successfully');
    this.isOnBreak = false;
  }

  private executeCommand(
    call: (payload: { employeeId: string; occurredAt: string }) => any,
    successMsg: string
  ): void {
    if (!this.selectedEmployeeId || this.loading) return;

    this.loading = true;
    const payload = {
      employeeId: this.selectedEmployeeId,
      occurredAt: new Date().toISOString()
    };

    call(payload).subscribe({
      next: () => {
        this.loading = false;
        this.showFeedback(successMsg, false);
        this.loadHistory();
      },
      error: (err: any) => {
        this.loading = false;
        const msg = err?.error?.message || err?.message || 'Operation failed';
        this.showFeedback(msg, true);
      }
    });
  }

  private showFeedback(message: string, isError: boolean): void {
    this.statusMessage = message;
    this.isError = isError;
    setTimeout(() => {
      this.statusMessage = '';
    }, 5000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
