package dev.latvian.mods.rhino.type;

import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link TypeInfoFactory} implementation.
 *
 * <p>{@link Class}-keyed lookups go through a {@link ClassValue}, so each entry
 * is tied to its class's lifetime and unloads with it. {@link TypeVariable}
 * lookups fall back to a synchronized {@link HashMap} (TypeVariables are not
 * Class-shaped, so {@code ClassValue} can't host them).
 *
 * <p>The {@link TypeUtils#IMMUTABLE_CACHE} preset map is consulted first so
 * canonical constants ({@link TypeInfo#OBJECT}, {@link TypeInfo#STRING}, etc.)
 * are returned by identity — preserving the fork's reference-equality
 * {@code TypeInfo.is(TypeInfo)} contract.
 */
final class ClassValueTypeInfoFactory implements TypeInfoFactory {

	private final ClassValue<TypeInfo> classCache = new ClassValue<>() {
		@Override
		protected TypeInfo computeValue(Class<?> c) {
			var preset = TypeUtils.IMMUTABLE_CACHE.get(c);
			if (preset != null) {
				return preset;
			}
			if (c.isArray()) {
				return create(c.getComponentType()).asArray();
			}
			if (c.isEnum()) {
				return new EnumTypeInfo(c);
			}
			if (c.isRecord()) {
				return new RecordTypeInfo(c);
			}
			if (c.isInterface()) {
				return new InterfaceTypeInfo(c);
			}
			return new BasicClassTypeInfo(c);
		}
	};

	private final Map<TypeVariable<?>, VariableTypeInfo> variableCache = new HashMap<>();

	@Override
	public TypeInfo create(Class<?> c) {
		if (c == null || c == Object.class) {
			return TypeInfo.OBJECT;
		}
		if (c == Void.TYPE) {
			return TypeInfo.PRIMITIVE_VOID;
		}
		return classCache.get(c);
	}

	@Override
	public VariableTypeInfo create(TypeVariable<?> typeVariable) {
		synchronized (variableCache) {
			return variableCache.computeIfAbsent(typeVariable, VariableTypeInfo::new);
		}
	}
}
