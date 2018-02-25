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

package roomie.codegen.util;


import android.support.annotation.Nullable;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import roomie.api.DatabaseType;
import roomie.api.adapter.BooleanTypeAdapter;
import roomie.api.adapter.ByteArrayAdapter;
import roomie.api.adapter.ByteTypeAdapter;
import roomie.api.adapter.DateTypeAdapter;
import roomie.api.adapter.DoubleTypeAdapter;
import roomie.api.adapter.FloatTypeAdapter;
import roomie.api.adapter.IntegerTypeAdapter;
import roomie.api.adapter.LongTypeAdapter;
import roomie.api.adapter.ShortTypeAdapter;
import roomie.api.adapter.StringTypeAdapter;
import roomie.api.adapter.TypeAdapter;

public class TypeMapping {
    private static final Map<TypeName, Class<? extends TypeAdapter<?>>> ADAPTER_MAPPING = new HashMap<>();
    private static final Map<TypeName, String> DB_TYPE_MAPPING = new HashMap<>();

    static {
        ADAPTER_MAPPING.put(ClassName.get(String.class), StringTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.LONG.box(), LongTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.LONG, LongTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.INT.box(), IntegerTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.INT, IntegerTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.SHORT.box(), ShortTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.SHORT, ShortTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.BYTE.box(), ByteTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.BYTE, ByteTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.DOUBLE.box(), DoubleTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.DOUBLE, DoubleTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.FLOAT.box(), FloatTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.FLOAT, FloatTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.BOOLEAN.box(), BooleanTypeAdapter.class);
        ADAPTER_MAPPING.put(TypeName.BOOLEAN, BooleanTypeAdapter.class);
        ADAPTER_MAPPING.put(ArrayTypeName.of(TypeName.BYTE), ByteArrayAdapter.class);
        ADAPTER_MAPPING.put(ClassName.get(Date.class), DateTypeAdapter.class);

        DB_TYPE_MAPPING.put(ClassName.get(String.class), DatabaseType.TEXT);
        DB_TYPE_MAPPING.put(TypeName.LONG.box(), DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.LONG, DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.INT.box(), DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.INT, DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.SHORT.box(), DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.SHORT, DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.BYTE.box(), DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.BYTE, DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.DOUBLE.box(), DatabaseType.FLOAT);
        DB_TYPE_MAPPING.put(TypeName.DOUBLE, DatabaseType.FLOAT);
        DB_TYPE_MAPPING.put(TypeName.FLOAT.box(), DatabaseType.FLOAT);
        DB_TYPE_MAPPING.put(TypeName.FLOAT, DatabaseType.FLOAT);
        DB_TYPE_MAPPING.put(TypeName.BOOLEAN.box(), DatabaseType.INT);
        DB_TYPE_MAPPING.put(TypeName.BOOLEAN, DatabaseType.INT);
        DB_TYPE_MAPPING.put(ArrayTypeName.of(TypeName.BYTE), DatabaseType.BLOB);
        DB_TYPE_MAPPING.put(ClassName.get(Date.class), DatabaseType.INT);

    }

    @Nullable
    public static Class<? extends TypeAdapter<?>> findAdapter(TypeName type) {
        return ADAPTER_MAPPING.get(type);
    }

    @Nullable
    @DatabaseType
    public static String findDatabaseType(TypeName type) {
        return DB_TYPE_MAPPING.get(type);
    }
}
