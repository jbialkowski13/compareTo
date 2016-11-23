package pl.whiter13.extension;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;
import java.util.Map;

public final class ElementUtil {


    private ElementUtil() {
        throw new AssertionError("No instances.");
    }

    static ImmutableSet<String> buildAnnotations(ExecutableElement element) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
        }
        return builder.build();
    }

    static TypeName getSuperClass(String packageName, String classToExtend, TypeName[] typeVariables) {
        final ClassName superClassWithoutParameters = ClassName.get(packageName, classToExtend);

        if (typeVariables.length > 0) {
            return ParameterizedTypeName.get(superClassWithoutParameters, typeVariables);
        } else {
            return superClassWithoutParameters;
        }
    }

    static CodeBlock newConstructorCall(CodeBlock constructorName, Object[] properties) {
        StringBuilder params = new StringBuilder("(");
        for (int i = properties.length; i > 0; i--) {
            params.append("$N");
            if (i > 1) params.append(", ");
        }
        params.append(")");
        return CodeBlock.builder()
                .add(constructorName)
                .addStatement(params.toString(), properties)
                .build();
    }

    static TypeVariableName[] getTypeVariables(TypeElement autoValueClass) {
        List<? extends TypeParameterElement> parameters = autoValueClass.getTypeParameters();
        TypeVariableName[] typeVariables = new TypeVariableName[parameters.size()];
        for (int i = 0, length = typeVariables.length; i < length; i++) {
            typeVariables[i] = TypeVariableName.get(parameters.get(i));
        }
        return typeVariables;
    }


    static MethodSpec newConstructor(Map<String, ExecutableElement> properties) {
        final List<ParameterSpec> params = Lists.newArrayList();

        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            TypeName typeName = TypeName.get(entry.getValue().getReturnType());
            params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
        }

        CodeBlock code = ElementUtil.newConstructorCall(CodeBlock.of("super"), properties.keySet().toArray());

        return MethodSpec.constructorBuilder().addParameters(params).addCode(code).build();
    }

}
