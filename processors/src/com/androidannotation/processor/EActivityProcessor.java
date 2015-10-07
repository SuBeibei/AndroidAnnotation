package com.androidannotation.processor;

import com.androidannotation.annotations.AfterViewInject;
import com.androidannotation.annotations.Click;
import com.androidannotation.annotations.EActivity;
import com.androidannotation.annotations.ViewById;
import com.androidannotation.processor.base.BaseProcessor;
import com.androidannotation.processor.bean.ActivityInfo;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
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
        TypeMirror activityTypeMirror = elementUtils.getTypeElement("android.app.Activity").asType();
        if (!typeUtils.isSubtype(element.asType(), activityTypeMirror)) {
            error(element,
                    "The class %s annotated with @%s must inherit from %s",
                    element.getSimpleName().toString(), "@EActivity", "android.app.Activity");
            return false;
        }
        return true;
    }

    private void processActivityElement(ActivityInfo activityInfo) {
        TypeElement activityElement = activityInfo.getActivityElement();
        for (Element element : elementUtils.getAllMembers(activityElement)) {
            if (element.getKind() == ElementKind.FIELD) {
                processViewById(element, activityInfo);
            } else if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement)element;
                if (element.getAnnotation(AfterViewInject.class) != null) {
                    processAfterViewInject(executableElement, activityInfo);
                } else if (element.getAnnotation(Click.class) != null) {
                    processClick(executableElement, activityInfo);
                }
            }
        }
    }

    private void processClick(ExecutableElement executableElement, ActivityInfo activityInfo) {
        // 不能用private修饰
        Set<Modifier> modifiers = executableElement.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            error(executableElement, "the method %s annotated with @Click can't be modified by private.", executableElement.getSimpleName().toString());
        }
        // 没有参数
        if (executableElement.getParameters().size() != 1 ) {
            error(executableElement, "the method %s annotated with @Click can't have parameters", executableElement.getSimpleName().toString());
        }
        TypeMirror viewTypeMirror = elementUtils.getTypeElement("android.view.View").asType();
        if (!typeUtils.isSubtype(executableElement.getParameters().get(0).asType(), viewTypeMirror)) {
            error(executableElement, "the only one parameter of the method %s annotated with @Click must be subtype of android.view.View", executableElement.getSimpleName().toString());
        }
        // 返回值为void
        if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
            error(executableElement, "the method %s annotated with @Click must return void", executableElement.getSimpleName().toString());
        }
        activityInfo.addClickMethod(executableElement, executableElement.getAnnotation(Click.class));
    }

    private void processAfterViewInject(ExecutableElement executableElement, ActivityInfo activityInfo) {
        // 不能用private修饰
        Set<Modifier> modifiers = executableElement.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            error(executableElement, "the method %s annotated with @AfterViewInject can't be modified by private.", executableElement.getSimpleName().toString());
        }
        // 没有参数
        if (executableElement.getParameters().size() != 0) {
            error(executableElement, "the method %s annotated with @AfterViewInject can't have parameters", executableElement.getSimpleName().toString());
        }
        // 返回值为void
        if (executableElement.getReturnType().getKind() != TypeKind.VOID) {
            error(executableElement, "the method %s annotated with @AfterViewInject must return void", executableElement.getSimpleName().toString());
        }
        activityInfo.addAfterViewInjectMethod(executableElement);
    }

    private void processViewById(Element element, ActivityInfo activityInfo) {
        ViewById annotation = element.getAnnotation(ViewById.class);
        if (annotation != null) {
            // 必须继承View
            TypeMirror viewTypeMirror = elementUtils.getTypeElement("android.view.View").asType();
            if (!typeUtils.isSubtype(element.asType(), viewTypeMirror)) {
                error(element,
                        "The field %s annotated with @ViewById must inherit from %s",
                        element.getSimpleName().toString(), "android.view.View");
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
