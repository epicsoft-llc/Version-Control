import {environment} from '../../../environments/environment';

/**
 * Builds API URLs from a path template. Components and repositories never
 * concatenate strings themselves: a template plus named parameters keeps the
 * base URL in one place and encodes every value.
 *
 * ```typescript
 * UrlBuilder.builder()
 *   .addPath('/v1/components/{id}')
 *   .addParam('id', component.id)
 *   .addQuery('environmentId', environmentId)
 *   .build();
 * ```
 */
export class UrlDto {

  constructor(private readonly url: string) {
  }

  public getUrl(): string {
    return this.url;
  }
}

export class UrlBuilder {

  private path = '';
  private readonly params = new Map<string, string>();
  private readonly query = new Map<string, string>();

  public static builder(): UrlBuilder {
    return new UrlBuilder();
  }

  public addPath(path: string): UrlBuilder {
    this.path = path;
    return this;
  }

  public addParam(name: string, value: string | number): UrlBuilder {
    this.params.set(name, String(value));
    return this;
  }

  public addQuery(name: string, value: string | number | boolean | undefined): UrlBuilder {
    if (value !== undefined) {
      this.query.set(name, String(value));
    }
    return this;
  }

  public build(): UrlDto {
    let path = this.path;
    this.params.forEach((value, name) => {
      path = path.replace(`{${name}}`, encodeURIComponent(value));
    });
    if (path.includes('{')) {
      throw new Error(`Unresolved path parameter in '${path}'`);
    }
    const query = Array.from(this.query.entries())
      .map(([name, value]) => `${encodeURIComponent(name)}=${encodeURIComponent(value)}`)
      .join('&');
    return new UrlDto(`${environment.apiBaseUrl}${path}${query.length > 0 ? `?${query}` : ''}`);
  }
}
