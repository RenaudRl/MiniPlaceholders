package io.github.miniplaceholders.api.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PlaceholderTagResolver#has(String)} answers from a key index rather than by walking every
 * placeholder. These tests pin the behaviour indexing had to preserve.
 *
 * <p>The distinction that matters here is key versus name: every {@code Placeholder.has}
 * implementation compares the <em>key</em>, and the two components are not the same. Indexing the
 * name instead would silently answer the wrong question, which is what {@link #matchesOnKeyNotName}
 * guards.
 */
class PlaceholderTagResolverHasTest {

  private static Placeholder global(final String key, final String name) {
    return new GlobalPlaceholder(key, name, (queue, ctx) -> Tag.selfClosingInserting(Component.text(key)));
  }

  private static PlaceholderTagResolver resolverOf(final String... keys) {
    final Placeholder[] placeholders = new Placeholder[keys.length];
    for (int i = 0; i < keys.length; i++) {
      placeholders[i] = global(keys[i], keys[i]);
    }
    return new PlaceholderTagResolver(placeholders);
  }

  @Test
  @DisplayName("a registered key is found")
  void findsRegisteredKey() {
    assertTrue(resolverOf("player_name").has("player_name"));
  }

  @Test
  @DisplayName("matching is on the key, never on the name")
  void matchesOnKeyNotName() {
    final PlaceholderTagResolver resolver =
        new PlaceholderTagResolver(global("expansion_field", "field"));

    assertTrue(resolver.has("expansion_field"));
    assertFalse(resolver.has("field"));
  }

  @Test
  @DisplayName("matching ignores case, in both directions")
  void matchingIgnoresCase() {
    final PlaceholderTagResolver lower = resolverOf("player_name");
    assertTrue(lower.has("PLAYER_NAME"));
    assertTrue(lower.has("Player_Name"));

    final PlaceholderTagResolver upper = resolverOf("PLAYER_NAME");
    assertTrue(upper.has("player_name"));
  }

  @Test
  @DisplayName("an unregistered key is not found")
  void rejectsUnknownKey() {
    assertFalse(resolverOf("player_name").has("player_health"));
  }

  @Test
  @DisplayName("a key that merely shares a prefix is not found")
  void rejectsPrefixOfRegisteredKey() {
    final PlaceholderTagResolver resolver = resolverOf("player_name");
    assertFalse(resolver.has("player"));
    assertFalse(resolver.has("player_names"));
  }

  @Test
  @DisplayName("every key of a multi-placeholder resolver is found")
  void findsEveryRegisteredKey() {
    final PlaceholderTagResolver resolver = resolverOf("alpha", "beta", "gamma");
    assertTrue(resolver.has("alpha"));
    assertTrue(resolver.has("beta"));
    assertTrue(resolver.has("gamma"));
    assertFalse(resolver.has("delta"));
  }

  @Test
  @DisplayName("duplicate keys stay resolvable")
  void toleratesDuplicateKeys() {
    assertTrue(resolverOf("same", "same").has("same"));
  }

  @Test
  @DisplayName("a miss on an already-lowercase name is still a miss")
  void missOnLowercaseNameStaysAMiss() {
    // Guards the allocation-free path: a name holding nothing to lowercase skips the second
    // lookup entirely, so this must not accidentally start returning true.
    final PlaceholderTagResolver resolver = resolverOf("player_name");
    assertFalse(resolver.has("unknown_tag"));
    assertFalse(resolver.has("player_nam"));
  }

  @Test
  @DisplayName("a miss on a mixed-case name is still a miss")
  void missOnMixedCaseNameStaysAMiss() {
    assertFalse(resolverOf("player_name").has("UNKNOWN_TAG"));
  }

  @Test
  @DisplayName("digits and separators do not trigger the slow path wrongly")
  void handlesNamesWithoutLetters() {
    final PlaceholderTagResolver resolver = resolverOf("stat_1", "a-b_c");
    assertTrue(resolver.has("stat_1"));
    assertTrue(resolver.has("a-b_c"));
    assertFalse(resolver.has("stat_2"));
  }

  @Test
  @DisplayName("the empty resolver finds nothing")
  void emptyResolverFindsNothing() {
    assertFalse(PlaceholderTagResolver.EMPTY.has("anything"));
    assertFalse(resolverOf().has("anything"));
  }
}
