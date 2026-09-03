package one.epicsoft.vc.version;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.epicsoft.vc.jpa.entity.ComponentEntity;
import one.epicsoft.vc.jpa.entity.UpdateCheckEntity;
import org.springframework.stereotype.Component;
import java.util.List;

import static one.epicsoft.vc.util.VcUtil.nonBlank;

/**
 * The versions a component may actually move to, out of what its last check
 * found.
 *
 * <p>The check stores the tag list as the source delivered it — unfiltered, so
 * that a rule can be tried against it later without asking the source again.
 * Anything that proposes a version has to put that list through the rule first.
 * Skipping that step is how a proposal ends up naming {@code 16.9-bookworm} for
 * a component that runs {@code 16.4} and accepts plain numbers, or a beta for a
 * component that does not take prereleases: both are in the raw list, and both
 * are versions the check itself would never have reported.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllowedCandidates {

  private final VersionSelector versionSelector;

  /**
   * The stored candidates, reduced to what the component's rule accepts.
   *
   * <p>Where no pattern is configured, one is derived from the version that is
   * deployed — the same rule the check itself applies, and per environment the
   * more accurate one: an environment running {@code 17.2-alpine} is asking
   * about alpine tags whatever the others run.
   *
   * <p>A rule that matches nothing falls back to the version the check reported,
   * which is the one thing known to have passed the rule. The refusal itself is
   * not raised here: it belongs to the check, which records it as an error, and
   * repeating it in a read-only view would turn a screen into a failure.
   *
   * @param deployedVersion what the environment in question runs
   */
  @Nonnull
  public List<String> of(@Nonnull final ComponentEntity component, @Nonnull final UpdateCheckEntity check,
                         final String deployedVersion) {
    final List<VersionCandidate> stored = CandidateTags.parse(check.getCandidateTags());
    if (stored.isEmpty()) {
      return this.reported(check);
    }
    final String pattern = nonBlank(component.getTagPattern()) ? component.getTagPattern()
                                                               : TagRule.derivePattern(deployedVersion);
    try {
      return this.versionSelector.matching(stored,
                                           pattern,
                                           component.getMajorConstraint(),
                                           component.isAllowPrerelease(),
                                           component.getVersionStrategy())
                 .stream()
                 .map(VersionCandidate::tag)
                 .toList();
    } catch (final VersionRuleException exception) {
      log.debug("Rule of component {} matches none of the stored candidates: {}", component.getId(),
                exception.getMessage());
      return this.reported(check);
    }
  }

  @Nonnull
  private List<String> reported(@Nonnull final UpdateCheckEntity check) {
    return nonBlank(check.getLatestVersion()) ? List.of(check.getLatestVersion()) : List.of();
  }
}
