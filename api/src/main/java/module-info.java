import org.jspecify.annotations.NullMarked;

/**
 * MiniPlaceholders API Module
 */
@NullMarked
open module io.github.miniplaceholders.api {
  requires io.github.miniplaceholders.connect;

  // Adventure 5 renamed the module `net.kyori.adventure` to `net.kyori.adventure.api`, split the
  // Key types into their own module, and dropped examination altogether.
  requires net.kyori.adventure.api;
  requires net.kyori.adventure.key;
  requires net.kyori.adventure.text.minimessage;
  requires net.kyori.adventure.text.serializer.legacy;

  requires static org.jetbrains.annotations;
  requires static org.jspecify;

  exports io.github.miniplaceholders.api;
  exports io.github.miniplaceholders.api.types;
  exports io.github.miniplaceholders.api.utils;
  exports io.github.miniplaceholders.api.resolver;
  exports io.github.miniplaceholders.api.placeholder;
  exports io.github.miniplaceholders.api.provider;
}
