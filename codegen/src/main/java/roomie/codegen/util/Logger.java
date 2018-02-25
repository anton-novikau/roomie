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


import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class Logger {

    private final Messager mMessager;

    public Logger(Messager messager) {
        mMessager = messager;
    }

    public String error(Element element, String msg, Object... args) {
        String message = String.format(msg, args);
        mMessager.printMessage(Diagnostic.Kind.ERROR, message, element);
        return message;
    }

    public String warn(Element element, String msg, Object... args) {
        String message = String.format(msg, args);
        mMessager.printMessage(Diagnostic.Kind.WARNING, message, element);
        return message;
    }

    public String info(Element element, String msg, Object... args) {
        String message = String.format(msg, args);
        mMessager.printMessage(Diagnostic.Kind.NOTE, message, element);
        return message;
    }
}
