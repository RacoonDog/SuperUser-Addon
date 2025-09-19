package io.github.racoondog.superuser.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.racoondog.superuser.OffthreadScheduler;
import io.github.racoondog.superuser.SuperUserCommandSource;
import io.github.racoondog.superuser.TickScheduler;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.meteordev.starscript.Script;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static meteordevelopment.meteorclient.commands.Commands.DISPATCHER;

public class ExecuteCommand extends Command {
    private static final SimpleCommandExceptionType CONDITIONAL_FAIL_EXCEPTION = new SimpleCommandExceptionType(
        Text.translatable("commands.execute.conditional.fail")
    );
    private CommandRegistryAccess registryAccess;

    public ExecuteCommand() {
        super("execute", "Flexible Meteor command execution.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        LiteralCommandNode<CommandSource> root = DISPATCHER.register(literal(this.getName()));
        registryAccess = mc.getNetworkHandler() != null
            ? CommandRegistryAccess.of(mc.getNetworkHandler().getRegistryManager(), mc.getNetworkHandler().getEnabledFeatures())
            : CommandRegistryAccess.of(BuiltinRegistries.createWrapperLookup(), FeatureSet.empty());

        builder.then(literal("run").redirect(DISPATCHER.getRoot()));

        if (mc.getNetworkHandler() != null) {
            builder.then(literal("send").then(argument("command", StringArgumentType.greedyString())
                .executes(ctx -> {
                    mc.getNetworkHandler().sendChatCommand(StringArgumentType.getString(ctx, "command"));
                    return 1;
                })
                /* todo fix suggestions
                .suggests((ctx, suggestionsBuilder) -> {
                    ParsedCommandNode<?> node = ctx.getNodes().getLast();
                    if (node.getNode() instanceof LiteralCommandNode<?>) return suggestionsBuilder.buildFuture();

                    int start = node.getRange().getStart();
                    String command = ctx.getInput().substring(
                        start,
                        Math.max(start, suggestionsBuilder.getStart())
                    );

                    CommandDispatcher<ClientCommandSource> dispatcher = mc.getNetworkHandler().getCommandDispatcher();
                    ClientCommandSource commandSource = (ClientCommandSource) ctx.getSource();
                    int cursor = suggestionsBuilder.getStart() - start;

                    StringReader commandReader = new StringReader(command);
                    commandReader.setCursor(cursor);

                    ParseResults<ClientCommandSource> results = dispatcher.parse(commandReader, commandSource);
                    return dispatcher.getCompletionSuggestions(results, cursor).thenApply(suggestions ->  new Suggestions(
                        new StringRange(
                            suggestions.getRange().getStart() + start,
                            suggestions.getRange().getEnd() + start
                        ),
                        suggestions.getList().stream().map(suggestion -> new Suggestion(
                            new StringRange(
                                suggestion.getRange().getStart() + start,
                                suggestion.getRange().getEnd() + start
                            ),
                            suggestion.getText(),
                            suggestion.getTooltip()
                            )).toList()
                    ));
                })
                 */
            ));
        }

        builder.then(addConditionArguments(root, literal("if"), true));
        builder.then(addConditionArguments(root, literal("unless"), false));

        builder.then(literal("delayed")
            .then(literal("ticks").then(ScheduleCommand.schedule(
                root, argument("delay", LongArgumentType.longArg(1)),
                (r, ctx) -> TickScheduler.INSTANCE.add(r, LongArgumentType.getLong(ctx, "delay"))
            )))
            .then(literal("millis").then(ScheduleCommand.schedule(
                root, argument("delay", LongArgumentType.longArg(1)),
                (r, ctx) -> OffthreadScheduler.INSTANCE.add(r, System.currentTimeMillis() + LongArgumentType.getLong(ctx, "delay"))
            )))
            .then(literal("seconds").then(ScheduleCommand.schedule(
                root, argument("delay", LongArgumentType.longArg(1)),
                (r, ctx) -> OffthreadScheduler.INSTANCE.add(r, System.currentTimeMillis() + LongArgumentType.getLong(ctx, "delay") * 1000L)
            )))
        );

        builder.then(literal("repeat").then(argument("times", IntegerArgumentType.integer(2))
            .then(literal("instant").fork(
                root,
                context -> Collections.nCopies(IntegerArgumentType.getInteger(context, "times"), context.getSource())
            ))
            .then(literal("ticks").then(addRepeatArguments(
                root, argument("delay", LongArgumentType.longArg(1)),
                (ctx, r, i, delay, now) -> TickScheduler.INSTANCE.add(r, delay * i)
            )))
            .then(literal("millis").then(addRepeatArguments(
                root, argument("delay", LongArgumentType.longArg(1)),
                (ctx, r, i, delay, now) -> OffthreadScheduler.INSTANCE.add(r, now + delay * i)
            )))
            .then(literal("seconds").then(addRepeatArguments(
                root, argument("delay", LongArgumentType.longArg(1)),
                (ctx, r, i, delay, now) -> OffthreadScheduler.INSTANCE.add(r, now + delay * 1000 * i)
            )))
        ));

        builder.then(literal("store")
            .then(addStoreArguments(root, literal("result"), true))
            .then(addStoreArguments(root, literal("success"), false))
        );
    }

    private ArgumentBuilder<CommandSource, ?> addConditionArguments(
        CommandNode<CommandSource> root,
        LiteralArgumentBuilder<CommandSource> builder,
        boolean positive
    ) {
        builder.then(literal("starscript")
            .then(addConditionLogic(
                root,
                argument("script", StringArgumentType.string()),
                positive,
                context -> {
                    @Nullable Script s = MeteorStarscript.compile(StringArgumentType.getString(context, "script"));
                    @Nullable String value;
                    return s != null && (value = MeteorStarscript.run(s)) != null && value.equalsIgnoreCase("true");
                }
            ))
        );

        /* todo proper registry access
        builder.then(literal("block")
            .then(argument("pos", BlockPosArgumentType.blockPos())
                .then(addConditionLogic(
                    root,
                    argument("block", BlockPredicateArgumentType.blockPredicate(registryAccess)),
                    positive,
                    context -> context.getArgument("block", BlockPredicateArgumentType.BlockPredicate.class)
                        .test(new CachedBlockPosition(mc.world, BlockPosArgumentType.getLoadedBlockPos(context, "pos"), true))
                ))
            )
        );
         */

        return builder;
    }

    private ArgumentBuilder<CommandSource, ?> addConditionLogic(
        CommandNode<CommandSource> root,
        ArgumentBuilder<CommandSource, ?> builder,
        boolean positive,
        Condition condition
    ) {
        return builder.fork(root, context -> getSourceOrEmptyForConditionFork(context, positive, condition.test(context))).executes(context -> {
            if (positive == condition.test(context)) {
                info("Test passed");
                return 1;
            } else {
                throw CONDITIONAL_FAIL_EXCEPTION.create();
            }
        });
    }

    private static Collection<CommandSource> getSourceOrEmptyForConditionFork(CommandContext<CommandSource> context, boolean positive, boolean value) {
        return value == positive ? List.of(context.getSource()) : List.of();
    }

    private ArgumentBuilder<CommandSource, ?> addRepeatArguments(
        CommandNode<CommandSource> root,
        ArgumentBuilder<CommandSource, ?> builder,
        Repeater repeater
    ) {
        return ScheduleCommand.schedule(
            root, builder,
            (r, ctx) -> {
                int times = IntegerArgumentType.getInteger(ctx, "times");
                long delay = LongArgumentType.getLong(ctx, "delay");
                long now = System.currentTimeMillis();
                for (int i = 0; i < times; i++) repeater.repeat(ctx, r, i, delay, now);
            }
        );
    }

    private ArgumentBuilder<CommandSource, ?> addStoreArguments( // todo defer result consumer on scheduled commands
        CommandNode<CommandSource> root, LiteralArgumentBuilder<CommandSource> builder, boolean requestResult
    ) {
        return builder.then(argument("variable", StringArgumentType.word()).redirect(
            root,
            context -> ((SuperUserCommandSource) context.getSource()).superuser$addResultConsumer((successful, returnValue) -> {
                String variable = StringArgumentType.getString(context, "variable");
                if (requestResult) {
                    MeteorStarscript.ss.set(variable, returnValue);
                } else {
                    MeteorStarscript.ss.set(variable, successful);
                }
            })
        ));
    }

    @FunctionalInterface
    interface Condition {
        boolean test(CommandContext<CommandSource> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface Repeater {
        void repeat(CommandContext<CommandSource> context, Runnable r, int index, long delay, long now) throws CommandSyntaxException;
    }
}
