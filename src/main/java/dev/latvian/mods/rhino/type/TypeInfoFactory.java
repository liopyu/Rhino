package dev.latvian.mods.rhino.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.function.Supplier;

/**
 * Pluggable factory for {@link TypeInfo} instances.
 *
 * <p>The {@link #GLOBAL} singleton is what every {@code TypeInfo.of(...)} call
 * routes through, and is backed by {@link java.lang.ClassValue} so per-class
 * entries are collected with the class itself rather than pinned in a static
 * map. Embedders that want different caching semantics (e.g. a fully unbounded
 * cache, or no cache) can implement this interface and call its {@code create}
 * methods directly.
 *
 * <p>Adapted from upstream's {@code org.mozilla.javascript.lc.type.TypeInfoFactory}.
 * The fork drops the {@code VarScope}-bound dispatch and the Android-target
 * fallback (this fork is JDK-21 only).
 */
public interface TypeInfoFactory {

	TypeInfoFactory GLOBAL = new ClassValueTypeInfoFactory();

	TypeInfo create(Class<?> c);

	VariableTypeInfo create(TypeVariable<?> typeVariable);

	default TypeInfo create(Type type) {
		return switch (type) {
			case Class<?> clz -> create(clz);
			case ParameterizedType paramType -> create(paramType.getRawType()).withParams(createArray(paramType.getActualTypeArguments()));
			case GenericArrayType arrType -> create(arrType.getGenericComponentType()).asArray();
			case TypeVariable<?> variable -> create(variable);
			case WildcardType wildcard -> {
				var lower = wildcard.getLowerBounds();
				if (lower.length == 0) {
					var upper = wildcard.getUpperBounds();
					if (upper.length == 0 || upper[0] == Object.class) {
						yield TypeInfo.NONE;
					}
					yield create(upper[0]);
				} else {
					yield create(lower[0]);
				}
			}
			case null, default -> TypeInfo.NONE;
		};
	}

	default TypeInfo[] createArray(Type[] array) {
		if (array.length == 0) {
			return TypeInfo.EMPTY_ARRAY;
		}
		var arr = new TypeInfo[array.length];
		for (int i = 0; i < array.length; i++) {
			arr[i] = create(array[i]);
		}
		return arr;
	}

	default TypeInfo safeCreate(Supplier<Type> supplier) {
		try {
			return create(supplier.get());
		} catch (Throwable ignored) {
			return TypeInfo.NONE;
		}
	}

	default TypeInfo[] safeCreateArray(Supplier<Type[]> supplier) {
		try {
			return createArray(supplier.get());
		} catch (Throwable ignored) {
			return TypeInfo.EMPTY_ARRAY;
		}
	}
}
