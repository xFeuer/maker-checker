// Checks that no two entries of a map have the same description.
//
// The rule gives this function the examples of a specification, by their names. An example that repeats the
// description of another one says nothing about itself.

function getSchema() {
    return {
        name: "descriptionsDiffer",
        description: "Reports an entry of a map that has the description of an earlier entry."
    };
}

function runRule(input) {
    var results = [];
    var described = [];

    Object.keys(input || {}).forEach(function (name) {
        var description = input[name] && input[name].description;
        if (typeof description !== "string") {
            return;
        }
        var earlier = described.filter(function (entry) {
            return entry.description === description;
        });
        if (earlier.length > 0) {
            results.push({ message: name + " has the description of " + earlier[0].name });
        } else {
            described.push({ name: name, description: description });
        }
    });
    return results;
}
