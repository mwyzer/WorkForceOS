import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AssistantResponse {
  answer: string;
  source: string;
}

@Injectable({ providedIn: 'root' })
export class AssistantService {
  constructor(private readonly http: HttpClient) {}

  ask(question: string): Observable<AssistantResponse> {
    return this.http.post<AssistantResponse>('/api/v1/assistant/ask', { question });
  }
}
