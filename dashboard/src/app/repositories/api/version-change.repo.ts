import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {VersionChangeDto, VersionChangeRequest} from '../dto/version-change.dto';

/**
 * The change history — and, by writing an entry, the only way to change a
 * version. There is deliberately no call that does one without the other.
 */
@Injectable({providedIn: 'root'})
export class VersionChangeRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/version-changes';

  public findAllByComponent(componentId: string): Observable<VersionChangeDto[]> {
    return this.http.get<VersionChangeDto[]>(UrlBuilder.builder()
                                                       .addPath(this.pathAll)
                                                       .addQuery('componentId', componentId)
                                                       .build().getUrl());
  }

  public findAllByComponentEnvironment(componentEnvironmentId: string): Observable<VersionChangeDto[]> {
    return this.http.get<VersionChangeDto[]>(UrlBuilder.builder()
                                                       .addPath(this.pathAll)
                                                       .addQuery('componentEnvironmentId', componentEnvironmentId)
                                                       .build().getUrl());
  }

  public changeVersion(request: VersionChangeRequest): Observable<VersionChangeDto> {
    return this.http.post<VersionChangeDto>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl(), request);
  }
}
