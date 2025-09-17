package io.github.racoondog.superuser.commands;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.racoondog.superuser.OffthreadScheduler;
import io.github.racoondog.superuser.SuperUserCommandSource;
import io.github.racoondog.superuser.TickScheduler;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import java.util.List;

import static meteordevelopment.meteorclient.commands.Commands.DISPATCHER;

public class ScheduleCommand extends Command {
    public ScheduleCommand() {
        super("schedule", "Schedules a command to be executed later.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> literalArgumentBuilder) {
        literalArgumentBuilder.then(literal("ticks").then(schedule(
            DISPATCHER.getRoot(), argument("delay", LongArgumentType.longArg(1)),
            (r, ctx) -> TickScheduler.INSTANCE.add(r, LongArgumentType.getLong(ctx, "delay"))
        )));

        literalArgumentBuilder.then(literal("millis").then(schedule(
            DISPATCHER.getRoot(), argument("delay", LongArgumentType.longArg(1)),
            (r, ctx) -> OffthreadScheduler.INSTANCE.add(r, System.currentTimeMillis() + LongArgumentType.getLong(ctx, "delay"))
        )));

        literalArgumentBuilder.then(literal("seconds").then(schedule(
            DISPATCHER.getRoot(), argument("delay", LongArgumentType.longArg(1)),
            (r, ctx) -> OffthreadScheduler.INSTANCE.add(r, System.currentTimeMillis() + LongArgumentType.getLong(ctx, "delay") * 1000L)
        )));
    }

    public static ArgumentBuilder<CommandSource, ?> schedule(
        CommandNode<CommandSource> root,
        ArgumentBuilder<CommandSource, ?> builder,
        Scheduler scheduler
    ) {
        return builder.forward(
            root,
            context -> {
                CommandContext<CommandSource> childContext = context.getChild();
                CommandSource source = context.getSource();

                scheduler.schedule(() -> ContextChain.tryFlatten(childContext).ifPresent(chain -> {
                    try {
                        chain.executeAll(source, SuperUserCommandSource.asResultConsumer());
                    } catch (CommandSyntaxException e) {
                        ChatUtils.error(e.getMessage());
                    }
                }), context);

                return List.of();
            },
            false
        );
    }

    @FunctionalInterface
    public interface Scheduler {
        void schedule(Runnable runnable, CommandContext<CommandSource> context) throws CommandSyntaxException;
    }
}
