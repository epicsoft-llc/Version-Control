import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {ComponentEnvironmentDto, ComponentEnvironmentRequest} from '../dto/component-environment.dto';

@Injectable({providedIn: 'root'})
export class ComponentEnvironmentRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/component-environments';
  private readonly pathOne = '/v1/component-environments/{id}';

  public findAllByComponent(componentId: string): Observable<ComponentEnvironmentDto[]> {
    return this.http.get<ComponentEnvironmentDto[]>(UrlBuilder.builder()
                                                              .addPath(this.pathAll)
                                                              .addQuery('componentId', componentId)
                                                              .build().getUrl());
  }

  public getById(id: string): Observable<ComponentEnvironmentDto> {
    return this.http.get<ComponentEnvironmentDto>(UrlBuilder.builder()
                                                            .addPath(this.pathOne)
                                                            .addParam('id', id)
                                                            .build().getUrl());
  }

  /** Sets the pair up or switches it off; a recorded version is never changed here. */
  public createOrUpdate(request: ComponentEnvironmentRequest): Observable<ComponentEnvironmentDto> {
    return this.http.post<ComponentEnvironmentDto>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl(),
                                                   request);
  }

  public delete(id: string): Observable<void> {
    return this.http.delete<void>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }
}
