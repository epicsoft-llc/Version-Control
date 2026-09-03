package one.epicsoft.vc.version;

import one.epicsoft.vc.enums.ConstraintState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether a version satisfies a range — and above all the third answer.
 *
 * <p>Reading an unreadable version as satisfied is the failure that costs the
 * most: it reports that everything fits exactly where nothing is known.
 */
class ConstraintEvaluatorTest {

  private final ConstraintEvaluator evaluator = new ConstraintEvaluator();

  @Test
  void readsARangeInTheNpmStyle() {
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("16.4", ">=14 <18"));
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("18.0", ">=14 <18"));
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("13.9", ">=14 <18"));
  }

  @Test
  void copesWithTwoSegmentVersions() {
    // PostgreSQL publishes 17.2, not 17.2.0, and the range has to hold anyway.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("17.2", ">=14"));
  }

  @Test
  void saysUnknownForAVersionItCannotRead() {
    // latest and stable carry no version at all; that is the case nobody checked
    // by hand either.
    assertEquals(ConstraintState.UNKNOWN, this.evaluator.evaluate("latest", ">=14"));
    assertEquals(ConstraintState.UNKNOWN, this.evaluator.evaluate("stable", ">=14"));
    assertEquals(ConstraintState.UNKNOWN, this.evaluator.evaluate(null, ">=14"));
  }

  @Test
  void readsTheVersionOutOfATagThatAlsoNamesAVariant() {
    // 16.4-alpine carries the version 16.4. Refusing it because the tag names a
    // variant would leave the component permanently unevaluatable: a violation
    // nobody can clear, and an upgrade nobody can carry out.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("16.4-alpine", ">=14 <18"));
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("18.0-bookworm", ">=14 <18"));
    // The same for a release tag and for a Debian version with an epoch.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("v2.1.0", ">=2 <3"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("5:29.7.2", ">=29"));
  }

  @Test
  void countsAPrereleaseThatIsActuallyDeployed() {
    // The tool records what runs. A component on 2.0.0-rc.1 is inside ">=1 <3",
    // and a red mark there would be about a decision somebody made deliberately.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("2.0.0-rc.1", ">=1 <3"));
  }

  @Test
  void keepsAPrereleaseApartFromItsRelease() {
    // The rc comes before the release, and reading it as the release would
    // answer ">=2.0.0" with yes for a build that is not 2.0.0 yet.
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("2.0.0-rc.1", ">=2.0.0"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("2.0.0", ">=2.0.0"));
  }

  @Test
  void acceptsAnOperatorWrittenApartFromItsVersion() {
    // ">= 14 < 18" is the same range as ">=14 <18" and is typed that way as
    // often as not; rejecting it would file a permanent violation and block
    // every upgrade of the prerequisite.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("16.4", ">= 14 < 18"));
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("18.0", ">= 14 < 18"));
  }

  @Test
  void readsABareWildcardAsAnyVersion() {
    // The library parses a lone x into a range that nothing satisfies, so a
    // dependency written that way would report a violation for every version
    // there will ever be.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("16.4", "x"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("16.4", "*"));
  }

  @Test
  void tellsWhetherARangeCanBeStoredAtAll() {
    // Checked where it is written, so an unreadable range never becomes a
    // permanent block with nothing to explain it.
    assertTrue(this.evaluator.isReadable(">=14 <18"));
    assertFalse(this.evaluator.isReadable(">= 14, < 18"));
    assertFalse(this.evaluator.isReadable(null));
  }

  @Test
  void readsAVariantOrARevisionAsTheReleaseItPackages() {
    // A semantic version parser reads both suffixes as prereleases, which sort
    // below the release: 1.2.3-alpine would fail ">=1.2.3", and 6.4.1-15 would
    // fail ">=6.4.1". Both are the release, packaged — and the false violation
    // would also block every update of the prerequisite behind it.
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("1.2.3-alpine", ">=1.2.3"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("6.4.1-15", ">=6.4.1"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("1.27.3-alpine", ">=1.27 <2"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("29.7.2-1~ubuntu.24.04~noble", ">=29"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("5:29.7.2", ">=29"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("v2.1.0", ">=2 <3"));
  }

  @Test
  void keepsAPreviewBelowItsRelease() {
    // The one suffix that has to stay: an rc read as its own release answers
    // ">=2.0.0" with yes, and that is exactly the version nobody signed off.
    assertEquals(ConstraintState.VIOLATED, this.evaluator.evaluate("2.0.0-rc.1", ">=2.0.0"));
    assertEquals(ConstraintState.SATISFIED, this.evaluator.evaluate("2.0.0", ">=2.0.0"));
  }

  @Test
  void saysUnknownForARangeItCannotRead() {
    // A typo in a range says nothing about the version; reporting it as met
    // would hide the typo for good.
    assertEquals(ConstraintState.UNKNOWN, this.evaluator.evaluate("16.4", "at least 14"));
    assertEquals(ConstraintState.UNKNOWN, this.evaluator.evaluate("16.4", null));
  }
}
