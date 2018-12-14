package de.dosmike.sponge.EventSounds.sounds;

import com.google.common.collect.ImmutableSet;
import com.itwookie.utils.Expiring;
import de.dosmike.sponge.EventSounds.EventSounds;
import de.dosmike.sponge.EventSounds.config.SoundCollector;
import de.dosmike.sponge.EventSounds.events.PlaySoundEvent;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.entity.damage.DamageType;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public class EventSoundRegistry {

    /**
     * When a player joins the server
     */
    public static final String EVENT_JOIN = "join";
    /**
     * Triggered by writing specific messages into chat
     */
    public static final String EVENT_CHAT = "chat";
    /**
     * When someone seems to just no be killable... surely hacks ;)
     */
    public static final String EVENT_KILLSTREAK = "killstreak";
    /**
     * When a player kills other players in quick succession.
     * Global Cooldown is disabled, Client Cooldown is timeout.
     */
    public static final String EVENT_MULTIKILL = "multikill";
    /**
     * When a player breaks the killstreak of another player with at least X kills
     */
    public static final String EVENT_COMBOBREAKER = "combobreaker";
    /**
     * When a player dies outside of PvP
     */
    public static final String EVENT_DEATH = "death";
    /**
     * This Category is only meant for auto-packing.<br>
     * Sounds in this group have no defined play behaviour, but will be added to the resource-pack.<br>
     * You can play the sound later with SoundTypes.of("es.plugin."+mySoundName).
     */
    public static final String EVENT_PLUGIN = "plugin";

    /** collects all events the plugin EventSounds itself reacts to */
    private static final Set<String> Events = new ImmutableSet.Builder<String>()
            .add(EVENT_JOIN)
            .add(EVENT_CHAT)
            .add(EVENT_KILLSTREAK)
            .add(EVENT_MULTIKILL)
            .add(EVENT_COMBOBREAKER)
            .add(EVENT_DEATH)
            .build();
    /** @return the names of events the plugin EventSounds itself reacts to */
    public static Collection<String> getStandardEvents() {
        return Events;
    }

    //event -> options
    private static Map<String, SoundOptions> event_options = new HashMap<>();

    //event -> sounds
    private static Map<String, Map<BiFitness<Integer, Player, Object>, Playable>> event_sounds = new HashMap<>();

    //event -> cooldowns
    private static Map<String, Refrigerator> event_cooldowns = new HashMap<>();

    private static NamedCounters<UUID> quake_killstreaks = new NamedCounters<>();
    private static Map<UUID, Expiring<Integer>> quake_multikills = new HashMap<>();
    private static int getMultiKillCount(UUID userid) {
        int timeout = (int)(event_options.get(EVENT_MULTIKILL).getCooldownClient()*1000);
        int multi = 1;
        Expiring<Integer> count = quake_multikills.get(userid);
        if (count != null && count.isAlive()) {
            multi = count.get()+1;
        }
        quake_multikills.put(userid, Expiring.expireIn(multi, timeout));
        return multi;
    }

    private static Set<String> saySoundTriggers = new TreeSet<>(Comparator.naturalOrder());
    public static Collection<String> listSaySounds() {
        return saySoundTriggers;
    }

    public static class Entry<K,V> implements Map.Entry<K,V> {
        K k; V v;
        public Entry(K k, V v) {
            this.k = k;
            this.v = v;
        }
        @Override
        public K getKey() {
            return k;
        }
        @Override
        public V getValue() {
            return v;
        }
        @Override
        public V setValue(V value) {
            V o = v;
            v = value;
            return o;
        }
        public static <L,R> Entry<L,R> of(L k, R v) {
            return new Entry<>(k,v);
        }
    }

    public static void triggerJoin(Player player) {
        SoundOptions options = event_options.get(EVENT_JOIN);
        Map<BiFitness<Integer, Player, Object>, Playable> sounds = event_sounds.get(EVENT_JOIN);
        Refrigerator fridge = event_cooldowns.get(EVENT_JOIN);

        if (!fridge.test(player)) return;
        Map<Playable, Integer> fitness = new HashMap<>();
        Optional<Playable> sound = sounds.entrySet().stream()
                .map(e -> Entry.of(e.getValue(), e.getKey().test(player,null)))
                .filter(e->e.getValue()>=0) //filter out failed tests
                .max((o1, o2)->{
                    int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                    if (testCompare == 0) //tests returned same value
                        //so return the one with higher priority
                        return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                    else
                        //return the one with higher test score
                        return testCompare;
                }).map(Entry::getKey);
        if (!sound.isPresent()) return;

        PlaySoundEvent event = new PlaySoundEvent(player, null, sound.get());
        try (CauseStackManager.StackFrame ignore = Sponge.getCauseStackManager().pushCauseFrame()) {
            if (Sponge.getEventManager().post(event))
                return;
        }

        if (options.getDelayMs() > 0) {
            EventSounds.getExecutor().schedule(()->sound.get().play(player, null), options.getDelayMs(), TimeUnit.MILLISECONDS);
        } else {
            sound.get().play(player, null);
        }
        fridge.put(player);
    }

    public static void triggerChat(Player player, String message) {
        SoundOptions options = event_options.get(EVENT_CHAT);
        Map<BiFitness<Integer, Player, Object>, Playable> sounds = event_sounds.get(EVENT_CHAT);
        Refrigerator fridge = event_cooldowns.get(EVENT_CHAT);

        if (!fridge.test(player)) return;
        Optional<Playable> sound = sounds.entrySet().stream()
                .map(e -> Entry.of(e.getValue(), e.getKey().test(player, message)))
                .filter(e->e.getValue()>=0) //filter out failed tests
                .max((o1, o2)->{
                    int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                    if (testCompare == 0) //tests returned same value
                        //so return the one with higher priority
                        return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                    else
                        //return the one with higher test score
                        return testCompare;
                }).map(Entry::getKey);
        if (!sound.isPresent()) return;

        PlaySoundEvent event = new PlaySoundEvent(player, null, sound.get());
        try (CauseStackManager.StackFrame ignore = Sponge.getCauseStackManager().pushCauseFrame()) {
            if (Sponge.getEventManager().post(event))
                return;
        }

        if (options.getDelayMs() > 0) {
            EventSounds.getExecutor().schedule(()->sound.get().play(player, null), options.getDelayMs(), TimeUnit.MILLISECONDS);
        } else {
            sound.get().play(player, null);
        }
        fridge.put(player);
    }

    public static void triggerQuake(Player player, Player victim) {

        Playable target = null;
        SoundOptions targetOptions = null;
        Set<Refrigerator> relatedCooldowns = new HashSet<>();

        int victimKills = quake_killstreaks.getValue(victim.getUniqueId());
        int sourceKills = quake_killstreaks.inrement(player.getUniqueId());
        //combo breaker
        if (event_cooldowns.get(EVENT_COMBOBREAKER).test(player)) {
            Optional<Playable> sound = event_sounds.get(EVENT_COMBOBREAKER).entrySet().stream()
                    .map(e -> Entry.of(e.getValue(), e.getKey().test(player,victimKills)))
                    .filter(e->e.getValue()>=0) //filter out failed tests
                    .max((o1, o2)->{
                        int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                        if (testCompare == 0) //tests returned same value
                            //so return the one with higher priority
                            return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                        else
                            //return the one with higher test score
                            return testCompare;
                    }).map(Entry::getKey);
            if (sound.isPresent()) {
                target = sound.get();
                targetOptions = event_options.get(EVENT_COMBOBREAKER);
                relatedCooldowns.add(event_cooldowns.get(EVENT_COMBOBREAKER));
            }
        }
        //multi kill
        int multi = getMultiKillCount(player.getUniqueId());
        if (multi>1) {
            Optional<Playable> sound = event_sounds.get(EVENT_MULTIKILL).entrySet().stream()
                    .map(e -> Entry.of(e.getValue(), e.getKey().test(player,multi)))
                    .filter(e->e.getValue()>=0) //filter out failed tests
                    .max((o1, o2)->{
                        int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                        if (testCompare == 0) //tests returned same value
                            //so return the one with higher priority
                            return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                        else
                            //return the one with higher test score
                            return testCompare;
                    }).map(Entry::getKey);
            if (sound.isPresent()) {
                if (target==null) {
                    target = sound.get();
                    targetOptions = event_options.get(EVENT_MULTIKILL);
                }
                //multikill has no cooldown since it has to happen within a certain timeframe
            }
        }
        //kill streak
        if (event_cooldowns.get(EVENT_KILLSTREAK).test(player)) {
            Optional<Playable> sound = event_sounds.get(EVENT_KILLSTREAK).entrySet().stream()
                    .map(e -> Entry.of(e.getValue(), e.getKey().test(player,sourceKills)))
                    .filter(e->e.getValue()>=0) //filter out failed tests
                    .max((o1, o2)->{
                        int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                        if (testCompare == 0) //tests returned same value
                            //so return the one with higher priority
                            return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                        else
                            //return the one with higher test score
                            return testCompare;
                    }).map(Entry::getKey);
            if (sound.isPresent()) {
                if (target==null) {
                    target = sound.get();
                    targetOptions = event_options.get(EVENT_KILLSTREAK);
                }
                //put in fridge anyways, since it was triggered, but overshadowed by more infrequent event
                relatedCooldowns.add(event_cooldowns.get(EVENT_KILLSTREAK));
            }
        }

        if (target != null) {
            PlaySoundEvent event = new PlaySoundEvent(player, victim, target);
            try (CauseStackManager.StackFrame ignore = Sponge.getCauseStackManager().pushCauseFrame()) {
                if (Sponge.getEventManager().post(event))
                    return;
            }
            for (Refrigerator cd : relatedCooldowns) cd.put(player);

            final Playable fsound = target;
            if (targetOptions.getDelayMs() > 0) {
                EventSounds.getExecutor().schedule(()->fsound.play(player, victim), targetOptions.getDelayMs(), TimeUnit.MILLISECONDS);
            } else {
                target.play(player, victim);
            }
        }

    }

    public static void triggerDeath(Player player, Object reason) {
        SoundOptions options = event_options.get(EVENT_DEATH);
        Map<BiFitness<Integer, Player, Object>, Playable> sounds = event_sounds.get(EVENT_JOIN);
        Refrigerator fridge = event_cooldowns.get(EVENT_JOIN);

        if (!fridge.test(player)) return;
        Optional<Playable> sound = sounds.entrySet().stream()
                .map(e -> Entry.of(e.getValue(), e.getKey().test(player, reason)))
                .filter(e->e.getValue()>=0) //filter out failed tests
                .max((o1, o2)->{
                    int testCompare = Integer.compare(o1.getValue(),o2.getValue());
                    if (testCompare == 0) //tests returned same value
                        //so return the one with higher priority
                        return Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
                    else
                        //return the one with higher test score
                        return testCompare;
                }).map(Entry::getKey);
        if (!sound.isPresent()) return;

        PlaySoundEvent event = new PlaySoundEvent(player, null, sound.get());
        try (CauseStackManager.StackFrame ignore = Sponge.getCauseStackManager().pushCauseFrame()) {
            if (Sponge.getEventManager().post(event))
                return;
        }

        if (options.getDelayMs() > 0) {
            EventSounds.getExecutor().schedule(()->sound.get().play(player, null), options.getDelayMs(), TimeUnit.MILLISECONDS);
        } else {
            sound.get().play(player, null);
        }
        fridge.put(player);
    }

    public static void resetPlayer(Player player) {
        quake_killstreaks.reset(player.getUniqueId());
        quake_multikills.remove(player.getUniqueId());
    }

    public static void resetAll() {
        event_options.clear();
        event_sounds.clear();
        event_cooldowns.clear();
        for (String event : Events) {
            event_options.put(event, SoundOptions.getDefault());
            event_sounds.put(event, new HashMap<>());
            event_cooldowns.put(event, new Refrigerator(0,0));
        }
        quake_multikills.clear();
        quake_killstreaks = new NamedCounters<>();
        saySoundTriggers.clear();
        sounds.clear();
    }
    public static void parseConfigurationNode(String event, ConfigurationNode cfgEvent) {
        if (!event.matches("[a-z]+"))
            throw new IllegalArgumentException("Event names have to be all lowercase letters (a-z)");

        SoundOptions p_options = SoundOptions.getDefault();
        Map<BiFitness<Integer, Player, Object>, Playable> p_sounds = new HashMap<>();

        for (Map.Entry<Object, ? extends ConfigurationNode> e : cfgEvent.getChildrenMap().entrySet()) {
            String key = (String)e.getKey();
            if ("_options".equalsIgnoreCase(key)) {
                p_options = SoundOptions.parseConfigurationNode(e.getValue());
            } else {
                EventSounds.l("Parsing group %s %s", event, e.getKey());
                Playable p_playable = Playable.builder().fromConfigurationNode(e.getValue()).build(event, key);
                BiFitness<Integer, Player, Object> p_predicate = parsePredicate(event, e.getValue());
                p_sounds.put(p_predicate, p_playable);
            }
        }
        event_options.put(event, p_options);
        event_sounds.put(event, p_sounds);
        event_cooldowns.put(event, new Refrigerator((long)(p_options.getCooldownGlobal()*1000), (long)(p_options.getCooldownClient()*1000)));

    }

    private static BiFitness<Integer, Player, Object> parsePredicate(String event, ConfigurationNode node) {
        if (EVENT_JOIN.equalsIgnoreCase(event)) {
            String uuid = node.getNode("userid").getString();
            String permission = node.getNode("permission").getString();
            UUID userid = null;
            if (uuid != null) try {
                userid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {/**/}
            EventSounds.l("Test join: %s %s", userid, permission);
            if (userid != null && (permission != null && !permission.isEmpty())) {
                //both set, match value 3
                final UUID val = userid;
                return (k,v)->(k.getUniqueId().equals(val) && k.hasPermission(permission)?3:-1);
            } else if (userid != null) {
                //userid set, more specific than permission, rule value 2
                final UUID val = userid;
                return (k,v)->k.getUniqueId().equals(val)?2:-1;
            } else if (permission != null && !permission.isEmpty()) {
                //permission set, rule value 1
                return (k,v)->k.hasPermission(permission)?1:-1;
            } else
                //no rules set, pass (returns 0)
                return (k,v)->0;
        } else if (EVENT_CHAT.equalsIgnoreCase(event)) {
            String[] text = node.getNode("text").getChildrenList().stream().map(ConfigurationNode::getString).filter(s->!s.contains(" ")).toArray(String[]::new);
            String uuid = node.getNode("userid").getString();
            String permission = node.getNode("permission").getString();
            if (text == null || text.length == 0)
                //if no text value was given this makes no sense
                return (k,v)->-1;

            for (String word : text)
                saySoundTriggers.add(word);

            UUID userid = null;
            if (uuid != null) try {
                userid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {/**/}
            if (userid != null && (permission != null && !permission.isEmpty())) {
                //both set, match value 3
                final UUID val = userid;
                return (k,v)->(containsString(text, v) && k.getUniqueId().equals(val) && k.hasPermission(permission)?3:-1);
            } else if (userid != null) {
                //userid set, more specific than permission, rule value 2
                final UUID val = userid;
                return (k,v)->(containsString(text, v) && k.getUniqueId().equals(val))?2:-1;
            } else if (permission != null && !permission.isEmpty()) {
                //permission set, rule value 1
                return (k,v)->(containsString(text, v) && k.hasPermission(permission))?1:-1;
            } else
                //no rules set, pass (returns 0)
                return (k,v)->(containsString(text, v))?0:-1;

        } else if (EVENT_KILLSTREAK.equalsIgnoreCase(event)||
            EVENT_MULTIKILL.equalsIgnoreCase(event)||
            EVENT_COMBOBREAKER.equalsIgnoreCase(event)) {
            Integer kills = node.getNode("kills").getInt();
            String uuid = node.getNode("userid").getString();
            String permission = node.getNode("permission").getString();
            if (kills < 0)
                return (k,v)->-1;

            UUID userid = null;
            if (uuid != null) try {
                userid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {/**/}
            if (userid != null && (permission != null && !permission.isEmpty())) {
                //both set, match value 3
                final UUID val = userid;
                return (k,v)->((kills == 0 || kills == v) && k.getUniqueId().equals(val) && k.hasPermission(permission)?(kills>0?13:3):-1);
            } else if (userid != null) {
                //userid set, more specific than permission, rule value 2
                final UUID val = userid;
                return (k,v)->((kills == 0 || kills == v) && k.getUniqueId().equals(val))?(kills>0?12:2):-1;
            } else if (permission != null && !permission.isEmpty()) {
                //permission set, rule value 1
                return (k,v)->((kills == 0 || kills == v) && k.hasPermission(permission))?(kills>0?11:1):-1;
            } else
                //no rules set, pass (returns 0)
                return (k,v)->(kills == 0 || kills == v)?(kills>0?10:0):-1;

        } else if (EVENT_DEATH.equalsIgnoreCase(event)) {
            String[] cause = node.getNode("cause").getChildrenList().stream().map(ConfigurationNode::getString).toArray(String[]::new);
            String uuid = node.getNode("userid").getString();
            String permission = node.getNode("permission").getString();
            if (cause == null)
                return (k,v)->-1;

            UUID userid = null;
            if (uuid != null) try {
                userid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {/**/}

            //if damage type set and matches +10, for "any" damage +0, damage type miss-match -10
            if (userid != null && (permission != null && !permission.isEmpty())) {
                //both set, base match value 3
                final UUID val = userid;
                return (k,v)->{
                    int base = 0;
                    if (cause.length>0) {
                        base += (v == null ||
                                containsString(cause, (v instanceof Living)
                                        ?(((Living)v).getType().getId())
                                        :((DamageType)v).getName()))?10:-10;
                    }
                    return base<0?-1:(k.getUniqueId().equals(val) && k.hasPermission(permission)?3+base:-1);
                };
            } else if (userid != null) {
                //userid set, more specific than permission, rule base value 2
                final UUID val = userid;
                return (k,v)->{
                    int base = 0;
                    if (cause.length>0) {
                        base += (v == null ||
                                containsString(cause, (v instanceof Living)
                                        ?(((Living)v).getType().getId())
                                        :((DamageType)v).getName()))?10:-10;
                    }
                    return base<0?-1:(k.getUniqueId().equals(val)?2+base:-1);
                };
            } else if (permission != null && !permission.isEmpty()) {
                //permission set, rule base value 1
                return (k,v)->{
                    int base = 0;
                    if (cause.length>0) {
                        base += (v == null ||
                                containsString(cause, (v instanceof Living)
                                        ?(((Living)v).getType().getId())
                                        :((DamageType)v).getName()))?10:-10;
                    }
                    return base<0?-1:(k.hasPermission(permission)?1+base:-1);
                };
            } else {
                //no rules set, pass (returns 0)
                return (k,v)->{
                    int base = 0;
                    if (cause.length>0) {
                        base += (v == null ||
                                containsString(cause, (v instanceof Living)
                                        ?(((Living)v).getType().getId())
                                        :((DamageType)v).getName()))?10:-10;
                    }
                    return base<0?-1:base;
                };
            }
        } else {
            return (k,v)->0;
        }
    }

    private static boolean containsString(String[] array, Object other) {
        if (!(other instanceof String)) return false;
        for (String s : array) if (s.equalsIgnoreCase((String)other)) return true;
        return false;
    }

    //region SoundDefinition Collection
    private static Set<SoundDefinition> sounds = new HashSet<>();
    public static Set<SoundDefinition> getSoundDefinitions() {
        return ImmutableSet.<SoundDefinition>builder().addAll(sounds).build();
    }

    public static SoundDefinition registerSound(@NotNull String event, @NotNull String name, @NotNull String category, @NotNull String... files) {
        if (!event.matches("[a-z]+"))
            throw new IllegalArgumentException("Event names have to be all lowercase letters (a-z)");
        Pattern regex;
        //SoundCollector is allowed to define one more key step for SoundType IDs : es.plugin.<PLUGINID>.<SOUNDNAME>
        //it also already checks for file existance in the plugin archives, so we can skip that here
        boolean isPrivileged = (Thread.currentThread().getStackTrace()[2].getClassName().equals(SoundCollector.class.getName()));
        if (isPrivileged)
            regex = Pattern.compile("[a-z][\\w-]{0,63}\\.[A-Za-z][A-Za-z0-9]{0,63}");
        else
            regex = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,63}");
        if (!regex.matcher(name).matches())
            throw new RuntimeException("Names have to start with a letter and can only contain letters and digits (invalid: "+name+")");
        if (!isPrivileged)
            for (String f : files) {
                File test = new File("./assets/eventsounds/", f);
                if (!test.exists() || !test.isFile())
                    throw new RuntimeException("File " + test.getAbsoluteFile() + " for sound " + name + " is missing");
            }

        String regName = String.format("es.%s.%s", event.toLowerCase(), name).toLowerCase();

        SoundDefinition definition = new SoundDefinition(regName, category, files);
//        EventSounds.l("Registered %s %s", event.toLowerCase(), regName);
        sounds.add(definition);
        return definition;
    }
    //endregion

}
