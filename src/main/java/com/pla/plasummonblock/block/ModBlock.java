package com.pla.plasummonblock.block;

import com.pla.plasummonblock.PlaSummonBlock;
import com.pla.plasummonblock.block.custom.EventCoinSacrificialAltar;
import com.pla.plasummonblock.item.ModItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlock {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PlaSummonBlock.MOD_ID);

    public static final RegistryObject<Block> EVENT_COIN_SACRIFICIAL_ALTAR = registerBlock("event_coin_sacrificial_altar",
            () -> new EventCoinSacrificialAltar(BlockBehaviour.Properties
                    .copy(Blocks.BEDROCK)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)) {

    });

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block>RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItem.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void  register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
