import { Component, Output, EventEmitter } from '@angular/core';

import { StatsService } from '../../stats.service';
import { Stats } from '../../stats';

interface Error {
  message: string;
  date: string;
}

@Component({
  selector: 'app-scanner',
  templateUrl: './scanner.component.html',
  styleUrls: ['./scanner.component.sass']
})
export class ScannerComponent {

  @Output() scanned = new EventEmitter<boolean>();

  total = 0;
  file: string;
  counter = 0;
  errors: Error[] = [];
  stats: Stats;

  indeterminate: string;

  constructor (
    private statsService: StatsService
  ) {
    this.getStats();
  }

  private getStats () {
    this.statsService.get().subscribe((stats: Stats) => {
      this.stats = stats;
    });
  }

  scan () {
    this.errors = [];

    const scanProgress = new EventSource('/api/scan-progress');

    scanProgress.addEventListener('start', (event: any) => {
      this.indeterminate = 'Counting files';
    });

    scanProgress.addEventListener('total', (event: any) => {
      this.indeterminate = null;
      this.total = this.total || event.data;
    });

    scanProgress.addEventListener('current-file', (event: any) => {
      this.file = event.data;
      this.counter += 1;
      console.log(`File ${ this.counter }/${ this.total }`);
    });

    scanProgress.addEventListener('cleanUp', (event: any) => {
      this.indeterminate = 'Cleaning up';
    });

    scanProgress.addEventListener('association', (event: any) => {
      this.indeterminate = 'Bundling volumes';
    });

    scanProgress.addEventListener('error', (event: any) => {
      this.errors.push({ message: event.data, date: new Date().toISOString() });
    });

    scanProgress.addEventListener('done', () => {
      this.indeterminate = null;
      this.counter = 0;
      this.total = 0;
      this.scanned.emit(true);
      this.getStats();
      scanProgress.close();
    });
  }
}
