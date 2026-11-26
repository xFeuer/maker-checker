// Checks that the example of a problem states the status code of the response that it is an example of.
//
// The rule gives this function the responses of an operation, by their status codes, with every reference
// resolved. The default response has no code of its own, so its examples may state any status.

function getSchema() {
    return {
        name: "problemStatusMatchesResponse",
        description: "Compares the status of each problem example with the status code of its response."
    };
}

function runRule(input) {
    var results = [];

    Object.keys(input || {}).forEach(function (code) {
        if (!/^[1-5][0-9][0-9]$/.test(code)) {
            return;
        }
        var content = (input[code] && input[code].content) || {};
        var problem = content["application/problem+json"] || {};
        var examples = problem.examples || {};

        Object.keys(examples).forEach(function (name) {
            var value = examples[name] && examples[name].value;
            if (value && value.status !== Number(code)) {
                results.push({
                    message: "the example " + name + " of the response " + code + " states the status " + value.status
                });
            }
        });
    });
    return results;
}
