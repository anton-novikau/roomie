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

package roomie.codegen;


import javax.lang.model.element.Element;

import roomie.codegen.util.Logger;

public class AbortProcessingException extends Exception {

    private final Element associatedElement;

    public AbortProcessingException(Element associatedElement, String message, Object... args) {
        super(String.format(message, args));

        this.associatedElement = associatedElement;
    }

    public AbortProcessingException(Throwable cause, Element associatedElement, String message, Object... args) {
        super(String.format(message, args), cause);

        this.associatedElement = associatedElement;
    }

    public Element getAssociatedElement() {
        return associatedElement;
    }
}
