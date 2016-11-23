package pl.whiter13.autoauto;

import com.google.auto.value.AutoValue;
import pl.whiter13.extension.CompareBy;

import javax.annotation.Nonnull;

@AutoValue
public abstract class Price implements Comparable<Price> {

    @Nonnull
    public static Price create(@Nonnull Double value, @Nonnull Double tax) {
        return new AutoValue_Price(value, tax);
    }

    @Nonnull
    @CompareBy
    public abstract Double getValue();

    @Nonnull
    public abstract Double getTax();
}
