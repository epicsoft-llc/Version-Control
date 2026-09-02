import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {ComponentDto, ComponentRequest} from '../dto/component.dto';

@Injectable({providedIn: 'root'})
export class ComponentRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/components';
  private readonly pathOne = '/v1/components/{id}';

  public findAllByProject(projectId: string): Observable<ComponentDto[]> {
    return this.http.get<ComponentDto[]>(UrlBuilder.builder()
                                                   .addPath(this.pathAll)
                                                   .addQuery('projectId', projectId)
                                                   .build().getUrl());
  }

  public getById(id: string): Observable<ComponentDto> {
    return this.http.get<ComponentDto>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }

  public create(request: ComponentRequest): Observable<ComponentDto> {
    return this.http.post<ComponentDto>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl(), request);
  }

  public update(id: string, request: ComponentRequest): Observable<ComponentDto> {
    return this.http.put<ComponentDto>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl(),
                                       request);
  }

  public delete(id: string): Observable<void> {
    return this.http.delete<void>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }
}
