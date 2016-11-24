package pl.whiter13.extension;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static pl.whiter13.extension.ElementUtil.getSuperClass;

@AutoService(AutoValueExtension.class)
public class ComparableAutoValueExtension extends AutoValueExtension {

    private CompareToMethod compareToMethod;

    @Override
    public boolean applicable(Context context) {

        final Types typeUtils = context.processingEnvironment().getTypeUtils();

        final TypeMirror comparable = CompareToMethod.getComparableTypeMirror(context);

        final List<? extends TypeMirror> interfaces = context.autoValueClass().getInterfaces();

        final boolean implementingComparable = isImplementingComparable(context, typeUtils, comparable, interfaces);

        if (implementingComparable) {
            compareToMethod = CompareToMethod.getCompareToMethod(context);

            return compareToMethod.isValid();
        }

        return false;
    }

    private boolean isImplementingComparable(Context context,
                                             Types typeUtils,
                                             TypeMirror comparable,
                                             List<? extends TypeMirror> interfaces) {

        for (TypeMirror singleInterface : interfaces) {

            final TypeMirror erasure = typeUtils.erasure(singleInterface);

            if (typeUtils.isAssignable(comparable, erasure)) {

                final boolean fullyAssignable = isGenericTypeAssignable(context,
                        typeUtils,
                        (DeclaredType) singleInterface);

                if (fullyAssignable) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGenericTypeAssignable(Context context,
                                            Types typeUtils,
                                            DeclaredType singleInterface) {

        final List<? extends TypeMirror> typeArguments = singleInterface.getTypeArguments();

        final TypeMirror genericComparableTypeMirror = typeArguments.get(0);

        final TypeMirror classTypeMirror = context.autoValueClass().asType();

        return typeUtils.isSameType(genericComparableTypeMirror, classTypeMirror);
    }


    @Override
    public Set<ExecutableElement> consumeMethods(Context context) {
        final ImmutableSet.Builder<ExecutableElement> methods = new ImmutableSet.Builder<>();

        for (ExecutableElement element : context.abstractMethods()) {
            switch (element.getSimpleName().toString()) {
                case CompareToMethod.METHOD_NAME:
                    methods.add(element);
                    break;
            }
        }
        return methods.build();
    }

    @Override
    public String generateClass(Context context,
                                String className,
                                String classToExtend,
                                boolean isFinal) {

        final TypeVariableName[] typeVariables = ElementUtil.getTypeVariables(context.autoValueClass());

        final TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .addModifiers(isFinal ? Modifier.FINAL : Modifier.ABSTRACT)
                .addTypeVariables(Arrays.asList(typeVariables))
                .superclass(getSuperClass(context.packageName(), classToExtend, typeVariables))
                .addMethod(ElementUtil.newConstructor(context.properties()))
                .addMethod(compareToMethod.toMethodSpec());

        final JavaFile javaFile = JavaFile
                .builder(context.packageName(),
                        subclass.build())
                .build();

        return javaFile.toString();

    }

    @Override
    public boolean mustBeFinal(Context context) {
        return false;
    }
}
