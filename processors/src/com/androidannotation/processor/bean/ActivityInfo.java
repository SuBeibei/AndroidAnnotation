package com.androidannotation.processor.bean;

import com.androidannotation.annotations.EActivity;
import com.androidannotation.annotations.ViewById;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActivityInfo {
    private TypeElement activityElement;

    private Map<ViewById, Element> viewElements = new LinkedHashMap<>();

    public ActivityInfo(TypeElement typeElement) {
        activityElement = typeElement;
    }

    public TypeElement getActivityElement() {
        return activityElement;
    }

    public void addViewElement(ViewById annotation ,Element element) {
        viewElements.put(annotation, element);
    }

    public void generateCode(Elements elementUtils, Types typeUtils, Filer filer) throws IOException, ClassNotFoundException {
        String className = activityElement.getSimpleName() + "_";
        String packageName = elementUtils.getPackageOf(activityElement).getQualifiedName().toString();

        int layoutId = activityElement.getAnnotation(EActivity.class).value();

        MethodSpec.Builder onCreateBuilder = MethodSpec.methodBuilder("onCreate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.VOID)
                .addParameter(ClassName.get("android.os", "Bundle"), "savedInstanceState")
                .addStatement("super.onCreate(savedInstanceState)")
                .addStatement("setContentView($L)", layoutId)
                .addCode("\n");

        for (Map.Entry<ViewById, Element> entry : viewElements.entrySet()) {
            int viewId = entry.getKey().value();
            Element element = entry.getValue();
            ClassName viewType = ClassName.get((TypeElement)typeUtils.asElement(element.asType()));

            String viewName = element.getSimpleName().toString();
            onCreateBuilder.addStatement("$T $L = ($T)findViewById($L)", viewType, viewName, viewType, viewId);
        }

        MethodSpec onCreate = onCreateBuilder.build();

        TypeSpec activity = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(activityElement))
                .addMethod(onCreate)
                .build();

        JavaFile.builder(packageName, activity).build().writeTo(filer);
    }

}
