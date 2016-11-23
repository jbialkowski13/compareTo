package pl.whiter13.extension;

import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import java.util.Map;
import java.util.Set;

public class Property {

    /**
     * Builds a List of {@link Property} for the given {@link AutoValueExtension.Context}.
     */
    public static ImmutableList<Property> buildProperties(AutoValueExtension.Context context) {
        ImmutableList.Builder<Property> values = ImmutableList.builder();
        for (Map.Entry<String, ExecutableElement> entry : context.properties().entrySet()) {
            values.add(new Property(entry.getKey(), entry.getValue()));
        }
        return values.build();
    }

    private final String methodName;
    private final String humanName;
    private final ExecutableElement element;
    private final TypeName type;
    private final ImmutableSet<String> annotations;

    public Property(String humanName, ExecutableElement element) {
        this.methodName = element.getSimpleName().toString();
        this.humanName = humanName;
        this.element = element;
        type = TypeName.get(element.getReturnType());
        annotations = ElementUtil.buildAnnotations(element);
    }

    /**
     * The method name of the property.
     */
    public String methodName() {
        return methodName;
    }

    /**
     * The human readable name of the property. If all properties use {@code get} or {@code is}
     * prefixes, this name will be different from {@link #methodName()}.
     */
    public String humanName() {
        return humanName;
    }

    /**
     * The underlying ExecutableElement representing the get method of the property.
     */
    public ExecutableElement element() {
        return element;
    }

    /**
     * The return type of the property.
     */
    public TypeName type() {
        return type;
    }

    /**
     * The set of annotations present on the original property.
     */
    public Set<String> annotations() {
        return annotations;
    }

    /**
     * True if the property can be null.
     */
    public Boolean nullable() {
        return annotations.contains("Nullable");
    }

    @Override
    public String toString() {
        return "Property{" +
                "methodName='" + methodName + '\'' +
                ", humanName='" + humanName + '\'' +
                ", element=" + element +
                ", type=" + type +
                ", annotations=" + annotations +
                '}';
    }
}
