import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  form: FormGroup;
  submitting: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      organizationName: ['', [Validators.required]],
      timezone: ['UTC'],
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get organizationName() {
    return this.form.get('organizationName')!;
  }

  get username() {
    return this.form.get('username')!;
  }

  get password() {
    return this.form.get('password')!;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    const payload = {
      organizationName: this.form.value.organizationName,
      timezone: this.form.value.timezone || 'UTC',
      adminUsername: this.form.value.username,
      adminPassword: this.form.value.password
    };

    this.authService.registerTenant(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Registration failed. Username may already exist.';
      }
    });
  }
}
