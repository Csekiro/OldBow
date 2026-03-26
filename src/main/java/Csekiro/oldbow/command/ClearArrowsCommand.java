package Csekiro.oldbow.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ClearArrowsCommand {
    private ClearArrowsCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("cleararrows")
                                .requires(source -> source.getEntity() instanceof ServerPlayerEntity)
                                .executes(context -> execute(context.getSource()))
                )
        );
    }

    private static int execute(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        int arrowCount = player.getStuckArrowCount();

        if (arrowCount <= 0) {
            source.sendFeedback(() -> Text.literal("你身上没有插着的箭矢。"), false);
            return Command.SINGLE_SUCCESS;
        }

        player.setStuckArrowCount(0);
        source.sendFeedback(() -> Text.literal("已清除你身上的 " + arrowCount + " 支箭矢。"), false);
        return Command.SINGLE_SUCCESS;
    }
}