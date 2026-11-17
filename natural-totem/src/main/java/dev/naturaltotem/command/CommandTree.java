package dev.naturaltotem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.naturaltotem.config.Option;
import dev.naturaltotem.config.Options;
import dev.naturaltotem.config.Presets;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Registra la forma del comando en el árbol que usa el cliente para autocompletar.
 *
 * <p>El cliente nunca ejecuta este árbol: el comando se atiende antes de enviarse. Sirve solo
 * para que al escribir {@code /ntotem } salgan las órdenes y los ajustes con el tabulador, y
 * para que el texto no se pinte en rojo como si fuera un comando desconocido.
 *
 * <p>El árbol del servidor se reemplaza entero en cada {@code CommandTreeS2CPacket}, así que
 * esto vuelve a llamarse cada vez que llega uno.
 */
public final class CommandTree {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "status", "on", "off", "toggle", "list", "get", "set", "reset", "preset", "save", "reload");

    private CommandTree() {
    }

    public static <S extends CommandSource> void register(CommandDispatcher<S> dispatcher) {
        for (String alias : TotemCommand.ALIASES) {
            dispatcher.register(build(alias));
        }
    }

    private static <S extends CommandSource> LiteralArgumentBuilder<S> build(String alias) {
        SuggestionProvider<S> keys = (context, builder) ->
                CommandSource.suggestMatching(Options.keysStartingWith(builder.getRemaining()), builder);
        SuggestionProvider<S> keysAndAll = (context, builder) -> {
            List<String> candidates = new ArrayList<>(Options.keysStartingWith(builder.getRemaining()));
            if ("all".startsWith(builder.getRemaining())) {
                candidates.add("all");
            }
            return CommandSource.suggestMatching(candidates, builder);
        };
        SuggestionProvider<S> values = (context, builder) ->
                CommandSource.suggestMatching(valuesFor(context.getInput()), builder);

        LiteralArgumentBuilder<S> root = LiteralArgumentBuilder.<S>literal(alias).executes(context -> 1);
        for (String subcommand : SUBCOMMANDS) {
            LiteralArgumentBuilder<S> node = LiteralArgumentBuilder.<S>literal(subcommand).executes(context -> 1);
            switch (subcommand) {
                case "get" -> node.then(argument("ajuste", keys));
                case "reset" -> node.then(argument("ajuste", keysAndAll));
                case "list" -> node.then(argument("texto", keys));
                case "preset" -> node.then(argument("grupo", (context, builder) ->
                        CommandSource.suggestMatching(Presets.names(), builder)));
                case "set" -> node.then(argument("ajuste", keys).then(valueArgument(values)));
                default -> {
                    // Las demás órdenes no llevan argumentos.
                }
            }
            root.then(node);
        }
        // Atajo: /ntotem <ajuste> <valor>
        root.then(argument("ajuste", keys).then(valueArgument(values)));
        return root;
    }

    private static <S extends CommandSource> RequiredArgumentBuilder<S, String> argument(
            String name, SuggestionProvider<S> suggestions) {
        return RequiredArgumentBuilder.<S, String>argument(name, StringArgumentType.word())
                .suggests(suggestions)
                .executes(context -> 1);
    }

    private static <S extends CommandSource> RequiredArgumentBuilder<S, String> valueArgument(
            SuggestionProvider<S> suggestions) {
        return RequiredArgumentBuilder.<S, String>argument("valor", StringArgumentType.greedyString())
                .suggests(suggestions)
                .executes(context -> 1);
    }

    /**
     * Propone los valores de la opción que se está escribiendo.
     *
     * <p>Se deduce del texto completo en vez de leer el argumento ya analizado porque, mientras
     * se escribe, el analizador todavía no tiene el valor y pedirlo lanzaría una excepción.
     */
    static List<String> valuesFor(String input) {
        List<String> words = TotemCommand.split(input);
        for (int index = words.size() - 1; index >= 0; index--) {
            Option<?> option = Options.byKey(words.get(index)).orElse(null);
            if (option != null) {
                return option.suggestions();
            }
        }
        return List.of();
    }
}
