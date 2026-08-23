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
 * Le cache des resolvers assembles, et surtout son invalidation.
 *
 * <p>Un cache mal invalide est un defaut muet : les placeholders d'une expansion enregistree apres
 * le premier appel ne sont jamais vus, aucune erreur n'est levee, et le profil s'ameliore. C'est
 * la meme forme de piege que l'index sur {@code has} — un correctif qui repond « non » a tort est
 * recompense par la mesure. Ces tests sont ecrits avant la mesure pour cette raison.
 */
class ResolverCacheTest implements MiniTest {

    @Test
    @DisplayName("deux appels de suite rendent le meme objet — l'assemblage n'est pas refait")
    void repeatedCallsReuseTheAssembly() {
        Expansion.builder("cachereuse")
            .globalPlaceholder("value", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        // C'est ce que le banc a sanctionne : l'assemblage etait refait a chaque message.
        assertSame(MiniPlaceholders.globalPlaceholders(), MiniPlaceholders.globalPlaceholders());
        assertSame(MiniPlaceholders.audienceGlobalPlaceholders(), MiniPlaceholders.audienceGlobalPlaceholders());
    }

    @Test
    @DisplayName("une expansion enregistree apres le premier appel est vue")
    void registrationInvalidatesTheCache() {
        final TagResolver before = MiniPlaceholders.globalPlaceholders();
        assertFalse(before.has("cacheinvalidate_late"));

        Expansion.builder("cacheinvalidate")
            .globalPlaceholder("late", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        assertTrue(MiniPlaceholders.globalPlaceholders().has("cacheinvalidate_late"),
            "le cache n'a pas ete invalide a l'enregistrement : les nouveaux placeholders sont invisibles");
    }

    @Test
    @DisplayName("une expansion retiree cesse d'etre vue")
    void unregistrationInvalidatesTheCache() {
        final Expansion expansion = Expansion.builder("cacheremoval")
            .globalPlaceholder("gone", (queue, ctx) -> Tags.EMPTY_TAG)
            .build();
        expansion.register();

        assertTrue(MiniPlaceholders.globalPlaceholders().has("cacheremoval_gone"));

        expansion.unregister();

        assertFalse(MiniPlaceholders.globalPlaceholders().has("cacheremoval_gone"),
            "le cache n'a pas ete invalide au retrait : une expansion disparue repond encore");
    }

    @Test
    @DisplayName("les cinq assemblages ont chacun leur entree de cache")
    void eachAssemblyIsCachedSeparately() {
        Expansion.builder("cachedistinct")
            .audiencePlaceholder("aud", Tags.emptyAudienceResolver())
            .globalPlaceholder("glob", (queue, ctx) -> Tags.EMPTY_TAG)
            .build()
            .register();

        // Si les cinq partageaient une entree, l'une ecraserait l'autre et le contenu
        // dependrait de l'ordre des appels.
        assertTrue(MiniPlaceholders.audiencePlaceholders().has("cachedistinct_aud"));
        assertFalse(MiniPlaceholders.audiencePlaceholders().has("cachedistinct_glob"));
        assertTrue(MiniPlaceholders.globalPlaceholders().has("cachedistinct_glob"));
        assertFalse(MiniPlaceholders.globalPlaceholders().has("cachedistinct_aud"));
        assertTrue(MiniPlaceholders.audienceGlobalPlaceholders().has("cachedistinct_aud"));
        assertTrue(MiniPlaceholders.audienceGlobalPlaceholders().has("cachedistinct_glob"));
    }
}
