import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UrlBuilder} from '../item/url.item';
import {ProjectDto, ProjectRequest} from '../dto/project.dto';
import {MatrixDto} from '../dto/matrix.dto';

@Injectable({providedIn: 'root'})
export class ProjectRepo {

  private readonly http: HttpClient = inject(HttpClient);

  private readonly pathAll = '/v1/projects';
  private readonly pathOne = '/v1/projects/{id}';
  private readonly pathMatrix = '/v1/projects/{id}/matrix';

  public findAll(): Observable<ProjectDto[]> {
    return this.http.get<ProjectDto[]>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl());
  }

  public getById(id: string): Observable<ProjectDto> {
    return this.http.get<ProjectDto>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }

  /** The version overview of one project, assembled by the server. */
  public getMatrix(id: string): Observable<MatrixDto> {
    return this.http.get<MatrixDto>(UrlBuilder.builder().addPath(this.pathMatrix).addParam('id', id).build().getUrl());
  }

  public create(request: ProjectRequest): Observable<ProjectDto> {
    return this.http.post<ProjectDto>(UrlBuilder.builder().addPath(this.pathAll).build().getUrl(), request);
  }

  public update(id: string, request: ProjectRequest): Observable<ProjectDto> {
    return this.http.put<ProjectDto>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl(),
                                     request);
  }

  public delete(id: string): Observable<void> {
    return this.http.delete<void>(UrlBuilder.builder().addPath(this.pathOne).addParam('id', id).build().getUrl());
  }
}
