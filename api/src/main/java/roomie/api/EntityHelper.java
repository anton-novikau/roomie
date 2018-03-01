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

package roomie.api;


import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

public interface EntityHelper<T> {
    @WorkerThread
    void load(@NonNull T entity, @NonNull DatabaseProvider databaseProvider);
    @WorkerThread
    void save(@NonNull T entity, @NonNull DatabaseProvider databaseProvider);
    @WorkerThread
    void delete(@NonNull T entity, @NonNull DatabaseProvider databaseProvider);

    String toString(@NonNull T entity);
}
