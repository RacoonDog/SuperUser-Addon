package io.github.racoondog.superuser.mixin;

import io.github.racoondog.superuser.SuperUserCommandSource;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ReturnValueConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientCommandSource.class)
public class ClientCommandSourceMixin implements SuperUserCommandSource {
    @Unique private ReturnValueConsumer consumer = ReturnValueConsumer.EMPTY;

    @Override
    public CommandSource superuser$addResultConsumer(ReturnValueConsumer consumer) {
        this.consumer = ReturnValueConsumer.chain(this.consumer, consumer);
        return (CommandSource) this;
    }

    @Override
    public void superuser$consumeResult(boolean successful, int returnValue) {
        this.consumer.onResult(successful, returnValue);
        this.consumer = ReturnValueConsumer.EMPTY;
    }
}
