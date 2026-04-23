package misanthropy.brute_force_culling_revived.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LifeTimer<T> {
    private final @NotNull Map<T, Integer> usageTick;

    public LifeTimer() {
        usageTick = new HashMap<>();
    }

    public void tick(int clientTick, int count) {
        usageTick.entrySet().removeIf(entry -> (clientTick - entry.getValue()) > count);
    }

    public void updateUsageTick(T hash, int tick) {
        usageTick.put(hash, tick);
    }

    public boolean contains(T hash) {
        return usageTick.containsKey(hash);
    }

    public void clear() {
        usageTick.clear();
    }

    public int size() {
        return usageTick.size();
    }

    public void foreach(Consumer<T> consumer) {
        for (T key : usageTick.keySet()) {
            consumer.accept(key);
        }
    }
}