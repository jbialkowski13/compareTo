package pl.whiter13.extension;


import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
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

    private ImmutableList<Property> compareByProperties;

    private CompareToMethod(AutoValueExtension.Context context) {

        this.properties = Property.buildProperties(context);
        this.environment = context.processingEnvironment();
        this.selfClass = context.autoValueClass();

        this.comparableTypeMirror = getComparableTypeMirror(context);

        this.compareByProperties = getCompareByProperties();
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

    boolean isValid() {

        final boolean hasOnlyOneCompareByExecutableElements =
                hasOnlyOneCompareByExecutableElements(compareByProperties);

        if (hasOnlyOneCompareByExecutableElements) {
            final Property compareByProperty = compareByProperties.get(0);

            return checkIsComparable(compareByProperty);
        }


        return false;
    }

    private boolean hasOnlyOneCompareByExecutableElements(ImmutableList<Property> compareByProperties) {
        if (compareByProperties.size() > 1) {
            final String message = String.format("Only one %s is allowed in class", COMPARE_BY_CLASS.getName());
            printErrorMessage(message);
            return false;
        } else if (compareByProperties.isEmpty()) {
            final String message = String.format("Missing %s annotation on method in %s", COMPARE_BY_CLASS.getName(), selfClass.toString());
            printErrorMessage(message);
            return false;
        }
        return true;
    }


    private CodeBlock generateCompare(ParameterSpec other) {

        final CodeBlock.Builder builder = CodeBlock.builder();

        final Property compareByProperty = compareByProperties.get(0);

        final String methodName = compareByProperty.methodName();

        builder.addStatement("return $N()." + METHOD_NAME + "($N.$N())",
                methodName,
                other,
                methodName);

        return builder.build();
    }

    private boolean checkIsComparable(Property property) {

        final TypeMirror returnType = property.element().getReturnType();

        final Types typeUtils = environment.getTypeUtils();

        final List<? extends TypeMirror> typeMirrors = typeUtils.directSupertypes(returnType);

        for (TypeMirror typeMirror : typeMirrors) {
            TypeMirror erasure = typeUtils.erasure(typeMirror);
            if (typeUtils.isAssignable(erasure, comparableTypeMirror)) {
                return true;
            }
        }

        final String message = String.format("%s.%s is not implementing %s",
                selfClass.toString(),
                property.methodName(),
                comparableTypeMirror.toString());

        printErrorMessage(message);

        return false;
    }


    private ImmutableList<Property> getCompareByProperties() {
        final ImmutableList.Builder<Property> compareByPropertiesBuilder = new ImmutableList.Builder<>();

        properties.stream()
                .filter(property -> isAnnotationPresent(property.element(), COMPARE_BY_CLASS))
                .forEach(compareByPropertiesBuilder::add);

        return compareByPropertiesBuilder.build();
    }

    private void printErrorMessage(String message) {
        environment.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }
}
