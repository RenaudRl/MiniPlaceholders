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
 * every placeholder. That method is not on a cold path: consumers such as CraftEngine's network
 * manager ask "is this a placeholder tag?" for every tag of every network message, and the previous
 * linear walk made that cost proportional to the number of registered placeholders. On a server
 * registering many expansions it was the single largest CPU consumer measured.
 *
 * <p>{@link #resolve} deliberately keeps the original sequential walk. A placeholder may match a
 * name yet still return {@code null} — an audience placeholder does so when the audience is not of
 * its target type — and several placeholders may share a name, so the first non-null result is the
 * contract. Only the membership test can be indexed without changing behaviour.
 *
 * <p>This was a record; it is a final class only because a record cannot carry the derived index.
 * The {@code placeholders()} accessor and the equality semantics are preserved.
 */
public final class PlaceholderTagResolver implements TagResolver {
  public static final PlaceholderTagResolver EMPTY = new PlaceholderTagResolver();

  private final Placeholder[] placeholders;
  private final Set<String> keys;

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

  public Placeholder[] placeholders() {
    return this.placeholders;
  }

  @Override
  public @Nullable Tag resolve(final String name, final ArgumentQueue arguments, final Context ctx) throws ParsingException {
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
    // A miss is the common outcome, not the exception: consumers such as CraftEngine's network
    // manager ask about *every* tag they meet, and most are not placeholders. Lowercasing
    // unconditionally therefore allocated a String on the majority of calls — a profile attributed
    // 0,8 % of server CPU to exactly that. When the name holds nothing that lowercasing would
    // change, the copy would be equal to the name we just tested, so it is pure waste.
    return needsLowerCasing(name) && this.keys.contains(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Whether lowercasing [name] could produce anything different from it.
   *
   * Deliberately conservative: any character that is not already its own lowercase sends us down
   * the allocating path. Tag names are constrained enough that this is exact in practice, and being
   * wrong in this direction only costs an allocation — never a wrong answer.
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
    // Reference comparison on the component, matching what the generated record equals did for an
    // array component.
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
