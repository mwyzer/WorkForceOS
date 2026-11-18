import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { AssistantService } from './assistant.service';

export interface ChatMessage {
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  source?: string;
}

@Component({
  selector: 'app-assistant',
  templateUrl: './assistant.component.html',
  styleUrls: ['./assistant.component.scss']
})
export class AssistantComponent {
  question: string = '';
  loading: boolean = false;
  messages: ChatMessage[] = [
    {
      text: 'Hello! I am your WorkforceOS AI Assistant. Ask me anything about staffing, attendance, leave, or risk intelligence.',
      sender: 'assistant',
      timestamp: new Date(),
      source: 'system'
    }
  ];

  suggestedPrompts: string[] = [
    'How many employees are on leave today?',
    'What is our current workforce risk summary?',
    'Show me recent overtime and staffing levels'
  ];

  constructor(
    private readonly assistantService: AssistantService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  send(): void {
    const q = this.question.trim();
    if (!q || this.loading) {
      return;
    }

    this.messages.push({
      text: q,
      sender: 'user',
      timestamp: new Date()
    });

    this.question = '';
    this.loading = true;

    this.assistantService.ask(q).subscribe({
      next: (res) => {
        this.messages.push({
          text: res.answer,
          sender: 'assistant',
          timestamp: new Date(),
          source: res.source
        });
        this.loading = false;
      },
      error: (err) => {
        this.messages.push({
          text: 'Sorry, I encountered an error processing your query. Please try again.',
          sender: 'assistant',
          timestamp: new Date(),
          source: 'error'
        });
        this.loading = false;
      }
    });
  }

  usePrompt(prompt: string): void {
    this.question = prompt;
    this.send();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
