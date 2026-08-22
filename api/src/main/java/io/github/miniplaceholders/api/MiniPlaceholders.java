package io.github.miniplaceholders.api;

import io.github.miniplaceholders.api.types.PlaceholderType;
import io.github.miniplaceholders.api.types.Platform;
import io.github.miniplaceholders.api.types.RelationalAudience;
import io.github.miniplaceholders.connect.InternalPlatform;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import io.github.miniplaceholders.api.placeholder.IndexedTagResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.miniplaceholders.api.utils.Resolvers.applyIfNotEmpty;
import static io.github.miniplaceholders.api.utils.Resolvers.collectIfNotEmpty;

/**
 * MiniPlaceholders, a component-based placeholders API.
 *
 * <p>This class allows you to obtain the {@link TagResolver}
 * that other plugins provide based on single {@link Audience},
 * 2-audience or global placeholders</p>
 *
 * @author 4drian3d
 * @see TagResolver
 * @see Expansion
 * @since 1.0.0
 */
public final class MiniPlaceholders {
    private MiniPlaceholders() {}
    static final Set<Expansion> expansions = ConcurrentHashMap.newKeySet();

    /**
     * An assembled resolver together with the registry version it was built from.
     *
     * <p>The resolver-returning methods below are called per message by consumers that inspect
     * tags, so rebuilding the assembly on every call is not affordable. Two threads may build
     * concurrently; both produce an equivalent resolver and the last write wins, which is why no
     * lock is taken on this path.
     */
    private record CachedResolver(int version, TagResolver resolver) {}

    private static volatile int registryVersion = 0;
    private static volatile CachedResolver globalCache;
    private static volatile CachedResolver audienceCache;
    private static volatile CachedResolver relationalCache;
    private static volatile CachedResolver audienceGlobalCache;
    private static volatile CachedResolver relationalGlobalCache;

    /** Appele a chaque enregistrement ou retrait d'expansion. */
    static void invalidateResolverCache() {
        registryVersion++;
    }

    /**
     * Get the platform on which MiniPlaceholders is running.
     *
     * @return the platform
     * @since 3.0.0
     */
    public static Platform platform() {
        return switch (InternalPlatform.platform()) {
            case PAPER -> Platform.PAPER;
            case VELOCITY -> Platform.VELOCITY;
            case FABRIC -> Platform.FABRIC;
            case SPONGE -> Platform.SPONGE;
            case MINESTOM -> Platform.MINESTOM;
        };
    }

    /**
     * Get the global placeholders
     *
     * <pre>TagResolver resolver = MiniPlaceholders.globalPlaceholders();
     * Component messageParsed = MiniMessage.miniMessage().deserialize({@link String}, resolver);</pre>
     *
     * @return global placeholders independent of any audience
     * @see TagResolver
     * @since 3.0.0
     */
    public static TagResolver globalPlaceholders() {
        final int version = registryVersion;
        final CachedResolver cached = globalCache;
        if (cached != null && cached.version() == version) {
            return cached.resolver();
        }
        final List<TagResolver> parts = new ArrayList<>(expansions.size());
        for (final Expansion expansion : expansions) {
            collectIfNotEmpty(expansion.globalPlaceholders(), parts);
        }
        final TagResolver built = IndexedTagResolver.of(parts);
        globalCache = new CachedResolver(version, built);
        return built;
    }

    /**
     * Gets the TagResolver that can get data from an Audience.
     * <br>
     * The audience is provided at the time of parsing from the respective MiniMessage instance.
     *
     * <pre>TagResolver resolver = MiniPlaceholders.audiencePlaceholders();
     * Component messageParsed = MiniMessage.miniMessage().deserialize({@link String}, {@link Audience}, resolver);
     * </pre>
     *
     * @return {@link TagResolver} with placeholders based on an audience
     * @since 3.0.0
     */
    public static TagResolver audiencePlaceholders() {
        final int version = registryVersion;
        final CachedResolver cached = audienceCache;
        if (cached != null && cached.version() == version) {
            return cached.resolver();
        }
        final List<TagResolver> parts = new ArrayList<>(expansions.size());
        for (final Expansion expansion : expansions) {
            collectIfNotEmpty(expansion.audiencePlaceholders(), parts);
        }
        final TagResolver built = IndexedTagResolver.of(parts);
        audienceCache = new CachedResolver(version, built);
        return built;
    }

    /**
     * Get the relational placeholders based on two audiences
     * <br>
     * The audiences are provided at the time of parsing
     * from the respective MiniMessage instance through the use of a {@link RelationalAudience}.
     *
     * <pre>{@code
     *      TagResolver resolver = MiniPlaceholders.relationalPlaceholders();
     *      Component parsed = MiniMessage.miniMessage().deserialize(@link String, {@link RelationalAudience}, resolver);
     * }</pre>
     *
     * @return placeholders based on two audiences
     * @since 3.0.0
     * @see RelationalAudience
     */
    public static TagResolver relationalPlaceholders() {
        final int version = registryVersion;
        final CachedResolver cached = relationalCache;
        if (cached != null && cached.version() == version) {
            return cached.resolver();
        }
        final List<TagResolver> parts = new ArrayList<>(expansions.size());
        for (final Expansion expansion : expansions) {
            collectIfNotEmpty(expansion.relationalPlaceholders(), parts);
        }

        final TagResolver built = IndexedTagResolver.of(parts);
        relationalCache = new CachedResolver(version, built);
        return built;
    }

