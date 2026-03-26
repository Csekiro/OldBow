package Csekiro.oldbow.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

public final class ClearArrowsCommand {
    private ClearArrowsCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("cleararrows")
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(ClearArrowsCommand::execute))
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(context, "targets");

        int affectedEntities = 0;
        int removedArrows = 0;
        int skippedEntities = 0;

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                skippedEntities++;
                continue;
            }

            int arrowCount = livingEntity.getStuckArrowCount();
            if (arrowCount > 0) {
                livingEntity.setStuckArrowCount(0);
                affectedEntities++;
                removedArrows += arrowCount;
            }
        }

        ServerCommandSource source = context.getSource();

        if (affectedEntities == 0) {
            if (skippedEntities > 0) {
                source.sendFeedback(() -> Text.literal("目标中没有可清除箭矢的活体实体。"), false);
            } else {
                source.sendFeedback(() -> Text.literal("目标身上没有可清除的箭矢。"), false);
            }
            return 0;
        }

        if (targets.size() == 1) {
            Entity target = targets.iterator().next();
            int finalRemovedArrows = removedArrows;
            source.sendFeedback(() -> Text.literal(
                    "已清除 " + target.getName().getString() + " 身上的 " + finalRemovedArrows + " 支箭矢。"
            ), false);
        } else {
            int finalAffectedEntities = affectedEntities;
            int finalRemovedArrows = removedArrows;
            int finalSkippedEntities = skippedEntities;

            if (finalSkippedEntities > 0) {
                source.sendFeedback(() -> Text.literal(
                        "已清除 " + finalAffectedEntities + " 个活体目标身上的箭矢，共 " + finalRemovedArrows +
                                " 支；跳过 " + finalSkippedEntities + " 个非活体实体。"
                ), false);
            } else {
                source.sendFeedback(() -> Text.literal(
                        "已清除 " + finalAffectedEntities + " 个目标身上的箭矢，共 " + finalRemovedArrows + " 支。"
                ), false);
            }
        }

        return affectedEntities;
    }
}