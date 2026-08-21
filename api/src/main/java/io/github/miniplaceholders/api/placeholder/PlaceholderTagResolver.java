package io.github.miniplaceholders.api.placeholder;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A TagResolver responsible for containing and processing multiple placeholders.
 *
 * <p>{@link #has(String)} answers from a set built once at construction rather than by walking
 * every placeholder.
 *
 * <p>{@link #resolve} deliberately keeps the original sequential walk. A placeholder may match a
 * name yet still return {@code null} — an audience placeholder does so when the audience is not of
 * its target type — and several placeholders may share a name, so the first non-null result is the
 * contract. Only the membership test can be indexed without changing behaviour.
 *
 * <p>The {@code placeholders()} accessor and the equality semantics of the former record are
 * preserved.
 */
public final class PlaceholderTagResolver implements TagResolver {
  public static final PlaceholderTagResolver EMPTY = new PlaceholderTagResolver();

  private final Placeholder[] placeholders;
  private final Set<String> keys;

  /**
   * Creates a resolver that resolves placeholders in the supplied order.
   *
   * @param placeholders the placeholders to process
   */
  public PlaceholderTagResolver(final Placeholder... placeholders) {
    this.placeholders = placeholders;
    // Indexed on key, not name: every Placeholder.has implementation compares the *key*, and the
    // two components differ. Matching is case-insensitive, as equalsIgnoreCase was. Locale.ROOT
    // keeps that independent of the server's default locale, which would otherwise fold 'I'
    // unexpectedly under a Turkish locale.
    final Set<String> collected = new HashSet<>(placeholders.length * 2);
    for (final Placeholder placeholder : placeholders) {
      collected.add(placeholder.key().toLowerCase(Locale.ROOT));
    }
    this.keys = Set.copyOf(collected);
  }

  /**
   * Returns the placeholders in their resolution order.
   *
   * @return the placeholders supplied at construction
   */
  public Placeholder[] placeholders() {
    return this.placeholders;
  }

  @Override
  public @Nullable Tag resolve(
      final String name,
      final ArgumentQueue arguments,
      final Context ctx
  ) throws ParsingException {
    @Nullable Tag tag;
    for (final Placeholder placeholder : placeholders) {
      tag = placeholder.resolve(name, arguments, ctx);
      if (tag != null) {
        return tag;
      }
    }
    return null;
  }

  @Override
  public boolean has(final String name) {
    if (this.keys.contains(name)) {
      return true;
    }
    return needsLowerCasing(name) && this.keys.contains(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Returns whether a case-insensitive lookup needs a lower-case copy of the name.
   *
   * @param name the name to inspect
   * @return whether lower-casing the name could change it
   */
  private static boolean needsLowerCasing(final String name) {
    for (int index = 0; index < name.length(); index++) {
      final char character = name.charAt(index);
      if (Character.toLowerCase(character) != character) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) return true;
    if (!(other instanceof final PlaceholderTagResolver that)) return false;
    // Match the former record's array-component equality, which compares the array by identity.
    return this.placeholders == that.placeholders;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this.placeholders);
  }

  @Override
  public String toString() {
    return "PlaceholderTagResolver[placeholders=" + Arrays.toString(this.placeholders) + ']';
  }
}
