
var zoomVariant = function(d, sm) {

    $("#questionCloseup").empty();
    var pairDiv = document.createElement("div");
    $(pairDiv).attr({"width" : "100%", "align" : "center"});
    var data = d.data;
    console.log(data, data.length);
    for (var i = 0 ; i < data.length ; i++) {
        $(pairDiv).append(data[i].pval);
        var q1 = data[i].q1,
            q2 = data[i].q2,
            q1Div = getQuestionHTML(q1, data[i].ct1, sm),
            q2Div = getQuestionHTML(q2, data[i].ct2, sm);
        $(q1Div).attr({"float" : "left", "align" : "left", "width" : "300px"});
        $(q2Div).attr({"float" : "right", "align" : "right", "width" : "300px"});
        $(q1Div).append("<b>#/Responses</b>:&nbsp;"+data[i].ct1);
        $(q2Div).append("<b>#/Responses</b>:&nbsp;"+data[i].ct2);
        makeResponseChart(q1, getResponseCounts(q1), q1Div);
        makeResponseChart(q2, getResponseCounts(q2), q2Div);
        $(pairDiv).append(q1Div);
        $(pairDiv).append(q2Div);
        $("#questionCloseup").append(pairDiv);
    }
    $("#questionCloseup").show();

};

var makeVariantDisplay = function (variants, sm) {

    var circleRadius = 30,
        padding = 10,
        circleDim = (circleRadius * 2) + padding,
        circlesPerRow = Math.floor(width / circleDim),
        height = Math.ceil((circlesPerRow / variants.length) * circleDim),
        circlesPerCol = Math.floor(height / circleDim),
        processData = function (d) {
        return {
            opacity : 1 - (_.reduce(d, function(a, b) { return a + b.val.pvalue }, 0) / d.length),
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

    console.log(circleDim);
    console.log(circlesPerRow);
    console.log(processData(variants[0]));

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
        .attr("cx", function(d, i) { return (circleDim  * (i % circlesPerRow)) + margin.left; })
        .attr("cy", function(d, i) { return (circleDim * Math.floor((i + 1) / circlesPerRow)) + margin.top; })
        .attr("fill", "green")
        .attr("stroke", "black")
        .attr("stroke-width", "1")
        .attr("opacity", function (d, i) { return d.opacity; });

    summaries.attr("cursor", "pointer")
        .on("click", function(d,i) { zoomVariant(d, sm); return; });

    summaries.append("title")
        .text(function (d) { return _.reduce(_.map(d.questions
                                                   , function (q) { return sm.getQuestionById(q).qtext+"\n"; })
                                             , function (a, b) { return a + b; }
                                             , "");
                                        });


};