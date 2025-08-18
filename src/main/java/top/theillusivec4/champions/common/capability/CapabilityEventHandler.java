package top.theillusivec4.champions.common.capability;

import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import top.theillusivec4.champions.api.affix.IAffix;
import top.theillusivec4.champions.api.IChampion;
import top.theillusivec4.champions.common.config.ChampionsConfig;
import top.theillusivec4.champions.common.network.NetworkHandler;
import top.theillusivec4.champions.common.network.SPacketSyncChampion;
import top.theillusivec4.champions.common.rank.Rank;
import top.theillusivec4.champions.common.rank.RankManager;
import top.theillusivec4.champions.common.util.ChampionBuilder;
import top.theillusivec4.champions.common.util.ChampionHelper;

import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CapabilityEventHandler {

    @SubscribeEvent
    public void attachCapabilities(final AttachCapabilitiesEvent<Entity> evt) {
        Entity entity = evt.getObject();

        if (ChampionHelper.isValidChampionEntity(entity)) {
            evt.addCapability(ChampionCapability.ID,
                    ChampionCapability.createProvider((LivingEntity) entity));
        }
    }

    /**
     * Preset of special spawn ranks
     * @param evt the finalizeSpawn event
     */
    @SubscribeEvent
    public void onSpecialSpawn(MobSpawnEvent.FinalizeSpawn evt) {
        LivingEntity entity = evt.getEntity();

        if (!entity.level().isClientSide()) {
            ChampionCapability.getCapability(entity).ifPresent(champion -> {
                IChampion.Server serverChampion = champion.getServer();

                if (serverChampion.getRank().isEmpty()) {
                    // Todo: Custom entity spawn rank base on mob spawn type
                    if (evt.getSpawnType() == MobSpawnType.SPAWNER) {
                        // 只对刷怪笼生成的怪物应用championSpawners配置
                        if (!ChampionsConfig.championSpawners) {
                            serverChampion.setRank(RankManager.getLowestRank());
                        } else {
                            // 使用概率配置来决定是否生成精英怪
                            RandomSource random = evt.getLevel().getRandom();
                            if (random.nextDouble() < ChampionsConfig.spawnerChampionChance) {
                                ChampionBuilder.spawn(champion);
                            } else {
                                serverChampion.setRank(RankManager.getLowestRank());
                            }
                        }
                    } else if (evt.getSpawnType() == MobSpawnType.SPAWN_EGG) {
                        // 刷怪蛋生成的怪物不受championSpawners配置限制，直接生成精英怪
                        // 如果刷怪蛋已经有预设的精英数据，ChampionBuilder.spawn会保持这些数据
                        ChampionBuilder.spawn(champion);
                    } else {
                        // 其他生成方式（自然生成等）正常处理
                        ChampionBuilder.spawn(champion);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onLivingConvert(LivingConversionEvent.Post evt) {
        LivingEntity entity = evt.getEntity();

        if (!entity.level().isClientSide()) {
            entity.reviveCaps();
            LivingEntity outcome = evt.getOutcome();
            ChampionCapability.getCapability(entity).ifPresent(
                    oldChampion -> {
                        if (ChampionHelper.isValidChampion(oldChampion.getServer())) {
                            ChampionCapability.getCapability(outcome)
                                    .ifPresent(newChampion -> {
                                        ChampionBuilder.copy(oldChampion, newChampion);
                                        IChampion.Server serverChampion = newChampion.getServer();
                                        NetworkHandler.syncChampionDataToPlayerTrackingEntity(serverChampion, outcome);
                                    });
                        }
                    });
            entity.invalidateCaps();
        }
    }

    @SubscribeEvent
    public void startTracking(PlayerEvent.StartTracking evt) {
        Entity entity = evt.getTarget();
        Player playerEntity = evt.getEntity();

        if (playerEntity instanceof ServerPlayer serverPlayer) {
            ChampionCapability.getCapability(entity).ifPresent(champion -> {
                IChampion.Server serverChampion = champion.getServer();
                if (ChampionHelper.isValidChampion(serverChampion)) {
                    NetworkHandler.INSTANCE
                            .send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                    new SPacketSyncChampion(entity.getId(),
                                            serverChampion.getRank().map(Rank::getTier).orElse(0),
                                            serverChampion.getRank().map(Rank::getDefaultColor).orElse(TextColor.fromRgb(0)).toString(),
                                            serverChampion.getAffixes().stream().map(IAffix::getIdentifier)
                                                    .collect(Collectors.toSet())));
                }
            });
        }
    }
}
