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

package roomie.lint.util;


import com.intellij.psi.PsiType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class KnownTypes {

    private final Set<String> knownTypes = new HashSet<>();

    public KnownTypes() {
        knownTypes.add(PsiType.BOOLEAN.getCanonicalText());
        knownTypes.add(PsiType.BOOLEAN.getBoxedTypeName());
        knownTypes.add(PsiType.BYTE.getCanonicalText());
        knownTypes.add(PsiType.BYTE.getBoxedTypeName());
        knownTypes.add(PsiType.SHORT.getCanonicalText());
        knownTypes.add(PsiType.SHORT.getBoxedTypeName());
        knownTypes.add(PsiType.INT.getCanonicalText());
        knownTypes.add(PsiType.INT.getBoxedTypeName());
        knownTypes.add(PsiType.LONG.getCanonicalText());
        knownTypes.add(PsiType.LONG.getBoxedTypeName());
        knownTypes.add(PsiType.FLOAT.getCanonicalText());
        knownTypes.add(PsiType.FLOAT.getBoxedTypeName());
        knownTypes.add(PsiType.DOUBLE.getCanonicalText());
        knownTypes.add(PsiType.DOUBLE.getBoxedTypeName());
        knownTypes.add(byte[].class.getCanonicalName());
        knownTypes.add(String.class.getCanonicalName());
        knownTypes.add(Date.class.getCanonicalName());
    }

    public boolean isKnownType(PsiType type) {
        return knownTypes.contains(type.getCanonicalText());
    }
}
