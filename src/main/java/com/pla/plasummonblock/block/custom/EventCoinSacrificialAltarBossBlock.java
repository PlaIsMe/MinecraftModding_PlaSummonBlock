package com.pla.plasummonblock.block.custom;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.pla.plasummonblock.config.PlaSummonBlockConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class EventCoinSacrificialAltarBossBlock extends Block {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final IntegerProperty EVENT_COINS_USED = IntegerProperty.create("event_coin_used", 0, 15);
    public static final IntegerProperty EVENT_COINS_LIMIT = IntegerProperty.create("event_coin_limit", 5, 15);

    public EventCoinSacrificialAltarBossBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(defaultBlockState()
                .setValue(EVENT_COINS_USED, 0)
                .setValue(EVENT_COINS_LIMIT, 5));
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(ENABLED)) {
            float chance = 0.35f;
            if (chance < pRandom.nextFloat()) {
                pLevel.addParticle(ParticleTypes.END_ROD, pPos.getX() + pRandom.nextDouble(),
                        pPos.getY() + 0.5D, pPos.getZ() + pRandom.nextDouble(),
                        pRandom.nextDouble() * 0.2 - 0.1, 0d, pRandom.nextDouble() * 0.2 - 0.1);
            }
        }
        super.animateTick(pState, pLevel, pPos, pRandom);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos,
                                 Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide) {
            ItemStack itemInHand = pPlayer.getItemInHand(pHand);
            Item eventCoinItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("economy", "event_coin_3"));

            if (!pState.getValue(ENABLED)) {
                pPlayer.displayClientMessage(
                        Component.literal("The altar is charging with mystical energy. Come back later!")
                                .withStyle(style -> style.withColor(0xFFFF00)),
                        true
                );
                return InteractionResult.SUCCESS;
            }
            if (itemInHand.is(eventCoinItem)) {
                itemInHand.shrink(1);

                int coinsUsed = pState.getValue(EVENT_COINS_USED);
                int coinsLimit = pState.getValue(EVENT_COINS_LIMIT);

                coinsUsed++;
                pLevel.setBlock(pPos, pState.setValue(EVENT_COINS_USED, coinsUsed), 3);
                pLevel.playSound(null, pPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);

                pPlayer.displayClientMessage(Component.literal("Coins used: " + coinsUsed + "/" + coinsLimit)
                        .withStyle(style -> style.withColor(0x00FF00)), true);

                if (coinsUsed == coinsLimit) {
                    pLevel.setBlock(pPos, pState.setValue(ENABLED, false)
                            .setValue(EVENT_COINS_USED, 0), 3);

                    CommandSourceStack source = pPlayer.createCommandSourceStack();
                    try {
                        int spawnX = pPos.getX();
                        int spawnY = pPos.getY();
                        int spawnZ = pPos.getZ();

                        List<? extends String> bosses = PlaSummonBlockConfig.BOSSES.get();
                        String randomMonster = bosses.get(pLevel.random.nextInt(bosses.size()));

                        String summonCommand = "summon " + randomMonster + " "
                                + spawnX + " " + (spawnY + 1) + " " + spawnZ
                                + " {Tags:[\"temporary_mob_" + spawnX + "_" + spawnY + "_" + spawnZ + "\"]}";
                        Objects.requireNonNull(pLevel.getServer()).getCommands().getDispatcher().execute(summonCommand, source);

                        String particlesCommand = "particle minecraft:explosion_emitter" + " "
                                + spawnX + " " + spawnY + " " + spawnZ;
                        Objects.requireNonNull(pLevel.getServer()).getCommands().getDispatcher().execute(particlesCommand, source);

                        pLevel.playSound(null, pPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.0f, 1.0f);

                    } catch (CommandSyntaxException e) {
                        LOGGER.error("Failed to execute command !!!", e);
                    }
                    Integer tickReset = PlaSummonBlockConfig.TICK_RESET.get();
                    pLevel.scheduleTick(pPos, this, tickReset);
                }
                return InteractionResult.SUCCESS;
            } else {
                pPlayer.displayClientMessage(Component.literal("Please use an event coin to interact with this altar.")
                        .withStyle(style -> style.withColor(0xFF0000)), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ENABLED, EVENT_COINS_USED, EVENT_COINS_LIMIT);
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        pLevel.getServer().execute(() -> {
            String uniqueTag = "temporary_mob_" + pPos.getX() + "_" + pPos.getY() + "_" + pPos.getZ();
            pLevel.getEntities((Entity) null, new AABB(pPos).inflate(50), entity -> entity.getTags().contains(uniqueTag))
                    .forEach(entity -> entity.discard());
        });

        Random random = new Random();
        int randomCoinLimit = random.nextInt(11) + 5;

        pLevel.setBlock(pPos, pState
                .setValue(ENABLED, true)
                .setValue(EVENT_COINS_USED, 0)
                .setValue(EVENT_COINS_LIMIT, randomCoinLimit),3);
        super.tick(pState, pLevel, pPos, pRandom);
    }
}
