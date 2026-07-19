package com.example.wave_motion_gun.client.renderer;

import com.example.wave_motion_gun.ExampleMod;
import com.example.wave_motion_gun.entity.MessengerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class MessengerRenderer extends HumanoidMobRenderer<MessengerEntity, PlayerModel<MessengerEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ExampleMod.MODID, "textures/entity/messenger4.png");

    public MessengerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MessengerEntity entity) {
        return TEXTURE;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(MessengerEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}