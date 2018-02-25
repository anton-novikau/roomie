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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UTypeReferenceExpression;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Collections;
import java.util.List;

import roomie.lint.util.KnownTypes;

public class ColumnDetector extends Detector implements Detector.UastScanner {

    private static final String UNKNOW_DB_TYPE_ISSUE_ID = "Roomie.UnknownColumnDbType";
    private static final String UNKNOW_DB_TYPE_ISSUE_TITLE = "Database type of the column must be explicitly specified";
    private static final String UNKNOW_DB_TYPE_ISSUE_BODY = "Database type of the column can not be automatically detected" +
            " and must be explicitly specified";

    private static final String UNKNOW_SERIALIZED_TYPE_ISSUE_ID = "Roomie.UnknownType";
    private static final String UNKNOW_SERIALIZED_TYPE_ISSUE_TITLE = "Type adapter must be explicitly provided";
    private static final String UNKNOW_SERIALIZED_TYPE_ISSUE_BODY = "Type adapter for serializing field data to database column" +
            " and deserialzing data from database column to field type must be explicitly provided";

    private static final String WRONG_ADAPTER_TYPE_ISSUE_ID = "Roomie.WrongAdapterType";
    private static final String WRONG_ADAPTER_TYPE_ISSUE_TITLE = "Type adapter must be of the same type as column";
    private static final String WRONG_ADAPTER_TYPE_ISSUE_BODY = "Type adapter be of the same type as column to be able" +
            " to know how to serialize/deserialize column data to database";

    private static final Issue ISSUE_UNKNOWN_DATABASE_TYPE = Issue.create(
            UNKNOW_DB_TYPE_ISSUE_ID,
            UNKNOW_DB_TYPE_ISSUE_TITLE,
            UNKNOW_DB_TYPE_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(ColumnDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    private static final Issue ISSUE_UNKNOWN_SERIALIZED_TYPE = Issue.create(
            UNKNOW_SERIALIZED_TYPE_ISSUE_ID,
            UNKNOW_SERIALIZED_TYPE_ISSUE_TITLE,
            UNKNOW_SERIALIZED_TYPE_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(ColumnDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    private static final Issue ISSUE_WRONG_ADAPTER_TYPE = Issue.create(
            WRONG_ADAPTER_TYPE_ISSUE_ID,
            WRONG_ADAPTER_TYPE_ISSUE_TITLE,
            WRONG_ADAPTER_TYPE_ISSUE_BODY,
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(ColumnDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    public static final Issue[] ISSUES = {
            ISSUE_UNKNOWN_DATABASE_TYPE,
            ISSUE_UNKNOWN_SERIALIZED_TYPE,
            ISSUE_WRONG_ADAPTER_TYPE
    };

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(UField.class);
    }

    @Override
    public UElementHandler createUastHandler(JavaContext context) {
        KnownTypes knownTypes = new KnownTypes();
        return new UElementHandler() {
            @Override
            public void visitField(UField node) {
                node.accept(new ColumnValidator(knownTypes, context));
            }
        };
    }

    private static class ColumnValidator extends AbstractUastVisitor {
        private final KnownTypes knownTypes;
        private final JavaContext context;

        ColumnValidator(KnownTypes knownTypes, JavaContext context) {
            this.knownTypes = knownTypes;
            this.context = context;
        }

        @Override
        public boolean visitAnnotation(UAnnotation node) {
            if ("roomie.api.Column".equals(node.getQualifiedName())) {
                UField field = (UField) node.getUastParent();
                if (field == null) {
                    return super.visitAnnotation(node); // early exit
                }

                detectUnknownDatabaseTypes(node, field);
                detectUnknownSerializedTypes(node, field);
                detectAdapterTypeMatchesColumnType(node, field);
            }

            return super.visitAnnotation(node);
        }

        private void detectUnknownDatabaseTypes(UAnnotation node, UField field) {
            if (!knownTypes.isKnownType(field.getType()) &&
                    node.findDeclaredAttributeValue("databaseType") == null) {
                context.report(ISSUE_UNKNOWN_DATABASE_TYPE,
                        field,
                        context.getNameLocation(node),
                        UNKNOW_DB_TYPE_ISSUE_TITLE);
            }
        }

        private void detectUnknownSerializedTypes(UAnnotation node, UField field) {
            if (!knownTypes.isKnownType(field.getType()) &&
                    node.findDeclaredAttributeValue("adapter") == null) {
                context.report(ISSUE_UNKNOWN_DATABASE_TYPE,
                        field,
                        context.getNameLocation(node),
                        UNKNOW_SERIALIZED_TYPE_ISSUE_TITLE);
            }
        }

        private void detectAdapterTypeMatchesColumnType(UAnnotation node, UField field) {
            UExpression adapter = node.findDeclaredAttributeValue("adapter");
            if (adapter == null) {
                return; // early exit
            }

            adapter.accept(new AbstractUastVisitor() {
                @Override
                public boolean visitTypeReferenceExpression(UTypeReferenceExpression node) {
                    PsiType type = node.getType();
                    PsiType[] superTypes = type.getSuperTypes();
                    if (superTypes.length == 0) {
                        return super.visitTypeReferenceExpression(node); // early exit
                    }

                    PsiClassType typeAdapter = null;
                    for (PsiType superType : superTypes) {
                        PsiClass superClass = PsiTypesUtil.getPsiClass(superType);
                        if (superClass == null) {
                            continue;
                        }

                        if ("roomie.api.adapter.TypeAdapter".equals(superClass.getQualifiedName())) {
                            typeAdapter = (PsiClassType) superType;
                            break;
                        }
                    }

                    if (typeAdapter == null || typeAdapter.getParameterCount() == 0) {
                        return super.visitTypeReferenceExpression(node); // early exit
                    }

                    PsiType typeAdapterParam = typeAdapter.getParameters()[0];
                    if (!field.getType().equals(typeAdapterParam)) {
                        context.report(ISSUE_WRONG_ADAPTER_TYPE,
                                field,
                                context.getLocation(adapter.getPsi()),
                                WRONG_ADAPTER_TYPE_ISSUE_TITLE);
                    }
                    return true;
                }
            });

        }
    }
}
