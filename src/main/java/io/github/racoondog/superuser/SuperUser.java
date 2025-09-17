package io.github.racoondog.superuser;

import com.mojang.logging.LogUtils;
import io.github.racoondog.superuser.commands.ExecuteCommand;
import io.github.racoondog.superuser.commands.ScheduleCommand;
import io.github.racoondog.superuser.commands.StarscriptCommand;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import org.slf4j.Logger;

public class SuperUser extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

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
