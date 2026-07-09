package dev.latvian.mods.rhino;

import java.io.Serial;

public class GeneratorState {
	public static final int GENERATOR_SEND = 0;
	public static final int GENERATOR_THROW = 1;
	public static final int GENERATOR_CLOSE = 2;

	public static class GeneratorClosedException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 2561315658662379681L;

		private final Object value;

		public GeneratorClosedException() {
			this(Undefined.INSTANCE);
		}

		public GeneratorClosedException(Object value) {
			this.value = value;
		}

		public Object getValue() {
			return value;
		}
	}

	int operation;
	Object value;
	RuntimeException returnedException;

	GeneratorState(int operation, Object value) {
		this.operation = operation;
		this.value = value;
	}
}
