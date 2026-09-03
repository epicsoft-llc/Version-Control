package one.epicsoft.vc.util;

import org.junit.jupiter.api.Test;

import static one.epicsoft.vc.util.VcUtil.isBlank;
import static one.epicsoft.vc.util.VcUtil.isConfigured;
import static one.epicsoft.vc.util.VcUtil.isTrue;
import static one.epicsoft.vc.util.VcUtil.nonBlank;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VcUtilTest {

  @Test
  void blankRecognisesNullEmptyAndWhitespace() {
    assertTrue(isBlank(null));
    assertTrue(isBlank(""));
    assertTrue(isBlank("   "));
    assertFalse(isBlank("x"));
    assertTrue(nonBlank("x"));
  }

  @Test
  void useEnvCountsAsNotConfigured() {
    // The whole point: a component must not believe it is configured and send
    // the literal "USE_ENV" to an upstream API.
    assertFalse(isConfigured("USE_ENV"));
    assertFalse(isConfigured("  USE_ENV  "));
    assertFalse(isConfigured(null));
    assertFalse(isConfigured(""));
    assertTrue(isConfigured("real-token"));
  }

  @Test
  void isTrueIsNullSafe() {
    assertTrue(isTrue(Boolean.TRUE));
    assertFalse(isTrue(Boolean.FALSE));
    assertFalse(isTrue(null));
  }
}
