package io.github.miniplaceholders.test;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that assembled resolvers are reused until expansion registration changes, and that
 * registration and removal invalidate every cache.
 */
class ResolverCacheTest implements MiniTest {

    @Test
    @DisplayName("consecutive calls reuse the same assembly")
    void repeatedCallsReuseTheAssembly() {
        Expansion.builder("cachereuse")
            .globalPlaceholder("value", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        assertSame(MiniPlaceholders.globalPlaceholders(), MiniPlaceholders.globalPlaceholders());
        assertSame(MiniPlaceholders.audienceGlobalPlaceholders(), MiniPlaceholders.audienceGlobalPlaceholders());
    }

    @Test
    @DisplayName("an expansion registered after the first lookup becomes visible")
    void registrationInvalidatesTheCache() {
        final TagResolver before = MiniPlaceholders.globalPlaceholders();
        assertFalse(before.has("cacheinvalidate_late"));

        Expansion.builder("cacheinvalidate")
            .globalPlaceholder("late", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        assertTrue(MiniPlaceholders.globalPlaceholders().has("cacheinvalidate_late"),
            "registration must invalidate the cache so new placeholders become visible");
    }

    @Test
    @DisplayName("an unregistered expansion is no longer visible")
    void unregistrationInvalidatesTheCache() {
        final Expansion expansion = Expansion.builder("cacheremoval")
            .globalPlaceholder("gone", (queue, ctx) -> Tags.EMPTY_TAG)
            .build();
        expansion.register();

        assertTrue(MiniPlaceholders.globalPlaceholders().has("cacheremoval_gone"));

        expansion.unregister();

        assertFalse(MiniPlaceholders.globalPlaceholders().has("cacheremoval_gone"),
            "unregistration must invalidate the cache so removed placeholders disappear");
    }

    @Test
    @DisplayName("each resolver assembly has its own cache entry")
    void eachAssemblyIsCachedSeparately() {
        Expansion.builder("cachedistinct")
            .audiencePlaceholder("aud", Tags.emptyAudienceResolver())
            .globalPlaceholder("glob", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        assertTrue(MiniPlaceholders.audiencePlaceholders().has("cachedistinct_aud"));
        assertFalse(MiniPlaceholders.audiencePlaceholders().has("cachedistinct_glob"));
        assertTrue(MiniPlaceholders.globalPlaceholders().has("cachedistinct_glob"));
        assertFalse(MiniPlaceholders.globalPlaceholders().has("cachedistinct_aud"));
        assertTrue(MiniPlaceholders.audienceGlobalPlaceholders().has("cachedistinct_aud"));
        assertTrue(MiniPlaceholders.audienceGlobalPlaceholders().has("cachedistinct_glob"));
    }
}
