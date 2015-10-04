package com.androidannotation.processor;

import com.androidannotation.annotations.EActivity;
import com.androidannotation.processor.base.BaseProcessor;
import com.squareup.javapoet.*;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class EActivityProcessor extends BaseProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<String>();
        annotations.add(EActivity.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(EActivity.class)) {
            if (isValid(annotatedElement)) {
                try {
                    generateCode(annotatedElement);
                } catch (IOException e) {
                    error(annotatedElement, e.getMessage());
                }
            }
        }
        return true;
    }

    private boolean isValid(Element element) {
        // 必须是class
        if (element.getKind() != ElementKind.CLASS) {
            error(element, "Only classes can be annotated width @%s", EActivity.class.getSimpleName());
            return false;
        }
        // 必须继承Activity
        TypeElement currentElement = (TypeElement)element;
        while (true) {
            TypeMirror superClassType = currentElement.getSuperclass();

            if (superClassType.getKind() == TypeKind.NONE) {
                error(element,
                        "The class %s annotated width @%s must inherit from %s",
                        element.getSimpleName().toString(), "android.app.Activity");
                return false;
            }

            if (superClassType.toString().equals("android.app.Activity")) {
                break;
            }

            currentElement = (TypeElement)typeUtils.asElement(superClassType);
        }
        return true;
    }

    private void generateCode(Element element) throws IOException {
        TypeElement classElement = (TypeElement)element;
        String className = classElement.getSimpleName() + "_";
        String packageName = elementUtils.getPackageOf(classElement).getQualifiedName().toString();

        MethodSpec onCreate = MethodSpec.methodBuilder("onCreate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addParameter(ClassName.get("android.os", "Bundle"), "savedInstanceState")
                .addStatement("super.onCreate(savedInstanceState)")
                .addStatement("setContentView($L)", classElement.getAnnotation(EActivity.class).value())
                .build();

        TypeSpec activity = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(classElement))
                .addMethod(onCreate)
                .build();

        JavaFile.builder(packageName, activity).build().writeTo(filer);
    }
}
