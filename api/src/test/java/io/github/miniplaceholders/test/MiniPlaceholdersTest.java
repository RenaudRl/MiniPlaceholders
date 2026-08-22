package io.github.miniplaceholders.test;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiniPlaceholdersTest implements MiniTest {
    /**
     * Composing audience and global resolvers manually produces the same observable membership
     * results as the combined API resolver.
     */
    @Test
    void combinedResolverMatchesManualComposition(){
        Expansion.builder("equality")
            .audiencePlaceholder("audience", Tags.emptyAudienceResolver())
            .globalPlaceholder("global", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
        .register();

        final TagResolver combined = MiniPlaceholders.audienceGlobalPlaceholders();
        final TagResolver composed = TagResolver.resolver(
            MiniPlaceholders.audiencePlaceholders(),
            MiniPlaceholders.globalPlaceholders()
        );

        for (final String name : new String[]{"equality_audience", "equality_global", "absent_tag"}) {
            assertEquals(composed.has(name), combined.has(name), name);
        }
        assertTrue(combined.has("equality_audience"));
        assertTrue(combined.has("equality_global"));
        assertFalse(combined.has("absent_tag"));
    }

    @Test
    void registrationTest() {
        Expansion expansion = Expansion.builder("testregistration")
                .build();
        expansion.register();

        assertNotNull(MiniPlaceholders.expansionByName("testregistration"));

        assertTrue(expansion.registered());
        assertThrows(IllegalStateException.class, expansion::register);

        assertDoesNotThrow(expansion::unregister);
    }
}
