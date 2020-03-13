import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ComicsServiceMocks } from '../../../testing/comics.service.mocks';
import { StatsServiceMocks } from '../../../testing/stats.service.mocks';
import { ComicsService } from '../../comics.service';
import { StatsService } from '../../stats.service';
import { SettingsPageModule } from '../settings.module';

import { ScannerComponent } from './scanner.component';

let component: ScannerComponent;
let fixture: ComponentFixture<ScannerComponent>;
let comicsService: jasmine.SpyObj<ComicsService>;
let statsService: jasmine.SpyObj<StatsService>;

describe('ScannerComponent', () => {

  beforeEach(() => {
    comicsService = ComicsServiceMocks.comicsService;
    statsService = StatsServiceMocks.statsService;

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
