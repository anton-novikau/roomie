/*
 * Copyright 2018 Anton Novikau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package roomie.api.adapter;


import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class TypeAdapterRegistry {

    private static final TypeAdapterRegistry INSTANCE = new TypeAdapterRegistry();

    private final Map<Class, TypeAdapter> registry = new HashMap<>();

    private TypeAdapterRegistry() {
        registry.put(BooleanTypeAdapter.class, new BooleanTypeAdapter());
        registry.put(ByteArrayAdapter.class, new ByteArrayAdapter());
        registry.put(ByteTypeAdapter.class, new ByteTypeAdapter());
        registry.put(DateTypeAdapter.class, new DateTypeAdapter());
        registry.put(DoubleTypeAdapter.class, new DoubleTypeAdapter());
        registry.put(FloatTypeAdapter.class, new FloatTypeAdapter());
        registry.put(IntegerTypeAdapter.class, new IntegerTypeAdapter());
        registry.put(LongTypeAdapter.class, new LongTypeAdapter());
        registry.put(ShortTypeAdapter.class, new ShortTypeAdapter());
        registry.put(StringTypeAdapter.class, new StringTypeAdapter());
    }

    public static TypeAdapterRegistry getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends TypeAdapter> T getTypeAdapter(Class<T> typeAdapterClass) {
        T adapter = (T) registry.get(typeAdapterClass);
        if (adapter == null) {
            try {
                adapter = typeAdapterClass.newInstance();
                registry.put(typeAdapterClass, adapter);
            } catch (InstantiationException e) {
                throw new RuntimeException("TypeAdapter must have a default constructor", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("TypeAdapter must have a default public constructor", e);
            }
        }
        return adapter;
    }
}
