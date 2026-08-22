package io.github.miniplaceholders.test;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiniPlaceholdersTest implements MiniTest {
    /**
     * Composing the two resolvers by hand must behave like asking for the combined one.
     *
     * <p>This assertion used to be {@code assertEquals} on the resolvers themselves. That no
     * longer holds, and the change is deliberate: {@code audienceGlobalPlaceholders()} now returns
     * an {@code IndexedTagResolver}, which answers {@code has} from one set instead of walking one
     * resolver per expansion, while {@code TagResolver.resolver(a, b)} still builds Adventure's
     * sequential resolver. The two are no longer the same object shape.
     *
     * <p>Structural equality between composed resolvers is therefore a contract this fork breaks.
     * It is stated here rather than hidden, because it is the point a maintainer would raise if
     * the change is proposed upstream. What the API promises in practice — the same answers to
     * {@code has} and the same tags from {@code resolve} — is what is asserted below.
     */
    @Test
    void methodEquality(){
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
