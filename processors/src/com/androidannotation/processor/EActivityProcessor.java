package com.androidannotation.processor;

import com.androidannotation.annotations.EActivity;
import com.androidannotation.annotations.ViewById;
import com.androidannotation.processor.base.BaseProcessor;
import com.androidannotation.processor.bean.ActivityInfo;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class EActivityProcessor extends BaseProcessor {
    private Map<String, ActivityInfo> activities = new LinkedHashMap<>();

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
                TypeElement activityElement = (TypeElement)annotatedElement;
                ActivityInfo activityInfo = new ActivityInfo(activityElement);
                processActivityElement(activityInfo);
                activities.put(activityElement.getQualifiedName().toString(), activityInfo);
            }
        }

        for (ActivityInfo activityInfo : activities.values()) {
            try {
                activityInfo.generateCode(elementUtils, typeUtils, filer);
            } catch (IOException e) {
                error(activityInfo.getActivityElement(), e.getMessage());
            } catch (ClassNotFoundException e) {
                error(activityInfo.getActivityElement(), e.getMessage());
            }
        }

        activities.clear();

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
                        "The class %s annotated with @%s must inherit from %s",
                        element.getSimpleName().toString(), "@EActivity", "android.app.Activity");
                return false;
            }

            if (superClassType.toString().equals("android.app.Activity")) {
                break;
            }

            currentElement = (TypeElement)typeUtils.asElement(superClassType);
        }
        return true;
    }

    private void processActivityElement(ActivityInfo activityInfo) {
        TypeElement activityElement = activityInfo.getActivityElement();
        for (Element element : elementUtils.getAllMembers(activityElement)) {
            if (element.getKind() == ElementKind.FIELD) {
                ViewById annotation = element.getAnnotation(ViewById.class);
                if (annotation != null) {
                    // 必须继承View
                    TypeElement currentElement = (TypeElement)typeUtils.asElement(element.asType());
                    while (true) {
                        TypeMirror superClassType = currentElement.getSuperclass();

                        if (superClassType.getKind() == TypeKind.NONE) {
                            error(element,
                                    "The field %s annotated with @ViewById must inherit from %s",
                                    element.getSimpleName().toString(), "android.view.View");
                        }

                        if (superClassType.toString().equals("android.view.View")) {
                            break;
                        }
                        currentElement = (TypeElement)typeUtils.asElement(superClassType);
                    }
                    // 不能用private修饰
                    Set<javax.lang.model.element.Modifier> modifiers  = element.getModifiers();
                    if (modifiers.contains(Modifier.PRIVATE)) {
                        error(element, "The field %s annotated with @ViewById shouldn't be modified by private", element.getSimpleName());
                    } else {
                        activityInfo.addViewElement(annotation, element);
                    }
                }
            }
        }
    }
}
