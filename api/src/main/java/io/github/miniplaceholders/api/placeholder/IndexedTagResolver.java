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
 * Answers {@link #has(String)} from a single set covering every assembled part, in constant time
 * regardless of how many expansions are registered.
 *
 * <p>Use this instead of {@link TagResolver#resolver(Iterable)} when the assembled resolver is
 * asked about tags far more often than it resolves them. Consumers that inspect every tag of every
 * message — CraftEngine's network manager, for one — otherwise pay a walk over one resolver per
 * expansion on each miss.
 *
 * <p>Only membership is indexed. {@link #resolve} delegates to the sequence unchanged: a
 * placeholder may match a name and still return {@code null}, so the "first non-null result"
 * contract belongs to the sequence and cannot be derived from the index.
 *
 * <p>A part whose keys cannot be enumerated is kept aside and still consulted by {@link #has}, so
 * assembling an unknown {@link TagResolver} never loses its tags.
 *
 * <p>Instances are immutable and safe to share across threads.
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
        // A miss is the common outcome here, so lowercasing unconditionally would allocate on the
        // majority of calls.
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
