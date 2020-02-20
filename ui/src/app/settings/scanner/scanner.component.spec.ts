import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { StatsServiceMocks as statsService } from '../../../testing/stats.service.mocks';
import { ComicsServiceMocks as comicsService } from '../../../testing/comics.service.mocks';
import { StatsService } from '../../stats.service';
import { ComicsService } from '../../comics.service';

import { ScannerComponent } from './scanner.component';
import { SettingsPageModule } from '../settings.module';

describe('ScannerComponent', () => {
  let component: ScannerComponent;
  let fixture: ComponentFixture<ScannerComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        SettingsPageModule,
        RouterTestingModule
      ],
      providers: [{
        provide: StatsService, useValue: statsService
      }, {
        provide: ComicsService, useValue: comicsService
      }]
    });
    fixture = TestBed.createComponent(ScannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('#scan', () => {

    let addEventListenerSpy;

    beforeEach(() => {
      addEventListenerSpy = spyOn(EventSource.prototype, 'addEventListener');
      spyOn(EventSource.prototype, 'close');
      component.scan();
    });

    it('adds event listeners', () => {
      expect(EventSource.prototype.addEventListener)
        .toHaveBeenCalledWith('total', jasmine.any(Function));
      expect(EventSource.prototype.addEventListener)
        .toHaveBeenCalledWith('current-file', jasmine.any(Function));
      expect(EventSource.prototype.addEventListener)
        .toHaveBeenCalledWith('scan-issue', jasmine.any(Function));
      expect(EventSource.prototype.addEventListener)
        .toHaveBeenCalledWith('done', jasmine.any(Function));
    });

    describe('when complete', () => {

      beforeEach(() => {
        const doneCallback = addEventListenerSpy.calls.mostRecent().args[1];
        spyOn(component.scanned, 'emit');
        doneCallback();
      });

      it('emits the `scanned` event', () => {
        expect(component.scanned.emit).toHaveBeenCalledWith(true);
      });

      it('closes the event source', () => {
        expect(EventSource.prototype.close).toHaveBeenCalled();
      });
    });
  });
});
