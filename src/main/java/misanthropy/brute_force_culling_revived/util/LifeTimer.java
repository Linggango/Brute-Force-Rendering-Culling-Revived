package misanthropy.brute_force_culling_revived.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class LifeTimer<T> {
    private final @NotNull Map<T, Integer> usageTick;
    private final @NotNull ArrayList<T> list;

    public LifeTimer() {
        usageTick = new HashMap<>();
        list = new ArrayList<>();
    }

    public void tick(int clientTick, int count) {
        boolean removedAny = false;
        Iterator<Map.Entry<T, Integer>> iterator = usageTick.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<T, Integer> entry = iterator.next();
            int tick = entry.getValue();
            if (clientTick - tick > count) {
                iterator.remove();
                removedAny = true;
            }
        }

        if (removedAny) {
            list.clear();
            list.addAll(usageTick.keySet());
        }
    }

    public void updateUsageTick(T hash, int tick) {
        if(!usageTick.containsKey(hash)) {
            list.add(hash);
        }
        usageTick.put(hash, tick);
    }

    public boolean contains(T hash) {
        return usageTick.containsKey(hash);
    }

    public void clear() {
        usageTick.clear();
        list.clear();
    }

    public int size() {
        return usageTick.size();
    }

    public void foreach(Consumer<T> consumer) {
        list.forEach(consumer);
    }
}