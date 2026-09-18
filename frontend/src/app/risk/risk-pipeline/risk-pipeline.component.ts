import { Component } from '@angular/core';

interface PipelineStage {
  step: number;
  name: string;
  detail: string;
  icon: string;
}

@Component({
  selector: 'app-risk-pipeline',
  templateUrl: './risk-pipeline.component.html',
  styleUrls: ['./risk-pipeline.component.scss']
})
export class RiskPipelineComponent {
  stages: PipelineStage[] = [
    {
      step: 1,
      name: 'Workforce data',
      detail: 'Employees, roster, attendance, leave, overtime, swaps',
      icon: 'database'
    },
    {
      step: 2,
      name: 'Risk engine',
      detail: 'Five rules score every published assignment and team',
      icon: 'calculator'
    },
    {
      step: 3,
      name: 'Structured risk',
      detail: 'Risk type, severity, score and affected window',
      icon: 'pulse'
    },
    {
      step: 4,
      name: 'LLM analysis',
      detail: 'Readable explanation and quantified business impact',
      icon: 'chip'
    },
    {
      step: 5,
      name: 'Recommendations',
      detail: 'Heuristic catalog merged with AI-suggested actions',
      icon: 'list'
    },
    {
      step: 6,
      name: 'Alert & action',
      detail: 'Open alerts with links to shift swap and overtime',
      icon: 'bell'
    }
  ];
}