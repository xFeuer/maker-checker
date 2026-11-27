package io.github.xfeuer.makerchecker.chassis.contract.testing;

import io.github.xfeuer.makerchecker.chassis.contract.Contract;
import io.github.xfeuer.makerchecker.chassis.problem.CommonProblem;
import io.github.xfeuer.makerchecker.chassis.problem.ProblemType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * What a test of an application asserts about the whole of its contract: its operations and its types of problem.
 * See ADR 0019.
 *
 * <p>Each assertion takes a list of what the contract has and the application does not build yet. An entry has to
 * leave the list with the change that builds it, and the list is empty when the application is complete.
 */
public final class ContractAssertions {

    private ContractAssertions() {}

    /**
     * The application maps every operation of its contract, and no other.
     *
     * @param basePackage the package of the application. A mapping of another package, such as the error page of
     *     the chassis, is no operation of the application.
     */
    public static void assertOperations(
            Contract contract, RequestMappingHandlerMapping mappings, String basePackage, Set<String> notBuiltYet) {
        Set<String> mapped = new TreeSet<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getBeanType().getPackageName().startsWith(basePackage)) {
                Set<String> methods = mapping.getMethodsCondition().getMethods().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
                for (String path : mapping.getPatternValues()) {
                    (methods.isEmpty() ? Set.of("ANY") : methods).forEach(method -> mapped.add(method + " " + path));
                }
            }
        });
        failOn(operationFindings(contract.operations(), mapped, notBuiltYet));
    }

    /**
     * Every type of problem that the application declares has the status and the title of the contract, and the
     * application declares every type that its contract names. The types of the chassis count as declared.
     */
    public static void assertProblemTypes(
            Contract contract, Collection<? extends ProblemType> declared, Set<String> notBuiltYet) {
        failOn(problemFindings(contract.problemTypes(), declared, List.of(CommonProblem.values()), notBuiltYet));
    }

    static List<String> operationFindings(Set<String> contract, Set<String> mapped, Set<String> notBuiltYet) {
        // The name of a path parameter is the business of each side: {holdId} here can be {id} there.
        Map<String, String> inContract = byShape(contract);
        Map<String, String> inApplication = byShape(mapped);
        Map<String, String> waiting = byShape(notBuiltYet);
        List<String> findings = new ArrayList<>();
        inContract.forEach((shape, name) -> {
            if (!inApplication.containsKey(shape) && !waiting.containsKey(shape)) {
                findings.add("The contract has %s, and the application does not map it.".formatted(name));
            }
        });
        inApplication.forEach((shape, name) -> {
            if (!inContract.containsKey(shape)) {
                findings.add("The application maps %s, and the contract does not have it.".formatted(name));
            }
        });
        waiting.forEach((shape, name) -> {
            if (!inContract.containsKey(shape)) {
                findings.add("The list of what is not built yet has %s, and the contract does not.".formatted(name));
            } else if (inApplication.containsKey(shape)) {
                findings.add("%s is built. It has to leave the list of what is not built yet.".formatted(name));
            }
        });
        return findings;
    }

    static List<String> problemFindings(
            Set<Contract.ProblemExample> contract,
            Collection<? extends ProblemType> own,
            Collection<? extends ProblemType> shared,
            Set<String> notBuiltYet) {
        Map<String, Contract.ProblemExample> named = new LinkedHashMap<>();
        contract.forEach(example -> named.put(nameOf(example.type().toString()), example));
        List<String> findings = new ArrayList<>();
        Set<String> declared = new TreeSet<>();
        for (ProblemType type : own) {
            String name = nameOf(type.type().toString());
            declared.add(name);
            if (!named.containsKey(name)) {
                findings.add("The application declares %s, and the contract does not name it.".formatted(name));
            }
        }
        shared.forEach(type -> declared.add(nameOf(type.type().toString())));
        List<ProblemType> all = new ArrayList<>(own);
        all.addAll(shared);
        for (ProblemType type : all) {
            Contract.ProblemExample example = named.get(nameOf(type.type().toString()));
            if (example != null
                    && (example.status() != type.status() || !example.title().equals(type.title()))) {
                findings.add("%s is %d \"%s\" in the code, and %d \"%s\" in the contract."
                        .formatted(
                                nameOf(type.type().toString()),
                                type.status(),
                                type.title(),
                                example.status(),
                                example.title()));
            }
        }
        named.keySet().forEach(name -> {
            if (!declared.contains(name) && !notBuiltYet.contains(name)) {
                findings.add("The contract names %s, and the application does not declare it.".formatted(name));
            }
        });
        notBuiltYet.forEach(name -> {
            if (!named.containsKey(name)) {
                findings.add("The list of what is not built yet has %s, and the contract does not.".formatted(name));
            } else if (declared.contains(name)) {
                findings.add("%s is declared. It has to leave the list of what is not built yet.".formatted(name));
            }
        });
        return findings;
    }

    private static Map<String, String> byShape(Set<String> operations) {
        Map<String, String> shapes = new LinkedHashMap<>();
        new TreeSet<>(operations).forEach(name -> shapes.put(name.replaceAll("\\{[^}]*}", "{}"), name));
        return shapes;
    }

    /** The last part of the address of a type, such as insufficient-funds. */
    private static String nameOf(String type) {
        return type.substring(type.lastIndexOf('/') + 1);
    }

    private static void failOn(List<String> findings) {
        if (!findings.isEmpty()) {
            throw new AssertionError("The application and its contract differ:\n  " + String.join("\n  ", findings));
        }
    }
}
