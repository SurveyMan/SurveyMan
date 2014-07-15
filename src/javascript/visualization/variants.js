
var zoomVariant = function(d, sm) {
    $("#questionCloseup").empty();
    var data = d.data;
        html = "<div class='row'></div>";
    for (var i = 0 ; i < data.length ; i++) {
        var newDiv = $.parseHTML(html);
        $(newDiv).append("<div class='col-md-12 text-center'><b>pVal:&nbsp;"+data[i].pval+"</div>");
        var q1 = data[i].q1,
            q2 = data[i].q2,
            q1Div = getQuestionHTML(q1, data[i].ct1, sm),
            q2Div = getQuestionHTML(q2, data[i].ct2, sm);
        $(q1Div).attr({"class" : "col-md-6"});
        $(q2Div).attr({"class" : "col-md-6"});
        makeResponseChart(q1, getResponseCounts(q1), q1Div);
        makeResponseChart(q2, getResponseCounts(q2), q2Div);
        $(q1Div).append("<div class='col-md-12'><b>#/Responses</b>:&nbsp;"+data[i].ct1+"</div>");
        $(q2Div).append("<div class='col-md-12'><b>#/Responses</b>:&nbsp;"+data[i].ct2+"</div>");
        $(newDiv).append(q1Div);
        $(newDiv).append(q2Div);
        $("#questionCloseup").append(newDiv);
    }
    $("#questionCloseup").show();
};

var makeVariantDisplay = function (variants, sm) {

    var circleRadius = 30,
        padding = 10,
        circleDim = (circleRadius * 2) + padding,
        circlesPerRow = Math.floor(width / circleDim),
        height = (Math.ceil(variants.length / circlesPerRow) * circleDim) + margin.top,
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

    console.log(height);

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
        .attr("cy", function(d, i) { return (circleDim * Math.floor(i / circlesPerRow)) + margin.top; })
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