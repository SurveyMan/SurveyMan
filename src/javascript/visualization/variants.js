var makeVariantDisplay = function (variants, sm) {

    var circleRadius = 30,
        padding = 10,
        circleDim = (circleRadius * 2) + padding,
        circlesPerRow = width / circleDim,
        circlesPerCol = height / circleDim,
        processData = function (d) {
        return {
            opacity : _.reduce(d, function(a, b) { a * b.val.pvalue }, 1),
            questions : _.uniq(_.flatten(_.map(d, function (_d) { return [_d.q1, _d.q2]; }))),
            data : _.map(d, function(_d) {
                        return {
                            q1 : sm.getQuestionById(_d.q1),
                            q2 : sm.getQuestionById(_d.q2),
                            ct1 : +_d.ct1,
                            ct2 : +_d.ct2,
                            pval : JSON.parse(_d.val.pvalue)
                        };
                    })
        };
    };

    d3.select("#vars").append("text").text("Each circle is a collection of variants; the color intensity indicates the number of pairwise comparisons that appear to be drawn from different distributions.\n");

    var svg = d3.select("#vars").append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.left + margin.right)
                .append("g")
                .attr("transform", "translate("+ margin.left + "," + margin.top + ")");

    var summaries = svg.selectAll("#vars")
        .data(_.map(variants, processData))
        .enter()
        .append("circle")
        .attr("r", 30)
        .attr("cx", function(d, i) { return (width % circlesPerRow  * i) + margin.left; })
        .attr("cy", function(d, i) { return ((height / 60) * i) + margin.top; })
        .attr("fill", "green")
        .attr("opacity", function (d) { d.opacity; })
        .append("title")
        .text(function (d) { return _.reduce(_.map(d.questions, function (q) { return q.qtext+"\n"; }),
                                        function (a, b) { return a + b; },
                                        "");
                                        });



};