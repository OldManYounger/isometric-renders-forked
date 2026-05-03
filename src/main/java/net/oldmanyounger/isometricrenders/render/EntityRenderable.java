package net.oldmanyounger.isometricrenders.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.oldmanyounger.isometricrenders.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrenders.property.IntProperty;
import net.oldmanyounger.isometricrenders.util.ExportPathSpec;
import net.oldmanyounger.isometricrenders.util.ParticleRestriction;
import org.jetbrains.annotations.Nullable;

/**
 * Renderable wrapper for a single entity.
 *
 * <p>This initial NeoForge port supports creating a fresh client-side entity
 * from an entity type. NBT-driven entity creation and passenger copying are
 * deferred until the basic entity render path is verified.</p>
 */
public class EntityRenderable extends DefaultRenderable<EntityRenderable.EntityPropertyBundle> implements TickingRenderable<EntityRenderable.EntityPropertyBundle> {
    private final Entity entity;

    /**
     * Creates an entity renderable.
     *
     * @param entity the entity to render
     */
    public EntityRenderable(Entity entity) {
        this.entity = entity;
    }

    // Creates a client-side entity renderable from an entity type.
    public static @Nullable EntityRenderable of(EntityType<?> type) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        Entity entity = type.create(minecraft.level);

        if (entity == null) {
            return null;
        }

        var player = minecraft.player;

        if (player != null) {
            entity.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        } else {
            entity.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        }

        entity.setOldPosAndRot();

        return new EntityRenderable(entity);
    }

    // Disables entity shadows before preview/export rendering.
    @Override
    public void prepare() {
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(false);
    }

    // Emits this entity's vertices through Minecraft's entity render dispatcher.
    @Override
    public void emitVertices(PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var minecraft = Minecraft.getInstance();

        poseStack.pushPose();

        // Center the entity vertically and face it toward the preview camera.
        poseStack.translate(0.0F, -0.5F * this.entity.getBbHeight(), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        this.applyPoseProperties();

        minecraft.getEntityRenderDispatcher().render(
                this.entity,
                0.0D,
                0.0D,
                0.0D,
                this.properties().yaw.get(),
                tickDelta,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT
        );

        poseStack.popPose();
    }

    // Restores entity shadows after rendering.
    @Override
    public void cleanUp() {
        Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(true);
    }

    // Returns the entity-specific transform properties.
    @Override
    public EntityPropertyBundle properties() {
        return EntityPropertyBundle.INSTANCE;
    }

    // Entity particles will eventually be allowed only during renderable ticks.
    @Override
    public ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.duringTick();
    }

    // Builds the default export path for this entity.
    @Override
    public ExportPathSpec exportPath() {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType());

        if (entityId == null) {
            entityId = ResourceLocation.fromNamespaceAndPath("unknown", "entity");
        }

        return ExportPathSpec.ofIdentified(entityId, "entity");
    }

    // Advances entity state by one tick when animation ticking is restored.
    @Override
    public void tick() {
        this.entity.tick();
    }

    // Applies yaw and pitch properties to the entity before rendering.
    private void applyPoseProperties() {
        float yaw = this.properties().yaw.get();
        float pitch = this.properties().pitch.get();

        this.entity.setYRot(yaw);
        this.entity.setXRot(pitch);
        this.entity.yRotO = yaw;
        this.entity.xRotO = pitch;

        if (this.entity instanceof LivingEntity livingEntity) {
            livingEntity.setYHeadRot(yaw);
            livingEntity.yHeadRotO = yaw;
            livingEntity.setYBodyRot(yaw);
            livingEntity.yBodyRotO = yaw;
        }
    }

    // Returns the rendered entity instance.
    public Entity entity() {
        return this.entity;
    }

    /**
     * Entity-specific render properties.
     */
    public static class EntityPropertyBundle extends DefaultPropertyBundle {
        public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

        public final IntProperty yaw = IntProperty.of(0, -180, 180).withRollover();
        public final IntProperty pitch = IntProperty.of(0, -90, 90).withRollover();

        private EntityPropertyBundle() {}
    }
}
