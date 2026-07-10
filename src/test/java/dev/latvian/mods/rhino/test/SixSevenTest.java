package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * regression test for issue #67 because this issue just likes to haunt me ig
 */
@SuppressWarnings("unused")
public class SixSevenTest {
	public record TestVersion(String version) implements Comparable<TestVersion> {
		@Override
		public int compareTo(TestVersion o) {
			return version.compareTo(o.version);
		}

		@Override
		public String toString() {
			return version;
		}
	}

	public static final RhinoTest TEST = new RhinoTest("non-string-key-map").withScopeAction((cx, rootScope) -> {
		// replicating Java.loadClass from the example code, minus DefaultArtifactVersion => TestVersion
		cx.addToScope(rootScope, "$TreeMap", TreeMap.class);
		cx.addToScope(rootScope, "$TestVersion", TestVersion.class);
		cx.addToScope(rootScope, "$ArrayList", ArrayList.class);
	});

	@Test
	public void issueNumberSixSeven() {
		TEST.test("issueNumberSixSeven", """
			let treeMap = new $TreeMap()
			
			populateTreeMap("4.0", "Testing 4.0")
			populateTreeMap("4.5", "Testing 4.5")
			
			function populateTreeMap(version, component) {
				treeMap.computeIfAbsent(new $TestVersion(version), (key) => new $ArrayList()).addLast(component)
			}
			
			console.info(treeMap.size())
			""", """
			2
			""");
	}
}
