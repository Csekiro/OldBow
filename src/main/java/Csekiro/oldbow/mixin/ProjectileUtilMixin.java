package Csekiro.oldbow.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    private static final double EXTRA_HITBOX = 0.15;

    // 只在“弹射物实体碰撞计算”调用链期间开启，避免影响别的 raycast/检测
    private static final ThreadLocal<Boolean> OB$ENABLED = ThreadLocal.withInitial(() -> false);

    // ---------- 开关：ProjectileEntity 重载 ----------
    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            require = 0
    )
    private static void ob$enableProjectileOverload(
            World world, ProjectileEntity projectile, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        OB$ENABLED.set(true);
    }

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("RETURN"),
            require = 0
    )
    private static void ob$disableProjectileOverload(
            World world, ProjectileEntity projectile, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        OB$ENABLED.set(false);
    }

    // broad phase：把候选查询 Box 也扩大一点，保证“扩大的目标 hitbox”不会因为候选集太小而漏掉
    @ModifyVariable(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4,
            require = 0
    )
    private static Box ob$expandQueryBoxProjectileOverload(Box box) {
        return box.expand(EXTRA_HITBOX);
    }

    // ---------- 开关：Entity + margin 重载（有些版本/调用会走这个） ----------
    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            require = 0
    )
    private static void ob$enableEntityMarginOverload(
            World world, Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, float margin,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        if (entity instanceof ProjectileEntity) OB$ENABLED.set(true);
    }

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("RETURN"),
            require = 0
    )
    private static void ob$disableEntityMarginOverload(
            World world, Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, float margin,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        if (entity instanceof ProjectileEntity) OB$ENABLED.set(false);
    }

    @ModifyVariable(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4,
            require = 0
    )
    private static Box ob$expandQueryBoxEntityMarginOverload(Box box, World world, Entity entity) {
        return (entity instanceof ProjectileEntity) ? box.expand(EXTRA_HITBOX) : box;
    }

    // ---------- 关键：把“目标实体”的 getBoundingBox() 临时 expand(0.3)，但目标是箭则不扩 ----------
    // 同时覆盖 getEntityCollision / raycast 里所有对 getBoundingBox 的调用；通过 ThreadLocal 开关确保只影响弹射物碰撞计算。
    @Redirect(
            method = {
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
                    "raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"),
            require = 0
    )
    private static Box ob$expandTargetAabb(Entity target) {
        Box original = target.getBoundingBox();

        if (!Boolean.TRUE.equals(OB$ENABLED.get())) return original;

        EntityType<?> type = target.getType();
        if (type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW) return original;

        return original.expand(EXTRA_HITBOX);
    }
}