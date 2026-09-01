/**
 * The few null-safe helpers the conventions assume. Kept small on purpose —
 * everything here exists because a bare truthiness check would be wrong for
 * `0` or `''`, not because a utility module is a good place for logic.
 */
import {Subscription} from 'rxjs';

export function isDefined<T>(value: T | undefined | null): value is T {
  return value !== undefined && value !== null;
}

export function notDefined<T>(value: T | undefined | null): value is undefined | null {
  return value === undefined || value === null;
}

export function nonBlank(value: string | undefined | null): value is string {
  return isDefined(value) && value.trim().length > 0;
}

export function isBlank(value: string | undefined | null): boolean {
  return !nonBlank(value);
}

export function unsubscribe(subscriptions: Subscription[] | Subscription | undefined): void {
  if (notDefined(subscriptions)) {
    return;
  }
  if (Array.isArray(subscriptions)) {
    subscriptions.forEach((sub) => sub.unsubscribe());
    subscriptions.length = 0;
    return;
  }
  subscriptions.unsubscribe();
}
