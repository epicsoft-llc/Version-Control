import {Subscription} from 'rxjs';
import {describe, expect, it} from 'vitest';
import {isBlank, isDefined, nonBlank, notDefined, unsubscribe} from './vc.utils';

/**
 * The cases these helpers exist for. A bare truthiness check is wrong for `0`
 * and for the empty string, and that is the whole reason the module is here —
 * so those two are what the tests are about.
 */
describe('vc.utils', () => {

  describe('isDefined', () => {

    it('keeps the falsy values that are values', () => {
      // The reason the helper exists: `if (value)` drops both of these.
      expect(isDefined(0)).toBe(true);
      expect(isDefined('')).toBe(true);
      expect(isDefined(false)).toBe(true);
    });

    it('rejects null and undefined, and only those', () => {
      expect(isDefined(null)).toBe(false);
      expect(isDefined(undefined)).toBe(false);
      expect(notDefined(null)).toBe(true);
      expect(notDefined(0)).toBe(false);
    });
  });

  describe('nonBlank', () => {

    it('treats whitespace as blank', () => {
      // A version field filled with spaces is empty; anything else would store
      // a value that looks like one in the database and like nothing on screen.
      expect(nonBlank('  ')).toBe(false);
      expect(nonBlank('\t\n')).toBe(false);
      expect(isBlank('  ')).toBe(true);
    });

    it('accepts text with surrounding whitespace', () => {
      expect(nonBlank(' 17.2 ')).toBe(true);
      expect(isBlank(' 17.2 ')).toBe(false);
    });

    it('rejects the empty string, null and undefined', () => {
      expect(nonBlank('')).toBe(false);
      expect(nonBlank(null)).toBe(false);
      expect(nonBlank(undefined)).toBe(false);
    });
  });

  describe('unsubscribe', () => {

    it('closes every subscription and empties the array', () => {
      const subscriptions = [new Subscription(), new Subscription()];

      unsubscribe(subscriptions);

      expect(subscriptions.every((subscription) => subscription.closed)).toBe(true);
      // Emptied, not just closed: a component that is destroyed and reused
      // would otherwise carry the dead ones around for the rest of its life.
      expect(subscriptions.length).toBe(0);
    });

    it('takes a single subscription as well', () => {
      const subscription = new Subscription();

      unsubscribe(subscription);

      expect(subscription.closed).toBe(true);
    });

    it('does nothing when there is nothing to close', () => {
      // Called from ngOnDestroy, which also runs when ngOnInit never did.
      expect(() => unsubscribe(undefined)).not.toThrow();
    });
  });
});
