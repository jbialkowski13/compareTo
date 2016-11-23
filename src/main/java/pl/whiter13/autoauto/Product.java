package pl.whiter13.autoauto;


import com.google.auto.value.AutoValue;
import pl.whiter13.extension.CompareBy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AutoValue
public abstract class Product implements Comparable<Product> {

    @Nonnull
    public static Product create(@Nonnull String name,
                                 @Nonnull Price price,
                                 @Nullable String description) {


        return new AutoValue_Product(
                name,
                price,
                description
        );
    }

    @Nonnull
    public abstract String getName();

    @Nonnull
    @CompareBy
    public abstract Price getPrice();

    @Nullable
    public abstract String getDescription();
}
