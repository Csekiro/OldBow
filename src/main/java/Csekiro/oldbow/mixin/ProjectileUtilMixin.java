package Csekiro.oldbow.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @Unique
    private static final double EXTRA_HITBOX = 0.15D;

    /**
     * 用计数器代替布尔值，避免嵌套调用时状态错乱。
     */
    @Unique
    private static final ThreadLocal<Integer> OB$DEPTH = ThreadLocal.withInitial(() -> 0);

    @Unique
    private static void ob$push() {
        OB$DEPTH.set(OB$DEPTH.get() + 1);
    }

    @Unique
    private static void ob$pop() {
        int v = OB$DEPTH.get() - 1;
        if (v <= 0) {
            OB$DEPTH.remove();
        } else {
            OB$DEPTH.set(v);
        }
    }

    @Unique
    private static boolean ob$enabled() {
        return OB$DEPTH.get() > 0;
    }

    /**
     * 只对白名单目标扩张命中盒。
     * 这里选择 LivingEntity。
     * 关键：绝不扩张 ProjectileEntity，
     * 否则两个弹射物擦身而过时会因为“扩张后的盒子”被误判为命中。
     */
    @Unique
    private static boolean ob$shouldExpandTarget(Entity target) {
        return ob$enabled()
                && target instanceof LivingEntity;
    }

    // ---------- 开关：ProjectileEntity 重载 ----------

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            require = 0
    )
    private static void ob$enableProjectileOverload(
            World world,
            ProjectileEntity projectile,
            Vec3d min,
            Vec3d max,
            Box box,
            Predicate<Entity> predicate,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        ob$push();
    }

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("RETURN"),
            require = 0
    )
    private static void ob$disableProjectileOverload(
            World world,
            ProjectileEntity projectile,
            Vec3d min,
            Vec3d max,
            Box box,
            Predicate<Entity> predicate,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        ob$pop();
    }

    /**
     * broad phase：扩大候选查询 Box。
     * 这样扩张后的目标盒不会因为候选集过小而漏判。
     */
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

    // ---------- 开关：Entity + margin 重载（部分版本/调用链会走这里） ----------

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("HEAD"),
            require = 0
    )
    private static void ob$enableEntityMarginOverload(
            World world,
            Entity entity,
            Vec3d min,
            Vec3d max,
            Box box,
            Predicate<Entity> predicate,
            float margin,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        if (entity instanceof ProjectileEntity) {
            ob$push();
        }
    }

    @Inject(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At("RETURN"),
            require = 0
    )
    private static void ob$disableEntityMarginOverload(
            World world,
            Entity entity,
            Vec3d min,
            Vec3d max,
            Box box,
            Predicate<Entity> predicate,
            float margin,
            CallbackInfoReturnable<EntityHitResult> cir
    ) {
        if (entity instanceof ProjectileEntity) {
            ob$pop();
        }
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

    // ---------- 关键修复 ----------
    // 只对白名单目标临时扩张 AABB。
    // 不扩张任何 ProjectileEntity，避免弹射物之间产生假命中。

    @Redirect(
            method = {
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;",
                    "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
                    "raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"
            ),
            require = 0
    )
    private static Box ob$expandTargetAabb(Entity target) {
        Box original = target.getBoundingBox();

        if (!ob$shouldExpandTarget(target)) {
            return original;
        }

        return original.expand(EXTRA_HITBOX);
    }
}