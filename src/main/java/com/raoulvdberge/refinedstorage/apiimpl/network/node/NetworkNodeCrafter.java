package com.raoulvdberge.refinedstorage.apiimpl.network.node;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.RSUtils;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternProvider;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerBasic;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerListenerNetworkNode;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerUpgrade;
import com.raoulvdberge.refinedstorage.item.ItemUpgrade;
import com.raoulvdberge.refinedstorage.tile.INetworkNodeHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.ArrayList;
import java.util.List;

public class NetworkNodeCrafter extends NetworkNode implements ICraftingPatternContainer {
    private static final String NBT_TRIGGERED_AUTOCRAFTING = "TriggeredAutocrafting";

    private ItemHandlerBasic patterns = new ItemHandlerBasic(9, new ItemHandlerListenerNetworkNode(this), s -> {
        // We can only validate the crafting pattern if the world exists.
        // If the world doesn't exist, this is probably called while reading and in that case it doesn't matter.
        if (holder.world() != null) {
            return s.getItem() instanceof ICraftingPatternProvider && ((ICraftingPatternProvider) s.getItem()).create(holder.world(), s, this).isValid();
        }

        return true;
    }) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);

            if (network != null && !holder.world().isRemote) {
                rebuildPatterns();
            }

            if (network != null) {
                network.rebuildPatterns();
            }
        }
    };

    private List<ICraftingPattern> actualPatterns = new ArrayList<>();

    private ItemHandlerUpgrade upgrades = new ItemHandlerUpgrade(4, new ItemHandlerListenerNetworkNode(this), ItemUpgrade.TYPE_SPEED);

    private boolean triggeredAutocrafting = false;

    public NetworkNodeCrafter(INetworkNodeHolder holder) {
        super(holder);
    }

    private void rebuildPatterns() {
        actualPatterns.clear();

        for (int i = 0; i < patterns.getSlots(); ++i) {
            ItemStack patternStack = patterns.getStackInSlot(i);

            if (!patternStack.isEmpty()) {
                ICraftingPattern pattern = ((ICraftingPatternProvider) patternStack.getItem()).create(holder.world(), patternStack, this);

                if (pattern.isValid()) {
                    actualPatterns.add(pattern);
                }
            }
        }
    }

    @Override
    public int getEnergyUsage() {
        int usage = RS.INSTANCE.config.crafterUsage + upgrades.getEnergyUsage();

        for (int i = 0; i < patterns.getSlots(); ++i) {
            if (!patterns.getStackInSlot(i).isEmpty()) {
                usage += RS.INSTANCE.config.crafterPerPatternUsage;
            }
        }

        return usage;
    }

    @Override
    public void update() {
        super.update();

        if (ticks == 1) {
            rebuildPatterns();
        }

        if (network != null && triggeredAutocrafting && holder.world().isBlockPowered(holder.pos())) {
            for (ICraftingPattern pattern : actualPatterns) {
                for (ItemStack output : pattern.getOutputs()) {
                    network.scheduleCraftingTask(output, 1, IComparer.COMPARE_DAMAGE | IComparer.COMPARE_NBT);
                }
            }
        }
    }

    @Override
    protected void onConnectedStateChange(INetworkMaster network, boolean state) {
        super.onConnectedStateChange(network, state);

        if (!state) {
            network.getCraftingTasks().stream()
                    .filter(task -> task.getPattern().getContainer().getPosition().equals(holder.pos()))
                    .forEach(network::cancelCraftingTask);
        }

        network.rebuildPatterns();
    }

    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);

        RSUtils.readItems(patterns, 0, tag);
        RSUtils.readItems(upgrades, 1, tag);
    }

    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        super.write(tag);

        RSUtils.writeItems(patterns, 0, tag);
        RSUtils.writeItems(upgrades, 1, tag);

        return tag;
    }

    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        super.writeConfiguration(tag);

        tag.setBoolean(NBT_TRIGGERED_AUTOCRAFTING, triggeredAutocrafting);

        return tag;
    }

    @Override
    public void readConfiguration(NBTTagCompound tag) {
        super.readConfiguration(tag);

        if (tag.hasKey(NBT_TRIGGERED_AUTOCRAFTING)) {
            triggeredAutocrafting = tag.getBoolean(NBT_TRIGGERED_AUTOCRAFTING);
        }
    }

    @Override
    public int getSpeedUpdateCount() {
        return upgrades.getUpgradeCount(ItemUpgrade.TYPE_SPEED);
    }

    @Override
    public IItemHandler getFacingInventory() {
        return RSUtils.getItemHandler(getFacingTile(), holder.getDirection().getOpposite());
    }

    @Override
    public List<ICraftingPattern> getPatterns() {
        return actualPatterns;
    }

    @Override
    public BlockPos getPosition() {
        return holder.pos();
    }

    public IItemHandler getPatternItems() {
        return patterns;
    }

    public IItemHandler getUpgrades() {
        return upgrades;
    }

    public boolean isTriggeredAutocrafting() {
        return triggeredAutocrafting;
    }

    public void setTriggeredAutocrafting(boolean triggeredAutocrafting) {
        this.triggeredAutocrafting = triggeredAutocrafting;
    }

    @Override
    public IItemHandler getDrops() {
        return new CombinedInvWrapper(patterns, upgrades);
    }

    @Override
    public boolean hasConnectivityState() {
        return true;
    }
}
