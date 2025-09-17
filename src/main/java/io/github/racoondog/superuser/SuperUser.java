package io.github.racoondog.superuser;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.github.racoondog.superuser.commands.ExecuteCommand;
import io.github.racoondog.superuser.commands.ScheduleCommand;
import io.github.racoondog.superuser.commands.StarscriptCommand;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import net.minecraft.command.ReturnValueConsumer;
import org.slf4j.Logger;

import java.util.WeakHashMap;

public class SuperUser extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final WeakHashMap<CommandContext<?>, ReturnValueConsumer> COMMAND_DELAYED_CONSUMER_MAP = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        Commands.DISPATCHER.setConsumer(SuperUserCommandSource.asResultConsumer());

        Commands.add(new ExecuteCommand());
        Commands.add(new ScheduleCommand());
        Commands.add(new StarscriptCommand());
    }

    @Override
    public String getPackage() {
        return "io.github.racoondog.superuser";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("RacoonDog", "SuperUser-Addon");
    }
}
