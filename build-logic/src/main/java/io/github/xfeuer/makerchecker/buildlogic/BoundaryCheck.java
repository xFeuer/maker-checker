package io.github.xfeuer.makerchecker.buildlogic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.work.DisableCachingByDefault;

/**
 * Fails when a project under {@code platform/} is among the dependencies of this project.
 *
 * <p>The automation tests the platform from the outside, so it never depends on its code. See ADR 0002.
 *
 * <p>The check reads resolved dependency graphs. It also finds a platform project that arrives through another
 * project.
 */
@DisableCachingByDefault(because = "The check has no outputs")
public abstract class BoundaryCheck extends DefaultTask {

    private static final String PLATFORM = ":platform:";

    /** The resolved dependency graph of each configuration to check, by the name of the configuration. */
    @Input
    public abstract MapProperty<String, ResolvedComponentResult> getGraphs();

    @TaskAction
    void check() {
        // Several configurations usually share a path, so the report groups them under it.
        Map<String, List<String>> configurationsByPath = new TreeMap<>();
        new TreeMap<>(getGraphs().get()).forEach((configuration, root) -> {
            for (String path : pathsToThePlatform(root)) {
                configurationsByPath
                        .computeIfAbsent(path, key -> new ArrayList<>())
                        .add(configuration);
            }
        });
        if (configurationsByPath.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("The automation must not depend on the platform."
                + " See docs/adr/0002-one-repository-with-a-black-box-boundary.md.");
        configurationsByPath.forEach((path, configurations) ->
                message.append("\n  ").append(path).append("\n      in ").append(String.join(", ", configurations)));
        throw new VerificationException(message.toString());
    }

    /** Walks the graph breadth-first, so each platform project is reported with a shortest path to it. */
    private static List<String> pathsToThePlatform(ResolvedComponentResult root) {
        Map<ComponentIdentifier, ComponentIdentifier> cameFrom = new HashMap<>();
        cameFrom.put(root.getId(), root.getId());
        Deque<ResolvedComponentResult> queue = new ArrayDeque<>(List.of(root));
        List<String> paths = new ArrayList<>();
        while (!queue.isEmpty()) {
            ResolvedComponentResult component = queue.removeFirst();
            for (DependencyResult dependency : component.getDependencies()) {
                // A constraint brings no code, and a dependency that did not resolve has nothing behind it.
                if (dependency.isConstraint() || !(dependency instanceof ResolvedDependencyResult resolved)) {
                    continue;
                }
                ResolvedComponentResult selected = resolved.getSelected();
                if (cameFrom.putIfAbsent(selected.getId(), component.getId()) != null) {
                    continue;
                }
                if (isOnThePlatform(selected.getId())) {
                    paths.add(path(selected.getId(), cameFrom));
                } else {
                    queue.addLast(selected);
                }
            }
        }
        return paths;
    }

    private static boolean isOnThePlatform(ComponentIdentifier id) {
        return id instanceof ProjectComponentIdentifier project
                && project.getProjectPath().startsWith(PLATFORM);
    }

    /** A project shows as its path, as in a build script. Anything else shows as Gradle names it. */
    private static String name(ComponentIdentifier id) {
        return id instanceof ProjectComponentIdentifier project ? project.getProjectPath() : id.getDisplayName();
    }

    private static String path(ComponentIdentifier to, Map<ComponentIdentifier, ComponentIdentifier> cameFrom) {
        Deque<String> names = new ArrayDeque<>();
        ComponentIdentifier id = to;
        while (true) {
            names.addFirst(name(id));
            ComponentIdentifier previous = cameFrom.get(id);
            if (previous.equals(id)) {
                return String.join(" -> ", names);
            }
            id = previous;
        }
    }
}
