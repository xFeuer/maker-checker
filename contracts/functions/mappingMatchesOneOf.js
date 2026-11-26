// Checks that the mapping of a discriminator names exactly the schemas of the oneOf next to it.
//
// The rule gives this function a schema that has a discriminator, as the specification writes it: with its
// references, not with what they point to. vacuum runs the function by itself and needs no Node.js for it.

function getSchema() {
    return {
        name: "mappingMatchesOneOf",
        description: "Compares the mapping of a discriminator with the oneOf next to it."
    };
}

function runRule(input) {
    var results = [];
    var discriminator = (input && input.discriminator) || {};
    var mapping = discriminator.mapping || {};
    var references = ((input && input.oneOf) || []).map(function (schema) {
        return schema.$ref;
    });

    Object.keys(mapping).forEach(function (value) {
        if (references.indexOf(mapping[value]) < 0) {
            results.push({
                message: "the mapping of " + value + " names " + mapping[value] + ", which the oneOf does not have"
            });
        }
    });
    references.forEach(function (reference) {
        var mapped = Object.keys(mapping).some(function (value) {
            return mapping[value] === reference;
        });
        if (!mapped) {
            results.push({ message: "the oneOf has " + reference + ", which the mapping does not name" });
        }
    });
    return results;
}
