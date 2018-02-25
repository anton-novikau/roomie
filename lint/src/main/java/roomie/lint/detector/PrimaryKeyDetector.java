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

package roomie.lint.detector;


import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimaryKeyDetector extends Detector implements Detector.UastScanner {

    private static final String PRIMARY_KEY_TYPE_ISSUE_ID = "Roomie.PrimaryKeyType";
    private static final String PRIMARY_KEY_TYPE_ISSUE_TITLE = "Primary key must be long";
    private static final String PRIMARY_KEY_TYPE_ISSUE_BODY = "Primary key of the database entity is required to a type of 'long'";

    private static final String PRIMARY_KEY_NOT_DEFINED_ISSUE_ID = "Roomie.PrimaryKeyNotDefined";
    private static final String PRIMARY_KEY_NOT_DEFINED_ISSUE_TITLE = "Primary key in the entity is not defined";
    private static final String PRIMARY_KEY_NOT_DEFINED_ISSUE_BODY = "Database entity requires a Primary key defined. " +
            "One column of a type 'long' must be marked with @PrimaryKey";

    private static final String PRIMARY_KEY_ALREDY_DEFINED_ISSUE_ID = "Roomie.PrimaryKeyAlredyDefined";
    private static final String PRIMARY_KEY_ALREDY_DEFINED_ISSUE_TITLE = "Primary key in the entity is alread defined";
    private static final String PRIMARY_KEY_ALREDY_DEFINED_ISSUE_BODY = "Database entity requires only one Primary key defined. " +
            "There are found at least two columns marked with @PrimaryKey";

    private static final String PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_ID = "Roomie.PrimaryKeyIsNotColumn";
    private static final String PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_TITLE = "Primary key must be a column of the entity";
    private static final String PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_BODY = "Primary key must be a column of the database entity" +
            "There field is annotated with @PrimaryKey, but isn't annotate with @Column";

    private static final Issue ISSUE_PRIMARY_KEY_TYPE = Issue.create(
            PRIMARY_KEY_TYPE_ISSUE_ID,
            PRIMARY_KEY_TYPE_ISSUE_TITLE,
            PRIMARY_KEY_TYPE_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(PrimaryKeyDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final Issue ISSUE_PRIMARY_KEY_NOT_DEFINED = Issue.create(
            PRIMARY_KEY_NOT_DEFINED_ISSUE_ID,
            PRIMARY_KEY_NOT_DEFINED_ISSUE_TITLE,
            PRIMARY_KEY_NOT_DEFINED_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(PrimaryKeyDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final Issue ISSUE_PRIMARY_KEY_ALREADY_DEFINED = Issue.create(
            PRIMARY_KEY_ALREDY_DEFINED_ISSUE_ID,
            PRIMARY_KEY_ALREDY_DEFINED_ISSUE_TITLE,
            PRIMARY_KEY_ALREDY_DEFINED_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(PrimaryKeyDetector.class, Scope.JAVA_FILE_SCOPE));

    private static final Issue ISSUE_PRIMARY_KEY_IS_NOT_COLUMN = Issue.create(
            PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_ID,
            PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_TITLE,
            PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(PrimaryKeyDetector.class, Scope.JAVA_FILE_SCOPE));

    public static final Issue[] ISSUES = {
            ISSUE_PRIMARY_KEY_TYPE,
            ISSUE_PRIMARY_KEY_NOT_DEFINED,
            ISSUE_PRIMARY_KEY_ALREADY_DEFINED,
            ISSUE_PRIMARY_KEY_IS_NOT_COLUMN
    };

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Arrays.asList(UClass.class, UField.class);
    }

    @Override
    public UElementHandler createUastHandler(JavaContext context) {
        return new UElementHandler() {
            @Override
            public void visitField(UField node) {
                node.accept(new PrimaryKeyValidator(context));
            }

            @Override
            public void visitClass(UClass uClass) {
                uClass.accept(new PrimaryKeyValidator(context));
            }
        };
    }

    private static class PrimaryKeyValidator extends AbstractUastVisitor {
        private final JavaContext context;

        public PrimaryKeyValidator(JavaContext context) {
            this.context = context;
        }

        @Override
        public boolean visitAnnotation(UAnnotation node) {
            if ("roomie.api.PrimaryKey".equals(node.getQualifiedName())) {
                detectPrimaryKeyType(node);
            } else if ("roomie.api.Entity".equals(node.getQualifiedName())) {
                detectPrimaryKeyDefined(node);
            }
            return super.visitAnnotation(node);
        }

        private void detectPrimaryKeyType(UAnnotation node) {
            UField annotatedField = (UField) node.getUastParent();
            if (annotatedField == null) {
                return; // early exit
            }

            String fieldType = annotatedField.getType().getCanonicalText();
            if (!PsiType.LONG.getCanonicalText().equals(fieldType) &&
                    !PsiType.LONG.getBoxedTypeName().equals(fieldType)) {
                context.report(ISSUE_PRIMARY_KEY_TYPE,
                        context.getNameLocation(annotatedField.getNameIdentifier()),
                        PRIMARY_KEY_TYPE_ISSUE_TITLE);
            }
        }

        private void detectPrimaryKeyDefined(UAnnotation node) {
            UClass annotatedClass = (UClass) node.getUastParent();
            if (annotatedClass == null) {
                return; // early exit
            }

            UField[] fields = annotatedClass.getFields();
            List<UField> pkFields = new ArrayList<>();
            for (UField field : fields) {
                UAnnotation pkAnnotation = field.findAnnotation("roomie.api.PrimaryKey");
                if (pkAnnotation != null) {
                    pkFields.add(field);
                }
            }

            if (pkFields.isEmpty()) {
                context.report(ISSUE_PRIMARY_KEY_NOT_DEFINED,
                        context.getNameLocation(annotatedClass.getNameIdentifier()),
                        PRIMARY_KEY_NOT_DEFINED_ISSUE_TITLE);
            }

            for (int index = 0, size = pkFields.size(); index < size; index++) {
                UField pkField = pkFields.get(index);
                if (index < (size - 1)) {
                    // don't report issue for the very last primary key
                    context.report(ISSUE_PRIMARY_KEY_ALREADY_DEFINED,
                            context.getNameLocation(pkField.getNameIdentifier()),
                            PRIMARY_KEY_ALREDY_DEFINED_ISSUE_TITLE);
                }

                UAnnotation columnAnnotation = pkField.findAnnotation("roomie.api.Column");
                if (columnAnnotation == null) {
                    context.report(ISSUE_PRIMARY_KEY_IS_NOT_COLUMN,
                            context.getNameLocation(pkField.getNameIdentifier()),
                            PRIMARY_KEY_IS_NOT_COLUMN_ISSUE_TITLE);
                }
            }
        }
    }
}
