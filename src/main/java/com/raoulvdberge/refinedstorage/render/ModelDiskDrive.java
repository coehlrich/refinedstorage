package com.raoulvdberge.refinedstorage.render;

import com.google.common.base.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.Collection;
import java.util.Collections;

public class ModelDiskDrive implements IModel {
    private static final ResourceLocation MODEL_BASE = new ResourceLocation("refinedstorage:block/disk_drive");

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.singletonList(MODEL_BASE);
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return Collections.emptyList();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        IModel baseModel;

        try {
            baseModel = ModelLoaderRegistry.getModel(MODEL_BASE);
        } catch (Exception e) {
            throw new Error("Unable to load disk drive base model", e);
        }

        return new BakedModelDiskDrive(baseModel.bake(state, format, bakedTextureGetter));
    }

    @Override
    public IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }
}