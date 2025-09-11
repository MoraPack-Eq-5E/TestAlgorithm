package com.grupo5e.morapack.algorithm.tabu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TabuList {
    private final Map<Object, Integer> tenureMap = new HashMap<>();
    private final int tenure;

    public TabuList(int tenure) {
        if (tenure < 1) throw new IllegalArgumentException("tenure must be >= 1");
        this.tenure = tenure;
    }

    public boolean isTabu(Object key) {
        Integer left = tenureMap.get(key);
        return left != null && left > 0;
    }

    public void add(Object key) {
        if (key != null) tenureMap.put(key, tenure);
    }

    /** Llamar una vez por iteración. */
    public void tick() {
        var toRemove = new ArrayList<Object>();
        for (var e : tenureMap.entrySet()) {
            int left = e.getValue() - 1;
            if (left <= 0) toRemove.add(e.getKey());
            else e.setValue(left);
        }
        toRemove.forEach(tenureMap::remove);
    }

    public void clear() { tenureMap.clear(); }
}
