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


import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;

public class DateTypeAdapter implements TypeAdapter<Date> {

    @Override
    public Date read(int position, Cursor cursor) {
        long timestamp = cursor.getLong(position);
        return timestamp > 0 ? new Date(timestamp) : null;
    }

    @Override
    public void write(String key, Date value, ContentValues values) {
        values.put(key, value != null ? value.getTime() : 0L);
    }
}
