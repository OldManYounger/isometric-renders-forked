package net.oldmanyounger.isometricrendersforked.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.oldmanyounger.isometricrendersforked.IsometricRendersForked;
import net.oldmanyounger.isometricrendersforked.property.DefaultPropertyBundle;
import net.oldmanyounger.isometricrendersforked.property.IntProperty;
import net.oldmanyounger.isometricrendersforked.util.ExportPathSpec;
import net.oldmanyounger.isometricrendersforked.util.ParticleRestriction;
import org.jetbrains.annotations.Nullable;

/**
 * Renderable wrapper for a single entity.
 *
 * <p>Entities can be created from a type, from command-provided NBT, or by
 * copying the entity currently targeted by the player.</p>
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
        return of(type, null);
    }

    // Creates a client-side entity renderable from an entity type and optional NBT.
    public static @Nullable EntityRenderable of(EntityType<?> type, @Nullable CompoundTag nbt) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        Entity entity = createEntity(type, nbt, minecraft.level);

        if (entity == null) {
            return null;
        }

        placeNearPlayer(entity);
        entity.setOldPosAndRot();

        return new EntityRenderable(entity);
    }

    // Copies the targeted entity into a standalone renderable entity.
    public static @Nullable EntityRenderable copyOf(Entity source) {
        CompoundTag nbt = source.saveWithoutId(new CompoundTag());
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());

        if (entityId != null) {
            nbt.putString("id", entityId.toString());
        }

        return of(source.getType(), nbt);
    }

    // Creates an entity from NBT when supplied, otherwise from the entity type.
    private static @Nullable Entity createEntity(EntityType<?> type, @Nullable CompoundTag nbt, Level level) {
        if (nbt == null) {
            return type.create(level);
        }

        CompoundTag copiedNbt = nbt.copy();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        if (entityId != null) {
            copiedNbt.putString("id", entityId.toString());
        }

        try {
            Entity entity = EntityType.loadEntityRecursive(copiedNbt, level, loadedEntity -> loadedEntity);

            if (entity != null) {
                return entity;
            }
        } catch (RuntimeException exception) {
            IsometricRendersForked.LOGGER.warn("Failed to load entity NBT for {}", entityId, exception);
        }

        return type.create(level);
    }

    // Places render-only entities near the client player for renderer context.
    private static void placeNearPlayer(Entity entity) {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            entity.moveTo(player.getX(), player.getY(), player.getZ(), entity.getYRot(), entity.getXRot());
        } else {
            entity.moveTo(0.0D, 0.0D, 0.0D, entity.getYRot(), entity.getXRot());
        }
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

    // Entity particles are allowed only during renderable ticks.
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
