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


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

import roomie.api.Column;
import roomie.api.DatabaseProvider;
import roomie.api.DatabaseType;
import roomie.api.Entity;
import roomie.api.EntityHelper;
import roomie.api.PrimaryKey;
import roomie.api.adapter.DefaultTypeAdapter;
import roomie.api.adapter.TypeAdapter;
import roomie.api.adapter.TypeAdapterRegistry;
import roomie.codegen.util.TypeMapping;

@SuppressWarnings("unused") // class is used by @AutoService
@AutoService(Processor.class)
@SupportedAnnotationTypes("roomie.api.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(EntityHelperProcessor.DB_HELPER_PACKAGE_KEY)
public class EntityHelperProcessor extends BaseAnnotationProcessor {
    static final String DB_HELPER_PACKAGE_KEY = "roomie.dbHelperPackage";

    private static final String ENTITY_HELPER_POSTFIX = "Helper";
    private static final String DB_HELPER_CLASS_NAME = "RoomieDatabaseHelper";

    private String databaseHelperPackage;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        String defaultHelperPackage = Entity.class.getPackage().toString();
        databaseHelperPackage = processingEnv.getOptions().getOrDefault(DB_HELPER_PACKAGE_KEY, defaultHelperPackage);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedClasses =
                roundEnv.getElementsAnnotatedWith(Entity.class);
        if (annotatedClasses.isEmpty()) {
            return true; // early exit
        }

        List<ClassName> helpers = new ArrayList<>(annotatedClasses.size());
        for (Element element : annotatedClasses) {
            if (element.getKind() != ElementKind.CLASS) {
                logger.error(element,
                        "%s can be applied only to classes",
                        Entity.class.getSimpleName());
                continue;
            }

            TypeElement type = (TypeElement) element;

            if (type.getModifiers().contains(Modifier.PRIVATE)) {
                logger.error(type,"Entity can't be a private class");
                continue;
            }

            String packageName = getPackageName(element);
            String className = type.getSimpleName() + ENTITY_HELPER_POSTFIX;
            helpers.add(ClassName.get(packageName, className));

            try {
                TypeSpec fileContent = generateEntityClass(type, className);

                writeSourceFile(className, packageName, fileContent, type);
            } catch (AbortProcessingException e) {
                logger.error(e.getAssociatedElement(), e.getMessage());
            }
        }

        try {
            generateDatabaseHelper(helpers);
        } catch (AbortProcessingException e) {
            logger.error(e.getAssociatedElement(), e.getMessage());
        }

        return true;
    }

    private void generateDatabaseHelper(List<ClassName> helpers) throws AbortProcessingException {
        TypeSpec dbHelperContent = generateDatabaseHelperClass(helpers);

        writeSourceFile(DB_HELPER_CLASS_NAME, databaseHelperPackage, dbHelperContent, null);
    }

    private TypeSpec generateDatabaseHelperClass(List<ClassName> helpers) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(DB_HELPER_CLASS_NAME);
        classBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        classBuilder.superclass(ClassName.bestGuess("android.database.sqlite.SQLiteOpenHelper"));

        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.bestGuess("android.content.Context"), "context")
                .addParameter(String.class, "name")
                .addParameter(ClassName.bestGuess("android.database.sqlite.SQLiteDatabase.CursorFactory"), "factory")
                .addParameter(int.class, "version")
                .addStatement("super(context, name, factory, version)")
                .build());

        classBuilder.addMethod(generateOnCreate(helpers));
        classBuilder.addMethod(generateOnUpgrade());

        return classBuilder.build();
    }

    private MethodSpec generateOnCreate(List<ClassName> helpers) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("onCreate");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(ClassName.bestGuess("android.database.sqlite.SQLiteDatabase"), "database");

        for (ClassName helper : helpers) {
            method.addStatement("database.execSQL($T.CREATE_STATEMENT)", helper);
        }

        return method.build();
    }

    private MethodSpec generateOnUpgrade() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("onUpgrade");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(ClassName.bestGuess("android.database.sqlite.SQLiteDatabase"), "database");
        method.addParameter(int.class, "oldVersion");
        method.addParameter(int.class, "newVersion");

        method.addComment("NO-OP");

        return method.build();
    }

    private TypeSpec generateEntityClass(TypeElement annotatedClass, String className) throws AbortProcessingException {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
        classBuilder.addModifiers(Modifier.PUBLIC);
        classBuilder.addSuperinterface(ParameterizedTypeName.get(ClassName.get(EntityHelper.class), ClassName.get(annotatedClass)));

        EntityMetadata metadata = obtainEntityMetadata(annotatedClass);

        classBuilder.addField(generateCreateTableStatement(annotatedClass, metadata));
        classBuilder.addField(generateProjection(annotatedClass, metadata));
        classBuilder.addMethod(generateQueryAll(annotatedClass, metadata));
        classBuilder.addMethod(generateLoadEntity(annotatedClass, metadata));
        classBuilder.addMethod(generateSaveEntity(annotatedClass, metadata));
        classBuilder.addMethod(generateDeleteEntity(annotatedClass, metadata));
        classBuilder.addMethod(generateToString(annotatedClass, metadata));
        classBuilder.addMethod(generateReadData(annotatedClass, metadata));
        classBuilder.addMethod(generateWriteData(annotatedClass, metadata));

        return classBuilder.build();
    }

    private FieldSpec generateCreateTableStatement(TypeElement type, EntityMetadata metadata)
            throws AbortProcessingException {
        FieldSpec.Builder createStmt = FieldSpec.builder(ClassName.get(String.class),
                "CREATE_STATEMENT",
                Modifier.PUBLIC,
                Modifier.STATIC,
                Modifier.FINAL);

        CodeBlock.Builder columns = CodeBlock.builder();
        VariableElement primaryKey = metadata.getPrimaryKey();
        columns.add("`$L` $L PRIMARY KEY AUTOINCREMENT",
                getColumnName(primaryKey),
                getDatabaseType(primaryKey));
        for (VariableElement column : metadata.getColumns()) {
            @DatabaseType
            String dbType = getDatabaseType(column);
            columns.add(", `$L` $L", getColumnName(column), dbType);
            String defaultValue = column.getAnnotation(Column.class).defaultValue();
            if (!StringUtils.isEmpty(defaultValue)) {
                String defaultStmt = dbType.equals(DatabaseType.TEXT) ? " DEFAULT '$L'" : " DEFAULT $L";
                columns.add(defaultStmt, defaultValue);
            }
        }
        CodeBlock.Builder initializer = CodeBlock.builder();
        initializer.add("CREATE TABLE IF NOT EXISTS `$L` ($L)", metadata.getTableName(), columns.build());
        createStmt.initializer("$S", initializer.build());
        return createStmt.build();
    }

    private FieldSpec generateProjection(TypeElement annotatedClass, EntityMetadata metadata) {
        FieldSpec.Builder projection = FieldSpec.builder(ArrayTypeName.of(String.class),
                "PROJECTION",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL);

        CodeBlock.Builder initializer = CodeBlock.builder();
        initializer.beginControlFlow("new $T", ArrayTypeName.of(String.class));

        List<VariableElement> columns = metadata.getAllColumns();
        for (int i = 0, size = columns.size(); i < size; i++) {
            VariableElement column = columns.get(i);
            if (i > 0) {
                initializer.add(",\n");
            }

            initializer.add("$S", getColumnName(column));
        }

        initializer.add("\n");
        initializer.endControlFlow();
        projection.initializer(initializer.build());

        return projection.build();
    }

    private MethodSpec generateQueryAll(TypeElement type, EntityMetadata metadata) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("queryAll");
        method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        ParameterizedTypeName returnType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(type));
        ClassName nonNullAnnotation = ClassName.bestGuess("android.support.annotation.NonNull");
        method.addAnnotation(nonNullAnnotation);
        method.addAnnotation(ClassName.bestGuess("android.support.annotation.WorkerThread"));
        method.returns(returnType);
        method.addParameter(ParameterSpec.builder(ClassName.get(DatabaseProvider.class), "databaseProvider")
                .addAnnotation(nonNullAnnotation)
                .build());

        method.addStatement("$T db = databaseProvider.getReadableDatabase()",
                ClassName.bestGuess("android.database.sqlite.SQLiteDatabase"));
        method.addStatement("$T cursor = db.query($S, PROJECTION, null, null, null, null, null)",
                ClassName.bestGuess("android.database.Cursor"),
                metadata.getTableName());
        method.addStatement("$T entities = new $T<>(cursor.getCount())", returnType, ClassName.get(ArrayList.class));
        method.beginControlFlow("try");
        method.beginControlFlow("while(cursor.moveToNext())");
        method.addStatement("$1T entity = new $1T()", ClassName.get(type));
        method.addStatement("loadFromCursor(entity, cursor)");
        method.addStatement("entities.add(entity)");
        method.endControlFlow();
        method.nextControlFlow("finally");
        method.addStatement("cursor.close()");
        method.endControlFlow();

        method.addStatement("return entities");

        return method.build();
    }

    private MethodSpec generateDeleteEntity(TypeElement type, EntityMetadata metadata) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("delete");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        ClassName nonNullAnnotation = ClassName.bestGuess("android.support.annotation.NonNull");
        method.addParameter(ParameterSpec.builder(ClassName.get(type), "entity")
                .addAnnotation(nonNullAnnotation)
                .build());
        method.addParameter(ParameterSpec.builder(ClassName.get(DatabaseProvider.class), "databaseProvider")
                .addAnnotation(nonNullAnnotation)
                .build());

        VariableElement pk = metadata.getPrimaryKey();
        Column columnAnnotation = pk.getAnnotation(Column.class);
        method.beginControlFlow("if (entity.$L == 0)", pk.getSimpleName());
        method.addStatement("throw new $T($S)", ClassName.bestGuess("android.database.SQLException"),
                "Primary key must be provided to load entity");
        method.endControlFlow();

        ClassName databaseClass = ClassName.bestGuess("android.database.sqlite.SQLiteDatabase");
        method.addStatement("$T db = databaseProvider.getReadableDatabase()", databaseClass);
        method.addStatement("$T deleted = db.delete($S, $S, new $T { $T.valueOf(entity.$L) })",
                TypeName.INT,
                metadata.getTableName(),
                columnAnnotation.name() + " = ?",
                ArrayTypeName.of(String.class),
                ClassName.get(String.class),
                pk.getSimpleName());

        method.beginControlFlow("if (deleted > 0)");
        method.addStatement("entity.$L = 0", pk.getSimpleName());
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateLoadEntity(TypeElement type, EntityMetadata metadata) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("load");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        ClassName nonNullAnnotation = ClassName.bestGuess("android.support.annotation.NonNull");
        method.addParameter(ParameterSpec.builder(ClassName.get(type), "entity")
                .addAnnotation(nonNullAnnotation)
                .build());
        method.addParameter(ParameterSpec.builder(ClassName.get(DatabaseProvider.class), "databaseProvider")
                .addAnnotation(nonNullAnnotation)
                .build());

        VariableElement pk = metadata.getPrimaryKey();
        Column columnAnnotation = pk.getAnnotation(Column.class);
        method.beginControlFlow("if (entity.$L == 0)", pk.getSimpleName());
        method.addStatement("throw new $T($S)", ClassName.bestGuess("android.database.SQLException"),
                "Primary key must be provided to load entity");
        method.endControlFlow();

        ClassName databaseClass = ClassName.bestGuess("android.database.sqlite.SQLiteDatabase");
        method.addStatement("$T db = databaseProvider.getReadableDatabase()", databaseClass);
        method.addStatement("$T cursor = db.query($S,\nPROJECTION,\n$S,\nnew $T { $T.valueOf(entity.$L) },\nnull,\nnull,\nnull)",
                ClassName.bestGuess("android.database.Cursor"),
                metadata.getTableName(),
                columnAnnotation.name() + " = ?",
                ArrayTypeName.of(String.class),
                ClassName.get(String.class),
                pk.getSimpleName());
        method.beginControlFlow("if (cursor.moveToNext())");
        method.addStatement("loadFromCursor(entity, cursor)");
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateSaveEntity(TypeElement type, EntityMetadata metadata) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("save");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        ClassName nonNullAnnotation = ClassName.bestGuess("android.support.annotation.NonNull");
        method.addParameter(ParameterSpec.builder(ClassName.get(type), "entity")
                .addAnnotation(nonNullAnnotation)
                .build());
        method.addParameter(ParameterSpec.builder(ClassName.get(DatabaseProvider.class), "databaseProvider")
                .addAnnotation(nonNullAnnotation)
                .build());

        ClassName databaseClass = ClassName.bestGuess("android.database.sqlite.SQLiteDatabase");
        method.addStatement("$T db = databaseProvider.getWritableDatabase()", databaseClass);
        method.addStatement("$T values = toContentValues(entity)", ClassName.bestGuess("android.content.ContentValues"));

        VariableElement pk = metadata.getPrimaryKey();
        Column columnAnnotation = pk.getAnnotation(Column.class);
        method.beginControlFlow("if (entity.$L > 0)", pk.getSimpleName());
        method.addStatement("db.update($S, values, $S, new $T { $T.valueOf(entity.$L) })",
                metadata.getTableName(),
                columnAnnotation.name() + " = ?",
                ArrayTypeName.of(String.class),
                ClassName.get(String.class),
                pk.getSimpleName());
        method.nextControlFlow("else");
        method.addStatement("entity.$L = db.insert($S, null, values)", pk.getSimpleName(), metadata.getTableName());
        method.endControlFlow();

        return method.build();
    }

    private MethodSpec generateToString(TypeElement type, EntityMetadata metadata) {
        MethodSpec.Builder method = MethodSpec.methodBuilder("toString");
        method.addAnnotation(Override.class);
        method.addModifiers(Modifier.PUBLIC);
        method.returns(String.class);
        method.addParameter(ParameterSpec.builder(ClassName.get(type), "entity")
                .addAnnotation(ClassName.bestGuess("android.support.annotation.NonNull"))
                .build());

        method.addStatement("$1T builder = new $1T($2S)",
                ClassName.get(StringBuilder.class),
                type.getSimpleName() + "{");
        VariableElement primaryKey = metadata.getPrimaryKey();
        method.addStatement("builder.append($S).append(entity.$L)",
                primaryKey.getSimpleName() + "=",
                primaryKey.getSimpleName());
        for (VariableElement column : metadata.getColumns()) {
            method.addStatement("builder.append($S).append(entity.$L)",
                    ", " + column.getSimpleName() + "=",
                    column.getSimpleName());
        }
        method.addStatement("builder.append($S)", "}");
        method.addCode("\n");

        method.addStatement("return $L", "builder.toString()");

        return method.build();
    }

    private MethodSpec generateReadData(TypeElement type, EntityMetadata metadata) throws AbortProcessingException {
        MethodSpec.Builder method = MethodSpec.methodBuilder("loadFromCursor");
        method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        method.addParameter(ClassName.get(type), "entity");
        method.addParameter(ClassName.bestGuess("android.database.Cursor"), "cursor");

        method.addStatement("$1T registry = $1T.getInstance()", ClassName.get(TypeAdapterRegistry.class));

        List<VariableElement> columns = metadata.getAllColumns();
        for (int i = 0, size = columns.size(); i < size; i++) {
            VariableElement column = columns.get(i);
            TypeName typeAdapter = getTypeAdapter(column);
            if (typeAdapter == null) {
                throw new AbortProcessingException(
                        column,
                        "%s has unsupported type %s for saving to database",
                        column.getSimpleName(), column.asType());
            }

            method.addStatement("entity.$L = registry.getTypeAdapter($T.class).read($L, cursor)",
                    column.getSimpleName(),
                    typeAdapter,
                    i);
        }

        return method.build();
    }

    private MethodSpec generateWriteData(TypeElement type, EntityMetadata metadata) throws AbortProcessingException {
        MethodSpec.Builder method = MethodSpec.methodBuilder("toContentValues");
        method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        method.addParameter(ClassName.get(type), "entity");
        ClassName contentValuesType = ClassName.bestGuess("android.content.ContentValues");
        method.returns(contentValuesType);

        method.addStatement("$1T values = new $1T()", contentValuesType);
        method.addStatement("$1T registry = $1T.getInstance()", ClassName.get(TypeAdapterRegistry.class));

        List<VariableElement> columns = metadata.getAllColumns();
        for (VariableElement column : columns) {
            TypeName typeAdapter = getTypeAdapter(column);
            if (typeAdapter == null) {
                throw new AbortProcessingException(
                        column,
                        "%s has unsupported type %s for saving to database",
                        column.getSimpleName(), column.asType());
            }

            PrimaryKey pk = column.getAnnotation(PrimaryKey.class);
            if (pk != null) {
                method.beginControlFlow("if (entity.$L > 0)", column.getSimpleName());
            }
            method.addStatement("registry.getTypeAdapter($T.class).write($S, entity.$L, values)",
                    typeAdapter,
                    getColumnName(column),
                    column.getSimpleName());
            if (pk != null) {
                method.endControlFlow();
            }
        }

        method.addCode("\n");
        method.addStatement("return values");
        return method.build();
    }

    private EntityMetadata obtainEntityMetadata(TypeElement annotatedClass) throws AbortProcessingException {
        Entity entityAnnotation = annotatedClass.getAnnotation(Entity.class);
        EntityMetadata metadata = new EntityMetadata(entityAnnotation.table());

        for (Element element : annotatedClass.getEnclosedElements()) {
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }

            Column columnAnnotation = element.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }

            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                logger.warn(element, "%s is private. Skip column data", element.getSimpleName());
                continue;
            }

            VariableElement field = (VariableElement) element;
            if (field.getAnnotation(PrimaryKey.class) != null) {
                if (!TypeName.LONG.equals(TypeName.get(field.asType()))) {
                    throw new AbortProcessingException(
                            field,
                            "Primary key %s can be only a long type",
                            field.getSimpleName());
                }
                if (metadata.getPrimaryKey() != null) {
                    throw new AbortProcessingException(
                            annotatedClass,
                            "Entity %s must have only one primary key defined",
                            annotatedClass.getSimpleName());
                }
                metadata.setPrimaryKey(field);
            } else {
                metadata.addColumn(field);
            }
        }

        if (!metadata.isValid()) {
            throw new AbortProcessingException(
                    annotatedClass,
                    "Invalid entity %s found. No primary key or empty table name defined",
                    annotatedClass.getSimpleName());
        }

        return metadata;
    }

    private String getColumnName(VariableElement field) {
        String columnName = field.getAnnotation(Column.class).name();
        if (columnName.isEmpty()) {
            columnName = field.getSimpleName().toString().toUpperCase();
        }
        return columnName;
    }

    private TypeName getTypeAdapter(VariableElement field) {
        TypeMirror adapter = null;
        try {
            field.getAnnotation(Column.class).adapter();
        } catch (MirroredTypeException e) {
            // always goes to catch block
            adapter = e.getTypeMirror();
        }

        if (adapter == null) {
            return findDefaultTypeAdapter(field);
        }

        TypeName adapterClassName = ClassName.get(adapter);
        ClassName defaultAdapterClassName = ClassName.get(DefaultTypeAdapter.class);
        if (adapterClassName.equals(defaultAdapterClassName)) {
            adapterClassName = findDefaultTypeAdapter(field);
        }
        return adapterClassName;
    }

    private TypeName findDefaultTypeAdapter(VariableElement element) {
        TypeName elementType = TypeName.get(element.asType());
        Class<? extends TypeAdapter<?>> adapterClass = TypeMapping.findAdapter(elementType);

        return  adapterClass != null ? ClassName.get(adapterClass) : null;
    }

    @DatabaseType
    private String getDatabaseType(VariableElement field) throws AbortProcessingException {
        Column columnAnnotation = field.getAnnotation(Column.class);

        @DatabaseType
        String databaseType = columnAnnotation.databaseType();
        if (StringUtils.isEmpty(databaseType)) {
            databaseType = TypeMapping.findDatabaseType(TypeName.get(field.asType()));
            if (StringUtils.isEmpty(databaseType)) {
                throw new AbortProcessingException(
                        field,
                        "Unknown database type for column field %s of type %s",
                        field,
                        field.asType());
            }
        }
        return databaseType;
    }
}
