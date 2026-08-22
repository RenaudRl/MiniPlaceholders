package io.github.miniplaceholders.api.placeholder;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A resolver that answers {@link #has(String)} from one set covering every expansion, while
 * delegating {@link #resolve} to the original sequence.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@link PlaceholderTagResolver#has(String)} was made O(1) because a linear walk over the
 * placeholders of a single expansion was the largest CPU consumer measured on a server registering
 * many of them. That fixed one level. The level above was untouched: the resolvers returned by
 * {@link io.github.miniplaceholders.api.MiniPlaceholders} assemble one resolver <em>per
 * expansion</em> into Adventure's sequential resolver, and {@code SequentialTagResolver.has}
 * walks that structure in full on every miss.
 *
 * <p>A profile taken on 22 August 2026 — three passes, fifty moving players — attributed 6,58 % of
 * whole-JVM CPU to this chain, roughly two thirds of it reached through CraftEngine's network
 * manager, which asks "is this a placeholder tag?" for every tag of every outgoing message. The
 * per-expansion cost was already minimal; what remained was the number of expansions walked.
 *
 * <h2>What is and is not indexed</h2>
 *
 * <p>Only membership is indexed. {@link #resolve} delegates to the sequence unchanged, because a
 * placeholder may match a name and still return {@code null} — an audience placeholder does so
 * when the audience is not of its type — and the contract is "first non-null result".
 *
 * <p>A part whose keys cannot be enumerated (any {@link TagResolver} that is not one of ours) is
 * kept aside and still asked. Answering {@code false} for such a part would silently drop tags,
 * and would <em>improve</em> every profile while doing so — which is exactly why it is guarded by
 * a test rather than by care.
 */
public final class IndexedTagResolver implements TagResolver {
    private final TagResolver delegate;
    private final Set<String> keys;
    private final TagResolver[] opaque;

    private IndexedTagResolver(final TagResolver delegate, final Set<String> keys, final TagResolver[] opaque) {
        this.delegate = delegate;
        this.keys = keys;
        this.opaque = opaque;
    }

    /**
     * Assembles parts in order, indexing the keys of those it can read.
     *
     * @param parts the resolvers to assemble, in resolution order
     * @return an empty resolver, the single part, or an indexed assembly
     */
    public static TagResolver of(final List<TagResolver> parts) {
        if (parts.isEmpty()) {
            return TagResolver.empty();
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        final Set<String> collected = new HashSet<>();
        final List<TagResolver> unreadable = new ArrayList<>();
        for (final TagResolver part : parts) {
            if (!collectKeys(part, collected)) {
                unreadable.add(part);
            }
        }
        return new IndexedTagResolver(
                TagResolver.resolver(parts),
                Set.copyOf(collected),
                unreadable.toArray(new TagResolver[0]));
    }

    /** @return whether every key of {@code part} could be read into {@code into} */
    private static boolean collectKeys(final TagResolver part, final Set<String> into) {
        if (part instanceof final PlaceholderTagResolver placeholders) {
            for (final Placeholder placeholder : placeholders.placeholders()) {
                into.add(placeholder.key().toLowerCase(Locale.ROOT));
            }
            return true;
        }
        if (part instanceof final IndexedTagResolver indexed) {
            into.addAll(indexed.keys);
            return indexed.opaque.length == 0;
        }
        return false;
    }

    @Override
    public boolean has(final String name) {
        if (this.keys.contains(name)) {
            return true;
        }
        // Same reasoning as PlaceholderTagResolver.has: a miss is the common outcome, and
        // lowercasing unconditionally would allocate a String on the majority of calls.
        if (needsLowerCasing(name) && this.keys.contains(name.toLowerCase(Locale.ROOT))) {
            return true;
        }
        for (final TagResolver resolver : this.opaque) {
            if (resolver.has(name)) {
                return true;
            }
        }
        return false;
    }

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
    public @Nullable Tag resolve(final String name, final ArgumentQueue arguments, final Context ctx) throws ParsingException {
        return this.delegate.resolve(name, arguments, ctx);
    }

    @Override
    public String toString() {
        return "IndexedTagResolver[keys=" + this.keys.size() + ", opaque=" + this.opaque.length + ']';
    }
}
