package com.raoulvdberge.refinedstorage.apiimpl.network.node;

import com.raoulvdberge.refinedstorage.RSUtils;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.api.network.INetworkNeighborhoodAware;
import com.raoulvdberge.refinedstorage.api.network.INetworkNode;
import com.raoulvdberge.refinedstorage.api.util.IWrenchable;
import com.raoulvdberge.refinedstorage.tile.INetworkNodeHolder;
import com.raoulvdberge.refinedstorage.tile.config.RedstoneMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class NetworkNode implements INetworkNode, INetworkNeighborhoodAware, IWrenchable {
    protected boolean rebuildOnUpdateChange;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    @Nullable
    protected INetworkMaster network;
    private boolean couldUpdate;
    protected int ticks;
    protected INetworkNodeHolder holder;

    private boolean active;

    public NetworkNode(INetworkNodeHolder holder) {
        this.holder = holder;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode redstoneMode) {
        this.redstoneMode = redstoneMode;
    }

    @Nonnull
    @Override
    public ItemStack getItemStack() {
        IBlockState state = holder.world().getBlockState(holder.pos());

        Item item = Item.getItemFromBlock(state.getBlock());

        return new ItemStack(item, 1, state.getBlock().getMetaFromState(state));
    }

    @Override
    public void onConnected(INetworkMaster network) {
        onConnectedStateChange(network, true);

        this.network = network;
    }

    @Override
    public void onDisconnected(INetworkMaster network) {
        this.network = null;

        onConnectedStateChange(network, false);
    }

    protected void onConnectedStateChange(INetworkMaster network, boolean state) {
    }

    @Override
    public void markDirty() {
        // @todo
    }

    @Override
    public boolean canUpdate() {
        return redstoneMode.isEnabled(holder.world(), holder.pos());
    }

    @Override
    public void update() {
        ++ticks;

        boolean canUpdate = getNetwork() != null && canUpdate();

        if (couldUpdate != canUpdate) {
            couldUpdate = canUpdate;

            if (hasConnectivityState()) {
                RSUtils.updateBlock(holder.world(), holder.pos());
            }

            if (network != null) {
                onConnectedStateChange(network, couldUpdate);

                if (rebuildOnUpdateChange) {
                    network.getNodeGraph().rebuild();
                }
            }
        }
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        writeConfiguration(tag);

        return tag;
    }

    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        return tag;
    }

    @Override
    public void read(NBTTagCompound tag) {
        readConfiguration(tag);
    }

    @Override
    public void readConfiguration(NBTTagCompound tag) {
    }

    @Nullable
    @Override
    public INetworkMaster getNetwork() {
        return network;
    }

    @Override
    public BlockPos getPos() {
        return holder.pos();
    }

    public boolean canConduct(@Nullable EnumFacing direction) {
        return true;
    }

    @Override
    public void walkNeighborhood(Operator operator) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (canConduct(facing)) {
                operator.apply(holder.world(), holder.pos().offset(facing), facing.getOpposite());
            }
        }
    }

    public TileEntity getFacingTile() {
        return holder.world().getTileEntity(holder.pos().offset(holder.getDirection()));
    }

    public IItemHandler getDrops() {
        return null;
    }

    public boolean hasConnectivityState() {
        return false;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NetworkNode
                && holder.pos().equals(((NetworkNode) o).holder.pos())
                && holder.world().provider.getDimension() == ((NetworkNode) o).holder.world().provider.getDimension();
    }

    @Override
    public int hashCode() {
        int result = holder.pos().hashCode();
        result = 31 * result + holder.world().provider.getDimension();
        return result;
    }
}
