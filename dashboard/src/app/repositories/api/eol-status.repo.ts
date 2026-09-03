import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {EolStatusDto} from '../dto/eol-status.dto';

/**
 * When support ends. A component that names no product answers with an empty
 * list — that is somebody saying the question does not apply, not a fault.
 */
@Injectable({providedIn: 'root'})
export class EolStatusRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/eol-statuses';

  public findAllByComponent(componentId: string): Observable<EolStatusDto[]> {
    return this.http.get<EolStatusDto[]>(UrlBuilder.builder()
                                                   .addPath(this.pathAll)
                                                   .addQuery('componentId', componentId)
                                                   .build().getUrl());
  }

  public checkNow(componentId: string): Observable<EolStatusDto[]> {
    return this.http.post<EolStatusDto[]>(UrlBuilder.builder()
                                                    .addPath(this.pathAll)
                                                    .addQuery('componentId', componentId)
                                                    .build().getUrl(), {});
  }
}
