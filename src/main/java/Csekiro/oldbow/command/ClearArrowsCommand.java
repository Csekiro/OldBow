package Csekiro.oldbow.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public final class ClearArrowsCommand {
    private ClearArrowsCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("cleararrows")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(ClearArrowsCommand::execute))
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");

        int affectedPlayers = 0;
        int removedArrows = 0;

        for (ServerPlayerEntity player : targets) {
            int arrowCount = player.getStuckArrowCount();
            if (arrowCount > 0) {
                player.setStuckArrowCount(0);
                affectedPlayers++;
                removedArrows += arrowCount;
            }
        }

        ServerCommandSource source = context.getSource();

        if (affectedPlayers == 0) {
            source.sendFeedback(() -> Text.literal("目标身上没有可清除的箭矢。"), false);
            return 0;
        }

        if (targets.size() == 1) {
            ServerPlayerEntity player = targets.iterator().next();
            int finalRemovedArrows = removedArrows;
            source.sendFeedback(() -> Text.literal(
                    "已清除 " + player.getName().getString() + " 身上的 " + finalRemovedArrows + " 支箭矢。"
            ), false);
        } else {
            int finalAffectedPlayers = affectedPlayers;
            int finalRemovedArrows = removedArrows;
            source.sendFeedback(() -> Text.literal(
                    "已清除 " + finalAffectedPlayers + " 名玩家身上的箭矢，共 " + finalRemovedArrows + " 支。"
            ), false);
        }

        return affectedPlayers;
    }
}