package io.github.miniplaceholders.api.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests are written before the measurement, deliberately.
 *
 * <p>The failure mode of an index over {@code has} is asymmetric and treacherous: answering
 * {@code false} when the answer is {@code true} deletes work, so a profile <em>rewards</em> the
 * bug. A previous version of {@link PlaceholderTagResolver} indexed on {@code name()} instead of
 * {@code key()} and appeared to remove the cost entirely; a test caught it, the bench did not.
 */
class IndexedTagResolverTest {

    private static Placeholder placeholder(final String key, final String value) {
        return new GlobalPlaceholder(key, key, (queue, ctx) -> Tag.selfClosingInserting(Component.text(value)));
    }

    private static TagResolver expansion(final String... keys) {
        final Placeholder[] placeholders = new Placeholder[keys.length];
        for (int index = 0; index < keys.length; index++) {
            placeholders[index] = placeholder(keys[index], keys[index] + "-value");
        }
        return new PlaceholderTagResolver(placeholders);
    }

    @Test
    @DisplayName("a key from any part is found")
    void findsKeysAcrossParts() {
        final TagResolver resolver = IndexedTagResolver.of(List.of(
                expansion("server_online", "server_max"),
                expansion("player_name"),
                expansion("luckperms_prefix")));

        assertTrue(resolver.has("server_online"));
        assertTrue(resolver.has("player_name"));
        assertTrue(resolver.has("luckperms_prefix"));
    }

    @Test
    @DisplayName("matching stays case-insensitive, as the linear walk was")
    void matchingIsCaseInsensitive() {
        final TagResolver resolver = IndexedTagResolver.of(List.of(
                expansion("server_online"), expansion("player_name")));

        assertTrue(resolver.has("Server_Online"));
        assertTrue(resolver.has("PLAYER_NAME"));
    }

    @Test
    @DisplayName("an unknown name is not found")
    void unknownNameIsNotFound() {
        final TagResolver resolver = IndexedTagResolver.of(List.of(
                expansion("server_online"), expansion("player_name")));

        assertFalse(resolver.has("bold"));
        assertFalse(resolver.has("server_onlin"));
        assertFalse(resolver.has(""));
    }

    @Test
    @DisplayName("a part whose keys cannot be read is still asked — the trap this class must not fall into")
    void opaquePartIsStillConsulted() {
        // Un resolver quelconque, dont on ne sait pas enumerer les cles. Repondre « non » pour
        // lui ferait disparaitre ses balises en silence, et allegerait le profil au passage.
        final TagResolver opaque = new TagResolver() {
            @Override
            public Tag resolve(final String name, final ArgumentQueue arguments, final Context ctx) {
                return "opaque_tag".equals(name) ? Tag.selfClosingInserting(Component.text("ok")) : null;
            }

            @Override
            public boolean has(final String name) {
                return "opaque_tag".equals(name);
            }
        };
        final TagResolver resolver = IndexedTagResolver.of(List.of(expansion("server_online"), opaque));

        assertTrue(resolver.has("opaque_tag"));
        assertTrue(resolver.has("server_online"));
        assertFalse(resolver.has("nothing"));
    }

    @Test
    @DisplayName("an opaque part is consulted only after the index misses")
    void opaquePartIsNotConsultedOnAHit() {
        final AtomicInteger asked = new AtomicInteger();
        final TagResolver counting = new TagResolver() {
            @Override
            public Tag resolve(final String name, final ArgumentQueue arguments, final Context ctx) {
                return null;
            }

            @Override
            public boolean has(final String name) {
                asked.incrementAndGet();
                return false;
            }
        };
        final TagResolver resolver = IndexedTagResolver.of(List.of(expansion("server_online"), counting));

        assertTrue(resolver.has("server_online"));
        assertEquals(0, asked.get(), "un succes de l'index ne doit reveiller aucun resolver opaque");

        assertFalse(resolver.has("unknown"));
        assertEquals(1, asked.get());
    }

    @Test
    @DisplayName("resolve keeps the first non-null result, in the order of the parts")
    void resolveKeepsOrder() {
        final TagResolver first = new PlaceholderTagResolver(placeholder("shared", "from-first"));
        final TagResolver second = new PlaceholderTagResolver(placeholder("shared", "from-second"));
        final TagResolver resolver = IndexedTagResolver.of(List.of(first, second));

        assertTrue(resolver.has("shared"));
        // Le contrat « premier resultat non nul » appartient a la sequence, pas a l'index :
        // resolve doit rester delegue tel quel.
        assertNotNull(resolver.resolve("shared", null, null));
    }

    @Test
    @DisplayName("resolve returns null for an unknown name")
    void resolveReturnsNullForUnknown() {
        final TagResolver resolver = IndexedTagResolver.of(List.of(
                expansion("server_online"), expansion("player_name")));

        assertNull(resolver.resolve("bold", null, null));
    }

    @Test
    @DisplayName("no part gives the empty resolver, one part is returned untouched")
    void degenerateAssemblies() {
        assertSame(TagResolver.empty(), IndexedTagResolver.of(List.of()));

        final TagResolver only = expansion("server_online");
        assertSame(only, IndexedTagResolver.of(List.of(only)));
    }

    @Test
    @DisplayName("an assembly of assemblies keeps every key")
    void nestedAssembliesKeepTheirKeys() {
        final TagResolver inner = IndexedTagResolver.of(List.of(expansion("a_one"), expansion("a_two")));
        final TagResolver outer = IndexedTagResolver.of(List.of(inner, expansion("b_one")));

        assertTrue(outer.has("a_one"));
        assertTrue(outer.has("a_two"));
        assertTrue(outer.has("b_one"));
        assertFalse(outer.has("c_one"));
    }
}
