package pl.whiter13.extension;


import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

import static com.google.auto.common.MoreElements.isAnnotationPresent;

class CompareToMethod {

    public static final String METHOD_NAME = "compareTo";

    private static final String OTHER_ARG_NAME = "other";

    private static final String COMPARABLE = "java.lang.Comparable";

    private static final Class<CompareBy> COMPARE_BY_CLASS = CompareBy.class;

    private final List<Property> properties;
    private final ProcessingEnvironment environment;
    private final TypeElement selfClass;
    private final TypeMirror comparableTypeMirror;

    CompareToMethod(AutoValueExtension.Context context) {
        this.properties = Property.buildProperties(context);
        this.environment = context.processingEnvironment();
        this.selfClass = context.autoValueClass();
        this.comparableTypeMirror = getComparableTypeMirror(context);
    }

    static TypeMirror getComparableTypeMirror(AutoValueExtension.Context context) {
        return context
                .processingEnvironment()
                .getElementUtils()
                .getTypeElement(COMPARABLE)
                .asType();
    }

    static CompareToMethod getCompareToMethod(AutoValueExtension.Context context) {
        return new CompareToMethod(
                context
        );
    }

    MethodSpec toMethodSpec() {
        final ClassName className = ClassName.get(selfClass);

        final ParameterSpec other = ParameterSpec
                .builder(className, OTHER_ARG_NAME)
                .build();

        return MethodSpec.methodBuilder(METHOD_NAME)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(other)
                .returns(TypeName.INT)
                .addCode(generateCompare(other))
                .build();
    }

    private CodeBlock generateCompare(ParameterSpec other) {

        final CodeBlock.Builder builder = CodeBlock.builder();

        final Property compareByProperty = findCompareBy();

        checkIsComparable(compareByProperty);

        final String methodName = compareByProperty.methodName();

        builder.addStatement("return $N()." + METHOD_NAME + "($N.$N())",
                methodName,
                other,
                methodName);

        return builder.build();
    }

    private void checkIsComparable(Property property) {

        final TypeMirror returnType = property.element().getReturnType();

        final Types typeUtils = environment.getTypeUtils();

        final List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(returnType);

        for (TypeMirror typeMirror : typeMirrors) {
            TypeMirror erasure = typeUtils.erasure(typeMirror);
            if (typeUtils.isAssignable(erasure, comparableTypeMirror)) {
                return;
            }
        }

        throw new IllegalStateException("Property " + property.methodName() + " is not implementing "
                + comparableTypeMirror.toString());
    }

    private Property findCompareBy() {
        final ImmutableList.Builder<Property> compareByPropertiesBuilder = new ImmutableList.Builder<>();

        properties.stream()
                .filter(property -> isAnnotationPresent(property.element(), COMPARE_BY_CLASS))
                .forEach(compareByPropertiesBuilder::add);

        final ImmutableList<Property> compareByProperties = compareByPropertiesBuilder.build();

        validatePropertyList(compareByProperties);

        return compareByProperties.get(0);
    }

    private void validatePropertyList(ImmutableList<Property> compareByProperties) {
        if (compareByProperties.size() > 1) {
            throw new IllegalStateException("Only one " + COMPARE_BY_CLASS.getName() + " is allowed in class");
        } else if (compareByProperties.isEmpty()) {
            throw new IllegalStateException("Missing " + COMPARE_BY_CLASS.getName() + " on Comparable field");
        }
    }
}
