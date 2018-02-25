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

package roomie.sample.db.entity;

import android.net.Uri;

import java.util.Date;

import roomie.api.Column;
import roomie.api.DatabaseType;
import roomie.api.Entity;
import roomie.api.PrimaryKey;
import roomie.sample.db.adapter.MessageTypeTypeAdapter;
import roomie.sample.db.adapter.UriTypeAdapter;

@Entity(table = "message")
public class MessageEntity extends BaseEntity<MessageEntity> {
    @Column(name = "_ID")
    @PrimaryKey
    long id;

    @Column
    String body;

    @Column(name = "MESSAGE_DATE")
    Date messageDate;

    @Column(name = "READ_COUNTER", defaultValue = "1")
    int readCounter;

    @Column(name = "USERPIC", adapter = UriTypeAdapter.class, databaseType = DatabaseType.TEXT)
    Uri image;

    @Column(name = "MESSAGE_TYPE", adapter = MessageTypeTypeAdapter.class, databaseType = DatabaseType.TEXT)
    MessageType messageType;

    public MessageEntity() {
        super(new MessageEntityHelper());
    }

    public long getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public Uri getImage() {
        return image;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
