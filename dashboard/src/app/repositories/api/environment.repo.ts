import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {EnvironmentDto, EnvironmentRequest} from '../dto/environment.dto';

@Injectable({providedIn: 'root'})
export class EnvironmentRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/environments';
  private readonly pathOne = '/v1/environments/{id}';

  public findAllByProject(projectId: string): Observable<EnvironmentDto[]> {
    return this.http.get<EnvironmentDto[]>(UrlBuilder.builder()
                                                     .addPath(this.pathAll)
                                                     .addQuery('projectId', projectId)
                                                     .build().getUrl());
  }

  public create(request: EnvironmentRequest): Observable<EnvironmentDto> {
    return this.http.post<EnvironmentDto>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl(), request);
  }

  public update(id: string, request: EnvironmentRequest): Observable<EnvironmentDto> {
    return this.http.put<EnvironmentDto>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl(),
                                         request);
  }

  public delete(id: string): Observable<void> {
    return this.http.delete<void>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }
}
