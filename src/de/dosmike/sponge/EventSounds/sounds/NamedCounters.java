package de.dosmike.sponge.EventSounds.sounds;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NamedCounters<N extends Object> {

    private Map<N, Integer> counters = new HashMap<>();

    public int getValue(N name) {
        return counters.getOrDefault(name, 0);
    }
    public int inrement(N name) {
        int value = getValue(name) +1;
        counters.put(name, value);
        return value;
    }
    public void reset(N name) {
        counters.remove(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamedCounters<?> that = (NamedCounters<?>) o;
        return Objects.equals(counters, that.counters);
    }

    @Override
    public int hashCode() {

        return Objects.hash(counters);
    }
}
