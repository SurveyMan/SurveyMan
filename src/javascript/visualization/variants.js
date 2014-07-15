
var getQuestionHTML = function(q, responseList, ct) {
    console.log(q.qtext, responseList.length, ct);
    var div = document.createElement("div");
//    div.id = q.id;
    $(div).attr({"class" : "col-md-6"});
    $(div).css("margin-top" , "10px");
    $(div).css("padding-right", "10px");
    //$(div).css("border", "solid");
    $(div).append("<p class='text-center'>" + q.qtext + "</p>");
    //$(div).append(sm.getOptionHTML(q));
    makeResponseChart(q, responseList, ct, div);
    return div;
};


var getResponseCounts = function(q) {
    // return a map of options to counts
    var jsonizedResponses = _.map(responses, function (r) { return JSON.parse(r.response.responses); }),
        responsesForQ = _.map(jsonizedResponses, function (sr) { return _.filter(sr, function (r) { return r.q===q.id;})})
    return _.filter(responsesForQ, function (rfq) { return rfq.length > 0 && rfq[0].hasOwnProperty("opts"); });
};

var makeResponseChart = function (q, responseMap, ct, targetDiv) {

    var margin = {top : 30, left: 25, bottom : 0, right : 40},
        indexLookup = _.map(q.options, function (o) { return o.id; }),
        svgWidth = 350,
        barWidth = svgWidth / q.options.length - 5,
        ctHeight = 10,
        axisThickness = 2,
        tickThickness = 1
        tickInterval = 5,
        tickLength = 5,
        color = _.uniq(_.map(_.range(q.options.length), function (foo) { return "hsl(" + ((360 / q.options.length) * foo) + ",100%,50%)"; })),
        data = _.flatten(_.map(q.options, function (option) {
                                                return _.map(_.range(q.options.length), function (i) {
                                                                                            return {
                                                                                                id : option.id,
                                                                                                text : option.otext,
                                                                                                index : i,
                                                                                                ct : _.filter(responseMap, function (m) {
                                                                                                                            var opt = m[0].opts[0];
                                                                                                                            return opt.o===option.id && opt.oindex===i;}).length
                                                                                            };
                                                                                        });
                                           })),
        svgHeight = ct * ctHeight;



    console.log("data", data);

    var svg = d3.select(targetDiv).append("svg")
        .attr("width", svgWidth + margin.left + margin.right)
        .attr("height", svgHeight + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate("+ margin.left + "," + margin.top + ")");

    var bars = svg.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d, i) { return margin.left + _.indexOf(indexLookup, d.id) * barWidth; })
        .attr("y", function (d, i) {
                        return margin.top + (_.reduce(_.filter(data, function (_d) { return d.id === _d.id && _d.index < d.index; })
                                              , function (n, o) { return n + o.ct; }
                                              , 0) * ctHeight);
                   })
        .attr("height", function (d, i) { return d.ct * ctHeight; })
        .attr("width", barWidth)
        .attr("fill", function(d, i) { return color[d.index]; })
        .attr("stroke", "black")
        .attr("stroke-width", tickThickness);

    bars.append("title")
        .text(function (d) { return d.text + " (index: " + d.index + ", count: " + d.ct + ")"; });

    var xAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("y1", margin.top)
        .attr("x2", margin.left + svgWidth)
        .attr("y2", margin.top)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");

    var x = _.map(q.options, function (option) { return option.otext; });

    var xLab = svg.selectAll(".xLabel")
        .data(x)
        .enter()
        .append("text")
        .text(function (d) { return d;})
        .style("text-anchor", "start")
        .attr("transform", "rotate(-15)")
//        .attr("x", function (d, i) { return margin.left + (i * barWidth); })
//        .attr("y", function (d, i) { return margin.top; });
        .attr("x", function (d, i) { return margin.left + (i * barWidth) + (Math.pow(i, 2) * Math.cos(15));} )
        .attr("y", function (d, i) { return margin.top + (i * Math.sin(15) * margin.top) + (Math.pow(i, 2) * Math.sin(15));})

    var yAxis = svg.append("line")
        .attr("x1", margin.left)
        .attr("x2", margin.left)
        .attr("y1", margin.top)
        .attr("y2", svgHeight+margin.top)
        .attr("stroke-width", axisThickness)
        .attr("stroke", "black");

    var y = _.rest(_.map(_.range(Math.floor((svgHeight / ctHeight) / tickInterval)), function (n) { return n * tickInterval; }));

   svg.selectAll(".yTick")
        .data(y)
        .enter()
        .append("line")
        .attr("x1", margin.left - tickLength)
        .attr("y1", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); })
        .attr("x2", margin.left)
        .attr("y2", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); })
        .attr("stroke-width", tickThickness)
        .attr("stroke", "black");


    var yLab = svg.selectAll(".yLabel")
        .data(y)
        .enter()
        .append("text")
        .text(function (d) { return d; })
        .style("text-anchor", "end")
        .attr("x", 0)
        .attr("y", function (d, i) { return margin.top + ((i + 1) * tickInterval * ctHeight); })


};
var zoomVariant = function(d, sm) {
    $("#questionCloseup").empty();
    var data = d.data;
        html = "<div class='row' style='margin-bottom:40px;'></div>";
    for (var i = 0 ; i < data.length ; i++) {
        var newDiv = $.parseHTML(html);
        $(newDiv).css("background", "#FFFAFA");
        $(newDiv).append("<div class='col-md-12 text-center'><b>pVal:&nbsp;"+data[i].pval+"</div>");
        var q1 = data[i].q1,
            q2 = data[i].q2,
            q1Div = getQuestionHTML(q1, getResponseCounts(q1), data[i].ct1),
            q2Div = getQuestionHTML(q2, getResponseCounts(q2), data[i].ct2);
        console.log(q1Div, q2Div);
        $(newDiv).append(q1Div);
        $(newDiv).append(q2Div);
        $(newDiv).append("<div class='col-md-12'></div>");
        $(newDiv).append("<div class='col-md-6 text-center'><b>#/Responses</b>:&nbsp;"+data[i].ct1+"</div>");
        $(newDiv).append("<div class='col-md-6 text-center'><b>#/Responses</b>:&nbsp;"+data[i].ct2+"</div>");
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

    d3.select("#vars").append("text").text("Each circle is a collection of variants; the color intensity indicates the average p-value of pairwise comparisons that appear to be drawn from different distributions.\n");

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