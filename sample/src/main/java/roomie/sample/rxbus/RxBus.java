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

package roomie.sample.rxbus;


import android.content.Context;

import io.reactivex.subjects.PublishSubject;
import roomie.sample.App;

public class RxBus {

    private final PublishSubject<Object> bus = PublishSubject.create();

    public static RxBus from(Context context) {
        return ((App)context.getApplicationContext()).getEventBus();
    }

    public void publish(Object event) {
        bus.onNext(event);
    }

    public <T> void subscribe(Class<T> type, Subscriber<T> subscriber) {
        bus.ofType(type).subscribe(subscriber);
    }
}
