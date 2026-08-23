package io.github.miniplaceholders.api.utils;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Class of utilities to manage TagResolvers.
 *
 * @since 1.2.0
 */
public final class Resolvers {
    private Resolvers() {}

    /**
     * Check if the TagResolver is an instance of TagResolver.empty(),
     * that is, it will always return null.
     *
     * @param resolver the resolver to check
     * @return true if the TagResolver is the same as TagResolver.empty(), else false
     * @since 1.2.0
     */
    public static boolean isEmpty(final TagResolver resolver) {
        return resolver == TagResolver.empty();
    }

    /**
     * Checks if the TagResolver provided is not a TagResolver.empty().
     *
     * @param resolver the resolver to check
     * @return false if the TagResolver is the same as TagResolver.empty(), else true
     * @since 1.2.0
     * 
     */
    public static boolean isNotEmpty(final TagResolver resolver) {
        return resolver != TagResolver.empty();
    }

    /**
     * Applies a TagResolver to a TagResolver.Builder in case it is not a TagResolver.empty().
     *
     * @param resolver the resolver to check
     * @param builder the resolver that can be added to the builder
     * @since 1.2.0
     */
    public static void applyIfNotEmpty(final TagResolver resolver, final TagResolver.Builder builder) {
        if (isNotEmpty(resolver)) {
            builder.resolver(resolver);
        }
    }

    /**
     * Collects a TagResolver into a list in case it is not a TagResolver.empty().
     *
     * <p>Use this rather than {@link #applyIfNotEmpty} when the caller must still inspect the
     * individual parts afterwards; a {@code TagResolver.Builder} folds them into an opaque
     * sequence.
     *
     * @param resolver the resolver to check
     * @param parts the list the resolver is appended to when it is not empty
     * @since 3.2.0
     */
    public static void collectIfNotEmpty(final TagResolver resolver, final java.util.List<TagResolver> parts) {
        if (isNotEmpty(resolver)) {
            parts.add(resolver);
        }
    }
}
