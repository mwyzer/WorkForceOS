import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { EmployeesComponent } from './employees/employees.component';
import { HomeComponent } from './home/home.component';
import { RiskDashboardComponent } from './risk/risk-dashboard/risk-dashboard.component';
import { AttendanceComponent } from './attendance/attendance.component';
import { AssistantComponent } from './assistant/assistant.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: '', component: HomeComponent, canActivate: [authGuard] },
  { path: 'employees', component: EmployeesComponent, canActivate: [authGuard] },
  { path: 'attendance', component: AttendanceComponent, canActivate: [authGuard] },
  { path: 'risk', component: RiskDashboardComponent, canActivate: [authGuard] },
  { path: 'assistant', component: AssistantComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
