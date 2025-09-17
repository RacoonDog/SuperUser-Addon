package io.github.racoondog.superuser.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.Nullable;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.Section;
import org.meteordev.starscript.compiler.Compiler;
import org.meteordev.starscript.compiler.Parser;
import org.meteordev.starscript.utils.Error;
import org.meteordev.starscript.utils.StarscriptError;
import org.meteordev.starscript.value.Value;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StarscriptCommand extends Command {
    private static final DynamicCommandExceptionType STARSCRIPT_EXCEPTION = new DynamicCommandExceptionType(value -> new LiteralMessage("Error evaluating Starscript expression: " + value));

    public StarscriptCommand() {
        super("starscript", "", "ss");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("eval").then(argument("script", StringArgumentType.greedyString()).executes(ctx -> {
            Parser.Result parseResult = Parser.parse(StringArgumentType.getString(ctx, "script"));

            if (parseResult.hasErrors()) {
                throw STARSCRIPT_EXCEPTION.create(parseResult.errors.stream().map(Error::toString).collect(Collectors.joining("\n")));
            }

            Script script = Compiler.compile(parseResult);

            try {
                // todo ensure thread safety
                Section result = MeteorStarscript.ss.run(script);
                if (result == null) {
                    throw STARSCRIPT_EXCEPTION.create("Null return value");
                }

                String resultString = result.toString();
                info("Expression evaluated to (highlight)" + resultString);

                try {
                    return Integer.parseInt(resultString);
                } catch (NumberFormatException e) {
                    return SINGLE_SUCCESS;
                }
            } catch (StarscriptError e) {
                throw STARSCRIPT_EXCEPTION.create(e.getMessage());
            }
        })));

        builder.then(literal("get").then(argument("variable", StringArgumentType.word()).executes(ctx -> {
            String variable = StringArgumentType.getString(ctx, "variable");

            @Nullable Supplier<Value> supplier = MeteorStarscript.ss.getGlobals().get(variable);

            if (supplier == null) {
                info("Variable does not exist.");
                return 0;
            } else {
                Value value = supplier.get();

                info("Variable is (highlight)" + value);
                return value.isNumber() ? (int) value.getNumber() : SINGLE_SUCCESS;
            }
        })));
    }
}
