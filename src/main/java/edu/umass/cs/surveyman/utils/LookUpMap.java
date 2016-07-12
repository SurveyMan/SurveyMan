package edu.umass.cs.surveyman.utils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LookUpMap <A, B, C> extends HashMap<A, Map<B, C>> {


    public LookUpMap() {}

    public LookUpMap (Collection<A> aKeys, Collection<B> bKeys, @Nonnull C defaultValue) {
        super();
        for (A aKey : aKeys) {
            for (B bKey : bKeys) {
                this.put(aKey, bKey, defaultValue);
            }
        }
    }

    public void put(A key1, B key2, @Nonnull C val) {
        Map<B, C> map = this.get(key1);
        if(map == null) {
            map = new HashMap<>();
            put(key1, map);
        }
        map.put(key2, val);
    }

    public C get(A key1, B key2) {
        return this.get(key1).get(key2);
    }

}
