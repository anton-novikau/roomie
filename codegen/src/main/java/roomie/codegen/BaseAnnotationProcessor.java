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

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import roomie.codegen.util.Logger;


public abstract class BaseAnnotationProcessor extends AbstractProcessor {
    protected static final String DEFAULT_INDENTATION = "    ";
    protected static final String DEFAULT_FILE_COMMENT = "Code generated from $L. Do not modify!";

    protected Logger logger;
    protected Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = new Logger(processingEnv.getMessager());
        elementUtils = processingEnv.getElementUtils();
    }

    protected String getPackageName(Element type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    protected void writeSourceFile(String className, String packageName, TypeSpec classContent,
            Element originatingElement) throws AbortProcessingException {
        try {
            String qualifiedName = toQualifiedName(packageName, className);
            JavaFile javaFile = JavaFile.builder(packageName, classContent)
                    .indent(DEFAULT_INDENTATION)
                    .addFileComment(DEFAULT_FILE_COMMENT, getClass().getSimpleName())
                    .build();
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedName, originatingElement);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(javaFile.toString());
            }
        } catch (IOException e) {
            throw new AbortProcessingException(e,
                    originatingElement,
                    "Could not write generated class %s: %s",
                    className,
                    e);
        }
    }

    private String toQualifiedName(String packageName, String className) {
        return packageName.length() > 0 ? packageName + '.' + className : className;
    }
}
