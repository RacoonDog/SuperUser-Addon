package io.github.racoondog.superuser;

import com.mojang.brigadier.ResultConsumer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ReturnValueConsumer;

public interface SuperUserCommandSource {
    CommandSource superuser$addResultConsumer(ReturnValueConsumer consumer);
    void superuser$consumeResult(boolean successful, int returnValue);

    static <S extends CommandSource> ResultConsumer<S> asResultConsumer() {
        return (context, success, result) -> {
            if (context.getSource() instanceof SuperUserCommandSource superUserSource) {
                superUserSource.superuser$consumeResult(success, result);
            }
        };
    }
}