    /**
     * Get a TagResolver that can obtain data based on a relationship of 2 audiences
     * and at the same time from the main audience and global placeholders.
     * <br>
     * The audience is provided at the time of parsing from the respective MiniMessage instance.
     *
     * <pre>
     * TagResolver resolver = MiniPlaceholders.audienceGlobalPlaceholders();
     * TagResolver resolver2 = TagResolver.resolver(
     *  MiniPlaceholders.audienceGlobalPlaceholders(),
     *  MiniPlaceholders.globalPlaceholders()
     * );
     * // This two resolvers returns the same TagResolver
     * assertEquals(resolver, resolver2);
     * Component parsed = MiniMessage.miniMessage().deserialize({@link String}, {@link Audience}, resolver);
     * </pre>
     *
     * @return {@link TagResolver} with placeholders based on an audience and the global placeholders
     * @since 1.1.0
     */
    public static TagResolver audienceGlobalPlaceholders() {
        final int version = registryVersion;
        final CachedResolver cached = audienceGlobalCache;
        if (cached != null && cached.version() == version) {
            return cached.resolver();
        }
        final List<TagResolver> parts = new ArrayList<>(expansions.size() * 2);

        for (final Expansion expansion : expansions) {
            collectIfNotEmpty(expansion.audiencePlaceholders(), parts);
            collectIfNotEmpty(expansion.globalPlaceholders(), parts);
        }

        final TagResolver built = IndexedTagResolver.of(parts);
        audienceGlobalCache = new CachedResolver(version, built);
        return built;
    }

    /**
     * Get the relational placeholders based on two audiences, based on the first audience,
     * and the global placeholders
     *
     * <pre>
     * TagResolver resolver = MiniPlaceholders.relationalGlobalPlaceholders();
     * TagResolver resolver2 = TagResolver.resolver(
     *  MiniPlaceholders.relationalPlaceholders(),
     *  MiniPlaceholders.audiencePlaceholders(),
     *  MiniPlaceholders.globalPlaceholders()
     * );
     * // This methods should return the same TagResolver
     * assertEquals(resolver, resolver2);
     * Component messageParsed = MiniMessage.miniMessage().deserialize({@link String}, {@link RelationalAudience}, resolver);
     * </pre>
     *
     * @return the placeholders based on two audiences, placeholders based on the first audience and the global placeholders
     * @since 3.0.0
     * @apiNote In the case of audience placeholders, the audience to be used will be the {@link RelationalAudience#audience()}
     * @see RelationalAudience
     */
    public static TagResolver relationalGlobalPlaceholders() {
        final int version = registryVersion;
        final CachedResolver cached = relationalGlobalCache;
        if (cached != null && cached.version() == version) {
            return cached.resolver();
        }
        final List<TagResolver> parts = new ArrayList<>(expansions.size() * 3);
        for (final Expansion expansion : expansions) {
            collectIfNotEmpty(expansion.audiencePlaceholders(), parts);
            collectIfNotEmpty(expansion.relationalPlaceholders(), parts);
            collectIfNotEmpty(expansion.globalPlaceholders(), parts);
        }

        final TagResolver built = IndexedTagResolver.of(parts);
        relationalGlobalCache = new CachedResolver(version, built);
        return built;
    }

    /**
     * Get a TagResolver based on the desired placeholder type.
     * <br>
     * {@link PlaceholderType#GLOBAL} will return {@link #globalPlaceholders()}
     * <br>
     * {@link PlaceholderType#AUDIENCE} will return {@link #audiencePlaceholders()}
     * <br>
     * {@link PlaceholderType#RELATIONAL} will return {@link #relationalPlaceholders()}
     *
     * @param type the desired type
     * @return the TagResolvers from the respective type
     * @see PlaceholderType
     */
    public static TagResolver placeholdersByType(final PlaceholderType type) {
        return switch (type) {
            case GLOBAL -> globalPlaceholders();
            case AUDIENCE -> audiencePlaceholders();
            case RELATIONAL -> relationalPlaceholders();
        };
    }

    /**
     * Get the amount of expansion registered.
     *
     * @return the amount of expansions registered
     * @since 3.0.0
     */
    public static int expansionCount(){
        return expansions.size();
    }

    /**
     * Get a specific expansion by name
     * <p>The name of each expansion is set when the expansion is created</p>
     * <pre>
     *     Expansion expansion = Expansion.builder("example").build();
     *     expansion.register();
     *
     *     assertThat(MiniPlaceholders.expansionByName("example")).isNotNull();
     * </pre>
     *
     * @param name the name of the required expansion
     * @return the required expansion, if not present, will return null
     * @see Expansion#builder(String)
     * @since 3.0.0
     */
    public static @Nullable Expansion expansionByName(final String name) {
        for (final Expansion expansion : expansions) {
            if (Objects.equals(expansion.name(), name)) {
                return expansion;
            }
        }
        return null;
    }

    /**
     * Obtain all available registered expansions
     *
     * @return all available registered expansions
     * @since 3.0.0
     */
    @Unmodifiable
    public static Collection<Expansion> expansionsAvailable() {
        return Collections.unmodifiableSet(expansions);
    }
}
